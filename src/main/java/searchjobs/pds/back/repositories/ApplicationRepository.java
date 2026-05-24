package searchjobs.pds.back.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchjobs.pds.back.entities.Application;
import searchjobs.pds.back.entities.Job;
import searchjobs.pds.back.entities.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUsuario(User usuario);

    Optional<Application> findByUsuarioAndVaga(User usuario, Job vaga);

    @Query("""
        SELECT DISTINCT a FROM Application a
        JOIN FETCH a.vaga v
        LEFT JOIN FETCH v.stacksRequisitadas
        WHERE a.usuario = :usuario
        ORDER BY a.dataInteracao DESC
    """)
    List<Application> findComFiltros(@Param("usuario") User usuario);
}
