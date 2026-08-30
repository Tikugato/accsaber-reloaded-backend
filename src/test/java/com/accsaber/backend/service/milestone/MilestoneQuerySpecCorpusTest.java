package com.accsaber.backend.service.milestone;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.accsaber.backend.model.dto.MilestoneQuerySpec;
import com.accsaber.backend.model.dto.MilestoneQuerySpec.SelectSpec;
import com.accsaber.backend.service.milestone.source.ItemSources;
import com.accsaber.backend.service.milestone.source.MapSources;
import com.accsaber.backend.service.milestone.source.MilestoneSourceRegistry;
import com.accsaber.backend.service.milestone.source.MilestoneSource;
import com.accsaber.backend.service.milestone.source.PlayerSources;
import com.accsaber.backend.service.milestone.source.ProgressSources;
import com.accsaber.backend.service.milestone.source.ProgressionSources;
import com.accsaber.backend.service.milestone.source.ScoreSources;
import com.accsaber.backend.service.milestone.sql.MilestoneSqlCompiler;
import com.accsaber.backend.service.milestone.sql.MilestoneSqlCompiler.Compiled;
import com.accsaber.backend.service.milestone.sql.MilestoneSqlCompiler.Context;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@Tag("integration")
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ MilestoneSourceRegistry.class, MilestoneSqlCompiler.class, ScoreSources.class, MapSources.class,
        PlayerSources.class, ProgressionSources.class, ItemSources.class, ProgressSources.class,
        MilestoneQueryBuilderService.class })
class MilestoneQuerySpecCorpusTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static final Long USER_ID = 76561198000000000L;
    private static final UUID CATEGORY_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MilestoneSqlCompiler compiler;

    @Autowired
    private MilestoneSourceRegistry registry;

    @Autowired
    private MilestoneQueryBuilderService queryBuilderService;

    record CorpusEntry(String source, String title, String categoryCode, Double targetValue,
            String comparison, MilestoneQuerySpec querySpec) {

        @Override
        public String toString() {
            return source.replace(".sql", "") + " :: " + title;
        }
    }

    static List<CorpusEntry> corpus() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (var stream = new ClassPathResource("milestone/query-spec-corpus.json").getInputStream()) {
            return List.of(mapper.readValue(stream, CorpusEntry[].class));
        }
    }

    static Stream<CorpusEntry> everySpec() throws Exception {
        return corpus().stream();
    }

    @Test
    @DisplayName("the corpus holds every shipped milestone")
    void corpusIsComplete() throws Exception {
        assertThat(corpus()).hasSize(264);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everySpec")
    @DisplayName("validates")
    void validates(CorpusEntry entry) {
        queryBuilderService.validate(entry.querySpec());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everySpec")
    @DisplayName("compiles to SQL postgres accepts")
    void compilesToRunnableSql(CorpusEntry entry) {
        UUID categoryId = entry.categoryCode() == null ? null : CATEGORY_ID;
        assertRunnable(compiler.scalar(entry.querySpec(), Context.of(USER_ID, categoryId).withHavingValue(0.0)));

        if (entry.querySpec().divisor() != null) {
            assertRunnable(compiler.scalar(entry.querySpec().divisor(), Context.of(USER_ID, categoryId)));
        }
        if (entry.querySpec().having() != null && entry.querySpec().having().valueQuery() != null) {
            assertRunnable(compiler.scalar(entry.querySpec().having().valueQuery(),
                    Context.of(USER_ID, categoryId)));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everySpec")
    @DisplayName("evaluates against an empty database without error")
    void evaluatesAgainstEmptyDatabase(CorpusEntry entry) {
        UUID categoryId = entry.categoryCode() == null ? null : CATEGORY_ID;
        queryBuilderService.evaluate(entry.querySpec(), USER_ID, categoryId);
    }

    @Test
    @DisplayName("every declared column on every source resolves against the real schema")
    void everySourceColumnResolves() {
        List<String> broken = new ArrayList<>();
        for (MilestoneSource source : registry.all()) {
            for (String column : source.columns().keySet()) {
                MilestoneQuerySpec spec = new MilestoneQuerySpec(
                        new SelectSpec("COUNT", column), source.name(), List.of());
                explainInSavepoint(spec, source.name() + "." + column, broken);
            }
        }
        assertThat(broken).isEmpty();
    }

    @Test
    @DisplayName("every source is reachable from at least one trigger")
    void everySourceHasATrigger() {
        assertThat(registry.all()).allSatisfy(source -> assertThat(source.triggers()).isNotEmpty());
    }

    private void explainInSavepoint(MilestoneQuerySpec spec, String label, List<String> broken) {
        Compiled compiled = compiler.scalar(spec, Context.of(USER_ID, null));
        entityManager.unwrap(Session.class).doWork(connection -> {
            Savepoint savepoint = connection.setSavepoint();
            try (PreparedStatement statement = connection.prepareStatement(
                    "EXPLAIN " + compiled.sql().replaceAll(":\\w+", "?"))) {
                int index = 1;
                for (Object value : compiled.params().values()) {
                    statement.setObject(index++, value);
                }
                statement.execute();
                connection.releaseSavepoint(savepoint);
            } catch (SQLException e) {
                connection.rollback(savepoint);
                broken.add(label + " -> " + e.getMessage().lines().findFirst().orElse(""));
            }
        });
    }

    private void assertRunnable(Compiled compiled) {
        Query query = entityManager.createNativeQuery("EXPLAIN " + compiled.sql());
        compiled.params().forEach(query::setParameter);
        assertThat(query.getResultList()).isNotEmpty();
    }
}
