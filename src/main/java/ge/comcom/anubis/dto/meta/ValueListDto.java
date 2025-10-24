package ge.comcom.anubis.dto.meta;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for ValueList")
public class ValueListDto {
    private Long id;
    private String name;
    private Map<String, String> nameI18n;
}

