package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "object_version")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_id", nullable = false)
    private Long objectId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "comment")
    private String comment;
}
