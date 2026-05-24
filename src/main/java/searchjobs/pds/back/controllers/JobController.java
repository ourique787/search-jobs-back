package searchjobs.pds.back.controllers;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import searchjobs.pds.back.entities.Job;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.services.ApplicationService;
import searchjobs.pds.back.services.JobService;

import java.util.List;

@RestController
@RequestMapping("api/jobs")
public class JobController {

    private final JobService jobService;
    private final ApplicationService applicationService;

    public JobController(JobService jobService, ApplicationService applicationService){
        this.jobService = jobService;
        this.applicationService = applicationService;
    }

    @GetMapping
    public ResponseEntity<List<Job>> listarTodas(){
        return ResponseEntity.ok(jobService.listarTodas());
    }

    @PostMapping
    public ResponseEntity<Job> criar(@RequestBody Job job){
        return ResponseEntity.ok(jobService.salvarVaga(job));
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
