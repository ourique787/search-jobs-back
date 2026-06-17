package searchjobs.pds.back.services;

import org.springframework.stereotype.Service;
import searchjobs.pds.back.entities.Job;
import searchjobs.pds.back.repositories.ApplicationRepository;
import searchjobs.pds.back.repositories.JobRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class JobValidationService {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public JobValidationService(JobRepository jobRepository,
                                ApplicationRepository applicationRepository) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
    }

    public void validarVagas() {
        System.out.println("\n════════════════════════════════════");
        System.out.println("🔍 [Validação] Buscando vagas no banco...");
        System.out.println("════════════════════════════════════");

        List<Job> vagas = jobRepository.findByAtivoTrue();

        System.out.println("🔍 [Validação] Verificando " + vagas.size() + " vagas...");

        AtomicInteger removidas  = new AtomicInteger();
        AtomicInteger desativadas = new AtomicInteger();
        AtomicInteger mantidas   = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (Job vaga : vagas) {
            executor.submit(() -> {
                try {
                    if (vagaEstaAtiva(vaga.getLinkOriginal())) {
                        mantidas.incrementAndGet();
                        return;
                    }

                    boolean temCandidatura = applicationRepository.existsByVagaId(vaga.getId());
                    if (temCandidatura) {
                        vaga.setAtivo(false);
                        jobRepository.save(vaga);
                        desativadas.incrementAndGet();
                        System.out.println("⚠️  [Validação] Desativada (tem candidatura): " + vaga.getTitulo());
                    } else {
                        jobRepository.delete(vaga);
                        removidas.incrementAndGet();
                        System.out.println("🗑️  [Validação] Removida: " + vaga.getTitulo() + " | " + vaga.getLinkOriginal());
                    }

                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.out.println("⚠️  [Validação] Erro ao verificar vaga id=" + vaga.getId()
                            + ": " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n════════════════════════════════════");
        System.out.println("✅ [Validação] Concluída!");
        System.out.println("   Mantidas:    " + mantidas.get());
        System.out.println("   Desativadas: " + desativadas.get() + " (com candidatura)");
        System.out.println("   Removidas:   " + removidas.get());
        System.out.println("════════════════════════════════════\n");
    }

    private boolean vagaEstaAtiva(String url) {
        if (url == null || url.isBlank()) return false;

        // Gupy será substituído futuramente — ignora por enquanto
        if (url.contains("gupy.io")) return true;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 404 || status == 410) return false;
            if (status >= 400) return true; // login wall ou erro do servidor — conservador

            String body = response.body().toLowerCase();

            if (url.contains("empregos.com.br")) {
                boolean encerrada = body.contains("essa vaga foi encerrada");
                if (encerrada) System.out.println("💀 [Validação] Encerrada (empregos.com.br): " + url);
                return !encerrada;
            }
            if (url.contains("infojobs.com.br")) {
                boolean encerrada = body.contains("não está mais disponivel")
                        || body.contains("não está mais disponível");
                if (encerrada) System.out.println("💀 [Validação] Encerrada (infojobs.com.br): " + url);
                return !encerrada;
            }

            return true;

        } catch (Exception e) {
            System.out.println("⚠️  [Validação] Falha ao checar " + url + " → " + e.getMessage());
            return true;
        }
    }
}
