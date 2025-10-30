package ge.comcom.anubis.service.security;

import ge.comcom.anubis.dto.AclDto;
import ge.comcom.anubis.dto.AclEntryDto;
import ge.comcom.anubis.dto.AclEntryRequest;
import ge.comcom.anubis.dto.AclRequest;
import ge.comcom.anubis.entity.security.Acl;
import ge.comcom.anubis.entity.security.AclEntry;
import ge.comcom.anubis.enums.GranteeType;
import ge.comcom.anubis.repository.security.AclEntryRepository;
import ge.comcom.anubis.repository.security.AclRepository;
import ge.comcom.anubis.repository.security.GroupRepository;
import ge.comcom.anubis.repository.security.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AclManagementService {

    private final AclRepository aclRepository;
    private final AclEntryRepository aclEntryRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Transactional(readOnly = true)
    public List<AclDto> list() {
        return aclRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(this::toDtoWithEntries)
                .toList();
    }

    @Transactional(readOnly = true)
    public AclDto get(Long id) {
        Acl acl = aclRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ACL not found: id=" + id));
        return toDtoWithEntries(acl);
    }

    public AclDto create(AclRequest request) {
        String name = request.getName().trim();
        if (aclRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("ACL name already exists: " + name);
        }

        Acl acl = Acl.builder()
                .name(name)
                .build();

        Acl saved = aclRepository.save(acl);
        return toDto(saved);
    }

    public AclDto update(Long id, AclRequest request) {
        Acl acl = aclRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ACL not found: id=" + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            String name = request.getName().trim();
            aclRepository.findByNameIgnoreCase(name)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("ACL name already exists: " + name);
                    });
            acl.setName(name);
        }

        Acl updated = aclRepository.save(acl);
        return toDto(updated);
    }

    public void delete(Long id) {
        if (!aclRepository.existsById(id)) {
            throw new EntityNotFoundException("ACL not found: id=" + id);
        }
        aclEntryRepository.deleteAll(aclEntryRepository.findByAcl_Id(id));
        aclRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<AclEntryDto> listEntries(Long aclId) {
        ensureAclExists(aclId);
        return aclEntryRepository.findByAcl_Id(aclId).stream()
                .map(this::toEntryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AclEntryDto getEntry(Long aclId, Long entryId) {
        AclEntry entry = aclEntryRepository.findByIdAndAcl_Id(entryId, aclId)
                .orElseThrow(() -> new EntityNotFoundException("ACL entry not found: id=" + entryId));
        return toEntryDto(entry);
    }

    public AclEntryDto createEntry(Long aclId, AclEntryRequest request) {
        Acl acl = aclRepository.findById(aclId)
                .orElseThrow(() -> new EntityNotFoundException("ACL not found: id=" + aclId));

        validateGrantee(request.getGranteeType(), request.getGranteeId());

        if (aclEntryRepository.existsByAcl_IdAndGranteeTypeAndGranteeId(aclId, request.getGranteeType(), request.getGranteeId())) {
            throw new IllegalArgumentException("ACL entry already exists for this grantee");
        }

        AclEntry entry = AclEntry.builder()
                .acl(acl)
                .granteeType(request.getGranteeType())
                .granteeId(request.getGranteeId())
                .canRead(request.getCanRead() != null ? request.getCanRead() : Boolean.FALSE)
                .canWrite(request.getCanWrite() != null ? request.getCanWrite() : Boolean.FALSE)
                .canDelete(request.getCanDelete() != null ? request.getCanDelete() : Boolean.FALSE)
                .canChangeAcl(request.getCanChangeAcl() != null ? request.getCanChangeAcl() : Boolean.FALSE)
                .build();

        AclEntry saved = aclEntryRepository.save(entry);
        return toEntryDto(saved);
    }

    public AclEntryDto updateEntry(Long aclId, Long entryId, AclEntryRequest request) {
        AclEntry entry = aclEntryRepository.findByIdAndAcl_Id(entryId, aclId)
                .orElseThrow(() -> new EntityNotFoundException("ACL entry not found: id=" + entryId));

        if (request.getGranteeType() != null && request.getGranteeId() != null) {
            validateGrantee(request.getGranteeType(), request.getGranteeId());

            if (aclEntryRepository.existsByAcl_IdAndGranteeTypeAndGranteeId(aclId, request.getGranteeType(), request.getGranteeId())
                    && (!entry.getGranteeType().equals(request.getGranteeType())
                    || !entry.getGranteeId().equals(request.getGranteeId()))) {
                throw new IllegalArgumentException("ACL entry already exists for this grantee");
            }

            entry.setGranteeType(request.getGranteeType());
            entry.setGranteeId(request.getGranteeId());
        }

        if (request.getCanRead() != null) {
            entry.setCanRead(request.getCanRead());
        }
        if (request.getCanWrite() != null) {
            entry.setCanWrite(request.getCanWrite());
        }
        if (request.getCanDelete() != null) {
            entry.setCanDelete(request.getCanDelete());
        }
        if (request.getCanChangeAcl() != null) {
            entry.setCanChangeAcl(request.getCanChangeAcl());
        }

        AclEntry saved = aclEntryRepository.save(entry);
        return toEntryDto(saved);
    }

    public void deleteEntry(Long aclId, Long entryId) {
        AclEntry entry = aclEntryRepository.findByIdAndAcl_Id(entryId, aclId)
                .orElseThrow(() -> new EntityNotFoundException("ACL entry not found: id=" + entryId));
        aclEntryRepository.delete(entry);
    }

    private void validateGrantee(GranteeType type, Long granteeId) {
        if (type == GranteeType.USER) {
            userRepository.findById(granteeId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found: id=" + granteeId));
        } else if (type == GranteeType.GROUP) {
            groupRepository.findById(granteeId)
                    .orElseThrow(() -> new EntityNotFoundException("Group not found: id=" + granteeId));
        } else {
            throw new IllegalArgumentException("Unsupported grantee type: " + type);
        }
    }

    private AclDto toDto(Acl acl) {
        return AclDto.builder()
                .id(acl.getId())
                .name(acl.getName())
                .entries(List.of())
                .build();
    }

    private AclDto toDtoWithEntries(Acl acl) {
        List<AclEntryDto> entries = aclEntryRepository.findByAcl_Id(acl.getId()).stream()
                .map(this::toEntryDto)
                .toList();
        return AclDto.builder()
                .id(acl.getId())
                .name(acl.getName())
                .entries(entries)
                .build();
    }

    private AclEntryDto toEntryDto(AclEntry entry) {
        return AclEntryDto.builder()
                .id(entry.getId())
                .aclId(entry.getAcl().getId())
                .granteeType(entry.getGranteeType())
                .granteeId(entry.getGranteeId())
                .canRead(entry.getCanRead())
                .canWrite(entry.getCanWrite())
                .canDelete(entry.getCanDelete())
                .canChangeAcl(entry.getCanChangeAcl())
                .build();
    }

    private void ensureAclExists(Long aclId) {
        if (!aclRepository.existsById(aclId)) {
            throw new EntityNotFoundException("ACL not found: id=" + aclId);
        }
    }
}
