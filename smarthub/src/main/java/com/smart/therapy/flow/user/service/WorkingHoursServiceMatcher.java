package com.smart.therapy.flow.user.service;

import com.smart.therapy.flow.billing.entity.Service;
import com.smart.therapy.flow.user.entity.UserProfileWorkingHours;

/**
 * Decides which availability rows a booking is allowed to use.
 * <p>
 * A therapist keeps two separate schedules: the clinical one (rows with no service,
 * used for ordinary bookings) and per-service ones such as the consultation /
 * public-site schedule. Mixing them lets marketing-site hours leak into clinical
 * booking, so every caller that turns availability into slots must filter through here.
 */
public final class WorkingHoursServiceMatcher {

    private WorkingHoursServiceMatcher() {
    }

    public static boolean matches(UserProfileWorkingHours hours, Service service) {
        Long hoursServiceId = hours.getService() != null ? hours.getService().getId() : null;
        if (service == null) {
            return hoursServiceId == null;
        }
        boolean serviceScoped = Boolean.TRUE.equals(service.getPublicSiteEnabled())
                || "CONSULTATION".equalsIgnoreCase(service.getServiceCode());
        if (serviceScoped) {
            return hoursServiceId != null && hoursServiceId.equals(service.getId());
        }
        return hoursServiceId == null;
    }
}
