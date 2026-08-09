package com.ingoboka_api.v1.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CountryCodesTest {

    @Test
    void mapsRwandaNameVariantsToRw() {
        assertThat(CountryCodes.normalizeRwandaAware("Rwanda")).isEqualTo("RW");
        assertThat(CountryCodes.normalizeRwandaAware("rwanda")).isEqualTo("RW");
        assertThat(CountryCodes.normalizeRwandaAware("RWANDA")).isEqualTo("RW");
    }

    @Test
    void uppercasesTwoLetterCodes() {
        assertThat(CountryCodes.normalizeRwandaAware("rw")).isEqualTo("RW");
        assertThat(CountryCodes.normalizeRwandaAware("RW")).isEqualTo("RW");
    }

    @Test
    void defaultsBlankToRw() {
        assertThat(CountryCodes.normalizeRwandaAware(null)).isEqualTo("RW");
        assertThat(CountryCodes.normalizeRwandaAware("")).isEqualTo("RW");
        assertThat(CountryCodes.normalizeRwandaAware("   ")).isEqualTo("RW");
    }

    @Test
    void defaultsUnknownLongValuesToRw() {
        assertThat(CountryCodes.normalizeRwandaAware("Unknown Country")).isEqualTo("RW");
    }
}
