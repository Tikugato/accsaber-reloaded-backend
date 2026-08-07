package com.accsaber.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.SerializationFeature;

import tools.jackson.databind.json.JsonMapper;

class PlainDoubleSerializationTest {

    private static final tools.jackson.databind.ObjectMapper HTTP = JsonMapper.builder()
            .addModule(new PlainDoubleJacksonConfig().plainDoubleModule())
            .build();

    private static final com.fasterxml.jackson.databind.ObjectMapper WS =
            new com.fasterxml.jackson.databind.ObjectMapper()
                    .registerModule(PlainDoubleJackson2Module.create())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    record Holder(Double value) {
    }

    private String http(Double d) {
        return HTTP.writeValueAsString(new Holder(d));
    }

    private String ws(Double d) throws Exception {
        return WS.writeValueAsString(new Holder(d));
    }

    @Nested
    @DisplayName("whole numbers serialise without a decimal point")
    class WholeNumbers {

        @Test
        void integralValuesLoseTheDecimalPoint() throws Exception {
            assertThat(http(-1.0)).isEqualTo("{\"value\":-1}");
            assertThat(http(0.0)).isEqualTo("{\"value\":0}");
            assertThat(http(1.0)).isEqualTo("{\"value\":1}");
            assertThat(http(25.0)).isEqualTo("{\"value\":25}");
            assertThat(http(100.0)).isEqualTo("{\"value\":100}");
            assertThat(ws(-1.0)).isEqualTo("{\"value\":-1}");
            assertThat(ws(25.0)).isEqualTo("{\"value\":25}");
        }

        @Test
        void negativeZeroIsPlainZero() {
            assertThat(http(-0.0)).isEqualTo("{\"value\":0}");
        }
    }

    @Nested
    @DisplayName("fractional values keep their precision")
    class Fractional {

        @Test
        void fractionsAreUntouched() throws Exception {
            assertThat(http(-2.5)).isEqualTo("{\"value\":-2.5}");
            assertThat(http(0.965)).isEqualTo("{\"value\":0.965}");
            assertThat(http(1006.040784)).isEqualTo("{\"value\":1006.040784}");
            assertThat(ws(0.965)).isEqualTo("{\"value\":0.965}");
        }

        @Test
        void smallMagnitudesDoNotUseExponentNotation() {
            assertThat(http(0.00001)).isEqualTo("{\"value\":0.00001}");
            assertThat(http(1.0E-7)).isEqualTo("{\"value\":0.0000001}");
        }

        @Test
        void largeMagnitudesDoNotUseExponentNotation() {
            assertThat(http(1.25E7)).isEqualTo("{\"value\":12500000}");
            assertThat(http(1.0E30)).isEqualTo("{\"value\":1000000000000000000000000000000}");
        }
    }

    @Nested
    @DisplayName("edge cases")
    class Edges {

        @Test
        void nullStaysNull() {
            assertThat(http(null)).isEqualTo("{\"value\":null}");
        }

        @Test
        void integralValuesBeyondExactDoublePrecisionStayPlain() {
            assertThat(http(9.007199254740994E15)).isEqualTo("{\"value\":9007199254740994}");
        }
    }
}
