package ge.comcom.anubis.dto;

import ge.comcom.anubis.enums.VersionChangeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * DTO для выдачи записей аудита версий наружу через REST.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Audit record for object version changes")
public class ObjectVersionAuditDto {

    @Schema(description = "Unique audit record ID", example = "42")
    private Long id;

    @Schema(description = "Object version ID", example = "101")
    private Long versionId;

    @Schema(description = "Type of change recorded")
    private VersionChangeType changeType;

    @Schema(description = "User ID that performed the change", example = "7")
    private Long modifiedBy;

    @Schema(description = "When the change happened")
    private Instant modifiedAt;

    @Schema(description = "Human readable summary of the change")
    private String changeSummary;

    @Schema(description = "Field affected by the change, if tracked")
    private String fieldChanged;

    @Schema(description = "Previous value of the field")
    private String oldValue;

    @Schema(description = "New value of the field")
    private String newValue;
}
