package com.ingoboka_api.v1.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneNumberUtilsTest {

    @Test
    void normalizesLocalRwandaFormat() {
        assertThat(PhoneNumberUtils.normalizeRwanda("0781234567")).isEqualTo("+250781234567");
    }

    @Test
    void normalizesCountryCodeWithoutPlus() {
        assertThat(PhoneNumberUtils.normalizeRwanda("250781234567")).isEqualTo("+250781234567");
    }

    @Test
    void keepsE164Format() {
        assertThat(PhoneNumberUtils.normalizeRwanda("+250781234567")).isEqualTo("+250781234567");
    }

    @Test
    void detectsSwaggerPlaceholderIdentifiers() {
        assertThat(PhoneNumberUtils.isPlaceholderIdentifier("email")).isTrue();
        assertThat(PhoneNumberUtils.isPlaceholderIdentifier("phone")).isTrue();
        assertThat(PhoneNumberUtils.isPlaceholderIdentifier("+250780000001")).isFalse();
    }
}
