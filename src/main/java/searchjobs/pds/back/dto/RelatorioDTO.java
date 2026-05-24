package searchjobs.pds.back.dto;

import searchjobs.pds.back.entities.Senioridade;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record RelatorioDTO(

        // Cabeçalho
        String nomeUsuario,
        String emailUsuario,
        LocalDateTime geradoEm,
        FiltrosAplicados filtrosAplicados,

        // Resumo agregado
        Resumo resumo,

        // Lista detalhada
        List<CandidaturaItem> candidaturas

) {

    public record FiltrosAplicados(
            String dataInicio,
            String dataFim,
            List<String> stacks,
            List<String> senioridades
    ) {}

    public record Resumo(
            int totalCandidaturas,
            Map<String, Long> porFonte,
            Map<String, Long> porSenioridade,
            Map<String, Long> porStack,
            Map<String, Long> porStatus,
            DistribuicaoTemporal distribuicaoTemporal
    ) {}

    public record DistribuicaoTemporal(
            Map<String, Long> porMes,
            Map<String, Long> porDiaDaSemana
    ) {}

    public record CandidaturaItem(
            Long vagaId,
            String titulo,
            String empresa,
            String fonte,
            Senioridade senioridade,
            List<String> stacks,
            String linkOriginal,
            String status,
            LocalDateTime dataInteracao
    ) {}
}
