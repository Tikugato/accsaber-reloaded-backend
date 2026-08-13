package com.accsaber.backend.service.score;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.accsaber.backend.model.dto.request.score.SubmitScoreRequest;
import com.accsaber.backend.model.entity.score.Score;

class ScorePayloadFieldsTest {

    private static Score row(Integer streak115, Integer bombHits) {
        return Score.builder().streak115(streak115).bombHits(bombHits).build();
    }

    private static SubmitScoreRequest incoming(Integer streak115, Integer bombHits) {
        SubmitScoreRequest request = new SubmitScoreRequest();
        request.setStreak115(streak115);
        request.setBombHits(bombHits);
        return request;
    }

    @Nested
    @DisplayName("merge streak115")
    class MergeStreak115 {

        @Test
        void takesTheHigherStreakWhenBeatLeaderArrivesAfterThePlugin() {
            Score existing = row(3, null);

            assertThat(ScorePayloadFields.merge(existing, incoming(5, null))).isTrue();
            assertThat(existing.getStreak115()).isEqualTo(5);
        }

        @Test
        void keepsTheHigherStreakWhenTheLaterSourceUndercounts() {
            Score existing = row(5, null);

            assertThat(ScorePayloadFields.merge(existing, incoming(3, null))).isFalse();
            assertThat(existing.getStreak115()).isEqualTo(5);
        }

        @Test
        void fillsAMissingStreak() {
            Score existing = row(null, null);

            assertThat(ScorePayloadFields.merge(existing, incoming(4, null))).isTrue();
            assertThat(existing.getStreak115()).isEqualTo(4);
        }

        @Test
        void ignoresAMissingIncomingStreak() {
            Score existing = row(4, null);

            assertThat(ScorePayloadFields.merge(existing, incoming(null, null))).isFalse();
            assertThat(existing.getStreak115()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("merge other fields")
    class MergeOtherFields {

        @Test
        void leavesAnAlreadyPopulatedFieldAlone() {
            Score existing = row(null, 2);

            assertThat(ScorePayloadFields.merge(existing, incoming(null, 7))).isFalse();
            assertThat(existing.getBombHits()).isEqualTo(2);
        }

        @Test
        void stillFillsNulls() {
            Score existing = row(null, null);

            assertThat(ScorePayloadFields.merge(existing, incoming(null, 7))).isTrue();
            assertThat(existing.getBombHits()).isEqualTo(7);
        }
    }
}
