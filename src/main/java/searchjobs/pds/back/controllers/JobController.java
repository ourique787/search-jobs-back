package searchjobs.pds.back.controllers;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import searchjobs.pds.back.entities.Job;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.services.ApplicationService;
import searchjobs.pds.back.services.DescricaoEnricherService;
import searchjobs.pds.back.services.JobService;
import searchjobs.pds.back.services.ScrapingOrchestratorService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/jobs")
public class JobController {

    private final JobService jobService;
    private final ApplicationService applicationService;
    private final DescricaoEnricherService descricaoEnricherService;
    private final ScrapingOrchestratorService scrapingOrchestratorService;

    public JobController(JobService jobService, ApplicationService applicationService,
                         DescricaoEnricherService descricaoEnricherService,
                         ScrapingOrchestratorService scrapingOrchestratorService) {
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.descricaoEnricherService = descricaoEnricherService;
        this.scrapingOrchestratorService = scrapingOrchestratorService;
    }

    @GetMapping
    public ResponseEntity<List<Job>> listarTodas(){
        return ResponseEntity.ok(jobService.listarTodas());
    }

    @PostMapping
    public ResponseEntity<Job> criar(@RequestBody Job job){
        return ResponseEntity.ok(jobService.salvarVaga(job));
    }

    @PostMapping("/iniciar-scraping")
    public ResponseEntity<Map<String, String>> iniciarScraping() {
        new Thread(scrapingOrchestratorService::iniciarPipeline).start();
        return ResponseEntity.ok(Map.of(
                "status", "Pipeline iniciado: 3 scrapers em paralelo + enriquecimento. Acompanhe os logs."
        ));
    }

    @PostMapping("/enriquecer-descricoes")
    public ResponseEntity<Map<String, String>> enriquecerDescricoes() {
        new Thread(descricaoEnricherService::enriquecerDescricoes).start();
        return ResponseEntity.ok(Map.of(
                "status", "Enriquecimento iniciado. Acompanhe os logs do servidor."
        ));
    }

    /**
     * Chamado pelo frontend quando o usuário clica em uma vaga.
     * Retorna os detalhes da vaga e registra automaticamente como "visualizado".
     */
    @PostMapping("/{id}/click")
    public ResponseEntity<Job> registrarClique(
            @PathVariable Long id,
            @AuthenticationPrincipal User usuario) {

        return jobService.buscarPorId(id)
                .map(vaga -> {
                    applicationService.registrarClique(usuario, vaga);
                    return ResponseEntity.ok(vaga);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
