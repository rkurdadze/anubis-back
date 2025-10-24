package ge.comcom.anubis.entity.workflow;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workflow_state")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkflowState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "state_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(nullable = false)
    private String name; // e.g. "Draft", "Under Review", "Approved"

    @Column(name = "is_initial")
    private Boolean isInitial = false;

    @Column(name = "is_final")
    private Boolean isFinal = false;
}
