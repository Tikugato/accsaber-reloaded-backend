package com.accsaber.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class OpenApiGroupConfig {

    private static final String[] INTERNAL_PATHS = {
            "/v1/admin/**",
            "/v1/ranking/**",
            "/v1/staff/**",
            "/v1/discord/**",
            "/v1/supporters/claim-by-role",
            "/v1/supporters/assign"
    };

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .displayName("Public API")
                .pathsToMatch("/v1/**")
                .pathsToExclude(INTERNAL_PATHS)
                .addOpenApiCustomizer(dropUnusedTags())
                .build();
    }

    @Bean
    public GroupedOpenApi staffApi() {
        return GroupedOpenApi.builder()
                .group("staff")
                .displayName("Ranking and Staff")
                .pathsToMatch("/v1/ranking/**", "/v1/staff/**")
                .addOpenApiCustomizer(dropUnusedTags())
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("Admin")
                .pathsToMatch("/v1/admin/**", "/v1/discord/**", "/v1/supporters/claim-by-role",
                        "/v1/supporters/assign")
                .addOpenApiCustomizer(dropUnusedTags())
                .build();
    }

    private OpenApiCustomizer dropUnusedTags() {
        return openApi -> {
            List<Tag> declared = openApi.getTags();
            if (declared == null || openApi.getPaths() == null) {
                return;
            }
            Set<String> referenced = referencedTags(openApi);
            openApi.setTags(declared.stream().filter(tag -> referenced.contains(tag.getName())).toList());
        };
    }

    private Set<String> referencedTags(OpenAPI openApi) {
        Set<String> referenced = new HashSet<>();
        for (PathItem path : openApi.getPaths().values()) {
            for (Operation operation : path.readOperations()) {
                if (operation.getTags() != null) {
                    referenced.addAll(operation.getTags());
                }
            }
        }
        return referenced;
    }
}
