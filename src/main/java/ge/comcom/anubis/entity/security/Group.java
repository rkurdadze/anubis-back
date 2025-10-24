package ge.comcom.anubis.entity.security;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "\"group\"")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g. "Administrators", "Viewers"
}
