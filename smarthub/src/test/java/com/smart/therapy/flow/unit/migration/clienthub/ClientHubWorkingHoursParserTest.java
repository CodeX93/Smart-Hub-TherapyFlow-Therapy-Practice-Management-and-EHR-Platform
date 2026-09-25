package com.smart.therapy.flow.unit.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubWorkingHoursParser;
import com.smart.therapy.flow.migration.clienthub.ClientHubWorkingHoursParser.ParsedShift;
import com.smart.therapy.flow.user.entity.ShiftMode;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientHubWorkingHoursParserTest {

    @Test
    void parsesObjectMapFormatFromV1Ui() {
        List<ParsedShift> shifts = ClientHubWorkingHoursParser.parse("""
                {"Monday":{"start":"09:00","end":"17:00"},"Wednesday":{"start":"10:00","end":"14:30"}}
                """);

        assertThat(shifts).containsExactly(
                new ParsedShift("MONDAY", LocalTime.of(9, 0), LocalTime.of(17, 0), ShiftMode.BOTH),
                new ParsedShift("WEDNESDAY", LocalTime.of(10, 0), LocalTime.of(14, 30), ShiftMode.BOTH));
    }

    @Test
    void parsesArrayFormatWithMode() {
        List<ParsedShift> shifts = ClientHubWorkingHoursParser.parse("""
                [
                  {"day":"Tuesday","start":"08:00","end":"12:00","enabled":true,"mode":"virtual"},
                  {"day":"Tuesday","start":"13:00","end":"17:00","enabled":false}
                ]
                """);

        assertThat(shifts).containsExactly(
                new ParsedShift("TUESDAY", LocalTime.of(8, 0), LocalTime.of(12, 0), ShiftMode.VIRTUAL));
    }

    @Test
    void fallsBackFromWorkingDays() {
        List<ParsedShift> shifts = ClientHubWorkingHoursParser.fromWorkingDaysFallback(List.of("Friday", "bad"));

        assertThat(shifts).containsExactly(
                new ParsedShift("FRIDAY", LocalTime.of(9, 0), LocalTime.of(17, 0), ShiftMode.BOTH));
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> ClientHubWorkingHoursParser.parse("{not-json"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
