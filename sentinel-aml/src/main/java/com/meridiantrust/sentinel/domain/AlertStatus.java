package com.meridiantrust.sentinel.domain;

/**
 * Alerts are never deleted - they only move forward through this state
 * machine, always leaving an audit trail behind them.
 */
public enum AlertStatus {
    NEW, IN_REVIEW, ESCALATED, CLEARED, SAR_FILED, CLOSED
}
