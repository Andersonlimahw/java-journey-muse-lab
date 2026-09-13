package com.muse.journey.shared;

import java.time.LocalDateTime;

/**
 * Shared kernel: uniform error payload returned by every vertical slice.
 */
public record ApiError(String error, int status, LocalDateTime timestamp) {

    public static ApiError of(String error, int status) {
        return new ApiError(error, status, LocalDateTime.now());
    }
}
