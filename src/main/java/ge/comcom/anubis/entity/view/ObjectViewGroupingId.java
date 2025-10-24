package ge.comcom.anubis.entity.view;

import lombok.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for ObjectViewGroupingEntity (view_id + level).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectViewGroupingId implements Serializable {

    private Long view;   // corresponds to view_id
    private Integer level;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObjectViewGroupingId that)) return false;
        return Objects.equals(view, that.view) && Objects.equals(level, that.level);
    }

    @Override
    public int hashCode() {
        return Objects.hash(view, level);
    }
}
