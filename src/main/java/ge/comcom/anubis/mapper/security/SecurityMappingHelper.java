package ge.comcom.anubis.mapper.security;

import ge.comcom.anubis.dto.RoleSummaryDto;
import ge.comcom.anubis.dto.SecurityPrincipalDto;
import ge.comcom.anubis.entity.security.*;
import ge.comcom.anubis.enums.GranteeType;
import ge.comcom.anubis.repository.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SecurityMappingHelper {

    private final UserGroupRepository userGroupRepository;
    private final AclEntryRepository aclEntryRepository;
    private final UserRoleRepository userRoleRepository;
    private final GroupRoleRepository groupRoleRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;

    public Set<Long> loadGroupIdsForUser(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        return userGroupRepository.findByIdUserId(userId).stream()
                .map(UserGroup::getGroup)
                .map(Group::getId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public Set<Long> loadMemberIdsForGroup(Long groupId) {
        if (groupId == null) {
            return Set.of();
        }
        return userGroupRepository.findByIdGroupId(groupId).stream()
                .map(UserGroup::getUser)
                .map(User::getId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public List<AclEntry> loadAclEntries(Long aclId) {
        if (aclId == null) {
            return List.of();
        }
        return aclEntryRepository.findByAcl_Id(aclId);
    }

    public Set<Long> loadRoleIdsForUser(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        return userRoleRepository.findByIdUserId(userId).stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .map(Role::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Long> loadRoleIdsForGroup(Long groupId) {
        if (groupId == null) {
            return Set.of();
        }
        return groupRoleRepository.findByIdGroupId(groupId).stream()
                .map(GroupRole::getRole)
                .filter(Objects::nonNull)
                .map(Role::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Long> loadEffectiveRoleIdsForUser(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        Set<Long> effective = new LinkedHashSet<>(loadRoleIdsForUser(userId));
        loadGroupIdsForUser(userId).forEach(groupId -> effective.addAll(loadRoleIdsForGroup(groupId)));
        return effective;
    }

    public SecurityPrincipalDto resolvePrincipal(GranteeType type, Long granteeId) {
        if (type == null || granteeId == null) {
            return null;
        }
        return switch (type) {
            case USER -> userRepository.findById(granteeId)
                    .map(user -> buildUserPrincipal(user))
                    .orElseGet(() -> missingPrincipal(type, granteeId));
            case GROUP -> groupRepository.findById(granteeId)
                    .map(group -> buildGroupPrincipal(group))
                    .orElseGet(() -> missingPrincipal(type, granteeId));
            case ROLE -> roleRepository.findById(granteeId)
                    .map(role -> buildRolePrincipal(role))
                    .orElseGet(() -> missingPrincipal(type, granteeId));
        };
    }

    private SecurityPrincipalDto buildUserPrincipal(User user) {
        Set<Long> groups = loadGroupIdsForUser(user.getId());
        Set<Long> directRoles = loadRoleIdsForUser(user.getId());
        Set<Long> effectiveRoles = loadEffectiveRoleIdsForUser(user.getId());
        return SecurityPrincipalDto.builder()
                .id(user.getId())
                .type(GranteeType.USER)
                .displayName(user.getFullName() != null ? user.getFullName() : user.getUsername())
                .login(user.getUsername())
                .status(user.getStatus())
                .groupIds(groups)
                .directRoleIds(directRoles)
                .effectiveRoleIds(effectiveRoles)
                .directRoles(toRoleSummaries(directRoles))
                .effectiveRoles(toRoleSummaries(effectiveRoles))
                .build();
    }

    private SecurityPrincipalDto buildGroupPrincipal(Group group) {
        Set<Long> members = loadMemberIdsForGroup(group.getId());
        Set<Long> roles = loadRoleIdsForGroup(group.getId());
        return SecurityPrincipalDto.builder()
                .id(group.getId())
                .type(GranteeType.GROUP)
                .displayName(group.getName())
                .memberIds(members)
                .directRoleIds(roles)
                .effectiveRoleIds(roles)
                .directRoles(toRoleSummaries(roles))
                .effectiveRoles(toRoleSummaries(roles))
                .build();
    }

    private SecurityPrincipalDto buildRolePrincipal(Role role) {
        Set<Long> roleIds = Set.of(role.getId());
        Set<RoleSummaryDto> summaries = toRoleSummaries(roleIds);
        return SecurityPrincipalDto.builder()
                .id(role.getId())
                .type(GranteeType.ROLE)
                .displayName(role.getName())
                .description(role.getDescription())
                .directRoleIds(roleIds)
                .effectiveRoleIds(roleIds)
                .directRoles(summaries)
                .effectiveRoles(summaries)
                .build();
    }

    private SecurityPrincipalDto missingPrincipal(GranteeType type, Long id) {
        return SecurityPrincipalDto.builder()
                .id(id)
                .type(type)
                .displayName("[missing " + type + " " + id + "]")
                .build();
    }

    private Set<RoleSummaryDto> toRoleSummaries(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return roleRepository.findByIdIn(ids).stream()
                .map(role -> RoleSummaryDto.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .active(role.getActive())
                        .build())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
