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
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 *
 */
public final class EmailUtil {

    private EmailUtil() {
        // singleton.
    }

    /**
     *
     * @param recipients
     * @param from
     * @param subject
     * @param content
     * @param userName
     * @param password
     * @param props
     */
    public static void sendEmail(final String[] recipients, final String from, final String subject, final String content, final String userName,
            final String password, final Properties props) {
        sendEmailWithAttachment(recipients, from, subject, content, null, userName, password, props);
    }

    /**
     * Send mail with attachment.
     *
     * @param recipients
     * @param from
     * @param subject
     * @param content
     * @param attachedFiles
     * @param userName
     * @param password
     * @param props
     */
    public static void sendEmailWithAttachment(final String[] recipients, final String from, final String subject, final String content,
            final String[] attachedFiles, final String userName, final String password, final Properties props) {
        send(recipients, from, subject, content, attachedFiles, false, userName, password, props);
    }

    /**
     * Send HTML mail.
     *
     * @param recipients
     * @param from
     * @param subject
     * @param content
     * @param userName
     * @param password
     * @param props
     */
    public static void sendHTMLEmail(final String[] recipients, final String from, final String subject, final String content, final String userName,
            final String password, final Properties props) {
        sendHTMLEmailWithAttachment(recipients, from, subject, content, null, userName, password, props);
    }

    /**
     * Send HTML mail with attachment.
     *
     * @param recipients
     * @param from
     * @param subject
     * @param content
     * @param attachedFiles
     * @param userName
     * @param password
     * @param props
     */
    public static void sendHTMLEmailWithAttachment(final String[] recipients, final String from, final String subject, final String content,
            final String[] attachedFiles, final String userName, final String password, final Properties props) {
        send(recipients, from, subject, content, attachedFiles, true, userName, password, props);
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
    private static void send(final String[] recipients, final String from, final String subject, final String content, final String[] attachedFiles,
            final boolean isHTML, final String userName, final String password, final Properties props) {

        try {
            final Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userName, password);
                }
            });

            // Define message
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

            // Put parts in message
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
        } catch (final MessagingException e) {
            throw new RuntimeException("Failed to send email to: " + Arrays.toString(recipients), e);
        }
    }
}
