package searchjobs.pds.back.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchjobs.pds.back.entities.Stack;
import searchjobs.pds.back.services.StackService;

import java.util.List;

@RestController
@RequestMapping("/api/stacks")
public class StackController {
    private final StackService stackService;

    public StackController(StackService stackService){
        this.stackService = stackService;
    }

    @GetMapping
    public List<Stack> listar(){
        return stackService.listarTodas();
    }
}
