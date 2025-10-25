package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.entity.core.VaultEntity;
import ge.comcom.anubis.repository.core.VaultRepository;
import ge.comcom.anubis.service.storage.VaultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing {@link VaultEntity}.
 * <p>
 * Each vault represents a logical repository grouping.
 * Vaults can be linked to their own default {@code FileStorageEntity} (DB, FS, or S3).
 * This allows separation of data and custom storage configuration per vault.
 */
@RestController
@RequestMapping("/api/v1/vaults")
@RequiredArgsConstructor
@Tag(name = "Vaults", description = "Vault management and storage configuration endpoints")
public class VaultController {

    private final VaultRepository vaultRepository;
    private final VaultService vaultService;

    // ================================================================
    // Get all active vaults
    // ================================================================
    @Operation(
            summary = "Get all active vaults",
            description = "Returns all vaults where is_active = true.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of active vaults",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = VaultEntity.class)
                            )
                    )
            }
    )
    @GetMapping("/active")
    public List<VaultEntity> getActiveVaults() {
        return vaultRepository.findByIsActiveTrue();
    }

    // ================================================================
    // Get vault by ID
    // ================================================================
    @Operation(summary = "Get vault by ID")
    @GetMapping("/{id}")
    public ResponseEntity<VaultEntity> getVaultById(@PathVariable Long id) {
        return vaultRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ================================================================
    // Get vault by code
    // ================================================================
    @Operation(summary = "Get vault by unique code")
    @GetMapping("/code/{code}")
    public ResponseEntity<VaultEntity> getVaultByCode(@PathVariable String code) {
        try {
            VaultEntity vault = vaultService.getVaultByCode(code);
            return ResponseEntity.ok(vault);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ================================================================
    // Create new vault
    // ================================================================
    @Operation(summary = "Create a new vault")
    @PostMapping
    public ResponseEntity<VaultEntity> createVault(@RequestBody VaultEntity vault) {
        VaultEntity saved = vaultRepository.save(vault);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ================================================================
    // Update existing vault
    // ================================================================
    @Operation(summary = "Update an existing vault")
    @PutMapping("/{id}")
    public ResponseEntity<VaultEntity> updateVault(
            @PathVariable Long id,
            @RequestBody VaultEntity updatedVault) {

        return vaultRepository.findById(id)
                .map(existing -> {
                    existing.setName(updatedVault.getName());
                    existing.setDescription(updatedVault.getDescription());
                    existing.setCode(updatedVault.getCode());
                    existing.setActive(updatedVault.isActive());
                    existing.setDefaultStorage(updatedVault.getDefaultStorage());
                    return ResponseEntity.ok(vaultRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ================================================================
    // Delete vault
    // ================================================================
    @Operation(summary = "Delete a vault by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVault(@PathVariable Long id) {
        if (!vaultRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        vaultRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
