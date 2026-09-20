package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.util.DateTimeFormat;

class TemporalOutputExceptionContractTest extends TestBase {
    @Test
    void dateAndCalendarPreserveCheckedAndWrappedOutputFailures() {
        verifyOutputPaths(new JUDateType(), new Date(0));
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(0);
        verifyOutputPaths(new CalendarType(), calendar);
    }

    @Test
    void xmlCalendarPreservesCheckedAndWrappedOutputFailures() throws Exception {
        final XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar("1970-01-01T00:00:00Z");
        verifyOutputPaths(new XMLGregorianCalendarType(), calendar);
    }

    @Test
    void incompleteXmlCalendarFailsBeforeAppendingItsLexicalRepresentation() throws Exception {
        final XMLGregorianCalendar incomplete = DatatypeFactory.newInstance().newXMLGregorianCalendar();
        final XMLGregorianCalendarType type = new XMLGregorianCalendarType();
        assertThrowsExactly(IllegalStateException.class, () -> type.appendTo(new StringBuilder(), incomplete));
    }

    @Test
    void jdbcConversionFailuresPrecedeAccessToTheStatement() {
        final InstantType instantType = new InstantType();
        assertThrowsExactly(NullPointerException.class, () -> instantType.get((ResultSet) null, 1));
        assertThrowsExactly(NullPointerException.class, () -> instantType.set((PreparedStatement) null, 1, Instant.EPOCH));
        Class<? extends RuntimeException> expectedExtremeValueFailure = NullPointerException.class;
        try {
            Timestamp.from(Instant.MAX);
        } catch (IllegalArgumentException e) {
            // Preserve the running JDK's conversion failure before the null statement is accessed.
            expectedExtremeValueFailure = IllegalArgumentException.class;
        }
        assertThrowsExactly(expectedExtremeValueFailure, () -> instantType.set((PreparedStatement) null, 1, Instant.MAX));
        final JdkDurationType durationType = new JdkDurationType();
        assertThrowsExactly(NullPointerException.class, () -> durationType.set((PreparedStatement) null, 1, Duration.ZERO));
        assertThrowsExactly(ArithmeticException.class, () -> durationType.set((PreparedStatement) null, 1, Duration.ofNanos(1)));
    }

    private static <T> void verifyOutputPaths(final Type<T> type, final T value) {
        final IOException failure = new IOException("destination failed");
        final Appendable broken = new Appendable() {
            @Override
            public Appendable append(final CharSequence text) throws IOException {
                throw failure;
            }

            @Override
            public Appendable append(final CharSequence text, final int start, final int end) throws IOException {
                throw failure;
            }

            @Override
            public Appendable append(final char ch) throws IOException {
                throw failure;
            }
        };

        assertSame(failure, assertThrowsExactly(IOException.class, () -> type.appendTo(broken, null)));
        assertSame(failure, assertThrowsExactly(UncheckedIOException.class, () -> type.appendTo(broken, value)).getCause());
        assertThrowsExactly(NullPointerException.class, () -> type.appendTo(null, null));
        assertThrowsExactly(IllegalArgumentException.class, () -> type.appendTo(null, value));
        assertThrowsExactly(NullPointerException.class, () -> type.serializeTo(null, null, null));
        assertThrowsExactly(IllegalArgumentException.class, () -> type.serializeTo(null, value, null));
        final JsonSerConfig longFormat = JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.LONG);
        assertThrowsExactly(NullPointerException.class, () -> type.serializeTo(null, value, longFormat));
    }
}
