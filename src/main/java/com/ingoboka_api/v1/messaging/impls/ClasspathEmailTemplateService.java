package com.ingoboka_api.v1.messaging.impls;

import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.messaging.services.EmailTemplateService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Slf4j
@Service
public class ClasspathEmailTemplateService implements EmailTemplateService {

    private static final String TEMPLATE_PREFIX = "templates/email/";

    @Override
    public RenderedEmail render(String templateName, Map<String, String> variables) {
        String textRaw = loadTemplate(templateName, ".txt");
        ParsedTemplate textTemplate = parseTemplate(textRaw);

        String htmlBody = textTemplate.body();
        try {
            String htmlRaw = loadTemplate(templateName, ".html");
            htmlBody = parseTemplate(htmlRaw).body();
        } catch (BusinessException ex) {
            log.debug("No HTML template for {}, using plain text only", templateName);
        }

        String subject = applyVariables(textTemplate.subject(), variables);
        htmlBody = applyVariables(htmlBody, variables);
        String textBody = applyVariables(textTemplate.body(), variables);

        return new RenderedEmail(subject, htmlBody, textBody);
    }

    private ParsedTemplate parseTemplate(String raw) {
        String subject = "Ingoboka notification";
        String body = raw;

        if (raw.startsWith("Subject:")) {
            int lineBreak = raw.indexOf('\n');
            if (lineBreak > 0) {
                subject = raw.substring("Subject:".length(), lineBreak).trim();
                body = raw.substring(lineBreak + 1).trim();
            }
        }

        return new ParsedTemplate(subject, body);
    }

    private String loadTemplate(String templateName, String extension) {
        String path = TEMPLATE_PREFIX + templateName + extension;
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                throw new BusinessException("Email template not found: " + templateName);
            }
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Failed to load email template {}", path, ex);
            throw new BusinessException("Failed to load email template: " + templateName);
        }
    }

    private String applyVariables(String template, Map<String, String> variables) {
        String result = template;
        if (variables == null) {
            return result;
        }
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace("{{" + entry.getKey() + "}}", value);
        }
        return result;
    }

    private record ParsedTemplate(String subject, String body) {}
}
