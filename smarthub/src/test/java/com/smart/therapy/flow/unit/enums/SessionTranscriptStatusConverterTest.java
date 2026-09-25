package com.smart.therapy.flow.unit.enums;

import com.smart.therapy.flow.session.enums.SessionTranscriptStatus;
import com.smart.therapy.flow.session.enums.SessionTranscriptStatusConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionTranscriptStatusConverterTest {

    private SessionTranscriptStatusConverter converter;

    @BeforeEach
    void setUp() {
        converter = new SessionTranscriptStatusConverter();
    }

    @Test
    @DisplayName("Should persist lowercase API values")
    void shouldPersistLowercaseValues() {
        assertThat(converter.convertToDatabaseColumn(SessionTranscriptStatus.READY)).isEqualTo("ready");
        assertThat(converter.convertToDatabaseColumn(SessionTranscriptStatus.RECORDING)).isEqualTo("recording");
        assertThat(converter.convertToDatabaseColumn(SessionTranscriptStatus.PROCESSING)).isEqualTo("processing");
    }

    @Test
    @DisplayName("Should read lowercase values from database")
    void shouldReadLowercaseValues() {
        assertThat(converter.convertToEntityAttribute("ready")).isEqualTo(SessionTranscriptStatus.READY);
        assertThat(converter.convertToEntityAttribute("recording")).isEqualTo(SessionTranscriptStatus.RECORDING);
        assertThat(converter.convertToEntityAttribute("processing")).isEqualTo(SessionTranscriptStatus.PROCESSING);
    }

    @Test
    @DisplayName("Should map legacy database values to new enum constants")
    void shouldMapLegacyValues() {
        assertThat(converter.convertToEntityAttribute("COMPLETED")).isEqualTo(SessionTranscriptStatus.READY);
        assertThat(converter.convertToEntityAttribute("completed")).isEqualTo(SessionTranscriptStatus.READY);
        assertThat(converter.convertToEntityAttribute("STARTED")).isEqualTo(SessionTranscriptStatus.RECORDING);
        assertThat(converter.convertToEntityAttribute("UPLOADING")).isEqualTo(SessionTranscriptStatus.RECORDING);
        assertThat(converter.convertToEntityAttribute("FINALIZING")).isEqualTo(SessionTranscriptStatus.PROCESSING);
    }

    @Test
    @DisplayName("Should reject unknown database values")
    void shouldRejectUnknownValues() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
