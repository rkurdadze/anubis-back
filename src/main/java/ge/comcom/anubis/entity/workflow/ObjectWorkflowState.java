package ge.comcom.anubis.entity.workflow;

import ge.comcom.anubis.entity.core.ObjectEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "object_workflow_state")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ObjectWorkflowState {

    @Id
    @Column(name = "object_id")
    private Long objectId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "object_id")
    private ObjectEntity object;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id")
    private WorkflowState state;
}
