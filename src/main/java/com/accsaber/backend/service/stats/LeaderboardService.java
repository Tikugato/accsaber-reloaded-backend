package com.accsaber.backend.service.stats;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.accsaber.backend.exception.ResourceNotFoundException;
import com.accsaber.backend.model.dto.response.player.LeaderboardResponse;
import com.accsaber.backend.model.dto.response.player.XpLeaderboardResponse;
import com.accsaber.backend.model.entity.user.User;
import com.accsaber.backend.model.entity.user.UserCategoryStatistics;
import com.accsaber.backend.repository.CategoryRepository;
import com.accsaber.backend.repository.user.UserCategoryStatisticsRepository;
import com.accsaber.backend.repository.user.UserRepository;
import com.accsaber.backend.service.milestone.LevelService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaderboardService {

    private final UserCategoryStatisticsRepository statisticsRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final LevelService levelService;

    public Page<LeaderboardResponse> getGlobal(UUID categoryId, String search, Pageable pageable) {
        verifyCategory(categoryId);
        boolean hasSearch = search != null && !search.isBlank();
        Pageable effective = withDefaultSort(pageable, Sort.by(Sort.Direction.ASC, "ranking"));
        Page<UserCategoryStatistics> page = hasSearch
                ? statisticsRepository.findActiveByCategoryPagedWithSearch(categoryId, search.trim(), effective)
                : statisticsRepository.findActiveByCategoryPaged(categoryId, effective);
        return enrichWithLastWeekRanking(page, categoryId);
    }

    public Page<LeaderboardResponse> getByCountry(UUID categoryId, String country, String search, Pageable pageable) {
        verifyCategory(categoryId);
        boolean hasSearch = search != null && !search.isBlank();
        Pageable effective = withDefaultSort(pageable, Sort.by(Sort.Direction.ASC, "countryRanking"));
        Page<UserCategoryStatistics> page = hasSearch
                ? statisticsRepository.findActiveByCategoryAndCountryPagedWithSearch(
                        categoryId, country, search.trim(), effective)
                : statisticsRepository.findActiveByCategoryAndCountryPaged(categoryId, country, effective);
        return enrichWithLastWeekRanking(page, categoryId);
    }

    public Page<XpLeaderboardResponse> getXpLeaderboard(String country, String search, Pageable pageable) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasCountry = country != null && !country.isBlank();
        Page<User> page;
        if (hasCountry && hasSearch) {
            page = userRepository.findXpLeaderboardByCountryWithSearch(country, search.trim(), pageable);
        } else if (hasCountry) {
            page = userRepository.findXpLeaderboardByCountry(country, pageable);
        } else if (hasSearch) {
            page = userRepository.findXpLeaderboardWithSearch(search.trim(), pageable);
        } else {
            page = userRepository.findXpLeaderboard(pageable);
        }
        return page.map(this::toXpResponse);
    }

    private Page<LeaderboardResponse> enrichWithLastWeekRanking(Page<UserCategoryStatistics> page, UUID categoryId) {
        List<Long> userIds = page.getContent().stream()
                .map(s -> s.getUser().getId())
                .toList();
        Map<Long, Integer> lastWeekRankings = Map.of();
        if (!userIds.isEmpty()) {
            lastWeekRankings = statisticsRepository.findRankingsOneWeekAgo(categoryId, userIds).stream()
                    .collect(Collectors.toMap(
                            row -> ((Number) row[0]).longValue(),
                            row -> row[1] != null ? ((Number) row[1]).intValue() : null));
        }
        Map<Long, Integer> finalRankings = lastWeekRankings;
        return page.map(stats -> toResponse(stats, finalRankings.get(stats.getUser().getId())));
    }

    private XpLeaderboardResponse toXpResponse(User user) {
        return XpLeaderboardResponse.builder()
                .ranking(user.getXpRanking())
                .countryRanking(user.getXpCountryRanking())
                .userId(String.valueOf(user.getId()))
                .userName(user.getName())
                .country(user.getCountry())
                .avatarUrl(user.getAvatarUrl())
                .totalXp(user.getTotalXp())
                .level(levelService.calculateLevel(user.getTotalXp()).getLevel())
                .build();
    }

    private LeaderboardResponse toResponse(UserCategoryStatistics stats, Integer rankingLastWeek) {
        return LeaderboardResponse.builder()
                .ranking(stats.getRanking())
                .countryRanking(stats.getCountryRanking())
                .userId(String.valueOf(stats.getUser().getId()))
                .userName(stats.getUser().getName())
                .country(stats.getUser().getCountry())
                .avatarUrl(stats.getUser().getAvatarUrl())
                .ap(stats.getAp())
                .averageAcc(stats.getAverageAcc())
                .averageAp(stats.getAverageAp())
                .rankedPlays(stats.getRankedPlays())
                .topPlayId(stats.getTopPlay() != null ? stats.getTopPlay().getId() : null)
                .rankingLastWeek(rankingLastWeek)
                .build();
    }

    private Pageable withDefaultSort(Pageable pageable, Sort defaultSort) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort);
    }

    private void verifyCategory(UUID categoryId) {
        categoryRepository.findByIdAndActiveTrue(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }
}
