package ge.comcom.anubis.dto;

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
    private boolean isActive;
    private FileStorageDto defaultStorage;
}
