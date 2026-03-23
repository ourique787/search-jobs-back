package searchjobs.pds.back.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchjobs.pds.back.entities.Application;
import searchjobs.pds.back.entities.User;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByUsuario(User usuario);
}
