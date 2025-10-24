package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ObjectClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Long id;

    @Column(nullable = false)
    private String name; // e.g. "Contract", "Invoice", "License"

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_type_id", nullable = false)
    private ObjectType objectType;
}
