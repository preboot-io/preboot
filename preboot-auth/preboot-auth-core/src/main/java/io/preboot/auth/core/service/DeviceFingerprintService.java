package io.preboot.auth.core.service;

import io.preboot.auth.api.dto.ClientInfo;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceFingerprintService {
    private final UserAgentAnalyzer uaa;

    public String generateFingerprint(HttpServletRequest request, ClientInfo clientInfo) {
        StringBuilder fingerprintBuilder = new StringBuilder();

        // Basic HTTP headers
        fingerprintBuilder
                .append(request.getHeader("User-Agent"))
                .append("|")
                .append(request.getHeader("Accept-Language"))
                .append("|")
                .append(request.getHeader("Sec-Ch-Ua-Platform"));

        // IP address (with proxy support)
        String ipAddress =
                Optional.ofNullable(request.getHeader("X-Forwarded-For")).orElse(request.getRemoteAddr());
        fingerprintBuilder.append("|").append(ipAddress);

        // Detailed User-Agent analysis
        UserAgent agent = uaa.parse(request.getHeader("User-Agent"));
        fingerprintBuilder
                .append("|")
                .append(agent.getValue(UserAgent.DEVICE_CLASS))
                .append("|")
                .append(agent.getValue(UserAgent.OPERATING_SYSTEM_NAME))
                .append("|")
                .append(agent.getValue(UserAgent.AGENT_NAME))
                .append("|")
                .append(agent.getValue(UserAgent.AGENT_VERSION));

        // Client-side collected info (if available)
        if (clientInfo != null) {
            fingerprintBuilder
                    .append("|")
                    .append(clientInfo.getScreenWidth())
                    .append("x")
                    .append(clientInfo.getScreenHeight())
                    .append("|")
                    .append(clientInfo.getTimezone()); // First 32 chars of canvas fingerprint

            // Canvas fingerprint may be shorter than expected or even null
            String canvas = clientInfo.getCanvas();
            if (canvas != null) {
                fingerprintBuilder.append("|");
                if (canvas.length() >= 32) {
                    fingerprintBuilder.append(canvas, 0, 32);
                } else {
                    fingerprintBuilder.append(canvas);
                }
            }
        }

        // Generate final hash
        return DigestUtils.sha256Hex(fingerprintBuilder.toString());
    }
}
