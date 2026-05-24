package searchjobs.pds.back.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import searchjobs.pds.back.entities.Job;
import searchjobs.pds.back.entities.Stack;
import searchjobs.pds.back.repositories.JobRepository;

import java.util.List;
import java.util.Optional;

@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Job salvarVaga(Job job) {
        return jobRepository.findByLinkOriginal(job.getLinkOriginal())
                .orElseGet(() -> jobRepository.save(job));
    }

    public Optional<Job> buscarPorLink(String link) {
        return jobRepository.findByLinkOriginal(link);
    }

    public Optional<Job> buscarPorId(Long id) {
        return jobRepository.findById(id);
    }

    @Transactional
    public void associarStack(Job vaga, Stack stack) {
        // Recarrega dentro da transação para evitar LazyInitializationException
        Job managed = jobRepository.findById(vaga.getId())
                .orElseThrow(() -> new RuntimeException("Job não encontrado: " + vaga.getId()));
        if (!managed.getStacksRequisitadas().contains(stack)) {
            managed.getStacksRequisitadas().add(stack);
            jobRepository.save(managed);
        }
    }

    public List<Job> listarTodas() {
        return jobRepository.findAll();
    }
}