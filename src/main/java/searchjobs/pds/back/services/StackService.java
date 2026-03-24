package searchjobs.pds.back.services;

import searchjobs.pds.back.entities.Stack;
import searchjobs.pds.back.repositories.StackRepository;

import java.util.List;

public class StackService {

    private final StackRepository stackRepository;

    public StackService(StackRepository stackRepository){
        this.stackRepository = stackRepository;
    }

    public List<Stack> listarTodas(){
        return stackRepository.findAll();
    }

    //se não tiver a stack, ele cria
    public Stack buscarOuCriar(String nome){
        return stackRepository.findByNomeIgnoreCase(nome)
                .orElseGet(() -> stackRepository.save(new Stack(null, nome)));
    }
}
