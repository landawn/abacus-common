/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.util.Arrays;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class EmailUtil {

    private static final String MAIL_SMTP_HOST = "mail.smtp.host";

    private EmailUtil() {
        // singleton.
    }

    /**
     *
     * @param recipients
     * @param from
     * @param subject
     * @param content
     * @param mailServer
     */
    public static void sendMail(String[] recipients, String from, String subject, String content, String mailServer) {
        sendMailWithAttachment(recipients, from, subject, content, mailServer, null);
    }

    /**
     * Send mail with attachment.
     *
     * @param recipients
     * @param from
     * @param subject
     * @param content
     * @param mailServer
     * @param attachedFiles
     */
    public static void sendMailWithAttachment(String[] recipients, String from, String subject, String content, String mailServer, String[] attachedFiles) {
        send(recipients, from, subject, content, attachedFiles, mailServer, false);
    }

    /**
     * Send HTML mail.
     *
     * @param recipients
     * @param from
     * @param subject
     * @param content
     * @param mailServer
     */
    public static void sendHTMLMail(String[] recipients, String from, String subject, String content, String mailServer) {
        sendHTMLMailWithAttachment(recipients, from, subject, content, mailServer, null);
    }

    /**
     * Send HTML mail with attachment.
     *
     * @param recipients
     * @param from
     * @param subject
     * @param content
     * @param mailServer
     * @param attachedFiles
     */
    public static void sendHTMLMailWithAttachment(String[] recipients, String from, String subject, String content, String mailServer, String[] attachedFiles) {
        send(recipients, from, subject, content, attachedFiles, mailServer, true);
    }

    /**
     *
     * @param recipients
     * @param from
     * @param subject
     * @param content
     * @param attachedFiles
     * @param mailServer
     * @param isHTML
     */
    private static void send(String[] recipients, String from, String subject, String content, String[] attachedFiles, String mailServer, boolean isHTML) {
        // Get system properties
        Properties props = System.getProperties();

        // Setup mail server
        props.put(MAIL_SMTP_HOST, mailServer);

        try {
            // Get session
            Session session = Session.getDefaultInstance(props, null);

            // Define message
            MimeMessage mail = new MimeMessage(session);

            InternetAddress[] to = new InternetAddress[recipients.length];

            for (int i = 0; i < recipients.length; i++) {
                to[i] = new InternetAddress(recipients[i]);
            }

            mail.setRecipients(Message.RecipientType.TO, to);

            mail.setFrom(new InternetAddress(from));

            mail.setSubject(subject);

            Multipart multipart = new MimeMultipart();
            BodyPart messageBodyPart = new MimeBodyPart();

            // Put parts in message
            if (isHTML) {
                messageBodyPart.setContent(content, "text/html");
            } else {
                messageBodyPart.setContent(content, "text/plain");
            }

            multipart.addBodyPart(messageBodyPart);

            if (!N.isNullOrEmpty(attachedFiles)) {
                for (String fileName : attachedFiles) {
                    messageBodyPart = new MimeBodyPart();

                    DataSource source = new FileDataSource(fileName);
                    messageBodyPart.setDataHandler(new DataHandler(source));

                    int index = fileName.lastIndexOf("\\");

                    if (index < 0) {
                        index = fileName.lastIndexOf("/");
                    }

                    messageBodyPart.setFileName((index >= 0) ? fileName.substring(index + 1) : fileName);
                    multipart.addBodyPart(messageBodyPart);
                }
            }

            mail.setContent(multipart);
            // Send message
            Transport.send(mail);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email to: " + Arrays.toString(recipients), e);
        }
    }
}
