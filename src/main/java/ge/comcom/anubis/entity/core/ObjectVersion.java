package ge.comcom.anubis.entity.core;

import ge.comcom.anubis.entity.security.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "object_version")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ObjectVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_id", nullable = false)
    private ObjectEntity object;

    @Column(name = "version_num", nullable = false)
    private Integer versionNum;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column
    private String comment;

    @Column(name = "single_file")
    private Boolean singleFile = true;
}
