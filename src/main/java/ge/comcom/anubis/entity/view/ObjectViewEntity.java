package ge.comcom.anubis.entity.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import ge.comcom.anubis.entity.security.User;
import ge.comcom.anubis.entity.view.ObjectViewGroupingEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    private String name;

    @Column(name = "is_common", nullable = false)
    private Boolean isCommon = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Type(JsonBinaryType.class) // ✅ правильный способ для Hibernate 6
    @Column(name = "filter_json", columnDefinition = "jsonb")
    @Comment("Stored JSON filter definition.")
    private JsonNode filterJson;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @OneToMany(mappedBy = "view", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ObjectViewGroupingEntity> groupings = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
