package com.accsaber.backend.service.milestone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.accsaber.backend.exception.ResourceNotFoundException;
import com.accsaber.backend.model.dto.MilestoneQuerySpec;
import com.accsaber.backend.model.dto.MilestoneQuerySpec.FilterSpec;
import com.accsaber.backend.model.dto.MilestoneQuerySpec.SelectSpec;
import com.accsaber.backend.model.dto.request.milestone.CreateMilestoneRequest;
import com.accsaber.backend.model.dto.response.milestone.MilestoneResponse;
import com.accsaber.backend.model.entity.map.MapDifficulty;
import com.accsaber.backend.model.entity.map.MapDifficultyMilestoneLink;
import com.accsaber.backend.model.entity.milestone.Milestone;
import com.accsaber.backend.model.entity.milestone.MilestoneSet;
import com.accsaber.backend.model.entity.milestone.MilestoneTier;
import com.accsaber.backend.repository.CategoryRepository;
import com.accsaber.backend.repository.map.MapDifficultyMilestoneLinkRepository;
import com.accsaber.backend.repository.map.MapDifficultyRepository;
import com.accsaber.backend.repository.milestone.MilestoneCompletionStatsRepository;
import com.accsaber.backend.repository.milestone.MilestoneRepository;
import com.accsaber.backend.repository.milestone.MilestoneSetRepository;
import com.accsaber.backend.repository.milestone.UserMilestoneLinkRepository;
import com.accsaber.backend.repository.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class MilestoneServiceMapLinkTest {

        @Mock
        private MilestoneRepository milestoneRepository;
        @Mock
        private MilestoneSetRepository milestoneSetRepository;
        @Mock
        private UserMilestoneLinkRepository userMilestoneLinkRepository;
        @Mock
        private MilestoneCompletionStatsRepository completionStatsRepository;
        @Mock
        private CategoryRepository categoryRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private MapDifficultyRepository mapDifficultyRepository;
        @Mock
        private MapDifficultyMilestoneLinkRepository mapDifficultyMilestoneLinkRepository;
        @Mock
        private MilestoneEvaluationService milestoneEvaluationService;
        @Mock
        private MilestoneQueryBuilderService queryBuilderService;

        @InjectMocks
        private MilestoneService service;

        private MilestoneSet set;
        private MilestoneQuerySpec querySpec;

        @BeforeEach
        void setUp() {
                set = MilestoneSet.builder()
                                .id(UUID.randomUUID())
                                .title("Map Milestones")
                                .description("Milestones tied to maps")
                                .setBonusXp(BigDecimal.ZERO)
                                .build();

                querySpec = new MilestoneQuerySpec(
                                new SelectSpec("MAX", "ap"),
                                "scores",
                                List.of(new FilterSpec("active", "=", true)));
        }

        private CreateMilestoneRequest buildRequest(List<UUID> mapDifficultyIds) {
                CreateMilestoneRequest request = new CreateMilestoneRequest();
                request.setSetId(set.getId());
                request.setTitle("Map Milestone");
                request.setDescription("Tied to specific maps");
                request.setType("milestone");
                request.setTier(MilestoneTier.gold);
                request.setXp(BigDecimal.valueOf(250));
                request.setQuerySpec(querySpec);
                request.setTargetValue(BigDecimal.valueOf(95));
                request.setComparison("GTE");
                request.setMapDifficultyIds(mapDifficultyIds);
                return request;
        }

        @Nested
        class CreateMilestoneWithMapLinks {

                @Test
                void withMapDifficultyIds_createsLinksForEach() {
                        UUID md1Id = UUID.randomUUID();
                        UUID md2Id = UUID.randomUUID();
                        MapDifficulty md1 = MapDifficulty.builder().id(md1Id).build();
                        MapDifficulty md2 = MapDifficulty.builder().id(md2Id).build();

                        Milestone saved = Milestone.builder()
                                        .id(UUID.randomUUID())
                                        .milestoneSet(set)
                                        .title("Map Milestone")
                                        .type("milestone")
                                        .tier(MilestoneTier.gold)
                                        .xp(BigDecimal.valueOf(250))
                                        .querySpec(querySpec)
                                        .targetValue(BigDecimal.valueOf(95))
                                        .comparison("GTE")
                                        .build();

                        when(milestoneSetRepository.findByIdAndActiveTrue(set.getId()))
                                        .thenReturn(Optional.of(set));
                        when(milestoneRepository.save(any())).thenReturn(saved);
                        when(mapDifficultyRepository.findByIdAndActiveTrue(md1Id)).thenReturn(Optional.of(md1));
                        when(mapDifficultyRepository.findByIdAndActiveTrue(md2Id)).thenReturn(Optional.of(md2));
                        when(mapDifficultyMilestoneLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                        MilestoneResponse response = service.createMilestone(buildRequest(List.of(md1Id, md2Id)));

                        assertThat(response).isNotNull();
                        verify(mapDifficultyMilestoneLinkRepository, times(2))
                                        .save(any(MapDifficultyMilestoneLink.class));
                }

                @Test
                void withMapDifficultyIds_linkContainsCorrectReferences() {
                        UUID mdId = UUID.randomUUID();
                        MapDifficulty md = MapDifficulty.builder().id(mdId).build();

                        Milestone saved = Milestone.builder()
                                        .id(UUID.randomUUID())
                                        .milestoneSet(set)
                                        .title("Map Milestone")
                                        .type("milestone")
                                        .tier(MilestoneTier.gold)
                                        .xp(BigDecimal.valueOf(250))
                                        .querySpec(querySpec)
                                        .targetValue(BigDecimal.valueOf(95))
                                        .comparison("GTE")
                                        .build();

                        when(milestoneSetRepository.findByIdAndActiveTrue(set.getId()))
                                        .thenReturn(Optional.of(set));
                        when(milestoneRepository.save(any())).thenReturn(saved);
                        when(mapDifficultyRepository.findByIdAndActiveTrue(mdId)).thenReturn(Optional.of(md));

                        ArgumentCaptor<MapDifficultyMilestoneLink> captor = ArgumentCaptor
                                        .forClass(MapDifficultyMilestoneLink.class);
                        when(mapDifficultyMilestoneLinkRepository.save(captor.capture()))
                                        .thenAnswer(inv -> inv.getArgument(0));

                        service.createMilestone(buildRequest(List.of(mdId)));

                        MapDifficultyMilestoneLink link = captor.getValue();
                        assertThat(link.getMilestone()).isEqualTo(saved);
                        assertThat(link.getMapDifficulty()).isEqualTo(md);
                }

                @Test
                void nullMapDifficultyIds_skipsLinkCreation() {
                        Milestone saved = Milestone.builder()
                                        .id(UUID.randomUUID())
                                        .milestoneSet(set)
                                        .title("No Maps")
                                        .type("milestone")
                                        .tier(MilestoneTier.bronze)
                                        .xp(BigDecimal.valueOf(100))
                                        .querySpec(querySpec)
                                        .targetValue(BigDecimal.ONE)
                                        .comparison("GTE")
                                        .build();

                        when(milestoneSetRepository.findByIdAndActiveTrue(set.getId()))
                                        .thenReturn(Optional.of(set));
                        when(milestoneRepository.save(any())).thenReturn(saved);

                        service.createMilestone(buildRequest(null));

                        verify(mapDifficultyMilestoneLinkRepository, never()).save(any());
                        verify(mapDifficultyRepository, never()).findByIdAndActiveTrue(any());
                }

                @Test
                void emptyMapDifficultyIds_skipsLinkCreation() {
                        Milestone saved = Milestone.builder()
                                        .id(UUID.randomUUID())
                                        .milestoneSet(set)
                                        .title("Empty Maps")
                                        .type("milestone")
                                        .tier(MilestoneTier.bronze)
                                        .xp(BigDecimal.valueOf(100))
                                        .querySpec(querySpec)
                                        .targetValue(BigDecimal.ONE)
                                        .comparison("GTE")
                                        .build();

                        when(milestoneSetRepository.findByIdAndActiveTrue(set.getId()))
                                        .thenReturn(Optional.of(set));
                        when(milestoneRepository.save(any())).thenReturn(saved);

                        service.createMilestone(buildRequest(List.of()));

                        verify(mapDifficultyMilestoneLinkRepository, never()).save(any());
                }

                @Test
                void mapDifficultyNotFound_throwsResourceNotFoundException() {
                        UUID missingId = UUID.randomUUID();

                        Milestone saved = Milestone.builder()
                                        .id(UUID.randomUUID())
                                        .milestoneSet(set)
                                        .title("Bad Link")
                                        .type("milestone")
                                        .tier(MilestoneTier.bronze)
                                        .xp(BigDecimal.ONE)
                                        .querySpec(querySpec)
                                        .targetValue(BigDecimal.ONE)
                                        .comparison("GTE")
                                        .build();

                        when(milestoneSetRepository.findByIdAndActiveTrue(set.getId()))
                                        .thenReturn(Optional.of(set));
                        when(milestoneRepository.save(any())).thenReturn(saved);
                        when(mapDifficultyRepository.findByIdAndActiveTrue(missingId))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> service.createMilestone(buildRequest(List.of(missingId))))
                                        .isInstanceOf(ResourceNotFoundException.class);
                }
        }

        @Nested
        class AddMapDifficultyLinks {

                private Milestone milestone;

                @BeforeEach
                void setUpMilestone() {
                        milestone = Milestone.builder()
                                        .id(UUID.randomUUID())
                                        .milestoneSet(set)
                                        .title("Existing Milestone")
                                        .type("milestone")
                                        .tier(MilestoneTier.silver)
                                        .xp(BigDecimal.valueOf(200))
                                        .querySpec(querySpec)
                                        .targetValue(BigDecimal.valueOf(90))
                                        .comparison("GTE")
                                        .build();
                }

                @Test
                void addsLinksToExistingMilestone() {
                        UUID md1Id = UUID.randomUUID();
                        UUID md2Id = UUID.randomUUID();
                        MapDifficulty md1 = MapDifficulty.builder().id(md1Id).build();
                        MapDifficulty md2 = MapDifficulty.builder().id(md2Id).build();

                        when(milestoneRepository.findByIdAndActiveTrue(milestone.getId()))
                                        .thenReturn(Optional.of(milestone));
                        when(mapDifficultyRepository.findByIdAndActiveTrue(md1Id)).thenReturn(Optional.of(md1));
                        when(mapDifficultyRepository.findByIdAndActiveTrue(md2Id)).thenReturn(Optional.of(md2));
                        when(mapDifficultyMilestoneLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                        service.addMapDifficultyLinks(milestone.getId(), List.of(md1Id, md2Id));

                        verify(mapDifficultyMilestoneLinkRepository, times(2))
                                        .save(any(MapDifficultyMilestoneLink.class));
                }

                @Test
                void linkReferencesCorrectMilestoneAndDifficulty() {
                        UUID mdId = UUID.randomUUID();
                        MapDifficulty md = MapDifficulty.builder().id(mdId).build();

                        when(milestoneRepository.findByIdAndActiveTrue(milestone.getId()))
                                        .thenReturn(Optional.of(milestone));
                        when(mapDifficultyRepository.findByIdAndActiveTrue(mdId)).thenReturn(Optional.of(md));

                        ArgumentCaptor<MapDifficultyMilestoneLink> captor = ArgumentCaptor
                                        .forClass(MapDifficultyMilestoneLink.class);
                        when(mapDifficultyMilestoneLinkRepository.save(captor.capture()))
                                        .thenAnswer(inv -> inv.getArgument(0));

                        service.addMapDifficultyLinks(milestone.getId(), List.of(mdId));

                        MapDifficultyMilestoneLink link = captor.getValue();
                        assertThat(link.getMilestone()).isEqualTo(milestone);
                        assertThat(link.getMapDifficulty()).isEqualTo(md);
                }

                @Test
                void milestoneNotFound_throwsResourceNotFoundException() {
                        UUID missingId = UUID.randomUUID();

                        when(milestoneRepository.findByIdAndActiveTrue(missingId))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> service.addMapDifficultyLinks(missingId, List.of(UUID.randomUUID())))
                                        .isInstanceOf(ResourceNotFoundException.class);
                }

                @Test
                void mapDifficultyNotFound_throwsResourceNotFoundException() {
                        UUID missingMdId = UUID.randomUUID();

                        when(milestoneRepository.findByIdAndActiveTrue(milestone.getId()))
                                        .thenReturn(Optional.of(milestone));
                        when(mapDifficultyRepository.findByIdAndActiveTrue(missingMdId))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> service.addMapDifficultyLinks(milestone.getId(), List.of(missingMdId)))
                                        .isInstanceOf(ResourceNotFoundException.class);
                }
        }
}
