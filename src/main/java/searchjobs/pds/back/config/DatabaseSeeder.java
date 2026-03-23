package searchjobs.pds.back.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import searchjobs.pds.back.entities.Stack;
import searchjobs.pds.back.entities.Senioridade;
import searchjobs.pds.back.entities.Job;
import searchjobs.pds.back.repositories.StackRepository;
import searchjobs.pds.back.repositories.JobRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class DatabaseSeeder implements CommandLineRunner {

    private final StackRepository stackRepository;
    private final JobRepository jobRepository;

    // O Spring injeta os Repositories automaticamente aqui
    public DatabaseSeeder(StackRepository stackRepository, JobRepository jobRepository) {
        this.stackRepository = stackRepository;
        this.jobRepository = jobRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        // 1. Testando e Populando Stacks (Tecnologias)
        if (stackRepository.count() == 0) {
            System.out.println(">> Populando banco com Stacks iniciais...");

            List<String> nomesStacks = Arrays.asList(
                    "Java", "Spring Boot", "Python", "JavaScript", "TypeScript",
                    "React", "Angular", "Node.js", "PostgreSQL", "Docker",
                    "AWS", "Kotlin", "C#", ".NET", "Vue.js"
            );

            List<Stack> stacks = nomesStacks.stream()
                    .map(nome -> new Stack(null, nome))
                    .collect(Collectors.toList());

            stackRepository.saveAll(stacks);
        }

        // 2. Testando e Populando uma Vaga de Exemplo (Opcional)
        if (jobRepository.count() == 0) {
            System.out.println(">> Criando vaga de teste para validar relacionamentos...");

            // Busca as stacks que acabamos de criar para associar à vaga
            Stack java = stackRepository.findByNomeIgnoreCase("Java").orElse(null);
            Stack spring = stackRepository.findByNomeIgnoreCase("Spring Boot").orElse(null);

            Job vagaTeste = new Job();
            vagaTeste.setTitulo("Desenvolvedor Java Backend");
            vagaTeste.setEmpresa("Tech Solutions PDS");
            vagaTeste.setDescricao("Vaga de teste para validar o sistema de busca.");
            vagaTeste.setFonte("Sistema Local");
            vagaTeste.setLinkOriginal("http://localhost:8080");
            vagaTeste.setSenioridade(Senioridade.JUNIOR);
            vagaTeste.setDataColeta(LocalDateTime.now());

            if (java != null && spring != null) {
                vagaTeste.setStacksRequisitadas(Set.of(java, spring));
            }

            jobRepository.save(vagaTeste);
            System.out.println(">> Vaga de teste salva com sucesso!");
        }
    }
}