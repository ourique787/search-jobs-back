package searchjobs.pds.back.services;

import org.springframework.stereotype.Service;
import searchjobs.pds.back.entities.Application;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.repositories.ApplicationRepository;

import java.util.List;

@Service
public class ApplicationService {
    private final ApplicationRepository applicationRepository;

    public ApplicationService(ApplicationRepository applicationRepository){
        this.applicationRepository = applicationRepository;
    }

    public Application candidatar(Application application){
        //implementar lógica futuramente
        return applicationRepository.save(application);
    }

    public List<Application> listarPorUsuario(User usuario){
        return applicationRepository.findByUsuario(usuario);
    }
}
