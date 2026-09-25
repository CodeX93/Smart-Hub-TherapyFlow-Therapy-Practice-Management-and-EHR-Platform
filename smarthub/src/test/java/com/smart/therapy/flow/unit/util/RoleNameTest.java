package com.smart.therapy.flow.unit.util;

import com.smart.therapy.flow.common.util.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RoleName Enum Tests")
class RoleNameTest {

    @Test
    @DisplayName("Should have all required role names")
    void shouldHaveAllRequiredRoleNames() {
        // Assert
        assertThat(RoleName.values()).contains(
                RoleName.ADMIN,
                RoleName.SUPERVISOR,
                RoleName.THERAPIST,
                RoleName.BILLING_SPECIALIST,
                RoleName.CLIENT,
                RoleName.SUPER_ADMIN
        );
    }

    @Test
    @DisplayName("Should convert role name to string correctly")
    void shouldConvertRoleNameToStringCorrectly() {
        // Act & Assert
        assertThat(RoleName.ADMIN.name()).isEqualTo("ADMIN");
        assertThat(RoleName.THERAPIST.name()).isEqualTo("THERAPIST");
        assertThat(RoleName.SUPERVISOR.name()).isEqualTo("SUPERVISOR");
    }

    @Test
    @DisplayName("Should parse role name from string")
    void shouldParseRoleNameFromString() {
        // Act & Assert
        assertThat(RoleName.valueOf("ADMIN")).isEqualTo(RoleName.ADMIN);
        assertThat(RoleName.valueOf("THERAPIST")).isEqualTo(RoleName.THERAPIST);
        assertThat(RoleName.valueOf("SUPERVISOR")).isEqualTo(RoleName.SUPERVISOR);
    }

    @Test
    @DisplayName("Should have consistent enum values")
    void shouldHaveConsistentEnumValues() {
        // Act
        RoleName[] values = RoleName.values();

        // Assert
        assertThat(values).isNotEmpty();
        assertThat(values.length).isGreaterThanOrEqualTo(4); // At least ADMIN, THERAPIST, SUPERVISOR, CLIENT
    }
}

