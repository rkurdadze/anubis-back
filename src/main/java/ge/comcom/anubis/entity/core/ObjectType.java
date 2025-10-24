package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "object_type")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ObjectType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "object_type_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g. "Document", "Customer", "Project"
}
