package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.dto.ObjectVersionAuditDto;
import ge.comcom.anubis.service.core.ObjectVersionAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST контроллер для выдачи истории изменений версий объектов.
 */
@RestController
@RequestMapping("/api/v1/versions/{versionId}/audit")
@RequiredArgsConstructor
@Tag(name = "Object Version Audit", description = "Audit log for object version lifecycle")
public class ObjectVersionAuditController {

    private final ObjectVersionAuditService auditService;

    @GetMapping
    @Operation(
            summary = "Get audit log for version",
            description = "Returns chronological list of changes recorded for the given object version"
    )
    public List<ObjectVersionAuditDto> getAudit(
            @Parameter(description = "Version ID", example = "12")
            @PathVariable Long versionId) {

        return auditService.getAuditDtoByVersion(versionId);
    }
}
