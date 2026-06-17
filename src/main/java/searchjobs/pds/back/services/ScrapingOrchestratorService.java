package searchjobs.pds.back.services;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Service
@Order(2)
public class ScrapingOrchestratorService implements CommandLineRunner {

    private final ScraperService scraperService;
    private final InfoJobsScraperService infoJobsScraperService;
    private final EmpregosBrScraperService empregosBrScraperService;
    private final JobEnricherService jobEnricherService;
    private final DescricaoEnricherService descricaoEnricherService;
    private final JobValidationService jobValidationService;

    public ScrapingOrchestratorService(ScraperService scraperService,
                                       InfoJobsScraperService infoJobsScraperService,
                                       EmpregosBrScraperService empregosBrScraperService,
                                       JobEnricherService jobEnricherService,
                                       DescricaoEnricherService descricaoEnricherService,
                                       JobValidationService jobValidationService) {
        this.scraperService = scraperService;
        this.infoJobsScraperService = infoJobsScraperService;
        this.empregosBrScraperService = empregosBrScraperService;
        this.jobEnricherService = jobEnricherService;
        this.descricaoEnricherService = descricaoEnricherService;
        this.jobValidationService = jobValidationService;
    }

    @Override
    public void run(String... args) {
        // Descomente para ativar o pipeline completo na inicialização:
       //  iniciarPipeline();
    }

    public void iniciarPipeline() {
        System.out.println("\n════════════════════════════════════");
        System.out.println("🚀 [Orchestrator] Scrapers iniciados em paralelo...");
        System.out.println("════════════════════════════════════\n");

        ExecutorService executor = Executors.newFixedThreadPool(3);

        Future<?> gupy     = executor.submit(scraperService::iniciarScraping);
        Future<?> infoJobs = executor.submit(infoJobsScraperService::iniciarScraping);
        Future<?> empregos = executor.submit(empregosBrScraperService::iniciarScraping);

        executor.shutdown();

        aguardar("Gupy",     gupy);
        aguardar("InfoJobs", infoJobs);
        aguardar("Empregos", empregos);

        System.out.println("\n════════════════════════════════════");
        System.out.println("✅ [Orchestrator] Scrapers concluídos — validando vagas existentes...");
        System.out.println("════════════════════════════════════\n");

        jobValidationService.validarVagas();

        System.out.println("\n════════════════════════════════════");
        System.out.println("✅ [Orchestrator] Validação concluída — iniciando enriquecimento...");
        System.out.println("════════════════════════════════════\n");

        jobEnricherService.enriquecerVagas();
        descricaoEnricherService.enriquecerDescricoes();

        System.out.println("\n════════════════════════════════════");
        System.out.println("🎉 [Orchestrator] Pipeline finalizado!");
        System.out.println("════════════════════════════════════\n");
    }

    private void aguardar(String nome, Future<?> future) {
        try {
            future.get();
        } catch (ExecutionException e) {
            System.err.println("❌ [Orchestrator] Erro no scraper " + nome + ": "
                    + e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("❌ [Orchestrator] Scraper " + nome + " interrompido.");
        }
    }
}
