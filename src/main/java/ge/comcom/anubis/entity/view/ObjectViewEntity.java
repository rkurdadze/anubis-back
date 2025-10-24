package ge.comcom.anubis.entity.view;

import ge.comcom.anubis.entity.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a saved search / virtual folder (like M-Files View).
 */
@Entity
@Table(name = "object_view")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectViewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_id")
    @Comment("Primary key. Example: 8001.")
    private Long id;

    @Column(name = "name", nullable = false)
    @Comment("View name. Example: 'My Active Documents'.")
    private String name;

    @Column(name = "is_common", nullable = false)
    @Comment("TRUE = shared (common), FALSE = private.")
    private Boolean isCommon = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @Comment("Creator user id reference.")
    private User createdBy;

    @Column(name = "filter_json", columnDefinition = "jsonb")
    @Comment("Stored JSON filter definition.")
    private String filterJson;

    @Column(name = "sort_order")
    @Comment("Ordering index.")
    private Integer sortOrder;

    @OneToMany(mappedBy = "view", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Comment("Grouping definitions for this view.")
    private List<ObjectViewGroupingEntity> groupings = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    @Comment("Creation timestamp.")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
