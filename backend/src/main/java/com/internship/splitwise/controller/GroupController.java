package com.internship.splitwise.controller;

import com.internship.splitwise.dto.GroupRequest;
import com.internship.splitwise.dto.GroupResponse;
import com.internship.splitwise.dto.UserResponse;
import com.internship.splitwise.model.Group;
import com.internship.splitwise.model.GroupMember;
import com.internship.splitwise.model.User;
import com.internship.splitwise.repository.GroupMemberRepository;
import com.internship.splitwise.repository.GroupRepository;
import com.internship.splitwise.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getAllGroups() {
        List<Group> groups = groupRepository.findAll();
        List<GroupResponse> responses = groups.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody GroupRequest request) {
        Group group = Group.builder()
                .name(request.getName().trim())
                .createdAt(LocalDateTime.now())
                .build();
        final Group savedGroup = groupRepository.save(group);

        List<User> members = new ArrayList<>();
        if (request.getMemberUserIds() != null && !request.getMemberUserIds().isEmpty()) {
            for (UUID userId : request.getMemberUserIds()) {
                userRepository.findById(userId).ifPresent(user -> {
                    GroupMember member = GroupMember.builder()
                            .groupId(savedGroup.getId())
                            .userId(user.getId())
                            .joinedAt(LocalDateTime.now())
                            .build();
                    groupMemberRepository.save(member);
                    members.add(user);
                });
            }
        } else {
            // Add all existing users by default to avoid empty groups in mock development
            List<User> allUsers = userRepository.findAll();
            for (User user : allUsers) {
                GroupMember member = GroupMember.builder()
                        .groupId(savedGroup.getId())
                        .userId(user.getId())
                        .joinedAt(LocalDateTime.now())
                        .build();
                groupMemberRepository.save(member);
                members.add(user);
            }
        }

        GroupResponse response = mapToResponse(savedGroup);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    private GroupResponse mapToResponse(Group group) {
        List<GroupMember> members = groupMemberRepository.findByGroupId(group.getId());
        List<UserResponse> userResponses = members.stream()
                .map(m -> {
                    User user = m.getUser();
                    if (user == null) {
                        // Safe fallback if LAZY load does not resolve
                        user = userRepository.findById(m.getUserId()).orElse(null);
                    }
                    return user;
                })
                .filter(java.util.Objects::nonNull)
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .members(userResponses)
                .createdAt(group.getCreatedAt())
                .build();
    }
}
