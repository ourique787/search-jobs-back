package searchjobs.pds.back.controllers;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchjobs.pds.back.entities.Job;
import searchjobs.pds.back.services.JobService;

import java.util.List;

@RestController
@RequestMapping("api/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService){
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<List<Job>> listarTodas(){
        return ResponseEntity.ok(jobService.listarTodas());
    }

    @PostMapping
    public ResponseEntity<Job> criar(@RequestBody Job job){
        return ResponseEntity.ok(jobService.salvarVaga(job));
    }
}
