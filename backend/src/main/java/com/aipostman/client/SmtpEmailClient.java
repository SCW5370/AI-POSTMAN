package com.aipostman.client;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailClient {

    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailClient.class);

    public boolean sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            JavaMailSender mailSender = createMailSenderFromEnv();
            String fromEmail = getSmtpValue("SMTP_USERNAME", "");
            if (fromEmail.isBlank()) {
                logger.error("SMTP 邮件发送失败: SMTP_USERNAME 未配置");
                return false;
            }

            // 创建邮件消息
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // 设置发件人、收件人、主题和内容
            helper.setFrom(fromEmail);  // 发件人邮箱
            helper.setTo(toEmail);  // 收件人邮箱
            helper.setSubject(subject);  // 邮件主题
            helper.setText(htmlContent, true);  // 邮件内容（true 表示支持 HTML）
            
            // 发送邮件
            mailSender.send(message);
            logger.info("SMTP 邮件发送成功到: {}", toEmail);
            return true;
        } catch (MessagingException e) {
            logger.error("SMTP 邮件发送失败: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("SMTP 邮件发送异常: {}", e.getMessage(), e);
            return false;
        }
    }

    private JavaMailSender createMailSenderFromEnv() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(getSmtpValue("SMTP_HOST", "smtp.qq.com"));
        sender.setPort(parsePort(getSmtpValue("SMTP_PORT", "587")));
        sender.setUsername(getSmtpValue("SMTP_USERNAME", ""));
        sender.setPassword(getSmtpValue("SMTP_PASSWORD", ""));

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        return sender;
    }

    private int parsePort(String rawPort) {
        try {
            return Integer.parseInt(rawPort.trim());
        } catch (Exception ex) {
            logger.warn("SMTP_PORT 非法，回退 587: {}", rawPort);
            return 587;
        }
    }

    private String getSmtpValue(String key, String fallback) {
        Path envPath = resolveProjectRoot().resolve(".env");
        if (Files.exists(envPath)) {
            try {
                List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                        continue;
                    }
                    int idx = trimmed.indexOf('=');
                    String lineKey = trimmed.substring(0, idx).trim();
                    if (!key.equals(lineKey)) {
                        continue;
                    }
                    String value = trimmed.substring(idx + 1).trim();
                    if (!value.isBlank()) {
                        return value;
                    }
                    break;
                }
            } catch (IOException e) {
                logger.warn("读取 .env 失败，使用系统环境变量或默认值: {}", e.getMessage());
            }
        }

        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        return fallback;
    }

    private Path resolveProjectRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        for (int i = 0; i < 5 && current != null; i++) {
            boolean hasProjectLayout = Files.isDirectory(current.resolve("backend"))
                    && Files.isDirectory(current.resolve("frontend"))
                    && Files.isDirectory(current.resolve("worker"));
            if (hasProjectLayout) {
                return current;
            }
            current = current.getParent();
        }

        current = Paths.get("").toAbsolutePath().normalize();
        for (int i = 0; i < 5 && current != null; i++) {
            if (Files.exists(current.resolve(".env")) || Files.exists(current.resolve(".env.example"))) {
                return current;
            }
            current = current.getParent();
        }
        return Paths.get("").toAbsolutePath().normalize();
    }
}
