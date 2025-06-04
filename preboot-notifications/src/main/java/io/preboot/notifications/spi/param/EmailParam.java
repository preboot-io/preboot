package io.preboot.notifications.spi.param;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class EmailParam {
    @NotBlank
    @Email
    private String senderEmail;

    @NotBlank
    @Email
    private String receiverEmail;

    @Email
    private String copyToEmail;

    @NotBlank
    private String template;

    @NotBlank
    private String subject;

    @Setter(AccessLevel.NONE)
    @Builder.Default
    private Map<String, Object> templateParams = new HashMap<>();

    @Setter(AccessLevel.NONE)
    @Builder.Default
    private Map<String, byte[]> attachments = new HashMap<>();

    public EmailParam addTemplateParam(String key, String value) {
        templateParams.put(key, value);
        return this;
    }

    public boolean hasAttachments() {
        return !CollectionUtils.isEmpty(attachments);
    }

    // This makes username required in the builder
    public static EmailParamBuilder builder(String from, String to, String subject, String template) {
        return new EmailParamBuilder()
                .senderEmail(from)
                .receiverEmail(to)
                .template(template)
                .subject(subject);
    }
}
