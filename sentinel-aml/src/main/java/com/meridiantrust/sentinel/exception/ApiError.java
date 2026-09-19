package com.meridiantrust.sentinel.exception;

import java.time.LocalDateTime;

/** Consistent JSON error envelope for every API error response. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
