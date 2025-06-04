package io.preboot.auth.api.dto;

import java.util.List;
import lombok.Data;

@Data
public class ClientInfo {
    private String userAgent;
    private String platform;
    private String language;
    private int screenWidth;
    private int screenHeight;
    private String timezone;
    private List<String> plugins;
    private boolean cookiesEnabled;
    private String canvas; // Canvas fingerprint
    private String webgl; // WebGL fingerprint
}
