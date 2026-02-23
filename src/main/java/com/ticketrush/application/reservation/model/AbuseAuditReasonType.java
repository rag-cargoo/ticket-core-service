package com.ticketrush.application.reservation.model;

public enum AbuseAuditReasonType {
    NONE,
    RATE_LIMIT_EXCEEDED,
    DUPLICATE_REQUEST_FINGERPRINT,
    DEVICE_FINGERPRINT_MULTI_ACCOUNT
}
