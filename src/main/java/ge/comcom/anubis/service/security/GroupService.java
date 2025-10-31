package ge.comcom.anubis.service.security;

import ge.comcom.anubis.dto.GroupDto;
import ge.comcom.anubis.dto.GroupRequest;
import ge.comcom.anubis.entity.security.Group;
import ge.comcom.anubis.entity.security.GroupRole;
import ge.comcom.anubis.entity.security.GroupRoleId;
import ge.comcom.anubis.entity.security.Role;
import ge.comcom.anubis.entity.security.User;
import ge.comcom.anubis.entity.security.UserGroup;
import ge.comcom.anubis.entity.security.UserGroupId;
import ge.comcom.anubis.mapper.security.GroupMapper;
import ge.comcom.anubis.repository.security.GroupRepository;
import ge.comcom.anubis.repository.security.UserGroupRepository;
import ge.comcom.anubis.repository.security.UserRepository;
import ge.comcom.anubis.repository.security.GroupRoleRepository;
import ge.comcom.anubis.repository.security.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final RoleRepository roleRepository;
    private final GroupRoleRepository groupRoleRepository;
    private final GroupMapper groupMapper;

    @Transactional(readOnly = true)
    public List<GroupDto> list() {
        return groupRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(groupMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDto get(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found: id=" + id));
        return groupMapper.toDto(group);
    }

    public GroupDto create(GroupRequest request) {
        String name = request.getName().trim();
        if (groupRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Group name already exists: " + name);
        }

        Group group = Group.builder()
                .name(name)
                .build();

        Group saved = groupRepository.save(group);
        if (request.getMemberIds() != null) {
            syncMembers(saved, request.getMemberIds());
        }
        if (request.getRoleIds() != null) {
            syncRoles(saved, request.getRoleIds());
        }
        return groupMapper.toDto(saved);
    }

    public GroupDto update(Long id, GroupRequest request) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found: id=" + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            String name = request.getName().trim();
            groupRepository.findByNameIgnoreCase(name)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Group name already exists: " + name);
                    });
            group.setName(name);
        }

        Group updated = groupRepository.save(group);

        if (request.getMemberIds() != null) {
            syncMembers(updated, request.getMemberIds());
        }

        if (request.getRoleIds() != null) {
            syncRoles(updated, request.getRoleIds());
        }

        return groupMapper.toDto(updated);
    }

    public void delete(Long id) {
        if (!groupRepository.existsById(id)) {
            throw new EntityNotFoundException("Group not found: id=" + id);
        }
        userGroupRepository.deleteByIdGroupId(id);
        groupRoleRepository.deleteByIdGroupId(id);
        groupRepository.deleteById(id);
    }

    private void syncMembers(Group group, Set<Long> memberIds) {
        Set<Long> targetIds = memberIds == null ? Set.of() : new HashSet<>(memberIds);

        List<User> users = targetIds.stream()
                .map(userId -> userRepository.findById(userId)
                        .orElseThrow(() -> new EntityNotFoundException("User not found: id=" + userId)))
                .toList();

        userGroupRepository.deleteByIdGroupId(group.getId());

        for (User user : users) {
            UserGroup membership = UserGroup.builder()
                    .id(new UserGroupId(user.getId(), group.getId()))
                    .user(user)
                    .group(group)
                    .build();
            userGroupRepository.save(membership);
        }
    }

    private void syncRoles(Group group, Set<Long> roleIds) {
        Set<Long> targetIds = roleIds == null ? Set.of() : new HashSet<>(roleIds);

        List<Role> roles = targetIds.stream()
                .map(roleId -> roleRepository.findById(roleId)
                        .orElseThrow(() -> new EntityNotFoundException("Role not found: id=" + roleId)))
                .toList();

        groupRoleRepository.deleteByIdGroupId(group.getId());

        for (Role role : roles) {
            GroupRole assignment = GroupRole.builder()
                    .id(new GroupRoleId(group.getId(), role.getId()))
                    .group(group)
                    .role(role)
                    .build();
            groupRoleRepository.save(assignment);
        }
    }

}
