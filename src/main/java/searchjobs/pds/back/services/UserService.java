package searchjobs.pds.back.services;

import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.repositories.UserRepository;

import java.util.Optional;

public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public User salvar(User user){
        return userRepository.save(user);
    }

    public Optional<User> buscarPorEmail(String email){
        return userRepository.findByEmail(email);
    }
}
