package io.preboot.files.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class ErrorResponse {

    private final String error;
    private final String message;
    private final int status;
    private final String path;
    private final List<String> details;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private final Instant timestamp;

    public static ErrorResponse of(String error, String message, int status, String path) {
        return ErrorResponse.builder()
                .error(error)
                .message(message)
                .status(status)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }
}
