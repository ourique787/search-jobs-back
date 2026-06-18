package searchjobs.pds.back.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import searchjobs.pds.back.entities.Job;
import searchjobs.pds.back.repositories.ApplicationRepository;
import searchjobs.pds.back.repositories.JobRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class JobTitleFilterService {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    public JobTitleFilterService(JobRepository jobRepository,
                                 ApplicationRepository applicationRepository) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public void filtrarVagasNaoTech() {
        List<Job> vagas = jobRepository.findByAtivoTrue();

        System.out.println("\n════════════════════════════════════");
        System.out.println("🔎 [FiltroTítulo] Verificando " + vagas.size() + " vagas...");
        System.out.println("════════════════════════════════════");

        AtomicInteger removidas   = new AtomicInteger();
        AtomicInteger desativadas = new AtomicInteger();
        AtomicInteger mantidas    = new AtomicInteger();

        for (Job vaga : vagas) {
            try {
                if (ScraperJobFilter.eTechJob(vaga.getTitulo())) {
                    mantidas.incrementAndGet();
                    continue;
                }

                boolean temCandidatura = applicationRepository.existsByVagaId(vaga.getId());
                if (temCandidatura) {
                    vaga.setAtivo(false);
                    jobRepository.save(vaga);
                    desativadas.incrementAndGet();
                    System.out.println("⚠️  [FiltroTítulo] Desativada (tem candidatura): " + vaga.getTitulo());
                } else {
                    // Limpa stacks antes de deletar para evitar violação de FK
                    vaga.getStacksRequisitadas().clear();
                    jobRepository.save(vaga);
                    jobRepository.delete(vaga);
                    removidas.incrementAndGet();
                    System.out.println("🗑️  [FiltroTítulo] Removida: " + vaga.getTitulo());
                }
            } catch (Exception e) {
                System.out.println("⚠️  [FiltroTítulo] Erro ao processar '" + vaga.getTitulo() + "': " + e.getMessage());
            }
        }

        System.out.println("\n════════════════════════════════════");
        System.out.println("✅ [FiltroTítulo] Concluído!");
        System.out.println("   Mantidas:    " + mantidas.get());
        System.out.println("   Removidas:   " + removidas.get());
        System.out.println("   Desativadas: " + desativadas.get() + " (com candidatura)");
        System.out.println("════════════════════════════════════\n");
    }
}
