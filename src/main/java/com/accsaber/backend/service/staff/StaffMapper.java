package com.accsaber.backend.service.staff;

import com.accsaber.backend.model.dto.response.staff.PublicStaffUserResponse;
import com.accsaber.backend.model.entity.staff.StaffUser;
import com.accsaber.backend.model.entity.user.User;

public final class StaffMapper {

    private StaffMapper() {
    }

    public static PublicStaffUserResponse toPublicResponse(StaffUser staffUser) {
        if (staffUser == null) {
            return null;
        }
        User user = staffUser.getUser();
        return PublicStaffUserResponse.builder()
                .id(staffUser.getId())
                .username(staffUser.getUsername())
                .role(staffUser.getRole())
                .userId(user != null ? String.valueOf(user.getId()) : null)
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .cdnAvatarUrl(user != null ? user.getCdnAvatarUrl() : null)
                .active(staffUser.isActive())
                .build();
    }
}
