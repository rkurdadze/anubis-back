package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "value_list")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ValueList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "value_list_id")
    private Long id;

    @Column(nullable = false)
    private String name; // e.g. "Status List"
}
