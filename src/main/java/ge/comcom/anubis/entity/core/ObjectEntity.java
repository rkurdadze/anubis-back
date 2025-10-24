package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "object")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ObjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "object_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_type_id", nullable = false)
    private ObjectType objectType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private ObjectClass objectClass;

    @Column
    private String name; // Example: “Invoice_2025_001”
}
