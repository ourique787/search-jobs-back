package searchjobs.pds.back.controllers;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import searchjobs.pds.back.entities.Job;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.services.ApplicationService;
import searchjobs.pds.back.services.DescricaoEnricherService;
import searchjobs.pds.back.services.JobService;
import searchjobs.pds.back.services.JobTitleFilterService;
import searchjobs.pds.back.services.JobValidationService;
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
    private final JobValidationService jobValidationService;
    private final JobTitleFilterService jobTitleFilterService;

    public JobController(JobService jobService, ApplicationService applicationService,
                         DescricaoEnricherService descricaoEnricherService,
                         ScrapingOrchestratorService scrapingOrchestratorService,
                         JobValidationService jobValidationService,
                         JobTitleFilterService jobTitleFilterService) {
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.descricaoEnricherService = descricaoEnricherService;
        this.scrapingOrchestratorService = scrapingOrchestratorService;
        this.jobValidationService = jobValidationService;
        this.jobTitleFilterService = jobTitleFilterService;
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

    @GetMapping("/diagnostico-url")
    public ResponseEntity<Map<String, Object>> diagnosticarUrl(@RequestParam String url) {
        try {
            var client = java.net.http.HttpClient.newBuilder()
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(10))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();
            var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            return ResponseEntity.ok(Map.of(
                    "status", response.statusCode(),
                    "finalUrl", response.uri().toString(),
                    "bodySnippet", body.length() > 500 ? body.substring(0, 500) : body,
                    "contemEncerrada", body.toLowerCase().contains("essa vaga foi encerrada"),
                    "contemDisponivel", body.toLowerCase().contains("não está mais disponivel")
                            || body.toLowerCase().contains("não está mais disponível")
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/filtrar-titulo")
    public ResponseEntity<Map<String, String>> filtrarTitulo() {
        new Thread(() -> {
            try {
                jobTitleFilterService.filtrarVagasNaoTech();
            } catch (Exception e) {
                System.err.println("❌ [FiltroTítulo] Erro fatal: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
        return ResponseEntity.ok(Map.of("status", "Filtro de título iniciado. Acompanhe os logs do servidor."));
    }

    @PostMapping("/validar-vagas")
    public ResponseEntity<Map<String, String>> validarVagas() {
        new Thread(() -> {
            try {
                jobValidationService.validarVagas();
            } catch (Exception e) {
                System.err.println("❌ [Validação] Erro fatal: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
        return ResponseEntity.ok(Map.of("status", "Validação iniciada. Acompanhe os logs do servidor."));
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
