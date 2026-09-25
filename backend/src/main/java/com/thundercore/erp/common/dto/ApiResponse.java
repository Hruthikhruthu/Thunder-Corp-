package com.thundercore.erp.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 * ApiResponse is the standard REST envelope returned by business endpoints.
 *
 * <p>Using one response shape keeps Axios handling predictable across modules
 * while still allowing binary report endpoints to return raw bytes.</p>
 */
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    /** Creates a successful response with an optional data payload. */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /** Creates an error response without a data payload. */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
