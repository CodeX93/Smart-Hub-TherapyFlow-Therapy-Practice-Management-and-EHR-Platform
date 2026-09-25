package com.smart.therapy.flow.unit.client.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.client.enums.ConsentType;
import com.smart.therapy.flow.client.portal.dto.ToggleConsentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConsentType deserialization")
class ConsentTypeTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("fromValue accepts enum constant names")
  void fromValueAcceptsEnumNames() {
    assertThat(ConsentType.fromValue("AI_PROCESSING")).isEqualTo(ConsentType.AI_PROCESSING);
    assertThat(ConsentType.fromValue("sms_communication")).isEqualTo(ConsentType.SMS_COMMUNICATION);
  }

  @Test
  @DisplayName("fromValue accepts display names")
  void fromValueAcceptsDisplayNames() {
    assertThat(ConsentType.fromValue("AI Processing Consent")).isEqualTo(ConsentType.AI_PROCESSING);
    assertThat(ConsentType.fromValue("sms communication")).isEqualTo(ConsentType.SMS_COMMUNICATION);
  }

  @Test
  @DisplayName("ToggleConsentRequest deserializes enum constant names")
  void toggleRequestDeserializesEnumName() throws Exception {
    ToggleConsentRequest request = objectMapper.readValue(
        """
            {
              "consentType": "AI_PROCESSING",
              "granted": false,
              "consentVersion": "1.0"
            }
            """,
        ToggleConsentRequest.class);

    assertThat(request.getConsentType()).isEqualTo(ConsentType.AI_PROCESSING);
    assertThat(request.getGranted()).isFalse();
  }

  @Test
  @DisplayName("fromValue rejects unknown values")
  void fromValueRejectsUnknown() {
    assertThatThrownBy(() -> ConsentType.fromValue("TWILIO"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
