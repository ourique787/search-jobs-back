package searchjobs.pds.back.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchjobs.pds.back.entities.Stack;

import java.util.Optional;

@Repository
public interface StackRepository extends JpaRepository<Stack, Long> {
    Optional<Stack> findByNomeIgnoreCase(String nome);
}
