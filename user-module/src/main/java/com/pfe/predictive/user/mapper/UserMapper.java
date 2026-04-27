package com.pfe.predictive.user.mapper;

import com.pfe.predictive.user.dto.*;
import com.pfe.predictive.core.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UserMapper - Entity ↔ DTO conversions for User
 *
 * @author User Module
 * @version 1.0
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) return null;

        List<String> roles = user.getRolesList() != null
            ? Arrays.asList(user.getRolesList().split(","))
            : List.of();

        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .phoneNumber(user.getPhoneNumber())
            .department(user.getDepartment())
            .status(user.getStatus())
            .roles(roles)
            .mfaEnabled(user.getMfaEnabled())
            .lastLoginDate(user.getLastLoginDate())
            .createdDate(user.getCreatedDate())
            .lastModifiedDate(user.getLastModifiedDate())
            .displayName(user.getDisplayName())
            .locked(user.isLocked())
            .build();
    }

    public UserDto toDto(User user) {
        if (user == null) return null;

        List<String> roles = user.getRolesList() != null
            ? Arrays.asList(user.getRolesList().split(","))
            : List.of();

        return UserDto.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .displayName(user.getDisplayName())
            .status(user.getStatus())
            .roles(roles)
            .build();
    }

    
    public List<UserResponse> toResponseList(List<User> users) {
        if (users == null) return List.of();

        return users.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    
    public List<UserDto> toDtoList(List<User> users) {
        if (users == null) return List.of();

        return users.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

   
    public Page<UserResponse> toResponsePage(Page<User> page) {
        if (page == null) return Page.empty();

        List<UserResponse> content = page.getContent().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

   
    public Page<UserDto> toDtoPage(Page<User> page) {
        if (page == null) return Page.empty();

        List<UserDto> content = page.getContent().stream()
            .map(this::toDto)
            .collect(Collectors.toList());

        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

   
    public LoginResponse toLoginResponse(User user, String jwtToken, String refreshToken, long expiresIn, long refreshExpiresIn) {
        return LoginResponse.builder()
            .token(jwtToken)
            .refreshToken(refreshToken)
            .user(toResponse(user))
            .expiresIn(expiresIn)
            .refreshExpiresIn(refreshExpiresIn)
            .build();
    }
}
