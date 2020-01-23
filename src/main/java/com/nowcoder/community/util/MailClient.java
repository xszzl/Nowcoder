package com.nowcoder.community.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

// 表示通用的Bean
@Component
public class MailClient {
    private static final Logger logger = LoggerFactory.getLogger(MailClient.class);

    // 用于发生邮件
    @Autowired
    private JavaMailSender mailSender;

    // 将发送人注入到bean中
    @Value("${spring.mail.username}")
    private String from;

    public void sendMail(String to,String subject,String content){
        // 构建MimeMessage，即邮件主体
        try{
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom(from);
            helper.setTo(to);
            // 设置主题
            helper.setSubject(subject);
            // 设置内容
            helper.setText(content,true);
            mailSender.send(helper.getMimeMessage());
        } catch (MessagingException e) {
            logger.error("发送邮件失败："+ e.getMessage());
        }
    }
}
