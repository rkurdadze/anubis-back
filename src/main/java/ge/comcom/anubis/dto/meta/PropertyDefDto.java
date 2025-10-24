package ge.comcom.anubis.dto.meta;

import ge.comcom.anubis.enums.PropertyDataType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Property Definition DTO")
public class PropertyDefDto {

    private Long id;
    private String name;
    private String captionI18n;
    private PropertyDataType dataType;
    private Long refObjectTypeId;
    private Long valueListId;
    private Boolean isMultiselect;
    private Boolean isRequired;
    private Boolean isUnique;
    private String regex;
    private String defaultValue;
    private String description;
}
