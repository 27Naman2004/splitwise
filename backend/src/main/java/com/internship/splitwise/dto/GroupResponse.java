package com.internship.splitwise.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class GroupResponse {
    private UUID id;
    private String name;
    private List<UserResponse> members;
    private LocalDateTime createdAt;
}
