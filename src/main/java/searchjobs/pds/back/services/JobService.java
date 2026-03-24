package searchjobs.pds.back.services;

import jakarta.transaction.Transactional;
import searchjobs.pds.back.entities.Job;
import searchjobs.pds.back.repositories.JobRepository;

import java.util.List;

public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository){
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Job salvarVaga(Job job){
        return jobRepository.findByLinkOriginal(job.getLinkOriginal())
                .orElseGet(() -> jobRepository.save(job));
    }

    public List<Job> listarTodas(){
        return jobRepository.findAll();
    }


}
