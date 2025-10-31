package ge.comcom.anubis.service.security;

import ge.comcom.anubis.dto.UserDto;
import ge.comcom.anubis.dto.UserRequest;
import ge.comcom.anubis.entity.security.Group;
import ge.comcom.anubis.entity.security.Role;
import ge.comcom.anubis.entity.security.User;
import ge.comcom.anubis.entity.security.UserGroup;
import ge.comcom.anubis.entity.security.UserGroupId;
import ge.comcom.anubis.entity.security.UserRole;
import ge.comcom.anubis.entity.security.UserRoleId;
import ge.comcom.anubis.enums.UserStatus;
import ge.comcom.anubis.mapper.security.UserMapper;
import ge.comcom.anubis.repository.security.GroupRepository;
import ge.comcom.anubis.repository.security.UserGroupRepository;
import ge.comcom.anubis.repository.security.UserRepository;
import ge.comcom.anubis.repository.security.UserRoleRepository;
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
public class UserService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "username"))
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDto get(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: id=" + id));
        return userMapper.toDto(user);
    }

    public UserDto create(UserRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        UserStatus status = request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE;

        User user = User.builder()
                .username(username)
                .fullName(request.getFullName())
                .passwordHash(request.getPasswordHash())
                .status(status)
                .build();

        User saved = userRepository.save(user);
        if (request.getGroupIds() != null) {
            syncGroups(saved, request.getGroupIds());
        }
        if (request.getRoleIds() != null) {
            syncRoles(saved, request.getRoleIds());
        }
        return userMapper.toDto(saved);
    }

    public UserDto update(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: id=" + id));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String username = request.getUsername().trim();
            userRepository.findByUsernameIgnoreCase(username)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Username already exists: " + username);
                    });
            user.setUsername(username);
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getPasswordHash() != null) {
            user.setPasswordHash(request.getPasswordHash());
        }

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        User updated = userRepository.save(user);

        if (request.getGroupIds() != null) {
            syncGroups(updated, request.getGroupIds());
        }

        if (request.getRoleIds() != null) {
            syncRoles(updated, request.getRoleIds());
        }

        return userMapper.toDto(updated);
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found: id=" + id);
        }
        userGroupRepository.deleteByIdUserId(id);
        userRoleRepository.deleteByIdUserId(id);
        userRepository.deleteById(id);
    }

    private void syncGroups(User user, Set<Long> groupIds) {
        Set<Long> targetIds = groupIds == null ? Set.of() : new HashSet<>(groupIds);

        List<Group> groups = targetIds.stream()
                .map(groupId -> groupRepository.findById(groupId)
                        .orElseThrow(() -> new EntityNotFoundException("Group not found: id=" + groupId)))
                .toList();

        userGroupRepository.deleteByIdUserId(user.getId());

        for (Group group : groups) {
            UserGroup membership = UserGroup.builder()
                    .id(new UserGroupId(user.getId(), group.getId()))
                    .user(user)
                    .group(group)
                    .build();
            userGroupRepository.save(membership);
        }
    }

    private void syncRoles(User user, Set<Long> roleIds) {
        Set<Long> targetIds = roleIds == null ? Set.of() : new HashSet<>(roleIds);

        List<Role> roles = targetIds.stream()
                .map(roleId -> roleRepository.findById(roleId)
                        .orElseThrow(() -> new EntityNotFoundException("Role not found: id=" + roleId)))
                .toList();

        userRoleRepository.deleteByIdUserId(user.getId());

        for (Role role : roles) {
            UserRole assignment = UserRole.builder()
                    .id(new UserRoleId(user.getId(), role.getId()))
                    .user(user)
                    .role(role)
                    .build();
            userRoleRepository.save(assignment);
        }
    }

}
