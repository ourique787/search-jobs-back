package searchjobs.pds.back.services;

import org.springframework.stereotype.Service;
import searchjobs.pds.back.dto.RelatorioDTO;
import searchjobs.pds.back.dto.RelatorioFiltroDTO;
import searchjobs.pds.back.entities.Application;
import searchjobs.pds.back.entities.Senioridade;
import searchjobs.pds.back.entities.Stack;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.repositories.ApplicationRepository;
import searchjobs.pds.back.repositories.StackRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelatorioService {

    private final ApplicationRepository applicationRepository;
    private final StackRepository stackRepository;

    private static final DateTimeFormatter MES_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM");

    private static final List<String> DIAS_SEMANA =
            List.of("SEGUNDA", "TERÇA", "QUARTA", "QUINTA", "SEXTA", "SÁBADO", "DOMINGO");

    public RelatorioService(ApplicationRepository applicationRepository,
                            StackRepository stackRepository) {
        this.applicationRepository = applicationRepository;
        this.stackRepository = stackRepository;
    }

    public RelatorioDTO gerar(User usuario, RelatorioFiltroDTO filtro) {

        // Resolve datas
        LocalDateTime dataInicio = filtro.dataInicio() != null
                ? filtro.dataInicio().atStartOfDay()
                : null;
        LocalDateTime dataFim = filtro.dataFim() != null
                ? filtro.dataFim().atTime(23, 59, 59)
                : null;

        // Resolve senioridades
        List<Senioridade> senioridades = (filtro.senioridades() != null && !filtro.senioridades().isEmpty())
                ? filtro.senioridades()
                : null;

        // Busca todas as candidaturas do usuário (JOIN FETCH já carrega vaga + stacks)
        List<Application> candidaturas = applicationRepository.findComFiltros(usuario);

        // Filtros em memória — PostgreSQL rejeita parâmetros NULL sem tipo em JPQL
        if (dataInicio != null) {
            candidaturas = candidaturas.stream()
                    .filter(a -> !a.getDataInteracao().isBefore(dataInicio))
                    .collect(Collectors.toList());
        }
        if (dataFim != null) {
            candidaturas = candidaturas.stream()
                    .filter(a -> !a.getDataInteracao().isAfter(dataFim))
                    .collect(Collectors.toList());
        }

        if (senioridades != null && !senioridades.isEmpty()) {
            Set<Senioridade> filtroSenioridade = new HashSet<>(senioridades);
            candidaturas = candidaturas.stream()
                    .filter(a -> filtroSenioridade.contains(a.getVaga().getSenioridade()))
                    .collect(Collectors.toList());
        }

        if (filtro.stackIds() != null && !filtro.stackIds().isEmpty()) {
            Set<Long> idsDesejados = new HashSet<>(filtro.stackIds());
            candidaturas = candidaturas.stream()
                    .filter(a -> a.getVaga().getStacksRequisitadas().stream()
                            .anyMatch(s -> idsDesejados.contains(s.getId())))
                    .collect(Collectors.toList());
        }

        // Monta descrição dos filtros aplicados
        List<String> nomesStacks = resolverNomesStacks(filtro.stackIds());
        List<String> nomesSenioridades = senioridades != null
                ? senioridades.stream().map(Enum::name).collect(Collectors.toList())
                : List.of("Todas");

        RelatorioDTO.FiltrosAplicados filtrosAplicados = new RelatorioDTO.FiltrosAplicados(
                filtro.dataInicio() != null ? filtro.dataInicio().toString() : "Sem limite",
                filtro.dataFim()    != null ? filtro.dataFim().toString()    : "Sem limite",
                nomesStacks.isEmpty() ? List.of("Todas") : nomesStacks,
                nomesSenioridades
        );

        // Monta itens detalhados
        List<RelatorioDTO.CandidaturaItem> itens = candidaturas.stream()
                .map(this::toItem)
                .collect(Collectors.toList());

        // Resumo
        RelatorioDTO.Resumo resumo = montarResumo(itens);

        return new RelatorioDTO(
                usuario.getNome(),
                usuario.getEmail(),
                LocalDateTime.now(),
                filtrosAplicados,
                resumo,
                itens
        );
    }

    // ── Conversão ─────────────────────────────────────────────────────────

    private RelatorioDTO.CandidaturaItem toItem(Application app) {
        List<String> stacks = app.getVaga().getStacksRequisitadas().stream()
                .map(Stack::getNome)
                .sorted()
                .collect(Collectors.toList());

        return new RelatorioDTO.CandidaturaItem(
                app.getVaga().getId(),
                app.getVaga().getTitulo(),
                app.getVaga().getEmpresa(),
                app.getVaga().getFonte(),
                app.getVaga().getSenioridade(),
                stacks,
                app.getVaga().getLinkOriginal(),
                app.getStatus() != null ? app.getStatus() : "visualizado",
                app.getDataInteracao()
        );
    }

    // ── Resumo agregado ───────────────────────────────────────────────────

    private RelatorioDTO.Resumo montarResumo(List<RelatorioDTO.CandidaturaItem> itens) {

        // Por fonte (Gupy, InfoJobs, Empregos.com.br)
        Map<String, Long> porFonte = itens.stream()
                .collect(Collectors.groupingBy(
                        i -> i.fonte() != null ? i.fonte() : "Desconhecida",
                        Collectors.counting()));

        // Por senioridade
        Map<String, Long> porSenioridade = itens.stream()
                .collect(Collectors.groupingBy(
                        i -> i.senioridade() != null ? i.senioridade().name() : "NAO_INFORMADO",
                        Collectors.counting()));

        // Por stack (cada vaga pode ter N stacks)
        Map<String, Long> porStack = new TreeMap<>();
        for (RelatorioDTO.CandidaturaItem item : itens) {
            for (String stack : item.stacks()) {
                porStack.merge(stack, 1L, Long::sum);
            }
        }

        // Por status
        Map<String, Long> porStatus = itens.stream()
                .collect(Collectors.groupingBy(
                        RelatorioDTO.CandidaturaItem::status,
                        Collectors.counting()));

        // Distribuição temporal
        RelatorioDTO.DistribuicaoTemporal distribuicao = montarDistribuicao(itens);

        return new RelatorioDTO.Resumo(
                itens.size(),
                new TreeMap<>(porFonte),
                new TreeMap<>(porSenioridade),
                porStack,
                new TreeMap<>(porStatus),
                distribuicao
        );
    }

    private RelatorioDTO.DistribuicaoTemporal montarDistribuicao(
            List<RelatorioDTO.CandidaturaItem> itens) {

        // Candidaturas por mês (ex: "2024-05" → 8)
        Map<String, Long> porMes = new TreeMap<>(
                itens.stream().collect(Collectors.groupingBy(
                        i -> i.dataInteracao().format(MES_FORMATTER),
                        Collectors.counting()))
        );

        // Candidaturas por dia da semana (1=seg … 7=dom)
        Map<String, Long> porDia = new LinkedHashMap<>();
        DIAS_SEMANA.forEach(d -> porDia.put(d, 0L));
        for (RelatorioDTO.CandidaturaItem item : itens) {
            int dow = item.dataInteracao().getDayOfWeek().getValue(); // 1=MON … 7=SUN
            String nomeDia = DIAS_SEMANA.get(dow - 1);
            porDia.merge(nomeDia, 1L, Long::sum);
        }

        return new RelatorioDTO.DistribuicaoTemporal(porMes, porDia);
    }

    // ── Utilitários ───────────────────────────────────────────────────────

    private List<String> resolverNomesStacks(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return stackRepository.findAllById(ids).stream()
                .map(Stack::getNome)
                .collect(Collectors.toList());
    }
}
