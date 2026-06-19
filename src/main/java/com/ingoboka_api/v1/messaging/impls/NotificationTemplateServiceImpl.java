package com.ingoboka_api.v1.messaging.impls;

import com.ingoboka_api.v1.common.enums.NotificationChannel;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.messaging.models.NotificationTemplate;
import com.ingoboka_api.v1.messaging.repositories.NotificationTemplateRepository;
import com.ingoboka_api.v1.messaging.services.NotificationTemplateService;
import com.ingoboka_api.v1.messaging.services.SmsDeliveryService;
import com.ingoboka_api.v1.messaging.services.UserNotificationService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private final NotificationTemplateRepository notificationTemplateRepository;
    private final UserNotificationService userNotificationService;
    private final JavaMailSender mailSender;
    private final SmsDeliveryService smsDeliveryService;

    @Override
    public void sendTemplated(
            UUID userId,
            UUID organizationId,
            String templateCode,
            NotificationChannel channel,
            String recipientEmail,
            Map<String, String> variables) {
        NotificationTemplate template = notificationTemplateRepository
                .findByCodeAndActiveTrue(templateCode)
                .orElseThrow(() -> new BusinessException("Notification template not found: " + templateCode));

        String subject = render(template.getSubjectTemplate(), variables);
        String body = render(template.getBodyTemplate(), variables);

        userNotificationService.dispatch(userId, organizationId, channel, templateCode, subject, body);

        if (channel == NotificationChannel.EMAIL && recipientEmail != null && !recipientEmail.isBlank()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(recipientEmail);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
            } catch (Exception ex) {
                log.warn("Email delivery failed for {}: {}", templateCode, ex.getMessage());
            }
        } else if (channel == NotificationChannel.SMS) {
            String phone = recipientEmail;
            if (phone != null && !phone.isBlank()) {
                smsDeliveryService.send(phone, body);
            } else {
                log.warn("SMS skipped for {} — no phone recipient", templateCode);
            }
        }
    }

    private String render(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }
}
