package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;

/**
 * Logical repository grouping (vault).
 * Each vault can define its own default file storage backend (DB / FS / S3).
 * Matches the schema defined in the extended baseline migration.
 */
@Entity
@Table(name = "vault")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaultEntity {

    /** Primary key (vault_id) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vault_id")
    private Long id;

    /** Unique vault code (e.g. "finance") */
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    /** Display name (e.g. "Finance Vault") */
    @Column(name = "name", nullable = false)
    private String name;

    /** Optional description */
    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_storage_id")
    private FileStorageEntity defaultStorage;

    /** Active flag */
    @Column(name = "is_active")
    private boolean active = true;
}
