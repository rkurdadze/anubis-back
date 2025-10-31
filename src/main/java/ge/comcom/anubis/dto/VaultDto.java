package ge.comcom.anubis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO представление логического хранилища (vault).
 */
@Data
public class VaultDto {

    private Long id;
    private String code;
    private String name;
    private String description;

    @JsonProperty("isActive")
    private Boolean active;

    private FileStorageDto defaultStorage;
}
