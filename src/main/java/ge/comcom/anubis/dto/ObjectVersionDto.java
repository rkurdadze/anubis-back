package ge.comcom.anubis.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectVersionDto {
    private Long id;
    private Long objectId;
    private Integer versionNum;
    private String comment;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private Boolean singleFile;
}
