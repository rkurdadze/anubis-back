package ge.comcom.anubis.entity.security;

import ge.comcom.anubis.entity.core.ObjectEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "acl")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Acl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "acl_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_id", nullable = false)
    private ObjectEntity object;

    @Column(name = "inherit_parent")
    private Boolean inheritParent = true;
}
