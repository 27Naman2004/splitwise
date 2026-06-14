package com.internship.splitwise.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class GroupRequest {

    @NotBlank(message = "Group name is required")
    private String name;

    private List<UUID> memberUserIds;
}
