package searchjobs.pds.back.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String empresa;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(unique = true)
    private String linkOriginal;
    private String fonte;

    @Enumerated(EnumType.STRING)
    private Senioridade senioridade;

    private LocalDateTime dataColeta = LocalDateTime.now();

    private boolean ativo = true;

    @ManyToMany
    @JoinTable(
            name = "job_stacks",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "stack_id")
    )
    private Set<Stack> stacksRequisitadas = new HashSet<>();
}