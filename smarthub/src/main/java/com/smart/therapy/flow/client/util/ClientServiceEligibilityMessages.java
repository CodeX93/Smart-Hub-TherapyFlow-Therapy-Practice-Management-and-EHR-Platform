package com.smart.therapy.flow.client.util;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * User-facing copy when a client cannot receive services (schedule / notes).
 * Avoids the misleading "inactive" wording for Pending and other statuses.
 */
@Component
public class ClientServiceEligibilityMessages {

    private final SystemOptionResolverService systemOptionResolverService;

    public ClientServiceEligibilityMessages(SystemOptionResolverService systemOptionResolverService) {
        this.systemOptionResolverService = systemOptionResolverService;
    }

    public String schedulingBlocked(Client client) {
        return blocked(client, "schedule sessions");
    }

    public String notesBlocked(Client client) {
        return blocked(client, "add or edit notes");
    }

    private String blocked(Client client, String action) {
        String key = resolveStatusKey(client);
        String label = resolveStatusLabel(key, client != null ? client.getStatus() : null);

        if (SystemOptionKeyMatcher.matchesAny(key, "pending")) {
            return "Cannot " + action + " while this client's status is Pending. "
                    + "Open Edit → Clinical tab and set Status to Active.";
        }
        if (SystemOptionKeyMatcher.matchesAny(key, "inactive")) {
            return "Cannot " + action + " for a closed (Inactive) client file. "
                    + "Use Open File, or open Edit → Clinical tab and set Status to Active.";
        }
        if (SystemOptionKeyMatcher.matchesAny(key, "discharged")) {
            return "Cannot " + action + " while this client's status is Discharged. "
                    + "Open Edit → Clinical tab and set Status to Active if care should resume.";
        }
        if (SystemOptionKeyMatcher.matchesAny(key, "waitlist")) {
            return "Cannot " + action + " while this client's status is Waitlist. "
                    + "Open Edit → Clinical tab and set Status to Active when ready to schedule.";
        }
        return "Cannot " + action + " while this client's status is " + label + ". "
                + "Open Edit → Clinical tab and set Status to Active.";
    }

    private String resolveStatusKey(Client client) {
        if (client == null || !StringUtils.hasText(client.getStatus())) {
            return "unknown";
        }
        String key = systemOptionResolverService.resolveOptionKey(
                SystemOptionCategories.CLIENT_STATUS, client.getStatus());
        if (StringUtils.hasText(key)) {
            return key.trim().toLowerCase(Locale.ROOT);
        }
        return client.getStatus().trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private String resolveStatusLabel(String key, String rawStatus) {
        String label = systemOptionResolverService.resolveOptionLabel(
                SystemOptionCategories.CLIENT_STATUS, key);
        if (StringUtils.hasText(label)) {
            return label;
        }
        if (StringUtils.hasText(rawStatus)) {
            return rawStatus.trim();
        }
        return "unknown";
    }
}
