package com.ingoboka_api.v1.messaging.services;

import java.util.Map;

public interface EmailTemplateService {

    RenderedEmail render(String templateName, Map<String, String> variables);

    record RenderedEmail(String subject, String body) {}
}
