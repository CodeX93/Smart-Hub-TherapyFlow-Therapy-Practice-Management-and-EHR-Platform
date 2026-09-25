package com.smart.therapy.flow.unit.session;

import com.smart.therapy.flow.session.repository.SessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Session reminder query eager fetches")
class SessionReminderQueryFetchTest {

    @Test
    @DisplayName("findSessionsForReminder joins service and room to avoid LazyInitializationException")
    void reminderQueryFetchesAssociationsUsedByReminderPayload() throws Exception {
        Query query = SessionRepository.class
                .getMethod("findSessionsForReminder", Instant.class, Instant.class)
                .getAnnotation(Query.class);

        assertThat(query).isNotNull();
        String jpql = query.value();
        assertThat(jpql).containsIgnoringCase("JOIN FETCH s.client");
        assertThat(jpql).containsIgnoringCase("JOIN FETCH s.therapist");
        assertThat(jpql).containsIgnoringCase("JOIN FETCH s.service");
        assertThat(jpql).containsIgnoringCase("JOIN FETCH s.room");
    }

    @Test
    @DisplayName("findSessionsForReminder must not JOIN FETCH integrations — SELECT DISTINCT breaks on the json metadata column (no Postgres equality operator); SessionReminderService hydrates them separately")
    void reminderQueryDoesNotDistinctOverJsonMetadata() throws Exception {
        Query query = SessionRepository.class
                .getMethod("findSessionsForReminder", Instant.class, Instant.class)
                .getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value()).doesNotContainIgnoringCase("JOIN FETCH s.integrations");
    }
}
