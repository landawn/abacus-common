package com.landawn.abacus.util;

import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class EmailUtil100Test extends TestBase {

    private Properties props;

    @BeforeEach
    public void setUp() {
        props = new Properties();
        props.put("mail.smtp.host", "localhost");
        props.put("mail.smtp.port", "25");
        props.put("mail.smtp.auth", "false");
    }

    @Test
    public void testSendEmail() {
        try {
            EmailUtil.sendEmail(new String[] { "test@example.com" }, "sender@example.com", "Test Subject", "Test Content", "username", "password", props);
        } catch (RuntimeException e) {
            Assertions.assertTrue(e.getMessage().contains("Failed to send email"));
        }
    }

    @Test
    public void testSendEmailWithAttachment() {
        try {
            EmailUtil.sendEmailWithAttachment(new String[] { "test@example.com" }, "sender@example.com", "Test Subject", "Test Content",
                    new String[] { "test.txt" }, "username", "password", props);
        } catch (RuntimeException e) {
            Assertions.assertTrue(e.getMessage().contains("Failed to send email"));
        }
    }

    @Test
    public void testSendHTMLEmail() {
        try {
            EmailUtil.sendHTMLEmail(new String[] { "test@example.com" }, "sender@example.com", "Test Subject", "<h1>Test HTML</h1>", "username", "password",
                    props);
        } catch (RuntimeException e) {
            Assertions.assertTrue(e.getMessage().contains("Failed to send email"));
        }
    }

    @Test
    public void testSendHTMLEmailWithAttachment() {
        try {
            EmailUtil.sendHTMLEmailWithAttachment(new String[] { "test@example.com" }, "sender@example.com", "Test Subject", "<h1>Test HTML</h1>",
                    new String[] { "test.txt" }, "username", "password", props);
        } catch (RuntimeException e) {
            Assertions.assertTrue(e.getMessage().contains("Failed to send email"));
        }
    }
}
