package com.accsaber.backend.config;

import java.math.BigDecimal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class PlainDoubleJacksonConfig {

    public static final class PlainDoubleSerializer extends ValueSerializer<Double> {

        @Override
        public void serialize(Double value, JsonGenerator gen, SerializationContext ctxt) {
            if (value == null) {
                gen.writeNull();
                return;
            }
            double d = value;
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                gen.writeNumber(d);
                return;
            }
            String rendered = Double.toString(d);
            if (rendered.indexOf('E') < 0) {
                gen.writeNumber(d);
                return;
            }
            gen.writeRawValue(new BigDecimal(rendered).toPlainString());
        }
    }

    @Bean
    JacksonModule plainDoubleModule() {
        SimpleModule module = new SimpleModule("PlainDoubleModule");
        PlainDoubleSerializer serializer = new PlainDoubleSerializer();
        module.addSerializer(Double.class, serializer);
        module.addSerializer(Double.TYPE, serializer);
        return module;
    }
}
