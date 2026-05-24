package searchjobs.pds.back.services;

import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import searchjobs.pds.back.entities.Job;
import searchjobs.pds.back.entities.Senioridade;
import searchjobs.pds.back.entities.Stack;
import searchjobs.pds.back.repositories.JobRepository;
import searchjobs.pds.back.repositories.StackRepository;

import java.util.List;

@Service
@Order(5) // Roda após todos os scrapers: DatabaseSeeder(1) > Gupy(2) > InfoJobs(3) > Empregos(4)
public class JobEnricherService implements CommandLineRunner {

    private final JobRepository jobRepository;
    private final StackRepository stackRepository;

    public JobEnricherService(JobRepository jobRepository, StackRepository stackRepository) {
        this.jobRepository = jobRepository;
        this.stackRepository = stackRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        enriquecerVagas();
    }

    @Transactional
    public void enriquecerVagas() {
        List<Job> vagas = jobRepository.findAllComStacks();
        List<Stack> todasStacks = stackRepository.findAll();

        if (vagas.isEmpty()) {
            System.out.println("⚠️ Nenhuma vaga para enriquecer.");
            return;
        }

        System.out.println("\n════════════════════════════════════");
        System.out.println("🔬 Iniciando enriquecimento de " + vagas.size() + " vagas...");
        System.out.println("════════════════════════════════════");

        int totalSenioridade = 0;
        int totalStacks      = 0;

        for (Job vaga : vagas) {
            String titulo = vaga.getTitulo() != null
                    ? vaga.getTitulo().toLowerCase()
                    : "";

            // --- SENIORIDADE ---
            if (vaga.getSenioridade() == null || vaga.getSenioridade() == Senioridade.NAO_INFORMADO) {
                Senioridade senioridade = detectarSenioridade(titulo);
                vaga.setSenioridade(senioridade);
                totalSenioridade++;
            }

            // --- STACKS ---
            for (Stack stack : todasStacks) {
                String nomeStack = stack.getNome().toLowerCase();

                // Verifica se o título contém o nome da stack
                // Usa \b para garantir match de palavra inteira (ex: "go" não bate em "django")
                boolean contemStack = titulo.matches(".*\\b" + nomeStack.replace(".", "\\.") + "\\b.*");

                if (contemStack && !vaga.getStacksRequisitadas().contains(stack)) {
                    vaga.getStacksRequisitadas().add(stack);
                    totalStacks++;
                }
            }

            jobRepository.save(vaga);
        }

        System.out.println("✅ Enriquecimento finalizado!");
        System.out.println("📊 Senioridades detectadas: " + totalSenioridade);
        System.out.println("🔧 Stacks associadas: " + totalStacks);
        System.out.println("════════════════════════════════════\n");
    }

    private Senioridade detectarSenioridade(String titulo) {
        // Ordem importa — verifica do mais específico para o mais genérico
        if (contemPalavra(titulo, "estágio", "estagio", "estagiário", "estagiario", "intern")) {
            return Senioridade.ESTAGIARIO;
        }
        if (contemPalavra(titulo, "junior", "júnior", "jr")) {
            return Senioridade.JUNIOR;
        }
        if (contemPalavra(titulo, "pleno", "pl", "mid", "intermediário", "intermediario")) {
            return Senioridade.PLENO;
        }
        if (contemPalavra(titulo, "senior", "sênior", "sênior", "sr", "especialista", "lead", "principal", "staff")) {
            return Senioridade.SENIOR;
        }
        return Senioridade.NAO_INFORMADO;
    }

    private boolean contemPalavra(String texto, String... palavras) {
        for (String palavra : palavras) {
            // \b garante que é palavra inteira — evita "sr" bater em "srta" por exemplo
            if (texto.matches(".*\\b" + palavra + "\\b.*")) {
                return true;
            }
        }
        return false;
    }
}