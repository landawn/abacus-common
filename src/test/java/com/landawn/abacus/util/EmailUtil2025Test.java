package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class EmailUtil2025Test extends TestBase {

    private Properties props;
    private File tempFile;

    @BeforeEach
    public void setUp() throws IOException {
        props = new Properties();
        props.put("mail.smtp.host", "localhost");
        props.put("mail.smtp.port", "25");
        props.put("mail.smtp.auth", "false");

        tempFile = File.createTempFile("email_test_", ".txt");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write("Test attachment content".getBytes());
        }
    }

    @AfterEach
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    public void test_sendEmail_singleRecipient() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmail(new String[] { "test@example.com" }, "sender@example.com", "Test Subject", "Test plain text content", "username", "password",
                    props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendEmail_multipleRecipients() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmail(new String[] { "test1@example.com", "test2@example.com", "test3@example.com" }, "sender@example.com", "Test Subject",
                    "Test plain text content for multiple recipients", "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
        assertTrue(exception.getMessage().contains("test1@example.com"));
    }

    @Test
    public void test_sendEmail_emptyContent() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmail(new String[] { "test@example.com" }, "sender@example.com", "Empty Content Test", "", "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendEmail_specialCharactersInSubject() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmail(new String[] { "test@example.com" }, "sender@example.com", "Subject with special chars: <>&\"'", "Test content", "username",
                    "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendEmail_unicodeContent() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmail(new String[] { "test@example.com" }, "sender@example.com", "Unicode Test", "Test with unicode: 你好世界 こんにちは مرحبا", "username",
                    "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendEmailWithAttachment_singleAttachment() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmailWithAttachment(new String[] { "test@example.com" }, "sender@example.com", "Test with Attachment",
                    "Please see the attached file.", new String[] { tempFile.getAbsolutePath() }, "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendEmailWithAttachment_multipleAttachments() throws IOException {
        File tempFile2 = File.createTempFile("email_test_2_", ".pdf");
        try {
            try (FileOutputStream fos = new FileOutputStream(tempFile2)) {
                fos.write("Test PDF content".getBytes());
            }

            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                EmailUtil.sendEmailWithAttachment(new String[] { "test@example.com" }, "sender@example.com", "Test with Multiple Attachments",
                        "Please see the attached files.", new String[] { tempFile.getAbsolutePath(), tempFile2.getAbsolutePath() }, "username", "password",
                        props);
            });
            assertTrue(exception.getMessage().contains("Failed to send email"));
        } finally {
            if (tempFile2.exists()) {
                tempFile2.delete();
            }
        }
    }

    @Test
    public void test_sendEmailWithAttachment_nullAttachments() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmailWithAttachment(new String[] { "test@example.com" }, "sender@example.com", "Test without Attachments",
                    "No attachments in this email.", null, "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendEmailWithAttachment_emptyAttachmentsArray() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmailWithAttachment(new String[] { "test@example.com" }, "sender@example.com", "Test with Empty Attachments Array",
                    "Empty attachments array.", new String[] {}, "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendEmailWithAttachment_windowsPath() throws IOException {
        File windowsFile = new File("C:\\temp\\test.txt");
        String windowsPath = windowsFile.getAbsolutePath();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmailWithAttachment(new String[] { "test@example.com" }, "sender@example.com", "Test Windows Path", "Testing Windows path handling.",
                    new String[] { windowsPath }, "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendEmailWithAttachment_unixPath() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmailWithAttachment(new String[] { "test@example.com" }, "sender@example.com", "Test Unix Path", "Testing Unix path handling.",
                    new String[] { "/tmp/test/file.txt" }, "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendEmailWithAttachment_pathWithSpaces() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmailWithAttachment(new String[] { "test@example.com" }, "sender@example.com", "Test Path with Spaces", "Testing path with spaces.",
                    new String[] { "C:\\Program Files\\My Documents\\test file.txt" }, "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendHtmlEmail_basicHTML() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendHtmlEmail(new String[] { "test@example.com" }, "sender@example.com", "HTML Email Test",
                    "<h1>Hello</h1><p>This is an <strong>HTML</strong> email.</p>", "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendHtmlEmail_complexHTML() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            String htmlContent = "<html><body>" + "<h2>Welcome!</h2>" + "<table border='1'>" + "<tr><th>Name</th><th>Email</th></tr>"
                    + "<tr><td>John</td><td>john@example.com</td></tr>" + "</table>" + "<a href='https://example.com'>Click here</a>" + "</body></html>";

            EmailUtil.sendHtmlEmail(new String[] { "test@example.com" }, "sender@example.com", "Complex HTML Email", htmlContent, "username", "password",
                    props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendHtmlEmail_multipleRecipients() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendHtmlEmail(new String[] { "test1@example.com", "test2@example.com" }, "sender@example.com", "HTML Email to Multiple Recipients",
                    "<h1>Hello All</h1><p>This is a test.</p>", "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendHtmlEmail_withInlineCSS() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            String htmlWithCSS = "<html>" + "<head><style>body { font-family: Arial; } h1 { color: blue; }</style></head>"
                    + "<body><h1>Styled Email</h1><p>With CSS</p></body>" + "</html>";

            EmailUtil.sendHtmlEmail(new String[] { "test@example.com" }, "sender@example.com", "Styled HTML Email", htmlWithCSS, "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendHtmlEmail_emptyHTML() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendHtmlEmail(new String[] { "test@example.com" }, "sender@example.com", "Empty HTML", "", "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendHtmlEmailWithAttachment_singleAttachment() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendHtmlEmailWithAttachment(new String[] { "test@example.com" }, "sender@example.com", "HTML Email with Attachment",
                    "<h1>Invoice</h1><p>Please find the invoice attached.</p>", new String[] { tempFile.getAbsolutePath() }, "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendHtmlEmailWithAttachment_multipleAttachments() throws IOException {
        File tempFile2 = File.createTempFile("email_test_3_", ".doc");
        try {
            try (FileOutputStream fos = new FileOutputStream(tempFile2)) {
                fos.write("Test document content".getBytes());
            }

            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                EmailUtil.sendHtmlEmailWithAttachment(new String[] { "test@example.com" }, "sender@example.com", "HTML Email with Multiple Attachments",
                        "<html><body><h2>Documents</h2><p>Please review the attached documents.</p></body></html>",
                        new String[] { tempFile.getAbsolutePath(), tempFile2.getAbsolutePath() }, "username", "password", props);
            });
            assertTrue(exception.getMessage().contains("Failed to send email"));
        } finally {
            if (tempFile2.exists()) {
                tempFile2.delete();
            }
        }
    }

    @Test
    public void test_sendHtmlEmailWithAttachment_nullAttachments() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendHtmlEmailWithAttachment(new String[] { "test@example.com" }, "sender@example.com", "HTML Email without Attachments",
                    "<h1>Hello</h1><p>No attachments here.</p>", null, "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendHtmlEmailWithAttachment_emptyAttachmentsArray() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendHtmlEmailWithAttachment(new String[] { "test@example.com" }, "sender@example.com", "HTML Email with Empty Attachments",
                    "<h1>Test</h1>", new String[] {}, "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendHtmlEmailWithAttachment_complexHTMLAndMultipleAttachments() throws IOException {
        File pdfFile = File.createTempFile("report_", ".pdf");
        File excelFile = File.createTempFile("data_", ".xlsx");

        try {
            try (FileOutputStream pdfFos = new FileOutputStream(pdfFile); FileOutputStream excelFos = new FileOutputStream(excelFile)) {
                pdfFos.write("PDF report content".getBytes());
                excelFos.write("Excel data content".getBytes());
            }

            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                String complexHTML = "<html><head><style>" + "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }"
                        + "table { border-collapse: collapse; width: 100%; }" + "th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }"
                        + "th { background-color: #4CAF50; color: white; }" + "</style></head><body>" + "<h1>Monthly Report</h1>" + "<p>Dear Team,</p>"
                        + "<p>Please find the monthly report and data files attached.</p>" + "<table>" + "<tr><th>Metric</th><th>Value</th></tr>"
                        + "<tr><td>Revenue</td><td>$100,000</td></tr>" + "<tr><td>Growth</td><td>15%</td></tr>" + "</table>"
                        + "<p>Best regards,<br>Management</p>" + "</body></html>";

                EmailUtil.sendHtmlEmailWithAttachment(new String[] { "team@example.com", "manager@example.com" }, "reports@example.com",
                        "Monthly Report - November 2025", complexHTML, new String[] { pdfFile.getAbsolutePath(), excelFile.getAbsolutePath() }, "report_user",
                        "report_password", props);
            });
            assertTrue(exception.getMessage().contains("Failed to send email"));
        } finally {
            if (pdfFile.exists())
                pdfFile.delete();
            if (excelFile.exists())
                excelFile.delete();
        }
    }

    @Test
    public void test_sendEmail_withAuthenticationEnabled() {
        Properties authProps = new Properties();
        authProps.put("mail.smtp.host", "smtp.gmail.com");
        authProps.put("mail.smtp.port", "587");
        authProps.put("mail.smtp.auth", "true");
        authProps.put("mail.smtp.starttls.enable", "true");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmail(new String[] { "test@example.com" }, "sender@gmail.com", "Test with Auth", "Testing SMTP authentication", "real_username",
                    "real_password", authProps);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendEmail_withSSL() {
        Properties sslProps = new Properties();
        sslProps.put("mail.smtp.host", "smtp.gmail.com");
        sslProps.put("mail.smtp.port", "465");
        sslProps.put("mail.smtp.auth", "true");
        sslProps.put("mail.smtp.socketFactory.port", "465");
        sslProps.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmail(new String[] { "test@example.com" }, "sender@gmail.com", "Test with SSL", "Testing SSL connection", "username", "password",
                    sslProps);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendEmail_veryLongContent() {
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longContent.append("This is line ").append(i).append(". ");
        }

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmail(new String[] { "test@example.com" }, "sender@example.com", "Very Long Email", longContent.toString(), "username", "password",
                    props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendEmail_specialEmailAddresses() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmail(new String[] { "user+tag@example.com", "user.name@example.co.uk" }, "sender.name+tag@example.com", "Special Email Addresses",
                    "Testing special characters in email addresses", "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendHtmlEmail_scriptTags() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            String htmlWithScript = "<html><body>" + "<h1>Test</h1>" + "<script>alert('test');</script>" + "<p>Content</p>" + "</body></html>";

            EmailUtil.sendHtmlEmail(new String[] { "test@example.com" }, "sender@example.com", "HTML with Script", htmlWithScript, "username", "password",
                    props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendEmailWithAttachment_filenameWithoutPath() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmailWithAttachment(new String[] { "test@example.com" }, "sender@example.com", "Test Simple Filename",
                    "Testing filename without path", new String[] { "simple_file.txt" }, "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendEmailWithAttachment_filenameExtractionWindows() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmailWithAttachment(new String[] { "test@example.com" }, "sender@example.com", "Test Filename Extraction",
                    "Testing filename extraction from path", new String[] { "C:\\Users\\Documents\\My Files\\report.pdf" }, "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }

    @Test
    public void test_sendEmailWithAttachment_filenameExtractionUnix() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            EmailUtil.sendEmailWithAttachment(new String[] { "test@example.com" }, "sender@example.com", "Test Filename Extraction Unix",
                    "Testing filename extraction from Unix path", new String[] { "/home/user/documents/my files/report.pdf" }, "username", "password", props);
        });
        assertTrue(exception.getMessage().contains("Failed to send email"));
    }
}
