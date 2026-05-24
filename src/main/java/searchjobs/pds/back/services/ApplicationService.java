package searchjobs.pds.back.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import searchjobs.pds.back.entities.Application;
import searchjobs.pds.back.entities.Job;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.repositories.ApplicationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationService {
    private final ApplicationRepository applicationRepository;

    public ApplicationService(ApplicationRepository applicationRepository){
        this.applicationRepository = applicationRepository;
    }

    public Application candidatar(Application application){
        return applicationRepository.save(application);
    }

    public List<Application> listarPorUsuario(User usuario){
        return applicationRepository.findByUsuario(usuario);
    }

    @Transactional
    public Application registrarClique(User usuario, Job vaga) {
        // Se o usuário já clicou nessa vaga, não duplica
        return applicationRepository.findByUsuarioAndVaga(usuario, vaga)
                .orElseGet(() -> {
                    Application app = new Application();
                    app.setUsuario(usuario);
                    app.setVaga(vaga);
                    app.setStatus("visualizado");
                    app.setDataInteracao(LocalDateTime.now());
                    return applicationRepository.save(app);
                });
    }
}
