package com.accsaber.backend.model.entity.campaign;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CampaignNodeBorderLayerConverter implements AttributeConverter<CampaignNodeBorderLayer, String> {

    @Override
    public String convertToDatabaseColumn(CampaignNodeBorderLayer layer) {
        if (layer == null)
            return null;
        return layer.name();
    }

    @Override
    public CampaignNodeBorderLayer convertToEntityAttribute(String value) {
        if (value == null)
            return null;
        return CampaignNodeBorderLayer.valueOf(value);
    }
}
