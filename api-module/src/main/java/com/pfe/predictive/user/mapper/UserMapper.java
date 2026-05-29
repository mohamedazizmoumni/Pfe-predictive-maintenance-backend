package com.pfe.predictive.user.mapper;

import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.user.dto.ProfileResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * UserMapper - Entity ↔ DTO conversions for User
 *
 * @author User Module
 * @version 1.0
 */
@Component
public class UserMapper {

    public ProfileResponse toProfileResponse(User user) {
        if (user == null) return null;

        List<String> roles = user.getRolesList() != null
            ? Arrays.asList(user.getRolesList().split(","))
            : new ArrayList<>();

        return ProfileResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .displayName(user.getDisplayName())
            .phoneNumber(user.getPhoneNumber())
            .department(user.getDepartment())
            .status(user.getStatus())
            .mfaEnabled(user.getMfaEnabled())
            .profilePictureUrl(user.getProfilePictureUrl())
            .roles(roles)
            .lastLoginDate(user.getLastLoginDate())
            .createdDate(user.getCreatedDate())
            .lastModifiedDate(user.getLastModifiedDate())
            .build();
    }
}
