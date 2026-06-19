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
        String raw = loadTemplate(templateName);
        String rendered = applyVariables(raw, variables);
        String subject = "Ingoboka notification";
        String body = rendered;

        int subjectIndex = rendered.indexOf("Subject:");
        if (subjectIndex == 0) {
            int lineBreak = rendered.indexOf('\n');
            if (lineBreak > 0) {
                subject = rendered.substring("Subject:".length(), lineBreak).trim();
                body = rendered.substring(lineBreak + 1).trim();
            }
        }

        return new RenderedEmail(subject, body);
    }

    private String loadTemplate(String templateName) {
        String path = TEMPLATE_PREFIX + templateName + ".txt";
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
}
