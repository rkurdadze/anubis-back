package ge.comcom.anubis.mapper.security;

import ge.comcom.anubis.entity.security.AclEntry;
import ge.comcom.anubis.entity.security.Group;
import ge.comcom.anubis.entity.security.User;
import ge.comcom.anubis.entity.security.UserGroup;
import ge.comcom.anubis.repository.security.AclEntryRepository;
import ge.comcom.anubis.repository.security.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SecurityMappingHelper {

    private final UserGroupRepository userGroupRepository;
    private final AclEntryRepository aclEntryRepository;

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
}
