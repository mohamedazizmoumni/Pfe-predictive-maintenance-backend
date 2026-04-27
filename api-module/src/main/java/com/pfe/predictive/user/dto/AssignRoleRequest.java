package com.pfe.predictive.user.dto;

import jakarta.validation.constraints.NotBlank;

public class AssignRoleRequest {
    @NotBlank(message = "Role name is required")
    private String roleName;

    public AssignRoleRequest() {}

    public AssignRoleRequest(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
