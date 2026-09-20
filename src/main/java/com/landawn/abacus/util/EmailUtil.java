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
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParameterList;

/**
 * Utility class for sending emails using the JavaMail API.
 * Provides convenient methods for sending plain text and HTML emails with or without attachments.
 *
 * <p>This class supports SMTP authentication and can be configured with various mail server properties.</p>
 *
 * <p><b>Usage Examples:</b></p>
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
 *     new String[] {"recipient@example.com"},
 *     "sender@example.com",
 *     "Test Subject",
 *     "Hello, this is a test email!",
 *     "username",
 *     "password",
 *     props
 * );
 *
 * // Send HTML email with attachment
 * EmailUtil.sendHtmlEmailWithAttachment(
 *     new String[] {"recipient@example.com"},
 *     "sender@example.com",
 *     "HTML Email",
 *     "<h1>Hello</h1><p>This is an HTML email!</p>",
 *     new String[] {"attachment.pdf"},
 *     "username",
 *     "password",
 *     props
 * );
 * }</pre>
 *
 */
public final class EmailUtil {

    private EmailUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Sends a plain text email to the specified recipients.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Properties props = new Properties();
     * props.put("mail.smtp.host", "smtp.example.com");
     * props.put("mail.smtp.auth", "true");
     *
     * EmailUtil.sendEmail(
     *     new String[] {"user1@example.com", "user2@example.com"},
     *     "sender@example.com",
     *     "Meeting Reminder",
     *     "Don't forget about tomorrow's meeting at 10 AM",
     *     "smtp_username",
     *     "smtp_password",
     *     props
     * );
     * }</pre>
     *
     * @param recipients array of email addresses to send the email to; must not be {@code null} or empty
     * @param from the sender's email address; must not be {@code null} or empty
     * @param subject the email subject. May be {@code null} or empty
     * @param content the plain text content of the email. May be {@code null} or empty
     * @param userName the username for SMTP authentication; may be {@code null} when authentication is disabled
     * @param password the password for SMTP authentication; may be {@code null} when authentication is disabled
     * @param props mail server properties; must not be {@code null}. Common properties include:
     *              <ul>
     *              <li>{@code mail.smtp.host} - SMTP server host (required)</li>
     *              <li>{@code mail.smtp.port} - SMTP server port (e.g., 25, 587, 465)</li>
     *              <li>{@code mail.smtp.auth} - enable authentication (true/false)</li>
     *              <li>{@code mail.smtp.starttls.enable} - enable STARTTLS (true/false)</li>
     *              <li>{@code mail.smtp.ssl.enable} - enable SSL (true/false)</li>
     *              <li>{@code mail.smtp.ssl.trust} - trusted hosts</li>
     *              </ul>
     * @throws IllegalArgumentException if {@code recipients} is null or empty, {@code from} is null or empty, or {@code props} is null
     * @throws NullPointerException if a recipient address is {@code null}
     * @throws RuntimeException if address parsing, message assembly, authentication, or mail transport fails
     * @see #sendEmailWithAttachment(String[], String, String, String, String[], String, String, Properties)
     * @see #sendHtmlEmail(String[], String, String, String, String, String, Properties)
     */
    public static void sendEmail(final String[] recipients, final String from, final String subject, final String content, final String userName,
            final String password, final Properties props) throws IllegalArgumentException, NullPointerException, RuntimeException {
        sendEmailWithAttachment(recipients, from, subject, content, null, userName, password, props);
    }

    /**
     * Sends a plain text email with file attachments to the specified recipients.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EmailUtil.sendEmailWithAttachment(
     *     new String[] {"recipient@example.com"},
     *     "sender@example.com",
     *     "Monthly Report",
     *     "Please find the monthly report attached.",
     *     new String[] {"/path/to/report.pdf", "/path/to/data.xlsx"},
     *     "username",
     *     "password",
     *     mailProperties
     * );
     * }</pre>
     *
     * @param recipients array of email addresses to send the email to; must not be {@code null} or empty
     * @param from the sender's email address; must not be {@code null} or empty
     * @param subject the email subject. May be {@code null} or empty
     * @param content the plain text content of the email. May be {@code null} or empty
     * @param attachedFiles array of file paths (absolute or relative) to attach, or {@code null} if no attachments.
     *                      Files must exist and be readable. The file name (not full path) will be used as the attachment name
     * @param userName the username for SMTP authentication; may be {@code null} when authentication is disabled
     * @param password the password for SMTP authentication; may be {@code null} when authentication is disabled
     * @param props mail server properties; must not be {@code null}. Common properties include:
     *              <ul>
     *              <li>{@code mail.smtp.host} - SMTP server host (required)</li>
     *              <li>{@code mail.smtp.port} - SMTP server port (e.g., 25, 587, 465)</li>
     *              <li>{@code mail.smtp.auth} - enable authentication (true/false)</li>
     *              <li>{@code mail.smtp.starttls.enable} - enable STARTTLS (true/false)</li>
     *              <li>{@code mail.smtp.ssl.enable} - enable SSL (true/false)</li>
     *              <li>{@code mail.smtp.ssl.trust} - trusted hosts</li>
     *              </ul>
     * @throws IllegalArgumentException if {@code recipients} is null or empty, {@code from} is null or empty, or {@code props} is null
     * @throws NullPointerException if a recipient address or an attachment path is {@code null}
     * @throws RuntimeException if address parsing, message assembly, authentication, attachment reading, or mail transport fails
     * @see #sendEmail(String[], String, String, String, String, String, Properties)
     * @see #sendHtmlEmailWithAttachment(String[], String, String, String, String[], String, String, Properties)
     */
    public static void sendEmailWithAttachment(final String[] recipients, final String from, final String subject, final String content,
            final String[] attachedFiles, final String userName, final String password, final Properties props)
            throws IllegalArgumentException, NullPointerException, RuntimeException {
        send(recipients, from, subject, content, attachedFiles, false, userName, password, props);
    }

    /**
     * Sends an HTML email to the specified recipients.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String htmlContent = "<html><body>" +
     *     "<h2>Welcome!</h2>" +
     *     "<p>Thank you for <b>registering</b>.</p>" +
     *     "<a href='https://example.com'>Visit our website</a>" +
     *     "</body></html>";
     *
     * EmailUtil.sendHtmlEmail(
     *     new String[] {"newuser@example.com"},
     *     "noreply@example.com",
     *     "Welcome to Our Service",
     *     htmlContent,
     *     "smtp_user",
     *     "smtp_pass",
     *     props
     * );
     * }</pre>
     *
     * @param recipients array of email addresses to send the email to; must not be {@code null} or empty
     * @param from the sender's email address; must not be {@code null} or empty
     * @param subject the email subject. May be {@code null} or empty
     * @param content the HTML content of the email. May be {@code null} or empty
     * @param userName the username for SMTP authentication; may be {@code null} when authentication is disabled
     * @param password the password for SMTP authentication; may be {@code null} when authentication is disabled
     * @param props mail server properties; must not be {@code null}. Common properties include:
     *              <ul>
     *              <li>{@code mail.smtp.host} - SMTP server host (required)</li>
     *              <li>{@code mail.smtp.port} - SMTP server port (e.g., 25, 587, 465)</li>
     *              <li>{@code mail.smtp.auth} - enable authentication (true/false)</li>
     *              <li>{@code mail.smtp.starttls.enable} - enable STARTTLS (true/false)</li>
     *              <li>{@code mail.smtp.ssl.enable} - enable SSL (true/false)</li>
     *              <li>{@code mail.smtp.ssl.trust} - trusted hosts</li>
     *              </ul>
     * @throws IllegalArgumentException if {@code recipients} is null or empty, {@code from} is null or empty, or {@code props} is null
     * @throws NullPointerException if a recipient address is {@code null}
     * @throws RuntimeException if address parsing, message assembly, authentication, or mail transport fails
     * @see #sendHtmlEmailWithAttachment(String[], String, String, String, String[], String, String, Properties)
     * @see #sendEmail(String[], String, String, String, String, String, Properties)
     */
    public static void sendHtmlEmail(final String[] recipients, final String from, final String subject, final String content, final String userName,
            final String password, final Properties props) throws IllegalArgumentException, NullPointerException, RuntimeException {
        sendHtmlEmailWithAttachment(recipients, from, subject, content, null, userName, password, props);
    }

    /**
     * Sends an HTML email with file attachments to the specified recipients.
     *
     * <p><b>Usage Examples:</b></p>
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
     * EmailUtil.sendHtmlEmailWithAttachment(
     *     new String[] {"customer@example.com"},
     *     "billing@example.com",
     *     "Invoice #12345",
     *     htmlContent,
     *     new String[] {"/path/to/invoice.pdf"},
     *     "smtp_user",
     *     "smtp_pass",
     *     props
     * );
     * }</pre>
     *
     * @param recipients array of email addresses to send the email to; must not be {@code null} or empty
     * @param from the sender's email address; must not be {@code null} or empty
     * @param subject the email subject. May be {@code null} or empty
     * @param content the HTML content of the email. May be {@code null} or empty
     * @param attachedFiles array of file paths (absolute or relative) to attach, or {@code null} if no attachments.
     *                      Files must exist and be readable. The file name (not full path) will be used as the attachment name
     * @param userName the username for SMTP authentication; may be {@code null} when authentication is disabled
     * @param password the password for SMTP authentication; may be {@code null} when authentication is disabled
     * @param props mail server properties; must not be {@code null}. Common properties include:
     *              <ul>
     *              <li>{@code mail.smtp.host} - SMTP server host (required)</li>
     *              <li>{@code mail.smtp.port} - SMTP server port (e.g., 25, 587, 465)</li>
     *              <li>{@code mail.smtp.auth} - enable authentication (true/false)</li>
     *              <li>{@code mail.smtp.starttls.enable} - enable STARTTLS (true/false)</li>
     *              <li>{@code mail.smtp.ssl.enable} - enable SSL (true/false)</li>
     *              <li>{@code mail.smtp.ssl.trust} - trusted hosts</li>
     *              </ul>
     * @throws IllegalArgumentException if {@code recipients} is null or empty, {@code from} is null or empty, or {@code props} is null
     * @throws NullPointerException if a recipient address or an attachment path is {@code null}
     * @throws RuntimeException if address parsing, message assembly, authentication, attachment reading, or mail transport fails
     * @see #sendHtmlEmail(String[], String, String, String, String, String, Properties)
     * @see #sendEmailWithAttachment(String[], String, String, String, String[], String, String, Properties)
     */
    public static void sendHtmlEmailWithAttachment(final String[] recipients, final String from, final String subject, final String content,
            final String[] attachedFiles, final String userName, final String password, final Properties props)
            throws IllegalArgumentException, NullPointerException, RuntimeException {
        send(recipients, from, subject, content, attachedFiles, true, userName, password, props);
    }

    /**
     * @throws IllegalArgumentException if {@code recipients} is null or empty, {@code from} is null or empty, or {@code props} is null
     * @throws NullPointerException if a recipient address or an attachment path is {@code null}
     * @throws RuntimeException if address parsing, message assembly, authentication, attachment reading, or mail transport fails
     */
    private static void send(final String[] recipients, final String from, final String subject, final String content, final String[] attachedFiles,
            final boolean isHTML, final String userName, final String password, final Properties props)
            throws IllegalArgumentException, NullPointerException, RuntimeException {
        try {
            final MimeMessage mail = createMessage(recipients, from, subject, content, attachedFiles, isHTML, userName, password, props);
            Transport.send(mail);
        } catch (final MessagingException e) {
            throw new RuntimeException("Failed to send email to " + recipients.length + " recipient(s)", e);
        }
    }

    /**
     * Builds the multipart {@link MimeMessage} that the {@code send*} methods transmit, without
     * sending it. The message body is added as a single {@code text/html} or {@code text/plain}
     * part encoded in UTF-8, followed by one part per entry in {@code attachedFiles}; a
     * {@code null} {@code content} is written as an empty body. The subject, the display name of any
     * address and each attachment file name are encoded as UTF-8 as well, independently of the JVM's
     * default charset; an attachment name is UTF-8 in both headers that carry it, {@code Content-Disposition}
     * and {@code Content-Type}.
     *
     * <p>This method is package-private so that message construction can be verified without
     * contacting an SMTP server.</p>
     *
     * @param recipients array of email addresses to send the email to; must not be {@code null} or empty
     * @param from the sender's email address; must not be {@code null} or empty
     * @param subject the email subject. May be {@code null} or empty
     * @param content the body of the email. May be {@code null} or empty
     * @param attachedFiles array of file paths to attach, or {@code null} if no attachments.
     *                      The file name (not full path) is used as the attachment name
     * @param isHTML {@code true} to send {@code content} as {@code text/html}, {@code false} for {@code text/plain}
     * @param userName the username for SMTP authentication; may be {@code null} when authentication is disabled
     * @param password the password for SMTP authentication; may be {@code null} when authentication is disabled
     * @param props mail server properties; must not be {@code null}
     * @return the constructed message, ready to be passed to {@link Transport#send(Message)}
     * @throws IllegalArgumentException if {@code recipients} is {@code null} or empty, {@code from} is {@code null}
     *         or empty, or {@code props} is {@code null}.
     * @throws NullPointerException if a recipient address or an attachment path is {@code null}
     * @throws MessagingException if an address is invalid or the message cannot be assembled
     * @throws IllegalStateException if an address carries a display name and this JVM does not support the
     *         {@code UTF-8} charset, which every conformant JVM is required to provide
     */
    static MimeMessage createMessage(final String[] recipients, final String from, final String subject, final String content, final String[] attachedFiles,
            final boolean isHTML, final String userName, final String password, final Properties props)
            throws IllegalArgumentException, NullPointerException, MessagingException, IllegalStateException {
        N.checkArgNotEmpty(recipients, cs.recipients);
        N.checkArgNotEmpty(from, cs.from);
        N.checkArgNotNull(props, cs.props);

        final Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userName, password);
            }
        });

        final MimeMessage mail = new MimeMessage(session);

        final InternetAddress[] to = new InternetAddress[recipients.length];

        for (int i = 0; i < recipients.length; i++) {
            to[i] = utf8Address(recipients[i]);
        }

        mail.setRecipients(Message.RecipientType.TO, to);

        mail.setFrom(utf8Address(from));

        // Pin the subject charset to UTF-8. setSubject(String) encodes with mail.mime.charset, falling
        // back to the JVM default charset, so the header charset varied by JVM and platform while the
        // body below is always UTF-8; on a legacy default charset a non-ASCII subject became '?'.
        mail.setSubject(subject, Charsets.UTF_8.name());

        final Multipart multipart = new MimeMultipart();
        final BodyPart messageBodyPart = new MimeBodyPart();
        final String body = content == null ? Strings.EMPTY : content;

        if (isHTML) {
            messageBodyPart.setContent(body, "text/html; charset=UTF-8");
        } else {
            messageBodyPart.setContent(body, "text/plain; charset=UTF-8");
        }

        multipart.addBodyPart(messageBodyPart);

        if (!N.isEmpty(attachedFiles)) {
            for (final String filePath : attachedFiles) {
                multipart.addBodyPart(newUtf8NamedAttachment(filePath, new File(filePath).getName()));
            }
        }

        mail.setContent(multipart);
        return mail;
    }

    /**
     * Parses {@code address} and re-encodes any display name as UTF-8.
     *
     * <p>{@link InternetAddress#toString()} encodes a non-ASCII personal name with the JVM default
     * charset, so {@code "Grüße <a@b>"} was serialized differently depending on the platform while the
     * message body was always UTF-8. Re-setting the personal part with an explicit charset pins it.</p>
     *
     * @param address an RFC 822 address, optionally with a display name
     * @return the parsed address whose display name, if any, is UTF-8 encoded
     * @throws NullPointerException if {@code address} is {@code null}
     * @throws MessagingException if {@code address} is not a valid RFC 822 address
     * @throws IllegalStateException if the address carries a display name and this JVM does not support the
     *         {@code UTF-8} charset, which every conformant JVM is required to provide
     */
    private static InternetAddress utf8Address(final String address) throws NullPointerException, MessagingException, IllegalStateException {
        final InternetAddress result = new InternetAddress(address);
        final String personal = result.getPersonal();

        if (Strings.isNotEmpty(personal)) {
            try {
                result.setPersonal(personal, Charsets.UTF_8.name());
            } catch (final UnsupportedEncodingException e) {
                // UTF-8 is required of every JVM; treat its absence as a broken environment.
                throw new IllegalStateException("UTF-8 is not supported by this JVM", e);
            }
        }

        return result;
    }

    /**
     * Creates the part that attaches {@code filePath} under the name {@code fileName}, written as UTF-8 in
     * <i>both</i> headers that carry a file name.
     *
     * <p>{@link #setUtf8FileName(Part, String)} pins the {@code Content-Disposition} parameter, but that is
     * only half of it: {@link MimeBodyPart#updateHeaders()} copies the file name into the {@code Content-Type}
     * {@code name} parameter when the message is written, and encodes <i>that</i> copy with
     * {@code mail.mime.charset}, falling back to the JVM default charset. The two names would then disagree,
     * and on a JVM whose default charset cannot represent the name the {@code Content-Type} copy degrades to
     * {@code ?} characters - the very corruption the disposition parameter is pinned to avoid, in the header
     * that older mail clients read. Re-encoding the parameter after {@code updateHeaders()} has produced it
     * keeps every other decision JavaMail makes about the header (content type, charset, transfer encoding)
     * untouched.</p>
     *
     * @param filePath the path of the file to attach
     * @param fileName the name of the attached file, without any directory
     * @return the attachment part
     * @throws NullPointerException if {@code filePath} is {@code null}
     * @throws MessagingException if the part cannot be assembled
     */
    private static MimeBodyPart newUtf8NamedAttachment(final String filePath, final String fileName) throws NullPointerException, MessagingException {
        final MimeBodyPart part = new MimeBodyPart() {
            /**
             * {@inheritDoc}
             * @throws MessagingException if the MIME headers cannot be updated or the UTF-8 filename parameter cannot be written
             */
            @Override
            protected void updateHeaders() throws MessagingException {
                super.updateHeaders();

                setUtf8ContentTypeName(this, fileName);
            }
        };

        final DataSource source = new FileDataSource(filePath);
        part.setDataHandler(new DataHandler(source));

        setUtf8FileName(part, fileName);

        return part;
    }

    /**
     * Re-encodes the {@code name} parameter of an already-rendered {@code Content-Type} header as UTF-8.
     *
     * <p>Does nothing when the header carries no {@code name} parameter, so a caller that turned the parameter
     * off with {@code -Dmail.mime.setcontenttypefilename=false} still gets no {@code name}.</p>
     *
     * @param part the attachment part whose {@code Content-Type} header has already been written
     * @param fileName the name of the attached file, without any directory
     * @throws MessagingException if the header cannot be read or written
     */
    private static void setUtf8ContentTypeName(final MimeBodyPart part, final String fileName) throws MessagingException {
        final String renderedContentType = part.getHeader("Content-Type", null);

        if (renderedContentType == null) {
            return;
        }

        final ContentType contentType = new ContentType(renderedContentType);

        if (contentType.getParameter("name") == null) {
            return;
        }

        final ParameterList parameters = contentType.getParameterList();
        parameters.set("name", fileName, Charsets.UTF_8.name());
        contentType.setParameterList(parameters);

        part.setHeader("Content-Type", contentType.toString());
    }

    /**
     * Names an attachment part by writing {@code fileName} as a UTF-8 {@code Content-Disposition} parameter.
     *
     * <p>{@link MimeBodyPart#setFileName(String)} encodes that parameter with {@code mail.mime.charset},
     * falling back to the JVM default charset, so a non-ASCII attachment name was serialized differently
     * depending on the platform while the message body is always UTF-8. Writing the parameter directly pins it
     * to the {@code filename*=UTF-8''...} form that RFC 2231 defines for MIME parameters. An RFC 2047 encoded
     * word is not valid in a parameter (RFC 2047 section 5) and is handed to the recipient undecoded, so the
     * charset must be carried by the parameter itself rather than by an encoded word. The {@code Content-Type}
     * copy of the same name is pinned by {@link #setUtf8ContentTypeName(MimeBodyPart, String)}.</p>
     *
     * @param part the attachment part to name
     * @param fileName the name of the attached file, without any directory
     * @throws MessagingException if the header cannot be written
     */
    private static void setUtf8FileName(final Part part, final String fileName) throws MessagingException {
        final ParameterList parameters = new ParameterList();
        parameters.set("filename", fileName, Charsets.UTF_8.name());

        part.setHeader("Content-Disposition", new ContentDisposition(Part.ATTACHMENT, parameters).toString());
    }
}
