package searchjobs.pds.back.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(2)
public class ScrapingOrchestratorService implements CommandLineRunner {

    @Value("${scraping.enabled:true}")
    private boolean scrapingEnabled;

    private final TramposScraperService tramposScraperService;
    private final InfoJobsScraperService infoJobsScraperService;
    private final EmpregosBrScraperService empregosBrScraperService;
    private final JobEnricherService jobEnricherService;
    private final DescricaoEnricherService descricaoEnricherService;
    private final JobTitleFilterService jobTitleFilterService;
    private final JobValidationService jobValidationService;

    public ScrapingOrchestratorService(TramposScraperService tramposScraperService,
                                       InfoJobsScraperService infoJobsScraperService,
                                       EmpregosBrScraperService empregosBrScraperService,
                                       JobEnricherService jobEnricherService,
                                       DescricaoEnricherService descricaoEnricherService,
                                       JobTitleFilterService jobTitleFilterService,
                                       JobValidationService jobValidationService) {
        this.tramposScraperService = tramposScraperService;
        this.infoJobsScraperService = infoJobsScraperService;
        this.empregosBrScraperService = empregosBrScraperService;
        this.jobEnricherService = jobEnricherService;
        this.descricaoEnricherService = descricaoEnricherService;
        this.jobTitleFilterService = jobTitleFilterService;
        this.jobValidationService = jobValidationService;
    }

    @Override
    public void run(String... args) {
        if (scrapingEnabled) {
            iniciarPipeline();
        } else {
            System.out.println("ℹ️ [Orchestrator] Scraping desativado (SCRAPING_ENABLED=false).");
        }
    }

    public void iniciarPipeline() {
        System.out.println("\n════════════════════════════════════");
        System.out.println("🚀 [Orchestrator] Scrapers iniciados sequencialmente...");
        System.out.println("════════════════════════════════════\n");

        executar("Trampos",  tramposScraperService::iniciarScraping);
        executar("InfoJobs", infoJobsScraperService::iniciarScraping);
        executar("Empregos", empregosBrScraperService::iniciarScraping);

        System.out.println("\n════════════════════════════════════");
        System.out.println("✅ [Orchestrator] Scrapers concluídos — enriquecendo stacks...");
        System.out.println("════════════════════════════════════\n");

        jobEnricherService.enriquecerVagas();
        descricaoEnricherService.enriquecerDescricoes();

        System.out.println("\n════════════════════════════════════");
        System.out.println("✅ [Orchestrator] Enriquecimento concluído — filtrando vagas não-tech...");
        System.out.println("════════════════════════════════════\n");

        jobTitleFilterService.filtrarVagasNaoTech();

        System.out.println("\n════════════════════════════════════");
        System.out.println("✅ [Orchestrator] Filtro concluído — validando vagas fora do ar...");
        System.out.println("════════════════════════════════════\n");

        jobValidationService.validarVagas();

        System.out.println("\n════════════════════════════════════");
        System.out.println("🎉 [Orchestrator] Pipeline finalizado!");
        System.out.println("════════════════════════════════════\n");
    }

    private void executar(String nome, Runnable tarefa) {
        try {
            tarefa.run();
        } catch (Exception e) {
            System.err.println("❌ [Orchestrator] Erro no scraper " + nome + ": " + e.getMessage());
        }
    }
}
