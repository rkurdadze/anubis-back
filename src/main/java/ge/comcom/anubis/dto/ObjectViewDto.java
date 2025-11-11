package ge.comcom.anubis.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Object for {@link ge.comcom.anubis.entity.view.ObjectViewEntity}.
 * <p>
 * Used for creating, updating, and returning saved views (virtual folders) in REST API.
 * Mirrors the structure of {@code ObjectViewEntity}, but uses scalar and simplified
 * references (e.g., {@code createdById} instead of {@code User} entity).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectViewDto {

    /** Primary key. Example: 8001. */
    private Long id;

    /** View name (e.g., “Active Licenses”, “By Organization”). */
    private String name;

    /** Whether the view is shared (true = common/public). */
    private Boolean isCommon;

    /** ID of the user who created this view. */
    private Long createdById;

    /** JSON-based filter definition (property and link filters). */
    private JsonNode filterJson;

    /** Sort order (optional field for UI ordering). */
    private Integer sortOrder;

    /** Optional grouping configuration (hierarchical display in UI). */
    private List<ViewGroupingDto> groupings;

    /** Timestamp when the view was created. */
    private Instant createdAt;
}
