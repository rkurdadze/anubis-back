package ge.comcom.anubis.service.security;

import ge.comcom.anubis.dto.RoleDto;
import ge.comcom.anubis.dto.RoleRequest;
import ge.comcom.anubis.entity.security.Role;
import ge.comcom.anubis.mapper.security.RoleMapper;
import ge.comcom.anubis.repository.security.GroupRoleRepository;
import ge.comcom.anubis.repository.security.RoleRepository;
import ge.comcom.anubis.repository.security.UserRoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final GroupRoleRepository groupRoleRepository;
    private final RoleMapper roleMapper;

    @Transactional(readOnly = true)
    public List<RoleDto> list() {
        return roleRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(roleMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleDto get(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: id=" + id));
        return roleMapper.toDto(role);
    }

    public RoleDto create(RoleRequest request) {
        String name = request.getName().trim();
        if (roleRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Role name already exists: " + name);
        }
        request.setName(name);
        request.setDescription(trimToNull(request.getDescription()));

        Role role = roleMapper.toEntity(request);
        role.setName(name);
        role.setDescription(request.getDescription());

        Role saved = roleRepository.save(role);
        return roleMapper.toDto(saved);
    }

    public RoleDto update(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: id=" + id));

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Role name cannot be blank");
            }
            if (Boolean.TRUE.equals(role.getSystem()) && !role.getName().equalsIgnoreCase(name)) {
                throw new IllegalStateException("Built-in roles cannot be renamed");
            }
            roleRepository.findByNameIgnoreCase(name)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Role name already exists: " + name);
                    });
            request.setName(name);
        }

        if (request.getDescription() != null) {
            request.setDescription(trimToNull(request.getDescription()));
        }

        if (request.getActive() != null && Boolean.TRUE.equals(role.getSystem()) && !request.getActive()) {
            throw new IllegalStateException("Built-in roles cannot be deactivated");
        }

        roleMapper.updateEntity(request, role);

        if (request.getName() != null) {
            role.setName(request.getName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getActive() != null) {
            role.setActive(request.getActive());
        }

        Role updated = roleRepository.save(role);
        return roleMapper.toDto(updated);
    }

    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: id=" + id));
        if (Boolean.TRUE.equals(role.getSystem())) {
            throw new IllegalStateException("Built-in roles cannot be deleted");
        }
        if (userRoleRepository.existsByIdRoleId(id) || groupRoleRepository.existsByIdRoleId(id)) {
            throw new IllegalStateException("Role is still assigned to users or groups");
        }
        roleRepository.delete(role);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
