package searchjobs.pds.back.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchjobs.pds.back.entities.Job;
import searchjobs.pds.back.entities.Senioridade;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findBySenioridade(Senioridade senioridade);

    //busca por uma empresa especifica
    List<Job> findByEmpresaContainingIgnoreCase(String empresa);
}
