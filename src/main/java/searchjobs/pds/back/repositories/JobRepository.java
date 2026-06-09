package searchjobs.pds.back.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchjobs.pds.back.entities.Job;
import searchjobs.pds.back.entities.Senioridade;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findBySenioridade(Senioridade senioridade);

    //busca por uma empresa especifica
    List<Job> findByEmpresaContainingIgnoreCase(String empresa);

    Optional<Job> findByLinkOriginal(String linkOriginal);

    @Query("SELECT DISTINCT j FROM Job j LEFT JOIN FETCH j.stacksRequisitadas")
    List<Job> findAllComStacks();

    @Query("SELECT j FROM Job j WHERE j.descricao IS NULL OR j.descricao = '' OR j.descricao = '.' OR LOWER(j.descricao) LIKE '%cookie%' OR LOWER(j.descricao) = 'descrição não encontrada'")
    List<Job> findSemDescricao();
}
