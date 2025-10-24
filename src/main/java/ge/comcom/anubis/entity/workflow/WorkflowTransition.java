package ge.comcom.anubis.entity.workflow;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workflow_transition")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkflowTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transition_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_state_id", nullable = false)
    private WorkflowState fromState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_state_id", nullable = false)
    private WorkflowState toState;

    private String name; // e.g. "Approve", "Reject"

    @Column(name = "condition_json")
    private String conditionJson; // e.g. {"role":"manager"}
}
