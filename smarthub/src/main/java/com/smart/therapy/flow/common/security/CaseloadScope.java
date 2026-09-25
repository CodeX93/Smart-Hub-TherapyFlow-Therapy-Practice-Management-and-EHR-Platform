package com.smart.therapy.flow.common.security;

/**
 * Effective clinical caseload visibility for a requester.
 */
public enum CaseloadScope {
    ALL,
    TEAM,
    OWN,
    TEAM_AND_OWN,
    NONE
}
