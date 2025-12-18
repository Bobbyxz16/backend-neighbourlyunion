package com.example.neighborhelp.dto.AdminDto;

import com.example.neighborhelp.entity.User.Role;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for updating user role (admin only operation)
 */
@Data
public class UpdateRoleRequest {

    @NotNull(message = "Role is required")
    private Role role;
}
