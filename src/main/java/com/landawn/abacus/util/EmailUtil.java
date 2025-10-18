/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Utility class for sending emails using JavaMail API.
 * Provides convenient methods for sending plain text and HTML emails with or without attachments.
 * 
 * <p>This class supports SMTP authentication and can be configured with various mail server properties.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Configure mail server properties
 * Properties props = new Properties();
 * props.put("mail.smtp.host", "smtp.gmail.com");
 * props.put("mail.smtp.port", "587");
 * props.put("mail.smtp.auth", "true");
 * props.put("mail.smtp.starttls.enable", "true");
 * 
 * // Send plain text email
 * EmailUtil.sendEmail(
 *     new String[]{"recipient@example.com"},
 *     "sender@example.com",
 *     "Test Subject",
 *     "Hello, this is a test email!",
 *     "username",
 *     "password",
 *     props
 * );
 * 
 * // Send HTML email with attachment
 * EmailUtil.sendHTMLEmailWithAttachment(
 *     new String[]{"recipient@example.com"},
 *     "sender@example.com",
 *     "HTML Email",
 *     "<h1>Hello</h1><p>This is an HTML email!</p>",
 *     new String[]{"attachment.pdf"},
 *     "username",
 *     "password",
 *     props
 * );
 * }</pre>
 */
public final class EmailUtil {

    private EmailUtil() {
        // singleton.
    }

    /**
     * Sends a plain text email to the specified recipients.
     *
     * <p>Example usage:</p></p>
     * <pre>{@code
     * Properties props = new Properties();
     * props.put("mail.smtp.host", "smtp.example.com");
     * props.put("mail.smtp.auth", "true");
     * 
     * EmailUtil.sendEmail(
     *     new String[]{"user1@example.com", "user2@example.com"},
     *     "sender@example.com",
     *     "Meeting Reminder",
     *     "Don't forget about tomorrow's meeting at 10 AM",
     *     "smtp_username",
     *     "smtp_password",
     *     props
     * );
     * }</pre>
     *
     * @param recipients array of email addresses to send the email to
     * @param from the sender's email address
     * @param subject the email subject
     * @param content the plain text content of the email
     * @param userName the username for SMTP authentication
     * @param password the password for SMTP authentication
     * @param props mail server properties (e.g., mail.smtp.host, mail.smtp.port)
     * @throws RuntimeException if sending the email fails
     */
    public static void sendEmail(final String[] recipients, final String from, final String subject, final String content, final String userName,
            final String password, final Properties props) {
        sendEmailWithAttachment(recipients, from, subject, content, null, userName, password, props);
    }

    /**
     * Sends a plain text email with file attachments to the specified recipients.
     *
     * <p>Example usage:</p></p>
     * <pre>{@code
     * EmailUtil.sendEmailWithAttachment(
     *     new String[]{"recipient@example.com"},
     *     "sender@example.com",
     *     "Monthly Report",
     *     "Please find the monthly report attached.",
     *     new String[]{"/path/to/report.pdf", "/path/to/data.xlsx"},
     *     "username",
     *     "password",
     *     mailProperties
     * );
     * }</pre>
     *
     * @param recipients array of email addresses to send the email to
     * @param from the sender's email address
     * @param subject the email subject
     * @param content the plain text content of the email
     * @param attachedFiles array of file paths to attach, or null if no attachments
     * @param userName the username for SMTP authentication
     * @param password the password for SMTP authentication
     * @param props mail server properties (e.g., mail.smtp.host, mail.smtp.port)
     * @throws RuntimeException if sending the email fails
     */
    public static void sendEmailWithAttachment(final String[] recipients, final String from, final String subject, final String content,
            final String[] attachedFiles, final String userName, final String password, final Properties props) {
        send(recipients, from, subject, content, attachedFiles, false, userName, password, props);
    }

    /**
     * Sends an HTML email to the specified recipients.
     *
     * <p>Example usage:</p></p>
     * <pre>{@code
     * String htmlContent = "<html><body>" +
     *     "<h2>Welcome!</h2>" +
     *     "<p>Thank you for <b>registering</b>.</p>" +
     *     "<a href='https://example.com'>Visit our website</a>" +
     *     "</body></html>";
     * 
     * EmailUtil.sendHTMLEmail(
     *     new String[]{"newuser@example.com"},
     *     "noreply@example.com",
     *     "Welcome to Our Service",
     *     htmlContent,
     *     "smtp_user",
     *     "smtp_pass",
     *     props
     * );
     * }</pre>
     *
     * @param recipients array of email addresses to send the email to
     * @param from the sender's email address
     * @param subject the email subject
     * @param content the HTML content of the email
     * @param userName the username for SMTP authentication
     * @param password the password for SMTP authentication
     * @param props mail server properties (e.g., mail.smtp.host, mail.smtp.port)
     * @throws RuntimeException if sending the email fails
     */
    public static void sendHTMLEmail(final String[] recipients, final String from, final String subject, final String content, final String userName,
            final String password, final Properties props) {
        sendHTMLEmailWithAttachment(recipients, from, subject, content, null, userName, password, props);
    }

    /**
     * Sends an HTML email with file attachments to the specified recipients.
     *
     * <p>Example usage:</p></p>
     * <pre>{@code
     * String htmlContent = "<html><body>" +
     *     "<h1>Invoice</h1>" +
     *     "<p>Please find your invoice attached.</p>" +
     *     "<table border='1'>" +
     *     "<tr><th>Item</th><th>Price</th></tr>" +
     *     "<tr><td>Product A</td><td>$100</td></tr>" +
     *     "</table>" +
     *     "</body></html>";
     * 
     * EmailUtil.sendHTMLEmailWithAttachment(
     *     new String[]{"customer@example.com"},
     *     "billing@example.com",
     *     "Invoice #12345",
     *     htmlContent,
     *     new String[]{"/path/to/invoice.pdf"},
     *     "smtp_user",
     *     "smtp_pass",
     *     props
     * );
     * }</pre>
     *
     * @param recipients array of email addresses to send the email to
     * @param from the sender's email address
     * @param subject the email subject
     * @param content the HTML content of the email
     * @param attachedFiles array of file paths to attach, or null if no attachments
     * @param userName the username for SMTP authentication
     * @param password the password for SMTP authentication
     * @param props mail server properties (e.g., mail.smtp.host, mail.smtp.port)
     * @throws RuntimeException if sending the email fails
     */
    public static void sendHTMLEmailWithAttachment(final String[] recipients, final String from, final String subject, final String content,
            final String[] attachedFiles, final String userName, final String password, final Properties props) {
        send(recipients, from, subject, content, attachedFiles, true, userName, password, props);
    }

    private static void send(final String[] recipients, final String from, final String subject, final String content, final String[] attachedFiles,
            final boolean isHTML, final String userName, final String password, final Properties props) {

        try {
            final Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userName, password);
                }
            });

            final MimeMessage mail = new MimeMessage(session);

            final InternetAddress[] to = new InternetAddress[recipients.length];

            for (int i = 0; i < recipients.length; i++) {
                to[i] = new InternetAddress(recipients[i]);
            }

            mail.setRecipients(Message.RecipientType.TO, to);

            mail.setFrom(new InternetAddress(from));

            mail.setSubject(subject);

            final Multipart multipart = new MimeMultipart();
            BodyPart messageBodyPart = new MimeBodyPart();

            if (isHTML) {
                messageBodyPart.setContent(content, "text/html");
            } else {
                messageBodyPart.setContent(content, "text/plain");
            }

            multipart.addBodyPart(messageBodyPart);

            if (!N.isEmpty(attachedFiles)) {
                for (final String fileName : attachedFiles) {
                    messageBodyPart = new MimeBodyPart();

                    final DataSource source = new FileDataSource(fileName);
                    messageBodyPart.setDataHandler(new DataHandler(source));

                    messageBodyPart.setFileName(new File(fileName).getName());
                    multipart.addBodyPart(messageBodyPart);
                }
            }

            mail.setContent(multipart);
            Transport.send(mail);
        } catch (final MessagingException e) {
            throw new RuntimeException("Failed to send email to: " + Arrays.toString(recipients), e);
        }
    }
}