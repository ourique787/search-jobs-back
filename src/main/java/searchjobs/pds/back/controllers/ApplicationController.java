package searchjobs.pds.back.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchjobs.pds.back.entities.Application;
import searchjobs.pds.back.services.ApplicationService;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService){
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<Application> candidatar(@RequestBody Application application){
        return ResponseEntity.ok(applicationService.candidatar(application));
    }
}
