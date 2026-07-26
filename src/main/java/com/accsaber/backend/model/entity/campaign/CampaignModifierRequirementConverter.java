package com.accsaber.backend.model.entity.campaign;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CampaignModifierRequirementConverter
        implements AttributeConverter<CampaignModifierRequirement, String> {

    @Override
    public String convertToDatabaseColumn(CampaignModifierRequirement requirement) {
        if (requirement == null)
            return null;
        return requirement.name();
    }

    @Override
    public CampaignModifierRequirement convertToEntityAttribute(String value) {
        if (value == null)
            return null;
        return CampaignModifierRequirement.valueOf(value);
    }
}
