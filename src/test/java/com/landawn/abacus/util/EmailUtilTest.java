package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.mail.Provider;
import javax.mail.Session;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.landawn.abacus.TestBase;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
public class EmailUtilTest extends TestBase {

    private static final String SMTP_TIMEOUT_MILLIS = "2000";

    private Properties props;
    private File tempFile;

    @BeforeEach
    public void setUp() throws IOException {
        props = new Properties();
        props.put("mail.transport.protocol.rfc822", "abacus-local");
        props.put("mail.abacus-local.class", RecordingTransport.class.getName());
        props.put("mail.smtp.port", "25");
        props.put("mail.smtp.auth", "false");
        configureSmtpTimeouts(props);

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
    public void messageBodiesAndEnvelopeRoundTripWithoutSmtp() throws Exception {
        final String[] recipients = { "user+tag@example.com", "user.name@example.co.uk" };
        for (final boolean html : new boolean[] { false, true }) {
            for (final String body : new String[] { null, "", "hello", "你好🙂 مرحبا", "<h1>A & B</h1>", "long content ".repeat(10000) }) {
                final MimeMessage message = EmailUtil.createMessage(recipients, "sender@example.com", "Subject <>&\"' 你好🙂", body,
                        null, html, null, null, props);
                final MimeMessage parsed = roundTrip(message);
                assertEquals("Subject <>&\"' 你好🙂", parsed.getSubject());
                assertEquals("sender@example.com", ((InternetAddress) parsed.getFrom()[0]).getAddress());
                assertEquals(2, parsed.getAllRecipients().length);
                for (int i = 0; i < recipients.length; i++) {
                    assertEquals(recipients[i], ((InternetAddress) parsed.getAllRecipients()[i]).getAddress());
                }
                final MimeMultipart parts = (MimeMultipart) parsed.getContent();
                assertEquals(1, parts.getCount());
                assertTrue(parts.getBodyPart(0).isMimeType(html ? "text/html" : "text/plain"));
                assertEquals(body == null ? "" : body, parts.getBodyPart(0).getContent());
            }
        }
    }

    @Test
    public void attachmentsRoundTripWithExactNamesAndBytes() throws Exception {
        final File unicode = new File(tempFile.getParentFile(), "附件-" + System.nanoTime() + ".txt");
        java.nio.file.Files.writeString(unicode.toPath(), "附件🙂", StandardCharsets.UTF_8);
        try {
            for (final boolean html : new boolean[] { false, true }) {
                for (final String[] files : new String[][] { null, {}, { tempFile.getPath() }, { tempFile.getPath(), unicode.getPath() } }) {
                    final MimeMessage parsed = roundTrip(EmailUtil.createMessage(new String[] { "test@example.com" }, "sender@example.com",
                            "", "body", files, html, null, null, props));
                    final MimeMultipart parts = (MimeMultipart) parsed.getContent();
                    assertEquals(1 + (files == null ? 0 : files.length), parts.getCount());
                    assertEquals("body", parts.getBodyPart(0).getContent());
                    for (int i = 0; files != null && i < files.length; i++) {
                        final javax.mail.BodyPart part = parts.getBodyPart(i + 1);
                        assertEquals(new File(files[i]).getName(), part.getFileName());
                        try (java.io.InputStream input = part.getInputStream()) {
                            org.junit.jupiter.api.Assertions.assertArrayEquals(java.nio.file.Files.readAllBytes(new File(files[i]).toPath()), input.readAllBytes());
                        }
                    }
                }
            }
        } finally {
            java.nio.file.Files.deleteIfExists(unicode.toPath());
        }
    }

    @Test
    public void publicSendMethodsUseDeterministicLocalTransport() throws Exception {
        props.put("mail.transport.protocol.rfc822", "abacus-local");
        props.put("mail.abacus-local.class", RecordingTransport.class.getName());
        try {
            for (int variant = 0; variant < 4; variant++) {
                RecordingTransport.sent.remove();
                final String[] recipients = { "test@example.com" };
                final String[] attachments = { tempFile.getPath() };
                switch (variant) {
                    case 0 -> EmailUtil.sendEmail(recipients, "sender@example.com", "subject", "body", null, null, props);
                    case 1 -> EmailUtil.sendHtmlEmail(recipients, "sender@example.com", "subject", "<b>body</b>", null, null, props);
                    case 2 -> EmailUtil.sendEmailWithAttachment(recipients, "sender@example.com", "subject", "body", attachments, null, null, props);
                    default -> EmailUtil.sendHtmlEmailWithAttachment(recipients, "sender@example.com", "subject", "<b>body</b>", attachments, null, null, props);
                }
                final MimeMessage sent = RecordingTransport.sent.get();
                org.junit.jupiter.api.Assertions.assertNotNull(sent);
                final MimeMultipart parts = (MimeMultipart) sent.getContent();
                assertEquals(variant < 2 ? 1 : 2, parts.getCount());
                assertEquals(variant % 2 == 0 ? "body" : "<b>body</b>", parts.getBodyPart(0).getContent());
            }
            final javax.mail.MessagingException failure = new javax.mail.MessagingException("local transport failure");
            RecordingTransport.failure.set(failure);
            final RuntimeException wrapped = assertThrows(RuntimeException.class,
                    () -> EmailUtil.sendEmail(new String[] { "test@example.com" }, "sender@example.com", "subject", "body", null, null, props));
            org.junit.jupiter.api.Assertions.assertSame(failure, wrapped.getCause());
            assertTrue(wrapped.getMessage().contains("1 recipient(s)"));
        } finally {
            RecordingTransport.sent.remove();
            RecordingTransport.failure.remove();
        }
    }

    @Test
    public void publicSendValidatesRequiredArgumentsBeforeTransport() {
        assertThrows(IllegalArgumentException.class, () -> EmailUtil.sendEmail(null, "sender@example.com", "", "", null, null, props));
        assertThrows(IllegalArgumentException.class, () -> EmailUtil.sendEmail(new String[0], "sender@example.com", "", "", null, null, props));
        assertThrows(IllegalArgumentException.class, () -> EmailUtil.sendEmail(new String[] { "test@example.com" }, null, "", "", null, null, props));
        assertThrows(IllegalArgumentException.class, () -> EmailUtil.sendEmail(new String[] { "test@example.com" }, "sender@example.com", "", "", null, null, null));
    }

    private static MimeMessage roundTrip(final MimeMessage message) throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeTo(output);
        return new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(output.toByteArray()));
    }

    /**
     * JavaMail {@link java.util.ServiceLoader} entry so a new {@link Session} can resolve
     * {@code abacus-local} without an SMTP server.
     */
    public static final class AbacusLocalProvider extends Provider {
        public AbacusLocalProvider() {
            super(Type.TRANSPORT, "abacus-local", RecordingTransport.class.getName(), "Abacus", "1.0");
        }
    }

    /** Test-only provider; it never opens a socket or sends mail. */
    public static final class RecordingTransport extends javax.mail.Transport {
        private static final ThreadLocal<MimeMessage> sent = new ThreadLocal<>();
        private static final ThreadLocal<javax.mail.MessagingException> failure = new ThreadLocal<>();

        public RecordingTransport(final Session session, final javax.mail.URLName url) {
            super(session, url);
        }

        @Override
        protected boolean protocolConnect(final String host, final int port, final String user, final String password) {
            return true;
        }

        @Override
        public void sendMessage(final javax.mail.Message message, final javax.mail.Address[] addresses) throws javax.mail.MessagingException {
            if (failure.get() != null) {
                throw failure.get();
            }
            try {
                sent.set(roundTrip((MimeMessage) message));
            } catch (final Exception e) {
                throw new javax.mail.MessagingException("Cannot serialize local message", e);
            }
        }
    }
    @Test
    public void test_createMessage_nullContentIsSerializedAsEmptyBody() throws Exception {
        final ByteArrayOutputStream plainOutput = new ByteArrayOutputStream();
        EmailUtil.createMessage(new String[] { "test@example.com" }, "sender@example.com", "Null body", null, null, false, "username", "password", props)
                .writeTo(plainOutput);

        final String plainMessage = plainOutput.toString("UTF-8");
        assertTrue(plainMessage.contains("text/plain"));

        final ByteArrayOutputStream htmlOutput = new ByteArrayOutputStream();
        EmailUtil.createMessage(new String[] { "test@example.com" }, "sender@example.com", "Null HTML body", null, null, true, "username", "password", props)
                .writeTo(htmlOutput);

        final String htmlMessage = htmlOutput.toString("UTF-8");
        assertTrue(htmlMessage.contains("text/html"));
    }

    @Test
    public void test_createMessage_validatesRequiredEnvelopeArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> EmailUtil.createMessage(new String[] { "test@example.com" }, "", "subject", "body", null, false, null, null, props));
        assertThrows(IllegalArgumentException.class,
                () -> EmailUtil.createMessage(new String[] { "test@example.com" }, "sender@example.com", "subject", "body", null, false, null, null, null));
    }

    @Test
    public void test_createMessage_pinsUtf8OnSubjectSenderAndAttachmentName() throws Exception {
        final String subject = "Bericht über Größe";
        final String personal = "Grüße Sender";
        final File attachment = new File(tempFile.getParentFile(), "Anhänge-" + System.nanoTime() + ".txt");

        try (FileOutputStream fos = new FileOutputStream(attachment)) {
            fos.write("Test attachment content".getBytes(StandardCharsets.UTF_8));
        }

        try {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            EmailUtil.createMessage(new String[] { "test@example.com" }, "\"" + personal + "\" <sender@example.com>", subject, "body",
                    new String[] { attachment.getPath() }, false, "username", "password", props).writeTo(output);

            // Re-parse the serialized message: the headers must survive a round trip whatever the JVM's
            // default charset is, and they must say UTF-8 rather than relying on the reader guessing.
            final MimeMessage parsed = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(output.toByteArray()));

            assertEquals(subject, parsed.getSubject());
            assertTrue(parsed.getHeader("Subject")[0].toUpperCase().contains("UTF-8"), parsed.getHeader("Subject")[0]);
            assertEquals(personal, ((InternetAddress) parsed.getFrom()[0]).getPersonal());
            assertTrue(parsed.getHeader("From")[0].toUpperCase().contains("UTF-8"), parsed.getHeader("From")[0]);

            final MimeBodyPart attachmentPart = (MimeBodyPart) ((MimeMultipart) parsed.getContent()).getBodyPart(1);
            final String disposition = attachmentPart.getHeader("Content-Disposition")[0];

            // The file name must be an RFC 2231 parameter, not an RFC 2047 encoded word: an encoded word is
            // invalid in a MIME parameter and is handed back to the recipient undecoded.
            assertEquals(attachment.getName(), attachmentPart.getFileName());
            assertTrue(disposition.contains("filename*"), disposition);
            assertTrue(disposition.toUpperCase().contains("UTF-8"), disposition);
        } finally {
            attachment.delete();
        }
    }

    /**
     * The attachment file name travels in two headers: the {@code Content-Disposition; filename} parameter
     * written by this class, and the {@code Content-Type; name} copy that {@code MimeBodyPart.updateHeaders()}
     * derives from it at write time. Only the first was pinned to UTF-8; the second was encoded with
     * {@code mail.mime.charset} (the JVM default charset), so the two disagreed and, on a default charset that
     * cannot represent the name, the copy older clients read degraded to {@code ?}.
     */
    @Test
    public void test_createMessage_pinsUtf8OnTheContentTypeNameParameterToo() throws Exception {
        final File attachment = new File(tempFile.getParentFile(), "Anhänge-" + System.nanoTime() + ".txt");

        try (FileOutputStream fos = new FileOutputStream(attachment)) {
            fos.write("Test attachment content".getBytes(StandardCharsets.UTF_8));
        }

        try {
            assertAttachmentNameIsUtf8InBothHeaders(attachment);

            // ... and again with the charset JavaMail would otherwise reach for forced to one that cannot
            // represent the name at all: the Content-Type copy used to come out as "name*=Shift_JIS''Anh%3F..."
            // while the disposition still said UTF-8.
            final Field cachedMimeCharset = mimeUtilityDefaultCharsetField();
            Assumptions.assumeTrue(cachedMimeCharset != null, "javax.mail no longer caches the default MIME charset in a settable field");

            final Object previous = cachedMimeCharset.get(null);

            try {
                cachedMimeCharset.set(null, "Shift_JIS");
                assertAttachmentNameIsUtf8InBothHeaders(attachment);
            } finally {
                cachedMimeCharset.set(null, previous);
            }
        } finally {
            attachment.delete();
        }
    }

    private void assertAttachmentNameIsUtf8InBothHeaders(final File attachment) throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        EmailUtil.createMessage(new String[] { "test@example.com" }, "sender@example.com", "subject", "body",
                new String[] { attachment.getPath() }, false, "username", "password", props).writeTo(output);

        final MimeMessage parsed = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(output.toByteArray()));
        final MimeBodyPart attachmentPart = (MimeBodyPart) ((MimeMultipart) parsed.getContent()).getBodyPart(1);
        final String contentType = attachmentPart.getHeader("Content-Type")[0];
        final String disposition = attachmentPart.getHeader("Content-Disposition")[0];

        assertTrue(contentType.contains("name*"), contentType);
        assertTrue(contentType.toUpperCase().contains("UTF-8"), contentType);
        assertEquals(attachment.getName(), new ContentType(contentType).getParameter("name"), contentType);

        assertTrue(disposition.contains("filename*"), disposition);
        assertTrue(disposition.toUpperCase().contains("UTF-8"), disposition);
        assertEquals(attachment.getName(), attachmentPart.getFileName(), disposition);
    }

    private static Field mimeUtilityDefaultCharsetField() {
        try {
            final Field field = MimeUtility.class.getDeclaredField("defaultMIMECharset");
            field.setAccessible(true);

            return field;
        } catch (final ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static void configureSmtpTimeouts(final Properties smtpProps) {
        smtpProps.put("mail.smtp.connectiontimeout", SMTP_TIMEOUT_MILLIS);
        smtpProps.put("mail.smtp.timeout", SMTP_TIMEOUT_MILLIS);
        smtpProps.put("mail.smtp.writetimeout", SMTP_TIMEOUT_MILLIS);
    }

}
