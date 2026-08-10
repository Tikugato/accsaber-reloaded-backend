package com.accsaber.backend.model.dto.response.supporter;

import java.time.Instant;

import com.accsaber.backend.model.entity.supporter.KofiEvent;
import com.accsaber.backend.model.entity.user.User;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KofiEventResponse {

    String kofiTransactionId;
    String type;
    String email;
    String fromName;
    Integer amountCents;
    String currency;
    String tierName;
    boolean subscription;
    boolean firstSubscription;
    Instant receivedAt;
    String claimedUserId;
    String claimedUserName;
    String claimedUserAvatarUrl;
    String claimedUserCdnAvatarUrl;
    Instant claimedAt;
    String claimSource;

    public static KofiEventResponse from(KofiEvent event) {
        KofiEventResponseBuilder b = KofiEventResponse.builder()
                .kofiTransactionId(event.getKofiTransactionId())
                .type(event.getType().name())
                .email(event.getEmail())
                .fromName(event.getFromName())
                .amountCents(event.getAmountCents())
                .currency(event.getCurrency())
                .tierName(event.getTierName())
                .subscription(event.isSubscription())
                .firstSubscription(event.isFirstSubscription())
                .receivedAt(event.getReceivedAt())
                .claimedAt(event.getClaimedAt());
        if (event.getClaimSource() != null) {
            b.claimSource(event.getClaimSource().name());
        }
        User claimed = event.getClaimedUser();
        if (claimed != null) {
            b.claimedUserId(String.valueOf(claimed.getId()))
                    .claimedUserName(claimed.getName())
                    .claimedUserAvatarUrl(claimed.getAvatarUrl())
                    .claimedUserCdnAvatarUrl(claimed.getCdnAvatarUrl());
        }
        return b.build();
    }
}
