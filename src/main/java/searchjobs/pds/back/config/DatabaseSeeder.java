package searchjobs.pds.back.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
@Order(1)
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
                    // LINGUAGENS & BACKEND
                    "Java", "Spring Boot", "Python", "C#", ".NET", "Node.js",
                    "PHP", "Laravel", "Go", "Ruby", "Rails", "Rust", "Elixir", "Kotlin", "Scala",

                    // FRONTEND
                    "JavaScript", "TypeScript", "React", "Angular", "Vue.js",
                    "Next.js", "Svelte", "HTML", "CSS", "Tailwind", "Bootstrap",

                    // MOBILE
                    "Flutter", "React Native", "Swift", "Objective-C", "Android", "iOS", "Ionic",

                    // BANCO DE DADOS
                    "PostgreSQL", "MySQL", "Oracle", "SQL Server", "MongoDB",
                    "Redis", "Elasticsearch", "Cassandra", "Firebase", "SQL",

                    // CLOUD & DEVOPS
                    "AWS", "Azure", "Google Cloud", "GCP", "Docker", "Kubernetes",
                    "Terraform", "Jenkins", "Ansible", "CI/CD", "Linux",

                    // OUTROS / FERRAMENTAS
                    "Git", "Jira", "Scrum", "Agile", "GraphQL", "Rest API", "Microservices"
            );

            List<Stack> stacks = nomesStacks.stream()
                    .map(nome -> new Stack(null, nome))
                    .collect(Collectors.toList());

            stackRepository.saveAll(stacks);
        }

    }
}
