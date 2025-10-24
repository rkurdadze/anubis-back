package ge.comcom.anubis.entity.workflow;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workflow")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workflow_id")
    private Long id;

    @Column(nullable = false)
    private String name; // e.g. "Document Approval"
}
