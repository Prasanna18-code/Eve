package com.collegeeventhub.admin.dto;

import com.collegeeventhub.common.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserRoleRequest {

    @NotNull
    private Role role;
}
