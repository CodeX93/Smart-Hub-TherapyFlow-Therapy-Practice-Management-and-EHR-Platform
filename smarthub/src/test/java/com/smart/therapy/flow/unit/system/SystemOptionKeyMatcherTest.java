package com.smart.therapy.flow.unit.system;

import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SystemOptionKeyMatcher")
class SystemOptionKeyMatcherTest {

    @Test
    @DisplayName("normalizes hyphen and underscore variants")
    void normalizesHyphenAndUnderscore() {
        assertThat(SystemOptionKeyMatcher.normalize("in-person")).isEqualTo("in_person");
        assertThat(SystemOptionKeyMatcher.normalize("in_person")).isEqualTo("in_person");
        assertThat(SystemOptionKeyMatcher.normalize("IN-PERSON")).isEqualTo("in_person");
    }

    @Test
    @DisplayName("matchesAny compares normalized keys")
    void matchesAnyComparesNormalizedKeys() {
        assertThat(SystemOptionKeyMatcher.matchesAny("in-person", "in_person", "online")).isTrue();
        assertThat(SystemOptionKeyMatcher.matchesAny("Online", "online", "phone")).isTrue();
        assertThat(SystemOptionKeyMatcher.matchesAny("hybrid", "in_person")).isFalse();
    }
}
