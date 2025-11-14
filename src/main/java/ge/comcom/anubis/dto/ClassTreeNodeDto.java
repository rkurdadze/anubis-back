package ge.comcom.anubis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Иерархический узел класса ObjectClass")
public class ClassTreeNodeDto {

    @Schema(description = "Идентификатор класса", example = "42")
    private Long id;

    @Schema(description = "Имя класса", example = "Договор")
    private String name;

    @Schema(description = "Описание", example = "Базовый класс документов")
    private String description;

    @Schema(description = "Активен ли класс", example = "true")
    private Boolean isActive;

    @Schema(description = "Связанный ObjectType", example = "5")
    private Long objectTypeId;

    @Schema(description = "ACL идентификатор", example = "2")
    private Long aclId;

    @Schema(description = "Родительский класс", example = "10")
    private Long parentClassId;

    @Builder.Default
    @Schema(description = "Потомки класса")
    private List<ClassTreeNodeDto> children = new ArrayList<>();
}
