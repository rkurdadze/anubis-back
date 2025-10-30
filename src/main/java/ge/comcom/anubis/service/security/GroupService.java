package ge.comcom.anubis.service.security;

import ge.comcom.anubis.dto.GroupDto;
import ge.comcom.anubis.dto.GroupRequest;
import ge.comcom.anubis.entity.security.Group;
import ge.comcom.anubis.entity.security.User;
import ge.comcom.anubis.entity.security.UserGroup;
import ge.comcom.anubis.entity.security.UserGroupId;
import ge.comcom.anubis.repository.security.GroupRepository;
import ge.comcom.anubis.repository.security.UserGroupRepository;
import ge.comcom.anubis.repository.security.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;

    @Transactional(readOnly = true)
    public List<GroupDto> list() {
        return groupRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDto get(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found: id=" + id));
        return toDto(group);
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
        return toDto(saved);
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

        return toDto(updated);
    }

    public void delete(Long id) {
        if (!groupRepository.existsById(id)) {
            throw new EntityNotFoundException("Group not found: id=" + id);
        }
        userGroupRepository.deleteByIdGroupId(id);
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
            if (userGroupRepository.existsByIdUserIdAndIdGroupId(user.getId(), group.getId())) {
                continue;
            }
            UserGroup membership = UserGroup.builder()
                    .id(new UserGroupId(user.getId(), group.getId()))
                    .user(user)
                    .group(group)
                    .build();
            userGroupRepository.save(membership);
        }
    }

    private GroupDto toDto(Group group) {
        Set<Long> userIds = userGroupRepository.findByIdGroupId(group.getId()).stream()
                .map(UserGroup::getUser)
                .map(User::getId)
                .collect(Collectors.toCollection(HashSet::new));

        return GroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .memberIds(userIds)
                .build();
    }
}
