package com.accsaber.backend.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

public class SearchFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions contributions) {
        contributions.getFunctionRegistry().registerPattern(
                "search_normalize",
                "search_normalize(?1)",
                contributions.getTypeConfiguration().getBasicTypeRegistry()
                        .resolve(StandardBasicTypes.STRING));
    }
}
