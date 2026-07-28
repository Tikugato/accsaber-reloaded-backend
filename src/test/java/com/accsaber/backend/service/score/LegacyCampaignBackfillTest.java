package com.accsaber.backend.service.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.accsaber.backend.client.BeatLeaderClient;
import com.accsaber.backend.client.ScoreSaberClient;
import com.accsaber.backend.model.dto.platform.beatleader.BeatLeaderScoreResponse;
import com.accsaber.backend.exception.ValidationException;
import com.accsaber.backend.model.entity.campaign.Campaign;
import com.accsaber.backend.model.entity.campaign.CampaignDifficulty;
import com.accsaber.backend.model.entity.campaign.UserCampaignStatus;
import com.accsaber.backend.model.entity.map.Difficulty;
import com.accsaber.backend.model.entity.map.MapDifficulty;
import com.accsaber.backend.model.entity.map.MapDifficultyStatus;
import com.accsaber.backend.repository.campaign.CampaignDifficultyRepository;
import com.accsaber.backend.repository.campaign.CampaignRepository;
import com.accsaber.backend.repository.campaign.UserCampaignRepository;
import com.accsaber.backend.repository.score.ScoreRepository;
import com.accsaber.backend.service.campaign.CampaignEvaluationService;
import com.accsaber.backend.service.infra.ModifierCacheService;
import com.accsaber.backend.service.player.DuplicateUserService;

@ExtendWith(MockitoExtension.class)
class LegacyCampaignBackfillTest {

    private static final Long USER = 76561198087536397L;
    private static final UUID CAMPAIGN = UUID.randomUUID();

    @Mock
    private DuplicateUserService duplicateUserService;
    @Mock
    private ModifierCacheService modifierCacheService;
    @Mock
    private CampaignDifficultyRepository campaignDifficultyRepository;
    @Mock
    private ScoreRepository scoreRepository;
    @Mock
    private BeatLeaderClient beatLeaderClient;
    @Mock
    private ScoreSaberClient scoreSaberClient;
    @Mock
    private ScoreService scoreService;
    @Mock
    private CampaignEvaluationService campaignEvaluationService;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private UserCampaignRepository userCampaignRepository;

    @InjectMocks
    private ScoreImportService scoreImportService;

    @BeforeEach
    void wireExecutor() {
        ReflectionTestUtils.setField(scoreImportService, "backfillExecutor", (Executor) Runnable::run);
    }

    @Test
    void backfillsUnrankedMapsQueryingBothPlatformsThenSettles() {
        MapDifficulty ranked = md(MapDifficultyStatus.RANKED, "bl-ranked", "ss-ranked");
        MapDifficulty alreadyHas = md(MapDifficultyStatus.CAMPAIGN, "bl-has", "ss-has");
        MapDifficulty toFetch = md(MapDifficultyStatus.CAMPAIGN, "bl-fetch", "ss-fetch");
        MapDifficulty barrierMap = md(MapDifficultyStatus.CAMPAIGN, "bl-barrier", "ss-barrier");

        when(duplicateUserService.resolvePrimaryUserId(USER)).thenReturn(USER);
        when(modifierCacheService.getModifierCodeToId()).thenReturn(Map.of());
        when(campaignDifficultyRepository.findActiveWithMapByCampaignId(CAMPAIGN))
                .thenReturn(List.of(node(ranked, false), node(alreadyHas, false),
                        node(toFetch, false), node(barrierMap, true)));

        when(scoreRepository.findEligibleCampaignRows(eq(USER), eq(List.of(alreadyHas.getId())), any()))
                .thenReturn(List.of(new com.accsaber.backend.model.entity.score.Score()));
        when(scoreRepository.findEligibleCampaignRows(eq(USER), eq(List.of(toFetch.getId())), any()))
                .thenReturn(List.of());
        when(beatLeaderClient.getPlayerScoreOnLeaderboard(String.valueOf(USER), "hash-bl-fetch", "Normal", "Standard"))
                .thenReturn(Optional.of(blScore(900_000)));
        when(scoreSaberClient.getPlayerScoreOnLeaderboard(String.valueOf(USER), "ss-fetch"))
                .thenReturn(Optional.empty());

        scoreImportService.backfillAndSettleLegacyCampaign(USER, CAMPAIGN);

        verify(beatLeaderClient).getPlayerScoreOnLeaderboard(String.valueOf(USER), "hash-bl-fetch", "Normal", "Standard");
        verify(scoreSaberClient).getPlayerScoreOnLeaderboard(String.valueOf(USER), "ss-fetch");
        verify(beatLeaderClient, never()).getPlayerScoreOnLeaderboard(anyString(), eq("hash-bl-ranked"), anyString(), anyString());
        verify(scoreSaberClient, never()).getPlayerScoreOnLeaderboard(anyString(), eq("ss-ranked"));
        verify(beatLeaderClient, never()).getPlayerScoreOnLeaderboard(anyString(), eq("hash-bl-has"), anyString(), anyString());
        verify(scoreSaberClient, never()).getPlayerScoreOnLeaderboard(anyString(), eq("ss-has"));
        verify(beatLeaderClient, never()).getPlayerScoreOnLeaderboard(anyString(), eq("hash-bl-barrier"), anyString(), anyString());
        verify(scoreService, times(1)).recordCampaignBackfillScore(any());
        verify(campaignEvaluationService).importLegacyScores(USER, CAMPAIGN);
    }

    @Test
    void recheckRejectsNonLegacyCampaigns() {
        when(campaignRepository.findByIdAndActiveTrue(CAMPAIGN))
                .thenReturn(Optional.of(Campaign.builder().id(CAMPAIGN).legacy(false).build()));

        assertThatThrownBy(() -> scoreImportService.recheckLegacyCampaign(CAMPAIGN, null))
                .isInstanceOf(ValidationException.class);

        verify(userCampaignRepository, never()).findUserIdsByCampaignAndStatus(any(), any());
        verify(campaignEvaluationService, never()).importLegacyScores(any(), any());
    }

    @Test
    void recheckSweepsEveryInProgressParticipant() {
        Long other = 76561198000000001L;
        MapDifficulty toFetch = md(MapDifficultyStatus.CAMPAIGN, "bl-fetch", "ss-fetch");

        when(campaignRepository.findByIdAndActiveTrue(CAMPAIGN))
                .thenReturn(Optional.of(Campaign.builder().id(CAMPAIGN).legacy(true).build()));
        when(userCampaignRepository.findUserIdsByCampaignAndStatus(CAMPAIGN, UserCampaignStatus.IN_PROGRESS))
                .thenReturn(List.of(USER, other));
        when(duplicateUserService.resolvePrimaryUserId(any())).thenAnswer(inv -> inv.getArgument(0));
        when(modifierCacheService.getModifierCodeToId()).thenReturn(Map.of());
        when(campaignDifficultyRepository.findActiveWithMapByCampaignId(CAMPAIGN))
                .thenReturn(List.of(node(toFetch, false)));
        when(scoreRepository.findEligibleCampaignRows(any(), eq(List.of(toFetch.getId())), any()))
                .thenReturn(List.of());
        when(beatLeaderClient.getPlayerScoreOnLeaderboard(anyString(), eq("hash-bl-fetch"), anyString(), anyString()))
                .thenReturn(Optional.of(blScore(900_000)));
        when(scoreSaberClient.getPlayerScoreOnLeaderboard(anyString(), eq("ss-fetch")))
                .thenReturn(Optional.empty());

        assertThat(scoreImportService.recheckLegacyCampaign(CAMPAIGN, null)).isCompleted();

        verify(beatLeaderClient).getPlayerScoreOnLeaderboard(String.valueOf(USER), "hash-bl-fetch", "Normal", "Standard");
        verify(beatLeaderClient).getPlayerScoreOnLeaderboard(String.valueOf(other), "hash-bl-fetch", "Normal", "Standard");
        verify(campaignEvaluationService).importLegacyScores(USER, CAMPAIGN);
        verify(campaignEvaluationService).importLegacyScores(other, CAMPAIGN);
    }

    @Test
    void recheckForOneUserResolvesTheirPrimaryAccount() {
        Long duplicate = 76561198000000002L;

        when(campaignRepository.findByIdAndActiveTrue(CAMPAIGN))
                .thenReturn(Optional.of(Campaign.builder().id(CAMPAIGN).legacy(true).build()));
        when(duplicateUserService.resolvePrimaryUserId(any()))
                .thenAnswer(inv -> duplicate.equals(inv.getArgument(0)) ? USER : inv.getArgument(0));
        when(modifierCacheService.getModifierCodeToId()).thenReturn(Map.of());
        when(campaignDifficultyRepository.findActiveWithMapByCampaignId(CAMPAIGN)).thenReturn(List.of());

        assertThat(scoreImportService.recheckLegacyCampaign(CAMPAIGN, duplicate)).isCompleted();

        verify(userCampaignRepository, never()).findUserIdsByCampaignAndStatus(any(), any());
        verify(campaignEvaluationService).importLegacyScores(USER, CAMPAIGN);
    }

    private static MapDifficulty md(MapDifficultyStatus status, String blLeaderboardId, String ssLeaderboardId) {
        return MapDifficulty.builder()
                .id(UUID.randomUUID())
                .status(status)
                .map(com.accsaber.backend.model.entity.map.Map.builder()
                        .songHash("hash-" + blLeaderboardId).build())
                .difficulty(Difficulty.NORMAL)
                .characteristic("Standard")
                .blLeaderboardId(blLeaderboardId)
                .ssLeaderboardId(ssLeaderboardId)
                .build();
    }

    private static CampaignDifficulty node(MapDifficulty md, boolean barrier) {
        return CampaignDifficulty.builder().id(UUID.randomUUID()).mapDifficulty(md).barrier(barrier).build();
    }

    private static BeatLeaderScoreResponse blScore(int baseScore) {
        BeatLeaderScoreResponse bl = new BeatLeaderScoreResponse();
        bl.setBaseScore(baseScore);
        return bl;
    }
}
