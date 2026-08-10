package com.accsaber.backend.model.entity.campaign;

import java.util.List;

public enum UserCampaignStatus {
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    ABANDONED("abandoned");

    public static final List<UserCampaignStatus> PARTICIPATING = List.of(IN_PROGRESS, COMPLETED);

    private final String dbValue;

    UserCampaignStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static UserCampaignStatus fromDbValue(String value) {
        for (UserCampaignStatus s : values()) {
            if (s.dbValue.equals(value))
                return s;
        }
        throw new IllegalArgumentException("Unknown user campaign status: " + value);
    }
}
