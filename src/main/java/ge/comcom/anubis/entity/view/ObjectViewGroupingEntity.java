package ge.comcom.anubis.entity.view;

import ge.comcom.anubis.entity.meta.PropertyDef;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

/**
 * Defines hierarchical grouping levels within a view.
 */
@Entity
@Table(name = "object_view_grouping")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ObjectViewGroupingId.class)
public class ObjectViewGroupingEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "view_id", nullable = false)
    @Comment("FK to object_view.")
    private ObjectViewEntity view;

    @Id
    @Column(name = "level", nullable = false)
    @Comment("Grouping level index (0..N).")
    private Integer level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_def_id", nullable = false)
    @Comment("Group-by property reference.")
    private PropertyDef propertyDef;
}
