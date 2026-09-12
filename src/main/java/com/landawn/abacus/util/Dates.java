/*
 * Copyright (C) 2018 HaiYang Li
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

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.IsoEra;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.DecimalStyle;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongFunction;
import java.util.function.Supplier;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedReflectiveOperationException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.function.LongObjFunction;

/**
 * Date/time creation, parsing, formatting, conversion, arithmetic, rounding, and comparison utilities.
 * Most methods operate on the legacy {@code java.util} and {@code java.sql} types; the nested
 * {@link DTF} class also accepts the immutable {@code java.time} types.
 *
 * <p><b>Naming Convention:</b> in this class, the unqualified names {@code Date}, {@code Time} and
 * {@code Timestamp} always refer to the {@code java.sql} types, which get the short method names
 * ({@code currentDate}, {@code parseToDate}, {@code createDate}, ...), while {@code java.util.Date}
 * is consistently named {@code JUDate} ({@code currentJUDate}, {@code parseToJUDate}, {@code createJUDate}, ...).
 * The {@link DateUtil} subclass is a name-only alias for these same utilities; {@code Dates} is the
 * canonical name and should be preferred in new code.
 *
 * <p><b>java.sql Date/Time Parsing Rules:</b> {@code parseToDate} and {@code parseToTime} resolve the
 * text to an instant and retain that instant's epoch milliseconds unchanged, just like
 * {@code parseToJUDate}, {@code parseToTimestamp}, and {@code parseToCalendar}. No JDBC civil-field
 * normalization is applied: date-time input keeps its time-of-day in a {@code java.sql.Date}, and dated
 * input keeps its date component in a {@code java.sql.Time}. Time-only input is anchored to
 * 1970-01-01 in its written, supplied, or default zone. To create conventional JDBC civil values from
 * the textual fields, use {@code parseToLocalDate}/{@code parseToLocalTime} and
 * {@code Date.valueOf}/{@code Time.valueOf}.
 * Pure numeric epoch-millisecond strings are rejected as ambiguous; use
 * {@link #parseEpochMillis(String)} for epoch text.
 *
 * <p><b>Parsing Strictness:</b> Formatted legacy input is non-lenient and must be consumed in full;
 * invalid fields, trailing whitespace, and trailing garbage are rejected. Each predefined pattern also
 * requires its exact canonical shape on every {@code parseTo*} target, so a field written narrower than
 * the pattern declares is rejected rather than accepted by {@code SimpleDateFormat}'s variable-width
 * reading ({@code "2025-1-15"} is not {@link #LOCAL_DATE_FORMAT}); pass a variable-width pattern such as
 * {@code "yyyy-M-d"} when shorter fields must be accepted. The predefined HTTP-date grammar additionally
 * requires canonical spelling, weekday agreement, and literal GMT.</p>
 *
 * <p><b>Legacy Pattern Locale:</b> {@code parse*}, {@code format}, and {@code formatTo} overloads
 * without a {@link Locale} use {@link Locale#US} deterministically. Use the overloads accepting a
 * locale for custom patterns containing localized fields such as {@code MMM}, {@code EEE}, or
 * {@code a}; the {@code java.time} entry points ({@code parseToLocalDate}, {@code parseToLocalTime},
 * {@code parseToLocalDateTime}, {@code parseToOffsetDateTime}, {@code parseToZonedDateTime} and
 * {@code parseToInstant}) have no locale parameter and always read a custom pattern with
 * {@code Locale.US}, so pass such a pattern to {@link DTF#of(String, Locale)} instead.
 * The predefined machine-readable constants remain US/ASCII regardless of the supplied
 * locale; HTTP-date therefore always uses its required English names. A locale selects <i>text</i>
 * &mdash; month, weekday and am/pm names &mdash; and never the calendar system: a Thai-Buddhist or
 * Japanese-imperial locale still yields proleptic ISO years, matching {@link DTF#of(String, Locale)}.
 * Digit shapes differ between the two engines: the legacy targets write and read a custom pattern's
 * numbers in the locale's digits (Arabic-Indic for {@code ar-EG}), while {@link DTF#of(String, Locale)}
 * always uses ASCII digits.</p>
 *
 * <p><b>Zone Rules:</b> zone offsets come from {@link ZoneId} throughout &mdash; parsing, formatting,
 * rounding and the civil-field comparisons all read one history, so the civil date this class prints
 * for an instant is the one its other operations compute with. That matters before 1900:
 * {@code java.util.TimeZone} discards the transitions earlier than 1900-01-01T00:00:00Z and reports
 * the zone's <i>present raw offset</i> for every instant before it - its table begins at 1900 with the
 * offset then in force, which in most zones is a transition that never happened - so most IANA zones
 * name a different local time there than {@code java.time} does (Asia/Kolkata was +05:53:28 until 1854
 * and +05:21:10 from 1870 until 1906, where the legacy table says +05:30 before 1900). The legacy table also ends at 2100: a zone
 * whose last change is an explicit transition rather than a recurring rule then drops to its raw
 * offset for ever (Africa/Casablanca and Africa/Windhoek read an hour behind {@code java.time} from
 * 2100 on). The predefined pattern constants, the {@code set*}/{@code add*} field operations and
 * {@code getFragment*(java.util.Date, ...)} therefore render and resolve pre-1900 and post-2099
 * values through the {@code java.time} rules. A custom
 * {@link SimpleDateFormat} pattern keeps the legacy engine on the format and the parse side alike, so both
 * sides read one set of rules, but it may name a different pre-1900 or post-2099 local time than a predefined
 * constant does - with one seam: the legacy table's synthetic transition at 1900-01-01T00:00:00Z (from the
 * present raw offset to the offset then in force) reads as a gap or an overlap to {@code SimpleDateFormat},
 * so within that delta of the seam a custom pattern rejects a wall clock the zone had, or reads it on the
 * other side, where a predefined constant resolves it through {@code java.time}. Two more caveats belong
 * to the {@code z} letter: a zone without an abbreviation prints a generic {@code GMT-03:00} label, which
 * reads back as that offset rather than the zone's history, and {@code SimpleDateFormat}'s first-match
 * name lookup can misread a name as a shorter one it prefixes ({@code WITA} as {@code WIT}) when the
 * fallback zone does not own it. A custom pattern therefore does not always read its own output back as the
 * same instant: the legacy table carries one daylight name per zone, so a saving other than one hour
 * (Europe/Berlin's 1945 double summer time at +03:00, Newfoundland's 1988) prints a name that reads back an
 * hour away; {@code Z} has no seconds field, so an offset with a seconds component (Africa/Monrovia at
 * -00:44:30) prints {@code -0044} and reads back 30 s away; and a zone-less custom pattern resolves a wall
 * clock a daylight-saving overlap repeats silently to the standard-time pass, where a predefined constant
 * rejects it. Use a predefined constant or {@link DTF} when historical zone offsets
 * matter. The same
 * holds for a {@link Calendar} this class returns ({@code createCalendar}, {@code parseToCalendar},
 * {@code parseToGregorianCalendar}, {@link DTF#parseToCalendar}): it carries the caller's zone object, so
 * its own {@code get} reads follow the legacy table before 1900 and from 2100 on, while {@code format},
 * {@code getFragment*} and {@code set*} read it through the aligned view.</p>
 *
 * <p><b>Legacy Civil Calendar:</b> Legacy string parsing and formatting, and the
 * {@code XMLGregorianCalendar} factories, use the proleptic Gregorian calendar (XML Schema
 * {@code dateTime} is proleptic, and an {@code XMLGregorianCalendar} reads its own fields back that
 * way). Predefined patterns whose {@code yyyy} field promises four digits accept and emit only
 * Common Era years {@code 0001} through {@code 9999}. Auto-detected legacy ISO input follows the same
 * range, rejects leap-second fields because {@code java.util.Date} cannot preserve them, and limits
 * numeric offsets to the {@link ZoneOffset} range -18:00 through +18:00.</p>
 *
 * <p><b>Default Time Zone Semantics:</b> when a zone-sensitive operation receives no explicit zone,
 * it uses the <i>live</i> machine default zone at call time ({@code TimeZone.getDefault()}), matching
 * JDK convention. This applies uniformly to legacy {@code parse*}/{@code format*} operations,
 * {@code Calendar} arithmetic, JDBC timestamp parsing, and the no-zone operations of the {@link DTF}
 * class &mdash; a {@code TimeZone.setDefault(...)} call affects all subsequent operations, so code that
 * changes the default mid-run should pass an explicit zone everywhere. Each parse call that needs the
 * default captures it <b>once</b>, so parsing and result construction never observe two different
 * defaults. Fixed UTC/GMT formats never consult the default for instant resolution.
 * {@code Calendar} overloads honor the calendar's own zone when no zone is supplied, falling back to
 * the live default zone for the rare {@code Calendar} implementation whose {@code getTimeZone()} returns
 * {@code null}. UTC-equivalence, wherever this class tests it (the fixed UTC/GMT formats, the
 * {@code Calendar} civil-field comparisons), is a property of a zone's rules and never of its ID or
 * class: {@code UTC}, {@code GMT}, {@code Etc/UTC} and a fixed zero-offset zone of any class under any
 * ID all qualify.</p>
 *
 * <p><b>Default Formats:</b> with a {@code null} or empty format, {@code java.util.Date} and the SQL
 * types write the UTC {@code 'Z'} forms (see {@link #format(java.util.Date)}), or the offset-bearing
 * forms when a zone is supplied. The {@code Calendar} overloads write an ISO zoned date-time with any
 * millisecond fraction, the effective offset, and the zone ID, so the instant and registered region
 * rules can be recovered. {@code XMLGregorianCalendar} writes the value's own XML lexical form,
 * retaining its fraction and numeric offset and leaving an undefined XML timezone undefined.</p>
 *
 * <p><b>A default zone {@code java.time} cannot express.</b> The live default may be a {@code TimeZone}
 * whose rules no {@link ZoneId} carries: its ID is unknown to {@code java.time} (a hand-built
 * {@link SimpleTimeZone} under a made-up name), or it reuses a registered region ID with different
 * rules. A whole-second fixed-offset zone is accepted everywhere, whatever its ID (a sub-second offset
 * is rejected by the operations that resolve through a {@link ZoneId}, listed below); for the two other
 * cases this class never substitutes rules silently, and each operation family behaves as its engine
 * allows. Operations that resolve through {@code Calendar} &mdash; the legacy {@code parse*}/{@code format*}
 * overloads with a pattern, and the {@code set*}/{@code add*} field arithmetic &mdash; follow the
 * custom rules with {@code Calendar}'s own resolution, not the rules described on those methods: a wall
 * clock an overlap repeats takes the standard-time offset (so setting a field to the value it already
 * holds can move a daylight-time instant to the standard-time pass), and a day, week, month or year step
 * that lands in a spring-forward gap resolves an hour before the gap. The civil-field queries without a zone parameter ({@code isSameDay},
 * {@code isSameMonth}, {@code isSameYear}, {@code isLastDayOfMonth}, {@code isLastDayOfYear},
 * {@code lengthOfMonth}, {@code lengthOfYear}) use the rules registered for the ID, and reject an
 * unknown ID that carries daylight-saving rules. Everything that resolves an instant through a
 * {@link ZoneId} &mdash; {@code round},
 * {@code truncate} and {@code ceiling}, the {@code java.time} parse targets, the {@link DTF} class and
 * the ISO zoned default of {@code format} &mdash; rejects both cases with
 * {@code IllegalArgumentException} rather than produce an instant the custom rules would not have.
 * A zone or offset written in the text being parsed makes the fallback irrelevant and is never
 * subject to this. Pass an explicit zone to avoid the question entirely.</p>
 *
 * <p><b>Parsing Contract:</b> parsing is <i>strict for non-null input</i>: a {@code null} reference
 * and the case-insensitive marker {@code "null"} (the token written by {@code formatTo} for a null
 * value) both return {@code null} from object {@code parse*} methods. Empty text and bare numeric text throw
 * {@code IllegalArgumentException}. Bare numeric text is ambiguous (epoch milliseconds
 * vs. a numeric date such as a year) &mdash; use {@link #parseEpochMillis(String)} for epoch input.
 * {@link #parseEpochMillis(String)} has no null result: a {@code null} reference and the {@code "null"}
 * marker return {@code 0}; {@link #parseEpochMillisToInstant(String)} returns {@link Instant#EPOCH}.
 * The result families behave as follows:</p>
 * <ul>
 *   <li><b>Civil-field targets</b> ({@code parseToLocalDate/LocalTime/LocalDateTime}): the civil fields
 *       are taken exactly as written; an offset or zone in the text never shifts them, and no zone
 *       parameter exists. The input must actually contain the target's fields (a year-only string cannot
 *       produce a {@code LocalDate}); time-only input is valid for {@code parseToLocalTime}.</li>
 *   <li><b>java.time instant targets</b> ({@code parseToOffsetDateTime/ZonedDateTime/Instant}): resolve
 *       one instant; a zone or offset written in the text always wins, an offset inconsistent with its
 *       bracketed region is rejected, and zone-less text is interpreted in the supplied zone or the live
 *       default. A complete local date is required (time-only input is rejected); a missing time means
 *       start of day. DST gaps and overlaps that the text does not disambiguate with an offset are
 *       rejected.</li>
 *   <li><b>Auto-detected shapes</b>: with no format, every target detects the same predefined shapes
 *       ({@code yyyy-MM-dd}, {@code HH:mm:ss}, the space- and T-separated date-times with an optional
 *       1&ndash;9 digit fraction, the T-separated one also with a trailing {@code Z}, the ISO offset forms with
 *       {@code +HH:mm} or compact {@code +HHmm} offsets, the bracketed-zone form (whose offset takes the same
 *       shapes, or {@code Z}), and HTTP-date; {@code MM-dd} is detected too and
 *       then rejected as a partial date by every target but {@code parseToLocalTime}, which rejects it for
 *       lacking time fields) and then applies its own completeness rules. The legacy instant targets
 *       additionally fall back to a general ISO 8601 reader for text no predefined shape matches: the
 *       basic (compact) date-time form ({@code 20250115T103045Z}; a bare {@code 20250115} is ambiguous
 *       numeric text everywhere) and times without a seconds field ({@code 2025-01-15T10:30}). The
 *       {@code java.time} and civil-field targets have no such fallback and reject that text with
 *       "Cannot detect a date/time format"; pass an explicit pattern there. That general reader resolves a
 *       zone-less text through {@code java.time}, so on these two shapes a fallback zone whose daylight-saving
 *       rules no {@code ZoneId} can express (a hand-built {@code SimpleTimeZone}) is rejected, although the
 *       predefined fixed-width shapes accept it through {@code Calendar}. A {@code Z} or {@code GMT}
 *       designator in auto-detected text names the zone and wins over a supplied fallback zone; only an
 *       explicitly supplied fixed-zone constant treats a non-UTC-equivalent zone as a conflict.</li>
 *   <li><b>Legacy instant targets</b> ({@code parseToDate/Time/Timestamp/JUDate/Calendar/
 *       GregorianCalendar/XMLGregorianCalendar}): resolve one instant. A zone or offset in the text
 *       wins; zone-less text is interpreted in the supplied zone or the captured default. For the standard zone-less
 *       local date and date-time shapes ({@code LOCAL_DATE_FORMAT}, {@code LOCAL_DATE_TIME_FORMAT},
 *       {@code ISO_LOCAL_DATE_TIME_FORMAT}, {@code LOCAL_TIMESTAMP_FORMAT},
 *       {@code ISO_LOCAL_TIMESTAMP_FORMAT}) DST gaps and overlaps are
 *       rejected (a date-only value resolves at local midnight); custom patterns keep the legacy
 *       {@code SimpleDateFormat} resolution rules. That strict resolution needs rules a {@link ZoneId}
 *       can express: a custom {@code TimeZone} whose own daylight-saving rules no {@code ZoneId} can
 *       represent is still accepted, but resolves through {@code Calendar}, which silently selects one
 *       side of an overlap. Except for {@code parseToTime}, a complete
 *       local date is required; missing time fields mean start of day, while year-only, month-day, and
 *       time-only patterns are rejected rather than completed from the 1970-01-01 base.
 *       {@code parseToTime} accepts either a complete date or a date-free pattern that resolves an
 *       actual clock time; genuinely time-only input is anchored to 1970-01-01 in the authoritative
 *       zone, while a partial date or a zone/literal-only pattern is rejected. A {@code Calendar} result
 *       preserves a zone or offset written in the text; the supplied/default zone is used for the
 *       result only when the text is zone-less. SQL {@code Date}/{@code Time} results retain the resolved
 *       epoch milliseconds without midnight/epoch-date normalization; use the civil-field targets with
 *       {@code Date.valueOf}/{@code Time.valueOf} when conventional JDBC fields are required.
 *       {@code XMLGregorianCalendar} has a narrower timezone field than {@code ZoneOffset}: only
 *       whole-minute offsets from -14:00 through +14:00 are representable, so its parser rejects an
 *       authoritative offset outside that range or containing non-zero seconds.</li>
 *   <li><b>Fraction grammar</b>: a named {@code .SSS} timestamp constant supplied explicitly requires
 *       exactly three fraction digits on every path. Auto-detected local and ISO timestamp forms accept
 *       1&ndash;9 digits interpreted as a fraction of a second on every target. In particular, the
 *       space-separated form follows the JDBC escape grammar used by {@code Timestamp.toString()}.</li>
 *   <li><b>Epoch input</b>: {@link #parseEpochMillis(String)} and
 *       {@link #parseEpochMillisToInstant(String)} only; compose with {@link #dateAt(Instant, ZoneId)} /
 *       {@link #timeAt(Instant, ZoneId)} for explicitly-zoned epoch-to-civil conversion.</li>
 * </ul>
 *
 * <h2 id="format-parse-round-trip-examples">Default Format/Parse Round-Trip Examples</h2>
 *
 * <p>In the calls below, {@code value} is the value shown in the third column and {@code text} is
 * the formatted text in the fourth column. Equality means value equality, not Java reference
 * identity.</p>
 *
 * <div style="max-width: 100%; overflow-x: auto;">
 * <table border="1" style="border-collapse: collapse; white-space: nowrap;">
 *   <caption>Legacy, SQL, and XML types</caption>
 *   <thead>
 *     <tr>
 *       <th scope="col">Type</th>
 *       <th scope="col">Format / parse calls</th>
 *       <th scope="col">Value before formatting</th>
 *       <th scope="col">Formatted text</th>
 *       <th scope="col">Parsed value</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>{@code java.sql.Date}</td>
 *       <td>{@code Dates.format(value)}<br>{@code Dates.parseToDate(text)}</td>
 *       <td>{@code epochMillis=1736937045123}</td>
 *       <td>{@code 2025-01-15T10:30:45.123Z}</td>
 *       <td>same epoch milliseconds</td>
 *     </tr>
 *     <tr>
 *       <td>{@code java.sql.Time}</td>
 *       <td>{@code Dates.format(value)}<br>{@code Dates.parseToTime(text)}</td>
 *       <td>{@code epochMillis=1736937045123}</td>
 *       <td>{@code 2025-01-15T10:30:45.123Z}</td>
 *       <td>same epoch milliseconds</td>
 *     </tr>
 *     <tr>
 *       <td>{@code java.sql.Timestamp}</td>
 *       <td>{@code Dates.format(value)}<br>{@code Dates.parseToTimestamp(text)}</td>
 *       <td>{@code 2025-01-15T10:30:45.123Z}</td>
 *       <td>{@code 2025-01-15T10:30:45.123Z}</td>
 *       <td>{@code 2025-01-15T10:30:45.123Z}</td>
 *     </tr>
 *     <tr>
 *       <td>{@code java.util.Date}</td>
 *       <td>{@code Dates.format(value)}<br>{@code Dates.parseToJUDate(text)}</td>
 *       <td>{@code epochMillis=1736937045000}</td>
 *       <td>{@code 2025-01-15T10:30:45Z}</td>
 *       <td>same epoch milliseconds</td>
 *     </tr>
 *     <tr>
 *       <td>{@code Calendar}</td>
 *       <td>{@code Dates.format(value)}<br>{@code Dates.parseToCalendar(text)}</td>
 *       <td>{@code epochMillis=1736917245123}<br>{@code zone=Asia/Kolkata}</td>
 *       <td>{@code 2025-01-15T10:30:45.123+05:30[Asia/Kolkata]}</td>
 *       <td>same epoch milliseconds and zone ID</td>
 *     </tr>
 *     <tr>
 *       <td>{@code GregorianCalendar}</td>
 *       <td>{@code Dates.format(value)}<br>{@code Dates.parseToGregorianCalendar(text)}</td>
 *       <td>{@code epochMillis=1736917245123}<br>{@code zone=Asia/Kolkata}</td>
 *       <td>{@code 2025-01-15T10:30:45.123+05:30[Asia/Kolkata]}</td>
 *       <td>same epoch milliseconds and zone ID</td>
 *     </tr>
 *     <tr>
 *       <td>{@code XMLGregorianCalendar}</td>
 *       <td>{@code Dates.format(value)}<br>{@code Dates.parseToXMLGregorianCalendar(text)}</td>
 *       <td>{@code 2025-01-15T10:30:45.123+05:30}</td>
 *       <td>{@code 2025-01-15T10:30:45.123+05:30}</td>
 *       <td>{@code 2025-01-15T10:30:45.123+05:30} (XML-equal, and text-equal for a whole-millisecond fraction)</td>
 *     </tr>
 *   </tbody>
 * </table>
 * </div>
 *
 * <br>
 *
 * <div style="max-width: 100%; overflow-x: auto;">
 * <table border="1" style="border-collapse: collapse; white-space: nowrap;">
 *   <caption>{@code java.time} types through {@link DTF}</caption>
 *   <thead>
 *     <tr>
 *       <th scope="col">Type</th>
 *       <th scope="col">Format / parse calls</th>
 *       <th scope="col">Value before formatting</th>
 *       <th scope="col">Formatted text</th>
 *       <th scope="col">Parsed value</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>{@code LocalDate}</td>
 *       <td>{@code DTF.LOCAL_DATE.format(value)}<br>{@code DTF.LOCAL_DATE.parseToLocalDate(text)}</td>
 *       <td>{@code 2025-01-15}</td>
 *       <td>{@code 2025-01-15}</td>
 *       <td>{@code 2025-01-15}</td>
 *     </tr>
 *     <tr>
 *       <td>{@code LocalTime}</td>
 *       <td>{@code DTF.LOCAL_TIME.format(value)}<br>{@code DTF.LOCAL_TIME.parseToLocalTime(text)}</td>
 *       <td>{@code 10:30:45}</td>
 *       <td>{@code 10:30:45}</td>
 *       <td>{@code 10:30:45}</td>
 *     </tr>
 *     <tr>
 *       <td>{@code LocalDateTime}</td>
 *       <td>{@code DTF.LOCAL_DATE_TIME.format(value)}<br>{@code DTF.LOCAL_DATE_TIME.parseToLocalDateTime(text)}</td>
 *       <td>{@code 2025-01-15T10:30:45}</td>
 *       <td>{@code 2025-01-15 10:30:45}</td>
 *       <td>{@code 2025-01-15T10:30:45}</td>
 *     </tr>
 *     <tr>
 *       <td>{@code OffsetDateTime}</td>
 *       <td>{@code DTF.ISO_OFFSET_DATE_TIME.format(value)}<br>{@code DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(text)}</td>
 *       <td>{@code 2025-01-15T10:30:45+05:30}</td>
 *       <td>{@code 2025-01-15T10:30:45+05:30}</td>
 *       <td>{@code 2025-01-15T10:30:45+05:30}</td>
 *     </tr>
 *     <tr>
 *       <td>{@code ZonedDateTime}</td>
 *       <td>{@code DTF.ISO_ZONED_DATE_TIME.format(value)}<br>{@code DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(text)}</td>
 *       <td>{@code 2025-01-15T10:30:45+05:30[Asia/Kolkata]}</td>
 *       <td>{@code 2025-01-15T10:30:45+05:30[Asia/Kolkata]}</td>
 *       <td>{@code 2025-01-15T10:30:45+05:30[Asia/Kolkata]}</td>
 *     </tr>
 *     <tr>
 *       <td>{@code Instant}</td>
 *       <td>{@code DTF.ISO_8601_TIMESTAMP.format(value)}<br>{@code DTF.ISO_8601_TIMESTAMP.parseToInstant(text)}</td>
 *       <td>{@code 2025-01-15T05:00:45.123Z}</td>
 *       <td>{@code 2025-01-15T05:00:45.123Z}</td>
 *       <td>{@code 2025-01-15T05:00:45.123Z}</td>
 *     </tr>
 *     <tr>
 *       <td>{@code LocalTime}, default precision</td>
 *       <td>{@code DTF.LOCAL_TIME.format(value)}<br>{@code DTF.LOCAL_TIME.parseToLocalTime(text)}</td>
 *       <td>{@code 10:30:45.123456789}</td>
 *       <td>{@code 10:30:45}</td>
 *       <td>{@code 10:30:45}</td>
 *     </tr>
 *     <tr>
 *       <td>{@code LocalTime}, nanosecond pattern</td>
 *       <td>{@code DTF.of("HH:mm:ss.SSSSSSSSS").format(value)}<br>{@code DTF.of("HH:mm:ss.SSSSSSSSS").parseToLocalTime(text)}</td>
 *       <td>{@code 10:30:45.123456789}</td>
 *       <td>{@code 10:30:45.123456789}</td>
 *       <td>{@code 10:30:45.123456789}</td>
 *     </tr>
 *   </tbody>
 * </table>
 * </div>
 *
 * <p>The plain {@code java.util.Date} default has second precision, so its example uses zero
 * milliseconds. SQL date/time/timestamp defaults and Calendar defaults retain milliseconds; a
 * {@code Timestamp} with additional sub-millisecond nanoseconds needs a fraction-bearing {@link DTF}
 * pattern to round-trip that finer precision.
 * {@code XMLGregorianCalendar} stores only a numeric offset, not a region ID. Its parser writes the
 * fraction at the narrowest scale of at least three digits ({@code .123}, {@code .500}, {@code .123456789}),
 * the same lexical form {@link #createXMLGregorianCalendar(long, TimeZone)} writes for whole
 * milliseconds. Select a fraction-bearing {@link DTF} pattern when sub-second {@code LocalTime}
 * precision must round-trip.</p>
 *
 * <p><b>Thread Safety:</b> Static operations and internal registries may be used concurrently. Mutable
 * arguments such as {@code Calendar}, {@code TimeZone}, and {@code Appendable} must not be modified
 * concurrently by the caller. Registered creator functions are invoked concurrently and must therefore
 * be thread-safe.</p>
 *
 * <p><b>Subtype-Preserving Operations:</b> generic operations returning the input's legacy
 * {@code Date} or {@code Calendar} subtype build the result by one rule: a registered creator if the
 * subtype has one, otherwise a declared {@code (long)} constructor (or, for {@code Calendar}, a declared
 * no-arg constructor) that can be invoked, otherwise {@code clone()} of the input. Cloning is what makes
 * subtypes with no usable constructor work, including the JDK's own {@code JapaneseImperialCalendar} and
 * {@code BuddhistCalendar}, which {@code Calendar.getInstance()} returns under some default locales.
 * They throw {@code IllegalStateException} when a creator, constructor, or {@code clone()} violates the
 * documented runtime-type, distinct-instance, settings, or requested-instant contract. See
 * {@link #registerDateCreator(Class, LongFunction)} and
 * {@link #registerCalendarCreator(Class, LongObjFunction)}.</p>
 *
 * <p><b>Attribution:</b>
 * Some methods are adapted from Apache Commons Lang, Google Guava, and other projects under the
 * Apache License 2.0.
 *
 * @see DTF
 * @see CalendarField
 * @see javax.xml.datatype.XMLGregorianCalendar
 */
public abstract sealed class Dates permits Dates.DateUtil {
    private static final Logger logger = LoggerFactory.getLogger(Dates.class);

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");

    /**
     * {@code ZoneId} of UTC time zone.
     * @see TimeZone#getTimeZone(String)
     * @see TimeZone#toZoneId()
     */
    public static final ZoneId UTC_ZONE_ID = UTC_TIME_ZONE.toZoneId();

    /**
     * {@code ZoneId} of GMT time zone.
     * @see TimeZone#getTimeZone(String)
     * @see TimeZone#toZoneId()
     */
    public static final ZoneId GMT_ZONE_ID = GMT_TIME_ZONE.toZoneId();

    /**
     * Date/Time format: {@code yyyy}.
     *
     * <p><b>Format-only for typed {@code Dates} parsing.</b> This constant is a formatting pattern
     * for grouping or filtering by year. Instant-bearing {@code Dates.parseTo*} methods (and the
     * matching {@link DTF} parsers for those targets) reject it as a partial date rather than
     * synthesizing 1 January. Use a complete-date pattern, or parse civil year text with
     * {@link DTF#of(String)} into a {@code TemporalAccessor} / {@code ChronoField#YEAR} if you need
     * the year field alone.</p>
     *
     * <p>Years are limited to an unsigned four-digit Common Era year ({@code 0001} through {@code 9999})
     * on both parsing and formatting.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date example = Dates.parseToJUDate("2023-12-25", Dates.LOCAL_DATE_FORMAT);
     * String year = Dates.format(example, Dates.LOCAL_YEAR_FORMAT);   // returns "2023"
     *
     * Calendar calendar = Calendar.getInstance();
     * String formattedYear = Dates.format(calendar, Dates.LOCAL_YEAR_FORMAT);
     *
     * // Typed Dates parsing rejects this format-only pattern as a partial date.
     * Dates.parseToJUDate("2023", Dates.LOCAL_YEAR_FORMAT);           // throws IllegalArgumentException
     * }</pre>
     *
     */
    public static final String LOCAL_YEAR_FORMAT = "yyyy";

    /**
     * Date/Time format: {@code MM-dd}.
     *
     * <p><b>Format-only for typed {@code Dates} parsing.</b> This constant is a formatting pattern
     * for recurring dates, anniversaries, and seasonal data. Instant-bearing {@code Dates.parseTo*}
     * methods (and the matching {@link DTF} parsers for those targets) reject it as a partial date
     * rather than assuming a year ({@code SimpleDateFormat} would otherwise silently use 1970).
     * Format with this pattern, or parse month-day text with {@link DTF#of(String)} when you need
     * civil fields without an instant.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date birthday = Dates.parseToJUDate("2023-12-25", Dates.LOCAL_DATE_FORMAT);
     * String monthDay = Dates.format(birthday, Dates.LOCAL_MONTH_DAY_FORMAT);   // returns "12-25"
     *
     * // Useful for anniversaries without year
     * Calendar anniversary = Calendar.getInstance();
     * String anniversaryStr = Dates.format(anniversary, Dates.LOCAL_MONTH_DAY_FORMAT);
     *
     * // Typed Dates parsing rejects this format-only pattern as a partial date.
     * Dates.parseToJUDate("12-25", Dates.LOCAL_MONTH_DAY_FORMAT);     // throws IllegalArgumentException
     * }</pre>
     *
     */
    public static final String LOCAL_MONTH_DAY_FORMAT = "MM-dd";

    // static final String LOCAL_MONTH_DAY_FORMAT_SLASH = "MM/dd";

    /**
     * Date/Time format: {@code yyyy-MM-dd}.
     *
     * <p>ISO 8601 date format without time. Standard format for representing dates in data exchange and databases.</p>
     *
     * <p>Parsing requires this exact shape: every field must be written at its full width
     * ({@code "2025-01-15"}); use a variable-width pattern such as {@code "yyyy-M-d"} to accept shorter fields.</p>
     *
     * <p><b>Round trip in a daylight-saving zone.</b> This pattern carries no offset, so a rendering
     * of an instant does not always identify one: parsing rejects a local date a daylight-saving gap
     * removes or an overlap repeats, and {@code format} can produce exactly such text: it writes
     * {@code "2018-11-04"} for an instant on that day in {@code America/Sao_Paulo}, where midnight does
     * not exist, and {@code parseTo*} then refuses that text. A fall-back overlap that repeats midnight
     * (00:00&ndash;01:00 in {@code America/Havana}, and 1950-04-16 in {@code America/Cuiaba}) is refused
     * for the same reason: the text names two instants and this pattern cannot say which, so it is
     * rejected rather than resolved to one of them &mdash; unlike
     * {@link #truncate(java.util.Date, int)}, which has an input instant to pick the side from. Use
     * {@link #ISO_OFFSET_DATE_TIME_FORMAT} or {@link #ISO_OFFSET_TIMESTAMP_FORMAT} when a rendering
     * must round-trip in every zone.</p>
     *
     * <p>Years are limited to an unsigned four-digit Common Era year ({@code 0001} through {@code 9999})
     * on both parsing and formatting.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String dateStr = "2023-12-25";
     * Date date = Dates.parseToDate(dateStr, Dates.LOCAL_DATE_FORMAT);
     *
     * java.util.Date example = Dates.parseToJUDate("2023-12-25", Dates.LOCAL_DATE_FORMAT);
     * String formatted = Dates.format(example, Dates.LOCAL_DATE_FORMAT);   // returns "2023-12-25"
     *
     * java.util.Calendar calendar = java.util.Calendar.getInstance();
     * String calendarDate = Dates.format(calendar, Dates.LOCAL_DATE_FORMAT);
     * // Using DTF instance for convenience
     * String dtfFormatted = DTF.LOCAL_DATE.format(example);
     * }</pre>
     *
     */
    public static final String LOCAL_DATE_FORMAT = "yyyy-MM-dd";

    // static final String LOCAL_DATE_FORMAT_SLASH = "yyyy/MM/dd";

    /**
     * Date/Time format: {@code HH:mm:ss}.
     *
     * <p>Time-only format in 24-hour notation. Useful for representing times without date context.</p>
     *
     * <p>Parsing requires this exact shape: every field must be written at its full width
     * ({@code "14:30:45"}); use a variable-width pattern such as {@code "H:m:s"} to accept shorter fields.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String timeStr = "14:30:45";
     * Time time = Dates.parseToTime(timeStr, Dates.LOCAL_TIME_FORMAT);
     * String formatted = Dates.format(time, Dates.LOCAL_TIME_FORMAT);   // returns "14:30:45"
     *
     * java.util.Calendar calendar = java.util.Calendar.getInstance();
     * String calendarTime = Dates.format(calendar, Dates.LOCAL_TIME_FORMAT);
     * // Using DTF instance
     * String dtfFormatted = DTF.LOCAL_TIME.format(time);
     *
     * // A time-only pattern carries no date: date-producing targets reject it.
     * Dates.parseToJUDate(timeStr, Dates.LOCAL_TIME_FORMAT);            // throws IllegalArgumentException
     * }</pre>
     *
     */
    public static final String LOCAL_TIME_FORMAT = "HH:mm:ss";

    /**
     * Date/Time format: {@code yyyy-MM-dd HH:mm:ss}.
     *
     * <p>Local date and time format without timezone. Useful for database timestamps and local system times.</p>
     *
     * <p>Parsing requires this exact shape: every field must be written at its full width
     * ({@code "2025-01-15 14:30:45"}); use a variable-width pattern such as {@code "yyyy-M-d H:m:s"} to accept shorter fields.</p>
     *
     * <p>Years are limited to an unsigned four-digit Common Era year ({@code 0001} through {@code 9999})
     * on both parsing and formatting.</p>
     *
     * <p><b>Round trip in a daylight-saving zone.</b> This pattern carries no offset, so a rendering
     * of an instant does not always identify one: parsing rejects a local date-time a daylight-saving
     * gap removes or an overlap repeats, and {@code format} can produce exactly such text: it writes
     * {@code "2025-11-02 01:30:00"} for the first pass of the autumn overlap in {@code America/New_York},
     * and {@code parseTo*} then refuses that text as ambiguous. Use
     * {@link #ISO_OFFSET_DATE_TIME_FORMAT} or {@link #ISO_OFFSET_TIMESTAMP_FORMAT} when a rendering
     * must round-trip in every zone.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String dateTimeStr = "2023-12-25 14:30:45";
     * Date date = Dates.parseToDate(dateTimeStr, Dates.LOCAL_DATE_TIME_FORMAT);
     *
     * java.util.Date example = Dates.parseToJUDate(dateTimeStr, Dates.LOCAL_DATE_TIME_FORMAT);
     * String formatted = Dates.format(example, Dates.LOCAL_DATE_TIME_FORMAT);   // returns "2023-12-25 14:30:45"
     *
     * java.util.Calendar calendar = java.util.Calendar.getInstance();
     * String calendarDateTime = Dates.format(calendar, Dates.LOCAL_DATE_TIME_FORMAT);
     * // Using DTF instance
     * String dtfFormatted = DTF.LOCAL_DATE_TIME.format(example);
     * }</pre>
     *
     */
    public static final String LOCAL_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * Date/Time format: {@code yyyy-MM-dd HH:mm:ss.SSS}.
     *
     * <p>Local date and time format with milliseconds. Provides high precision for local timestamps,
     * commonly used in application logging and database records.</p>
     *
     * <p>When supplied explicitly, the fraction is exactly three digits ({@code .SSS}). Auto-detection
     * instead recognizes the JDBC escape grammar, where the fraction is 1&ndash;9 digits read as a
     * fraction of a second ({@code java.sql.Timestamp.toString()} trims trailing zeros, e.g.
     * {@code "2025-01-15 10:30:45.5"} means 500&nbsp;ms).</p>
     *
     * <p>Parsing requires this exact shape: every field up to the seconds must be written at its full
     * width ({@code "2025-01-15 10:30:45.123"}); use a variable-width pattern such as
     * {@code "yyyy-M-d H:m:s.SSS"} to accept shorter fields.</p>
     *
     * <p>Years are limited to an unsigned four-digit Common Era year ({@code 0001} through {@code 9999})
     * on both parsing and formatting.</p>
     *
     * <p><b>Round trip in a daylight-saving zone.</b> This pattern carries no offset, so a rendering
     * of an instant does not always identify one: parsing rejects a local date-time a daylight-saving
     * gap removes or an overlap repeats, and {@code format} can produce exactly such text: it writes
     * {@code "2025-11-02 01:30:00.000"} for the first pass of the autumn overlap in {@code America/New_York},
     * and {@code parseTo*} then refuses that text as ambiguous. Use
     * {@link #ISO_OFFSET_DATE_TIME_FORMAT} or {@link #ISO_OFFSET_TIMESTAMP_FORMAT} when a rendering
     * must round-trip in every zone.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String timestampStr = "2023-12-25 14:30:45.123";
     * Date date = Dates.parseToDate(timestampStr, Dates.LOCAL_TIMESTAMP_FORMAT);
     *
     * Timestamp example = Dates.parseToTimestamp(timestampStr, Dates.LOCAL_TIMESTAMP_FORMAT);
     * String formatted = Dates.format(example, Dates.LOCAL_TIMESTAMP_FORMAT);   // returns "2023-12-25 14:30:45.123"
     *
     * java.util.Calendar calendar = java.util.Calendar.getInstance();
     * String calendarTimestamp = Dates.format(calendar, Dates.LOCAL_TIMESTAMP_FORMAT);
     * // High precision logging
     * System.out.println("Event occurred at: " + Dates.format(Dates.currentJUDate(), Dates.LOCAL_TIMESTAMP_FORMAT));
     *
     * // every field must be written at its full width: this used to read as 10:00:45
     * Dates.parseToTimestamp("2025-01-15 010:0:45.123", Dates.LOCAL_TIMESTAMP_FORMAT);
     *                                                                // throws IllegalArgumentException
     * }</pre>
     *
     */
    public static final String LOCAL_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss}.
     *
     * <p>ISO 8601 local date and time format without timezone. Standard format for data interchange
     * where the timezone is implicit or handled separately.</p>
     *
     * <p>Parsing requires this exact shape: every field must be written at its full width
     * ({@code "2025-01-15T14:30:45"}); use a variable-width pattern such as {@code "yyyy-M-d'T'H:m:s"} to accept shorter fields.</p>
     *
     * <p>Years are limited to an unsigned four-digit Common Era year ({@code 0001} through {@code 9999})
     * on both parsing and formatting.</p>
     *
     * <p><b>Round trip in a daylight-saving zone.</b> This pattern carries no offset, so a rendering
     * of an instant does not always identify one: parsing rejects a local date-time a daylight-saving
     * gap removes or an overlap repeats, and {@code format} can produce exactly such text: it writes
     * {@code "2025-11-02T01:30:00"} for the first pass of the autumn overlap in {@code America/New_York},
     * and {@code parseTo*} then refuses that text as ambiguous. Use
     * {@link #ISO_OFFSET_DATE_TIME_FORMAT} or {@link #ISO_OFFSET_TIMESTAMP_FORMAT} when a rendering
     * must round-trip in every zone.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String isoStr = "2023-12-25T14:30:45";
     * Date date = Dates.parseToDate(isoStr, Dates.ISO_LOCAL_DATE_TIME_FORMAT);
     *
     * java.util.Date example = Dates.parseToJUDate(isoStr, Dates.ISO_LOCAL_DATE_TIME_FORMAT);
     * String formatted = Dates.format(example, Dates.ISO_LOCAL_DATE_TIME_FORMAT);   // returns "2023-12-25T14:30:45"
     * // Using DTF instance for convenience
     * String dtfFormatted = DTF.ISO_LOCAL_DATE_TIME.format(example);
     *
     * java.time.LocalDateTime localDateTime = java.time.LocalDateTime.now();
     * String isoFormatted = DTF.ISO_LOCAL_DATE_TIME.format(localDateTime);
     * }</pre>
     *
     * @see DateTimeFormatter#ISO_LOCAL_DATE_TIME
     */
    public static final String ISO_LOCAL_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss.SSS}.
     *
     * <p>ISO 8601 local date-time with milliseconds and no zone designator &mdash; the millisecond-bearing
     * sibling of {@link #ISO_LOCAL_DATE_TIME_FORMAT}, and the T-separated sibling of
     * {@link #LOCAL_TIMESTAMP_FORMAT}. Auto-detection returns this shape for T-separated timestamp text
     * that has a fractional second but no trailing {@code 'Z'}.</p>
     *
     * <p>When supplied explicitly, the fraction is exactly three digits ({@code .SSS}); auto-detection
     * additionally accepts a 1&ndash;9 digit fraction of a second.</p>
     *
     * <p>Parsing requires this exact shape: every field up to the seconds must be written at its full
     * width ({@code "2025-01-15T10:30:45.123"}); use a variable-width pattern such as
     * {@code "yyyy-M-d'T'H:m:s.SSS"} to accept shorter fields.</p>
     *
     * <p>Years are limited to an unsigned four-digit Common Era year ({@code 0001} through {@code 9999})
     * on both parsing and formatting.</p>
     *
     * <p><b>Round trip in a daylight-saving zone.</b> This pattern carries no offset, so a rendering
     * of an instant does not always identify one: parsing rejects a local date-time a daylight-saving
     * gap removes or an overlap repeats, and {@code format} can produce exactly such text: it writes
     * {@code "2025-11-02T01:30:00.000"} for the first pass of the autumn overlap in {@code America/New_York},
     * and {@code parseTo*} then refuses that text as ambiguous. Use
     * {@link #ISO_OFFSET_DATE_TIME_FORMAT} or {@link #ISO_OFFSET_TIMESTAMP_FORMAT} when a rendering
     * must round-trip in every zone.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Timestamp example = Dates.parseToTimestamp("2023-12-25T14:30:45.123", Dates.ISO_LOCAL_TIMESTAMP_FORMAT, utc);
     * Dates.format(example, Dates.ISO_LOCAL_TIMESTAMP_FORMAT, utc);   // returns "2023-12-25T14:30:45.123"
     *
     * // exactly three fraction digits are required when the constant is supplied explicitly
     * Dates.parseToTimestamp("2023-12-25T14:30:45.1", Dates.ISO_LOCAL_TIMESTAMP_FORMAT, utc);
     *                                                                // throws IllegalArgumentException
     *
     * // and every field must be written at its full width: this used to read as 10:00:45
     * Dates.parseToTimestamp("2025-01-15T010:0:45.123", Dates.ISO_LOCAL_TIMESTAMP_FORMAT, utc);
     *                                                                // throws IllegalArgumentException
     * }</pre>
     *
     * @see #ISO_LOCAL_DATE_TIME_FORMAT
     * @see #LOCAL_TIMESTAMP_FORMAT
     * @see #ISO_8601_TIMESTAMP_FORMAT
     */
    public static final String ISO_LOCAL_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    /**
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ssXXX}.
     *
     * <p>ISO 8601 date and time with UTC offset. Useful for APIs and services that need to represent
     * a moment in time with explicit offset but without timezone identification. Legacy parsing and
     * formatting require the canonical {@code Z} or {@code [+-]HH:mm} suffix (parsing also accepts the
     * basic {@code [+-]HHmm} form, as does auto-detection, with or without a fraction); numeric offsets are
     * limited to -18:00 through +18:00 and must have whole-minute precision.</p>
     *
     * <p>Years are limited to an unsigned four-digit Common Era year ({@code 0001} through {@code 9999})
     * on both parsing and formatting.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String offsetStr = "2023-12-25T14:30:45+05:30";
     * Dates.parseToDate(offsetStr, Dates.ISO_OFFSET_DATE_TIME_FORMAT).getTime();   // returns 1703494845000
     *
     * // the same pattern through a DTF instance, which also accepts the java.time types
     * OffsetDateTime offsetDT = OffsetDateTime.parse(offsetStr);
     * DTF.ISO_OFFSET_DATE_TIME.format(offsetDT);                           // returns "2023-12-25T14:30:45+05:30"
     * DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime("2023-12-25T14:30:45Z");
     *                                                                     // returns 2023-12-25T14:30:45Z
     * }</pre>
     *
     * @see DateTimeFormatter#ISO_OFFSET_DATE_TIME
     */
    public static final String ISO_OFFSET_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    /**
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss.SSSXXX}.
     *
     * <p>The millisecond-bearing sibling of {@link #ISO_OFFSET_DATE_TIME_FORMAT}. This is the default
     * written by {@code format(value, null, timeZone)} for {@code java.sql.Date}, {@link Time} and
     * {@link Timestamp}, so that an explicitly-zoned default rendering still identifies one instant and
     * parses back to the same epoch value.</p>
     *
     * <p>Legacy parsing and formatting require the canonical {@code Z} or {@code [+-]HH:mm} suffix
     * (parsing also accepts the basic {@code [+-]HHmm} form); numeric offsets are limited to -18:00
     * through +18:00 and must have whole-minute precision. The fraction is exactly three digits.</p>
     *
     * <p>Years are limited to an unsigned four-digit Common Era year ({@code 0001} through {@code 9999})
     * on both parsing and formatting.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
     * Timestamp value = new Timestamp(1736937045123L);
     * Dates.format(value, Dates.ISO_OFFSET_TIMESTAMP_FORMAT, kolkata);
     *                                                     // returns "2025-01-15T16:00:45.123+05:30"
     * Dates.parseToTimestamp("2025-01-15T16:00:45.123+05:30", Dates.ISO_OFFSET_TIMESTAMP_FORMAT).getTime();
     *                                                     // returns 1736937045123
     *
     * Dates.parseToTimestamp("2025-01-15T16:00:45.123", Dates.ISO_OFFSET_TIMESTAMP_FORMAT);
     *                                                     // throws IllegalArgumentException (no offset)
     * }</pre>
     *
     * @see #ISO_OFFSET_DATE_TIME_FORMAT
     * @see #format(java.util.Date, String, TimeZone)
     */
    public static final String ISO_OFFSET_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    /**
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'}.
     *
     * <p>ISO 8601 format with complete timezone information including offset and timezone ID.
     * Most comprehensive format for representing a moment in time with full context.</p>
     *
     * <p>Years are limited to an unsigned four-digit Common Era year ({@code 0001} through {@code 9999})
     * on both parsing and formatting.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zonedDT = ZonedDateTime.of(2023, 12, 25, 15, 30, 45, 0, ZoneId.of("America/New_York"));
     * DTF.ISO_ZONED_DATE_TIME.format(zonedDT);   // returns "2023-12-25T15:30:45-05:00[America/New_York]"
     *
     * DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime("2023-12-25T14:25:30+05:30[Asia/Kolkata]");
     *                                            // returns 2023-12-25T14:25:30+05:30[Asia/Kolkata]
     * }</pre>
     *
     * <p>Unlike the sibling {@link SimpleDateFormat}-compatible constants, the {@code XXX'['VV']'}
     * portion is only meaningful to {@link DateTimeFormatter}. Dates routes this named constant through
     * its strict java.time engine, and {@code DTF.ISO_ZONED_DATE_TIME} is the direct formatter
     * counterpart.</p>
     *
     * <p><b>The emitted offset can be wider than {@code XXX} declares.</b> Both this constant and its
     * {@code DTF} counterpart write offset seconds when the zone has them, so a historical local-mean-time
     * offset comes out at seconds precision: {@code format(new java.util.Date(0L), ISO_ZONED_DATE_TIME_FORMAT,
     * TimeZone.getTimeZone("Africa/Monrovia"))} returns
     * {@code "1969-12-31T23:15:30-00:44:30[Africa/Monrovia]"}. Whole-minute output is unchanged, and the
     * value still parses back with this constant. This is the one predefined constant that widens its
     * offset field; {@link #ISO_OFFSET_DATE_TIME_FORMAT} instead rejects a sub-minute offset.</p>
     *
     * @see DateTimeFormatter#ISO_ZONED_DATE_TIME
     */
    public static final String ISO_ZONED_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'";

    /**
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss'Z'}.
     *
     * <p>ISO 8601 UTC date and time format (Zulu time). This is the default date/time format
     * for UTC representation. Widely used in APIs, logs, and communication protocols.</p>
     *
     * <p>Parsing requires this exact shape: every field must be written at its full width
     * ({@code "2025-01-15T14:30:45Z"}); use a variable-width pattern such as {@code "yyyy-M-d'T'H:m:s'Z'"} to accept shorter fields.</p>
     *
     * <p>Years are limited to an unsigned four-digit Common Era year ({@code 0001} through {@code 9999})
     * on both parsing and formatting.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String utcStr = "2023-12-25T14:30:45Z";
     * Date date = Dates.parseToDate(utcStr, Dates.ISO_8601_DATE_TIME_FORMAT);
     *
     * java.util.Date example = Dates.parseToJUDate(utcStr, Dates.ISO_8601_DATE_TIME_FORMAT);
     * String formatted = Dates.format(example, Dates.ISO_8601_DATE_TIME_FORMAT);   // returns "2023-12-25T14:30:45Z"
     * // Using DTF instance
     * java.time.ZonedDateTime utcTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("UTC"));
     * String utcFormatted = DTF.ISO_8601_DATE_TIME.format(utcTime);
     * }</pre>
     *
     */
    public static final String ISO_8601_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    // static final String ISO_8601_DATE_TIME_FORMAT_SLASH = "yyyy/MM/dd'T'HH:mm:ss'Z'";

    /**
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}.
     *
     * <p>ISO 8601 UTC timestamp format with milliseconds. This is the default timestamp format
     * for high-precision UTC representation. Ideal for precise logging, monitoring, and distributed systems.</p>
     *
     * <p>When supplied explicitly, the fraction is exactly three digits ({@code .SSS}); auto-detection
     * additionally accepts a 1&ndash;9 digit fraction of a second before the {@code 'Z'}.</p>
     *
     * <p>Parsing requires this exact shape: every field up to the seconds must be written at its full
     * width ({@code "2025-01-15T10:30:45.123Z"}); use a variable-width pattern such as
     * {@code "yyyy-M-d'T'H:m:s.SSS'Z'"} to accept shorter fields.</p>
     *
     * <p>Years are limited to an unsigned four-digit Common Era year ({@code 0001} through {@code 9999})
     * on both parsing and formatting.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String timestampStr = "2023-12-25T14:30:45.123Z";
     * Date date = Dates.parseToDate(timestampStr, Dates.ISO_8601_TIMESTAMP_FORMAT);
     *
     * Timestamp example = Dates.parseToTimestamp(timestampStr, Dates.ISO_8601_TIMESTAMP_FORMAT);
     * String formatted = Dates.format(example, Dates.ISO_8601_TIMESTAMP_FORMAT);   // returns "2023-12-25T14:30:45.123Z"
     * // Default format for system logging
     * System.out.println("Server started at: " + Dates.format(Dates.currentJUDate(), Dates.ISO_8601_TIMESTAMP_FORMAT));
     *
     * // every field must be written at its full width: this used to read as 10:00:45
     * Dates.parseToTimestamp("2025-01-15T010:0:45.123Z", Dates.ISO_8601_TIMESTAMP_FORMAT);
     *                                                                // throws IllegalArgumentException
     * }</pre>
     *
     */
    public static final String ISO_8601_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    // static final String ISO_8601_TIMESTAMP_FORMAT_SLASH = "yyyy/MM/dd'T'HH:mm:ss.SSS'Z'";

    /**
     * Strict HTTP-date format (the IMF-fixdate form): {@code EEE, dd MMM yyyy HH:mm:ss 'GMT'}.
     * Formatting always converts the instant to GMT, and parsing accepts the literal {@code GMT}
     * only. It therefore produces values suitable for HTTP {@code Date}, {@code Expires}, and
     * {@code Last-Modified} headers. Formatting is limited to Common Era years 0001 through 9999,
     * the four-digit range representable by the HTTP grammar; dates use the proleptic Gregorian
     * calendar (there is no legacy 1582 cutover).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone gmt = TimeZone.getTimeZone("GMT");
     * java.util.Date date = Dates.parseToJUDate("Mon, 25 Dec 2023 14:30:45 GMT", Dates.HTTP_DATE_FORMAT, gmt);
     * date.getTime();                                             // returns 1703514645000
     *
     * // the value suitable for a Last-Modified, Date, or Expires header
     * Dates.format(date, Dates.HTTP_DATE_FORMAT, gmt);            // returns "Mon, 25 Dec 2023 14:30:45 GMT"
     * DTF.HTTP_DATE.format(date);                                 // returns "Mon, 25 Dec 2023 14:30:45 GMT"
     *
     * // the weekday must agree with the date, and the zone must be GMT
     * Dates.parseToJUDate("Tue, 25 Dec 2023 14:30:45 GMT", Dates.HTTP_DATE_FORMAT, gmt);
     *                                                             // throws IllegalArgumentException
     * }</pre>
     *
     */
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss 'GMT'";

    /**
     * Alias for {@link #HTTP_DATE_FORMAT}. This formatter implements the GMT-only HTTP-date subset,
     * not a general RFC 1123 parser for arbitrary named or numeric zones.
     *
     * @deprecated use {@link #HTTP_DATE_FORMAT}, whose name describes the supported grammar precisely.
     */
    @Deprecated
    public static final String RFC_1123_DATE_TIME_FORMAT = HTTP_DATE_FORMAT;

    /**
     * Sentinel value representing half a month; may be passed as the {@code field} argument of the
     * {@code round}, {@code truncate}, and {@code ceiling} methods to operate at half-month
     * granularity (the first vs. the second half of the month).
     *
     * @see #round(java.util.Date, int)
     * @see #truncate(java.util.Date, int)
     * @see #ceiling(java.util.Date, int)
     */
    public static final int SEMI_MONTH = 1001;

    @SuppressWarnings("deprecation")
    private static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    // Pattern values are caller controlled. Bound the number of queues retained so a service
    // that receives many one-off formats cannot grow this process-wide cache indefinitely.
    private static final int MAX_POOLED_FORMATS = 64;

    private static final Map<DateFormatKey, Queue<DateFormat>> dfPool = new ConcurrentCacheMap<>(MAX_POOLED_FORMATS);

    /** Pool key: DateFormatSymbols and calendar construction are locale-sensitive. */
    private record DateFormatKey(String format, Locale locale) {
    }

    private static final Queue<DateFormat> utcTimestampDFPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private static final Queue<DateFormat> utcDateTimeDFPool = new ArrayBlockingQueue<>(POOL_SIZE);

    // Reusable buffers for UTC timestamp formatting.
    private static final Queue<char[]> utcTimestampFormatCharsPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private static final DatatypeFactory dataTypeFactory;

    static {
        DatatypeFactory temp = null;

        try {
            temp = DatatypeFactory.newInstance();
        } catch (final Exception e) {
            logger.error("Failed to initialize DatatypeFactory; XMLGregorianCalendar operations will throw UnsupportedOperationException", e);
        }

        dataTypeFactory = temp;
    }

    private static final Map<Class<? extends java.util.Date>, LongFunction<? extends java.util.Date>> dateCreatorPool = new ConcurrentHashMap<>();

    static {
        dateCreatorPool.put(java.util.Date.class, java.util.Date::new);
        dateCreatorPool.put(java.sql.Date.class, java.sql.Date::new);
        dateCreatorPool.put(Time.class, Time::new);
        dateCreatorPool.put(Timestamp.class, Timestamp::new);
    }

    private static final Map<Class<? extends java.util.Calendar>, LongObjFunction<? super Calendar, ? extends java.util.Calendar>> calendarCreatorPool = new ConcurrentHashMap<>();

    static {
        calendarCreatorPool.put(java.util.GregorianCalendar.class, (millis, c) -> {
            // Must be `new GregorianCalendar()`, not `Calendar.getInstance()`: under a default locale
            // that selects a non-Gregorian calendar (e.g. ja-JP-u-ca-japanese), getInstance() returns a
            // JapaneseImperialCalendar, which would ClassCastException at the GregorianCalendar caller.
            final GregorianCalendar ret = new GregorianCalendar();

            copyCalendarSettings(c, ret);
            ret.setTimeInMillis(millis);

            return ret;
        });
    }

    // Propagates the behavioral settings of the source/template calendar — not just the time zone —
    // so round/add/truncate-style operations don't silently reset leniency or week conventions.
    private static void copyCalendarSettings(final Calendar source, final Calendar target) {
        // Read the zone once - a Calendar implementation may answer differently on a second call.
        final TimeZone sourceZone = source.getTimeZone();

        if (sourceZone != null) {
            target.setTimeZone((TimeZone) sourceZone.clone());
        }

        target.setLenient(source.isLenient());
        target.setFirstDayOfWeek(source.getFirstDayOfWeek());
        target.setMinimalDaysInFirstWeek(source.getMinimalDaysInFirstWeek());

        if (source instanceof GregorianCalendar && target instanceof GregorianCalendar) {
            // Preserve a caller-customized Julian/Gregorian cutover; a freshly constructed
            // GregorianCalendar would silently revert to the JDK default (1582).
            ((GregorianCalendar) target).setGregorianChange(((GregorianCalendar) source).getGregorianChange());
        }
    }

    /**
     * The calendar this class computes with internally, in a defensive copy of {@code timeZone}. The
     * public {@code current*}/{@code create*} factories and subtype reconstruction deliberately do not
     * use it: they must retain JDK and caller semantics, including the default calendar system and
     * cutover. Two properties matter here and neither is the default: the calendar system must be
     * Gregorian (a {@code BuddhistCalendar} or {@code JapaneseImperialCalendar} from the default locale
     * reports different years and restarts {@code DAY_OF_YEAR} at an era change), and the
     * Julian/Gregorian cutover must be pushed out of range so civil fields stay proleptic. Formatting,
     * parsing, rounding and the {@code java.time} helpers are all proleptic; field arithmetic that used a
     * default {@link GregorianCalendar} read pre-1582 instants off from the value {@code format} prints
     * for them (ten days just before the 1582 cutover, less the earlier the instant, zero around AD 300,
     * and then a growing excess: eleven days by 1200 BC).
     */
    private static GregorianCalendar newProlepticGregorianCalendar(final TimeZone timeZone) {
        final GregorianCalendar result = new GregorianCalendar((TimeZone) timeZone.clone());
        result.setGregorianChange(new java.util.Date(Long.MIN_VALUE));
        return result;
    }

    /** As {@link #newProlepticGregorianCalendar(TimeZone)}, with explicit locale-dependent week settings. */
    private static GregorianCalendar newProlepticGregorianCalendar(final TimeZone timeZone, final Locale locale) {
        final GregorianCalendar result = new GregorianCalendar((TimeZone) timeZone.clone(), locale);
        result.setGregorianChange(new java.util.Date(Long.MIN_VALUE));
        return result;
    }

    /**
     * Returns an independent clone with the same runtime type. Custom calendar implementations are
     * supported by this class, so a broken/hostile {@code clone()} must be rejected before the clone is
     * mutated or exposed to a registered creator.
     */
    private static Calendar cloneCalendar(final Calendar source) {
        final Object cloned = source.clone();

        if (!(cloned instanceof Calendar) || cloned == source || cloned.getClass() != source.getClass()) {
            throw new IllegalStateException(
                    "Calendar.clone() for " + source.getClass().getName() + " must return a distinct Calendar of the same runtime class");
        }

        return (Calendar) cloned;
    }

    Dates() {
        // Utility class - prevent instantiation
    }

    /**
     * Registers a custom date creator for a specific date class.
     *
     * <p>This method implements a pluggable factory pattern that allows third-party date classes to be created by the Dates utility.
     * The date creator function must accept one argument: the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * It must return a new object whose runtime class is exactly {@code dateClass} and whose
     * {@link java.util.Date#getTime()} value equals that argument. Results are validated when used.</p>
     *
     * <p>Only custom date classes (those not in the {@code java.*}, {@code javax.*}, or {@code com.landawn.abacus.*} packages)
     * can be registered. Built-in classes cannot be overridden for security and stability reasons.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     *
     * class MyDate extends java.util.Date {
     *     MyDate(long millis) { super(millis); }
     * }
     * Dates.registerDateCreator(MyDate.class, MyDate::new);                           // returns true (first registration)
     * Dates.registerDateCreator(MyDate.class, MyDate::new);                           // returns false (already registered)
     *
     * Dates.registerDateCreator(java.util.Date.class, java.util.Date::new);           // throws IllegalArgumentException (java.* is restricted)
     * Dates.registerDateCreator(java.sql.Timestamp.class, java.sql.Timestamp::new);   // throws IllegalArgumentException (java.* is restricted)
     * }</pre>
     *
     * @param <T> the type of the date class extending {@code java.util.Date}.
     * @param dateClass the class of the date to register the creator for, must not be in restricted packages. Not {@code null}.
     * @param dateCreator the thread-safe function that creates a distinct instance of exactly
     *        {@code dateClass}, taking the time in milliseconds as its argument. Not {@code null}.
     * @return {@code true} if the date creator was successfully registered, {@code false} if the class was already registered.
     *         Registration is atomic for a given class, so at most one concurrent caller can succeed.
     * @throws IllegalArgumentException if {@code dateClass} or {@code dateCreator} is {@code null}, or if
     *         {@code dateClass} is from a restricted package ({@code java.*}, {@code javax.*}, or
     *         {@code com.landawn.abacus.*}).
     * @see #registerCalendarCreator(Class, LongObjFunction)
     */
    public static <T extends java.util.Date> boolean registerDateCreator(final Class<T> dateClass, final LongFunction<? extends T> dateCreator)
            throws IllegalArgumentException {
        N.checkArgNotNull(dateClass, cs.dateClass);
        N.checkArgNotNull(dateCreator, cs.dateCreator);

        if (isRestrictedCreatorPackage(dateClass)) {
            throw new IllegalArgumentException(
                    "Cannot register a creator for a class from a restricted package (java.*, javax.*, com.landawn.abacus.*): " + dateClass.getName());
        }

        return dateCreatorPool.putIfAbsent(dateClass, dateCreator) == null;
    }

    /**
     * Removes the creator registered for a custom date class. Built-in and otherwise restricted
     * classes cannot be unregistered.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class MyDate extends java.util.Date {
     *     MyDate(long millis) { super(millis); }
     * }
     * Dates.registerDateCreator(MyDate.class, MyDate::new);
     *
     * Dates.unregisterDateCreator(MyDate.class);   // returns true (the registration was removed)
     * Dates.unregisterDateCreator(MyDate.class);   // returns false (nothing left to remove)
     *
     * Dates.unregisterDateCreator(java.sql.Timestamp.class);   // throws IllegalArgumentException (java.* is restricted)
     * Dates.unregisterDateCreator(null);                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param dateClass the custom date class whose registration is to be removed.
     * @return {@code true} if a registration was removed, otherwise {@code false}.
     * @throws IllegalArgumentException if {@code dateClass} is {@code null} or belongs to a restricted package.
     * @see #registerDateCreator(Class, LongFunction)
     */
    public static boolean unregisterDateCreator(final Class<? extends java.util.Date> dateClass) throws IllegalArgumentException {
        N.checkArgNotNull(dateClass, cs.dateClass);

        if (isRestrictedCreatorPackage(dateClass)) {
            throw new IllegalArgumentException(
                    "Cannot unregister a creator for a class from a restricted package (java.*, javax.*, com.landawn.abacus.*): " + dateClass.getName());
        }

        return dateCreatorPool.remove(dateClass) != null;
    }

    /**
     * Registers a custom calendar creator for a specific calendar class.
     *
     * <p>This method implements a pluggable factory pattern that allows third-party calendar classes to be created by the Dates utility.
     * The calendar creator function must accept two arguments: the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT) and a
     * {@code Calendar} instance to use as a template. The template is an isolated clone of the caller's
     * calendar and may be read or modified by the creator without mutating the caller's object. The
     * creator must return a distinct object whose runtime class is exactly {@code calendarClass}; the
     * caller's calendar settings are reapplied and the requested instant is validated before return.</p>
     *
     * <p>Only custom calendar classes (those not in the {@code java.*}, {@code javax.*}, or {@code com.landawn.abacus.*} packages)
     * can be registered. Built-in classes cannot be overridden for security and stability reasons.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     *
     * class MyCal extends java.util.GregorianCalendar {
     *     MyCal(long millis, Calendar template) { super(); setTimeInMillis(millis); }
     * }
     * Dates.registerCalendarCreator(MyCal.class, (millis, tmpl) -> new MyCal(millis, tmpl));   // returns true
     * Dates.registerCalendarCreator(MyCal.class, (millis, tmpl) -> new MyCal(millis, tmpl));   // returns false (already registered)
     *
     * Dates.registerCalendarCreator(java.util.GregorianCalendar.class,
     *         (millis, tmpl) -> new java.util.GregorianCalendar());   // throws IllegalArgumentException (java.* is restricted)
     * }</pre>
     *
     * @param <T> the type of the calendar class extending {@code java.util.Calendar}.
     * @param calendarClass the class of the calendar to register the creator for, must not be in restricted packages. Not {@code null}.
     * @param calendarCreator the thread-safe function that creates a distinct instance of exactly
     *        {@code calendarClass}, taking the time in milliseconds and an isolated {@code Calendar}
     *        template as arguments. Not {@code null}.
     * @return {@code true} if the calendar creator was successfully registered, {@code false} if the class was already registered.
     *         Registration is atomic for a given class, so at most one concurrent caller can succeed.
     * @throws IllegalArgumentException if {@code calendarClass} or {@code calendarCreator} is {@code null}, or if
     *         {@code calendarClass} is from a restricted package ({@code java.*}, {@code javax.*}, or
     *         {@code com.landawn.abacus.*}).
     * @see #registerDateCreator(Class, LongFunction)
     */
    public static <T extends java.util.Calendar> boolean registerCalendarCreator(final Class<T> calendarClass,
            final LongObjFunction<? super Calendar, ? extends T> calendarCreator) throws IllegalArgumentException {
        N.checkArgNotNull(calendarClass, cs.calendarClass);
        N.checkArgNotNull(calendarCreator, cs.calendarCreator);

        if (isRestrictedCreatorPackage(calendarClass)) {
            throw new IllegalArgumentException(
                    "Cannot register a creator for a class from a restricted package (java.*, javax.*, com.landawn.abacus.*): " + calendarClass.getName());
        }

        return calendarCreatorPool.putIfAbsent(calendarClass, calendarCreator) == null;
    }

    /**
     * Removes the creator registered for a custom calendar class. Built-in and otherwise restricted
     * classes cannot be unregistered.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class MyCal extends java.util.GregorianCalendar {
     *     MyCal(long millis, Calendar template) { super(); setTimeInMillis(millis); }
     * }
     * Dates.registerCalendarCreator(MyCal.class, (millis, tmpl) -> new MyCal(millis, tmpl));
     *
     * Dates.unregisterCalendarCreator(MyCal.class);   // returns true (the registration was removed)
     * Dates.unregisterCalendarCreator(MyCal.class);   // returns false (nothing left to remove)
     *
     * Dates.unregisterCalendarCreator(java.util.GregorianCalendar.class);   // throws IllegalArgumentException (java.* is restricted)
     * Dates.unregisterCalendarCreator(null);                                // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendarClass the custom calendar class whose registration is to be removed.
     * @return {@code true} if a registration was removed, otherwise {@code false}.
     * @throws IllegalArgumentException if {@code calendarClass} is {@code null} or belongs to a restricted package.
     * @see #registerCalendarCreator(Class, LongObjFunction)
     */
    public static boolean unregisterCalendarCreator(final Class<? extends Calendar> calendarClass) throws IllegalArgumentException {
        N.checkArgNotNull(calendarClass, cs.calendarClass);

        if (isRestrictedCreatorPackage(calendarClass)) {
            throw new IllegalArgumentException(
                    "Cannot unregister a creator for a class from a restricted package (java.*, javax.*, com.landawn.abacus.*): " + calendarClass.getName());
        }

        return calendarCreatorPool.remove(calendarClass) != null;
    }

    private static boolean isRestrictedCreatorPackage(final Class<?> cls) {
        final String packageName = ClassUtil.getPackageName(cls);
        return Strings.startsWithAny(packageName, "java.", "javax.") || N.equals(packageName, "com.landawn.abacus")
                || packageName.startsWith("com.landawn.abacus.");
    }

    /**
     * Returns the current time in milliseconds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long millis = Dates.currentTimeMillis();   // returns current epoch millis
     * Date date = new Date(millis);
     * assert millis > 0L;                             // returns true (current time is after 1970-01-01)
     * }</pre>
     *
     * <p>Note: this method is exactly equivalent to {@link System#currentTimeMillis()}; it exists
     * only for discoverability alongside the other {@code current*} methods. It is not to be
     * confused with {@link #currentTime()}, which returns a {@code java.sql.Time} object.</p>
     *
     * @return the current time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @deprecated this method is byte-for-byte identical to {@link System#currentTimeMillis()} and
     *             exists only as a confusion hazard with {@link #currentTime()}; call
     *             {@link System#currentTimeMillis()} directly instead.
     * @see System#currentTimeMillis()
     * @see #currentTime()
     */
    @Deprecated
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Returns a new instance of {@code java.sql.Time} wrapping the current epoch milliseconds as-is.
     *
     * <p>The value is not normalized to the 1970-01-01 date component the JDBC {@code java.sql.Time}
     * contract describes, and no default-zone or locale conversion is involved (epoch milliseconds are
     * zone-independent). For a normalized SQL time, use {@code Time.valueOf(LocalTime.now())} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Time t = Dates.currentTime();    // returns a new Time for now
     * assert t != null;                // returns true (never null)
     * Time t2 = Dates.currentTime();   // returns another Time instance
     * assert t != t2;                  // returns true (each call returns a new object)
     * }</pre>
     *
     * @return a new {@code java.sql.Time} instance representing the current time.
     */
    public static Time currentTime() {
        return new Time(System.currentTimeMillis());
    }

    /**
     * Returns a new instance of {@code java.sql.Date} wrapping the current epoch milliseconds as-is.
     *
     * <p>The value retains the current time-of-day; it is not normalized to midnight as the JDBC
     * {@code java.sql.Date} contract describes, and no default-zone or locale conversion is involved
     * (epoch milliseconds are zone-independent). For a normalized SQL date, use
     * {@code Date.valueOf(LocalDate.now())} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date d = Dates.currentDate();    // returns a new Date for today
     * assert d != null;                // returns true (never null)
     * Date d2 = Dates.currentDate();   // returns another Date instance
     * assert d != d2;                  // returns true (each call returns a new object)
     * }</pre>
     *
     * @return a new {@code java.sql.Date} instance representing the current date.
     */
    public static Date currentDate() {
        return new Date(System.currentTimeMillis());
    }

    /**
     * Returns a new instance of {@code java.sql.Timestamp} wrapping the current epoch milliseconds.
     * No default-zone or locale conversion is involved; epoch milliseconds are zone-independent.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Timestamp ts = Dates.currentTimestamp();    // returns a new Timestamp for now
     * assert ts != null;                          // returns true (never null)
     * Timestamp ts2 = Dates.currentTimestamp();   // returns another Timestamp instance
     * assert ts != ts2;                           // returns true (each call returns a new object)
     * }</pre>
     *
     * @return a new {@code java.sql.Timestamp} instance representing the current date and time with millisecond precision.
     */
    public static Timestamp currentTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    /**
     * Returns a new instance of {@code java.util.Date} wrapping the current epoch milliseconds.
     * No default-zone or locale conversion is involved; epoch milliseconds are zone-independent.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date d = Dates.currentJUDate();    // returns a new java.util.Date for now
     * assert d != null;                            // returns true (never null)
     * java.util.Date d2 = Dates.currentJUDate();   // returns another java.util.Date instance
     * assert d != d2;                              // returns true (each call returns a new object)
     * }</pre>
     *
     * @return a new {@code java.util.Date} instance representing the current date and time.
     */
    public static java.util.Date currentJUDate() {
        return new java.util.Date();
    }

    /**
     * Returns a new instance of {@code java.util.Calendar} based on the current time in the default time zone with the default locale.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.currentCalendar();
     * int year = cal.get(Calendar.YEAR);   // year is the current year
     * assert cal != null;                  // returns true (never null)
     * assert year >= 1970;                 // returns true (current year is after the epoch)
     * }</pre>
     *
     * @return a new {@code Calendar} instance representing the current date and time.
     */
    public static Calendar currentCalendar() {
        return Calendar.getInstance();
    }

    /**
     * Returns a new instance of {@code GregorianCalendar} based on the current time in the default time zone with the default locale.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GregorianCalendar cal = Dates.currentGregorianCalendar();
     * int year = cal.get(Calendar.YEAR);   // year is the current year
     * assert cal != null;                  // returns true (never null)
     * assert year >= 1970;                 // returns true (current year is after the epoch)
     * }</pre>
     *
     * @return a new {@code GregorianCalendar} instance representing the current date and time.
     */
    public static GregorianCalendar currentGregorianCalendar() {
        return new GregorianCalendar();
    }

    /**
     * Returns a new instance of {@code XMLGregorianCalendar} based on the current Gregorian Calendar.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar xmlCal = Dates.currentXMLGregorianCalendar();
     * int year = xmlCal.getYear();   // year is the current year
     * assert xmlCal != null;         // returns true (never null when DatatypeFactory is available)
     * assert year >= 1970;           // returns true (current year is after the epoch)
     * }</pre>
     *
     * @return a new {@code XMLGregorianCalendar} instance representing the current date and time.
     * @throws IllegalArgumentException if the current offset of the default time zone is not a whole
     *         number of minutes in the range -14:00 through +14:00, which XML Schema cannot represent.
     * @throws UnsupportedOperationException if the {@code DatatypeFactory} is not available.
     * @see #createXMLGregorianCalendar(long)
     */
    public static XMLGregorianCalendar currentXMLGregorianCalendar() throws IllegalArgumentException, UnsupportedOperationException {
        return createXMLGregorianCalendar(System.currentTimeMillis());
    }

    /**
     * Calculates the current time in milliseconds with the specified time amount added or subtracted.
     * This method adds or subtracts the given amount in the specified time unit to the current system time.
     * Nanosecond and microsecond amounts are truncated toward zero to whole milliseconds; conversions
     * that would overflow {@code long} throw rather than saturate like {@link TimeUnit#toMillis(long)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long futureTime = Dates.currentTimeMillisPlus(5, TimeUnit.MINUTES);   // futureTime is about 5 minutes from now
     * long pastTime = Dates.currentTimeMillisPlus(-2, TimeUnit.HOURS);      // pastTime is about 2 hours ago
     * assert futureTime > pastTime;                                         // returns true
     *
     * Dates.currentTimeMillisPlus(500, TimeUnit.MICROSECONDS);              // adds 0 ms (truncated toward zero)
     * Dates.currentTimeMillisPlus(1, (TimeUnit) null);                      // throws IllegalArgumentException
     * Dates.currentTimeMillisPlus(Long.MAX_VALUE, TimeUnit.DAYS);           // throws ArithmeticException
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.SECONDS, TimeUnit.MINUTES). Not {@code null}.
     * @return the current time in milliseconds with the specified amount applied.
     * @throws IllegalArgumentException if {@code unit} is {@code null}.
     * @throws ArithmeticException if conversion to milliseconds or addition to the current epoch value overflows.
     */
    @Beta
    public static long currentTimeMillisPlus(final long amount, final TimeUnit unit) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(unit, cs.unit);

        return Math.addExact(System.currentTimeMillis(), toMillisExact(amount, unit));
    }

    /**
     * Returns a new {@code java.sql.Time} instance representing the current time with the specified time amount added or subtracted.
     * This method creates a new Time object by applying the given amount in the specified time unit to the current system time.
     *
     * The amount is converted to a whole-millisecond fixed duration (sub-millisecond remainder is
     * truncated toward zero, and overflow throws), not
     * calendar arithmetic: across a daylight-saving transition, one {@code TimeUnit.DAYS} "day" is exactly
     * 24 hours of epoch time, so the result may not fall on the same wall-clock time (use the {@code add*}
     * methods for calendar-aware arithmetic).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Time future = Dates.currentTimePlus(30, TimeUnit.MINUTES);   // future is about 30 minutes from now
     * Time past = Dates.currentTimePlus(-1, TimeUnit.HOURS);       // past is about 1 hour ago
     * assert future.getTime() > past.getTime();                    // returns true (future is later)
     * Time same = Dates.currentTimePlus(0, TimeUnit.SECONDS);      // same is based on the current time
     * assert same != null;                                         // returns true (never null)
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS).
     * @return a new {@code java.sql.Time} instance representing the current time with the specified amount applied.
     * @throws IllegalArgumentException if {@code unit} is {@code null}.
     * @throws ArithmeticException if conversion to milliseconds or addition to the current epoch value overflows.
     */
    @Beta
    public static Time currentTimePlus(final long amount, final TimeUnit unit) throws IllegalArgumentException, ArithmeticException {
        return new Time(currentTimeMillisPlus(amount, unit));
    }

    /**
     * Returns a new {@code java.sql.Date} instance representing the current date with the specified time amount added or subtracted.
     * This method creates a new Date object by applying the given amount in the specified time unit to the current system time.
     *
     * The amount is converted to a whole-millisecond fixed duration (sub-millisecond remainder is
     * truncated toward zero, and overflow throws), not
     * calendar arithmetic: across a daylight-saving transition, one {@code TimeUnit.DAYS} "day" is exactly
     * 24 hours of epoch time, so the result may not fall on the same wall-clock time (use the {@code add*}
     * methods for calendar-aware arithmetic).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date tomorrow = Dates.currentDatePlus(1, TimeUnit.DAYS);    // tomorrow is about 1 day from now
     * Date lastWeek = Dates.currentDatePlus(-7, TimeUnit.DAYS);   // lastWeek is about 7 days ago
     * assert tomorrow.getTime() > lastWeek.getTime();             // returns true (tomorrow is later)
     * Date now = Dates.currentDatePlus(0, TimeUnit.DAYS);         // now is based on the current date
     * assert now != null;                                         // returns true (never null)
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.DAYS, TimeUnit.HOURS).
     * @return a new {@code java.sql.Date} instance representing the current date with the specified amount applied.
     * @throws IllegalArgumentException if {@code unit} is {@code null}.
     * @throws ArithmeticException if conversion to milliseconds or addition to the current epoch value overflows.
     */
    @Beta
    public static Date currentDatePlus(final long amount, final TimeUnit unit) throws IllegalArgumentException, ArithmeticException {
        return new Date(currentTimeMillisPlus(amount, unit));
    }

    /**
     * Returns a new {@code java.sql.Timestamp} instance representing the current timestamp with the specified time amount added or subtracted.
     * This method creates a new Timestamp object by applying the given amount in the specified time unit to the current system time.
     *
     * The amount is converted to a whole-millisecond fixed duration (sub-millisecond remainder is
     * truncated toward zero, and overflow throws), not
     * calendar arithmetic: across a daylight-saving transition, one {@code TimeUnit.DAYS} "day" is exactly
     * 24 hours of epoch time, so the result may not fall on the same wall-clock time (use the {@code add*}
     * methods for calendar-aware arithmetic).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Timestamp future = Dates.currentTimestampPlus(5, TimeUnit.MINUTES);   // future is about 5 minutes from now
     * Timestamp past = Dates.currentTimestampPlus(-3, TimeUnit.HOURS);      // past is about 3 hours ago
     * assert future.getTime() > past.getTime();                             // returns true (future is later)
     * Timestamp now = Dates.currentTimestampPlus(0, TimeUnit.SECONDS);      // now is based on the current time
     * assert now != null;                                                   // returns true (never null)
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS).
     * @return a new {@code java.sql.Timestamp} instance representing the current timestamp with the specified amount applied.
     * @throws IllegalArgumentException if {@code unit} is {@code null}.
     * @throws ArithmeticException if conversion to milliseconds or addition to the current epoch value overflows.
     */
    @Beta
    public static Timestamp currentTimestampPlus(final long amount, final TimeUnit unit) throws IllegalArgumentException, ArithmeticException {
        return new Timestamp(currentTimeMillisPlus(amount, unit));
    }

    /**
     * Returns a new {@code java.util.Date} instance representing the current date/time with the specified time amount added or subtracted.
     * This method creates a new Date object by applying the given amount in the specified time unit to the current system time.
     *
     * The amount is converted to a whole-millisecond fixed duration (sub-millisecond remainder is
     * truncated toward zero, and overflow throws), not
     * calendar arithmetic: across a daylight-saving transition, one {@code TimeUnit.DAYS} "day" is exactly
     * 24 hours of epoch time, so the result may not fall on the same wall-clock time (use the {@code add*}
     * methods for calendar-aware arithmetic).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date future = Dates.currentJUDatePlus(2, TimeUnit.DAYS);   // future is about 2 days from now
     * java.util.Date past = Dates.currentJUDatePlus(-7, TimeUnit.DAYS);    // past is about 7 days ago
     * assert future.getTime() > past.getTime();                            // returns true (future is later)
     * java.util.Date now = Dates.currentJUDatePlus(0, TimeUnit.DAYS);      // now is based on the current time
     * assert now != null;                                                  // returns true (never null)
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS).
     * @return a new {@code java.util.Date} instance representing the current date/time with the specified amount applied.
     * @throws IllegalArgumentException if {@code unit} is {@code null}.
     * @throws ArithmeticException if conversion to milliseconds or addition to the current epoch value overflows.
     */
    @Beta
    public static java.util.Date currentJUDatePlus(final long amount, final TimeUnit unit) throws IllegalArgumentException, ArithmeticException {
        return new java.util.Date(currentTimeMillisPlus(amount, unit));
    }

    /**
     * Returns a new {@code java.util.Calendar} instance representing the current date/time with the specified time amount added or subtracted.
     * This method creates a new Calendar object by applying the given amount in the specified time unit to the current system time.
     *
     * The amount is converted to a whole-millisecond fixed duration (sub-millisecond remainder is
     * truncated toward zero, and overflow throws), not
     * calendar arithmetic: across a daylight-saving transition, one {@code TimeUnit.DAYS} "day" is exactly
     * 24 hours of epoch time, so the result may not fall on the same wall-clock time (use the {@code add*}
     * methods for calendar-aware arithmetic).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar future = Dates.currentCalendarPlus(3, TimeUnit.HOURS);   // future is about 3 hours from now
     * Calendar past = Dates.currentCalendarPlus(-10, TimeUnit.DAYS);    // past is about 10 days ago
     * assert future.getTimeInMillis() > past.getTimeInMillis();         // returns true (future is later)
     * Calendar now = Dates.currentCalendarPlus(0, TimeUnit.HOURS);      // now is based on the current time
     * assert now != null;                                               // returns true (never null)
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS).
     * @return a new {@code java.util.Calendar} instance representing the current date/time with the specified amount applied.
     * @throws IllegalArgumentException if {@code unit} is {@code null}.
     * @throws ArithmeticException if conversion to milliseconds or addition to the current epoch value overflows.
     */
    @Beta
    public static Calendar currentCalendarPlus(final long amount, final TimeUnit unit) throws IllegalArgumentException, ArithmeticException {
        final Calendar ret = Calendar.getInstance();
        ret.setTimeInMillis(currentTimeMillisPlus(amount, unit));
        return ret;
    }

    /**
     * Returns a new {@code GregorianCalendar} instance representing the current time with the specified
     * time amount added or subtracted. This is the {@code GregorianCalendar} counterpart of
     * {@link #currentCalendarPlus(long, TimeUnit)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GregorianCalendar nextHour = Dates.currentGregorianCalendarPlus(1, TimeUnit.HOURS);   // about 1 hour from now
     * GregorianCalendar lastWeek = Dates.currentGregorianCalendarPlus(-7, TimeUnit.DAYS);   // about 7 days ago
     * assert nextHour.getTimeInMillis() > lastWeek.getTimeInMillis();                       // returns true
     *
     * Dates.currentGregorianCalendarPlus(0, TimeUnit.SECONDS);                              // based on the current time
     * Dates.currentGregorianCalendarPlus(1, (TimeUnit) null);                               // throws IllegalArgumentException
     * Dates.currentGregorianCalendarPlus(Long.MAX_VALUE, TimeUnit.DAYS);                    // throws ArithmeticException
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter. Not {@code null}.
     * @return a new {@code GregorianCalendar} instance with the specified amount applied to the current time.
     * @throws IllegalArgumentException if {@code unit} is {@code null}.
     * @throws ArithmeticException if conversion to milliseconds or the resulting epoch-millisecond value overflows a {@code long}.
     * @see #currentCalendarPlus(long, TimeUnit)
     */
    @Beta
    public static GregorianCalendar currentGregorianCalendarPlus(final long amount, final TimeUnit unit) throws IllegalArgumentException, ArithmeticException {
        final GregorianCalendar ret = new GregorianCalendar();
        ret.setTimeInMillis(currentTimeMillisPlus(amount, unit));
        return ret;
    }

    /**
     * Returns a new {@code XMLGregorianCalendar} instance representing the current time with the specified
     * time amount added or subtracted. This is the {@code XMLGregorianCalendar} counterpart of
     * {@link #currentCalendarPlus(long, TimeUnit)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar nextHour = Dates.currentXMLGregorianCalendarPlus(1, TimeUnit.HOURS);   // about 1 hour from now
     * XMLGregorianCalendar lastWeek = Dates.currentXMLGregorianCalendarPlus(-7, TimeUnit.DAYS);   // about 7 days ago
     * assert nextHour.toGregorianCalendar().getTimeInMillis()
     *         > lastWeek.toGregorianCalendar().getTimeInMillis();                                 // returns true
     *
     * Dates.currentXMLGregorianCalendarPlus(0, TimeUnit.SECONDS);                                 // based on the current time
     * Dates.currentXMLGregorianCalendarPlus(1, (TimeUnit) null);                                  // throws IllegalArgumentException
     * Dates.currentXMLGregorianCalendarPlus(Long.MAX_VALUE, TimeUnit.DAYS);                       // throws ArithmeticException
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter. Not {@code null}.
     * @return a new {@code XMLGregorianCalendar} instance with the specified amount applied to the current time.
     * @throws IllegalArgumentException if {@code unit} is {@code null}, or if the default zone's offset
     *         at the resulting instant is not a whole number of minutes in the range -14:00 through
     *         +14:00, which XML Schema cannot represent.
     * @throws ArithmeticException if conversion to milliseconds or the resulting epoch-millisecond value overflows a {@code long}.
     * @throws UnsupportedOperationException if the {@code DatatypeFactory} is not available.
     * @see #currentCalendarPlus(long, TimeUnit)
     * @see #createXMLGregorianCalendar(long)
     */
    @Beta
    public static XMLGregorianCalendar currentXMLGregorianCalendarPlus(final long amount, final TimeUnit unit)
            throws IllegalArgumentException, ArithmeticException, UnsupportedOperationException {
        return createXMLGregorianCalendar(currentTimeMillisPlus(amount, unit));
    }

    /**
     * Creates a new instance of {@code java.util.Date} based on the provided calendar's time value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(0L);       // cal is at epoch millis
     * java.util.Date d = Dates.createJUDate(cal);    // d is at the same instant as cal
     * assert d.getTime() == cal.getTimeInMillis();   // returns true
     * assert d.getTime() == 0L;                      // returns true
     *
     * Dates.createJUDate((Calendar) null);          // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the calendar providing the time value, not {@code null}.
     * @return a new {@code java.util.Date} instance representing the same point in time.
     * @throws IllegalArgumentException if calendar is {@code null}.
     * @see #createJUDate(java.util.Date)
     * @see #createJUDate(long)
     */
    public static java.util.Date createJUDate(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createJUDate(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of {@code java.util.Date} based on the provided date's time value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date original = new java.util.Date(1000L);
     * java.util.Date copy = Dates.createJUDate(original);   // copy is a new instance with the same time
     * assert copy.getTime() == 1000L;                       // returns true
     * assert copy != original;                              // returns true (distinct object)
     *
     * Dates.createJUDate((java.util.Date) null);            // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date providing the time value, not {@code null}.
     * @return a new {@code java.util.Date} instance representing the same point in time.
     * @throws IllegalArgumentException if date is {@code null}.
     * @see #createJUDate(Calendar)
     * @see #createJUDate(long)
     */
    public static java.util.Date createJUDate(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createJUDate(date.getTime());
    }

    /**
     * Creates a new instance of {@code java.util.Date} based on the provided time in milliseconds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date d = Dates.createJUDate(0L);   // d is at epoch millis
     * assert d.getTime() == 0L;                    // returns true
     *
     * java.util.Date d2 = Dates.createJUDate(1736937045000L);
     * assert d2.getTime() == 1736937045000L;             // returns true (echoes the input millis)
     * assert Dates.createJUDate(-1L).getTime() == -1L;   // returns true (negative millis = before the epoch)
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code java.util.Date} instance representing the specified point in time.
     * @see #createJUDate(Calendar)
     * @see #createJUDate(java.util.Date)
     * @see #createDate(long)
     * @see #createTime(long)
     * @see #createTimestamp(long)
     */
    public static java.util.Date createJUDate(final long timeInMillis) {
        return new java.util.Date(timeInMillis);
    }

    /**
     * Creates a new instance of {@code java.sql.Date} based on the provided calendar's time value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(0L);             // cal is at epoch millis
     * java.sql.Date sqlDate = Dates.createDate(cal);       // sqlDate is at the same instant as cal
     * assert sqlDate.getTime() == cal.getTimeInMillis();   // returns true
     * assert sqlDate.getTime() == 0L;                      // returns true
     *
     * Dates.createDate((Calendar) null);               // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the calendar providing the time value, not {@code null}.
     * @return a new {@code java.sql.Date} instance representing the same point in time.
     * @throws IllegalArgumentException if calendar is {@code null}.
     * @see #createDate(java.util.Date)
     * @see #createDate(long)
     * @see #createJUDate(Calendar)
     */
    public static Date createDate(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createDate(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of {@code java.sql.Date} based on the provided date's time value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date utilDate = new java.util.Date(1000L);
     * java.sql.Date sqlDate = Dates.createDate(utilDate);   // converts to java.sql.Date, same instant
     * assert sqlDate.getTime() == 1000L;                    // returns true
     *
     * Dates.createDate((java.util.Date) null);              // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date providing the time value, not {@code null}.
     * @return a new {@code java.sql.Date} instance representing the same point in time.
     * @throws IllegalArgumentException if date is {@code null}.
     * @see #createDate(Calendar)
     * @see #createDate(long)
     * @see #createJUDate(java.util.Date)
     */
    public static Date createDate(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createDate(date.getTime());
    }

    /**
     * Creates a new instance of {@code java.sql.Date} based on the provided time in milliseconds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.sql.Date d = Dates.createDate(0L);   // d is at epoch millis
     * assert d.getTime() == 0L;                 // returns true
     *
     * java.sql.Date d2 = Dates.createDate(1736937045000L);
     * assert d2.getTime() == 1736937045000L;           // returns true (echoes the input millis, no truncation)
     * assert Dates.createDate(-1L).getTime() == -1L;   // returns true (negative millis = before the epoch)
     * }</pre>
     *
     * <p>Note: the given milliseconds are used as-is. Formatted-string parsing through
     * {@link #parseToDate(String, String)} likewise retains the resolved instant without truncating it
     * to midnight. Pure numeric strings are rejected by {@code parseTo*} as ambiguous, so the
     * serialization round-trip for numeric text is {@link #parseEpochMillis(String)} plus
     * {@code createDate(long)}.</p>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code java.sql.Date} instance representing the specified point in time.
     * @see #createDate(Calendar)
     * @see #createDate(java.util.Date)
     * @see #createJUDate(long)
     * @see #createTime(long)
     * @see #createTimestamp(long)
     * @see #parseToDate(String)
     */
    public static Date createDate(final long timeInMillis) {
        return new Date(timeInMillis);
    }

    /**
     * Creates a new instance of {@code java.sql.Time} based on the provided calendar's time value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(0L);          // cal is at epoch millis
     * Time time = Dates.createTime(cal);                // time is at the same instant as cal
     * assert time.getTime() == cal.getTimeInMillis();   // returns true
     * assert time.getTime() == 0L;                      // returns true
     *
     * Dates.createTime((Calendar) null);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the calendar providing the time value, not {@code null}.
     * @return a new {@code java.sql.Time} instance representing the same point in time.
     * @throws IllegalArgumentException if calendar is {@code null}.
     * @see #createTime(java.util.Date)
     * @see #createTime(long)
     * @see #createDate(Calendar)
     */
    public static Time createTime(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createTime(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of {@code java.sql.Time} based on the provided date's time value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date utilDate = new java.util.Date(1000L);
     * Time time = Dates.createTime(utilDate);    // converts to java.sql.Time, same instant
     * assert time.getTime() == 1000L;            // returns true
     *
     * Dates.createTime((java.util.Date) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date providing the time value, not {@code null}.
     * @return a new {@code java.sql.Time} instance representing the same point in time.
     * @throws IllegalArgumentException if date is {@code null}.
     * @see #createTime(Calendar)
     * @see #createTime(long)
     * @see #createDate(java.util.Date)
     */
    public static Time createTime(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createTime(date.getTime());
    }

    /**
     * Creates a new instance of {@code java.sql.Time} based on the provided time in milliseconds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Time t = Dates.createTime(0L);              // t is at epoch millis
     * assert t.getTime() == 0L;                   // returns true
     *
     * Time t2 = Dates.createTime(52245000L);           // t2 is 52,245,000 ms after epoch
     * assert t2.getTime() == 52245000L;                // returns true (echoes the input millis)
     * assert Dates.createTime(-1L).getTime() == -1L;   // returns true (negative millis = before the epoch)
     * }</pre>
     *
     * <p>Note: the given milliseconds are used as-is. Formatted-string parsing through
     * {@link #parseToTime(String, String)} likewise retains the resolved instant without rebasing its
     * date component to 1970-01-01. Pure numeric strings are rejected by {@code parseTo*} as ambiguous,
     * so the serialization round-trip for numeric text is {@link #parseEpochMillis(String)} plus
     * {@code createTime(long)}.</p>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code java.sql.Time} instance representing the specified point in time.
     * @see #createTime(Calendar)
     * @see #createTime(java.util.Date)
     * @see #createDate(long)
     * @see #createTimestamp(long)
     * @see #parseToTime(String)
     */
    public static Time createTime(final long timeInMillis) {
        return new Time(timeInMillis);
    }

    /**
     * Creates a new instance of {@code java.sql.Timestamp} based on the provided calendar's time value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(0L);        // cal is at epoch millis
     * Timestamp ts = Dates.createTimestamp(cal);      // ts is at the same instant as cal
     * assert ts.getTime() == cal.getTimeInMillis();   // returns true
     * assert ts.getTime() == 0L;                      // returns true
     *
     * Dates.createTimestamp((Calendar) null);      // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the calendar providing the time value, not {@code null}.
     * @return a new {@code java.sql.Timestamp} instance representing the same point in time.
     * @throws IllegalArgumentException if calendar is {@code null}.
     * @see #createTimestamp(java.util.Date)
     * @see #createTimestamp(long)
     * @see #createTime(Calendar)
     */
    public static Timestamp createTimestamp(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createTimestamp(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of {@code java.sql.Timestamp} based on the provided date's time value.
     * If {@code date} is itself a {@link Timestamp}, its complete nanosecond-precision instant is
     * preserved; other {@code Date} implementations supply millisecond precision only.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date utilDate = new java.util.Date(1000L);
     * Timestamp ts = Dates.createTimestamp(utilDate);   // converts to java.sql.Timestamp, same instant
     * assert ts.getTime() == 1000L;                     // returns true
     *
     * Dates.createTimestamp((java.util.Date) null);     // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date providing the time value, not {@code null}.
     * @return a new {@code java.sql.Timestamp} instance representing the same point in time, including
     *         any sub-millisecond fraction carried by a {@code Timestamp} input.
     * @throws IllegalArgumentException if date is {@code null}.
     * @see #createTimestamp(Calendar)
     * @see #createTimestamp(long)
     * @see #createTime(java.util.Date)
     */
    public static Timestamp createTimestamp(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return date instanceof Timestamp ? Timestamp.from(((Timestamp) date).toInstant()) : createTimestamp(date.getTime());
    }

    /**
     * Creates a new instance of {@code java.sql.Timestamp} based on the provided time in milliseconds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Timestamp ts = Dates.createTimestamp(0L);   // ts is at epoch millis
     * assert ts.getTime() == 0L;                  // returns true
     *
     * Timestamp ts2 = Dates.createTimestamp(1736937045123L);
     * assert ts2.getTime() == 1736937045123L;               // returns true (millisecond precision preserved)
     * assert Dates.createTimestamp(-1L).getTime() == -1L;   // returns true (negative millis = before the epoch)
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code java.sql.Timestamp} instance representing the specified point in time.
     * @see #createTimestamp(Calendar)
     * @see #createTimestamp(java.util.Date)
     * @see #createTime(long)
     */
    public static Timestamp createTimestamp(final long timeInMillis) {
        return new Timestamp(timeInMillis);
    }

    /**
     * Creates a new instance of {@code java.util.Calendar} based on the provided calendar's time value.
     * The returned calendar preserves the provided calendar's time zone (a cloned copy); other settings
     * (locale-derived week rules, leniency) follow the default locale, not the source calendar.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar source = Dates.createCalendar(0L);                               // source is at epoch millis
     * Calendar copy = Dates.createCalendar(source);                             // copy is at the same instant and zone as source
     * assert copy.getTimeInMillis() == source.getTimeInMillis();                // returns true
     * assert copy.getTimeInMillis() == 0L;                                      // returns true
     * assert copy.getTimeZone().getID().equals(source.getTimeZone().getID());   // returns true (zone preserved)
     *
     * Dates.createCalendar((Calendar) null);                  // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the calendar providing the time value, not {@code null}.
     * @return a new {@code java.util.Calendar} instance representing the same point in time in the same time zone.
     * @throws IllegalArgumentException if calendar is {@code null}.
     * @see #createCalendar(java.util.Date)
     * @see #createCalendar(long)
     * @see #createCalendar(long, TimeZone)
     */
    public static Calendar createCalendar(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        // Preserve the source zone as well as the instant (createCalendar(long, TimeZone) clones the zone).
        return createCalendar(calendar.getTimeInMillis(), calendar.getTimeZone());
    }

    /**
     * Creates a new instance of {@code java.util.Calendar} based on the provided date's time value.
     * The returned calendar instance uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = new java.util.Date(1000L);
     * Calendar cal = Dates.createCalendar(date);     // cal is at the same instant as date
     * assert cal.getTimeInMillis() == 1000L;         // returns true
     *
     * Dates.createCalendar((java.util.Date) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date providing the time value, not {@code null}.
     * @return a new {@code java.util.Calendar} instance representing the same point in time.
     * @throws IllegalArgumentException if date is {@code null}.
     * @see #createCalendar(Calendar)
     * @see #createCalendar(long)
     * @see #createCalendar(long, TimeZone)
     */
    public static Calendar createCalendar(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createCalendar(date.getTime());
    }

    /**
     * Creates a new instance of {@code java.util.Calendar} based on the provided time in milliseconds.
     * The returned calendar instance uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(0L);   // cal is at epoch millis in the default time zone
     * assert cal.getTimeInMillis() == 0L;        // returns true
     *
     * Calendar cal2 = Dates.createCalendar(1736937045000L);
     * assert cal2.getTimeInMillis() == 1736937045000L;                 // returns true (echoes the input millis)
     * assert Dates.createCalendar(-1L).getTimeInMillis() == -1L;       // returns true (negative millis = before the epoch)
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code java.util.Calendar} instance representing the specified point in time.
     * @see #createCalendar(Calendar)
     * @see #createCalendar(java.util.Date)
     * @see #createCalendar(long, TimeZone)
     * @see #createGregorianCalendar(long)
     * @see #createXMLGregorianCalendar(long)
     */
    public static Calendar createCalendar(final long timeInMillis) {
        final Calendar c = Calendar.getInstance();

        c.setTimeInMillis(timeInMillis);

        return c;
    }

    /**
     * Creates a new instance of {@code java.util.Calendar} based on the provided time in milliseconds and the specified time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(0L, utc);    // cal is at epoch millis in UTC
     * assert cal.getTimeInMillis() == 0L;              // returns true
     * assert cal.get(Calendar.YEAR) == 1970;           // returns true (1970-01-01 in UTC)
     * assert cal.get(Calendar.HOUR_OF_DAY) == 0;       // returns true
     *
     * Calendar def = Dates.createCalendar(0L, null);   // uses the default time zone
     * assert def.getTimeInMillis() == 0L;              // returns true
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @param tz the time zone for the calendar; if {@code null}, the default time zone is used.
     * @return a new {@code java.util.Calendar} instance with the specified time and time zone. Its own
     *         {@code get} reads follow the JDK zone table, which differs from the {@code java.time} history
     *         before 1900 and from 2100 on (see the class's <i>Zone Rules</i>); {@code format} and the field
     *         operations read it through the aligned view.
     * @see #createCalendar(long)
     * @see #createCalendar(Calendar)
     * @see #createCalendar(java.util.Date)
     */
    public static Calendar createCalendar(final long timeInMillis, final TimeZone tz) {
        final Calendar c = tz == null ? Calendar.getInstance() : Calendar.getInstance((TimeZone) tz.clone());

        c.setTimeInMillis(timeInMillis);

        return c;
    }

    /**
     * Creates a new instance of {@code java.util.GregorianCalendar} based on the provided calendar's time value.
     * The returned calendar preserves the provided calendar's time zone (a cloned copy); other settings
     * (locale-derived week rules, leniency) follow the default locale, not the source calendar.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar source = Dates.createCalendar(0L);                                  // source is at epoch millis
     * GregorianCalendar gregCal = Dates.createGregorianCalendar(source);           // gregCal is at the same instant as source
     * assert gregCal.getTimeInMillis() == source.getTimeInMillis();                // returns true
     * assert gregCal.getTimeInMillis() == 0L;                                      // returns true
     * assert gregCal.getTimeZone().getID().equals(source.getTimeZone().getID());   // returns true (zone preserved)
     *
     * Dates.createGregorianCalendar((Calendar) null);                      // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the calendar providing the time value, not {@code null}.
     * @return a new {@code java.util.GregorianCalendar} instance representing the same point in time in the same time zone.
     * @throws IllegalArgumentException if calendar is {@code null}.
     * @see #createGregorianCalendar(java.util.Date)
     * @see #createGregorianCalendar(long)
     * @see #createGregorianCalendar(long, TimeZone)
     */
    public static GregorianCalendar createGregorianCalendar(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createGregorianCalendar(calendar.getTimeInMillis(), calendar.getTimeZone());
    }

    /**
     * Creates a new instance of {@code java.util.GregorianCalendar} based on the provided date's time value.
     * The returned calendar instance uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = new java.util.Date(1000L);
     * GregorianCalendar gregCal = Dates.createGregorianCalendar(date);   // gregCal is at the same instant as date
     * assert gregCal.getTimeInMillis() == 1000L;                         // returns true
     *
     * Dates.createGregorianCalendar((java.util.Date) null);              // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date providing the time value, not {@code null}.
     * @return a new {@code java.util.GregorianCalendar} instance representing the same point in time.
     * @throws IllegalArgumentException if date is {@code null}.
     * @see #createGregorianCalendar(Calendar)
     * @see #createGregorianCalendar(long)
     * @see #createGregorianCalendar(long, TimeZone)
     */
    public static GregorianCalendar createGregorianCalendar(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createGregorianCalendar(date.getTime());
    }

    /**
     * Creates a new instance of {@code java.util.GregorianCalendar} based on the provided time in milliseconds.
     * The returned calendar instance uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GregorianCalendar gregCal = Dates.createGregorianCalendar(0L);   // gregCal is at epoch millis
     * assert gregCal.getTimeInMillis() == 0L;                          // returns true
     *
     * GregorianCalendar g2 = Dates.createGregorianCalendar(1736937045000L);
     * assert g2.getTimeInMillis() == 1736937045000L;                          // returns true (echoes the input millis)
     * assert Dates.createGregorianCalendar(-1L).getTimeInMillis() == -1L;     // returns true (negative millis = before the epoch)
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code java.util.GregorianCalendar} instance representing the specified point in time.
     * @see #createGregorianCalendar(Calendar)
     * @see #createGregorianCalendar(java.util.Date)
     * @see #createGregorianCalendar(long, TimeZone)
     * @see #createCalendar(long)
     * @see #createXMLGregorianCalendar(long)
     */
    public static GregorianCalendar createGregorianCalendar(final long timeInMillis) {
        final GregorianCalendar c = new GregorianCalendar();

        c.setTimeInMillis(timeInMillis);

        return c;
    }

    /**
     * Creates a new instance of {@code java.util.GregorianCalendar} based on the provided time in milliseconds and the specified time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * GregorianCalendar gregCal = Dates.createGregorianCalendar(0L, utc);   // gregCal is at epoch millis in UTC
     * assert gregCal.getTimeInMillis() == 0L;                               // returns true
     * assert gregCal.get(Calendar.YEAR) == 1970;                            // returns true (1970-01-01 in UTC)
     *
     * GregorianCalendar def = Dates.createGregorianCalendar(0L, null);      // uses the default time zone
     * assert def.getTimeInMillis() == 0L;                                   // returns true
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @param tz the time zone for the calendar; if {@code null}, the default time zone is used.
     * @return a new {@code java.util.GregorianCalendar} instance with the specified time and time zone; its own
     *         {@code get} reads follow the JDK zone table before 1900 and from 2100 on (see
     *         {@link #createCalendar(long, TimeZone)}).
     * @see #createGregorianCalendar(long)
     * @see #createGregorianCalendar(Calendar)
     * @see #createGregorianCalendar(java.util.Date)
     */
    public static GregorianCalendar createGregorianCalendar(final long timeInMillis, final TimeZone tz) {
        final GregorianCalendar c = tz == null ? new GregorianCalendar() : new GregorianCalendar((TimeZone) tz.clone());

        c.setTimeInMillis(timeInMillis);

        return c;
    }

    /**
     * Creates a new instance of {@code XMLGregorianCalendar} based on the provided calendar's time value.
     * The returned calendar preserves the provided calendar's time zone as an XML numeric offset; the
     * instant is unchanged, and the civil fields are the proleptic Gregorian ones XML Schema defines,
     * whatever calendar system or cutover {@code calendar} itself uses. Other XML fields follow
     * {@link #createXMLGregorianCalendar(long, TimeZone)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar source = Dates.createCalendar(0L, TimeZone.getTimeZone("UTC"));
     * XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(source);
     * assert xmlCal.toGregorianCalendar().getTimeInMillis() == 0L;   // returns true (same instant)
     * assert xmlCal.getYear() == 1970;                               // returns true (1970-01-01 in UTC)
     *
     * Dates.createXMLGregorianCalendar((Calendar) null);        // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the calendar providing the time value, not {@code null}.
     * @return a new {@code XMLGregorianCalendar} instance representing the same point in time in the source calendar's time zone.
     * @throws IllegalArgumentException if calendar is {@code null}, or if the calendar's zone offset at
     *         that instant is not a whole number of minutes in the range -14:00 through +14:00, which
     *         XML Schema cannot represent.
     * @throws UnsupportedOperationException if the {@code DatatypeFactory} is not available.
     * @see #createXMLGregorianCalendar(java.util.Date)
     * @see #createXMLGregorianCalendar(long)
     * @see #createXMLGregorianCalendar(long, TimeZone)
     */
    public static XMLGregorianCalendar createXMLGregorianCalendar(final Calendar calendar) throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createXMLGregorianCalendar(calendar.getTimeInMillis(), calendar.getTimeZone());
    }

    /**
     * Creates a new instance of {@code XMLGregorianCalendar} based on the provided date's time value.
     * The returned calendar instance uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = new java.util.Date(0L);
     * XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(date);
     * assert xmlCal.toGregorianCalendar().getTimeInMillis() == 0L;    // returns true (same instant)
     *
     * Dates.createXMLGregorianCalendar((java.util.Date) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date providing the time value, not {@code null}.
     * @return a new {@code XMLGregorianCalendar} instance representing the same point in time.
     * @throws IllegalArgumentException if date is {@code null}, or if the default zone's offset at that
     *         instant is not a whole number of minutes in the range -14:00 through +14:00, which XML
     *         Schema cannot represent.
     * @throws UnsupportedOperationException if the {@code DatatypeFactory} is not available.
     * @see #createXMLGregorianCalendar(Calendar)
     * @see #createXMLGregorianCalendar(long)
     * @see #createXMLGregorianCalendar(long, TimeZone)
     */
    public static XMLGregorianCalendar createXMLGregorianCalendar(final java.util.Date date) throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(date, cs.date);

        return createXMLGregorianCalendar(date.getTime());
    }

    /**
     * Creates a new instance of {@code XMLGregorianCalendar} based on the provided time in milliseconds.
     * The returned calendar instance uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(0L);   // xmlCal is at epoch millis
     * assert xmlCal.toGregorianCalendar().getTimeInMillis() == 0L;          // returns true
     *
     * XMLGregorianCalendar x2 = Dates.createXMLGregorianCalendar(1736937045000L);
     * assert x2.toGregorianCalendar().getTimeInMillis() == 1736937045000L;   // returns true (echoes the input millis)
     * assert Dates.createXMLGregorianCalendar(-1L).toGregorianCalendar().getTimeInMillis() == -1L;
     *                                                                        // returns true (negative millis = before the epoch)
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code XMLGregorianCalendar} instance representing the specified point in time.
     * @throws IllegalArgumentException if the default time zone's offset at {@code timeInMillis} is not
     *         a whole number of minutes in the range -14:00 through +14:00, which XML Schema cannot
     *         represent; see {@link #createXMLGregorianCalendar(long, TimeZone)}.
     * @throws UnsupportedOperationException if the {@code DatatypeFactory} is not available.
     * @see #createXMLGregorianCalendar(Calendar)
     * @see #createXMLGregorianCalendar(java.util.Date)
     * @see #createXMLGregorianCalendar(long, TimeZone)
     * @see #createCalendar(long)
     * @see #createGregorianCalendar(long)
     */
    public static XMLGregorianCalendar createXMLGregorianCalendar(final long timeInMillis) throws IllegalArgumentException, UnsupportedOperationException {
        // Capture the default zone once and hand it to the zone-taking overload, so the representability
        // check and the calendar that is built observe the same zone even if another thread calls
        // TimeZone.setDefault in between.
        return createXMLGregorianCalendar(timeInMillis, TimeZone.getDefault());
    }

    /**
     * Creates a new instance of {@code XMLGregorianCalendar} based on the provided time in milliseconds and the specified time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(0L, utc);
     * assert xmlCal.getYear() == 1970;                                              // returns true (1970-01-01 in UTC)
     * assert xmlCal.toString().equals("1970-01-01T00:00:00.000Z");                  // returns true
     *
     * XMLGregorianCalendar def = Dates.createXMLGregorianCalendar(0L, null);   // uses the default time zone
     * assert def != null;                                                      // returns true
     * }</pre>
     *
     * <p><b>Civil fields are proleptic Gregorian.</b> XML Schema {@code dateTime} has no Julian/Gregorian
     * cutover, and an {@code XMLGregorianCalendar} converts itself back to an instant on that basis, so the
     * fields are written the way {@link #parseToXMLGregorianCalendar(String)} and {@link #format(java.util.Date)}
     * name the instant: {@code 1500-03-01T00:00:00Z} yields {@code 1500-03-01}, and
     * {@code toGregorianCalendar()} returns the instant given here. (Fields copied from a default-cutover
     * {@link GregorianCalendar} would be Julian before 1582-10-15 and read back ten days early.)</p>
     *
     * <p><b>Time zone limitation.</b> An {@code XMLGregorianCalendar} stores its zone as a whole
     * number of minutes in the range -14:00 through +14:00. A zone whose offset at {@code timeInMillis}
     * falls outside that field is rejected rather than represented approximately: rounding the field
     * while keeping the civil fields would move the instant, which is what this factory exists to
     * preserve. Real historical zones reach the sub-minute case &mdash; {@code Africa/Monrovia} was
     * -00:44:30 until 1972. The same rule already governs {@link #parseToXMLGregorianCalendar(String)}
     * and {@link #format(XMLGregorianCalendar, String, TimeZone)}.</p>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @param tz the time zone for the calendar; if {@code null}, the default time zone is used.
     * @return a new {@code XMLGregorianCalendar} instance with the specified time and time zone.
     * @throws IllegalArgumentException if the effective zone's offset at {@code timeInMillis} is not a
     *         whole number of minutes in the range -14:00 through +14:00.
     * @throws UnsupportedOperationException if the {@code DatatypeFactory} is not available.
     * @see #createXMLGregorianCalendar(long)
     * @see #createXMLGregorianCalendar(Calendar)
     * @see #createXMLGregorianCalendar(java.util.Date)
     */
    public static XMLGregorianCalendar createXMLGregorianCalendar(final long timeInMillis, final TimeZone tz)
            throws IllegalArgumentException, UnsupportedOperationException {
        if (dataTypeFactory == null) {
            throw new UnsupportedOperationException("DatatypeFactory is not available. XMLGregorianCalendar operations are not supported.");
        }

        // newXMLGregorianCalendar(GregorianCalendar) keeps the local civil fields and rounds the XML
        // timezone field to whole minutes, so a sub-minute offset silently produced a DIFFERENT instant
        // (Africa/Monrovia at -00:44:30 came back 30 s early). Reject it here exactly as the parse and
        // format sides already do, instead of returning a value that fails this method's own contract.
        // Snapshot a caller-owned mutable zone before inspecting it, so the representability check and
        // the calendar that is built observe one coherent rule set (TimeZone.getDefault() already clones).
        // Before 1900 the legacy zone table and java.time's rules can disagree (Calcutta 1899: +05:30 vs the
        // +05:21:10 it really kept); format() renders such an instant through java.time's offset, and so must
        // this factory, or the XML fields drift 8m50s from the text they were parsed from. The stand-in the
        // renderer uses carries that offset, and a sub-minute one is rejected below as unrepresentable, exactly
        // as format(date, ISO_OFFSET_DATE_TIME_FORMAT, zone) rejects it.
        final TimeZone effectiveTimeZone = legacyRenderingZone((TimeZone) (tz == null ? TimeZone.getDefault() : tz).clone(), timeInMillis);
        checkXMLTimeZoneRepresentable(effectiveTimeZone, timeInMillis);

        // A proleptic calendar, not createGregorianCalendar: DatatypeFactory copies the civil fields of
        // the calendar it is given, and XML Schema dateTime is proleptic Gregorian - the value's own
        // toGregorianCalendar() reads the fields back on a pure-Gregorian calendar. Built from the JDK
        // default 1582 cutover, an instant before 1582-10-15 was written with Julian fields
        // (1500-03-01 came out as 1500-02-20) and therefore read back ten days early, while
        // parseToXMLGregorianCalendar wrote the proleptic fields for the very same instant.
        final GregorianCalendar source = newProlepticGregorianCalendar(effectiveTimeZone);
        source.setTimeInMillis(timeInMillis);

        return dataTypeFactory.newXMLGregorianCalendar(source);
    }

    /**
     * Parses a string representation of a date into a {@code java.util.Date} object.
     * Attempts to automatically detect the date format from common patterns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToJUDate("2025-01-15T10:30:45Z").getTime();   // returns 1736937045000 (ISO-8601 UTC)
     * Dates.parseToJUDate("1736937045000");                    // throws IllegalArgumentException; use Dates.parseEpochMillis("1736937045000") instead
     *
     * Dates.parseToJUDate((String) null);                      // returns null
     * Dates.parseToJUDate("");                                 // throws IllegalArgumentException
     * Dates.parseToJUDate("null");                             // returns null (the literal string "null")
     * }</pre>
     *
     * @param date the string representation of the date to be parsed.
     * @return the parsed {@code java.util.Date} instance, or {@code null} if the input is {@code null}
     *         or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the date string cannot be parsed.
     * @see #parseToJUDate(String, String)
     * @see #parseToJUDate(String, String, TimeZone)
     * @see #createJUDate(long)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static java.util.Date parseToJUDate(final String date) throws IllegalArgumentException {
        return parseToJUDate(date, null);
    }

    /**
     * Parses a string representation of a date into a {@code java.util.Date} object using the specified format.
     * If the format is {@code null} or empty, attempts to automatically detect the format.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // parse and format in the same (default) zone, so the round-trip is stable
     * Dates.format(Dates.parseToJUDate("22/10/2025", "dd/MM/yyyy"), "yyyy-MM-dd");
     *                                                  // returns "2025-10-22"
     * Dates.parseToJUDate("2025-01-15T10:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT).getTime();
     *                                                  // returns 1736937045000
     *
     * Dates.parseToJUDate((String) null, "dd/MM/yyyy");   // returns null
     * Dates.parseToJUDate("not-a-date", "yyyy-MM-dd");    // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the string representation of the date to be parsed.
     * @param format the date format pattern; if {@code null} or empty, common formats are attempted automatically.
     * @return the parsed {@code java.util.Date} instance, or {@code null} if the input is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the date string cannot be parsed using the specified format.
     * @see #parseToJUDate(String)
     * @see #parseToJUDate(String, String, TimeZone)
     * @see SimpleDateFormat
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static java.util.Date parseToJUDate(final String date, final String format) throws IllegalArgumentException {
        return parseToJUDate(date, format, null);
    }

    /**
     * Parses a string representation of a date into a {@code java.util.Date} object using the specified format and time zone.
     * If the format is {@code null} or empty, attempts to automatically detect the format.
     * If the time zone is {@code null}, uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.parseToJUDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss", utc).getTime();
     *                                                          // returns 1736937045000
     * Dates.parseToJUDate("2025-10-22", "yyyy-MM-dd", utc).getTime();
     *                                                          // returns 1761091200000 (midnight UTC)
     *
     * Dates.parseToJUDate((String) null, "yyyy-MM-dd", utc);   // returns null
     * Dates.parseToJUDate("bad", "yyyy-MM-dd", utc);           // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Note: UTC semantics attach only to the two predefined constants {@link #ISO_8601_DATE_TIME_FORMAT}
     * and {@link #ISO_8601_TIMESTAMP_FORMAT}, and to auto-detected ISO-8601 input whose value ends in the
     * {@code 'Z'} designator (when no explicit format is given). Such input is always interpreted in UTC.
     * Supplying one of the two constants together with a non-UTC {@code timeZone} throws an
     * {@code IllegalArgumentException}, while a {@code null} or UTC-equivalent zone (a fixed zero offset
     * under any ID) is accepted and resolved to UTC; the {@code format} counterpart applies the same rule,
     * see {@link #format(java.util.Date, String, TimeZone)}. Auto-detected text is different: the
     * {@code Z} it carries is data, so it wins over {@code timeZone} exactly as a numeric offset or a
     * bracketed region does, and {@code timeZone} is simply not consulted. A quoted {@code 'Z'} in any
     * other pattern is a plain literal with no zone meaning. {@link #HTTP_DATE_FORMAT} is independently
     * fixed to GMT and, when supplied explicitly, rejects a non-UTC-equivalent {@code timeZone}.</p>
     *
     * <p>Note: purely numeric input (with a {@code null} or empty format) is rejected as ambiguous; use {@link #parseEpochMillis(String)} for epoch-millisecond text.</p>
     *
     * @param date the string representation of the date to be parsed.
     * @param format the date format pattern; if {@code null} or empty, common formats are attempted automatically.
     * @param timeZone the time zone for parsing; if {@code null}, the default time zone is used.
     * @return the parsed {@code java.util.Date} instance, or {@code null} if the input is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the pattern lacks a complete date, the date string cannot be
     *         parsed using the specified format, or a fixed UTC/GMT format is combined with a
     *         non-UTC-equivalent time zone.
     * @see #parseToJUDate(String)
     * @see #parseToJUDate(String, String)
     * @see SimpleDateFormat
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static java.util.Date parseToJUDate(final String date, final String format, final TimeZone timeZone) throws IllegalArgumentException {
        return parseToJUDate(date, format, timeZone, Locale.US);
    }

    /**
     * Parses with an explicit locale for locale-sensitive {@link SimpleDateFormat} pattern fields such
     * as {@code MMM}, {@code EEE}, and {@code a}. Predefined machine-readable constants remain
     * US/ASCII; {@link #HTTP_DATE_FORMAT} therefore always uses the English names required by HTTP.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.parseToJUDate("15 Jan 2025", "dd MMM yyyy", utc, Locale.US).getTime();
     *                                                  // returns 1736899200000
     * Dates.parseToJUDate("15 janv. 2025", "dd MMM yyyy", utc, Locale.FRENCH).getTime();
     *                                                  // returns 1736899200000 (French month name)
     *
     * Dates.parseToJUDate((String) null, "dd MMM yyyy", utc, Locale.US);      // returns null
     * Dates.parseToJUDate("15 Jan 2025", "dd MMM yyyy", utc, Locale.FRENCH);  // throws IllegalArgumentException (English name, French locale)
     * Dates.parseToJUDate("15 Jan 2025", "dd MMM yyyy", utc, null);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the text to parse
     * @param format the pattern, or {@code null}/empty for automatic detection
     * @param timeZone the parsing zone, or {@code null} for the live machine default zone
     * @param locale the locale for locale-sensitive pattern fields; must not be {@code null}
     * @return the parsed date, or {@code null} for a {@code null} reference or the case-insensitive marker {@code "null"}
     * @throws IllegalArgumentException if {@code locale} is {@code null}, the pattern lacks a complete
     *         date, the text cannot be parsed, or the zone conflicts with a fixed-zone format
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static java.util.Date parseToJUDate(final String date, final String format, final TimeZone timeZone, final Locale locale)
            throws IllegalArgumentException {
        N.checkArgNotNull(locale, cs.locale);

        if (isNullParseInput(date)) {
            return null;
        }

        rejectEmptyDateTime(date);
        checkCompleteLegacyDateFormat(date, format, "parseToJUDate");

        return createJUDate(parse(date, format, timeZone, locale));
    }

    /**
     * Parses text into a {@code java.sql.Date}. The text is resolved to an instant and its epoch
     * milliseconds are retained unchanged, matching {@link #parseToJUDate(String)} and
     * {@link #parseToTimestamp(String)}. No truncation to midnight is performed. An offset or zone
     * designator written in the text takes precedence; zone-less text uses the live default zone.
     *
     * <p>To create a conventional JDBC date from the textual civil date fields, use
     * {@link #parseToLocalDate(String)} with {@link java.sql.Date#valueOf(LocalDate)} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToDate("2025-01-15");                    // resolves start of day in the default zone
     * assert Dates.parseToDate("2025-01-15 10:30:45").getTime()
     *     == Dates.parseToJUDate("2025-01-15 10:30:45").getTime();    // returns true; time-of-day retained
     * java.sql.Date value = new java.sql.Date(1736937045123L);
     * assert value.equals(Dates.parseToDate(Dates.format(value)));    // returns true (round-trips through the default format)
     *
     * Dates.parseToDate((String) null);                   // returns null
     * Dates.parseToDate("null");                          // returns null (the formatTo null-token)
     * Dates.parseToDate("");                              // throws IllegalArgumentException
     * Dates.parseToDate("1736937045000");                 // throws IllegalArgumentException (ambiguous numeric; use parseEpochMillis)
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @return the parsed {@code java.sql.Date}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         contains only a partial date, or cannot be parsed.
     * @see #parseToDate(String, String)
     * @see #parseToLocalDate(String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Date parseToDate(final String text) throws IllegalArgumentException {
        return parseToDate(text, null);
    }

    /**
     * Parses text into a {@code java.sql.Date} using the specified format (or auto-detection when
     * {@code format} is {@code null}/empty). The text is resolved to an instant — zone-less text is
     * interpreted in the live default zone, while a zone or offset written in the text takes precedence —
     * and that instant's epoch milliseconds are retained unchanged.
     *
     * <p>To create a conventional JDBC date from the textual civil date exactly as written (never shifted
     * by an offset or zone designator), use {@link #parseToLocalDate(String, String)} with
     * {@link java.sql.Date#valueOf(LocalDate)} instead.</p>
     *
     * <p>Note: a complete date is required; time-only text (e.g. with pattern {@code HH:mm:ss}) and
     * other partial-date patterns are rejected rather than completed from the 1970-01-01 base. Use
     * {@link #parseToTime(String, String)} for time-only input.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToDate("22/10/2025", "dd/MM/yyyy");                 // resolves start of day in the default zone
     * assert Dates.parseToDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss").getTime()
     *     == Dates.parseToJUDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss").getTime();   // returns true
     *
     * Dates.parseToDate((String) null, "dd/MM/yyyy");                // returns null
     * Dates.parseToDate("", "dd/MM/yyyy");                           // throws IllegalArgumentException
     * Dates.parseToDate("bad", "dd/MM/yyyy");                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @param format a predefined format constant or a {@link java.text.SimpleDateFormat} pattern, or
     *        {@code null}/empty for auto-detection.
     * @return the parsed {@code java.sql.Date}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         or cannot be parsed.
     * @see #parseToDate(String, String, TimeZone)
     * @see #parseToLocalDate(String, String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Date parseToDate(final String text, final String format) throws IllegalArgumentException {
        return parseToDate(text, format, null);
    }

    /**
     * Parses text into a {@code java.sql.Date} using the specified format and time zone. The zone is the
     * fallback for interpreting zone-less text; a zone or offset written in the text always takes
     * precedence. The resolved instant's epoch milliseconds are retained unchanged. If
     * {@code timeZone} is {@code null}, the live default zone is used.
     *
     * <p>To create a conventional JDBC date from the textual civil date exactly as written, use
     * {@link #parseToLocalDate(String, String)} with {@link java.sql.Date#valueOf(LocalDate)} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.parseToDate("2025-01-15", "yyyy-MM-dd", utc).getTime();            // returns 1736899200000 (start of day UTC)
     * Dates.parseToDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss", utc).getTime();
     *                                                                    // returns 1736937045000 (time retained)
     *
     * Dates.parseToDate((String) null, "yyyy-MM-dd", utc);                     // returns null
     * Dates.parseToDate("bad", "yyyy-MM-dd", utc);                             // throws IllegalArgumentException
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @param format a predefined format constant or a {@link java.text.SimpleDateFormat} pattern, or
     *        {@code null}/empty for auto-detection.
     * @param timeZone the fallback zone for interpreting zone-less text;
     *        if {@code null}, the live default zone is used.
     * @return the parsed {@code java.sql.Date}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text, the
     *         pattern lacks a complete date, the value cannot be parsed, or the zone conflicts with a
     *         fixed-zone format.
     * @see #parseToDate(String, String)
     * @see #parseToLocalDate(String, String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Date parseToDate(final String text, final String format, final TimeZone timeZone) throws IllegalArgumentException {
        return parseToDate(text, format, timeZone, Locale.US);
    }

    /**
     * As {@link #parseToDate(String, String, TimeZone)} with an explicit locale for locale-sensitive
     * pattern fields (such as {@code MMM} or {@code EEE}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.parseToDate("15 Jan 2025", "dd MMM yyyy", utc, Locale.US).getTime();
     *                                                  // returns 1736899200000
     * Dates.parseToDate("15 janv. 2025", "dd MMM yyyy", utc, Locale.FRENCH).getTime();
     *                                                  // returns 1736899200000 (French month name)
     *
     * Dates.parseToDate((String) null, "dd MMM yyyy", utc, Locale.US);      // returns null
     * Dates.parseToDate("15 Jan 2025", "dd MMM yyyy", utc, Locale.FRENCH);  // throws IllegalArgumentException (English name, French locale)
     * Dates.parseToDate("15 Jan 2025", "dd MMM yyyy", utc, null);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @param format the pattern, or {@code null}/empty for auto-detection.
     * @param timeZone the fallback zone for interpreting zone-less text;
     *        if {@code null}, the live default zone is used.
     * @param locale the locale for locale-sensitive pattern fields; must not be {@code null}.
     * @return the parsed {@code java.sql.Date}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if {@code locale} is {@code null}, the text is empty, ambiguous
     *         numeric text, the pattern lacks a complete date, the value cannot be parsed, or the zone
     *         conflicts with a fixed-zone format.
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Date parseToDate(final String text, final String format, final TimeZone timeZone, final Locale locale) throws IllegalArgumentException {
        N.checkArgNotNull(locale, cs.locale);

        if (isNullParseInput(text)) {
            return null;
        }

        rejectEmptyDateTime(text);
        checkCompleteLegacyDateFormat(text, format, "parseToDate");

        return createDate(parse(text, format, timeZone, locale));
    }

    /**
     * Parses text into a {@code java.sql.Time}. The text is resolved to an instant and its epoch
     * milliseconds are retained unchanged, matching {@link #parseToJUDate(String)} and
     * {@link #parseToTimestamp(String)}. Dated input is not rebased to 1970-01-01. Time-only input is
     * anchored to 1970-01-01 in its written zone or offset, or in the live default zone when none is
     * written. Input containing only a partial date is rejected.
     *
     * <p>To create a conventional JDBC time from the textual civil time fields, use
     * {@link #parseToLocalTime(String)} with {@link java.sql.Time#valueOf(LocalTime)} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToTime("14:30:45");                          // anchored to 1970-01-01 in the default zone
     * Dates.parseToTime("2025-01-15T10:30:45Z").getTime();    // returns 1736937045000 (date retained)
     * java.sql.Time value = new java.sql.Time(1736937045123L);
     * assert value.equals(Dates.parseToTime(Dates.format(value)));   // returns true (round-trips through the default format)
     *
     * Dates.parseToTime((String) null);                 // returns null
     * Dates.parseToTime("");                            // throws IllegalArgumentException
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @return the parsed {@code java.sql.Time}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         contains only a partial date, or cannot be parsed.
     * @see #parseToTime(String, String)
     * @see #parseToLocalTime(String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Time parseToTime(final String text) throws IllegalArgumentException {
        return parseToTime(text, null);
    }

    /**
     * Parses text into a {@code java.sql.Time} using the specified format (or auto-detection when
     * {@code format} is {@code null}/empty). The text is resolved to an instant — zone-less text
     * is interpreted in the live default zone, while a zone or offset written in the text takes
     * precedence — and that instant's epoch milliseconds are retained unchanged. A complete date is
     * retained; a date-free pattern that identifies a clock hour is anchored to 1970-01-01 in the
     * authoritative zone. A partial date (for example, year plus time but no month/day), a zone/literal-only
     * pattern, or time fields that cannot identify a clock hour (for example {@code mm:ss}) are rejected.
     *
     * <p>To create a conventional JDBC time from the textual civil time exactly as written, use
     * {@link #parseToLocalTime(String, String)} with {@link java.sql.Time#valueOf(LocalTime)} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToTime("14:30:45", "HH:mm:ss");                          // returns 14:30:45 (on 1970-01-01 in the default zone)
     * Dates.format(Dates.parseToTime("14:30:45.123", "HH:mm:ss.SSS"), "HH:mm:ss.SSS");
     *                                                                     // returns "14:30:45.123" (the fraction is retained)
     *
     * Dates.parseToTime("14:30:45.123", "HH:mm:ss.SSS").toString();       // returns "14:30:45" (Time.toString() never shows millis)
     * assert Dates.parseToTime("14:30:45.123", "HH:mm:ss.SSS").getTime() % 1000 == 123;
     *                                                                     // returns true (the fraction is still in the epoch value)
     * Dates.parseToTime((String) null, "HH:mm:ss");                       // returns null
     * Dates.parseToTime("bad", "HH:mm:ss");                               // throws IllegalArgumentException
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @param format a predefined format constant or a {@link java.text.SimpleDateFormat} pattern, or
     *        {@code null}/empty for auto-detection.
     * @return the parsed {@code java.sql.Time}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         contains only a partial date, lacks a resolvable time when no date is present, or cannot be parsed.
     * @see #parseToTime(String, String, TimeZone)
     * @see #parseToLocalTime(String, String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Time parseToTime(final String text, final String format) throws IllegalArgumentException {
        return parseToTime(text, format, null);
    }

    /**
     * Parses text into a {@code java.sql.Time} using the specified format and time zone. The zone is the
     * fallback for interpreting zone-less text and anchoring time-only text; a zone or offset written in
     * the text always takes precedence. The resolved instant's epoch milliseconds are retained
     * unchanged. A pattern containing only a partial date, or containing neither a complete date nor
     * enough time fields to identify a clock hour, is rejected. If {@code timeZone} is {@code null},
     * the live default zone is used.
     *
     * <p>To create a conventional JDBC time from the textual civil time exactly as written, use
     * {@link #parseToLocalTime(String, String)} with {@link java.sql.Time#valueOf(LocalTime)} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.parseToTime("14:30:45", "HH:mm:ss", utc).getTime();                        // returns 52245000 (14:30:45 on 1970-01-01 in UTC)
     * Dates.format(Dates.parseToTime("14:30:45", "HH:mm:ss", utc), "HH:mm:ss", utc);   // returns "14:30:45"
     *
     * Dates.parseToTime((String) null, "HH:mm:ss", utc);          // returns null
     * Dates.parseToTime("bad", "HH:mm:ss", utc);                  // throws IllegalArgumentException
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @param format a predefined format constant or a {@link java.text.SimpleDateFormat} pattern, or
     *        {@code null}/empty for auto-detection.
     * @param timeZone the fallback zone for interpreting zone-less text and anchoring time-only text;
     *        if {@code null}, the live default zone is used.
     * @return the parsed {@code java.sql.Time}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         contains only a partial date, lacks a resolvable time when no date is present, cannot be
     *         parsed, or the zone conflicts with a fixed-zone format.
     * @see #parseToTime(String, String)
     * @see #parseToLocalTime(String, String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Time parseToTime(final String text, final String format, final TimeZone timeZone) throws IllegalArgumentException {
        return parseToTime(text, format, timeZone, Locale.US);
    }

    /**
     * As {@link #parseToTime(String, String, TimeZone)} with an explicit locale for locale-sensitive
     * pattern fields (such as {@code a} or {@code EEE}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.parseToTime("02:30:45 PM", "hh:mm:ss a", utc, Locale.US).getTime();
     *                                                  // returns 52245000 (14:30:45 on 1970-01-01 UTC)
     * Dates.parseToTime("14:30:45", "HH:mm:ss", utc, Locale.FRENCH).getTime();
     *                                                  // returns 52245000 (no locale-sensitive field in this pattern)
     *
     * Dates.parseToTime((String) null, "hh:mm:ss a", utc, Locale.US);   // returns null
     * Dates.parseToTime("bad", "hh:mm:ss a", utc, Locale.US);           // throws IllegalArgumentException
     * Dates.parseToTime("02:30:45 PM", "hh:mm:ss a", utc, null);        // throws IllegalArgumentException
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @param format the pattern, or {@code null}/empty for auto-detection.
     * @param timeZone the fallback zone for interpreting zone-less text and anchoring time-only text;
     *        if {@code null}, the live default zone is used.
     * @param locale the locale for locale-sensitive pattern fields; must not be {@code null}.
     * @return the parsed {@code java.sql.Time}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if {@code locale} is {@code null}, the text is empty, ambiguous
     *         numeric text, contains only a partial date, lacks a resolvable time when no date is
     *         present, cannot be parsed, or the zone conflicts with a fixed-zone format.
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Time parseToTime(final String text, final String format, final TimeZone timeZone, final Locale locale) throws IllegalArgumentException {
        N.checkArgNotNull(locale, cs.locale);

        if (isNullParseInput(text)) {
            return null;
        }

        rejectEmptyDateTime(text);
        checkCompleteOrTimeOnlyLegacyDateFormat(text, format, "parseToTime");

        return createTime(parse(text, format, timeZone, locale));
    }

    /**
     * Parses a string representation of a timestamp into a {@code java.sql.Timestamp} object.
     * Attempts to automatically detect the timestamp format from common patterns.
     *
     * <p>The JDBC timestamp escape format {@code "yyyy-mm-dd hh:mm:ss.fffffffff"} produced by
     * {@link Timestamp#toString()} is also supported. Its fractional second may contain 1 to 9 digits and is
     * interpreted as a fraction of a second (e.g. {@code ".5"} is half a second), preserving the full nanosecond
     * precision; such values are interpreted in the requested time zone (the live default zone when none is
     * given). A wall time in a daylight-saving gap or overlap is rejected because it does not identify one
     * unambiguous instant. Every auto-detected local or ISO timestamp form uses the same 1&ndash;9 digit
     * fraction-of-second semantics across targets
     * ({@code parseToJUDate}/{@code parseToCalendar}/{@code parseToLocalDateTime}/...), while an explicitly
     * supplied named {@code .SSS} constant requires exactly three fraction digits.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToTimestamp("2025-01-15T10:30:45.123Z").getTime();         // returns 1736937045123 (ISO-8601 UTC, with millis)
     * Dates.parseToTimestamp("2025-01-15 10:30:45.123456789").getNanos();   // returns 123456789 (Timestamp.toString() format, nanosecond precision)
     * Dates.parseToTimestamp("2025-01-15 10:30:45.5").getNanos();           // returns 500000000 (fractional second, not milliseconds)
     *
     * Dates.parseToTimestamp((String) null);                                // returns null
     * Dates.parseToTimestamp("null");                                       // returns null (the literal string "null")
     * Dates.parseToTimestamp("");                                           // throws IllegalArgumentException
     * Dates.parseToTimestamp("1736937045123");                              // throws IllegalArgumentException; use Dates.parseEpochMillis("1736937045123") instead
     * }</pre>
     *
     * @param date the string representation of the timestamp to be parsed.
     * @return the parsed {@code java.sql.Timestamp} instance, or {@code null} if the input is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the timestamp string cannot be parsed.
     * @see #parseToTimestamp(String, String)
     * @see #parseToTimestamp(String, String, TimeZone)
     * @see #parseToDate(String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Timestamp parseToTimestamp(final String date) throws IllegalArgumentException {
        return parseToTimestamp(date, null);
    }

    /**
     * Parses a string representation of a timestamp into a {@code java.sql.Timestamp} object using the specified format.
     * If the format is {@code null} or empty, attempts to automatically detect the format.
     * Auto-detected JDBC timestamp-shaped input (and input using the matching standard local format
     * constants) is parsed strictly and rejects daylight-saving gaps and overlaps; any other explicit
     * custom pattern remains authoritative.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.format(Dates.parseToTimestamp("2025-01-15 10:30:45.123", "yyyy-MM-dd HH:mm:ss.SSS"),
     *         "yyyy-MM-dd HH:mm:ss.SSS");                       // returns "2025-01-15 10:30:45.123" (default zone round-trip)
     *
     * Dates.parseToTimestamp((String) null, "yyyy-MM-dd HH:mm:ss.SSS");   // returns null
     * Dates.parseToTimestamp("bad", "yyyy-MM-dd HH:mm:ss.SSS");           // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the string representation of the timestamp to be parsed.
     * @param format the timestamp format pattern; if {@code null} or empty, common formats are attempted automatically.
     * @return the parsed {@code java.sql.Timestamp} instance, or {@code null} if the input is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the timestamp string cannot be parsed using the specified format.
     * @see #parseToTimestamp(String)
     * @see #parseToTimestamp(String, String, TimeZone)
     * @see #parseToTime(String, String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Timestamp parseToTimestamp(final String date, final String format) throws IllegalArgumentException {
        return parseToTimestamp(date, format, null);
    }

    /**
     * Parses a string representation of a timestamp into a {@code java.sql.Timestamp} object using the specified format and time zone.
     * If the format is {@code null} or empty, attempts to automatically detect the format.
     * If the time zone is {@code null}, the live machine default time zone is used for input that carries no
     * zone or offset of its own.
     * Auto-detected JDBC
     * timestamp-shaped input (and input using the matching standard local format constants) is parsed
     * strictly and rejects daylight-saving gaps and overlaps; any other explicit custom pattern remains
     * authoritative. A custom {@code TimeZone} whose daylight-saving rules no {@link ZoneId} can
     * represent is accepted here exactly as {@link #parseToJUDate(String, String, TimeZone)} accepts it,
     * with the nanosecond fraction preserved, but resolves through {@code Calendar} and therefore
     * silently selects one side of an overlap instead of rejecting it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.parseToTimestamp("2025-01-15 10:30:45.123", "yyyy-MM-dd HH:mm:ss.SSS", utc).getTime();
     *                                                          // returns 1736937045123
     *
     * Dates.parseToTimestamp((String) null, "yyyy-MM-dd HH:mm:ss.SSS", utc);   // returns null
     * Dates.parseToTimestamp("bad", "yyyy-MM-dd HH:mm:ss.SSS", utc);           // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Note: the two fixed-{@code 'Z'} UTC constants and {@link #HTTP_DATE_FORMAT}, when supplied
     * explicitly, reject a {@code timeZone} that is not UTC-equivalent (a fixed zero offset under any ID
     * qualifies). With auto-detection the {@code Z} or {@code GMT} the text carries is data, so it wins over
     * {@code timeZone} exactly as a numeric offset or a bracketed region does, and no conflict arises.
     * {@link #ISO_OFFSET_DATE_TIME_FORMAT} accepts either {@code Z} or a numeric offset; that offset is
     * authoritative and {@code timeZone} is only a fallback.</p>
     *
     * <p>Note: purely numeric input (with a {@code null} or empty format) is rejected as ambiguous; use {@link #parseEpochMillis(String)} for epoch-millisecond text.</p>
     *
     * @param date the string representation of the timestamp to be parsed.
     * @param format the timestamp format pattern; if {@code null} or empty, common formats are attempted automatically.
     * @param timeZone the time zone for parsing; if {@code null}, the live machine default time zone is used.
     * @return the parsed {@code java.sql.Timestamp} instance, or {@code null} if the input is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the pattern lacks a complete date, the timestamp string cannot
     *         be parsed using the specified format, or a fixed UTC/GMT format is combined with a
     *         non-UTC-equivalent time zone.
     * @see #parseToTimestamp(String)
     * @see #parseToTimestamp(String, String)
     * @see #parseToTime(String, String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Timestamp parseToTimestamp(final String date, final String format, final TimeZone timeZone) throws IllegalArgumentException {
        return parseToTimestamp(date, format, timeZone, Locale.US);
    }

    /**
     * Parses date/time text into a {@code Timestamp} with an explicit locale for locale-sensitive pattern
     * fields, exactly as {@link #parseToTimestamp(String, String, TimeZone)} does with {@code Locale.US}; the
     * locale matters only for a custom pattern with month, weekday or am/pm names.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.parseToTimestamp("2025-01-15 10:30:45.123", Dates.LOCAL_TIMESTAMP_FORMAT, utc, Locale.US).getTime();
     *                                                  // returns 1736937045123
     * Dates.parseToTimestamp("15 janv. 2025 10:30:45", "dd MMM yyyy HH:mm:ss", utc, Locale.FRENCH).getTime();
     *                                                  // returns 1736937045000 (French month name)
     *
     * Dates.parseToTimestamp((String) null, Dates.LOCAL_TIMESTAMP_FORMAT, utc, Locale.US);   // returns null
     * Dates.parseToTimestamp("bad", Dates.LOCAL_TIMESTAMP_FORMAT, utc, Locale.US);           // throws IllegalArgumentException
     * Dates.parseToTimestamp("2025-01-15 10:30:45.123", Dates.LOCAL_TIMESTAMP_FORMAT, utc, null);
     *                                                  // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the text to parse
     * @param format the pattern, or {@code null}/empty for automatic detection
     * @param timeZone the parsing zone, or {@code null} for the documented default-zone policy
     * @param locale the locale for locale-sensitive pattern fields; must not be {@code null}
     * @return the parsed timestamp, or {@code null} for a {@code null} reference or the case-insensitive marker {@code "null"}
     * @throws IllegalArgumentException if {@code locale} is {@code null}, the pattern lacks a complete
     *         date, the text cannot be parsed, or the zone conflicts with a fixed-zone format
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Timestamp parseToTimestamp(final String date, final String format, final TimeZone timeZone, final Locale locale)
            throws IllegalArgumentException {
        N.checkArgNotNull(locale, cs.locale);

        if (isNullParseInput(date)) {
            return null;
        }

        rejectEmptyDateTime(date);
        checkCompleteLegacyDateFormat(date, format, "parseToTimestamp");

        if (Strings.isEmpty(format)) {
            final Instant exactInstant = parseAutoTimestampToInstant(date, timeZone);

            if (exactInstant != null) {
                return Timestamp.from(exactInstant);
            }
        }

        // Resolve JDBC timestamp escape strings through one strict path. Timestamp.valueOf uses the
        // ambient default zone and chooses a DST-overlap offset differently from java.time, so selecting
        // between the two algorithms made an explicit-zone result depend on unrelated global state.
        final boolean jdbcPattern = Strings.isEmpty(format) || (LOCAL_DATE_TIME_FORMAT.equals(format) && date.length() == 19)
                || (LOCAL_TIMESTAMP_FORMAT.equals(format) && date.length() == 23);

        if (jdbcPattern && isJdbcTimestampString(date)) {
            final TimeZone effectiveZone = timeZone == null ? TimeZone.getDefault() : (TimeZone) timeZone.clone();

            try {
                return parseJdbcTimestamp(date, effectiveZone);
            } catch (final DateTimeException | IllegalArgumentException e) {
                // On the failure path only, so a successful parse pays nothing for it: the JDBC resolver
                // and checkGapAndOverlap reject exactly the same nonexistent and ambiguous wall times,
                // but only the latter names the offending local date-time - the diagnostic every sibling
                // parse target produces. Any other failure leaves it silent and falls through below.
                checkGapAndOverlap(date, checkDateFormat(date, format), effectiveZone);

                // An empty format reads as "the auto-detected format": naming the JDBC grammar here
                // told the caller they had passed a pattern they never wrote.
                throw parseFailure(date, format, effectiveZone, e);
            }
        }

        return createTimestamp(parse(date, format, timeZone, locale));
    }

    /**
     * Parses a JDBC timestamp escape string ({@code yyyy-mm-dd hh:mm:ss[.fffffffff]}) in an explicit
     * zone, preserving the nanosecond fraction. Invalid fields, nonexistent local times, and ambiguous
     * local times are rejected.
     */
    private static Timestamp parseJdbcTimestamp(final String str, final TimeZone timeZone) {
        final int year = parseInt(str, 0, 4);
        final int month = parseInt(str, 5, 7);
        final int day = parseInt(str, 8, 10);
        final int hour = parseInt(str, 11, 13);
        final int minute = parseInt(str, 14, 16);
        final int second = parseInt(str, 17, 19);

        if (year < 1) {
            throw new DateTimeException("JDBC timestamp year must be at least 0001: " + year);
        }

        int nanos = 0;

        if (str.length() > 19) {
            // Fractional second: 1-9 digits after the '.' at index 19, scaled to nanoseconds.
            int fraction = parseInt(str, 20, str.length());

            for (int digits = str.length() - 20; digits < 9; digits++) {
                fraction *= 10;
            }

            nanos = fraction;
        }

        final LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hour, minute, second);

        return Timestamp.from(Instant.ofEpochMilli(resolveLocalMillis(localDateTime, timeZone)).plusNanos(nanos));
    }

    /**
     * Resolves a whole-second zone-less local date-time to epoch milliseconds in {@code timeZone}.
     * Callers pass the value with its fraction removed and add that fraction to the result, because the
     * fallback below has no sub-second field; zone rules never transition at a fractional second, so
     * dropping it cannot change which offsets are valid.
     *
     * <p>A zone {@link ZoneId} can express faithfully gets the strict resolver, so daylight-saving gaps
     * and overlaps are rejected. A custom zone carrying rules no {@code ZoneId} can express falls back
     * to a non-lenient proleptic {@link GregorianCalendar}, matching what the legacy {@code parseTo*}
     * siblings accept for the same zone: failing instead would have made {@code parseToTimestamp} the
     * only target that rejects a zone {@code parseToJUDate} and {@code parseToDate} handle. The
     * fallback keeps {@code Calendar}'s weaker daylight-saving resolution, which silently picks one
     * side of an overlap.</p>
     */
    private static long resolveLocalMillis(final LocalDateTime localDateTime, final TimeZone timeZone) {
        final ZoneId zoneId;

        try {
            zoneId = toZoneId(timeZone);
        } catch (final IllegalArgumentException e) {
            final GregorianCalendar calendar = newProlepticGregorianCalendar(timeZone);
            calendar.setLenient(false);
            calendar.clear();
            //noinspection MagicConstant
            calendar.set(localDateTime.getYear(), localDateTime.getMonthValue() - 1, localDateTime.getDayOfMonth(), localDateTime.getHour(),
                    localDateTime.getMinute(), localDateTime.getSecond());

            return calendar.getTimeInMillis();
        }

        return resolveLocalDateTimeStrict(localDateTime, zoneId, null).toInstant().toEpochMilli();
    }

    /**
     * Parses an auto-detected timestamp shape through a nanosecond-capable path. Returns {@code null}
     * when {@code text} is not one of the supported fractional timestamp shapes, allowing the caller
     * to continue through the general legacy parser.
     */
    private static Instant parseAutoTimestampToInstant(final String text, final TimeZone timeZone) {
        final String detectedFormat = checkDateFormat(text, null);

        if (text.length() > 20 && text.charAt(10) == 'T' && text.charAt(19) == '.' && text.endsWith("]") && text.lastIndexOf('[') > 20
                && isIsoOffsetOrZuluBefore(text, text.lastIndexOf('['))) {
            // The DTF parsers read 'yyyy' proleptically and would accept year 0000 here alone.
            checkFixedFourDigitYearText(text, ISO_ZONED_DATE_TIME_FORMAT);

            return DTF.AUTO_ISO_ZONED_DATE_TIME.parseToInstant(normalizeCompactIsoOffsetText(text, ISO_ZONED_DATE_TIME_FORMAT), timeZone);
        }

        // isJdbcTimestampString, not just the detected format: detection only inspects the '-' at index 4,
        // the separator at index 10, the '.' at index 19 and the fraction digits, so text whose remaining
        // separators are wrong ("2025-01x15 10:30:45.123") reached parseJdbcTimestamp, which reads fixed
        // digit positions and therefore returned a plausible but unintended instant. Text this rejects
        // falls through to parse(), whose checkFixedWidthLegacyText reports the canonical-shape error.
        if (LOCAL_TIMESTAMP_FORMAT.equals(detectedFormat) && text.length() > 19 && isJdbcTimestampString(text)) {
            final TimeZone effectiveZone = timeZone == null ? TimeZone.getDefault() : (TimeZone) timeZone.clone();

            try {
                return parseJdbcTimestamp(text, effectiveZone).toInstant();
            } catch (final DateTimeException | IllegalArgumentException e) {
                throw parseFailure(text, null, effectiveZone, e);
            }
        }

        if (ISO_LOCAL_TIMESTAMP_FORMAT.equals(detectedFormat) && text.length() > 19 && text.charAt(19) == '.') {
            checkFixedFourDigitYearText(text, detectedFormat);

            // Not through a DTF instant parser: that converts the zone with toZoneId and has no Calendar
            // fallback, so a custom TimeZone whose rules no ZoneId can express was rejected on this one
            // auto-detected shape while parseToJUDate and parseToCalendar accepted the same text, and
            // the explicit constant was accepted by every target. resolveLocalMillis carries the
            // class-wide fallback; it works in whole seconds, so the fraction is re-added afterwards.
            final TimeZone effectiveZone = timeZone == null ? TimeZone.getDefault() : (TimeZone) timeZone.clone();

            try {
                final LocalDateTime localDateTime = DTF.AUTO_ISO_LOCAL_TIMESTAMP.parseToLocalDateTime(text);

                return Instant.ofEpochMilli(resolveLocalMillis(localDateTime.withNano(0), effectiveZone)).plusNanos(localDateTime.getNano());
            } catch (final DateTimeException | IllegalArgumentException e) {
                // The DTF parse failure already wraps the DateTimeParseException; report that cause once.
                throw parseFailure(text, null, effectiveZone, e.getCause() instanceof Exception ? (Exception) e.getCause() : e);
            }
        }

        if (ISO_8601_TIMESTAMP_FORMAT.equals(detectedFormat) && text.length() > 19 && text.charAt(19) == '.') {
            checkFixedFourDigitYearText(text, detectedFormat);

            // No fallback zone at all: the text's own 'Z' is the zone, and a designator the text carries
            // is data, not a caller-chosen fixed-zone pattern, so it wins over the supplied zone exactly
            // as a numeric offset does (see checkTimeZone's formatAutoDetected). Passing the zone would
            // make the fixed-Z formatter treat it as a conflict.
            return DTF.AUTO_ISO_8601_TIMESTAMP.parseToInstant(text, null);
        }

        if (isFractionalIsoOffsetDateTime(text)) {
            checkFixedFourDigitYearText(text, ISO_OFFSET_DATE_TIME_FORMAT);

            try {
                // The compact +HHmm offset is accepted without a fraction and by the explicit constants;
                // DateTimeFormatter.ISO_OFFSET_DATE_TIME insists on the colon, so widen it here too.
                return OffsetDateTime.parse(normalizeCompactIsoOffsetText(text, ISO_OFFSET_DATE_TIME_FORMAT), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        .toInstant();
            } catch (final DateTimeException | IllegalArgumentException e) {
                // The grammar is named as DETECTED, not as the caller's: this branch only runs for an
                // auto-detected parse, and rendering a description in the "format '...'" slot told the caller
                // they had supplied it. The other auto-detected sites report it the same way.
                throw autoDetectedParseFailure(text, "ISO_OFFSET_DATE_TIME with a 1-9 digit fraction", null, e);
            }
        }

        return null;
    }

    /**
     * Returns whether {@code text} has an ISO local date-time, a 1-9 digit fraction, and a numeric offset
     * in the {@code +HH:mm}, compact {@code +HHmm} or seconds-precision {@code +HH:mm:ss} form (26
     * characters at the shortest) - the same offset shapes the fraction-less ISO forms accept, so an
     * hour-only {@code -08} is rejected here exactly as it is without a fraction.
     */
    private static boolean isFractionalIsoOffsetDateTime(final String text) {
        if (text.length() < 26 || text.charAt(4) != '-' || text.charAt(7) != '-' || text.charAt(10) != 'T' || text.charAt(13) != ':' || text.charAt(16) != ':'
                || text.charAt(19) != '.') {
            return false;
        }

        int offsetIndex = text.indexOf('+', 20);

        if (offsetIndex < 0) {
            offsetIndex = text.indexOf('-', 20);
        }

        final int fractionDigits = offsetIndex - 20;
        return fractionDigits >= 1 && fractionDigits <= 9 && isAllDigits(text, 20, offsetIndex) && isIsoOffsetTail(text, offsetIndex + 1);
    }

    /** Whether {@code text} from {@code start} on is exactly {@code HHmm}, {@code HH:mm} or {@code HH:mm:ss}. */
    private static boolean isIsoOffsetTail(final String text, final int start) {
        return isIsoOffsetTail(text, start, text.length());
    }

    /** Whether the offset that ends at {@code end} (exclusive) is {@code Z} or a signed {@link #isIsoOffsetTail} shape. */
    private static boolean isIsoOffsetOrZuluBefore(final String text, final int end) {
        if (text.charAt(end - 1) == 'Z') {
            return true;
        }

        final int sign = Math.max(text.lastIndexOf('+', end - 1), text.lastIndexOf('-', end - 1));

        return sign > 10 && isIsoOffsetTail(text, sign + 1, end);
    }

    /** Whether {@code text} between {@code start} and {@code end} (exclusive) is exactly {@code HHmm}, {@code HH:mm} or {@code HH:mm:ss}. */
    private static boolean isIsoOffsetTail(final String text, final int start, final int end) {
        final int len = end - start;

        switch (len) {
            case 4:
                return isAllDigits(text, start, start + 4);
            case 5:
                return isAllDigits(text, start, start + 2) && text.charAt(start + 2) == ':' && isAllDigits(text, start + 3, start + 5);
            case 8:
                return isAllDigits(text, start, start + 2) && text.charAt(start + 2) == ':' && isAllDigits(text, start + 3, start + 5)
                        && text.charAt(start + 5) == ':' && isAllDigits(text, start + 6, start + 8);
            default:
                return false;
        }
    }

    /**
     * Resolves a local date-time without the adjustment performed by {@link LocalDateTime#atZone(ZoneId)}.
     * A gap has no valid offset and an overlap has two; both are rejected unless the input supplied a
     * valid preferred offset that disambiguates the overlap.
     */
    private static ZonedDateTime resolveLocalDateTimeStrict(final LocalDateTime localDateTime, final ZoneId zone, final ZoneOffset preferredOffset) {
        final List<ZoneOffset> validOffsets = zone.getRules().getValidOffsets(localDateTime);

        if (preferredOffset != null) {
            if (!validOffsets.contains(preferredOffset)) {
                throw new DateTimeException(validOffsets.isEmpty() ? "Nonexistent local date-time " + localDateTime + " in zone " + zone
                        : "Offset " + preferredOffset + " is not valid for local date-time " + localDateTime + " in zone " + zone + "; valid offsets are "
                                + validOffsets);
            }

            return ZonedDateTime.ofStrict(localDateTime, preferredOffset, zone);
        }

        if (validOffsets.isEmpty()) {
            throw new DateTimeException("Nonexistent local date-time " + localDateTime + " in zone " + zone);
        }

        if (validOffsets.size() > 1) {
            throw new DateTimeException("Ambiguous local date-time " + localDateTime + " in zone " + zone + "; valid offsets are " + validOffsets);
        }

        return ZonedDateTime.ofStrict(localDateTime, validOffsets.get(0), zone);
    }

    /**
     * Converts a legacy {@link TimeZone} only when its actual rules can be represented faithfully by
     * {@link ZoneId}. The ID is never trusted by itself: a custom zone may reuse a registered ID while
     * carrying completely different rules.
     * @throws IllegalArgumentException if {@code timeZone} is {@code null}, its rules cannot be represented by a {@code ZoneId}, or its fixed offset is sub-second or outside the supported range.
     */
    private static ZoneId toZoneId(final TimeZone timeZone) throws IllegalArgumentException {
        N.checkArgNotNull(timeZone, cs.timeZone);

        final int rawOffsetMillis = timeZone.getRawOffset();
        final TimeZone fixedOffsetZone = new SimpleTimeZone(rawOffsetMillis, "fixed-offset");

        // Check the rules before consulting the ID. This preserves a manually-created fixed zone even
        // when it deliberately or accidentally reuses a registered region ID.
        if (timeZone.hasSameRules(fixedOffsetZone) && fixedOffsetZone.hasSameRules(timeZone)) {
            return toZoneOffset(timeZone, rawOffsetMillis);
        }

        final ZoneId zoneId;

        try {
            zoneId = timeZone.toZoneId();
        } catch (final DateTimeException originalException) {
            // An ID java.time does not know. Only the rules matter, and a zone that observes no daylight
            // saving is a plain fixed offset whatever it is called. The fast path above recognises that
            // only for SimpleTimeZone (its hasSameRules is the one reciprocal with the probe); any other
            // TimeZone subclass lands here, and useDaylightTime() is the same self-description the fast
            // path trusts.
            if (!timeZone.useDaylightTime() && timeZone.getDSTSavings() == 0) {
                return toZoneOffset(timeZone, rawOffsetMillis);
            }

            throw new IllegalArgumentException(
                    "Time zone '" + timeZone.getID() + "' has daylight-saving rules under an ID java.time does not know, so no java.time.ZoneId can express it",
                    originalException);
        }

        final TimeZone registeredZone = TimeZone.getTimeZone(zoneId);

        // Use a reciprocal check because TimeZone subclasses are allowed to specialize hasSameRules.
        if (!timeZone.hasSameRules(registeredZone) || !registeredZone.hasSameRules(timeZone)) {
            throw new IllegalArgumentException(
                    "Time zone '" + timeZone.getID() + "' carries rules that differ from the registered java.time zone with the same ID");
        }

        return zoneId;
    }

    /**
     * The instant at which the JDK's legacy {@code java.util.TimeZone} history begins. {@code ZoneInfo}
     * drops the transitions before 1900 and reports the zone's present raw offset for every earlier
     * instant ({@code ZoneInfo.getLastRawOffset}), while {@code java.time} keeps the complete
     * local-mean-time history; the table then begins here with the offset in force in 1900, so most zones
     * carry a synthetic transition at this very instant. From here until {@link #LEGACY_ZONE_TABLE_END} the
     * two engines agree on the offset at every instant for every zone the runtime ships - though not on
     * every transition: the synthetic one here, and a week's drift of the projected rule after 2100 in
     * Asia/Gaza and Asia/Hebron, are why {@code set*} works on a calendar pinned to the source offset.
     */
    private static final long LEGACY_ZONE_HISTORY_START = -2208988800000L; // 1900-01-01T00:00:00Z

    /**
     * The instant from which the legacy table can disagree with {@code java.time} again. The JDK's
     * {@code ZoneInfoFile} closes every zone's transition table at the start of local year 2100
     * ({@code LASTYEAR}); a zone whose history ends on an explicit transition rather than a recurring
     * rule then drops to its raw offset for ever, where {@code ZoneRules} keeps the last offset in
     * force: Africa/Casablanca and Africa/El_Aaiun (+01 read as +00), Africa/Windhoek (+02 read as
     * +01); Asia/Gaza and Asia/Hebron differ for one week in some later years. Windhoek's table closes
     * at 2099-12-31T23:00Z (local standard midnight), so the bound keeps a day of slack for any raw offset.
     */
    private static final long LEGACY_ZONE_TABLE_END = 4102358400000L; // 2099-12-31T00:00:00Z

    /**
     * The time zone to hand the legacy {@link Calendar}/{@link SimpleDateFormat} engine so that it reads
     * and writes the same civil fields {@code java.time} does at {@code epochMillis}.
     *
     * <p>{@code java.util.TimeZone}'s transition table begins at
     * {@link #LEGACY_ZONE_HISTORY_START 1900-01-01T00:00:00Z}; {@code java.time} keeps every zone's
     * local-mean-time history, and 548 of the 604 IANA zones disagree before that point
     * (Asia/Kolkata kept +05:21:10 from 1870 until 1906, where the legacy table says
     * +05:30 - the present raw offset - before 1900). This class rounds, compares and parses through {@link ZoneId}, so a legacy
     * engine left on the truncated table printed one civil date while the rest of the class computed
     * with another: {@code format} said {@code 1899-06-15 12:00:00} where {@code truncate(DATE)}
     * landed on {@code 1899-06-15 00:08:50} and {@code parseToTimestamp} read that same text as an
     * instant 8m50s away from the one {@code parseToJUDate} produced.
     *
     * <p>The stand-in is a fixed-offset zone carrying the original ID, and it is substituted only for
     * the instants where the two engines actually disagree - all of which sit in one unbounded
     * local-mean-time regime, so a single offset describes the whole neighbourhood. Field arithmetic
     * that carries such a value out of that regime keeps the offset of the side it started on, so the
     * result's wall clock can differ from the input's by the local-mean-time delta; following the full
     * history instead would need a zone object {@code java.util.TimeZone} cannot express, and the code
     * this replaced produced the same value there while <i>also</i> misreading the input's civil date.
     * Values from 1900 to the end of 2099 keep the caller's own zone object and therefore render
     * byte-for-byte as before, daylight-saving names included; from 2100 on the legacy table can drop a
     * zone to its raw offset for ever ({@link #LEGACY_ZONE_TABLE_END}), so those instants are compared
     * too - the stand-in then carries the offset {@code java.time} keeps, and a zone whose recurring rule
     * both engines project alike (New York, Berlin, Sydney) still gets its own object back.
     *
     * @param timeZone the zone the caller asked for; not {@code null}
     * @param epochMillis the instant whose civil fields are about to be read or written
     * @return {@code timeZone} itself, or a fixed-offset stand-in that agrees with its {@link ZoneRules}
     */
    private static TimeZone legacyRenderingZone(final TimeZone timeZone, final long epochMillis) {
        if (epochMillis >= LEGACY_ZONE_HISTORY_START && epochMillis < LEGACY_ZONE_TABLE_END) {
            return timeZone;
        }

        final ZoneId zoneId;

        try {
            zoneId = toZoneId(timeZone);
        } catch (final IllegalArgumentException e) {
            // Rules no ZoneId can express: there is no java.time view to align with, and every other
            // operation in this class either rejects such a zone or falls back to Calendar for it too.
            return timeZone;
        }

        final int isoOffset = zoneId.getRules().getOffset(Instant.ofEpochMilli(epochMillis)).getTotalSeconds() * 1000;

        return isoOffset == timeZone.getOffset(epochMillis) ? timeZone : new SimpleTimeZone(isoOffset, timeZone.getID());
    }

    /**
     * The instant named by the civil fields {@code work} now holds, resolved the way
     * {@link ZonedDateTime#ofLocal(LocalDateTime, ZoneId, ZoneOffset)} resolves them: a wall clock an
     * overlap repeats keeps the offset the <i>input</i> was already on whenever that offset is still one
     * of the two valid ones, a wall clock a gap removes moves forward by the length of the gap, and a
     * single valid offset leaves nothing to choose.
     *
     * <p>For {@code work} calendars pinned to a fixed offset by {@link #addCivilFieldMillis}, whose own
     * {@code getTimeInMillis()} is therefore not in the result's zone. {@link Calendar} has no
     * preferred-offset concept - it resolves every ambiguous wall time to the standard-time offset - and
     * no single gap rule either, which is why {@code addMonths}/{@code addYears} used to land an hour away
     * from the {@code addDays}/{@code addWeeks} that reached the same civil date. This is the policy the
     * rest of the class already applies: {@code truncate}/{@code round}/{@code ceiling} resolve boundaries
     * by the same rule, and the parsers reject an overlap the text cannot disambiguate.</p>
     *
     * @param work a proleptic {@link GregorianCalendar} holding the computed civil fields; not {@code null}
     * @param rules the rules of the zone the result belongs to; not {@code null}
     * @param preferredOffset the offset the operation started on, kept when the result is ambiguous
     * @return the resolved epoch milliseconds
     */
    private static long resolveCivilFields(final Calendar work, final ZoneRules rules, final ZoneOffset preferredOffset) {
        // Before reading any field: this is what applies Calendar's own resolution and therefore what
        // reports an invalid field combination to the caller.
        final long fixedOffsetMillis = work.getTimeInMillis();
        final LocalDateTime local = civilFieldsOf(work);

        if (local == null) {
            return fixedOffsetMillis;
        }

        final List<ZoneOffset> validOffsets = rules.getValidOffsets(local);

        if (validOffsets.size() == 1) {
            return instantAt(local, validOffsets.get(0));
        }

        if (validOffsets.isEmpty()) {
            // A spring-forward gap removed this wall clock: move forward by the length of the gap, which
            // is what java.time does and what Calendar already does for a month or year step.
            final ZoneOffsetTransition gap = rules.getTransition(local);
            return instantAt(local.plusSeconds(gap.getDuration().getSeconds()), gap.getOffsetAfter());
        }

        // ofLocal's tie-break: the offset the operation started on when it is still valid, otherwise the
        // earlier occurrence - never Calendar's unconditional standard-time choice.
        return instantAt(local, validOffsets.contains(preferredOffset) ? preferredOffset : validOffsets.get(0));
    }

    /**
     * The instant the civil fields of {@code work} name in {@code zone}, resolved the way this class
     * resolves every wall clock: a single valid offset is taken, a wall clock a spring-forward gap removes
     * moves forward by the gap, and a wall clock an overlap repeats keeps the offset the source was on.
     *
     * <p>{@code work} carries the fields on a calendar pinned to the offset {@code renderingZone} - the zone
     * {@link #legacyRenderingZone(TimeZone, long)} chose for the source - shows at the source, so no zone
     * table has normalised them. The rules the fields are resolved through are {@code zone}'s own whenever
     * the legacy history exists at the source, so a value from 2100 on that is moved back into the zone's
     * history follows it (a 2100 Casablanca value set to 2025 lands in that year's Ramadan offset, or is
     * carried forward out of its gap, exactly as {@code ZonedDateTime} does), and a target beyond the table's
     * end - {@code setYears(x, 9999)} - lands where {@code addYears} does. A pre-1900 source resolves through
     * {@code renderingZone}: the fixed-offset stand-in where the two histories disagree (the documented
     * regime-crossing rule), the zone itself where they agree. For a zone no {@link ZoneId} can express
     * {@code work} is the zone's own calendar and {@code Calendar}'s resolution is the only view available.</p>
     */
    private static long resolveCivilFieldsPreferringSourceOffset(final Calendar work, final long sourceMillis, final TimeZone zone,
            final TimeZone renderingZone) {
        final long computedMillis = work.getTimeInMillis();
        final TimeZone rulesZone = sourceMillis >= LEGACY_ZONE_HISTORY_START ? zone : renderingZone;
        final ZoneId zoneId;

        try {
            zoneId = toZoneId(rulesZone == null ? TimeZone.getDefault() : rulesZone);
        } catch (final IllegalArgumentException e) {
            // Rules no ZoneId can express: Calendar's own resolution is the only view available, exactly
            // as in resolveLocalMillis and checkGapAndOverlap.
            return computedMillis;
        }

        final ZoneRules rules = zoneId.getRules();
        final LocalDateTime local = civilFieldsOf(work);

        if (local == null) {
            return computedMillis;
        }

        final List<ZoneOffset> validOffsets = rules.getValidOffsets(local);

        if (validOffsets.isEmpty()) {
            // A gap only the real rules see: the stand-in's fixed offset normalised nothing, so the fields
            // name a wall clock the zone skips. Forward by the gap's length, as ZonedDateTime resolves it.
            final ZoneOffsetTransition gap = rules.getTransition(local);

            return instantAt(local.plusSeconds(gap.getDuration().getSeconds()), gap.getOffsetAfter());
        }

        if (validOffsets.size() == 1) {
            return instantAt(local, validOffsets.get(0));
        }

        final ZoneOffset sourceOffset = rules.getOffset(Instant.ofEpochMilli(sourceMillis));

        return instantAt(local, validOffsets.contains(sourceOffset) ? sourceOffset : validOffsets.get(0));
    }

    /**
     * The local date-time the civil fields of {@code work} name, or {@code null} when they fall outside
     * {@link LocalDateTime}'s year range - unreachable for any epoch-millisecond value, since the widest
     * one is year 292,278,994.
     */
    @MayReturnNull
    private static LocalDateTime civilFieldsOf(final Calendar work) {
        try {
            return LocalDateTime.of(work.get(Calendar.ERA) == GregorianCalendar.AD ? work.get(Calendar.YEAR) : 1 - work.get(Calendar.YEAR),
                    work.get(Calendar.MONTH) + 1, work.get(Calendar.DAY_OF_MONTH), work.get(Calendar.HOUR_OF_DAY), work.get(Calendar.MINUTE),
                    work.get(Calendar.SECOND), work.get(Calendar.MILLISECOND) * 1_000_000);
        } catch (final RuntimeException e) {
            return null;
        }
    }

    /** The four {@link CalendarField}s whose {@code add} is calendar arithmetic, not a fixed duration. */
    private static boolean isCivilAddField(final CalendarField unit) {
        return unit == CalendarField.YEAR || unit == CalendarField.MONTH || unit == CalendarField.WEEK_OF_YEAR || unit == CalendarField.DAY_OF_MONTH;
    }

    /**
     * Adds {@code amount} of a calendar field to {@code sourceMillis}, evaluated in {@code timeZone}.
     *
     * <p>The field walk runs on a calendar pinned to the source instant's own offset, so no zone
     * transition can perturb it, and the single resolution afterwards applies one documented rule to the
     * result. That split matters because {@link Calendar#add(int, int)} does not use one rule: a day or
     * week step is elapsed time corrected for the offset change, which lands an hour <i>before</i> a
     * spring-forward gap (so {@code addDays} walked the wall clock backwards from 02:00 to 01:00), while a
     * month or year step is a field rewrite that resolves the same gap forward. Both now behave as
     * {@link ZonedDateTime#plusDays(long)} and {@link ZonedDateTime#plusMonths(long)} do.</p>
     *
     * <p>For a pre-1900 source the rules come from {@link #legacyRenderingZone(TimeZone, long)}'s stand-in,
     * so such a value is evaluated and resolved in the same single local-mean-time offset it is rendered
     * in, exactly as before. Any later source - including one past the 2100 end of the legacy table,
     * which is rendered through a stand-in too - is resolved through the zone's real rules: the walk is
     * pinned to the source's own offset regardless, and a result that lands back inside the zone's
     * history (a 2100 value moved 75 years back into a Ramadan month of Casablanca) must follow that
     * history, not the stand-in's single offset.</p>
     *
     * @param sourceMillis the instant to add to
     * @param timeZone the zone the civil fields belong to; not {@code null}
     * @param amount the amount to add, may be negative
     * @param unit one of the fields {@link #isCivilAddField(CalendarField)} accepts
     * @return the resulting epoch milliseconds
     * @throws ArithmeticException if the result leaves the signed-long epoch-millisecond range
     */
    private static long addCivilFieldMillis(final long sourceMillis, final TimeZone timeZone, final int amount, final CalendarField unit)
            throws ArithmeticException {
        final TimeZone renderingZone = legacyRenderingZone(timeZone, sourceMillis);
        final ZoneId zoneId;

        try {
            zoneId = toZoneId(sourceMillis >= LEGACY_ZONE_HISTORY_START ? timeZone : renderingZone);
        } catch (final IllegalArgumentException e) {
            // Rules no ZoneId can express: keep the legacy engine end to end rather than approximating
            // them with a fixed offset, which would change the field walk itself.
            final GregorianCalendar legacy = newProlepticGregorianCalendar(renderingZone);
            legacy.setTimeInMillis(sourceMillis);
            addCalendarFieldExact(legacy, amount, unit);

            return legacy.getTimeInMillis();
        }

        final ZoneRules rules = zoneId.getRules();
        final ZoneOffset sourceOffset = rules.getOffset(Instant.ofEpochMilli(sourceMillis));

        // Not createCalendar(date) and not the caller's calendar: the field walk must be proleptic
        // Gregorian, so that a pre-1582 instant lands on the date format() prints for it, and must not
        // follow a default locale that selects a Buddhist or Japanese-imperial calendar.
        final GregorianCalendar work = newProlepticGregorianCalendar(new SimpleTimeZone(sourceOffset.getTotalSeconds() * 1000, "civil-arithmetic"));
        work.setTimeInMillis(sourceMillis);
        addCalendarFieldExact(work, amount, unit);

        return resolveCivilFields(work, rules, sourceOffset);
    }

    /**
     * @throws IllegalArgumentException if the fixed offset is not a whole number of seconds or is outside the {@code ZoneOffset} range.
     */
    private static ZoneOffset toZoneOffset(final TimeZone timeZone, final int rawOffsetMillis) throws IllegalArgumentException {
        if (rawOffsetMillis % 1000 != 0) {
            throw new IllegalArgumentException("Fixed time zone '" + timeZone.getID() + "' has a sub-second offset (" + rawOffsetMillis
                    + " ms) that cannot be represented as a java.time.ZoneOffset");
        }

        try {
            return ZoneOffset.ofTotalSeconds(rawOffsetMillis / 1000);
        } catch (final DateTimeException offsetException) {
            throw new IllegalArgumentException(
                    "Fixed time zone '" + timeZone.getID() + "' has an offset outside the java.time range: " + rawOffsetMillis + " ms", offsetException);
        }
    }

    /**
     * Parses a string representation of a date/time into a proleptic {@link GregorianCalendar},
     * returned as the general {@code Calendar} type.
     * Attempts to automatically detect the format from common patterns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToCalendar("2025-01-15T10:30:45Z").getTimeInMillis();   // returns 1736937045000
     * Dates.parseToCalendar("1736937045000");                            // throws IllegalArgumentException; use Dates.parseEpochMillis("1736937045000") instead
     *
     * Dates.parseToCalendar((String) null);                              // returns null
     * Dates.parseToCalendar("");                                         // throws IllegalArgumentException
     * Dates.parseToCalendar("null");                                     // returns null (the literal string "null")
     * }</pre>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @return the parsed proleptic {@code GregorianCalendar}, or {@code null} if the input is {@code null} or the case-insensitive marker {@code "null"}.
     *         Its own {@code get} reads follow the JDK zone table, which differs from the {@code java.time}
     *         history before 1900 and from 2100 on (see the class's <i>Zone Rules</i>); {@code format} and the
     *         field operations read it through the aligned view.
     * @throws IllegalArgumentException if the date/time string cannot be parsed.
     * @see #parseToCalendar(String, String)
     * @see #parseToCalendar(String, String, TimeZone)
     * @see #createCalendar(long)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Calendar parseToCalendar(final String calendar) throws IllegalArgumentException {
        return parseToCalendar(calendar, null);
    }

    /**
     * Parses a string representation of a date/time into a proleptic {@link GregorianCalendar},
     * returned as the general {@code Calendar} type, using the specified format.
     * If the format is {@code null} or empty, attempts to automatically detect the format.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.format(Dates.parseToCalendar("22/10/2025 14:30", "dd/MM/yyyy HH:mm"), "dd/MM/yyyy HH:mm");
     *                                                  // returns "22/10/2025 14:30" (default zone round-trip)
     *
     * Dates.parseToCalendar((String) null, "dd/MM/yyyy HH:mm");   // returns null
     * Dates.parseToCalendar("bad", "dd/MM/yyyy HH:mm");           // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @param format the date/time format pattern; if {@code null} or empty, common formats are attempted automatically.
     * @return the parsed proleptic {@code GregorianCalendar}, or {@code null} if the input is {@code null} or the case-insensitive marker {@code "null"}.
     *         Its own {@code get} reads follow the JDK zone table, which differs from the {@code java.time}
     *         history before 1900 and from 2100 on (see the class's <i>Zone Rules</i>); {@code format} and the
     *         field operations read it through the aligned view.
     * @throws IllegalArgumentException if the date/time string cannot be parsed using the specified format.
     * @see #parseToCalendar(String)
     * @see #parseToCalendar(String, String, TimeZone)
     * @see #parseToJUDate(String, String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Calendar parseToCalendar(final String calendar, final String format) throws IllegalArgumentException {
        return parseToCalendar(calendar, format, null);
    }

    /**
     * Parses a string representation of a date/time into a proleptic {@link GregorianCalendar},
     * returned as the general {@code Calendar} type, using the specified format and time zone.
     * If the format is {@code null} or empty, attempts to automatically detect the format.
     * If the time zone is {@code null}, uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.parseToCalendar("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss", utc).getTimeInMillis();
     *                                                          // returns 1736937045000
     *
     * Dates.parseToCalendar((String) null, "yyyy-MM-dd HH:mm:ss", utc);   // returns null
     * Dates.parseToCalendar("bad", "yyyy-MM-dd HH:mm:ss", utc);           // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Note: the two fixed-{@code 'Z'} UTC constants and {@link #HTTP_DATE_FORMAT}, when supplied
     * explicitly, reject a {@code timeZone} that is not UTC-equivalent (a fixed zero offset under any ID
     * qualifies). With auto-detection the {@code Z} or {@code GMT} the text carries is data, so it wins over
     * {@code timeZone} exactly as a numeric offset or a bracketed region does, and no conflict arises.
     * {@link #ISO_OFFSET_DATE_TIME_FORMAT} accepts either {@code Z} or a numeric offset; that offset is
     * authoritative and {@code timeZone} is only a fallback.</p>
     *
     * <p>Note: purely numeric input (with a {@code null} or empty format) is rejected as ambiguous; use {@link #parseEpochMillis(String)} for epoch-millisecond text.</p>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @param format the date/time format pattern; if {@code null} or empty, common formats are attempted automatically.
     * @param timeZone the fallback zone for zone-less text; if {@code null}, the live default zone is used.
     *        A zone or offset written in the text is preserved by the returned calendar: an offset exactly,
     *        a zone name as the fallback zone when it names that zone, else as the region the JDK maps the
     *        name to when that region shows the wall clock the text spells, else as that wall clock's fixed
     *        offset (the JDK's two zone-name tables disagree for names such as {@code IST} and {@code CST}).
     *        The zone must show the wall clock the text spells: a name the fallback zone owns on a date it
     *        spends on the other offset (a standard name in summer) yields the fixed offset too. {@code GMT} and
     *        {@code UTC} text are offset spellings to {@code SimpleDateFormat} and yield the fixed zone even when
     *        they are the fallback's own short name. The zone's identity is recovered by re-reading the text
     *        with the {@code java.time} grammar; a spelling only {@code SimpleDateFormat} understands (its
     *        {@code u} and {@code F} letters, extra whitespace between fields, a one-digit {@code GMT+5:30}) keeps
     *        the wall clock on a fixed-offset zone carrying the offset {@code SimpleDateFormat} applied - only
     *        the region's identity is lost.
     * @return the parsed proleptic {@code GregorianCalendar}, or {@code null} if the input is {@code null} or the case-insensitive marker {@code "null"}.
     *         Its own {@code get} reads follow the JDK zone table, which differs from the {@code java.time}
     *         history before 1900 and from 2100 on (see the class's <i>Zone Rules</i>); {@code format} and the
     *         field operations read it through the aligned view.
     * @throws IllegalArgumentException if the pattern lacks a complete date, the date/time string cannot
     *         be parsed using the specified format, or a fixed UTC/GMT format is combined with a
     *         non-UTC-equivalent time zone.
     * @see #parseToCalendar(String)
     * @see #parseToCalendar(String, String)
     * @see #createCalendar(long, TimeZone)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Calendar parseToCalendar(final String calendar, final String format, final TimeZone timeZone) throws IllegalArgumentException {
        return parseToCalendar(calendar, format, timeZone, Locale.US);
    }

    /**
     * Parses a calendar with an explicit locale for locale-sensitive legacy pattern fields. The
     * returned object is a proleptic {@link GregorianCalendar}; {@code locale} also supplies its
     * locale-dependent week settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.parseToCalendar("15 Jan 2025", "dd MMM yyyy", utc, Locale.US).getTimeInMillis();
     *                                                  // returns 1736899200000
     * Dates.parseToCalendar("15 janv. 2025", "dd MMM yyyy", utc, Locale.FRENCH).getTimeInMillis();
     *                                                  // returns 1736899200000 (French month name)
     *
     * Dates.parseToCalendar((String) null, "dd MMM yyyy", utc, Locale.US);      // returns null
     * Dates.parseToCalendar("15 Jan 2025", "dd MMM yyyy", utc, Locale.FRENCH);  // throws IllegalArgumentException (English name, French locale)
     * Dates.parseToCalendar("15 Jan 2025", "dd MMM yyyy", utc, null);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the text to parse
     * @param format the pattern, or {@code null}/empty for automatic detection
     * @param timeZone the fallback zone for zone-less text, or {@code null} for the live machine default zone;
     *        a zone or offset written in the text is preserved by the returned calendar (a zone name as
     *        described on {@link #parseToCalendar(String, String, TimeZone)})
     * @param locale the locale for locale-sensitive pattern fields; must not be {@code null}
     * @return the parsed calendar, or {@code null} for a {@code null} reference or the case-insensitive marker {@code "null"};
     *         its own {@code get} reads follow the JDK zone table before 1900 and from 2100 on (see
     *         {@link #parseToCalendar(String, String, TimeZone)})
     * @throws IllegalArgumentException if {@code locale} is {@code null}, the pattern lacks a complete
     *         date, the text cannot be parsed, or the zone conflicts with a fixed-zone format
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Calendar parseToCalendar(final String calendar, final String format, final TimeZone timeZone, final Locale locale)
            throws IllegalArgumentException {
        N.checkArgNotNull(locale, cs.locale);

        if (isNullParseInput(calendar)) {
            return null;
        }

        rejectEmptyDateTime(calendar);
        checkCompleteLegacyDateFormat(calendar, format, "parseToCalendar");

        // Build a proleptic result. A textual zone/offset is part of Calendar's value and is preserved;
        // the captured supplied/default zone is only the fallback for zone-less text. Returning a
        // default-cutover Calendar would reinterpret pre-1582 instants as different civil dates.
        final boolean noZoneSupplied = timeZone == null;
        final TimeZone suppliedZone = noZoneSupplied ? TimeZone.getDefault() : (TimeZone) timeZone.clone();
        final AppliedZoneOffset applied = new AppliedZoneOffset();
        final long millis = parse(calendar, format, suppliedZone, locale, noZoneSupplied, applied);
        return createParsedGregorianCalendar(millis, calendarResultTimeZone(calendar, format, millis, suppliedZone, locale, applied), locale);
    }

    /**
     * Parses a string representation of a date/time into a {@code java.util.GregorianCalendar} object.
     * Attempts to automatically detect the format from common patterns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToGregorianCalendar("2025-01-15T10:30:45Z").getTimeInMillis();   // returns 1736937045000
     * Dates.parseToGregorianCalendar("1736937045000");                            // throws IllegalArgumentException; use Dates.parseEpochMillis("1736937045000") instead
     *
     * Dates.parseToGregorianCalendar((String) null);                              // returns null
     * Dates.parseToGregorianCalendar("");                                         // throws IllegalArgumentException
     * Dates.parseToGregorianCalendar("null");                                     // returns null (the literal string "null")
     * }</pre>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @return the parsed {@code java.util.GregorianCalendar} instance, or {@code null} if the input is {@code null} or the case-insensitive marker {@code "null"}.
     *         Its own {@code get} reads follow the JDK zone table, which differs from the {@code java.time}
     *         history before 1900 and from 2100 on (see the class's <i>Zone Rules</i>); {@code format} and the
     *         field operations read it through the aligned view.
     * @throws IllegalArgumentException if the date/time string cannot be parsed.
     * @see #parseToGregorianCalendar(String, String)
     * @see #parseToGregorianCalendar(String, String, TimeZone)
     * @see #parseToCalendar(String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static GregorianCalendar parseToGregorianCalendar(final String calendar) throws IllegalArgumentException {
        return parseToGregorianCalendar(calendar, null);
    }

    /**
     * Parses a string representation of a date/time into a {@code java.util.GregorianCalendar} object using the specified format.
     * If the format is {@code null} or empty, attempts to automatically detect the format.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.format(Dates.parseToGregorianCalendar("22/10/2025 14:30", "dd/MM/yyyy HH:mm"), "dd/MM/yyyy HH:mm");
     *                                                  // returns "22/10/2025 14:30" (default zone round-trip)
     *
     * Dates.parseToGregorianCalendar((String) null, "dd/MM/yyyy HH:mm");   // returns null
     * Dates.parseToGregorianCalendar("bad", "dd/MM/yyyy HH:mm");           // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @param format the date/time format pattern; if {@code null} or empty, common formats are attempted automatically.
     * @return the parsed {@code java.util.GregorianCalendar} instance, or {@code null} if the input is {@code null} or the case-insensitive marker {@code "null"}.
     *         Its own {@code get} reads follow the JDK zone table, which differs from the {@code java.time}
     *         history before 1900 and from 2100 on (see the class's <i>Zone Rules</i>); {@code format} and the
     *         field operations read it through the aligned view.
     * @throws IllegalArgumentException if the date/time string cannot be parsed using the specified format.
     * @see #parseToGregorianCalendar(String)
     * @see #parseToGregorianCalendar(String, String, TimeZone)
     * @see #parseToCalendar(String, String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static GregorianCalendar parseToGregorianCalendar(final String calendar, final String format) throws IllegalArgumentException {
        return parseToGregorianCalendar(calendar, format, null);
    }

    /**
     * Parses a string representation of a date/time into a {@code java.util.GregorianCalendar} object using the specified format and time zone.
     * If the format is {@code null} or empty, attempts to automatically detect the format.
     * If the time zone is {@code null}, uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.parseToGregorianCalendar("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss", utc).getTimeInMillis();
     *                                                          // returns 1736937045000
     *
     * Dates.parseToGregorianCalendar((String) null, "yyyy-MM-dd HH:mm:ss", utc);   // returns null
     * Dates.parseToGregorianCalendar("bad", "yyyy-MM-dd HH:mm:ss", utc);           // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Note: the two fixed-{@code 'Z'} UTC constants and {@link #HTTP_DATE_FORMAT}, when supplied
     * explicitly, reject a {@code timeZone} that is not UTC-equivalent (a fixed zero offset under any ID
     * qualifies). With auto-detection the {@code Z} or {@code GMT} the text carries is data, so it wins over
     * {@code timeZone} exactly as a numeric offset or a bracketed region does, and no conflict arises.
     * {@link #ISO_OFFSET_DATE_TIME_FORMAT} accepts either {@code Z} or a numeric offset; that offset is
     * authoritative and {@code timeZone} is only a fallback.</p>
     *
     * <p>Note: purely numeric input (with a {@code null} or empty format) is rejected as ambiguous; use {@link #parseEpochMillis(String)} for epoch-millisecond text.</p>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @param format the date/time format pattern; if {@code null} or empty, common formats are attempted automatically.
     * @param timeZone the fallback zone for zone-less text; if {@code null}, the live default zone is used.
     *        A zone or offset written in the text is preserved by the returned calendar: an offset exactly,
     *        a zone name as the fallback zone when it names that zone, else as the region the JDK maps the
     *        name to when that region shows the wall clock the text spells, else as that wall clock's fixed
     *        offset (the JDK's two zone-name tables disagree for names such as {@code IST} and {@code CST}).
     *        The zone must show the wall clock the text spells: a name the fallback zone owns on a date it
     *        spends on the other offset (a standard name in summer) yields the fixed offset too. {@code GMT} and
     *        {@code UTC} text are offset spellings to {@code SimpleDateFormat} and yield the fixed zone even when
     *        they are the fallback's own short name. The zone's identity is recovered by re-reading the text
     *        with the {@code java.time} grammar; a spelling only {@code SimpleDateFormat} understands (its
     *        {@code u} and {@code F} letters, extra whitespace between fields, a one-digit {@code GMT+5:30}) keeps
     *        the wall clock on a fixed-offset zone carrying the offset {@code SimpleDateFormat} applied - only
     *        the region's identity is lost.
     * @return the parsed {@code java.util.GregorianCalendar} instance, or {@code null} if the input is {@code null} or the case-insensitive marker {@code "null"}.
     *         Its own {@code get} reads follow the JDK zone table, which differs from the {@code java.time}
     *         history before 1900 and from 2100 on (see the class's <i>Zone Rules</i>); {@code format} and the
     *         field operations read it through the aligned view.
     * @throws IllegalArgumentException if the pattern lacks a complete date, the date/time string cannot
     *         be parsed using the specified format, or a fixed UTC/GMT format is combined with a
     *         non-UTC-equivalent time zone.
     * @see #parseToGregorianCalendar(String)
     * @see #parseToGregorianCalendar(String, String)
     * @see #parseToCalendar(String, String, TimeZone)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static GregorianCalendar parseToGregorianCalendar(final String calendar, final String format, final TimeZone timeZone)
            throws IllegalArgumentException {
        return parseToGregorianCalendar(calendar, format, timeZone, Locale.US);
    }

    /**
     * Parses a proleptic Gregorian calendar with an explicit locale for locale-sensitive legacy
     * pattern fields and locale-dependent week settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.parseToGregorianCalendar("15 Jan 2025", "dd MMM yyyy", utc, Locale.US).getTimeInMillis();
     *                                                  // returns 1736899200000
     * Dates.parseToGregorianCalendar("15 janv. 2025", "dd MMM yyyy", utc, Locale.FRENCH).getTimeInMillis();
     *                                                  // returns 1736899200000 (French month name)
     *
     * Dates.parseToGregorianCalendar((String) null, "dd MMM yyyy", utc, Locale.US);      // returns null
     * Dates.parseToGregorianCalendar("15 Jan 2025", "dd MMM yyyy", utc, Locale.FRENCH);  // throws IllegalArgumentException (English name, French locale)
     * Dates.parseToGregorianCalendar("15 Jan 2025", "dd MMM yyyy", utc, null);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the text to parse
     * @param format the pattern, or {@code null}/empty for automatic detection
     * @param timeZone the fallback zone for zone-less text, or {@code null} for the live machine default zone;
     *        a zone or offset written in the text is preserved by the returned calendar (a zone name as
     *        described on {@link #parseToCalendar(String, String, TimeZone)})
     * @param locale the locale for locale-sensitive pattern fields; must not be {@code null}
     * @return the parsed Gregorian calendar, or {@code null} for a {@code null} reference or the case-insensitive marker {@code "null"};
     *         its own {@code get} reads follow the JDK zone table before 1900 and from 2100 on (see
     *         {@link #parseToCalendar(String, String, TimeZone)})
     * @throws IllegalArgumentException if {@code locale} is {@code null}, the pattern lacks a complete
     *         date, the text cannot be parsed, or the zone conflicts with a fixed-zone format
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static GregorianCalendar parseToGregorianCalendar(final String calendar, final String format, final TimeZone timeZone, final Locale locale)
            throws IllegalArgumentException {
        N.checkArgNotNull(locale, cs.locale);

        if (isNullParseInput(calendar)) {
            return null;
        }

        rejectEmptyDateTime(calendar);
        checkCompleteLegacyDateFormat(calendar, format, "parseToGregorianCalendar");

        // Proleptic parse+create with textual-zone preservation; see parseToCalendar.
        final boolean noZoneSupplied = timeZone == null;
        final TimeZone suppliedZone = noZoneSupplied ? TimeZone.getDefault() : (TimeZone) timeZone.clone();
        final AppliedZoneOffset applied = new AppliedZoneOffset();
        final long millis = parse(calendar, format, suppliedZone, locale, noZoneSupplied, applied);
        return createParsedGregorianCalendar(millis, calendarResultTimeZone(calendar, format, millis, suppliedZone, locale, applied), locale);
    }

    /**
     * Parses a string representation of a date/time into a {@code javax.xml.datatype.XMLGregorianCalendar} object.
     * Attempts to automatically detect the format from common patterns. Because the XML Schema
     * timezone field stores minutes, the authoritative textual/default-zone offset must be a
     * whole-minute value from -14:00 through +14:00; an offset with seconds is rejected rather than
     * silently truncated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45Z").toGregorianCalendar().getTimeInMillis();
     *                                                  // returns 1736937045000
     * Dates.parseToXMLGregorianCalendar("1736937045000");
     *                                                  // throws IllegalArgumentException; use Dates.parseEpochMillis("1736937045000") instead
     *
     * Dates.parseToXMLGregorianCalendar((String) null);   // returns null
     * Dates.parseToXMLGregorianCalendar("");              // throws IllegalArgumentException
     * Dates.parseToXMLGregorianCalendar("null");          // returns null (the literal string "null")
     * }</pre>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @return the parsed {@code javax.xml.datatype.XMLGregorianCalendar} instance, or {@code null} if the input is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the date/time string cannot be parsed or its effective
     *         offset cannot be represented by {@code XMLGregorianCalendar}.
     * @throws UnsupportedOperationException if the {@code DatatypeFactory} is not available.
     * @see #parseToXMLGregorianCalendar(String, String)
     * @see #parseToXMLGregorianCalendar(String, String, TimeZone)
     * @see #parseToGregorianCalendar(String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static XMLGregorianCalendar parseToXMLGregorianCalendar(final String calendar) throws IllegalArgumentException, UnsupportedOperationException {
        return parseToXMLGregorianCalendar(calendar, null);
    }

    /**
     * Parses a string representation of a date/time into a {@code javax.xml.datatype.XMLGregorianCalendar} object using the specified format.
     * If the format is {@code null} or empty, attempts to automatically detect the format.
     * The effective offset must be a whole-minute value from -14:00 through +14:00.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.format(Dates.parseToXMLGregorianCalendar("22/10/2025 14:30", "dd/MM/yyyy HH:mm").toGregorianCalendar(),
     *         "dd/MM/yyyy HH:mm");                     // returns "22/10/2025 14:30" (default zone round-trip)
     *
     * Dates.parseToXMLGregorianCalendar((String) null, "dd/MM/yyyy HH:mm");   // returns null
     * Dates.parseToXMLGregorianCalendar("bad", "dd/MM/yyyy HH:mm");           // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @param format the date/time format pattern; if {@code null} or empty, common formats are attempted automatically.
     * @return the parsed {@code javax.xml.datatype.XMLGregorianCalendar} instance, or {@code null} if the input is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the date/time string cannot be parsed using the specified
     *         format or its effective offset cannot be represented by {@code XMLGregorianCalendar}.
     * @throws UnsupportedOperationException if the {@code DatatypeFactory} is not available.
     * @see #parseToXMLGregorianCalendar(String)
     * @see #parseToXMLGregorianCalendar(String, String, TimeZone)
     * @see #parseToGregorianCalendar(String, String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static XMLGregorianCalendar parseToXMLGregorianCalendar(final String calendar, final String format)
            throws IllegalArgumentException, UnsupportedOperationException {
        return parseToXMLGregorianCalendar(calendar, format, null);
    }

    /**
     * Parses a string representation of a date/time into a {@code javax.xml.datatype.XMLGregorianCalendar} object using the specified format and time zone.
     * If the format is {@code null} or empty, attempts to automatically detect the format.
     * If the time zone is {@code null}, uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.parseToXMLGregorianCalendar("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss", utc)
     *         .toGregorianCalendar().getTimeInMillis();        // returns 1736937045000
     *
     * Dates.parseToXMLGregorianCalendar((String) null, "yyyy-MM-dd HH:mm:ss", utc);   // returns null
     * Dates.parseToXMLGregorianCalendar("bad", "yyyy-MM-dd HH:mm:ss", utc);           // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Note: the two fixed-{@code 'Z'} UTC constants and {@link #HTTP_DATE_FORMAT}, when supplied
     * explicitly, reject a {@code timeZone} that is not UTC-equivalent (a fixed zero offset under any ID
     * qualifies). With auto-detection the {@code Z} or {@code GMT} the text carries is data, so it wins over
     * {@code timeZone} exactly as a numeric offset or a bracketed region does, and no conflict arises.
     * {@link #ISO_OFFSET_DATE_TIME_FORMAT} accepts either {@code Z} or a numeric offset; that offset is
     * authoritative and {@code timeZone} is only a fallback.</p>
     *
     * <p>Note: purely numeric input (with a {@code null} or empty format) is rejected as ambiguous; use {@link #parseEpochMillis(String)} for epoch-millisecond text.</p>
     *
     * <p>The resulting XML timezone is a numeric whole-minute offset. Values outside the XML Schema
     * range -14:00 through +14:00, including offsets with non-zero seconds, are rejected rather than
     * rounded or truncated.</p>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @param format the date/time format pattern; if {@code null} or empty, common formats are attempted automatically.
     * @param timeZone the fallback zone for zone-less text; if {@code null}, the live default zone is used.
     *        A zone or offset written in the text is preserved in the result's XML time-zone field (a zone
     *        name resolves as described on {@link #parseToCalendar(String, String, TimeZone)}).
     * @return the parsed {@code javax.xml.datatype.XMLGregorianCalendar} instance, or {@code null} if the input is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the pattern lacks a complete date, the date/time string cannot
     *         be parsed using the specified format, its effective offset cannot be represented by
     *         {@code XMLGregorianCalendar}, or a fixed UTC/GMT format is combined with a
     *         non-UTC-equivalent time zone.
     * @throws UnsupportedOperationException if the {@code DatatypeFactory} is not available.
     * @see #parseToXMLGregorianCalendar(String)
     * @see #parseToXMLGregorianCalendar(String, String)
     * @see #parseToGregorianCalendar(String, String, TimeZone)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static XMLGregorianCalendar parseToXMLGregorianCalendar(final String calendar, final String format, final TimeZone timeZone)
            throws IllegalArgumentException, UnsupportedOperationException {
        return parseToXMLGregorianCalendar(calendar, format, timeZone, Locale.US);
    }

    /**
     * Parses an XML Gregorian calendar with an explicit locale for locale-sensitive legacy pattern
     * fields. Civil fields are derived with the proleptic Gregorian calendar.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.parseToXMLGregorianCalendar("15 Jan 2025", "dd MMM yyyy", utc, Locale.US).toGregorianCalendar().getTimeInMillis();
     *                                                  // returns 1736899200000
     * Dates.parseToXMLGregorianCalendar("15 janv. 2025", "dd MMM yyyy", utc, Locale.FRENCH).toGregorianCalendar().getTimeInMillis();
     *                                                  // returns 1736899200000 (French month name)
     *
     * Dates.parseToXMLGregorianCalendar((String) null, "dd MMM yyyy", utc, Locale.US);      // returns null
     * Dates.parseToXMLGregorianCalendar("15 Jan 2025", "dd MMM yyyy", utc, Locale.FRENCH);  // throws IllegalArgumentException (English name, French locale)
     * Dates.parseToXMLGregorianCalendar("15 Jan 2025", "dd MMM yyyy", utc, null);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the text to parse
     * @param format the pattern, or {@code null}/empty for automatic detection
     * @param timeZone the fallback zone for zone-less text, or {@code null} for the live machine default zone;
     *        a zone or offset written in the text is preserved in the result's XML time-zone field (a zone
     *        name resolves as described on {@link #parseToCalendar(String, String, TimeZone)})
     * @param locale the locale for locale-sensitive pattern fields; must not be {@code null}
     * @return the parsed XML calendar, or {@code null} for a {@code null} reference or the case-insensitive marker {@code "null"}
     * @throws IllegalArgumentException if {@code locale} is {@code null}, the pattern lacks a complete
     *         date, the text cannot be parsed, the effective offset is not a whole-minute value in
     *         -14:00 through +14:00, or the zone conflicts with a fixed-zone format
     * @throws UnsupportedOperationException if XML datatype support is unavailable
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static XMLGregorianCalendar parseToXMLGregorianCalendar(final String calendar, final String format, final TimeZone timeZone, final Locale locale)
            throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(locale, cs.locale);

        if (isNullParseInput(calendar)) {
            return null;
        }

        // After the null contract, before the work: a missing factory made every argument check and the
        // entire parse run first, only to fail on something none of it depended on.
        if (dataTypeFactory == null) {
            throw new UnsupportedOperationException("DatatypeFactory is not available. XMLGregorianCalendar operations are not supported.");
        }

        rejectEmptyDateTime(calendar);
        checkCompleteLegacyDateFormat(calendar, format, "parseToXMLGregorianCalendar");

        // Proleptic parse+create with textual-zone preservation; see parseToCalendar.
        final boolean noZoneSupplied = timeZone == null;
        final TimeZone suppliedZone = noZoneSupplied ? TimeZone.getDefault() : (TimeZone) timeZone.clone();
        final Instant exactInstant = Strings.isEmpty(format) ? parseAutoTimestampToInstant(calendar, suppliedZone) : null;
        final AppliedZoneOffset applied = new AppliedZoneOffset();
        final long millis = exactInstant == null ? parse(calendar, format, suppliedZone, locale, noZoneSupplied, applied) : exactInstant.toEpochMilli();
        // The XML value carries an offset, not a zone, so the fields are written through the offset format()
        // renders this instant with - before 1900 that is java.time's historical one, not the legacy table's
        // (see createXMLGregorianCalendar(long, TimeZone)); a sub-minute one is unrepresentable and rejected.
        final TimeZone resultZone = legacyRenderingZone(calendarResultTimeZone(calendar, format, millis, suppliedZone, locale, applied), millis);

        checkXMLTimeZoneRepresentable(resultZone, millis);

        final XMLGregorianCalendar result = dataTypeFactory.newXMLGregorianCalendar(createParsedGregorianCalendar(millis, resultZone, locale));

        if (exactInstant != null) {
            // The full nanosecond fraction, at the narrowest scale that still writes whole milliseconds
            // the way the factory does: a plain BigDecimal.valueOf(nano, 9) made "10:30:45.123Z" read
            // back as ".123000000" while createXMLGregorianCalendar wrote ".123" for the same instant, so
            // the default format/parse round trip was XML-equal but never text-equal. Trailing zeros are
            // stripped and the scale floored at 3 (".500", ".000"), so sub-millisecond digits survive
            // (".123456789") and whole-millisecond values match the factory's lexical form.
            final BigDecimal fraction = BigDecimal.valueOf(exactInstant.getNano(), 9).stripTrailingZeros();
            result.setFractionalSecond(fraction.scale() < 3 ? fraction.setScale(3) : fraction);
        }

        return result;
    }

    /**
     * Parses text into a {@code LocalDate}, preserving the civil date exactly as written.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToLocalDate("2025-01-01");                            // returns 2025-01-01
     * Dates.parseToLocalDate("2025-01-15 10:30:45");                   // returns 2025-01-15 (time part ignored)
     * Dates.parseToLocalDate("2025-01-01T00:30:00+14:00");             // returns 2025-01-01 (textual fields, offset ignored)
     *
     * Dates.parseToLocalDate((String) null);                           // returns null
     * Dates.parseToLocalDate("null");                                  // returns null (the formatTo null-token)
     * Dates.parseToLocalDate("");                                      // throws IllegalArgumentException
     * Dates.parseToLocalDate("14:30:45");                              // throws IllegalArgumentException (no date fields)
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @return the parsed {@code LocalDate}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         or cannot be parsed.
     * @see #parseToLocalDate(String, String)
     * @see #parseToLocalDateTime(String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static LocalDate parseToLocalDate(final String text) throws IllegalArgumentException {
        return parseToLocalDate(text, null);
    }

    /**
     * Parses text into a {@code LocalDate} using the specified format (or auto-detection when
     * {@code format} is {@code null}/empty). The civil date is taken exactly as written; any offset or
     * zone in the text is ignored for field extraction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToLocalDate("15/01/2025", "dd/MM/yyyy");     // returns 2025-01-15
     * Dates.parseToLocalDate("2025-01-15", null);             // returns 2025-01-15 (auto-detected)
     *
     * Dates.parseToLocalDate((String) null, "dd/MM/yyyy");    // returns null
     * Dates.parseToLocalDate("", "dd/MM/yyyy");               // throws IllegalArgumentException
     * Dates.parseToLocalDate("15-01-2025", "dd/MM/yyyy");     // throws IllegalArgumentException (does not match the pattern)
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @param format a predefined format constant or a {@link DateTimeFormatter} pattern, or
     *        {@code null}/empty for auto-detection.
     * @return the parsed {@code LocalDate}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         lacks date fields, or cannot be parsed.
     * @see DTF#parseToLocalDate(CharSequence)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static LocalDate parseToLocalDate(final String text, final String format) throws IllegalArgumentException {
        if (isNullParseInput(text)) {
            return null;
        }

        rejectEmptyDateTime(text);
        final boolean autoDetected = Strings.isEmpty(format);
        final String effectiveFormat = requireDetectedFormat(text, format);
        checkCompleteCivilFormat(effectiveFormat, text, true, false);

        return parseReportingCallerFormat(text, format,
                () -> dtfForParsing(effectiveFormat, autoDetected).parseToLocalDate(normalizeCompactIsoOffsetText(text, effectiveFormat)));
    }

    /**
     * Parses text into a {@code LocalTime}, preserving the civil time exactly as written.
     * Input without time fields is rejected.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToLocalTime("14:30:45");                        // returns 14:30:45
     * Dates.parseToLocalTime("2025-01-15 14:30:45");             // returns 14:30:45 (date part ignored)
     * Dates.parseToLocalTime("2025-01-15T14:30:45+05:30");       // returns 14:30:45 (textual fields, offset ignored)
     *
     * Dates.parseToLocalTime((String) null);                     // returns null
     * Dates.parseToLocalTime("null");                            // returns null (the formatTo null-token)
     * Dates.parseToLocalTime("");                                // throws IllegalArgumentException
     * Dates.parseToLocalTime("2025-01-15");                      // throws IllegalArgumentException (no time fields)
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @return the parsed {@code LocalTime}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         lacks time fields, or cannot be parsed.
     * @see #parseToLocalTime(String, String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static LocalTime parseToLocalTime(final String text) throws IllegalArgumentException {
        return parseToLocalTime(text, null);
    }

    /**
     * Parses text into a {@code LocalTime} using the specified format (or auto-detection when
     * {@code format} is {@code null}/empty). The civil time is taken exactly as written; any offset or
     * zone in the text is ignored for field extraction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToLocalTime("14:30:45", "HH:mm:ss");   // returns 14:30:45
     * Dates.parseToLocalTime("02:30 PM", "hh:mm a");    // returns 14:30 (locale-sensitive field, Locale.US)
     *
     * Dates.parseToLocalTime((String) null, "HH:mm:ss");   // returns null
     * Dates.parseToLocalTime("", "HH:mm:ss");              // throws IllegalArgumentException
     * Dates.parseToLocalTime("2:30", "HH:mm:ss");          // throws IllegalArgumentException (does not match the pattern)
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @param format a predefined format constant or a {@link DateTimeFormatter} pattern, or
     *        {@code null}/empty for auto-detection.
     * @return the parsed {@code LocalTime}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         lacks time fields, or cannot be parsed.
     * @see DTF#parseToLocalTime(CharSequence)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static LocalTime parseToLocalTime(final String text, final String format) throws IllegalArgumentException {
        if (isNullParseInput(text)) {
            return null;
        }

        rejectEmptyDateTime(text);
        final boolean autoDetected = Strings.isEmpty(format);
        final String effectiveFormat = requireDetectedFormat(text, format);
        checkCompleteCivilFormat(effectiveFormat, text, false, true);

        return parseReportingCallerFormat(text, format,
                () -> dtfForParsing(effectiveFormat, autoDetected).parseToLocalTime(normalizeCompactIsoOffsetText(text, effectiveFormat)));
    }

    /**
     * Parses text into a {@code LocalDateTime}, preserving the civil fields exactly as written.
     * Input without both complete date and time fields is rejected.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToLocalDateTime("2025-01-15 10:30:45");         // returns 2025-01-15T10:30:45
     * Dates.parseToLocalDateTime("2025-01-15T10:30:45");         // returns 2025-01-15T10:30:45
     * Dates.parseToLocalDateTime("2025-01-15T10:30:45Z");        // returns 2025-01-15T10:30:45 (textual fields, zone ignored)
     *
     * Dates.parseToLocalDateTime((String) null);                 // returns null
     * Dates.parseToLocalDateTime("null");                        // returns null (the formatTo null-token)
     * Dates.parseToLocalDateTime("");                            // throws IllegalArgumentException
     * Dates.parseToLocalDateTime("2025-01-15");                  // throws IllegalArgumentException (no time fields)
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @return the parsed {@code LocalDateTime}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         lacks complete date or time fields, or cannot be parsed.
     * @see #parseToLocalDateTime(String, String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static LocalDateTime parseToLocalDateTime(final String text) throws IllegalArgumentException {
        return parseToLocalDateTime(text, null);
    }

    /**
     * Parses text into a {@code LocalDateTime} using the specified format (or auto-detection when
     * {@code format} is {@code null}/empty). The civil fields are taken exactly as written; any offset
     * or zone in the text is ignored for field extraction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToLocalDateTime("15/01/2025 10:30", "dd/MM/yyyy HH:mm");   // returns 2025-01-15T10:30
     * Dates.parseToLocalDateTime("2025-01-15 10:30:45", null);              // returns 2025-01-15T10:30:45 (auto-detected)
     *
     * Dates.parseToLocalDateTime((String) null, "dd/MM/yyyy HH:mm");        // returns null
     * Dates.parseToLocalDateTime("", "dd/MM/yyyy HH:mm");                   // throws IllegalArgumentException
     * Dates.parseToLocalDateTime("15/01/2025", "dd/MM/yyyy HH:mm");         // throws IllegalArgumentException (no time in the text)
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @param format a predefined format constant or a {@link DateTimeFormatter} pattern, or
     *        {@code null}/empty for auto-detection.
     * @return the parsed {@code LocalDateTime}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         lacks complete date or time fields, or cannot be parsed.
     * @see DTF#parseToLocalDateTime(CharSequence)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static LocalDateTime parseToLocalDateTime(final String text, final String format) throws IllegalArgumentException {
        if (isNullParseInput(text)) {
            return null;
        }

        rejectEmptyDateTime(text);
        final boolean autoDetected = Strings.isEmpty(format);
        final String effectiveFormat = requireDetectedFormat(text, format);
        checkCompleteCivilFormat(effectiveFormat, text, true, true);

        return parseReportingCallerFormat(text, format,
                () -> dtfForParsing(effectiveFormat, autoDetected).parseToLocalDateTime(normalizeCompactIsoOffsetText(text, effectiveFormat)));
    }

    /**
     * Parses text into an {@code OffsetDateTime}. An offset written in the text is always preserved;
     * zone-less text is interpreted in the live default zone (or {@code timeZone} via
     * {@link #parseToOffsetDateTime(String, String, TimeZone)}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToOffsetDateTime("2025-01-15T10:30:45+05:30");   // returns 2025-01-15T10:30:45+05:30 (textual offset kept)
     * Dates.parseToOffsetDateTime("2025-01-15T10:30:45Z");        // returns 2025-01-15T10:30:45Z
     * Dates.parseToOffsetDateTime("2025-01-15 10:30:45");         // zone-less: offset comes from the live default zone
     *
     * Dates.parseToOffsetDateTime((String) null);                 // returns null
     * Dates.parseToOffsetDateTime("null");                        // returns null (the formatTo null-token)
     * Dates.parseToOffsetDateTime("");                            // throws IllegalArgumentException
     * Dates.parseToOffsetDateTime("14:30:45");                    // throws IllegalArgumentException (no complete local date)
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @return the parsed {@code OffsetDateTime}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         lacks a complete local date, or cannot be parsed.
     * @see #parseToZonedDateTime(String)
     * @see #parseToInstant(String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static OffsetDateTime parseToOffsetDateTime(final String text) throws IllegalArgumentException {
        return parseToOffsetDateTime(text, null, null);
    }

    /**
     * Parses text into an {@code OffsetDateTime} using the specified format (or auto-detection when
     * {@code format} is {@code null}/empty).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToOffsetDateTime("2025-01-15T10:30:45+05:30", Dates.ISO_OFFSET_DATE_TIME_FORMAT);
     *                                                                   // returns 2025-01-15T10:30:45+05:30
     * Dates.parseToOffsetDateTime("2025-01-15T10:30:45+05:30", null);    // returns 2025-01-15T10:30:45+05:30 (auto-detected)
     *
     * Dates.parseToOffsetDateTime((String) null, Dates.ISO_OFFSET_DATE_TIME_FORMAT);   // returns null
     * Dates.parseToOffsetDateTime("", Dates.ISO_OFFSET_DATE_TIME_FORMAT);              // throws IllegalArgumentException
     * Dates.parseToOffsetDateTime("bad", Dates.ISO_OFFSET_DATE_TIME_FORMAT);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @param format a predefined format constant or a java.time-compatible pattern, or
     *        {@code null}/empty for auto-detection.
     * @return the parsed {@code OffsetDateTime}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         lacks a complete local date, or cannot be parsed.
     * @see DTF#parseToOffsetDateTime(CharSequence)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static OffsetDateTime parseToOffsetDateTime(final String text, final String format) throws IllegalArgumentException {
        return parseToOffsetDateTime(text, format, null);
    }

    /**
     * Parses text into an {@code OffsetDateTime}, interpreting zone-less text in {@code timeZone}.
     * An offset written in the text always wins; the offset of a zone-less value is derived from the
     * supplied (or live default) zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
     * Dates.parseToOffsetDateTime("2025-01-15 10:30:45", null, kolkata);
     *                                                    // returns 2025-01-15T10:30:45+05:30 (zone-less text takes the supplied zone)
     * Dates.parseToOffsetDateTime("2025-01-15T10:30:45Z", null, null);
     *                                                    // returns 2025-01-15T10:30:45Z (textual offset wins over the zone)
     *
     * Dates.parseToOffsetDateTime((String) null, null, kolkata);   // returns null
     * Dates.parseToOffsetDateTime("", null, kolkata);              // throws IllegalArgumentException
     * Dates.parseToOffsetDateTime("14:30:45", null, kolkata);      // throws IllegalArgumentException (no complete local date)
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @param format a predefined format constant or a java.time-compatible pattern, or
     *        {@code null}/empty for auto-detection.
     * @param timeZone the zone to interpret zone-less text in, or {@code null} for the live default.
     * @return the parsed {@code OffsetDateTime}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         lacks a complete local date, cannot be parsed, or {@code timeZone} conflicts with a
     *         fixed UTC/GMT format.
     * @see DTF#parseToOffsetDateTime(CharSequence, TimeZone)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static OffsetDateTime parseToOffsetDateTime(final String text, final String format, final TimeZone timeZone) throws IllegalArgumentException {
        if (isNullParseInput(text)) {
            return null;
        }

        rejectEmptyDateTime(text);
        final String effectiveFormat = requireDetectedFormat(text, format);
        checkCompleteInstantFormat(effectiveFormat, text);

        return parseReportingCallerFormat(text, format, () -> dtfForParsing(effectiveFormat, Strings.isEmpty(format))
                .parseToOffsetDateTime(normalizeCompactIsoOffsetText(text, effectiveFormat), fallbackZoneFor(format, effectiveFormat, timeZone)));
    }

    /**
     * Parses text into a {@code ZonedDateTime}. A zone or offset written in the text always wins (an
     * offset inconsistent with its bracketed zone is rejected); zone-less text is interpreted in the
     * live default zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToZonedDateTime("2025-01-15T10:30:45+05:30[Asia/Kolkata]");
     *                                              // returns 2025-01-15T10:30:45+05:30[Asia/Kolkata] (region ID kept)
     * Dates.parseToZonedDateTime("2025-01-15T10:30:45Z");   // returns 2025-01-15T10:30:45Z[UTC]
     * Dates.parseToZonedDateTime("2025-01-15 10:30:45");    // zone-less: resolved in the live default zone
     *
     * Dates.parseToZonedDateTime((String) null);            // returns null
     * Dates.parseToZonedDateTime("null");                   // returns null (the formatTo null-token)
     * Dates.parseToZonedDateTime("");                       // throws IllegalArgumentException
     * Dates.parseToZonedDateTime("2025-01-15T10:30:45+01:00[Asia/Kolkata]");
     *                                              // throws IllegalArgumentException (offset contradicts the region)
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @return the parsed {@code ZonedDateTime}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         lacks a complete local date, or cannot be parsed.
     * @see #parseToZonedDateTime(String, String, TimeZone)
     * @see #parseToInstant(String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static ZonedDateTime parseToZonedDateTime(final String text) throws IllegalArgumentException {
        return parseToZonedDateTime(text, null, null);
    }

    /**
     * Parses text into a {@code ZonedDateTime} using the specified format (or auto-detection when
     * {@code format} is {@code null}/empty).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToZonedDateTime("2025-01-15T10:30:45+05:30[Asia/Kolkata]", Dates.ISO_ZONED_DATE_TIME_FORMAT);
     *                                              // returns 2025-01-15T10:30:45+05:30[Asia/Kolkata]
     * Dates.parseToZonedDateTime("2025-01-15T10:30:45Z", null);   // returns 2025-01-15T10:30:45Z[UTC] (auto-detected)
     *
     * Dates.parseToZonedDateTime((String) null, Dates.ISO_ZONED_DATE_TIME_FORMAT);   // returns null
     * Dates.parseToZonedDateTime("", Dates.ISO_ZONED_DATE_TIME_FORMAT);              // throws IllegalArgumentException
     * Dates.parseToZonedDateTime("bad", Dates.ISO_ZONED_DATE_TIME_FORMAT);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @param format a predefined format constant or a java.time-compatible pattern, or
     *        {@code null}/empty for auto-detection.
     * @return the parsed {@code ZonedDateTime}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         lacks a complete local date, or cannot be parsed.
     * @see DTF#parseToZonedDateTime(CharSequence)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static ZonedDateTime parseToZonedDateTime(final String text, final String format) throws IllegalArgumentException {
        return parseToZonedDateTime(text, format, null);
    }

    /**
     * Parses text into a {@code ZonedDateTime}, interpreting zone-less text in {@code timeZone}.
     * Zone provenance for the result: the textual region ID, then the textual offset, then the
     * supplied (or live default) zone. DST gaps and unresolved overlaps are rejected.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
     * Dates.parseToZonedDateTime("2025-01-15 10:30:45", null, kolkata);
     *                                              // returns 2025-01-15T10:30:45+05:30[Asia/Kolkata]
     * Dates.parseToZonedDateTime("2025-01-15T10:30:45Z", null, null);
     *                                              // returns 2025-01-15T10:30:45Z[UTC] (textual zone wins)
     *
     * Dates.parseToZonedDateTime((String) null, null, kolkata);   // returns null
     * Dates.parseToZonedDateTime("", null, kolkata);              // throws IllegalArgumentException
     * // 2025-03-09 02:30 does not exist in America/Los_Angeles (spring-forward gap)
     * Dates.parseToZonedDateTime("2025-03-09 02:30:00", null, TimeZone.getTimeZone("America/Los_Angeles"));
     *                                              // throws IllegalArgumentException
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @param format a predefined format constant or a java.time-compatible pattern, or
     *        {@code null}/empty for auto-detection.
     * @param timeZone the zone to interpret zone-less text in, or {@code null} for the live default.
     * @return the parsed {@code ZonedDateTime}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         lacks a complete local date, cannot be parsed, or {@code timeZone} conflicts with a
     *         fixed UTC/GMT format.
     * @see DTF#parseToZonedDateTime(CharSequence, TimeZone)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static ZonedDateTime parseToZonedDateTime(final String text, final String format, final TimeZone timeZone) throws IllegalArgumentException {
        if (isNullParseInput(text)) {
            return null;
        }

        rejectEmptyDateTime(text);
        final String effectiveFormat = requireDetectedFormat(text, format);
        checkCompleteInstantFormat(effectiveFormat, text);

        return parseReportingCallerFormat(text, format, () -> dtfForParsing(effectiveFormat, Strings.isEmpty(format))
                .parseToZonedDateTime(normalizeCompactIsoOffsetText(text, effectiveFormat), fallbackZoneFor(format, effectiveFormat, timeZone)));
    }

    /**
     * Parses text into an {@code Instant}. A zone or offset written in the text always wins; zone-less
     * text is interpreted in the live default zone (or the supplied zone in the overloads).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToInstant("2025-01-15T10:30:45Z");          // returns 2025-01-15T10:30:45Z
     * Dates.parseToInstant("2025-01-15T10:30:45+05:30");     // returns 2025-01-15T05:00:45Z (shifted by the textual offset)
     * Dates.parseToInstant("2025-01-15 10:30:45");           // zone-less: resolved in the live default zone
     *
     * Dates.parseToInstant((String) null);                   // returns null
     * Dates.parseToInstant("null");                          // returns null (the formatTo null-token)
     * Dates.parseToInstant("");                              // throws IllegalArgumentException
     * Dates.parseToInstant("1736937045000");                 // throws IllegalArgumentException (use parseEpochMillisToInstant)
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @return the parsed {@code Instant}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         lacks a complete local date, or cannot be parsed.
     * @see #parseToInstant(String, String, TimeZone)
     * @see #parseEpochMillis(String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Instant parseToInstant(final String text) throws IllegalArgumentException {
        return parseToInstant(text, null, null);
    }

    /**
     * Parses text into an {@code Instant} using the specified format (or auto-detection when
     * {@code format} is {@code null}/empty).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseToInstant("2025-01-15T10:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT);   // returns 2025-01-15T10:30:45Z
     * Dates.parseToInstant("2025-01-15T10:30:45+05:30", null);                         // returns 2025-01-15T05:00:45Z (auto-detected)
     *
     * Dates.parseToInstant((String) null, Dates.ISO_8601_DATE_TIME_FORMAT);   // returns null
     * Dates.parseToInstant("", Dates.ISO_8601_DATE_TIME_FORMAT);              // throws IllegalArgumentException
     * Dates.parseToInstant("bad", Dates.ISO_8601_DATE_TIME_FORMAT);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @param format a predefined format constant or a java.time-compatible pattern, or
     *        {@code null}/empty for auto-detection.
     * @return the parsed {@code Instant}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         lacks a complete local date, or cannot be parsed.
     * @see DTF#parseToInstant(CharSequence)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Instant parseToInstant(final String text, final String format) throws IllegalArgumentException {
        return parseToInstant(text, format, null);
    }

    /**
     * Parses text into an {@code Instant}, interpreting zone-less text in {@code timeZone}. A zone or
     * offset written in the text always wins. DST gaps and unresolved overlaps are rejected.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
     * Dates.parseToInstant("2025-01-15 10:30:45", null, kolkata);         // returns 2025-01-15T05:00:45Z
     * Dates.parseToInstant("2025-01-15T10:30:45+02:00", null, kolkata);   // returns 2025-01-15T08:30:45Z (textual offset wins)
     * Dates.parseToInstant("2025-01-15T10:30:45Z", null, null);           // returns 2025-01-15T10:30:45Z
     *
     * Dates.parseToInstant((String) null, null, kolkata);                 // returns null
     * Dates.parseToInstant("", null, kolkata);                            // throws IllegalArgumentException
     * Dates.parseToInstant("14:30:45", null, kolkata);                    // throws IllegalArgumentException (no complete local date)
     * Dates.parseToInstant("2025-01-15T10:30:45Z", null, kolkata);        // returns 2025-01-15T10:30:45Z (the textual 'Z' wins, kolkata unused)
     * Dates.parseToInstant("2025-01-15T10:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT, kolkata);
     *                                                                     // throws IllegalArgumentException (a fixed-UTC constant was chosen)
     * }</pre>
     *
     * @param text the text to parse, or {@code null}.
     * @param format a predefined format constant or a java.time-compatible pattern, or
     *        {@code null}/empty for auto-detection.
     * @param timeZone the zone to interpret zone-less text in, or {@code null} for the live default.
     * @return the parsed {@code Instant}, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
     * @throws IllegalArgumentException if the text is empty, ambiguous numeric text,
     *         lacks a complete local date, cannot be parsed, or {@code timeZone} conflicts with an
     *         explicitly supplied fixed UTC/GMT format (auto-detected text never conflicts: a
     *         {@code Z} or {@code GMT} it carries wins over {@code timeZone}).
     * @see DTF#parseToInstant(CharSequence, TimeZone)
     * @see #parseEpochMillis(String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static Instant parseToInstant(final String text, final String format, final TimeZone timeZone) throws IllegalArgumentException {
        if (isNullParseInput(text)) {
            return null;
        }

        rejectEmptyDateTime(text);
        final String effectiveFormat = requireDetectedFormat(text, format);
        checkCompleteInstantFormat(effectiveFormat, text);

        return parseReportingCallerFormat(text, format, () -> dtfForParsing(effectiveFormat, Strings.isEmpty(format))
                .parseToInstant(normalizeCompactIsoOffsetText(text, effectiveFormat), fallbackZoneFor(format, effectiveFormat, timeZone)));
    }

    /** Creates the proleptic Gregorian result promised by the legacy parsing contract. */
    private static GregorianCalendar createParsedGregorianCalendar(final long millis, final TimeZone timeZone, final Locale locale) {
        final GregorianCalendar result = newProlepticGregorianCalendar(timeZone == null ? TimeZone.getDefault() : timeZone, locale);
        result.setTimeInMillis(millis);
        return result;
    }

    /**
     * Selects the zone carried by a Calendar-like parse result. A zone or offset in the text is part
     * of the value and wins; {@code fallbackZone} is used only for zone-less text. This is deliberately
     * best-effort for arbitrary legacy patterns: the instant ({@code millis}) has already been parsed
     * successfully by the authoritative legacy path, so failure to query a custom pattern's zone must not
     * re-parse the value under subtly different java.time rules, and a zone the query does recover is
     * kept only where it shows the wall clock the text spells - see
     * {@link #zoneReproducingParsedInstant(String, TemporalAccessor, ZoneId, long, TimeZone, Locale, int, AppliedZoneOffset)}.
     */
    private static TimeZone calendarResultTimeZone(final String text, final String format, final long millis, final TimeZone fallbackZone, final Locale locale,
            final AppliedZoneOffset applied) {
        final String effectiveFormat = checkDateFormat(text, format);
        final int zoneStart = text.lastIndexOf('[');

        // A trailing [region] is zone syntax only for the ISO-zoned grammar. Arbitrary legacy
        // patterns may contain the same characters as quoted literals (for example, '[UTC]').
        if ((Strings.isEmpty(format) || ISO_ZONED_DATE_TIME_FORMAT.equals(effectiveFormat)) && zoneStart > 10 && text.endsWith("]")) {
            try {
                return TimeZone.getTimeZone(ZoneId.of(text.substring(zoneStart + 1, text.length() - 1)));
            } catch (final DateTimeException | IllegalArgumentException e) {
                // The authoritative parser will already have rejected an invalid standard zoned value.
            }
        }

        if (HTTP_DATE_FORMAT.equals(effectiveFormat)) {
            return (TimeZone) GMT_TIME_ZONE.clone();
        }

        if (ISO_8601_DATE_TIME_FORMAT.equals(effectiveFormat) || ISO_8601_TIMESTAMP_FORMAT.equals(effectiveFormat)
                || (Strings.isEmpty(format) && text.endsWith("Z"))) {
            return (TimeZone) UTC_TIME_ZONE.clone();
        }

        final ZoneOffset isoOffset = isoOffsetFromText(text);

        if (isoOffset != null && (Strings.isEmpty(format) || ISO_OFFSET_DATE_TIME_FORMAT.equals(effectiveFormat)
                || ISO_OFFSET_TIMESTAMP_FORMAT.equals(effectiveFormat) || ISO_ZONED_DATE_TIME_FORMAT.equals(effectiveFormat))) {
            return TimeZone.getTimeZone(isoOffset);
        }

        if (Strings.isNotEmpty(format)) {
            // The text was parsed by SimpleDateFormat; java.time re-reads it only to recover the zone. A spelling the
            // two grammars read differently would make that re-read fail and silently keep the fallback zone, whose
            // civil fields are then not the text's, so the zone letters are rewritten first (collapseZoneLetterRuns):
            // SimpleDateFormat reads a zone NAME, "GMT+05:30", "GMT" and "UTC" under 'Z' and an RFC 822 offset under
            // 'z' alike, where java.time binds each letter to one grammar. java.time's full-name trie also holds the
            // zone IDs, so "NZ" matches the front of "NZDT" when long names are tried before short ones: a failure
            // is retried with the short names first.
            for (int attempt = 0; attempt < 4; attempt++) {
                // Attempts: long names first with ASCII digits, then short names first, then each with the locale's
                // digits - the legacy engine reads and writes a custom pattern's numbers in those.
                final boolean shortNamesFirst = (attempt & 1) != 0;
                final boolean localeDigits = attempt >= 2;

                try {
                    final String javaTimePattern = collapseZoneLetterRuns(effectiveFormat, shortNamesFirst);
                    // Case-insensitive and lenient, as SimpleDateFormat is: it accepted "aest", "pacific standard time",
                    // a full month or weekday name under MMM/EEE and a short one under MMMM/EEEE, and a fraction of any
                    // width under S. A spelling the java.time grammar still cannot re-read is handled by the last
                    // resort below.
                    final DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().parseCaseInsensitive()
                            .parseLenient()
                            .appendPattern(javaTimePattern);

                    if (DTF.containsYearOfEra(javaTimePattern) && !DTF.containsProlepticYear(javaTimePattern)) {
                        builder.parseDefaulting(ChronoField.ERA, IsoEra.CE.getValue());
                    }

                    final TemporalAccessor parsed = builder.toFormatter(locale)
                            .withResolverStyle(ResolverStyle.STRICT)
                            .withDecimalStyle(localeDigits ? DecimalStyle.of(locale) : DecimalStyle.STANDARD)
                            .parse(text);
                    ZoneId parsedZone = parsed.query(TemporalQueries.zone());

                    if (parsedZone == null && parsed.isSupported(ChronoField.OFFSET_SECONDS)) {
                        parsedZone = ZoneOffset.ofTotalSeconds(parsed.get(ChronoField.OFFSET_SECONDS));
                    }

                    if (parsedZone != null) {
                        return zoneReproducingParsedInstant(text, parsed, parsedZone, millis, fallbackZone, locale, shortYearRun(javaTimePattern), applied);
                    }

                    break; // re-read, and the text carries no zone: the fallback zone is the zone
                } catch (final DateTimeException | IllegalArgumentException e) {
                    // Retry with the other name order or digit shapes; after all four, the last resort decides.
                }
            }
        }

        return lastResortZone(millis, fallbackZone, applied);
    }

    /**
     * The zone of a Calendar-family result whose text {@code java.time} could not re-read, or that carries no zone:
     * the fallback zone when the offset {@code SimpleDateFormat} applied is the fallback's own at that instant
     * (zone-less text, or a name that resolved to the fallback), else a fixed-offset zone carrying that offset - so
     * the calendar shows the wall clock the text spells even for a spelling only {@code SimpleDateFormat}
     * understands (locale digits, padded pattern letters, {@code GMT+5:30}, its {@code u} and {@code F} letters,
     * extra whitespace, a zone letter run into a digit field); only the region's identity is lost. Before this
     * existed such text kept the fallback zone, whose fields were not the text's.
     */
    private static TimeZone lastResortZone(final long millis, final TimeZone fallbackZone, final AppliedZoneOffset applied) {
        if (applied != null && applied.known && applied.millis != fallbackZone.getOffset(millis) && applied.millis % 1000 == 0
                && Math.abs(applied.millis) <= MAX_ZONE_OFFSET_SECONDS * 1000L) {
            return TimeZone.getTimeZone(ZoneOffset.ofTotalSeconds(applied.millis / 1000));
        }

        return (TimeZone) fallbackZone.clone();
    }

    /** The widest offset {@link ZoneOffset} admits, in seconds. */
    private static final int MAX_ZONE_OFFSET_SECONDS = 18 * 60 * 60;

    /**
     * The zone a Calendar-family result carries for text whose zone {@code java.time} re-parsed as
     * {@code parsedZone}, chosen so that the calendar shows the wall clock the text spells.
     *
     * <p>The instant came from {@code SimpleDateFormat}; the re-parse only recovers the zone. For a numeric
     * offset or a fixed-offset zone ({@code -0800}, {@code GMT+05:30}, {@code UTC}) the two engines cannot
     * disagree. For a zone <i>name</i> they routinely do: {@code SimpleDateFormat} matches the name against
     * the fallback zone's own names first and then the JDK's zone table, and forces the standard or
     * daylight offset the name denotes, while {@code java.time} maps the same abbreviation to a
     * CLDR-preferred region ({@code AEST} to Australia/Sydney, {@code IST} to Africa/Abidjan, {@code CST}
     * to America/Chicago). Handing the calendar the re-parsed region therefore showed 21:30 for text that
     * said {@code 20:30:45 AEST} with an Australia/Brisbane fallback, and the previous day for
     * {@code 10:30:45 CST} with Asia/Shanghai. So a region is accepted only when its offset at the instant
     * reproduces the wall clock the text spells; the fallback zone is tried first when the text names it,
     * which is {@code SimpleDateFormat}'s own order; a wall clock the region skips is accepted the way the
     * rest of this class resolves it; and when no zone reproduces it - a standard-time name written for a
     * date the zone spends on daylight time, say - the result carries the offset the text implies as a
     * fixed-offset zone, so its fields are still the text's.</p>
     */
    private static TimeZone zoneReproducingParsedInstant(final String text, final TemporalAccessor parsed, final ZoneId parsedZone, final long millis,
            final TimeZone fallbackZone, final Locale locale, final int shortYearRun, final AppliedZoneOffset applied) {
        final TimeZone region = TimeZone.getTimeZone(parsedZone);
        final LocalDate localDate = parsed.query(TemporalQueries.localDate());
        final LocalTime localTime = parsed.query(TemporalQueries.localTime());
        LocalDateTime local = localDate == null ? null : LocalDateTime.of(localDate, localTime == null ? LocalTime.MIDNIGHT : localTime);
        final long impliedSeconds;

        if (applied != null && applied.known && applied.millis % 1000 == 0) {
            // What SimpleDateFormat applied IS the wall clock's distance from the instant: no re-read field (a
            // two-digit year read in another century, a fraction or a week-date read differently) can put it off,
            // and a pattern without a complete date can still be checked.
            impliedSeconds = applied.millis / 1000;
            local = LocalDateTime.ofEpochSecond(Math.floorDiv(millis, 1000L) + impliedSeconds, 0, ZoneOffset.UTC);
        } else if (local == null) {
            // A pattern without a complete date leaves nothing to check the zone against.
            return region;
        } else {
            if (shortYearRun == 2 || (shortYearRun == 1 && local.getYear() >= 0 && local.getYear() <= 99)) {
                // SimpleDateFormat reads a two-digit year - "yy", or "y" given exactly two digits - inside a window
                // of 80 years back and 20 years forward from today, java.time "yy" within 2000-2099 and "y"
                // literally: "75" was 1975 to the instant and 2075 (or 75) to the re-read.
                local = twoDigitYearAsSimpleDateFormat(local);
            }

            // In whole seconds: offsets are, and the engines can read a sub-second field differently ('S').
            impliedSeconds = local.toEpochSecond(ZoneOffset.UTC) - Math.floorDiv(millis, 1000L);
        }

        if (parsedZone.getRules().isFixedOffset() && parsedZone.getRules().getOffset(Instant.EPOCH).getTotalSeconds() == impliedSeconds) {
            // A numeric offset or a fixed-offset zone reproduces the wall clock unless the engines split the text
            // differently (a zone letter directly followed by a digit field): then the checks below decide.
            return region;
        }

        if (Math.abs(impliedSeconds) > MAX_ZONE_OFFSET_SECONDS) {
            // Only reachable without the applied offset: the engines read some field differently and the wall
            // clock cannot be recovered, so keep the zone the text names.
            return region;
        }

        final int impliedMillis = (int) impliedSeconds * 1000;
        final boolean textNamesFallback = containsDisplayName(text, fallbackZone, locale);
        final TimeZone first = textNamesFallback ? fallbackZone : region;
        final TimeZone second = textNamesFallback ? region : fallbackZone;

        if (first.getOffset(millis) == impliedMillis) {
            return first == region ? region : (TimeZone) fallbackZone.clone();
        }

        if (second.getOffset(millis) == impliedMillis) {
            return second == region ? region : (TimeZone) fallbackZone.clone();
        }

        // A wall clock the region skips ("02:30 EST" on the spring-forward day): the region resolves it
        // forward to the same instant, exactly as set*/add* and the parsers do everywhere else.
        if (ZonedDateTime.ofLocal(local.withNano(0), parsedZone, null).toEpochSecond() == Math.floorDiv(millis, 1000L)) {
            return region;
        }

        return TimeZone.getTimeZone(ZoneOffset.ofTotalSeconds((int) impliedSeconds));
    }

    /**
     * The length, 1 or 2, of the first unquoted run of at most two {@code y} in {@code pattern} - the letters
     * {@code SimpleDateFormat} reads as a two-digit year when the text supplies exactly two digits - or 0.
     */
    private static int shortYearRun(final String pattern) {
        boolean inQuote = false;

        for (int i = 0, len = pattern.length(); i < len; i++) {
            final char ch = pattern.charAt(i);

            if (ch == '\'') {
                inQuote = !inQuote;
            } else if (!inQuote && ch == 'y' && (i == 0 || pattern.charAt(i - 1) != 'y')) {
                int end = i + 1;

                while (end < len && pattern.charAt(end) == 'y') {
                    end++;
                }

                if (end - i <= 2) {
                    return end - i;
                }

                i = end - 1;
            }
        }

        return 0;
    }

    /**
     * {@code reParsed} with its two-digit year moved into {@code SimpleDateFormat}'s default window: the hundred
     * years starting 80 years before now, compared on the full date-time as {@code SimpleDateFormat} compares it
     * (against the moment the formatter was created; a pooled formatter can be older than this call by minutes).
     */
    private static LocalDateTime twoDigitYearAsSimpleDateFormat(final LocalDateTime reParsed) {
        final LocalDateTime windowStart = LocalDateTime.now().minusYears(80);
        final int year = windowStart.getYear() + Math.floorMod(reParsed.getYear() - windowStart.getYear(), 100);
        final LocalDateTime candidate = reParsed.withYear(year);

        return candidate.isBefore(windowStart) ? candidate.plusYears(100) : candidate;
    }

    /** Whether {@code text} contains one of {@code zone}'s four display names, ignoring case. */
    private static boolean containsDisplayName(final String text, final TimeZone zone, final Locale locale) {
        for (final int style : new int[] { TimeZone.SHORT, TimeZone.LONG }) {
            if (Strings.containsIgnoreCase(text, zone.getDisplayName(false, style, locale))
                    || Strings.containsIgnoreCase(text, zone.getDisplayName(true, style, locale))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Rewrites a legacy ({@code SimpleDateFormat}) pattern for the java.time re-parse that recovers a zone from
     * the text: every unquoted run of {@code Z} or {@code z}, whatever its length, becomes
     * {@code [Z][zzzz][z]} (or {@code [Z][z][zzzz]} when {@code shortNamesFirst}). {@code SimpleDateFormat} parses
     * both letters with one grammar and ignores the run's length: an RFC 822 offset ({@code -0800}), a short or a
     * long zone name ({@code PST}, {@code Pacific Standard Time}), {@code GMT+05:30}, {@code GMT} and {@code UTC}
     * are all accepted under either letter. {@code DateTimeFormatter} binds each letter and length to one grammar
     * (one to three {@code Z} an RFC 822 offset, four the localized {@code GMT-08:00}, five {@code -08:00}; one to
     * three {@code z} a short name, four a long one), so a pattern passed through as it is failed the re-parse for
     * a name under {@code Z} or an offset under {@code z} and silently kept the fallback zone. The offset comes
     * first because a name grammar would consume the {@code -08} of {@code -0800}; the two name orders exist
     * because the full-name trie also holds zone IDs ({@code NZ} matches the front of {@code NZDT}) while the
     * short one holds names that prefix long ones ({@code Japan}).
     *
     * <p>Runs {@code SimpleDateFormat} merely pads but {@code DateTimeFormatterBuilder} rejects ("Too many pattern
     * letters": {@code aa}, {@code HHH}, {@code mmm}, {@code sss}, {@code ddd}, {@code DDDD}, {@code FF},
     * {@code GGGGGG}, {@code MMMMMM}, {@code EEEEEE}, ten or more {@code S}) are cut to the longest run java.time
     * reads with the same meaning; in lenient mode the width does not constrain the text.
     */
    private static String collapseZoneLetterRuns(final String pattern, final boolean shortNamesFirst) {
        final String zoneGrammar = shortNamesFirst ? "[Z][z][zzzz]" : "[Z][zzzz][z]";
        final StringBuilder sb = new StringBuilder(pattern.length() + 16);
        boolean inQuote = false;

        for (int i = 0, len = pattern.length(); i < len; i++) {
            final char ch = pattern.charAt(i);

            if (ch == '\'') {
                inQuote = !inQuote;
                sb.append(ch);
            } else if (!inQuote && (ch == 'Z' || ch == 'z')) {
                int end = i + 1;

                while (end < len && pattern.charAt(end) == ch) {
                    end++;
                }

                sb.append(zoneGrammar);
                i = end - 1;
            } else if (!inQuote && longestJavaTimeRun(ch) > 0) {
                int end = i + 1;

                while (end < len && pattern.charAt(end) == ch) {
                    end++;
                }

                for (int n = Math.min(end - i, longestJavaTimeRun(ch)); n > 0; n--) {
                    sb.append(ch);
                }

                i = end - 1;
            } else {
                sb.append(ch);
            }
        }

        return sb.toString();
    }

    /** The longest run of a pattern letter {@code DateTimeFormatterBuilder} accepts with SimpleDateFormat's meaning, or 0 to leave it alone. */
    private static int longestJavaTimeRun(final char letter) {
        switch (letter) {
            case 'a':
            case 'F':
                return 1;
            case 'd':
            case 'H':
            case 'h':
            case 'k':
            case 'K':
            case 'm':
            case 's':
                return 2;
            case 'D':
                return 3;
            case 'G':
            case 'M':
            case 'L':
            case 'E':
                return 4;
            case 'S':
                return 9;
            default:
                return 0;
        }
    }

    /** Extracts an ISO numeric offset after the date-time portion, or {@code null} when absent/invalid. */
    private static ZoneOffset isoOffsetFromText(final String text) {
        final int bracket = text.lastIndexOf('[');
        final int end = bracket > 10 ? bracket : text.length();
        final int plus = text.lastIndexOf('+', end - 1);
        final int minus = text.lastIndexOf('-', end - 1);
        final int offsetStart = Math.max(plus, minus);

        if (offsetStart <= 10) {
            return null;
        }

        String offset = text.substring(offsetStart, end);

        if (offset.length() == 5 && offset.charAt(3) != ':') {
            offset = offset.substring(0, 3) + ':' + offset.substring(3);
        }

        try {
            return ZoneOffset.of(offset);
        } catch (final DateTimeException e) {
            return null;
        }
    }

    private static boolean isPossibleLong(final CharSequence dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return false;
        }

        final int fromIndex = (dateTime.charAt(0) == '-' || dateTime.charAt(0) == '+') ? 1 : 0;

        if (fromIndex == dateTime.length()) {
            return false;
        }

        for (int i = fromIndex, len = dateTime.length(); i < len; i++) {
            final char ch = dateTime.charAt(i);

            if (ch < '0' || ch > '9') {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether the specified string is in the JDBC timestamp escape format: either
     * {@code "yyyy-MM-dd HH:mm:ss"} or that base followed by 1 to 9 fractional-second digits.
     *
     * @param str the candidate string; never {@code null}.
     * @return {@code true} if {@code str} matches the JDBC timestamp escape format, optionally with a fractional second.
     */
    private static boolean isJdbcTimestampString(final String str) {
        final int len = str.length();

        if (len != 19 && (len < 21 || len > 29)) {
            return false;
        }

        if (str.charAt(4) != '-' || str.charAt(7) != '-' || str.charAt(10) != ' ' || str.charAt(13) != ':' || str.charAt(16) != ':'
                || (len > 19 && str.charAt(19) != '.')) {
            return false;
        }

        return isAllDigits(str, 0, 4) && isAllDigits(str, 5, 7) && isAllDigits(str, 8, 10) && isAllDigits(str, 11, 13) && isAllDigits(str, 14, 16)
                && isAllDigits(str, 17, 19) && (len == 19 || isAllDigits(str, 20, len));
    }

    private static boolean isAllDigits(final String str, final int fromIndex, final int toIndex) {
        if (fromIndex >= toIndex) {
            return false;
        }

        for (int i = fromIndex; i < toIndex; i++) {
            final char ch = str.charAt(i);

            if (ch < '0' || ch > '9') {
                return false;
            }
        }

        return true;
    }

    /**
     * Parses {@code str} with {@code sdf} and requires the entire input to match the pattern, so
     * trailing whitespace or garbage is not silently truncated into a plausible date.
     * @throws ParseException if the text cannot be parsed completely, including any trailing characters.
     */
    private static java.util.Date parseFully(final DateFormat sdf, final String str) throws ParseException {
        final ParsePosition pos = new ParsePosition(0);
        final java.util.Date result = sdf.parse(str, pos);

        if (result == null) {
            throw new ParseException("Unparseable date: \"" + str + "\"", pos.getErrorIndex() < 0 ? pos.getIndex() : pos.getErrorIndex());
        }

        if (pos.getIndex() != str.length()) {
            throw new ParseException("Unparseable date \"" + str + "\": unexpected trailing characters at index " + pos.getIndex(), pos.getIndex());
        }

        return result;
    }

    /**
     * Parses signed decimal text as epoch milliseconds. This is the explicit entry point for epoch
     * input &mdash; bare numeric text is deliberately rejected as ambiguous by the other
     * {@code parseTo*} methods.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseEpochMillis("1736937045000");   // returns 1736937045000
     * Dates.parseEpochMillis("-1");              // returns -1 (before the epoch)
     * Dates.parseEpochMillis("2023-01-01");      // throws IllegalArgumentException (not numeric)
     *
     * Dates.parseEpochMillis((String) null);     // returns 0
     * Dates.parseEpochMillis("");                // throws IllegalArgumentException
     * Dates.parseEpochMillis("null");            // returns 0 (the literal string "null")
     * }</pre>
     *
     * @param text the signed decimal epoch-millisecond text; may be {@code null}.
     * @return the epoch milliseconds. A {@code null} reference and the case-insensitive marker
     *         {@code "null"} return {@code 0} (the same value as the numeric text {@code "0"}).
     * @throws IllegalArgumentException if the text is empty, not a signed decimal value, or out of
     *         range for a {@code long}.
     * @see #parseEpochMillisToInstant(String)
     * @see Instant#ofEpochMilli(long)
     */
    public static long parseEpochMillis(final String text) throws IllegalArgumentException {
        if (isNullParseInput(text)) {
            return 0L;
        }

        rejectEmptyDateTime(text);

        if (!isPossibleLong(text)) {
            throw new IllegalArgumentException("Not a signed decimal epoch-millisecond value: \"" + text + "\"");
        }

        try {
            return Long.parseLong(text);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Numeric date-time string is out of range for epoch milliseconds: \"" + text + "\"", e);
        }
    }

    /**
     * Parses signed decimal epoch-millisecond text into an {@code Instant}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseEpochMillisToInstant("1736937045000");   // returns 2025-01-15T10:30:45Z
     * Dates.parseEpochMillisToInstant("0");               // returns 1970-01-01T00:00:00Z (the epoch)
     * Dates.parseEpochMillisToInstant("-1");              // returns 1969-12-31T23:59:59.999Z (before the epoch)
     *
     * Dates.parseEpochMillisToInstant((String) null);     // returns 1970-01-01T00:00:00Z
     * Dates.parseEpochMillisToInstant("null");            // returns 1970-01-01T00:00:00Z (the literal string "null")
     * Dates.parseEpochMillisToInstant("");                // throws IllegalArgumentException
     * Dates.parseEpochMillisToInstant("2025-01-15");      // throws IllegalArgumentException (not numeric)
     * }</pre>
     *
     * @param text the signed decimal epoch-millisecond text; may be {@code null}.
     * @return the {@code Instant}. A {@code null} reference and the case-insensitive marker
     *         {@code "null"} return {@link Instant#EPOCH} (the same value as the numeric text {@code "0"}).
     * @throws IllegalArgumentException if the text is empty or not a valid epoch-millisecond value.
     * @see #parseEpochMillis(String)
     * @see #dateAt(Instant, ZoneId)
     */
    public static Instant parseEpochMillisToInstant(final String text) throws IllegalArgumentException {
        return Instant.ofEpochMilli(parseEpochMillis(text));
    }

    /**
     * Converts an instant to the civil date in the specified zone. This is the explicit, zoned
     * epoch-to-civil conversion; the reverse of {@code LocalDate.atStartOfDay(zone)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Instant instant = Instant.ofEpochMilli(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * Dates.dateAt(instant, ZoneOffset.UTC);                    // returns 2025-01-15
     * Dates.dateAt(instant, ZoneId.of("Asia/Kolkata"));         // returns 2025-01-15 (16:00 local, same day)
     *
     * // the zone decides the day: 2025-01-15T00:00Z is still 2025-01-14 in Los Angeles
     * Dates.dateAt(Instant.ofEpochMilli(1736899200000L), ZoneId.of("America/Los_Angeles"));   // returns 2025-01-14
     * Dates.dateAt(null, ZoneOffset.UTC);                                                     // throws IllegalArgumentException
     * Dates.dateAt(instant, (ZoneId) null);                                                   // throws IllegalArgumentException
     * }</pre>
     *
     * @param instant the instant; must not be {@code null}.
     * @param zone the zone in which the civil date is taken; must not be {@code null}.
     * @return the civil {@code LocalDate} of {@code instant} in {@code zone}.
     * @throws IllegalArgumentException if {@code instant} or {@code zone} is {@code null}.
     * @throws DateTimeException if the instant lies outside the range the civil type can represent (near
     *         {@link Instant#MIN} or {@link Instant#MAX}).
     * @see #timeAt(Instant, ZoneId)
     * @see #dateTimeAt(Instant, ZoneId)
     */
    public static LocalDate dateAt(final Instant instant, final ZoneId zone) throws IllegalArgumentException, DateTimeException {
        N.checkArgNotNull(instant, cs.instant);
        N.checkArgNotNull(zone, cs.zone);

        return instant.atZone(zone).toLocalDate();
    }

    /**
     * Converts an instant to the civil time in the specified zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Instant instant = Instant.ofEpochMilli(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * Dates.timeAt(instant, ZoneOffset.UTC);                    // returns 10:30:45
     * Dates.timeAt(instant, ZoneId.of("Asia/Kolkata"));         // returns 16:00:45 (+05:30)
     *
     * Dates.timeAt(Instant.ofEpochMilli(0L), ZoneOffset.UTC);   // returns 00:00 (LocalTime hides zero seconds)
     * Dates.timeAt(null, ZoneOffset.UTC);                       // throws IllegalArgumentException
     * Dates.timeAt(instant, (ZoneId) null);                     // throws IllegalArgumentException
     * }</pre>
     *
     * @param instant the instant; must not be {@code null}.
     * @param zone the zone in which the civil time is taken; must not be {@code null}.
     * @return the civil {@code LocalTime} of {@code instant} in {@code zone}.
     * @throws IllegalArgumentException if {@code instant} or {@code zone} is {@code null}.
     * @throws DateTimeException if the instant lies outside the range the civil type can represent (near
     *         {@link Instant#MIN} or {@link Instant#MAX}).
     * @see #dateAt(Instant, ZoneId)
     * @see #dateTimeAt(Instant, ZoneId)
     */
    public static LocalTime timeAt(final Instant instant, final ZoneId zone) throws IllegalArgumentException, DateTimeException {
        N.checkArgNotNull(instant, cs.instant);
        N.checkArgNotNull(zone, cs.zone);

        return instant.atZone(zone).toLocalTime();
    }

    /**
     * Converts an instant to the civil date and time in the specified zone. This is the combined form of
     * {@link #dateAt(Instant, ZoneId)} and {@link #timeAt(Instant, ZoneId)}, and the reverse of
     * {@code localDateTime.atZone(zone).toInstant()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Instant instant = Instant.ofEpochMilli(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * Dates.dateTimeAt(instant, ZoneOffset.UTC);                // returns 2025-01-15T10:30:45
     * Dates.dateTimeAt(instant, ZoneId.of("Asia/Kolkata"));     // returns 2025-01-15T16:00:45
     *
     * // the zone decides the day as well as the time
     * Dates.dateTimeAt(Instant.ofEpochMilli(1736899200000L), ZoneId.of("America/Los_Angeles"));
     *                                                           // returns 2025-01-14T16:00
     * Dates.dateTimeAt(null, ZoneOffset.UTC);                    // throws IllegalArgumentException
     * Dates.dateTimeAt(instant, (ZoneId) null);                  // throws IllegalArgumentException
     * }</pre>
     *
     * @param instant the instant; must not be {@code null}.
     * @param zone the zone in which the civil date and time are taken; must not be {@code null}.
     * @return the civil {@code LocalDateTime} of {@code instant} in {@code zone}.
     * @throws IllegalArgumentException if {@code instant} or {@code zone} is {@code null}.
     * @throws DateTimeException if the instant lies outside the range the civil type can represent (near
     *         {@link Instant#MIN} or {@link Instant#MAX}).
     * @see #dateAt(Instant, ZoneId)
     * @see #timeAt(Instant, ZoneId)
     */
    public static LocalDateTime dateTimeAt(final Instant instant, final ZoneId zone) throws IllegalArgumentException, DateTimeException {
        N.checkArgNotNull(instant, cs.instant);
        N.checkArgNotNull(zone, cs.zone);

        return instant.atZone(zone).toLocalDateTime();
    }

    private static java.util.Date parseISO8601(final String dateTime, final String formatToUse, final String explicitFormat, final TimeZone timeZone,
            final boolean zoneIsDefaultSnapshot) {
        final TimeZone effectiveTimeZone = checkTimeZone(dateTime, formatToUse, timeZone, zoneIsDefaultSnapshot, Strings.isEmpty(explicitFormat));

        if (Strings.isNotEmpty(explicitFormat) && (ISO_8601_DATE_TIME_FORMAT.equals(formatToUse) || ISO_8601_TIMESTAMP_FORMAT.equals(formatToUse))) {
            final DateFormat sdf = getSDF(formatToUse, UTC_TIME_ZONE);

            try {
                return parseFully(sdf, dateTime);
            } catch (final ParseException e) {
                throw parseFailure(dateTime, formatToUse, UTC_TIME_ZONE, e);
            } finally {
                recycleSDF(formatToUse, UTC_TIME_ZONE, sdf);
            }
        }

        try {
            return java.util.Date.from(ISO8601Util.parseInstantWithDefaultZone(dateTime, () -> toZoneId(effectiveTimeZone)));
        } catch (final RuntimeException e) {
            throw parseFailure(dateTime, formatToUse, effectiveTimeZone, e);
        }
    }

    static long parse(final String dateTime, final String format, final TimeZone timezone, final Locale locale) {
        return parse(dateTime, format, timezone, locale, false);
    }

    /**
     * @param zoneIsDefaultSnapshot {@code true} when {@code timezone} is the caller's captured live
     *        default (single-snapshot contract), not an explicitly supplied zone: fixed UTC/GMT
     *        conflict checks are skipped, and the snapshot is used as-is instead of re-reading
     *        {@link TimeZone#getDefault()}.
     */
    private static long parse(final String dateTime, final String format, final TimeZone timezone, final Locale locale, final boolean zoneIsDefaultSnapshot) {
        return parse(dateTime, format, timezone, locale, zoneIsDefaultSnapshot, null);
    }

    /**
     * The zone offset {@code SimpleDateFormat} applied to a legacy parse - the raw offset of the zone it settled on
     * plus the daylight offset the text's zone name or numeric offset dictated - which is exactly the difference
     * between the wall clock the text spells and the instant it produced, whatever the spelling. Read from the
     * formatter's own calendar right after the parse; the Calendar-family targets use it to check, and as the last
     * resort to replace, the zone the java.time re-read recovers (see {@link #calendarResultTimeZone}).
     */
    private static final class AppliedZoneOffset {
        private int millis;
        private boolean known;
    }

    /**
     * @param appliedOffset receives the offset {@code SimpleDateFormat} applied when the text went through it;
     *        {@code null} when the caller does not need it
     * @throws IllegalArgumentException if {@code locale} is {@code null}, or the text, format, or time-zone combination cannot be resolved as a valid unambiguous instant.
     */
    private static long parse(final String dateTime, final String format, final TimeZone timezone, final Locale locale, final boolean zoneIsDefaultSnapshot,
            final AppliedZoneOffset appliedOffset) throws IllegalArgumentException {
        N.checkArgNotNull(locale, cs.locale);

        if (Strings.isEmpty(format) && isPossibleLong(dateTime)) {
            // Bare numeric text is ambiguous (epoch milliseconds vs. a numeric date such as a year):
            // fail loudly instead of silently choosing epoch semantics. Use parseEpochMillis for epoch input.
            throw new IllegalArgumentException("Ambiguous numeric date/time text; supply an explicit format or use parseEpochMillis: \"" + dateTime + "\"");
        }

        String formatToUse = checkDateFormat(dateTime, format);
        boolean extendedIsoAutoDetected = false;

        if (Strings.isEmpty(format) && Strings.isEmpty(formatToUse)) {
            formatToUse = detectExtendedIsoFormat(dateTime);
            extendedIsoAutoDetected = Strings.isNotEmpty(formatToUse);
        }

        checkFixedFourDigitYearText(dateTime, formatToUse);
        checkFixedWidthLegacyText(dateTime, formatToUse);

        if (!extendedIsoAutoDetected) {
            checkIsoOffsetText(dateTime, formatToUse);
        }

        // The named .SSS constants mean exactly three fraction digits when supplied explicitly;
        // SimpleDateFormat would otherwise read ".5" as 5 milliseconds and accept any digit count.
        if (Strings.isNotEmpty(format)) {
            checkNamedTimestampFraction(dateTime, formatToUse);
        }

        // ISO region IDs (VV) are not SimpleDateFormat syntax. Extended auto-detected offset forms
        // (fractional seconds or offset seconds) likewise need DateTimeFormatter's ISO grammar.
        if (ISO_ZONED_DATE_TIME_FORMAT.equals(formatToUse) || (extendedIsoAutoDetected && ISO_OFFSET_DATE_TIME_FORMAT.equals(formatToUse))) {
            try {
                return dtfForParsing(formatToUse, extendedIsoAutoDetected).parseToInstant(normalizeCompactIsoOffsetText(dateTime, formatToUse), timezone)
                        .toEpochMilli();
            } catch (final DateTimeException | IllegalArgumentException e) {
                // The caller's own format, as the two auto-detected branches below do. The
                // extendedIsoAutoDetected half of the condition above only fires on an empty format, and
                // naming the detected grammar there told the caller they had passed a pattern they never
                // wrote. For an explicitly supplied format the two are the same string: checkDateFormat
                // returns the caller's format verbatim when it is not empty.
                throw parseFailure(dateTime, format, timezone, e);
            }
        }

        // Auto-detected JDBC escape text (space separator, Timestamp.toString() output) carries a 1-9
        // digit fraction of a second; route it through the shared JDBC grammar so every target agrees.
        if (Strings.isEmpty(format) && LOCAL_TIMESTAMP_FORMAT.equals(formatToUse) && dateTime.length() > 19) {
            final TimeZone effectiveZone = timezone == null ? TimeZone.getDefault() : (TimeZone) timezone.clone();

            try {
                return parseJdbcTimestamp(dateTime, effectiveZone).getTime();
            } catch (final DateTimeException | IllegalArgumentException e) {
                // The caller's own format, not the detected one: this branch only runs when the format was
                // empty, and naming the detected grammar told them they had passed a pattern they never
                // wrote. parseAutoTimestampToInstant already reports it this way.
                throw parseFailure(dateTime, format, effectiveZone, e);
            }
        }

        // Auto-detected T-separated timestamp text with a non-three-digit fraction gets java.time
        // fraction-of-second semantics, not SimpleDateFormat's millisecond-count reading.
        if (Strings.isEmpty(format) && ISO_LOCAL_TIMESTAMP_FORMAT.equals(formatToUse) && dateTime.length() != 23) {
            final TimeZone effectiveZone = timezone == null ? TimeZone.getDefault() : (TimeZone) timezone.clone();

            try {
                final LocalDateTime localDateTime = dtfForParsing(formatToUse, true).parseToLocalDateTime(dateTime);
                return resolveLocalMillis(localDateTime.withNano(0), effectiveZone) + localDateTime.getNano() / 1_000_000;
            } catch (final DateTimeException | IllegalArgumentException e) {
                // As above: an empty format must be reported as "the auto-detected format".
                throw parseFailure(dateTime, format, effectiveZone, e);
            }
        }

        // use ISO8601Util.parseInstant for better performance. ISO_OFFSET_TIMESTAMP_FORMAT takes the same route as
        // its fraction-less sibling: SimpleDateFormat's non-lenient GregorianCalendar limits ZONE_OFFSET to
        // -13:00..+14:00, which rejected the -18:00..+18:00 the constant documents and, before 1900, offsets the
        // constant's own format() had written (Pacific/Guam -14:21).
        if (Strings.isEmpty(formatToUse) || ISO_OFFSET_DATE_TIME_FORMAT.equals(formatToUse) || ISO_OFFSET_TIMESTAMP_FORMAT.equals(formatToUse)
                || ISO_8601_DATE_TIME_FORMAT.equals(formatToUse) || ISO_8601_TIMESTAMP_FORMAT.equals(formatToUse)) {
            return parseISO8601(dateTime, formatToUse, format, timezone, zoneIsDefaultSnapshot).getTime();
        }

        final TimeZone timeZoneToUse = checkTimeZone(dateTime, formatToUse, timezone, zoneIsDefaultSnapshot, Strings.isEmpty(format));

        // Before both resolvers, not after: a nonexistent or ambiguous wall time gets the precise
        // diagnostic naming the local date-time, instead of a bare "Unparseable date" from
        // SimpleDateFormat or a generic failure from the strict java.time resolver below.
        checkGapAndOverlap(dateTime, formatToUse, timeZoneToUse);

        final long timeInMillis = fastDateParse(dateTime, formatToUse, timeZoneToUse);

        if (timeInMillis != Long.MIN_VALUE) {
            return timeInMillis;
        }

        final DateFormat sdf = getSDF(formatToUse, timeZoneToUse, locale);

        try {
            // SimpleDateFormat's XXX accepts only the extended +HH:mm offset, while the predefined ISO
            // offset constants also accept the basic +HHmm form (checkIsoOffsetText already validated it).
            final java.util.Date parsed = parseFully(sdf, normalizeCompactIsoOffsetText(dateTime, formatToUse));

            if (appliedOffset != null) {
                // Before the HTTP round trip below, which formats and so rewrites the calendar's fields.
                final Calendar parsedFields = sdf.getCalendar();
                appliedOffset.millis = parsedFields.get(Calendar.ZONE_OFFSET) + parsedFields.get(Calendar.DST_OFFSET);
                appliedOffset.known = true;
            }

            // SimpleDateFormat may accept a weekday that contradicts the date and may accept
            // non-canonical widths/casing. HTTP IMF-fixdate is a fixed 29-character grammar, so
            // round-trip the parsed instant through the same GMT formatter before accepting it.
            if (HTTP_DATE_FORMAT.equals(formatToUse) && (dateTime.length() != 29 || !dateTime.equals(sdf.format(parsed)))) {
                throw new ParseException("Non-canonical or contradictory HTTP-date: \"" + dateTime + "\"", 0);
            }

            return parsed.getTime();
        } catch (final ParseException e) {
            throw parseFailure(dateTime, formatToUse, timeZoneToUse, e);
        } finally {
            recycleSDF(formatToUse, timeZoneToUse, locale, sdf);
        }
    }

    /**
     * Enforces the exactly-three-fraction-digits grammar of the named {@code .SSS} timestamp constants
     * when one is supplied explicitly. Auto-detected text instead uses the JDBC escape grammar (1-9
     * fraction digits, fraction-of-second semantics); see {@link #parse(String, String, TimeZone, Locale)}.
     *
     * <p>The test covers the constant's whole canonical shape - the exact length, the {@code '.'} at index
     * 19, the three digits and the trailing {@code Z} - because
     * {@link #checkFixedWidthLegacyText(String, String)} checks only the head of these three constants and
     * opts out entirely for text that cannot put the fraction at index 19. The rejection message therefore
     * names that whole shape: reporting only the fraction width told a caller whose text carried exactly
     * three fraction digits that it had the wrong number of them.</p>
     */
    private static void checkNamedTimestampFraction(final String dateTime, final String formatToUse) {
        final boolean zulu = ISO_8601_TIMESTAMP_FORMAT.equals(formatToUse);
        final boolean local = LOCAL_TIMESTAMP_FORMAT.equals(formatToUse) || ISO_LOCAL_TIMESTAMP_FORMAT.equals(formatToUse);

        if (!zulu && !local) {
            return;
        }

        final boolean valid = dateTime.length() == (zulu ? 24 : 23) && dateTime.charAt(19) == '.' && isAllDigits(dateTime, 20, 23)
                && (!zulu || dateTime.charAt(23) == 'Z');

        if (!valid) {
            throw new IllegalArgumentException("Format '" + formatToUse + "' requires its exact canonical shape: " + (zulu ? 24 : 23)
                    + " characters, every field written as digits at its full width, exactly three fractional-second digits"
                    + (zulu ? " and a trailing 'Z'" : "") + ": \"" + dateTime + "\"; use a variable-width pattern such as \""
                    + variableWidthEquivalent(formatToUse) + "\" to accept shorter fields, or auto-detection (no format), which accepts"
                    + " the JDBC escape grammar with 1-9 fraction digits");
        }
    }

    /**
     * Rejects wall times in a DST gap or overlap for the standard zone-less local date and date-time
     * shapes ({@link #LOCAL_DATE_FORMAT}, {@link #LOCAL_DATE_TIME_FORMAT},
     * {@link #ISO_LOCAL_DATE_TIME_FORMAT}, {@link #LOCAL_TIMESTAMP_FORMAT},
     * {@link #ISO_LOCAL_TIMESTAMP_FORMAT}), matching the strict
     * resolution the JDBC and java.time paths apply. A date-only value is resolved at local midnight.
     * The configured
     * {@code SimpleDateFormat} is non-lenient and already rejects gaps, but silently selects one offset
     * in an overlap; this check also rejects that ambiguity, including historical overlaps in zones that
     * no longer observe daylight saving. Custom patterns remain non-lenient but do not receive the
     * explicit overlap check, as do zones whose rules cannot be represented faithfully as a {@link ZoneId}.
     */
    private static void checkGapAndOverlap(final String dateTime, final String formatToUse, final TimeZone timeZone) {
        final boolean dateOnly = LOCAL_DATE_FORMAT.equals(formatToUse);

        // ISO_LOCAL_TIMESTAMP_FORMAT belongs here with its space-separated twin LOCAL_TIMESTAMP_FORMAT.
        // Every other spelling of a zone-less local timestamp already reaches a strict resolver: the
        // 19-character forms via the two date-time constants, and any auto-detected fraction that is not
        // exactly three digits via the java.time branch in parse(). Only the T-separated 23-character
        // form fell through to SimpleDateFormat, which silently picks one offset in an overlap (and
        // reports a gap as a bare "Unparseable date" instead of naming the nonexistent local time).
        if (!(dateOnly || LOCAL_DATE_TIME_FORMAT.equals(formatToUse) || ISO_LOCAL_DATE_TIME_FORMAT.equals(formatToUse)
                || LOCAL_TIMESTAMP_FORMAT.equals(formatToUse) || ISO_LOCAL_TIMESTAMP_FORMAT.equals(formatToUse)) || dateTime.length() < (dateOnly ? 10 : 19)) {
            return;
        }

        // This check runs before the text has been parsed, so it must confirm the shape itself: reading
        // digits out of positions that hold something else would report malformed input as a DST gap.
        if (!hasExpectedGapCheckShape(dateTime, formatToUse, dateOnly)) {
            return;
        }

        final ZoneId zoneId;

        try {
            zoneId = toZoneId(timeZone);
        } catch (final IllegalArgumentException e) {
            return; // zone rules not representable as a ZoneId: keep non-lenient legacy parsing without an overlap check
        }

        if (zoneId.getRules().isFixedOffset()) {
            return;
        }

        final LocalDateTime localDateTime;

        try {
            localDateTime = LocalDateTime.of(parseInt(dateTime, 0, 4), parseInt(dateTime, 5, 7), parseInt(dateTime, 8, 10),
                    dateOnly ? 0 : parseInt(dateTime, 11, 13), dateOnly ? 0 : parseInt(dateTime, 14, 16), dateOnly ? 0 : parseInt(dateTime, 17, 19));
        } catch (final RuntimeException e) {
            return; // the parser already accepted the text; nothing more to validate
        }

        final List<ZoneOffset> validOffsets = zoneId.getRules().getValidOffsets(localDateTime);

        if (validOffsets.isEmpty()) {
            throw new IllegalArgumentException("Nonexistent local date-time " + localDateTime + " in zone " + zoneId + " (DST gap): \"" + dateTime + "\"");
        }

        if (validOffsets.size() > 1) {
            throw new IllegalArgumentException("Ambiguous local date-time " + localDateTime + " in zone " + zoneId + " (DST overlap): \"" + dateTime
                    + "\"; valid offsets are " + validOffsets);
        }
    }

    /**
     * Returns whether {@code dateTime} is exactly the zone-less shape {@code formatToUse} names, so
     * {@link #checkGapAndOverlap} can read its fields before the authoritative parser has validated
     * anything.
     *
     * <p>The <i>whole</i> text is matched, not just the fields the check reads: text that merely starts
     * like this shape (trailing garbage, a wrong fraction width) is a syntax error, and reporting it as
     * a nonexistent or ambiguous wall time would name the wrong problem. Every reachable value here has
     * one fixed width &mdash; the named {@code .SSS} constants are held to exactly three fraction digits
     * by {@link #checkNamedTimestampFraction}, and auto-detected text with any other fraction width has
     * already been routed to a strict java.time path by {@link #parse}.</p>
     */
    private static boolean hasExpectedGapCheckShape(final String dateTime, final String formatToUse, final boolean dateOnly) {
        final boolean withFraction = LOCAL_TIMESTAMP_FORMAT.equals(formatToUse) || ISO_LOCAL_TIMESTAMP_FORMAT.equals(formatToUse);

        if (dateTime.length() != (dateOnly ? 10 : (withFraction ? 23 : 19))) {
            return false;
        }

        if (dateTime.charAt(4) != '-' || dateTime.charAt(7) != '-' || !isAllDigits(dateTime, 0, 4) || !isAllDigits(dateTime, 5, 7)
                || !isAllDigits(dateTime, 8, 10)) {
            return false;
        }

        if (dateOnly) {
            return true;
        }

        // Both T-separated constants must select 'T' here. Adding a format to the guard list in
        // checkGapAndOverlap without adding it here would leave this looking for a space, silently
        // skipping the check for that format.
        final char separator = ISO_LOCAL_DATE_TIME_FORMAT.equals(formatToUse) || ISO_LOCAL_TIMESTAMP_FORMAT.equals(formatToUse) ? 'T' : ' ';

        return dateTime.charAt(10) == separator && dateTime.charAt(13) == ':' && dateTime.charAt(16) == ':' && isAllDigits(dateTime, 11, 13)
                && isAllDigits(dateTime, 14, 16) && isAllDigits(dateTime, 17, 19)
                && (!withFraction || (dateTime.charAt(19) == '.' && isAllDigits(dateTime, 20, 23)));
    }

    /**
     * Runs a {@link DTF}-backed parse and restates any failure in terms of the pattern the caller
     * actually passed.
     *
     * <p>{@link #dtfForParsing} maps the predefined constants onto {@code java.time} formatters whose
     * year field is the proleptic {@code uuuu}, so an unwrapped {@code DTF} failure named a pattern
     * ({@code 'uuuu-MM-dd'}) that the caller never wrote and cannot find anywhere in this class. Only
     * a genuine parse failure is restated - it is the one that carries the underlying
     * {@code DateTimeException}, whose message and error index are preserved as the cause.</p>
     */
    private static <R> R parseReportingCallerFormat(final String text, final String format, final Supplier<R> parser) {
        try {
            return parser.get();
        } catch (final IllegalArgumentException e) {
            if (e.getCause() instanceof DateTimeException) {
                throw parseFailure(text, format, null, (Exception) e.getCause());
            }

            if (e.getCause() instanceof IllegalArgumentException) {
                // Not a parse failure: DTF wraps the IllegalArgumentException toZoneId throws for a
                // fallback zone no ZoneId can express in the same "Cannot parse ..." message, which blamed
                // the input for a problem with the zone. That diagnosis already names the zone; report it
                // as is instead of restating it as a failure to parse text that was fine.
                throw (IllegalArgumentException) e.getCause();
            }

            throw e;
        }
    }

    /**
     * Rejects a predefined pattern that cannot supply the civil fields the target needs, so the caller
     * gets this class's own message instead of {@code java.time}'s internal "Unable to obtain LocalDate
     * from TemporalAccessor: {Year=2023},ISO of type java.time.format.Parsed". This mirrors
     * {@link #checkCompleteInstantFormat} on the instant-bearing side. Only the predefined constants
     * are checked; the fields of a custom pattern are the caller's own business and the formatter
     * reports what it could not obtain.
     */
    private static void checkCompleteCivilFormat(final String format, final String text, final boolean requiresDate, final boolean requiresTime) {
        final boolean partialDate = LOCAL_YEAR_FORMAT.equals(format) || LOCAL_MONTH_DAY_FORMAT.equals(format);

        if (requiresDate && (partialDate || LOCAL_TIME_FORMAT.equals(format))) {
            throw new IllegalArgumentException("Format '" + format + "' does not contain a complete local date, so no date can be taken from: \"" + text
                    + "\"; format a value with it instead, or parse with DTF.of(String) to read the civil fields it does carry");
        }

        if (requiresTime && (partialDate || LOCAL_DATE_FORMAT.equals(format))) {
            throw new IllegalArgumentException("Format '" + format + "' does not contain a time, so no time can be taken from: \"" + text
                    + "\"; use a pattern with time fields, or parseToLocalDate for the date alone");
        }
    }

    /**
     * Builds the uniform parse-failure exception for the legacy parse paths: an
     * {@link IllegalArgumentException} whose message carries the input, the effective format, and the
     * time zone, with the underlying parse error retained as the cause.
     */
    private static IllegalArgumentException parseFailure(final String dateTime, final String format, final TimeZone timeZone, final Exception cause) {
        return new IllegalArgumentException(
                "Cannot parse \"" + dateTime + "\" with " + (Strings.isEmpty(format) ? "the auto-detected format" : "format '" + format + "'")
                        + (timeZone == null ? "" : " in time zone " + timeZone.getID()) + ": " + cause.getMessage(),
                cause);
    }

    /**
     * The {@link #parseFailure} variant for a path that auto-detected a grammar which is not a
     * {@code SimpleDateFormat} pattern the caller could have written. It names the grammar as
     * <i>detected</i> rather than putting it in the "format '...'" slot, which reads as the caller's own
     * argument - the same reason the other auto-detected paths report "the auto-detected format".
     *
     * @param dateTime the text that could not be parsed
     * @param detectedGrammar a human-readable name for the grammar that was tried
     * @param timeZone the effective zone, or {@code null} to leave it out of the message
     * @param cause the underlying parse error, retained
     */
    private static IllegalArgumentException autoDetectedParseFailure(final String dateTime, final String detectedGrammar, final TimeZone timeZone,
            final Exception cause) {
        return new IllegalArgumentException("Cannot parse \"" + dateTime + "\" with the auto-detected format (" + detectedGrammar + ")"
                + (timeZone == null ? "" : " in time zone " + timeZone.getID()) + ": " + cause.getMessage(), cause);
    }

    /**
     * Formats the current local date using the format {@code yyyy-MM-dd}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String s = Dates.formatCurrentLocalDate();   // returns e.g. "2025-10-22" (today in the live default zone)
     * assert s.length() == 10;                     // returns true (yyyy-MM-dd is always 10 chars)
     * assert s.matches("\\d{4}-\\d{2}-\\d{2}");    // returns true (matches the yyyy-MM-dd shape)
     * assert s.charAt(4) == '-';                   // returns true
     * }</pre>
     *
     * @return a non-null string representation of the current date in {@code yyyy-MM-dd} format,
     *         rendered in the live machine default zone.
     * @see #formatCurrentLocalDateTime()
     * @see #formatCurrentDateTime()
     */
    public static String formatCurrentLocalDate() {
        return format(currentJUDate(), LOCAL_DATE_FORMAT);
    }

    /**
     * Formats the current local date and time using the format {@code yyyy-MM-dd HH:mm:ss}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String s = Dates.formatCurrentLocalDateTime();                   // returns e.g. "2025-10-22 14:30:45" (live default zone)
     * assert s.length() == 19;                                         // returns true (yyyy-MM-dd HH:mm:ss is always 19 chars)
     * assert s.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");   // returns true (matches the pattern shape)
     * assert s.charAt(10) == ' ';                                      // returns true (space between date and time)
     * }</pre>
     *
     * @return a non-null string representation of the current date and time in
     *         {@code yyyy-MM-dd HH:mm:ss} format, rendered in the live machine default zone.
     * @see #formatCurrentLocalDate()
     * @see #formatCurrentDateTime()
     */
    public static String formatCurrentLocalDateTime() {
        return format(currentJUDate(), LOCAL_DATE_TIME_FORMAT);
    }

    /**
     * Formats the current date and time using the ISO 8601 format {@code yyyy-MM-dd'T'HH:mm:ss'Z'}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String s = Dates.formatCurrentDateTime();                         // returns e.g. "2025-10-22T14:30:45Z" (now, in UTC)
     * assert s.length() == 20;                                          // returns true
     * assert s.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");   // returns true (ISO-8601 'Z' shape)
     * assert s.endsWith("Z");                                           // returns true (UTC designator)
     * }</pre>
     *
     * @return a {@code non-null} string representation of the current date and time in ISO 8601 format {@code yyyy-MM-dd'T'HH:mm:ss'Z'}, rendered in UTC.
     * @see #formatCurrentLocalDateTime()
     * @see #formatCurrentTimestamp()
     * @see #format(java.util.Date)
     */
    public static String formatCurrentDateTime() {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            fastDateFormat(sb, null, System.currentTimeMillis(), false);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Formats the current date and time including milliseconds using the ISO 8601 format {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String s = Dates.formatCurrentTimestamp();                                 // returns e.g. "2025-10-22T14:30:45.123Z" (now, in UTC, with millis)
     * assert s.length() == 24;                                                   // returns true
     * assert s.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");   // returns true (with .SSS millis)
     * assert s.endsWith("Z");                                                    // returns true (UTC designator)
     * }</pre>
     *
     * @return a {@code non-null} string representation of the current timestamp in ISO 8601 format {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}, rendered in UTC.
     * @see #formatCurrentDateTime()
     * @see #format(java.util.Date)
     */
    public static String formatCurrentTimestamp() {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            fastDateFormat(sb, null, System.currentTimeMillis(), true);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Formats the provided date using a default format that depends on the date type.
     * For {@code java.sql.Date}, {@link Time}, and {@link Timestamp} instances, the format
     * {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} is used so their epoch-millisecond value can round-trip.
     * For plain {@code java.util.Date} instances, the format {@code yyyy-MM-dd'T'HH:mm:ss'Z'} is used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // The default 'Z' format renders the instant in UTC, independent of the default time zone.
     * Dates.format(new java.util.Date(1736937045000L));       // returns "2025-01-15T10:30:45Z"
     * Dates.format(new java.sql.Timestamp(1736937045123L));   // returns "2025-01-15T10:30:45.123Z" (Timestamp adds millis)
     * Dates.format(new java.sql.Date(1736937045123L));        // returns "2025-01-15T10:30:45.123Z"
     * Dates.format(new java.sql.Time(1736937045123L));        // returns "2025-01-15T10:30:45.123Z"
     *
     * Dates.format(new java.util.Date(0L));                   // returns "1970-01-01T00:00:00Z"
     * Dates.format((java.util.Date) null);                    // returns null
     * }</pre>
     *
     * @param date the java.util.Date instance to be formatted; may be {@code null}.
     * @return a string representation of the date, or {@code null} if the date is {@code null}.
     * @throws IllegalArgumentException if a non-null value is outside the Common Era year range
     *         {@code 0001} through {@code 9999} required by the default format
     * @see #format(java.util.Date, String)
     * @see #format(java.util.Date, String, TimeZone)
     * @see #formatCurrentDateTime()
     * @see #formatCurrentTimestamp()
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static String format(final java.util.Date date) throws IllegalArgumentException {
        return format(date, null, null);
    }

    /**
     * Formats the provided date into a string representation using the specified format.
     * If {@code format} is {@code null} or empty, {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} is used for
     * {@code java.sql.Date}, {@link Time}, and {@link Timestamp};
     * {@code yyyy-MM-dd'T'HH:mm:ss'Z'} is used for plain {@code java.util.Date} instances.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = new java.util.Date(1736937045000L);   // the instant 2025-01-15T10:30:45Z
     * Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT);        // returns "2025-01-15T10:30:45Z" (UTC, format-independent of zone)
     * Dates.format(date, null);                                   // returns "2025-01-15T10:30:45Z" (default 'Z' format)
     * Dates.format(date, "");                                     // returns "2025-01-15T10:30:45Z" (empty uses the same default)
     *
     * Dates.format((java.util.Date) null, "yyyy-MM-dd");          // returns null
     * }</pre>
     *
     * <p>Except for the two predefined UTC constants and {@link #HTTP_DATE_FORMAT}, patterns render in
     * the live machine default zone. A quoted {@code 'Z'} in a custom pattern is plain
     * text and does not change that rule. Pass an explicit {@link TimeZone} for zone-stable output.</p>
     *
     * @param date the date to be formatted.
     * @param format the date format pattern; if {@code null} or empty, the default format depends on the date type.
     * @return a string representation of the date, or {@code null} if the date is {@code null}.
     * @throws IllegalArgumentException if the pattern is invalid or a predefined format cannot
     *         represent the value's year or effective UTC offset
     * @see #format(java.util.Date, String, TimeZone)
     * @see #parseToJUDate(String, String)
     * @see SimpleDateFormat
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static String format(final java.util.Date date, final String format) throws IllegalArgumentException {
        return format(date, format, null);
    }

    /**
     * Formats the provided date into a string representation using the specified format and time zone.
     * If no time zone is provided, a {@code null} or empty format means {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} for
     * {@code java.sql.Date}, {@link Time}, and {@link Timestamp}, and
     * {@code yyyy-MM-dd'T'HH:mm:ss'Z'} for plain {@code java.util.Date} instances (rendered in UTC).
     * With an explicit time zone, a {@code null} or empty format means the corresponding offset-bearing
     * default ({@link #ISO_OFFSET_TIMESTAMP_FORMAT} / {@link #ISO_OFFSET_DATE_TIME_FORMAT}) rendered in
     * that zone, so the text still identifies one instant. It parses back to the same epoch value at the
     * precision that type's default carries: milliseconds for {@code java.sql.Date}, {@link Time} and
     * {@link Timestamp}, whole seconds for a plain {@code java.util.Date} &mdash; the same precision its
     * UTC default has, so a plain {@code java.util.Date} loses any millisecond fraction either way. Pass
     * {@link #ISO_OFFSET_TIMESTAMP_FORMAT} explicitly to keep it. That default can only write a
     * whole-minute offset from -18:00 through +18:00; supply an explicit zone-less pattern such as
     * {@link #LOCAL_TIMESTAMP_FORMAT} for a zone outside that range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = new java.util.Date(1736937045000L);   // the instant 2025-01-15T10:30:45Z
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss", utc);           // returns "2025-01-15 10:30:45"
     * Dates.format(date, "yyyy-MM-dd", utc);                    // returns "2025-01-15"
     *
     * // a null format with an explicit zone writes that zone's offset, so the value round-trips
     * Dates.format(date, null, TimeZone.getTimeZone("Asia/Kolkata"));
     *                                                           // returns "2025-01-15T16:00:45+05:30"
     * Dates.format((java.util.Date) null, "yyyy-MM-dd", utc);   // returns null
     * }</pre>
     *
     * <p>Note: UTC output attaches only to the two predefined constants {@link #ISO_8601_DATE_TIME_FORMAT}
     * and {@link #ISO_8601_TIMESTAMP_FORMAT}: they fix the output to UTC, and passing a non-UTC
     * {@code timeZone} together with one of them throws an {@code IllegalArgumentException}; a
     * {@code null} or UTC-equivalent zone is accepted and resolved to UTC. A quoted {@code 'Z'} in any
     * other pattern is a plain literal with no zone meaning. This mirrors the
     * {@code parse*(String, String, TimeZone)} counterparts, which reject the same combination; see
     * {@link #parseToJUDate(String, String, TimeZone)}. (A {@code null} or empty format is exempt: the default
     * pattern selected for an explicit zone writes that zone's numeric offset and carries no
     * {@code 'Z'} literal.) {@link #HTTP_DATE_FORMAT} is independently fixed to GMT and has the same
     * UTC-equivalent-zone requirement.</p>
     *
     * @param date the date to be formatted.
     * @param format the date format pattern; if {@code null} or empty, the default format depends on the date type.
     * @param timeZone the time zone for formatting; if {@code null}, the default time zone is used with an
     *        explicit pattern, while a {@code null} or empty {@code format} instead renders in UTC;
     *        must be {@code null} or UTC-equivalent when the format is fixed to UTC/GMT
     *        ({@link #ISO_8601_DATE_TIME_FORMAT}, {@link #ISO_8601_TIMESTAMP_FORMAT}, or {@link #HTTP_DATE_FORMAT}).
     * @return a string representation of the date, or {@code null} if the date is {@code null}.
     * @throws IllegalArgumentException if the pattern is invalid, a fixed-zone format conflicts with
     *         {@code timeZone}, or a predefined format cannot represent the value's year or effective UTC offset
     * @see #format(java.util.Date, String)
     * @see #parseToJUDate(String, String, TimeZone)
     * @see SimpleDateFormat
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static String format(final java.util.Date date, final String format, final TimeZone timeZone) throws IllegalArgumentException {
        return format(date, format, timeZone, Locale.US);
    }

    /**
     * Formats a date with an explicit locale for locale-sensitive {@link SimpleDateFormat} fields.
     * Predefined machine-readable constants remain US/ASCII; HTTP dates always use protocol-required English.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = new java.util.Date(1736937045000L);   // the instant 2025-01-15T10:30:45Z
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.format(date, "dd MMM yyyy", utc, Locale.US);       // returns "15 Jan 2025"
     * Dates.format(date, "dd MMM yyyy", utc, Locale.FRENCH);   // returns "15 janv. 2025"
     * Dates.format(date, "EEEE", utc, Locale.FRENCH);          // returns "mercredi"
     *
     * Dates.format(date, "yyyy-MM-dd", utc, Locale.FRENCH);                 // returns "2025-01-15" (no locale-sensitive field)
     * Dates.format((java.util.Date) null, "dd MMM yyyy", utc, Locale.US);   // returns null
     * Dates.format(date, "dd MMM yyyy", utc, (Locale) null);                // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to format, possibly {@code null}
     * @param format the pattern, or {@code null} or empty for the type-dependent default
     * @param timeZone the output zone, or {@code null} for the documented default-zone policy
     * @param locale the locale for locale-sensitive pattern fields; must not be {@code null}
     * @return the formatted text, or {@code null} when {@code date} is {@code null}
     * @throws IllegalArgumentException if {@code locale} is {@code null}, the pattern is invalid, a
     *         fixed-zone format conflicts with {@code timeZone}, or a predefined format cannot represent
     *         the value's year or effective UTC offset
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static String format(final java.util.Date date, final String format, final TimeZone timeZone, final Locale locale) throws IllegalArgumentException {
        return formatDate(null, date, format, timeZone, locale);
    }

    /**
     * Formats the provided calendar as an ISO zoned date-time in the calendar's own time zone. The
     * default output contains seconds, a fractional part when milliseconds are non-zero, the offset,
     * and the zone ID. This preserves the calendar's epoch-millisecond value and registered time-zone
     * rules across {@code parseToCalendar(format(calendar))}.
     *
     * <p>This is {@link DateTimeFormatter#ISO_ZONED_DATE_TIME}'s form, which differs from
     * {@link #ISO_ZONED_DATE_TIME_FORMAT} in two ways: the fraction is written when non-zero (the constant
     * writes none), and a fixed-offset zone that is not a registered region ({@code SimpleTimeZone}, a
     * custom {@code TimeZone} subclass) writes its offset alone, {@code 2025-01-15T11:30:45+01:00}, where the
     * constant writes {@code 2025-01-15T11:30:45+01:00[+01:00]}. Both forms parse back with auto-detection.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(1736917245123L, TimeZone.getTimeZone("Asia/Kolkata"));
     * Dates.format(cal);   // returns "2025-01-15T10:30:45.123+05:30[Asia/Kolkata]"
     *
     * Dates.format((Calendar) null);                         // returns null
     * }</pre>
     *
     * @param calendar the calendar to be formatted.
     * @return a string representation of the calendar, or {@code null} if the calendar is {@code null}.
     * @throws IllegalArgumentException if a non-null value is outside the Common Era year range
     *         {@code 0001} through {@code 9999} required by the default format, or if the calendar's
     *         zone carries rules no {@link ZoneId} can express (the default renders through
     *         {@code java.time}; a pattern overload other than {@link #ISO_ZONED_DATE_TIME_FORMAT} keeps
     *         the legacy engine and accepts such a zone)
     * @see #format(Calendar, String)
     * @see #format(Calendar, String, TimeZone)
     * @see #format(java.util.Date)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static String format(final Calendar calendar) throws IllegalArgumentException {
        return format(calendar, null, null);
    }

    /**
     * Formats the provided calendar using the specified format. If {@code format} is {@code null} or empty,
     * the timezone- and millisecond-preserving ISO zoned default described by {@link #format(Calendar)}
     * is used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(1736937045000L, TimeZone.getTimeZone("UTC"));
     * Dates.format(cal, Dates.ISO_8601_DATE_TIME_FORMAT);    // returns "2025-01-15T10:30:45Z"
     * Dates.format(cal, null);                               // returns "2025-01-15T10:30:45Z[UTC]"
     *
     * Dates.format((Calendar) null, "yyyy-MM-dd");           // returns null
     * }</pre>
     *
     * <p>Except for the two predefined UTC constants and {@link #HTTP_DATE_FORMAT}, patterns render in
     * the calendar's own time zone (matching {@code DTF.format(Calendar)}); pass an explicit-zone
     * overload to override it. A quoted
     * {@code 'Z'} in a custom pattern is plain text.</p>
     *
     * @param calendar the calendar to be formatted.
     * @param format the date format pattern; if {@code null} or empty, the default format is used.
     * @return a string representation of the calendar, or {@code null} if the calendar is {@code null}.
     * @throws IllegalArgumentException if the pattern is invalid, a predefined format cannot
     *         represent the value's year or effective UTC offset, or the calendar's zone carries rules no
     *         {@link ZoneId} can express and the format is the ISO zoned default or
     *         {@link #ISO_ZONED_DATE_TIME_FORMAT}
     * @see #format(Calendar)
     * @see #format(Calendar, String, TimeZone)
     * @see #parseToCalendar(String, String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static String format(final Calendar calendar, final String format) throws IllegalArgumentException {
        return format(calendar, format, null);
    }

    /**
     * Formats the provided calendar using the specified format and time zone. A {@code null} or empty format
     * uses the ISO zoned default; {@code timeZone} overrides the calendar's own zone when supplied,
     * and otherwise the calendar's zone is retained.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(1736937045000L);   // calendar at the 2025-01-15T10:30:45Z instant (default zone)
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);      // returns "2025-01-15 10:30:45"
     * Dates.format(cal, "yyyy-MM-dd", utc);               // returns "2025-01-15"
     *
     * Dates.format((Calendar) null, "yyyy-MM-dd", utc);   // returns null
     * }</pre>
     *
     * <p>Note: UTC output attaches only to the two predefined constants {@link #ISO_8601_DATE_TIME_FORMAT}
     * and {@link #ISO_8601_TIMESTAMP_FORMAT}. Passing a non-UTC {@code timeZone} together with one of
     * them throws an {@code IllegalArgumentException}; a {@code null} or UTC-equivalent zone is accepted
     * and resolved to UTC. A quoted {@code 'Z'} in any other pattern is a plain literal with no zone
     * meaning. The ISO zoned default selected by a {@code null} or empty format writes the effective zone explicitly.
     * {@link #HTTP_DATE_FORMAT} is independently fixed to GMT and has the same
     * UTC-equivalent-zone requirement.</p>
     *
     * @param calendar the calendar to be formatted.
     * @param format the date format pattern; if {@code null} or empty, the default format is used.
     * @param timeZone the time zone for formatting; if {@code null}, the calendar's own time zone is used
     *        (matching {@code DTF.format(Calendar)}); an explicit zone must be UTC-equivalent when the
     *        format is fixed to UTC/GMT
     *        ({@link #ISO_8601_DATE_TIME_FORMAT}, {@link #ISO_8601_TIMESTAMP_FORMAT}, or {@link #HTTP_DATE_FORMAT}).
     * @return a string representation of the calendar, or {@code null} if the calendar is {@code null}.
     * @throws IllegalArgumentException if the pattern is invalid, a fixed-zone format conflicts with
     *         {@code timeZone}, a predefined format cannot represent the value's year or effective UTC
     *         offset, or the effective zone carries rules no {@link ZoneId} can express and the format is
     *         the ISO zoned default or {@link #ISO_ZONED_DATE_TIME_FORMAT}
     * @see #format(Calendar, String)
     * @see #format(Calendar)
     * @see #parseToCalendar(String, String, TimeZone)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static String format(final Calendar calendar, final String format, final TimeZone timeZone) throws IllegalArgumentException {
        return format(calendar, format, timeZone, Locale.US);
    }

    /**
     * Formats a calendar with an explicit locale for locale-sensitive legacy pattern fields.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);   // the instant 2025-01-15T10:30:45Z
     * Dates.format(cal, "dd MMM yyyy", utc, Locale.US);           // returns "15 Jan 2025"
     * Dates.format(cal, "dd MMM yyyy", utc, Locale.GERMAN);       // returns "15 Jan. 2025"
     *
     * Dates.format(cal, "yyyy-MM-dd", utc, Locale.GERMAN);            // returns "2025-01-15" (no locale-sensitive field)
     * Dates.format((Calendar) null, "dd MMM yyyy", utc, Locale.US);   // returns null
     * Dates.format(cal, "dd MMM yyyy", utc, (Locale) null);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the calendar to format, possibly {@code null}
     * @param format the pattern, or {@code null} or empty for the default
     * @param timeZone the output zone, or {@code null} to use the calendar's own zone
     * @param locale the locale for locale-sensitive pattern fields; must not be {@code null}
     * @return the formatted text, or {@code null} when {@code calendar} is {@code null}
     * @throws IllegalArgumentException if {@code locale} is {@code null}, the pattern is invalid, a
     *         fixed-zone format conflicts with {@code timeZone}, a predefined format cannot represent
     *         the value's year or effective UTC offset, or the effective zone carries rules no
     *         {@link ZoneId} can express and the format is the ISO zoned default or
     *         {@link #ISO_ZONED_DATE_TIME_FORMAT}
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static String format(final Calendar calendar, final String format, final TimeZone timeZone, final Locale locale) throws IllegalArgumentException {
        N.checkArgNotNull(locale, cs.locale);

        if (calendar == null) {
            return null;
        }

        if (Strings.isEmpty(format)) {
            return formatCalendarDefault(calendar, timeZone, null);
        }

        // Calendar overloads honor the calendar's own zone when no zone is supplied, matching
        // DTF.format(Calendar) and Apache Commons Lang DateFormatUtils. Zone-fixed formats (the two
        // ISO-8601 UTC constants, HTTP-date) keep their fixed zone instead of throwing a conflict.
        final TimeZone outputZone = timeZone == null && !isZoneFixedFormat(format) ? calendar.getTimeZone() : timeZone;

        return format(createJUDate(calendar), format, outputZone, locale);
    }

    /**
     * Formats the provided XMLGregorianCalendar using its XML Schema lexical representation. The
     * value's civil fields, fractional-second scale, and numeric timezone are retained exactly; an
     * undefined XML timezone remains absent. XML Schema calendars carry an offset but no region ID.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(
     *         1736917245123L, TimeZone.getTimeZone("Asia/Kolkata"));
     * Dates.format(cal);   // returns "2025-01-15T10:30:45.123+05:30"
     *
     * Dates.format((XMLGregorianCalendar) null);                                     // returns null
     * }</pre>
     *
     * @param calendar the XMLGregorianCalendar instance to be formatted; may be {@code null}.
     * @return a string representation of the XMLGregorianCalendar instance, or {@code null} if the calendar is {@code null}.
     * @throws IllegalStateException if XML lexical formatting is selected and the fields do not form a valid XML Schema built-in date/time type
     * @see #format(Calendar)
     * @see #format(XMLGregorianCalendar, String)
     * @see #format(XMLGregorianCalendar, String, TimeZone)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static String format(final XMLGregorianCalendar calendar) throws IllegalStateException {
        return format(calendar, null, null);
    }

    /**
     * Formats the provided XMLGregorianCalendar using the specified format. A {@code null} or empty format
     * uses its XML Schema lexical representation, as described by
     * {@link #format(XMLGregorianCalendar)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1736937045000L, TimeZone.getTimeZone("UTC"));
     * Dates.format(cal, Dates.ISO_8601_DATE_TIME_FORMAT);                            // returns "2025-01-15T10:30:45Z"
     * Dates.format(cal, null);                                                       // returns "2025-01-15T10:30:45.000Z"
     *
     * Dates.format((XMLGregorianCalendar) null, "yyyy-MM-dd");                       // returns null
     * }</pre>
     *
     * @param calendar the XMLGregorianCalendar instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null} or empty, the default format is used.
     * @return a string representation of the XMLGregorianCalendar instance, or {@code null} if the calendar is {@code null}.
     * @throws IllegalArgumentException if the pattern is invalid or a predefined format cannot
     *         represent the value's year or effective UTC offset
     * @throws IllegalStateException if XML lexical formatting is selected and the fields do not form a valid XML Schema built-in date/time type
     * @see #format(XMLGregorianCalendar)
     * @see #format(XMLGregorianCalendar, String, TimeZone)
     * @see #format(Calendar, String)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static String format(final XMLGregorianCalendar calendar, final String format) throws IllegalArgumentException, IllegalStateException {
        return format(calendar, format, null);
    }

    /**
     * Formats the provided XMLGregorianCalendar using the specified format and time zone. With a
     * {@code null} or empty format and no zone override, its XML lexical representation is returned unchanged.
     * With a {@code null} or empty format and an explicit zone, the represented instant is rendered as an ISO
     * offset date-time in that zone. Because XML Schema has no region-ID field, only the effective
     * numeric offset is written. Fractional seconds are preserved exactly through nanosecond
     * precision; a finer fraction or leap second is rejected because {@link Instant} cannot represent
     * it exactly for zone conversion. With no override, the XML lexical default remains lossless for
     * those values. With a pattern, the value is read through {@link XMLGregorianCalendar#toGregorianCalendar()},
     * which rolls a leap second over into the next minute ({@code 23:59:60} prints as {@code 00:00:00} of the
     * next day) rather than rejecting it. If the XML timezone is undefined, its civil fields are interpreted in the live
     * default timezone before conversion to the requested output zone. The override's effective offset
     * must be a whole-minute XML Schema value from -14:00 through +14:00.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1736937045000L);   // the instant 2025-01-15T10:30:45Z
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);                  // returns "2025-01-15 10:30:45"
     * Dates.format(cal, "yyyy-MM-dd", utc);                           // returns "2025-01-15"
     *
     * Dates.format((XMLGregorianCalendar) null, "yyyy-MM-dd", utc);   // returns null
     * }</pre>
     *
     * <p>Note: UTC output attaches only to the two predefined constants {@link #ISO_8601_DATE_TIME_FORMAT}
     * and {@link #ISO_8601_TIMESTAMP_FORMAT}. Passing a non-UTC {@code timeZone} together with one of
     * them throws an {@code IllegalArgumentException}; a {@code null} or UTC-equivalent zone is accepted
     * and resolved to UTC. A quoted {@code 'Z'} in any other pattern is a plain literal with no zone
     * meaning. The default selected by a {@code null} or empty format writes the XML value's own offset, or the effective numeric
     * offset of an explicit override zone. {@link #HTTP_DATE_FORMAT} is independently fixed to GMT and
     * has the same UTC-equivalent-zone requirement.</p>
     *
     * @param calendar the XMLGregorianCalendar instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null} or empty, the default format is used.
     * @param timeZone the output-zone override; if {@code null} and {@code format} is null or empty, the XML
     *        calendar's own timezone is retained, for a custom pattern as well as for the lexical default
     *        (the live default time zone when the XML timezone field is undefined);
     *        must be {@code null} or UTC-equivalent when the format is fixed to UTC/GMT
     *        ({@link #ISO_8601_DATE_TIME_FORMAT}, {@link #ISO_8601_TIMESTAMP_FORMAT}, or {@link #HTTP_DATE_FORMAT}).
     * @return a string representation of the XMLGregorianCalendar instance, or {@code null} if the calendar is {@code null}.
     * @throws IllegalArgumentException if the pattern is invalid, a fixed-zone format conflicts with
     *         {@code timeZone}, a predefined format cannot represent the value's year or effective UTC
     *         offset, or default-format zone conversion cannot represent its effective XML timezone, a
     *         leap second, or a fraction finer than nanoseconds exactly
     * @throws IllegalStateException if XML lexical formatting is selected and the fields do not form a valid XML Schema built-in date/time type
     * @see #format(XMLGregorianCalendar)
     * @see #format(XMLGregorianCalendar, String)
     * @see #format(Calendar, String, TimeZone)
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static String format(final XMLGregorianCalendar calendar, final String format, final TimeZone timeZone)
            throws IllegalArgumentException, IllegalStateException {
        return format(calendar, format, timeZone, Locale.US);
    }

    /**
     * Formats an XML Gregorian calendar with an explicit locale for locale-sensitive legacy pattern fields.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1736937045000L, utc);
     * Dates.format(cal, "dd MMM yyyy", utc, Locale.US);       // returns "15 Jan 2025"
     * Dates.format(cal, "dd MMM yyyy", utc, Locale.GERMAN);   // returns "15 Jan. 2025"
     *
     * Dates.format(cal, "yyyy-MM-dd", utc, Locale.GERMAN);                        // returns "2025-01-15" (no locale-sensitive field)
     * Dates.format((XMLGregorianCalendar) null, "dd MMM yyyy", utc, Locale.US);   // returns null
     * Dates.format(cal, "dd MMM yyyy", utc, (Locale) null);                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the XML calendar to format, possibly {@code null}
     * @param format the pattern, or {@code null} or empty for the XML lexical default
     * @param timeZone the output-zone override, or {@code null} to retain the XML timezone
     * @param locale the locale for locale-sensitive pattern fields; must not be {@code null}
     * @return the formatted text, or {@code null} when {@code calendar} is {@code null}
     * @throws IllegalArgumentException if {@code locale} is {@code null}, the pattern is invalid, a
     *         fixed-zone format conflicts with {@code timeZone}, a predefined format cannot represent
     *         the value's year or effective UTC offset, or default-format zone conversion cannot represent
     *         its effective XML timezone, a leap second, or a fraction finer than nanoseconds exactly
     * @throws IllegalStateException if XML lexical formatting is selected and the fields do not form a valid XML Schema built-in date/time type
     * @see <a href="#format-parse-round-trip-examples">Default format/parse round-trip examples</a>
     */
    @MayReturnNull
    public static String format(final XMLGregorianCalendar calendar, final String format, final TimeZone timeZone, final Locale locale)
            throws IllegalArgumentException, IllegalStateException {
        N.checkArgNotNull(locale, cs.locale);

        if (calendar == null) {
            return null;
        }

        if (Strings.isEmpty(format)) {
            return formatXMLGregorianCalendarDefault(calendar, timeZone, null);
        }

        final GregorianCalendar gregorianCalendar = calendar.toGregorianCalendar();

        return format(createJUDate(gregorianCalendar), format, xmlOutputTimeZone(gregorianCalendar, format, timeZone), locale);
    }

    /**
     * The zone a custom pattern renders an XML calendar in: the caller's override, else the value's own
     * XML timezone (the live default when that field is undefined, which is how
     * {@link XMLGregorianCalendar#toGregorianCalendar()} already resolves it). This matches the
     * {@code Calendar} overloads; dropping the value's defined offset for the default zone silently
     * changed the wall-clock fields a zone-sensitive pattern printed. Zone-fixed formats keep theirs.
     */
    private static TimeZone xmlOutputTimeZone(final GregorianCalendar gregorianCalendar, final String format, final TimeZone timeZone) {
        return timeZone == null && !isZoneFixedFormat(format) ? gregorianCalendar.getTimeZone() : timeZone;
    }

    /**
     * Formats the provided date using a default format and appends the result to the specified Appendable.
     * For {@code java.sql.Date}, {@link Time}, and {@link Timestamp} instances, the format
     * {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} is used so their epoch-millisecond value can round-trip.
     * For plain {@code java.util.Date} instances, the format {@code yyyy-MM-dd'T'HH:mm:ss'Z'} is used.
     * If the date is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder("Date: ");
     * Dates.formatTo(new java.util.Date(1736937045000L), sb);
     * sb.toString();                                       // returns "Date: 2025-01-15T10:30:45Z"
     *
     * StringBuilder ts = new StringBuilder();
     * Dates.formatTo(new java.sql.Timestamp(1736937045123L), ts);
     * ts.toString();                                       // returns "2025-01-15T10:30:45.123Z" (Timestamp adds millis)
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((java.util.Date) null, nb);
     * nb.toString();                                       // returns "null" (literal appended for null input)
     * }</pre>
     *
     * @param date the java.util.Date instance to be formatted; may be {@code null}.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws IllegalArgumentException if {@code appendable} is {@code null}, or a non-null value is
     *         outside the Common Era year range {@code 0001} through {@code 9999} required by the default format
     * @throws UncheckedIOException if writing the formatted date/time text or null marker to {@code appendable} fails
     * @see #format(java.util.Date)
     * @see #formatTo(java.util.Date, String, Appendable)
     * @see #formatTo(java.util.Date, String, TimeZone, Appendable)
     */
    public static void formatTo(final java.util.Date date, final Appendable appendable) throws IllegalArgumentException, UncheckedIOException {
        formatTo(date, null, null, appendable);
    }

    /**
     * Formats the provided date into a string representation using the specified format and appends the result to the specified Appendable.
     * If {@code format} is {@code null} or empty, {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} is used for
     * {@code java.sql.Date}, {@link Time}, and {@link Timestamp};
     * {@code yyyy-MM-dd'T'HH:mm:ss'Z'} is used for plain {@code java.util.Date} instances.
     * If the date is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = new java.util.Date(1736937045000L);   // the instant 2025-01-15T10:30:45Z
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(date, Dates.ISO_8601_DATE_TIME_FORMAT, sb);
     * sb.toString();                                       // returns "2025-01-15T10:30:45Z"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((java.util.Date) null, "yyyy-MM-dd", nb);
     * nb.toString();                                       // returns "null" (literal appended for null input)
     * }</pre>
     *
     * <p>Except for the two predefined UTC constants and {@link #HTTP_DATE_FORMAT}, patterns render in
     * the live machine default zone. A quoted {@code 'Z'} in a custom pattern is plain
     * text. Use the explicit-zone overload for zone-stable output.</p>
     *
     * @param date the java.util.Date instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null} or empty, the default format is used.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws IllegalArgumentException if {@code appendable} is {@code null}, the pattern is invalid,
     *         or a predefined format cannot represent the value's year or effective UTC offset
     * @throws UncheckedIOException if writing the formatted date/time text or null marker to {@code appendable} fails
     * @see #format(java.util.Date, String)
     * @see #formatTo(java.util.Date, Appendable)
     * @see #formatTo(java.util.Date, String, TimeZone, Appendable)
     */
    public static void formatTo(final java.util.Date date, final String format, final Appendable appendable)
            throws IllegalArgumentException, UncheckedIOException {
        formatTo(date, format, null, appendable);
    }

    /**
     * Formats the provided date into a string representation using the specified format and time zone, and appends the result to the specified Appendable.
     * If no time zone is provided, a {@code null} or empty format means {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} for
     * {@code java.sql.Date}, {@link Time}, and {@link Timestamp}, and
     * {@code yyyy-MM-dd'T'HH:mm:ss'Z'} for plain {@code java.util.Date} instances (rendered in UTC).
     * With an explicit time zone, a {@code null} or empty format means the corresponding offset-bearing
     * default ({@link #ISO_OFFSET_TIMESTAMP_FORMAT} / {@link #ISO_OFFSET_DATE_TIME_FORMAT}) rendered in
     * that zone, so the text still identifies one instant. It parses back to the same epoch value at the
     * precision that type's default carries: milliseconds for {@code java.sql.Date}, {@link Time} and
     * {@link Timestamp}, whole seconds for a plain {@code java.util.Date}. That default can only write a
     * whole-minute offset from -18:00 through +18:00.
     * If the date is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = new java.util.Date(1736937045000L);   // the instant 2025-01-15T10:30:45Z
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(date, "yyyy-MM-dd HH:mm:ss", utc, sb);
     * sb.toString();                                       // returns "2025-01-15 10:30:45"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((java.util.Date) null, "yyyy-MM-dd", utc, nb);
     * nb.toString();                                       // returns "null" (literal appended for null input)
     * }</pre>
     *
     * <p>Note: UTC output attaches only to the two predefined constants {@link #ISO_8601_DATE_TIME_FORMAT}
     * and {@link #ISO_8601_TIMESTAMP_FORMAT}. Passing a non-UTC {@code timeZone} together with one of
     * them throws an {@code IllegalArgumentException}; a {@code null} or UTC-equivalent zone is accepted
     * and resolved to UTC. A quoted {@code 'Z'} in any other pattern is a plain literal with no zone
     * meaning. (A {@code null} or empty format is exempt: the default pattern selected for an explicit zone
     * writes that zone's numeric offset and carries no {@code 'Z'} literal.) {@link #HTTP_DATE_FORMAT}
     * is independently fixed to GMT and has the same UTC-equivalent-zone requirement.</p>
     *
     * @param date the java.util.Date instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null} or empty, the default format is used.
     * @param timeZone the time zone for formatting; if {@code null}, the default time zone is used with an
     *        explicit pattern, while a {@code null} or empty {@code format} instead renders in UTC;
     *        must be {@code null} or UTC-equivalent when the format is fixed to UTC/GMT
     *        ({@link #ISO_8601_DATE_TIME_FORMAT}, {@link #ISO_8601_TIMESTAMP_FORMAT}, or {@link #HTTP_DATE_FORMAT}).
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws IllegalArgumentException if {@code appendable} is {@code null}, the pattern is invalid,
     *         a fixed-zone format conflicts with {@code timeZone}, or a predefined format cannot
     *         represent the value's year or effective UTC offset
     * @throws UncheckedIOException if writing the formatted date/time text or null marker to {@code appendable} fails
     * @see #format(java.util.Date, String, TimeZone)
     * @see #formatTo(java.util.Date, Appendable)
     * @see #formatTo(java.util.Date, String, Appendable)
     */
    public static void formatTo(final java.util.Date date, final String format, final TimeZone timeZone, final Appendable appendable)
            throws IllegalArgumentException, UncheckedIOException {
        formatTo(date, format, timeZone, Locale.US, appendable);
    }

    /**
     * Appends a date formatted with an explicit locale for locale-sensitive legacy pattern fields.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = new java.util.Date(1736937045000L);   // the instant 2025-01-15T10:30:45Z
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(date, "dd MMM yyyy", utc, Locale.FRENCH, sb);
     * sb.toString();                                       // returns "15 janv. 2025"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((java.util.Date) null, "dd MMM yyyy", utc, Locale.US, nb);
     * nb.toString();                                                            // returns "null" (literal appended for null input)
     * Dates.formatTo(date, "dd MMM yyyy", utc, Locale.US, (Appendable) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to format; {@code null} appends {@code "null"}
     * @param format the pattern, or {@code null} or empty for the type-dependent default
     * @param timeZone the output zone, or {@code null} for the documented default-zone policy
     * @param locale the locale for locale-sensitive pattern fields; must not be {@code null}
     * @param appendable the destination; must not be {@code null}
     * @throws IllegalArgumentException if {@code locale} or {@code appendable} is {@code null}, the
     *         pattern is invalid, a fixed-zone format conflicts with {@code timeZone}, or a predefined
     *         format cannot represent the value's year or effective UTC offset
     * @throws UncheckedIOException if appending fails
     */
    public static void formatTo(final java.util.Date date, final String format, final TimeZone timeZone, final Locale locale, final Appendable appendable)
            throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(appendable, cs.appendable);
        N.checkArgNotNull(locale, cs.locale);

        if (date == null) {
            formatToForNull(appendable);
            return;
        }

        formatDate(appendable, date, format, timeZone, locale);
    }

    /**
     * Formats the provided calendar using the timezone- and millisecond-preserving ISO zoned default
     * described by {@link #format(Calendar)} and appends it to the specified Appendable.
     * If the calendar is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(1736917245123L, TimeZone.getTimeZone("Asia/Kolkata"));
     * StringBuilder sb = new StringBuilder("Calendar: ");
     * Dates.formatTo(cal, sb);
     * sb.toString();   // returns "Calendar: 2025-01-15T10:30:45.123+05:30[Asia/Kolkata]"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((Calendar) null, nb);
     * nb.toString();                                  // returns "null" (literal appended for null input)
     * }</pre>
     *
     * @param calendar the java.util.Calendar instance to be formatted; may be {@code null}.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws IllegalArgumentException if {@code appendable} is {@code null}, a non-null value is
     *         outside the Common Era year range {@code 0001} through {@code 9999} required by the default
     *         format, or the calendar's zone carries rules no {@link ZoneId} can express
     * @throws UncheckedIOException if writing the formatted date/time text or null marker to {@code appendable} fails
     * @see #format(java.util.Calendar)
     * @see #formatTo(Calendar, String, Appendable)
     * @see #formatTo(Calendar, String, TimeZone, Appendable)
     */
    public static void formatTo(final Calendar calendar, final Appendable appendable) throws IllegalArgumentException, UncheckedIOException {
        formatTo(calendar, null, null, appendable);
    }

    /**
     * Formats the provided calendar using the specified format and appends the result. A
     * {@code null} or empty format uses the ISO zoned default described by {@link #format(Calendar)}.
     * If the calendar is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(1736937045000L);   // calendar at the 2025-01-15T10:30:45Z instant (default zone)
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(cal, Dates.ISO_8601_DATE_TIME_FORMAT, sb);
     * sb.toString();                                  // returns "2025-01-15T10:30:45Z"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((Calendar) null, "yyyy-MM-dd", nb);
     * nb.toString();                                  // returns "null" (literal appended for null input)
     * }</pre>
     *
     * @param calendar the java.util.Calendar instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null} or empty, the default format is used.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws IllegalArgumentException if {@code appendable} is {@code null}, the pattern is invalid,
     *         a predefined format cannot represent the value's year or effective UTC offset, or the effective zone carries rules no
     *         {@link ZoneId} can express and the format is the ISO zoned default or
     *         {@link #ISO_ZONED_DATE_TIME_FORMAT}
     * @throws UncheckedIOException if writing the formatted date/time text or null marker to {@code appendable} fails
     * @see #format(java.util.Calendar, String)
     * @see #formatTo(Calendar, Appendable)
     * @see #formatTo(Calendar, String, TimeZone, Appendable)
     */
    public static void formatTo(final Calendar calendar, final String format, final Appendable appendable)
            throws IllegalArgumentException, UncheckedIOException {
        formatTo(calendar, format, null, appendable);
    }

    /**
     * Formats the provided calendar using the specified format and time zone and appends the result.
     * A {@code null} or empty format uses the ISO zoned default; an explicit {@code timeZone} overrides the
     * calendar's own zone, which is retained otherwise.
     * If the calendar is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(1736937045000L);   // calendar at the 2025-01-15T10:30:45Z instant (default zone)
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(cal, "yyyy-MM-dd HH:mm:ss", utc, sb);
     * sb.toString();                                  // returns "2025-01-15 10:30:45"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((Calendar) null, "yyyy-MM-dd", utc, nb);
     * nb.toString();                                  // returns "null" (literal appended for null input)
     * }</pre>
     *
     * <p>Note: UTC output attaches only to the two predefined constants {@link #ISO_8601_DATE_TIME_FORMAT}
     * and {@link #ISO_8601_TIMESTAMP_FORMAT}. Passing a non-UTC {@code timeZone} together with one of
     * them throws an {@code IllegalArgumentException}; a {@code null} or UTC-equivalent zone is accepted
     * and resolved to UTC. A quoted {@code 'Z'} in any other pattern is a plain literal with no zone
     * meaning. The ISO zoned default selected by a {@code null} or empty format writes the effective zone explicitly.
     * {@link #HTTP_DATE_FORMAT} is independently fixed to GMT and has the same
     * UTC-equivalent-zone requirement.</p>
     *
     * @param calendar the java.util.Calendar instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null} or empty, the default format is used.
     * @param timeZone the output-zone override; if {@code null}, the calendar's own zone is used;
     *        must be {@code null} or UTC-equivalent when the format is fixed to UTC/GMT
     *        ({@link #ISO_8601_DATE_TIME_FORMAT}, {@link #ISO_8601_TIMESTAMP_FORMAT}, or {@link #HTTP_DATE_FORMAT}).
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws IllegalArgumentException if {@code appendable} is {@code null}, the pattern is invalid,
     *         a fixed-zone format conflicts with {@code timeZone}, a predefined format cannot
     *         represent the value's year or effective UTC offset, or the effective zone carries rules no
     *         {@link ZoneId} can express and the format is the ISO zoned default or
     *         {@link #ISO_ZONED_DATE_TIME_FORMAT}
     * @throws UncheckedIOException if writing the formatted date/time text or null marker to {@code appendable} fails
     * @see #format(java.util.Calendar, String, TimeZone)
     * @see #formatTo(Calendar, Appendable)
     * @see #formatTo(Calendar, String, Appendable)
     */
    public static void formatTo(final Calendar calendar, final String format, final TimeZone timeZone, final Appendable appendable)
            throws IllegalArgumentException, UncheckedIOException {
        formatTo(calendar, format, timeZone, Locale.US, appendable);
    }

    /**
     * Appends a calendar formatted with an explicit locale for locale-sensitive legacy pattern fields.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);   // the instant 2025-01-15T10:30:45Z
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(cal, "dd MMM yyyy", utc, Locale.GERMAN, sb);
     * sb.toString();                                       // returns "15 Jan. 2025"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((Calendar) null, "dd MMM yyyy", utc, Locale.US, nb);
     * nb.toString();                                                           // returns "null" (literal appended for null input)
     * Dates.formatTo(cal, "dd MMM yyyy", utc, Locale.US, (Appendable) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the calendar to format; {@code null} appends {@code "null"}
     * @param format the pattern, or {@code null} or empty for the ISO zoned default
     * @param timeZone the output-zone override, or {@code null} to retain the calendar's own zone
     * @param locale the locale for locale-sensitive pattern fields; must not be {@code null}
     * @param appendable the destination; must not be {@code null}
     * @throws IllegalArgumentException if {@code locale} or {@code appendable} is {@code null}, the
     *         pattern is invalid, a fixed-zone format conflicts with {@code timeZone}, a predefined
     *         format cannot represent the value's year or effective UTC offset, or the effective zone carries rules no
     *         {@link ZoneId} can express and the format is the ISO zoned default or
     *         {@link #ISO_ZONED_DATE_TIME_FORMAT}
     * @throws UncheckedIOException if appending fails
     */
    public static void formatTo(final Calendar calendar, final String format, final TimeZone timeZone, final Locale locale, final Appendable appendable)
            throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(appendable, cs.appendable);
        N.checkArgNotNull(locale, cs.locale);

        if (calendar == null) {
            formatToForNull(appendable);
            return;
        }

        if (Strings.isEmpty(format)) {
            formatCalendarDefault(calendar, timeZone, appendable);
        } else {
            // Match format(Calendar, ...): unless the pattern has fixed UTC/GMT semantics, a null
            // override retains the Calendar's own zone rather than falling back to the live default.
            final TimeZone outputZone = timeZone == null && !isZoneFixedFormat(format) ? calendar.getTimeZone() : timeZone;
            formatTo(createJUDate(calendar), format, outputZone, locale, appendable);
        }
    }

    /**
     * Appends the XML Schema lexical representation described by
     * {@link #format(XMLGregorianCalendar)}.
     * If the calendar is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(
     *         1736917245123L, TimeZone.getTimeZone("Asia/Kolkata"));
     * StringBuilder sb = new StringBuilder("XML Calendar: ");
     * Dates.formatTo(cal, sb);
     * sb.toString();   // returns "XML Calendar: 2025-01-15T10:30:45.123+05:30"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((XMLGregorianCalendar) null, nb);
     * nb.toString();                                  // returns "null" (literal appended for null input)
     * }</pre>
     *
     * @param calendar the XMLGregorianCalendar instance to be formatted; may be {@code null}.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws IllegalArgumentException if {@code appendable} is {@code null}
     * @throws IllegalStateException if XML lexical formatting is selected and the fields do not form a valid XML Schema built-in date/time type
     * @throws UncheckedIOException if writing the formatted date/time text or null marker to {@code appendable} fails
     * @see #format(XMLGregorianCalendar)
     * @see #formatTo(XMLGregorianCalendar, String, Appendable)
     * @see #formatTo(XMLGregorianCalendar, String, TimeZone, Appendable)
     */
    public static void formatTo(final XMLGregorianCalendar calendar, final Appendable appendable)
            throws IllegalArgumentException, IllegalStateException, UncheckedIOException {
        formatTo(calendar, null, null, appendable);
    }

    /**
     * Formats the provided XMLGregorianCalendar using the specified format and appends the result. A
     * {@code null} or empty format uses its XML Schema lexical representation.
     * If the calendar is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1736937045000L);   // the instant 2025-01-15T10:30:45Z
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(cal, Dates.ISO_8601_DATE_TIME_FORMAT, sb);
     * sb.toString();                                  // returns "2025-01-15T10:30:45Z"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((XMLGregorianCalendar) null, "yyyy-MM-dd", nb);
     * nb.toString();                                  // returns "null" (literal appended for null input)
     * }</pre>
     *
     * @param calendar the XMLGregorianCalendar instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null} or empty, the default format is used.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws IllegalArgumentException if {@code appendable} is {@code null}, the pattern is invalid,
     *         or a predefined format cannot represent the value's year or effective UTC offset
     * @throws IllegalStateException if XML lexical formatting is selected and the fields do not form a valid XML Schema built-in date/time type
     * @throws UncheckedIOException if writing the formatted date/time text or null marker to {@code appendable} fails
     * @see #format(XMLGregorianCalendar, String)
     * @see #formatTo(XMLGregorianCalendar, Appendable)
     * @see #formatTo(XMLGregorianCalendar, String, TimeZone, Appendable)
     */
    public static void formatTo(final XMLGregorianCalendar calendar, final String format, final Appendable appendable)
            throws IllegalArgumentException, IllegalStateException, UncheckedIOException {
        formatTo(calendar, format, null, appendable);
    }

    /**
     * Formats the provided XMLGregorianCalendar using the specified format and time zone and appends
     * the result. With a {@code null} or empty format and no zone override, the XML lexical representation is
     * retained. With a {@code null} or empty format and an explicit zone, the represented instant is rendered
     * as an ISO offset date-time carrying that zone's effective numeric offset, preserving fractional
     * seconds exactly through nanosecond precision. If the XML timezone is undefined, its civil fields
     * are interpreted in the live default timezone before conversion. The output offset must be a
     * whole-minute XML Schema value from -14:00 through +14:00.
     * If the calendar is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1736937045000L);   // the instant 2025-01-15T10:30:45Z
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(cal, "yyyy-MM-dd HH:mm:ss", utc, sb);
     * sb.toString();                                  // returns "2025-01-15 10:30:45"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((XMLGregorianCalendar) null, "yyyy-MM-dd", utc, nb);
     * nb.toString();                                  // returns "null" (literal appended for null input)
     * }</pre>
     *
     * <p>Note: UTC output attaches only to the two predefined constants {@link #ISO_8601_DATE_TIME_FORMAT}
     * and {@link #ISO_8601_TIMESTAMP_FORMAT}. Passing a non-UTC {@code timeZone} together with one of
     * them throws an {@code IllegalArgumentException}; a {@code null} or UTC-equivalent zone is accepted
     * and resolved to UTC. A quoted {@code 'Z'} in any other pattern is a plain literal with no zone
     * meaning. The default selected by a {@code null} or empty format writes the XML value's own offset, or the effective numeric
     * offset of an explicit override zone. {@link #HTTP_DATE_FORMAT} is independently fixed to GMT and
     * has the same UTC-equivalent-zone requirement.</p>
     *
     * @param calendar the XMLGregorianCalendar instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null} or empty, the default format is used.
     * @param timeZone the output-zone override; if {@code null} and {@code format} is null or empty, the XML
     *        calendar's own timezone is retained, for a custom pattern as well as for the lexical default
     *        (the live default time zone when the XML timezone field is undefined);
     *        must be {@code null} or UTC-equivalent when the format is fixed to UTC/GMT
     *        ({@link #ISO_8601_DATE_TIME_FORMAT}, {@link #ISO_8601_TIMESTAMP_FORMAT}, or {@link #HTTP_DATE_FORMAT}).
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws IllegalArgumentException if {@code appendable} is {@code null}, the pattern is invalid,
     *         a fixed-zone format conflicts with {@code timeZone}, a predefined format cannot
     *         represent the value's year or effective UTC offset, or default-format zone conversion cannot
     *         represent its effective XML timezone, a leap second, or a fraction finer than nanoseconds exactly
     * @throws IllegalStateException if XML lexical formatting is selected and the fields do not form a valid XML Schema built-in date/time type
     * @throws UncheckedIOException if writing the formatted date/time text or null marker to {@code appendable} fails
     * @see #format(XMLGregorianCalendar, String, TimeZone)
     * @see #formatTo(XMLGregorianCalendar, Appendable)
     * @see #formatTo(XMLGregorianCalendar, String, Appendable)
     */
    public static void formatTo(final XMLGregorianCalendar calendar, final String format, final TimeZone timeZone, final Appendable appendable)
            throws IllegalArgumentException, IllegalStateException, UncheckedIOException {
        formatTo(calendar, format, timeZone, Locale.US, appendable);
    }

    /**
     * Appends an XML Gregorian calendar formatted with an explicit locale for locale-sensitive legacy pattern fields.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1736937045000L, utc);
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(cal, "dd MMM yyyy", utc, Locale.GERMAN, sb);
     * sb.toString();                                       // returns "15 Jan. 2025"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((XMLGregorianCalendar) null, "dd MMM yyyy", utc, Locale.US, nb);
     * nb.toString();                                                           // returns "null" (literal appended for null input)
     * Dates.formatTo(cal, "dd MMM yyyy", utc, Locale.US, (Appendable) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the XML calendar to format; {@code null} appends {@code "null"}
     * @param format the pattern, or {@code null} or empty for the XML lexical default
     * @param timeZone the output-zone override, or {@code null} to retain the XML timezone
     * @param locale the locale for locale-sensitive pattern fields; must not be {@code null}
     * @param appendable the destination; must not be {@code null}
     * @throws IllegalArgumentException if {@code locale} or {@code appendable} is {@code null}, the
     *         pattern is invalid, a fixed-zone format conflicts with {@code timeZone}, a predefined
     *         format cannot represent the value's year or effective UTC offset, or default-format zone
     *         conversion cannot represent its effective XML timezone, a leap second, or a fraction
     *         finer than nanoseconds exactly
     * @throws IllegalStateException if XML lexical formatting is selected and the fields do not form a valid XML Schema built-in date/time type
     * @throws UncheckedIOException if appending fails
     */
    public static void formatTo(final XMLGregorianCalendar calendar, final String format, final TimeZone timeZone, final Locale locale,
            final Appendable appendable) throws IllegalArgumentException, IllegalStateException, UncheckedIOException {
        N.checkArgNotNull(appendable, cs.appendable);
        N.checkArgNotNull(locale, cs.locale);

        if (calendar == null) {
            formatToForNull(appendable);
            return;
        }

        if (Strings.isEmpty(format)) {
            formatXMLGregorianCalendarDefault(calendar, timeZone, appendable);
        } else {
            // Match format(XMLGregorianCalendar, ...): a null override retains the XML timezone.
            final GregorianCalendar gregorianCalendar = calendar.toGregorianCalendar();
            formatTo(createJUDate(gregorianCalendar), format, xmlOutputTimeZone(gregorianCalendar, format, timeZone), locale, appendable);
        }
    }

    /**
     * Formats a legacy calendar's exact millisecond value together with the effective offset and
     * region ID. A caller-supplied zone overrides the calendar's own zone; otherwise the calendar's
     * zone is authoritative.
     */
    private static String formatCalendarDefault(final Calendar calendar, final TimeZone timeZone, final Appendable appendable) {
        final TimeZone calendarZone = timeZone == null ? calendar.getTimeZone() : timeZone;
        final TimeZone outputZone = (TimeZone) (calendarZone == null ? TimeZone.getDefault() : calendarZone).clone();
        final java.util.Date date = createJUDate(calendar);

        checkFixedFourDigitYear(date, outputZone, ISO_ZONED_DATE_TIME_FORMAT);

        final String str = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(exactInstant(date).atZone(toZoneId(outputZone)));

        if (appendable != null) {
            appendFormattedText(appendable, str);
            return null;
        }

        return str;
    }

    /**
     * Uses the XML lexical representation when no override zone is supplied, retaining the XML
     * fraction and offset exactly (and leaving an undefined XML timezone undefined). With an
     * explicit override zone, renders the represented instant with that zone's numeric offset.
     */
    private static String formatXMLGregorianCalendarDefault(final XMLGregorianCalendar calendar, final TimeZone timeZone, final Appendable appendable) {
        final String str;

        if (timeZone == null) {
            str = calendar.toXMLFormat();
        } else {
            final TimeZone outputZone = (TimeZone) timeZone.clone();
            final Instant instant = exactInstant(calendar);
            final java.util.Date date = java.util.Date.from(instant);

            checkFixedFourDigitYear(date, outputZone, ISO_OFFSET_DATE_TIME_FORMAT);
            // The rendered offset is the java.time one (below), so that is the offset to check: before
            // 1900 the legacy table says a whole-minute -05:00 for New York where ZoneRules give the
            // local-mean-time -04:56:02 that XML Schema cannot represent.
            checkXMLTimeZoneRepresentable(legacyRenderingZone(outputZone, instant.toEpochMilli()), instant.toEpochMilli());
            str = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atZone(toZoneId(outputZone)).toOffsetDateTime());
        }

        if (appendable != null) {
            appendFormattedText(appendable, str);
            return null;
        }

        return str;
    }

    /**
     * @throws UncheckedIOException if appending the formatted text fails.
     */
    private static void appendFormattedText(final Appendable appendable, final String str) throws UncheckedIOException {
        try {
            appendable.append(str);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @throws IllegalArgumentException if {@code locale} is {@code null}, or a non-null date cannot be represented by the requested format and time zone.
     * @throws UncheckedIOException if writing the formatted result to {@code appendable} fails.
     */
    private static String formatDate(final Appendable appendable, final java.util.Date date, String format, TimeZone timeZone, final Locale locale)
            throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(locale, cs.locale);

        if (date == null) {
            if (appendable != null) {
                formatToForNull(appendable);
            }

            return null;
        }

        final boolean includeMillis = date instanceof Timestamp || date instanceof Date || date instanceof Time;

        if (Strings.isEmpty(format) && (timeZone == null)) {
            if (appendable == null) {
                final StringBuilder sb = Objectory.createStringBuilder();

                try {
                    fastDateFormat(sb, null, date.getTime(), includeMillis);

                    return sb.toString();
                } finally {
                    Objectory.recycle(sb);
                }
            } else {
                fastDateFormat(null, appendable, date.getTime(), includeMillis);

                return null;
            }
        }

        if (Strings.isEmpty(format)) {
            // Reaching here means an explicit time zone was given (the default-format/null-zone case is
            // handled by the fast path above). Write the effective offset rather than a zone-less local
            // form: the text then still identifies one instant and keeps one separator regardless of the
            // runtime type. The 'Z'-terminated UTC defaults would (correctly) reject any non-UTC zone.
            // Precision follows the type, matching its UTC default exactly: SQL Date/Time/Timestamp keep
            // milliseconds, a plain java.util.Date is whole seconds.
            format = includeMillis ? ISO_OFFSET_TIMESTAMP_FORMAT : ISO_OFFSET_DATE_TIME_FORMAT;
        }

        timeZone = checkTimeZone(null, format, timeZone);

        // A predefined constant means one grammar and one civil view on every entry point, so the
        // legacy formatter has to see the zone rules the rest of this class uses. ISO_ZONED_DATE_TIME
        // is excluded because it already renders through java.time and needs the region ID, not an
        // offset. Custom SimpleDateFormat patterns keep the legacy engine on the format and the parse
        // side alike, so both sides read one set of rules - which is not the same as round-tripping:
        // a daylight name, a Z offset with a seconds component and a zone-less overlap each read back
        // as a different instant (see the class javadoc's zone-rules paragraph and legacyRenderingZone).
        if (isPredefinedLegacyFormat(format) && !ISO_ZONED_DATE_TIME_FORMAT.equals(format)) {
            timeZone = legacyRenderingZone(timeZone, date.getTime());
        }

        if (hasFixedFourDigitLeadingYear(format)) {
            checkFixedFourDigitYear(date, timeZone, format);
        }

        if (ISO_OFFSET_DATE_TIME_FORMAT.equals(format) || ISO_OFFSET_TIMESTAMP_FORMAT.equals(format)) {
            checkIsoOffsetAtInstant(date, timeZone);
        }

        if (ISO_ZONED_DATE_TIME_FORMAT.equals(format)) {
            final String str = DTF.ISO_ZONED_DATE_TIME.format(exactInstant(date).atZone(toZoneId(timeZone)));

            if (appendable != null) {
                try {
                    appendable.append(str);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            return str;
        }

        final DateFormat sdf = getSDF(format, timeZone, locale);

        try {
            if (HTTP_DATE_FORMAT.equals(format)) {
                checkHttpDateYear(Instant.ofEpochMilli(date.getTime()));
            }

            final String str = sdf.format(date);

            if (appendable != null) {
                try {
                    appendable.append(str);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            return str;
        } finally {
            recycleSDF(format, timeZone, locale, sdf);
        }
    }

    private static void fastDateFormat(final StringBuilder sb, final Appendable appendable, final long timeInMillis, final boolean includeMillis) {
        // The civil fields come from java.time rather than from a GregorianCalendar built per call: this is
        // the default rendering of every Date, and constructing and completing a calendar cost several
        // times what the rest of the method does. Both views are proleptic ISO in UTC, so the fields are
        // identical, and a proleptic year below 1 is exactly the BCE case the old ERA test rejected.
        // LocalDateTime.ofEpochSecond accepts the whole long epoch-millisecond range (the extreme epoch
        // days are far inside its limits), so no DateTimeException can arise before the year guard.
        final int milliSecond = (int) Math.floorMod(timeInMillis, 1000L);
        final LocalDateTime utc = LocalDateTime.ofEpochSecond(Math.floorDiv(timeInMillis, 1000L), milliSecond * 1_000_000, ZoneOffset.UTC);
        final int year = utc.getYear();
        final int month = utc.getMonthValue();
        final int day = utc.getDayOfMonth();
        final int hour = utc.getHour();
        final int minute = utc.getMinute();
        final int second = utc.getSecond();

        if (year < 1 || year > 9999) {
            throw new IllegalArgumentException(
                    "ISO 8601 formatting supports Common Era years from 0001 through 9999; got instant " + Instant.ofEpochMilli(timeInMillis));
        }

        char[] utcTimestamp = utcTimestampFormatCharsPool.poll();

        if (utcTimestamp == null) {
            utcTimestamp = new char[24];
            utcTimestamp[4] = '-';
            utcTimestamp[7] = '-';
            utcTimestamp[10] = 'T';
            utcTimestamp[13] = ':';
            utcTimestamp[16] = ':';
            utcTimestamp[19] = '.';
            utcTimestamp[23] = 'Z';
        }

        // Everything after the buffer is taken belongs inside the try: the finally is the only thing
        // that returns it to the pool, so a throw from the field writes would drop it permanently.
        try {
            writePaddedInt(utcTimestamp, 0, year, 4);
            writePaddedInt(utcTimestamp, 5, month, 2);
            writePaddedInt(utcTimestamp, 8, day, 2);
            writePaddedInt(utcTimestamp, 11, hour, 2);
            writePaddedInt(utcTimestamp, 14, minute, 2);
            writePaddedInt(utcTimestamp, 17, second, 2);

            if (includeMillis) {
                utcTimestamp[19] = '.';
                writePaddedInt(utcTimestamp, 20, milliSecond, 3);
            } else {
                utcTimestamp[19] = 'Z';
            }

            if (includeMillis) {
                if (sb == null) {
                    if (appendable instanceof Writer) {
                        // Copy before writing: utcTimestamp is recycled in the finally block, and
                        // Writer.write(char[]) does not require the callee to copy, so a custom Writer that
                        // retained the array would observe later formatting calls mutating its content.
                        ((Writer) appendable).write(utcTimestamp.clone());
                    } else {
                        appendable.append(String.valueOf(utcTimestamp));
                    }
                } else {
                    sb.append(utcTimestamp);
                }
            } else {
                if (sb == null) {
                    if (appendable instanceof Writer) {
                        // Copy before writing; see the millisecond branch above for why the pooled buffer
                        // must not escape into a caller-supplied Writer.
                        ((Writer) appendable).write(java.util.Arrays.copyOf(utcTimestamp, 20), 0, 20);
                    } else {
                        // Do not expose the pooled backing array through a CharBuffer: a legal custom
                        // Appendable may retain the CharSequence after this method returns, while the
                        // array is immediately recycled and mutated by later formatting calls.
                        appendable.append(new String(utcTimestamp, 0, 20));
                    }
                } else {
                    sb.append(utcTimestamp, 0, 20);
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            utcTimestampFormatCharsPool.offer(utcTimestamp);
        }
    }

    /** Writes a non-negative integer as exactly {@code width} ASCII digits. */
    private static void writePaddedInt(final char[] buffer, final int offset, int value, final int width) {
        if (value < 0) {
            throw new IllegalArgumentException("Value must be non-negative: " + value);
        }

        for (int i = offset + width - 1; i >= offset; i--) {
            buffer[i] = (char) ('0' + value % 10);
            value /= 10;
        }

        if (value != 0) {
            throw new IllegalArgumentException("Value does not fit in " + width + " digits");
        }
    }

    //-----------------------------------------------------------------------

    private static Instant exactInstant(final java.util.Date date) {
        return date instanceof Timestamp ? ((Timestamp) date).toInstant() : Instant.ofEpochMilli(date.getTime());
    }

    /**
     * Converts an XML date-time to the nanosecond precision supported by {@link Instant}. The XML
     * lexical default never calls this method and can therefore retain finer fractions and leap
     * seconds unchanged; conversion for a zone override rejects values that cannot be represented
     * exactly instead of silently truncating them.
     */
    private static Instant exactInstant(final XMLGregorianCalendar calendar) {
        if (calendar.getSecond() == 60) {
            throw new IllegalArgumentException("A leap-second XMLGregorianCalendar cannot be converted exactly to an Instant for zone conversion");
        }

        final Instant millisecondInstant = calendar.toGregorianCalendar().toInstant();
        final BigDecimal fraction = calendar.getFractionalSecond();

        if (fraction == null) {
            return millisecondInstant;
        }

        try {
            final int nanos = fraction.movePointRight(9).intValueExact();
            return Instant.ofEpochSecond(millisecondInstant.getEpochSecond(), nanos);
        } catch (final ArithmeticException e) {
            throw new IllegalArgumentException(
                    "XMLGregorianCalendar fractional seconds finer than nanoseconds cannot be converted exactly for zone conversion: " + fraction, e);
        }
    }

    /** Ensures an instant's effective zone offset fits XML Schema's whole-minute, +/-14:00 field. */
    private static void checkXMLTimeZoneRepresentable(final TimeZone timeZone, final long epochMillis) {
        final int offsetMillis = timeZone.getOffset(epochMillis);
        final int maxOffsetMillis = 14 * 60 * 60 * 1000;

        if (offsetMillis % (60 * 1000) != 0 || Math.abs((long) offsetMillis) > maxOffsetMillis) {
            throw new IllegalArgumentException("XMLGregorianCalendar timezone must be a whole-minute offset in the range -14:00 through +14:00; zone '"
                    + timeZone.getID() + "' has offset " + offsetMillis + " milliseconds at epoch millisecond " + epochMillis);
        }
    }

    private static int compareInstants(final java.util.Date left, final java.util.Date right) {
        return exactInstant(left).compareTo(exactInstant(right));
    }

    /**
     * Adapted from Apache Commons Lang under Apache License v2; an out-of-range day-of-month is clamped
     * rather than rolled over - see below.
     * <br />
     *
     * Sets the years field to a date returning a new object.
     * The original {@code Date} is unchanged.
     * For a {@link Timestamp}, sub-millisecond nanoseconds are preserved. Only
     * {@link #setMilliseconds(java.util.Date, int)} replaces the complete fractional second.
     *
     * <p>A day-of-month the target year does not have is clamped to the last valid day rather than
     * rolling into the next month: 29 February in a common year becomes 28 February. This matches
     * {@link LocalDate#withYear(int)}. The time of day is carried over unchanged.</p>
     *
     * <p><b>{@code amount} is a proleptic ISO year</b>, the scale the rest of this class reads and
     * {@code format} prints: {@code 1} is 1 CE, {@code 0} is 1 BCE, and negative values run further back,
     * matching {@link LocalDate#withYear(int)}. It is <i>not</i> {@link Calendar#YEAR}'s year-of-era, which
     * would silently change meaning with the input's era &mdash; on a BCE value {@code setYears(date, 2000)}
     * would have produced 2000 BCE, and no BCE value could be moved into the Common Era at all.</p>
     *
     * <p><b>Daylight saving.</b> The field being set keeps the value given or the call throws: when the
     * result names a wall clock a spring-forward gap removes, the value is resolved forward by the gap's
     * length as {@link ZonedDateTime#withHour(int)} does, and the call throws only if that moved the
     * field that was set ({@code setYears} and {@code setMonths} also keep the day of month they carry over,
     * so a calendar day the zone skips - Apia 2011-12-30 - is rejected). {@code setHours(date, 2)} therefore fails on a day whose whole 02:00 hour is
     * missing (02:30 becomes 03:30), while {@code setDays} carries a 00:30 time of day to 01:30 on a day
     * with no local midnight, and {@code setMinutes(00:00:59, 15)} on a day whose gap starts at 00:01
     * (Newfoundland, 1987) lands on 01:15:59 because the minute field it set is intact. When an autumn
     * overlap repeats the resulting wall clock, the result keeps the offset the input was already on &mdash; so setting a field to the
     * value it already holds never moves the instant &mdash; and takes the earlier of the two occurrences
     * when the input was on neither. That is the rule {@link ZonedDateTime} resolution and
     * {@link #truncate(java.util.Date, int)} both follow.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.setYears(date, 2025), "yyyy-MM-dd HH:mm:ss");   // returns "2025-11-24 10:30:45"
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                         // returns "2024-11-24 10:30:45" (original unchanged)
     *
     * // 29 February clamps to the last day of February in a common year
     * java.util.Date leapDay = Dates.parseToJUDate("2024-02-29 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.setYears(leapDay, 2025), "yyyy-MM-dd HH:mm:ss");   // returns "2025-02-28 10:30:45"
     *
     * Dates.setYears((java.util.Date) null, 2025);                       // throws IllegalArgumentException
     *
     * // the amount is a proleptic ISO year: 0 is 1 BCE and negatives run further back
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * java.util.Date ce = Dates.parseToJUDate("0005-06-15 10:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * Dates.format(Dates.setYears(ce, 0), "yyyy-MM-dd G", utc);          // returns "0001-06-15 BC"
     * Dates.format(Dates.setYears(ce, -4), "yyyy-MM-dd G", utc);         // returns "0005-06-15 BC"
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not {@code null}.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is {@code null}, if {@code amount} is out of range
     *         for the field, or if the requested value names a wall clock the evaluating time zone does
     *         not have on that date (a daylight-saving gap).
     *         A day-of-month the resulting month does not have is clamped, not rejected.
     * @see Calendar#YEAR
     * @see Calendar#set(int, int)
     */
    public static <T extends java.util.Date> T setYears(final T date, final int amount) throws IllegalArgumentException {
        return set(date, Calendar.YEAR, amount);
    }

    //-----------------------------------------------------------------------

    /**
     * Adapted from Apache Commons Lang under Apache License v2; an out-of-range day-of-month is clamped
     * rather than rolled over - see below.
     * <br />
     *
     * Sets the months field to a date returning a new object.
     * The original {@code Date} is unchanged.
     * For a {@link Timestamp}, sub-millisecond nanoseconds are preserved. Only
     * {@link #setMilliseconds(java.util.Date, int)} replaces the complete fractional second.
     *
     * <p>A day-of-month the target month does not have is clamped to the last valid day rather than
     * rolling into the next month: 31 January becomes 28 or 29 February. This matches
     * {@link LocalDate#withMonth(int)}. The time of day is carried over unchanged.</p>
     *
     * <p><b>Daylight saving.</b> The field being set keeps the value given or the call throws: when the
     * result names a wall clock a spring-forward gap removes, the value is resolved forward by the gap's
     * length as {@link ZonedDateTime#withHour(int)} does, and the call throws only if that moved the
     * field that was set ({@code setYears} and {@code setMonths} also keep the day of month they carry over,
     * so a calendar day the zone skips - Apia 2011-12-30 - is rejected). {@code setHours(date, 2)} therefore fails on a day whose whole 02:00 hour is
     * missing (02:30 becomes 03:30), while {@code setDays} carries a 00:30 time of day to 01:30 on a day
     * with no local midnight, and {@code setMinutes(00:00:59, 15)} on a day whose gap starts at 00:01
     * (Newfoundland, 1987) lands on 01:15:59 because the minute field it set is intact. When an autumn
     * overlap repeats the resulting wall clock, the result keeps the offset the input was already on &mdash; so setting a field to the
     * value it already holds never moves the instant &mdash; and takes the earlier of the two occurrences
     * when the input was on neither. That is the rule {@link ZonedDateTime} resolution and
     * {@link #truncate(java.util.Date, int)} both follow.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.setMonths(date, 0), "yyyy-MM-dd HH:mm:ss");   // returns "2024-01-24 10:30:45" (0 = January)
     * Dates.format(Dates.setMonths(date, 5), "yyyy-MM-dd HH:mm:ss");   // returns "2024-06-24 10:30:45" (5 = June)
     *
     * // 31 January clamps to the last day of February
     * java.util.Date endOfJanuary = Dates.parseToJUDate("2024-01-31 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.setMonths(endOfJanuary, 1), "yyyy-MM-dd HH:mm:ss");   // returns "2024-02-29 10:30:45"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                       // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.setMonths((java.util.Date) null, 0);                       // throws IllegalArgumentException
     * Dates.setMonths(date, 12);                                       // throws IllegalArgumentException (month out of range)
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not {@code null}.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is {@code null}, if {@code amount} is out of range
     *         for the field, or if the requested value names a wall clock the evaluating time zone does
     *         not have on that date (a daylight-saving gap).
     *         A day-of-month the resulting month does not have is clamped, not rejected.
     * @see Calendar#MONTH
     * @see Calendar#set(int, int)
     */
    public static <T extends java.util.Date> T setMonths(final T date, final int amount) throws IllegalArgumentException {
        return set(date, Calendar.MONTH, amount);
    }

    //-----------------------------------------------------------------------

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Sets the day of month field to a date returning a new object.
     * The original {@code Date} is unchanged.
     * For a {@link Timestamp}, sub-millisecond nanoseconds are preserved. Only
     * {@link #setMilliseconds(java.util.Date, int)} replaces the complete fractional second.
     *
     * <p><b>Daylight saving.</b> The field being set keeps the value given or the call throws: when the
     * result names a wall clock a spring-forward gap removes, the value is resolved forward by the gap's
     * length as {@link ZonedDateTime#withHour(int)} does, and the call throws only if that moved the
     * field that was set ({@code setYears} and {@code setMonths} also keep the day of month they carry over,
     * so a calendar day the zone skips - Apia 2011-12-30 - is rejected). {@code setHours(date, 2)} therefore fails on a day whose whole 02:00 hour is
     * missing (02:30 becomes 03:30), while {@code setDays} carries a 00:30 time of day to 01:30 on a day
     * with no local midnight, and {@code setMinutes(00:00:59, 15)} on a day whose gap starts at 00:01
     * (Newfoundland, 1987) lands on 01:15:59 because the minute field it set is intact. When an autumn
     * overlap repeats the resulting wall clock, the result keeps the offset the input was already on &mdash; so setting a field to the
     * value it already holds never moves the instant &mdash; and takes the earlier of the two occurrences
     * when the input was on neither. That is the rule {@link ZonedDateTime} resolution and
     * {@link #truncate(java.util.Date, int)} both follow.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.setDays(date, 15), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-15 10:30:45"
     * Dates.format(Dates.setDays(date, 1), "yyyy-MM-dd HH:mm:ss");    // returns "2024-11-01 10:30:45"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                      // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.setDays((java.util.Date) null, 15);                       // throws IllegalArgumentException
     *
     * // an explicit day the month does not have is rejected, not clamped
     * java.util.Date february = Dates.parseToJUDate("2024-02-05 10:00:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.setDays(february, 31);                                    // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not {@code null}.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is {@code null}, if {@code amount} is out of range
     *         for the field, or if the requested value names a wall clock the evaluating time zone does
     *         not have on that date (a daylight-saving gap).
     *         Unlike {@link #setYears(java.util.Date, int)} and {@link #setMonths(java.util.Date, int)},
     *         which clamp, an explicit day-of-month the month does not have is rejected.
     * @see Calendar#DAY_OF_MONTH
     * @see Calendar#set(int, int)
     */
    public static <T extends java.util.Date> T setDays(final T date, final int amount) throws IllegalArgumentException {
        return set(date, Calendar.DAY_OF_MONTH, amount);
    }

    //-----------------------------------------------------------------------

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Sets the hours field to a date returning a new object.
     * Hours range from 0-23.
     * The original {@code Date} is unchanged.
     * For a {@link Timestamp}, sub-millisecond nanoseconds are preserved. Only
     * {@link #setMilliseconds(java.util.Date, int)} replaces the complete fractional second.
     *
     * <p><b>Daylight saving.</b> The field being set keeps the value given or the call throws: when the
     * result names a wall clock a spring-forward gap removes, the value is resolved forward by the gap's
     * length as {@link ZonedDateTime#withHour(int)} does, and the call throws only if that moved the
     * field that was set ({@code setYears} and {@code setMonths} also keep the day of month they carry over,
     * so a calendar day the zone skips - Apia 2011-12-30 - is rejected). {@code setHours(date, 2)} therefore fails on a day whose whole 02:00 hour is
     * missing (02:30 becomes 03:30), while {@code setDays} carries a 00:30 time of day to 01:30 on a day
     * with no local midnight, and {@code setMinutes(00:00:59, 15)} on a day whose gap starts at 00:01
     * (Newfoundland, 1987) lands on 01:15:59 because the minute field it set is intact. When an autumn
     * overlap repeats the resulting wall clock, the result keeps the offset the input was already on &mdash; so setting a field to the
     * value it already holds never moves the instant &mdash; and takes the earlier of the two occurrences
     * when the input was on neither. That is the rule {@link ZonedDateTime} resolution and
     * {@link #truncate(java.util.Date, int)} both follow.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.setHours(date, 14), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-24 14:30:45"
     * Dates.format(Dates.setHours(date, 0), "yyyy-MM-dd HH:mm:ss");    // returns "2024-11-24 00:30:45" (midnight hour)
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                       // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.setHours((java.util.Date) null, 14);                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not {@code null}.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is {@code null}, if {@code amount} is out of range
     *         for the field, or if the requested value names a wall clock the evaluating time zone does
     *         not have on that date (a daylight-saving gap).
     * @see Calendar#HOUR_OF_DAY
     * @see Calendar#set(int, int)
     */
    public static <T extends java.util.Date> T setHours(final T date, final int amount) throws IllegalArgumentException {
        return set(date, Calendar.HOUR_OF_DAY, amount);
    }

    //-----------------------------------------------------------------------

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Sets the minutes field to a date returning a new object.
     * The original {@code Date} is unchanged.
     * For a {@link Timestamp}, sub-millisecond nanoseconds are preserved. Only
     * {@link #setMilliseconds(java.util.Date, int)} replaces the complete fractional second.
     *
     * <p><b>Daylight saving.</b> The field being set keeps the value given or the call throws: when the
     * result names a wall clock a spring-forward gap removes, the value is resolved forward by the gap's
     * length as {@link ZonedDateTime#withHour(int)} does, and the call throws only if that moved the
     * field that was set ({@code setYears} and {@code setMonths} also keep the day of month they carry over,
     * so a calendar day the zone skips - Apia 2011-12-30 - is rejected). {@code setHours(date, 2)} therefore fails on a day whose whole 02:00 hour is
     * missing (02:30 becomes 03:30), while {@code setDays} carries a 00:30 time of day to 01:30 on a day
     * with no local midnight, and {@code setMinutes(00:00:59, 15)} on a day whose gap starts at 00:01
     * (Newfoundland, 1987) lands on 01:15:59 because the minute field it set is intact. When an autumn
     * overlap repeats the resulting wall clock, the result keeps the offset the input was already on &mdash; so setting a field to the
     * value it already holds never moves the instant &mdash; and takes the earlier of the two occurrences
     * when the input was on neither. That is the rule {@link ZonedDateTime} resolution and
     * {@link #truncate(java.util.Date, int)} both follow.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.setMinutes(date, 15), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-24 10:15:45"
     * Dates.format(Dates.setMinutes(date, 0), "yyyy-MM-dd HH:mm:ss");    // returns "2024-11-24 10:00:45"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                         // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.setMinutes((java.util.Date) null, 15);                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not {@code null}.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is {@code null}, if {@code amount} is out of range
     *         for the field, or if the requested value names a wall clock the evaluating time zone does
     *         not have on that date (a daylight-saving gap).
     * @see Calendar#MINUTE
     * @see Calendar#set(int, int)
     */
    public static <T extends java.util.Date> T setMinutes(final T date, final int amount) throws IllegalArgumentException {
        return set(date, Calendar.MINUTE, amount);
    }

    //-----------------------------------------------------------------------

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Sets the seconds field to a date returning a new object.
     * The original {@code Date} is unchanged.
     * For a {@link Timestamp}, sub-millisecond nanoseconds are preserved. Only
     * {@link #setMilliseconds(java.util.Date, int)} replaces the complete fractional second.
     *
     * <p><b>Daylight saving.</b> The field being set keeps the value given or the call throws: when the
     * result names a wall clock a spring-forward gap removes, the value is resolved forward by the gap's
     * length as {@link ZonedDateTime#withHour(int)} does, and the call throws only if that moved the
     * field that was set ({@code setYears} and {@code setMonths} also keep the day of month they carry over,
     * so a calendar day the zone skips - Apia 2011-12-30 - is rejected). {@code setHours(date, 2)} therefore fails on a day whose whole 02:00 hour is
     * missing (02:30 becomes 03:30), while {@code setDays} carries a 00:30 time of day to 01:30 on a day
     * with no local midnight, and {@code setMinutes(00:00:59, 15)} on a day whose gap starts at 00:01
     * (Newfoundland, 1987) lands on 01:15:59 because the minute field it set is intact. When an autumn
     * overlap repeats the resulting wall clock, the result keeps the offset the input was already on &mdash; so setting a field to the
     * value it already holds never moves the instant &mdash; and takes the earlier of the two occurrences
     * when the input was on neither. That is the rule {@link ZonedDateTime} resolution and
     * {@link #truncate(java.util.Date, int)} both follow.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.setSeconds(date, 0), "yyyy-MM-dd HH:mm:ss");    // returns "2024-11-24 10:30:00"
     * Dates.format(Dates.setSeconds(date, 59), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-24 10:30:59"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                         // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.setSeconds((java.util.Date) null, 0);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not {@code null}.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is {@code null}, if {@code amount} is out of range
     *         for the field, or if the requested value names a wall clock the evaluating time zone does
     *         not have on that date (a daylight-saving gap).
     * @see Calendar#SECOND
     * @see Calendar#set(int, int)
     */
    public static <T extends java.util.Date> T setSeconds(final T date, final int amount) throws IllegalArgumentException {
        return set(date, Calendar.SECOND, amount);
    }

    //-----------------------------------------------------------------------

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Sets the milliseconds field to a date returning a new object.
     * The original {@code Date} is unchanged.
     * For a {@link Timestamp}, this replaces the complete fractional second: sub-millisecond
     * nanoseconds are discarded and {@code nanos} becomes {@code amount * 1_000_000}.
     *
     * <p><b>Daylight saving.</b> The field being set keeps the value given or the call throws: when the
     * result names a wall clock a spring-forward gap removes, the value is resolved forward by the gap's
     * length as {@link ZonedDateTime#withHour(int)} does, and the call throws only if that moved the
     * field that was set ({@code setYears} and {@code setMonths} also keep the day of month they carry over,
     * so a calendar day the zone skips - Apia 2011-12-30 - is rejected). {@code setHours(date, 2)} therefore fails on a day whose whole 02:00 hour is
     * missing (02:30 becomes 03:30), while {@code setDays} carries a 00:30 time of day to 01:30 on a day
     * with no local midnight, and {@code setMinutes(00:00:59, 15)} on a day whose gap starts at 00:01
     * (Newfoundland, 1987) lands on 01:15:59 because the minute field it set is intact. When an autumn
     * overlap repeats the resulting wall clock, the result keeps the offset the input was already on &mdash; so setting a field to the
     * value it already holds never moves the instant &mdash; and takes the earlier of the two occurrences
     * when the input was on neither. That is the rule {@link ZonedDateTime} resolution and
     * {@link #truncate(java.util.Date, int)} both follow.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2024-11-24 10:30:45.123", "yyyy-MM-dd HH:mm:ss.SSS");
     * Dates.format(Dates.setMilliseconds(date, 500), "yyyy-MM-dd HH:mm:ss.SSS");   // returns "2024-11-24 10:30:45.500"
     * Dates.format(Dates.setMilliseconds(date, 0), "yyyy-MM-dd HH:mm:ss.SSS");     // returns "2024-11-24 10:30:45.000"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss.SSS");                               // returns "2024-11-24 10:30:45.123" (original unchanged)
     * Dates.setMilliseconds((java.util.Date) null, 500);                           // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not {@code null}.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is {@code null}, if {@code amount} is out of range
     *         for the field, or if the requested value names a wall clock the evaluating time zone does
     *         not have on that date (a daylight-saving gap).
     * @see Calendar#MILLISECOND
     * @see Calendar#set(int, int)
     */
    public static <T extends java.util.Date> T setMilliseconds(final T date, final int amount) throws IllegalArgumentException {
        return set(date, Calendar.MILLISECOND, amount);
    }

    //-----------------------------------------------------------------------

    /**
     * Adapted from Apache Commons Lang under Apache License v2; a day-of-month the resulting month does
     * not have is clamped rather than rolled over - see below.
     * <br />
     *
     * Sets the specified field to a date returning a new object.
     * This does not use a lenient calendar.
     * The original {@code Date} is unchanged.
     * For a {@link Timestamp}, sub-millisecond nanoseconds are preserved unless
     * {@code calendarField} is {@link Calendar#MILLISECOND}, which replaces the complete fractional second.
     *
     * <p>Setting {@link Calendar#YEAR} or {@link Calendar#MONTH} clamps a day-of-month the resulting
     * month does not have. Every other field rejects a value that is out of range, and
     * {@link Calendar#DAY_OF_MONTH} additionally rejects an in-range day the current month does not
     * have (31 in February, say).</p>
     *
     * @param <T> the concrete {@code java.util.Date} subtype returned by this method
     * @param date the date, not {@code null}.
     * @param calendarField the {@code Calendar} field to set the amount to.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is {@code null}, if the field value is out of range
     *         for the field, or if it is an in-range day-of-month the current month does not have.
     * @see Calendar#set(int, int)
     */
    private static <T extends java.util.Date> T set(final T date, final int calendarField, final int amount) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        // A fresh calendar per call, so this method is thread safe. It must be proleptic as well as
        // Gregorian: reading a pre-1582 instant through the default cutover made setYears/setMonths/
        // setDays disagree with the date format() prints for the very same value. legacyRenderingZone
        // closes the same gap on the zone axis - java.util.TimeZone has no history before 1900.
        //
        // The write itself runs on a calendar PINNED to the single offset the rendering zone shows at the
        // source (java.time's offset there), exactly as addCivilFieldMillis pins its walk: Calendar still
        // validates ranges and the day of month and clamps for YEAR/MONTH, but it can no longer normalise the
        // wall clock through the legacy table, whose TRANSITIONS differ from ZoneRules even where its offsets
        // agree - a synthetic one at 1900-01-01T00:00Z (the present raw offset before, the 1900 offset after:
        // a phantom 30-minute gap in Windhoek, 5h43 in Shanghai, 7 h in Cambridge Bay, so setMinutes on
        // 1900-01-01 01:30 Windhoek threw for a wall clock the zone has) and the projected rule of Asia/Gaza
        // and Asia/Hebron falling a week off after 2100. The fields are then resolved through the real rules
        // and the field that was set is checked on the RESOLVED wall clock. A zone no ZoneId can express
        // keeps Calendar's own resolution end to end, as the class contract says.
        final TimeZone zone = TimeZone.getDefault();
        final long sourceMillis = date.getTime();
        final TimeZone renderingZone = legacyRenderingZone(zone, sourceMillis);
        final boolean pinned = hasJavaTimeRules(zone);
        final Calendar c = newProlepticGregorianCalendar(pinned ? new SimpleTimeZone(renderingZone.getOffset(sourceMillis), zone.getID()) : renderingZone);
        c.setLenient(false);
        c.setTime(date);

        final long resultMillis = setFieldMillis(c, calendarField, amount, date, zone, renderingZone, pinned);
        final T result = createDate(resultMillis, date);

        // A Timestamp carries precision below Calendar.MILLISECOND. Preserve that hidden fraction
        // when another field is set, but setting MILLISECOND defines the complete fractional second.
        return calendarField == Calendar.MILLISECOND ? clearSubMillis(result, resultMillis) : preserveSubMillis(result, date);
    }

    /**
     * Writes one field non-leniently and returns the resulting instant, resolved by
     * {@link #resolveCivilFieldsPreferringSourceOffset(Calendar, long, TimeZone, TimeZone)} so that a
     * daylight-saving overlap keeps the offset {@code source} was already on and a gap resolves forward; on a
     * pinned calendar ({@code pinned}) the field that was set - and the day of month {@code setYears}/
     * {@code setMonths} clamped - must survive that resolution, or the call throws: the contract is "the field
     * being set keeps the value given or the call throws", and the pinned calendar cannot see the zone's gaps.
     *
     * <p>It also replaces {@code Calendar}'s bare field-name rejection message with one that names the
     * operation: {@code Calendar} reports an out-of-range value as just {@code "MONTH"} or
     * {@code "DAY_OF_MONTH: 31 -> 2"}, which tells a caller of the public {@code set*} methods neither
     * which call failed nor what it passed.</p>
     */
    private static long setFieldMillis(final Calendar c, final int calendarField, final int amount, final java.util.Date source, final TimeZone zone,
            final TimeZone renderingZone, final boolean pinned) {
        try {
            int writtenDayOfMonth = -1;

            if (calendarField == Calendar.YEAR || calendarField == Calendar.MONTH) {
                // Changing the year or the month can leave a day-of-month the new month does not have
                // (29 February in a common year, 31 January moved to February). Clamp to the last valid
                // day, as LocalDate.withYear/withMonth do, rather than rolling into the next month.
                // Calendar's non-lenient check only re-validates fields the caller set, so YEAR would
                // otherwise let the overflow through silently while MONTH rejected it.
                final int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);

                c.set(Calendar.DAY_OF_MONTH, 1);

                if (calendarField == Calendar.YEAR) {
                    setProlepticYear(c, amount);
                } else {
                    c.set(Calendar.MONTH, amount);
                }

                writtenDayOfMonth = Math.min(dayOfMonth, c.getActualMaximum(Calendar.DAY_OF_MONTH));
                c.set(Calendar.DAY_OF_MONTH, writtenDayOfMonth);
            } else {
                //noinspection MagicConstant
                c.set(calendarField, amount);
            }

            final long resolved = resolveCivilFieldsPreferringSourceOffset(c, source.getTime(), zone, renderingZone);

            if (pinned) {
                requireWrittenFieldsIntact(resolved, calendarField, amount, writtenDayOfMonth,
                        source.getTime() >= LEGACY_ZONE_HISTORY_START ? zone : renderingZone);
            }

            return resolved;
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Dates." + setMethodName(calendarField) + "(date, " + amount + ") is not valid for " + exactInstant(source)
                    + " in time zone " + c.getTimeZone().getID() + ": " + e.getMessage(), e);
        }
    }

    /** Whether {@code zone} has rules a {@link ZoneId} can carry (see {@link #toZoneId(TimeZone)}). */
    private static boolean hasJavaTimeRules(final TimeZone zone) {
        try {
            toZoneId(zone);
            return true;
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * The "field being set keeps the value given or the call throws" rule, checked on the wall clock the resolved
     * instant shows in {@code rulesZone}: a wall clock a spring-forward gap removes was carried forward by the gap,
     * which is fine unless it moved the field the caller wrote (or the day of month {@code setYears}/{@code setMonths}
     * clamped to, which keeps a skipped calendar day - Apia 2011-12-30 - rejected). The message has the shape
     * {@code Calendar}'s non-lenient check used to produce, which {@link #setFieldMillis} wraps.
     */
    private static void requireWrittenFieldsIntact(final long resolvedMillis, final int calendarField, final int amount, final int writtenDayOfMonth,
            final TimeZone rulesZone) {
        final Instant instant = Instant.ofEpochMilli(resolvedMillis);
        final LocalDateTime got = LocalDateTime.ofInstant(instant, toZoneId(rulesZone).getRules().getOffset(instant));
        final int have;

        switch (calendarField) {
            case Calendar.YEAR:
                have = got.getYear();
                break;
            case Calendar.MONTH:
                have = got.getMonthValue() - 1;
                break;
            case Calendar.DAY_OF_MONTH:
                have = got.getDayOfMonth();
                break;
            case Calendar.HOUR_OF_DAY:
                have = got.getHour();
                break;
            case Calendar.MINUTE:
                have = got.getMinute();
                break;
            case Calendar.SECOND:
                have = got.getSecond();
                break;
            case Calendar.MILLISECOND:
                have = got.getNano() / 1_000_000;
                break;
            default:
                return;
        }

        if (have != amount) {
            // Calendar's own message names the field DAY_OF_MONTH, where fieldName() says DATE (the same constant).
            throw new IllegalArgumentException(
                    (calendarField == Calendar.DAY_OF_MONTH ? "DAY_OF_MONTH" : fieldName(calendarField)) + ": " + amount + " -> " + have);
        }

        if (writtenDayOfMonth >= 0 && got.getDayOfMonth() != writtenDayOfMonth) {
            throw new IllegalArgumentException("DAY_OF_MONTH: " + writtenDayOfMonth + " -> " + got.getDayOfMonth());
        }
    }

    /**
     * Writes {@code year} as the proleptic ISO year: 1 for 1 CE, 0 for 1 BCE and negative before that -
     * the scale {@link #civilYear(java.util.Date, TimeZone)}, {@code format} and {@link DTF} all read.
     *
     * <p>{@link Calendar#YEAR} is a year <i>of era</i>, so writing the caller's value straight into it
     * meant {@code setYears} silently changed meaning with the input's era: on a BCE value
     * {@code setYears(date, 2000)} produced 2000 BCE, no BCE value could be moved into the Common Era at
     * all, and year 0 was unreachable. Setting {@code ERA} alongside {@code YEAR} makes the argument mean
     * the same thing on both sides of 1 CE, and matches {@link LocalDate#withYear(int)}.</p>
     */
    private static void setProlepticYear(final Calendar c, final int year) {
        final long yearOfEra = year >= 1 ? year : 1L - year;

        if (yearOfEra > Integer.MAX_VALUE) {
            // Only reachable for year == Integer.MIN_VALUE, where 1 - year overflows int.
            throw new IllegalArgumentException("YEAR: " + year + " is outside the supported proleptic year range");
        }

        c.set(Calendar.ERA, year >= 1 ? GregorianCalendar.AD : GregorianCalendar.BC);
        c.set(Calendar.YEAR, (int) yearOfEra);
    }

    /** The public {@code set*} method a {@code Calendar} field belongs to, for diagnostics. */
    private static String setMethodName(final int calendarField) {
        switch (calendarField) {
            case Calendar.YEAR:
                return "setYears";
            case Calendar.MONTH:
                return "setMonths";
            case Calendar.DAY_OF_MONTH:
                return "setDays";
            case Calendar.HOUR_OF_DAY:
                return "setHours";
            case Calendar.MINUTE:
                return "setMinutes";
            case Calendar.SECOND:
                return "setSeconds";
            case Calendar.MILLISECOND:
                return "setMilliseconds";
            default:
                return "set(" + fieldName(calendarField) + ")";
        }
    }

    /**
     * Adds or subtracts the specified amount of time, expressed in the given
     * {@link TimeUnit}, to the supplied date and returns a new object of the same
     * concrete type. The original date is unchanged. The amount is converted to
     * milliseconds via an exact, overflow-throwing conversion ({@code Math.multiplyExact}, unlike
     * {@link TimeUnit#toMillis(long)} which saturates) and applied as plain
     * millisecond arithmetic (it does not account for daylight-saving-time
     * transitions).
     *
     * <p><b>Difference from {@link Calendar#roll(int, int)}:</b> despite its name, this method performs
     * plain <i>addition</i> that carries into larger fields &mdash; it does <b>not</b> have
     * {@code Calendar.roll} semantics, which change a single field without altering larger fields. For
     * example, adding 5 days to Jan 30 advances into February, whereas {@code Calendar.roll(DAY_OF_MONTH, 5)}
     * would wrap within January and leave the month unchanged. The field-specific {@code add*} methods are
     * the preferred replacements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * java.util.Date date = new java.util.Date(1736937045000L);   // the instant 2025-01-15T10:30:45Z
     * Dates.format(Dates.roll(date, 5, TimeUnit.DAYS), Dates.ISO_8601_DATE_TIME_FORMAT, utc);
     *                                                  // returns "2025-01-20T10:30:45Z" (+5 days)
     * Dates.format(Dates.roll(date, -2, TimeUnit.HOURS), Dates.ISO_8601_DATE_TIME_FORMAT, utc);
     *                                                  // returns "2025-01-15T08:30:45Z" (-2 hours)
     *
     * // the recommended replacement, same result
     * Dates.format(Dates.addHours(date, -2), Dates.ISO_8601_DATE_TIME_FORMAT, utc);
     *                                                  // returns "2025-01-15T08:30:45Z"
     * Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, utc);   // returns "2025-01-15T10:30:45Z" (original unchanged)
     * Dates.roll(date, 1, (TimeUnit) null);                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the concrete {@code java.util.Date} subtype of {@code date} which is also the return type.
     * @param date the date to add to, must not be {@code null}.
     * @param amount the amount of time to add or subtract (negative values subtract).
     * @param unit the time unit of the {@code amount} parameter, must not be {@code null}.
     * @return a new date of the same type as {@code date} with the specified amount applied.
     * @throws IllegalArgumentException if {@code date} or {@code unit} is {@code null}.
     * @throws ArithmeticException if conversion to milliseconds or the resulting epoch-millisecond value overflows a {@code long}.
     * @deprecated misleadingly named (it adds, it does not {@link Calendar#roll(int, int) roll}); use the
     *             field-specific {@code add*} methods instead, e.g. {@link #addHours(java.util.Date, int)},
     *             {@link #addMinutes(java.util.Date, int)}, {@link #addSeconds(java.util.Date, int)},
     *             {@link #addMilliseconds(java.util.Date, int)}, which are exact replacements for
     *             {@code MILLISECONDS} through {@code HOURS} (and now have {@code long} overloads);
     *             {@code NANOSECONDS} and {@code MICROSECONDS} have no replacement (this method truncates
     *             them to whole milliseconds). Note {@link #addDays(java.util.Date, int)} is
     *             <i>not</i> equivalent for {@code TimeUnit.DAYS}: it uses daylight-saving-aware calendar
     *             arithmetic, so it can differ across DST transitions, and it takes an {@code int} amount.
     */
    @Beta
    @Deprecated
    public static <T extends java.util.Date> T roll(final T date, final long amount, final TimeUnit unit) throws IllegalArgumentException, ArithmeticException {
        return addToDate(date, amount, unit);
    }

    /**
     * Adds or subtracts the specified amount of the given {@link CalendarField}
     * to the supplied date and returns a new object of the same concrete type.
     * The original date is unchanged. For {@code MONTH}, {@code YEAR},
     * {@code DAY_OF_MONTH} and {@code WEEK_OF_YEAR} the field is walked on the
     * calendar fields and the result resolved through the zone's rules exactly
     * as the {@code add*} methods do (a gap resolves forward, an overlap keeps
     * the source offset); for finer fields plain millisecond arithmetic is used.
     *
     * <p><b>Difference from {@link Calendar#roll(int, int)}:</b> despite its name, this method performs
     * plain <i>addition</i> that carries into larger fields &mdash; it does <b>not</b> have
     * {@code Calendar.roll} semantics, which change a single field without altering larger fields. For
     * example, adding 2 months to Nov 15 advances the year into the next January, whereas
     * {@code Calendar.roll(MONTH, 2)} would wrap within the same year. The field-specific {@code add*}
     * methods are the preferred replacements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * java.util.Date date = new java.util.Date(1736937045000L);   // the instant 2025-01-15T10:30:45Z
     * Dates.format(Dates.roll(date, 5, CalendarField.DAY_OF_MONTH), Dates.ISO_8601_DATE_TIME_FORMAT, utc);
     *                                                  // returns "2025-01-20T10:30:45Z" (+5 days)
     * // -2 months carries into the previous year; Calendar.roll would wrap within 2025
     * Dates.format(Dates.roll(date, -2, CalendarField.MONTH), Dates.ISO_8601_DATE_TIME_FORMAT, utc);
     *                                                  // returns "2024-11-15T10:30:45Z"
     *
     * // the recommended replacement, same result
     * Dates.format(Dates.addMonths(date, -2), Dates.ISO_8601_DATE_TIME_FORMAT, utc);
     *                                                  // returns "2024-11-15T10:30:45Z"
     * Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, utc);         // returns "2025-01-15T10:30:45Z" (original unchanged)
     * Dates.roll(date, 1, (CalendarField) null);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the concrete {@code java.util.Date} subtype of {@code date} which is also the return type.
     * @param date the date to add to, must not be {@code null}.
     * @param amount the amount to add or subtract (negative values subtract).
     * @param unit the calendar field unit to add by, must not be {@code null}. Accepted values: every
     *        {@link CalendarField} constant, including {@code WEEK_OF_YEAR} (calendar-aware week addition).
     * @return a new date of the same type as {@code date} with the specified amount applied.
     * @throws IllegalArgumentException if {@code date} or {@code unit} is {@code null}.
     * @throws ArithmeticException if the resulting epoch-millisecond value overflows a {@code long}.
     * @deprecated misleadingly named (it adds, it does not {@link Calendar#roll(int, int) roll}); use the
     *             field-specific {@code add*} methods instead, e.g. {@link #addYears(java.util.Date, int)},
     *             {@link #addMonths(java.util.Date, int)}, {@link #addWeeks(java.util.Date, int)},
     *             {@link #addDays(java.util.Date, int)}.
     */
    @Beta
    @Deprecated
    public static <T extends java.util.Date> T roll(final T date, final int amount, final CalendarField unit)
            throws IllegalArgumentException, ArithmeticException {
        return addToDate(date, amount, unit);
    }

    /**
     * Adds or subtracts the specified amount of time, expressed in the given
     * {@link TimeUnit}, to the supplied calendar and returns a new calendar of
     * the same concrete type. The original calendar is unchanged. The amount is
     * converted to milliseconds via an exact, overflow-throwing conversion ({@code Math.multiplyExact},
     * unlike {@link TimeUnit#toMillis(long)} which saturates) and applied
     * as plain millisecond arithmetic (it does not account for daylight-saving-time
     * transitions).
     *
     * <p><b>Difference from {@link Calendar#roll(int, int)}:</b> despite its name, this method performs
     * plain <i>addition</i> that carries into larger fields &mdash; it does <b>not</b> have
     * {@code Calendar.roll} semantics, which change a single field without altering larger fields. The
     * field-specific {@code add*} methods are the preferred replacements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(1736937045000L);    // calendar at the 2025-01-15T10:30:45Z instant (default zone)
     * Dates.roll(cal, 5, TimeUnit.DAYS).getTimeInMillis();    // returns 1737369045000 (+5 days of elapsed time)
     * Dates.roll(cal, -2, TimeUnit.HOURS).getTimeInMillis();  // returns 1736929845000 (-2 hours)
     *
     * // the recommended replacement, same result
     * Dates.addHours(cal, -2).getTimeInMillis();             // returns 1736929845000
     * cal.getTimeInMillis();                                 // returns 1736937045000 (original unchanged)
     * Dates.roll(cal, 1, (TimeUnit) null);                   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the concrete {@code Calendar} subtype of {@code calendar} which is also the return type.
     * @param calendar the calendar to add to, must not be {@code null}.
     * @param amount the amount of time to add or subtract (negative values subtract).
     * @param unit the time unit of the {@code amount} parameter, must not be {@code null}.
     * @return a new calendar of the same type as {@code calendar} with the specified amount applied.
     * @throws IllegalArgumentException if {@code calendar} or {@code unit} is {@code null}.
     * @throws ArithmeticException if conversion to milliseconds or the resulting epoch-millisecond value overflows a {@code long}.
     * @deprecated misleadingly named (it adds, it does not {@link Calendar#roll(int, int) roll}); use the
     *             field-specific {@code add*} methods instead, e.g. {@link #addHours(Calendar, int)},
     *             {@link #addMinutes(Calendar, int)}, {@link #addSeconds(Calendar, int)},
     *             {@link #addMilliseconds(Calendar, int)}, which are exact replacements for
     *             {@code MILLISECONDS} through {@code HOURS} (and now have {@code long} overloads);
     *             {@code NANOSECONDS} and {@code MICROSECONDS} have no replacement (this method truncates
     *             them to whole milliseconds). Note {@link #addDays(Calendar, int)} is <i>not</i> equivalent
     *             for {@code TimeUnit.DAYS}: it uses daylight-saving-aware day arithmetic, so it can differ
     *             across DST transitions, and it takes an {@code int} amount.
     */
    @Beta
    @Deprecated
    public static <T extends Calendar> T roll(final T calendar, final long amount, final TimeUnit unit) throws IllegalArgumentException, ArithmeticException {
        return addToCalendar(calendar, amount, unit);
    }

    /**
     * Adds or subtracts the specified amount of the given {@link CalendarField}
     * to the supplied calendar and returns a new calendar of the same concrete
     * type. The original calendar is unchanged. A civil field ({@code YEAR}, {@code MONTH},
     * {@code WEEK_OF_YEAR}, {@code DAY_OF_MONTH}) is walked on the calendar fields and the result resolved
     * through the zone's rules exactly as the {@code add*} methods do (the calendar's own settings and cutover
     * are not used); a finer field adds elapsed time.
     *
     * <p><b>Difference from {@link Calendar#roll(int, int)}:</b> despite its name, this method performs
     * plain <i>addition</i> that carries into larger fields, exactly as the {@code add*} methods do &mdash; it
     * does <b>not</b> have {@code Calendar.roll} semantics, which change a single field without altering
     * larger fields. The field-specific {@code add*} methods are the preferred replacements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);   // the instant 2025-01-15T10:30:45Z (UTC, no DST)
     * Dates.format(Dates.roll(cal, 5, CalendarField.DAY_OF_MONTH), "yyyy-MM-dd HH:mm:ss", utc);
     *                                                  // returns "2025-01-20 10:30:45" (+5 days)
     * Dates.format(Dates.roll(cal, -2, CalendarField.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss", utc);
     *                                                  // returns "2025-01-15 08:30:45" (-2 hours)
     *
     * // the recommended replacement, same result
     * Dates.format(Dates.addHours(cal, -2), "yyyy-MM-dd HH:mm:ss", utc);
     *                                                  // returns "2025-01-15 08:30:45"
     * cal.getTimeInMillis();                                      // returns 1736937045000 (original unchanged)
     * Dates.roll(cal, 1, (CalendarField) null);                   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the concrete {@code Calendar} subtype of {@code calendar} which is also the return type.
     * @param calendar the calendar to add to, must not be {@code null}.
     * @param amount the amount to add or subtract (negative values subtract).
     * @param unit the calendar field unit to add by, must not be {@code null}. Accepted values: every
     *        {@link CalendarField} constant, including {@code WEEK_OF_YEAR} (calendar-aware week addition).
     *        Date-field arithmetic runs on a proleptic Gregorian calendar in {@code calendar}'s own zone,
     *        whatever calendar system and cutover {@code calendar} itself carries.
     * @return a new calendar of the same type as {@code calendar} with the specified amount applied.
     * @throws IllegalArgumentException if {@code calendar} or {@code unit} is {@code null}.
     * @throws ArithmeticException if the resulting epoch-millisecond value overflows a {@code long}.
     * @deprecated misleadingly named (it adds, it does not {@link Calendar#roll(int, int) roll}); use the
     *             field-specific {@code add*} methods instead, e.g. {@link #addYears(Calendar, int)},
     *             {@link #addMonths(Calendar, int)}, {@link #addWeeks(Calendar, int)},
     *             {@link #addDays(Calendar, int)}.
     */
    @Beta
    @Deprecated
    public static <T extends Calendar> T roll(final T calendar, final int amount, final CalendarField unit)
            throws IllegalArgumentException, ArithmeticException {
        return addToCalendar(calendar, amount, unit);
    }

    //-----------------------------------------------------------------------

    // Companion to toMillisExact(long, TimeUnit) for the CalendarField-based add path. Only the
    // sub-day fields are reachable: the caller routes DAY_OF_MONTH/WEEK_OF_YEAR/MONTH/YEAR through
    // addCivilFieldMillis (a field walk resolved through the zone's rules), so those branches would be
    // dead code here. The amount is taken as long so the multiplication cannot overflow int before widening.
    private static long toMillis(final long amount, final CalendarField field) {
        switch (field) {
            case MILLISECOND:
                return amount;

            case SECOND:
                return amount * 1_000L;

            case MINUTE:
                return amount * 60_000L;

            case HOUR_OF_DAY:
                return amount * 3_600_000L;

            default:
                throw new IllegalArgumentException("Unsupported unit: " + field);
        }
    }

    //-----------------------------------------------------------------------

    // Internal implementation of the add* family (and the deprecated roll* family). Kept private so the
    // public add* methods never delegate through the deprecated roll* methods.

    /**
     * @throws IllegalArgumentException if {@code date} or {@code unit} is {@code null}.
     * @throws ArithmeticException if conversion or date arithmetic overflows.
     * @throws IllegalStateException if construction of the result violates the required runtime type, distinct-instance, or requested-instant contract.
     */
    private static <T extends java.util.Date> T addToDate(final T date, final long amount, final TimeUnit unit)
            throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(unit, cs.unit);

        return preserveSubMillis(createDate(Math.addExact(date.getTime(), toMillisExact(amount, unit)), date), date);
    }

    /**
     * @throws IllegalArgumentException if {@code date} or {@code unit} is {@code null}, or the calendar field is unsupported.
     * @throws ArithmeticException if conversion or date arithmetic overflows.
     * @throws IllegalStateException if construction of the result violates the required runtime type, distinct-instance, or requested-instant contract.
     */
    private static <T extends java.util.Date> T addToDate(final T date, final int amount, final CalendarField unit)
            throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(unit, cs.unit);

        // DAY_OF_MONTH and WEEK_OF_YEAR also need calendar-rule arithmetic so that crossing a
        // DST boundary preserves wall-clock time of day. Pre-fix the millisecond-arithmetic
        // path treated a day as exactly 86_400_000ms, so addDays(d, 1) on a spring-forward day
        // landed an hour off in zones that observe DST.
        if (isCivilAddField(unit)) {
            return preserveSubMillis(createDate(addCivilFieldMillis(date.getTime(), TimeZone.getDefault(), amount, unit), date), date);
        } else {
            return preserveSubMillis(createDate(Math.addExact(date.getTime(), toMillis(amount, unit)), date), date);
        }
    }

    /**
     * @throws IllegalArgumentException if {@code calendar} or {@code unit} is {@code null}.
     * @throws ArithmeticException if conversion or calendar arithmetic overflows.
     * @throws IllegalStateException if construction of the result violates the required runtime type, distinct-instance, or requested-instant contract.
     */
    private static <T extends Calendar> T addToCalendar(final T calendar, final long amount, final TimeUnit unit)
            throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        N.checkArgNotNull(calendar, cs.calendar); //NOSONAR
        N.checkArgNotNull(unit, cs.unit);

        return createCalendar(calendar, Math.addExact(calendar.getTimeInMillis(), toMillisExact(amount, unit)));
    }

    /**
     * @throws IllegalArgumentException if {@code calendar} or {@code unit} is {@code null}, or the calendar field is unsupported.
     * @throws ArithmeticException if conversion or calendar arithmetic overflows.
     * @throws IllegalStateException if construction of the result violates the required runtime type, distinct-instance, or requested-instant contract.
     */
    private static <T extends Calendar> T addToCalendar(final T calendar, final int amount, final CalendarField unit)
            throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        N.checkArgNotNull(calendar, cs.calendar);
        N.checkArgNotNull(unit, cs.unit);

        final long millis = calendar.getTimeInMillis();
        final TimeZone zone = zoneOf(calendar);

        if (isCivilAddField(unit)) {
            return createCalendar(calendar, addCivilFieldMillis(millis, zone, amount, unit));
        }

        // HOUR_OF_DAY and finer are a fixed elapsed duration, so they must not be re-resolved as civil
        // fields: addHours(c, 24) across a spring-forward is 24 elapsed hours, which is a different wall
        // clock, exactly as the java.util.Date overloads define it - and exactly what they compute, so the
        // same millisecond arithmetic is used here (a work calendar gave the identical result at more cost).
        return createCalendar(calendar, Math.addExact(millis, toMillis(amount, unit)));
    }

    /**
     * Adds a calendar field without allowing {@link Calendar#add(int, int)} to wrap the signed-long
     * epoch range. A single call with an enormous year amount can wrap more than once and finish on the
     * expected side of the starting instant, so amounts are split into bounded chunks. For Gregorian
     * calendars, 100 million years are less than half of the complete signed-long millisecond range;
     * therefore any wrap in one chunk necessarily reverses direction and is detectable. Smaller fields
     * are bounded by the same conservative chunk size. Equal instants are allowed rather than treated as
     * a wrap: the work calendar is pinned to a fixed offset, so no non-zero chunk yields one, and the
     * check stays a pure direction test.
     */
    private static void addCalendarFieldExact(final Calendar calendar, final int amount, final CalendarField unit) {
        // Month chunks span complete 400-year Gregorian cycles so intermediate additions cannot
        // clamp the day (including February 29) before the final target month is reached. The
        // slightly smaller bound retains the direction-based overflow check below.
        final int maxChunk = unit == CalendarField.MONTH ? 99_998_400 : 100_000_000;
        // The instant the caller passed, not the running cursor: the amount is applied in chunks, so
        // reporting beforeMillis named an intermediate value the caller never supplied.
        final long startMillis = calendar.getTimeInMillis();
        long remaining = amount;

        while (remaining != 0) {
            final int chunk = (int) Math.max(-maxChunk, Math.min(maxChunk, remaining));
            final long beforeMillis = calendar.getTimeInMillis();

            //noinspection MagicConstant
            calendar.add(unit.value(), chunk);

            final long afterMillis = calendar.getTimeInMillis();

            if ((chunk > 0 && afterMillis < beforeMillis) || (chunk < 0 && afterMillis > beforeMillis)) {
                throw new ArithmeticException(
                        "Date-time arithmetic overflow: adding " + amount + " " + unit + " to epoch millis " + startMillis + " wrapped the supported range");
            }

            remaining -= chunk;
        }
    }

    /**
     * Adds a number of years to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p>Uses calendar arithmetic, so the wall-clock time of day is preserved across a daylight-saving
     * transition. A wall clock a spring-forward gap removes moves forward by the length of the gap, and one
     * an autumn overlap repeats keeps the offset the input was already on (the earlier occurrence when the
     * input was on neither), so every {@code add*} field names the same instant for the same civil target
     * and agrees with the corresponding {@link ZonedDateTime} {@code plus} method.</p>
     *
     * <p>This overload evaluates in the live JVM default time zone; use the {@code Calendar} overloads,
     * which evaluate in the calendar's own zone, to control it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // parse and format in the same (default) zone so the wall-clock result is stable
     * java.util.Date date = Dates.parseToJUDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.addYears(date, 1), "yyyy-MM-dd HH:mm:ss");    // returns "2026-01-15 10:30:45"
     * Dates.format(Dates.addYears(date, -1), "yyyy-MM-dd HH:mm:ss");   // returns "2024-01-15 10:30:45"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                       // returns "2025-01-15 10:30:45" (original unchanged)
     * Dates.addYears((java.util.Date) null, 1);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add years to, not {@code null}.
     * @param amount the amount of years to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of years added.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if the resulting instant is outside the signed-long epoch-millisecond range.
     */
    public static <T extends java.util.Date> T addYears(final T date, final int amount) throws IllegalArgumentException, ArithmeticException {
        return addToDate(date, amount, CalendarField.YEAR);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of months to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p>Uses calendar arithmetic, so the wall-clock time of day is preserved across a daylight-saving
     * transition. A wall clock a spring-forward gap removes moves forward by the length of the gap, and one
     * an autumn overlap repeats keeps the offset the input was already on (the earlier occurrence when the
     * input was on neither), so every {@code add*} field names the same instant for the same civil target
     * and agrees with the corresponding {@link ZonedDateTime} {@code plus} method.</p>
     *
     * <p>This overload evaluates in the live JVM default time zone; use the {@code Calendar} overloads,
     * which evaluate in the calendar's own zone, to control it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.addMonths(date, 3), "yyyy-MM-dd HH:mm:ss");    // returns "2025-04-15 10:30:45"
     * Dates.format(Dates.addMonths(date, -2), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-15 10:30:45"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                        // returns "2025-01-15 10:30:45" (original unchanged)
     * Dates.addMonths((java.util.Date) null, 3);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add months to, not {@code null}.
     * @param amount the amount of months to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of months added.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if the resulting instant is outside the signed-long epoch-millisecond range.
     */
    public static <T extends java.util.Date> T addMonths(final T date, final int amount) throws IllegalArgumentException, ArithmeticException {
        return addToDate(date, amount, CalendarField.MONTH);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of weeks to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p>Uses calendar arithmetic, so the wall-clock time of day is preserved across a daylight-saving
     * transition. A wall clock a spring-forward gap removes moves forward by the length of the gap, and one
     * an autumn overlap repeats keeps the offset the input was already on (the earlier occurrence when the
     * input was on neither), so every {@code add*} field names the same instant for the same civil target
     * and agrees with the corresponding {@link ZonedDateTime} {@code plus} method.</p>
     *
     * <p>This overload evaluates in the live JVM default time zone; use the {@code Calendar} overloads,
     * which evaluate in the calendar's own zone, to control it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.addWeeks(date, 2), "yyyy-MM-dd HH:mm:ss");    // returns "2025-01-29 10:30:45"
     * Dates.format(Dates.addWeeks(date, -1), "yyyy-MM-dd HH:mm:ss");   // returns "2025-01-08 10:30:45"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                       // returns "2025-01-15 10:30:45" (original unchanged)
     * Dates.addWeeks((java.util.Date) null, 2);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add weeks to, not {@code null}.
     * @param amount the amount of weeks to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of weeks added.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if the resulting instant is outside the signed-long epoch-millisecond range.
     */
    public static <T extends java.util.Date> T addWeeks(final T date, final int amount) throws IllegalArgumentException, ArithmeticException {
        return addToDate(date, amount, CalendarField.WEEK_OF_YEAR);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of days to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p>Uses daylight-saving-aware calendar arithmetic: the wall-clock time of day is preserved, so the
     * epoch difference across a daylight-saving transition may differ from 24 hours by the size of the shift
     * (23 or 25 hours for the usual one-hour move; {@code Australia/Lord_Howe} shifts 30 minutes, giving 23.5
     * or 24.5 hours). This means {@code addDays(d, 1)} is <i>not</i> always equivalent to
     * {@code addHours(d, 24)} &mdash; the {@code addHours}/{@code addMinutes}/{@code addSeconds}/
     * {@code addMilliseconds} methods use plain millisecond arithmetic. A wall clock a spring-forward gap
     * removes moves forward by the length of the gap, and one an autumn overlap repeats keeps the offset
     * the input was already on (the earlier occurrence when the input was on neither), so every
     * {@code add*} field names the same instant for the same civil target and agrees with
     * {@link ZonedDateTime#plusDays(long)}.</p>
     *
     * <p>This overload evaluates in the live JVM default time zone; use the {@code Calendar} overloads,
     * which evaluate in the calendar's own zone, to control it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.addDays(date, 1), "yyyy-MM-dd HH:mm:ss");    // returns "2025-01-16 10:30:45"
     * Dates.format(Dates.addDays(date, -5), "yyyy-MM-dd HH:mm:ss");   // returns "2025-01-10 10:30:45"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                      // returns "2025-01-15 10:30:45" (original unchanged)
     * Dates.addDays((java.util.Date) null, 1);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add days to, not {@code null}.
     * @param amount the amount of days to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of days added.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if the resulting instant is outside the signed-long epoch-millisecond range.
     */
    public static <T extends java.util.Date> T addDays(final T date, final int amount) throws IllegalArgumentException, ArithmeticException {
        return addToDate(date, amount, CalendarField.DAY_OF_MONTH);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of hours to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p>Uses plain millisecond arithmetic ({@code amount * 3_600_000}): the result is exactly
     * {@code amount} hours later in epoch terms, even across daylight-saving transitions &mdash;
     * unlike {@link #addDays(java.util.Date, int)}, which preserves wall-clock time of day.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     *
     * // hour/minute/second arithmetic is plain millisecond math (UTC-stable)
     * java.util.Date date = new java.util.Date(1736937045000L);                       // the instant 2025-01-15T10:30:45Z
     *
     * Dates.format(Dates.addHours(date, 5), Dates.ISO_8601_DATE_TIME_FORMAT, utc);    // returns "2025-01-15T15:30:45Z"
     * Dates.format(Dates.addHours(date, -3), Dates.ISO_8601_DATE_TIME_FORMAT, utc);   // returns "2025-01-15T07:30:45Z"
     *
     * Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, utc);                       // returns "2025-01-15T10:30:45Z" (original unchanged)
     * Dates.addHours((java.util.Date) null, 3);                                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add hours to, not {@code null}.
     * @param amount the amount of hours to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of hours added.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if the resulting epoch-millisecond value overflows a {@code long}.
     */
    public static <T extends java.util.Date> T addHours(final T date, final int amount) throws IllegalArgumentException, ArithmeticException {
        return addToDate(date, amount, CalendarField.HOUR_OF_DAY);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of minutes to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * java.util.Date date = new java.util.Date(1736937045000L);                          // the instant 2025-01-15T10:30:45Z
     * Dates.format(Dates.addMinutes(date, 30), Dates.ISO_8601_DATE_TIME_FORMAT, utc);    // returns "2025-01-15T11:00:45Z"
     * Dates.format(Dates.addMinutes(date, -15), Dates.ISO_8601_DATE_TIME_FORMAT, utc);   // returns "2025-01-15T10:15:45Z"
     *
     * Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, utc);                          // returns "2025-01-15T10:30:45Z" (original unchanged)
     * Dates.addMinutes((java.util.Date) null, 30);                                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add minutes to, not {@code null}.
     * @param amount the amount of minutes to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of minutes added.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if the resulting epoch-millisecond value overflows a {@code long}.
     */
    public static <T extends java.util.Date> T addMinutes(final T date, final int amount) throws IllegalArgumentException, ArithmeticException {
        return addToDate(date, amount, CalendarField.MINUTE);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of seconds to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * java.util.Date date = new java.util.Date(1736937045000L);                          // the instant 2025-01-15T10:30:45Z
     * Dates.format(Dates.addSeconds(date, 15), Dates.ISO_8601_DATE_TIME_FORMAT, utc);    // returns "2025-01-15T10:31:00Z"
     * Dates.format(Dates.addSeconds(date, -45), Dates.ISO_8601_DATE_TIME_FORMAT, utc);   // returns "2025-01-15T10:30:00Z"
     *
     * Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, utc);                          // returns "2025-01-15T10:30:45Z" (original unchanged)
     * Dates.addSeconds((java.util.Date) null, 45);                                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add seconds to, not {@code null}.
     * @param amount the amount of seconds to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of seconds added.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if the resulting epoch-millisecond value overflows a {@code long}.
     */
    public static <T extends java.util.Date> T addSeconds(final T date, final int amount) throws IllegalArgumentException, ArithmeticException {
        return addToDate(date, amount, CalendarField.SECOND);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of milliseconds to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = new java.util.Date(1736937045000L);   // the instant 2025-01-15T10:30:45Z
     * Dates.addMilliseconds(date, 500).getTime();                 // returns 1736937045500
     * Dates.addMilliseconds(date, -1000).getTime();               // returns 1736937044000
     *
     * date.getTime();                                             // returns 1736937045000 (original unchanged)
     * Dates.addMilliseconds((java.util.Date) null, 500);          // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add milliseconds to, not {@code null}.
     * @param amount the amount of milliseconds to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of milliseconds added.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if the resulting epoch-millisecond value overflows a {@code long}.
     */
    public static <T extends java.util.Date> T addMilliseconds(final T date, final int amount) throws IllegalArgumentException, ArithmeticException {
        return addToDate(date, amount, CalendarField.MILLISECOND);
    }

    /**
     * Adds a number of hours to a date returning a new object, accepting a {@code long} amount.
     * The original {@code Date} is unchanged.
     *
     * <p>Uses plain millisecond arithmetic ({@code amount * 3_600_000}): the result is exactly
     * {@code amount} hours later in epoch terms, even across daylight-saving transitions &mdash;
     * unlike {@link #addDays(java.util.Date, int)}, which preserves wall-clock time of day.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * java.util.Date date = new java.util.Date(1736937045000L);   // the instant 2025-01-15T10:30:45Z
     * Dates.format(Dates.addHours(date, 3L), Dates.ISO_8601_DATE_TIME_FORMAT, utc);
     *                                                  // returns "2025-01-15T13:30:45Z" (+3 hours)
     * Dates.format(Dates.addHours(date, -2L), Dates.ISO_8601_DATE_TIME_FORMAT, utc);
     *                                                  // returns "2025-01-15T08:30:45Z" (-2 hours)
     *
     * Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, utc);   // returns "2025-01-15T10:30:45Z" (original unchanged)
     * Dates.addHours((java.util.Date) null, 3L);                  // throws IllegalArgumentException
     * Dates.addHours(date, Long.MAX_VALUE);                       // throws ArithmeticException (epoch overflow)
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add hours to, not {@code null}.
     * @param amount the amount of hours to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of hours added.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if conversion to milliseconds or the resulting epoch-millisecond value overflows a {@code long}.
     * @see #addHours(java.util.Date, int)
     */
    public static <T extends java.util.Date> T addHours(final T date, final long amount) throws IllegalArgumentException, ArithmeticException {
        return addToDate(date, amount, TimeUnit.HOURS);
    }

    /**
     * Adds a number of minutes to a date returning a new object, accepting a {@code long} amount.
     * The original {@code Date} is unchanged.
     *
     * <p>Uses plain millisecond arithmetic ({@code amount * 60_000}); see
     * {@link #addHours(java.util.Date, long)} for the daylight-saving implications.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * java.util.Date date = new java.util.Date(1736937045000L);   // the instant 2025-01-15T10:30:45Z
     * Dates.format(Dates.addMinutes(date, 90L), Dates.ISO_8601_DATE_TIME_FORMAT, utc);
     *                                                  // returns "2025-01-15T12:00:45Z" (+90 minutes)
     * Dates.format(Dates.addMinutes(date, -30L), Dates.ISO_8601_DATE_TIME_FORMAT, utc);
     *                                                  // returns "2025-01-15T10:00:45Z" (-30 minutes)
     *
     * Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, utc);   // returns "2025-01-15T10:30:45Z" (original unchanged)
     * Dates.addMinutes((java.util.Date) null, 90L);               // throws IllegalArgumentException
     * Dates.addMinutes(date, Long.MAX_VALUE);                     // throws ArithmeticException (epoch overflow)
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add minutes to, not {@code null}.
     * @param amount the amount of minutes to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of minutes added.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if conversion to milliseconds or the resulting epoch-millisecond value overflows a {@code long}.
     * @see #addMinutes(java.util.Date, int)
     */
    public static <T extends java.util.Date> T addMinutes(final T date, final long amount) throws IllegalArgumentException, ArithmeticException {
        return addToDate(date, amount, TimeUnit.MINUTES);
    }

    /**
     * Adds a number of seconds to a date returning a new object, accepting a {@code long} amount.
     * The original {@code Date} is unchanged.
     *
     * <p>Uses plain millisecond arithmetic ({@code amount * 1_000}); see
     * {@link #addHours(java.util.Date, long)} for the daylight-saving implications.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * java.util.Date date = new java.util.Date(1736937045000L);   // the instant 2025-01-15T10:30:45Z
     * Dates.format(Dates.addSeconds(date, 15L), Dates.ISO_8601_DATE_TIME_FORMAT, utc);
     *                                                  // returns "2025-01-15T10:31:00Z" (+15 seconds)
     * Dates.format(Dates.addSeconds(date, -45L), Dates.ISO_8601_DATE_TIME_FORMAT, utc);
     *                                                  // returns "2025-01-15T10:30:00Z" (-45 seconds)
     *
     * Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, utc);   // returns "2025-01-15T10:30:45Z" (original unchanged)
     * Dates.addSeconds((java.util.Date) null, 15L);               // throws IllegalArgumentException
     * Dates.addSeconds(date, Long.MAX_VALUE);                     // throws ArithmeticException (epoch overflow)
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add seconds to, not {@code null}.
     * @param amount the amount of seconds to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of seconds added.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if conversion to milliseconds or the resulting epoch-millisecond value overflows a {@code long}.
     * @see #addSeconds(java.util.Date, int)
     */
    public static <T extends java.util.Date> T addSeconds(final T date, final long amount) throws IllegalArgumentException, ArithmeticException {
        return addToDate(date, amount, TimeUnit.SECONDS);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of milliseconds to a date returning a new object, accepting a {@code long} amount.
     * The original {@code Date} is unchanged.
     *
     * <p>This {@code long} overload complements {@link #addMilliseconds(java.util.Date, int)}: a millisecond
     * count near {@code 2^31} (about 24.8 days) overflows an {@code int}, so use this overload when the amount
     * may exceed the {@code int} range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = new java.util.Date(1736937045000L);      // the instant 2025-01-15T10:30:45Z
     * Dates.addMilliseconds(date, 5_000_000_000L).getTime();         // returns 1741937045000 (~57.9 days later)
     * Dates.addMilliseconds(date, -5_000_000_000L).getTime();        // returns 1731937045000 (~57.9 days earlier)
     *
     * date.getTime();                                                // returns 1736937045000 (original unchanged)
     * Dates.addMilliseconds((java.util.Date) null, 1L);              // throws IllegalArgumentException
     * Dates.addMilliseconds(date, Long.MAX_VALUE);                   // throws ArithmeticException (epoch overflow)
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add milliseconds to, not {@code null}.
     * @param amount the amount of milliseconds to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of milliseconds added.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if the resulting epoch-millisecond value overflows a {@code long}.
     */
    public static <T extends java.util.Date> T addMilliseconds(final T date, final long amount) throws IllegalArgumentException, ArithmeticException {
        return addToDate(date, amount, TimeUnit.MILLISECONDS);
    }

    /**
     * Adds a number of years to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p>The arithmetic runs on a proleptic Gregorian calendar in {@code calendar}'s own time zone,
     * whatever calendar system and Julian/Gregorian cutover {@code calendar} itself carries, so the
     * result always matches the civil date {@link #format(Calendar)} prints &mdash; the same rule
     * {@link #round(Calendar, int)}, {@link #truncate(Calendar, int)} and {@link #ceiling(Calendar, int)}
     * follow. The result keeps {@code calendar}'s runtime type, zone and settings.</p>
     *
     * <p>The wall-clock time of day is preserved across a daylight-saving transition. A wall clock a
     * spring-forward gap removes moves forward by the length of the gap, and one an autumn overlap repeats
     * keeps the offset the input was already on (the earlier occurrence when the input was on neither), so
     * every {@code add*} field names the same instant for the same civil target.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);            // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addYears(cal, 1), "yyyy-MM-dd HH:mm:ss", utc);    // returns "2026-01-15 10:30:45"
     * Dates.format(Dates.addYears(cal, -1), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2024-01-15 10:30:45"
     *
     * cal.getTimeInMillis();                                               // returns 1736937045000 (original unchanged)
     * Dates.addYears((Calendar) null, 1);                                  // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add years to, not {@code null}.
     * @param amount the amount of years to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of years added.
     * @throws IllegalArgumentException if the calendar is {@code null}.
     * @throws ArithmeticException if the resulting instant is outside the signed-long epoch-millisecond range.
     */
    public static <T extends Calendar> T addYears(final T calendar, final int amount) throws IllegalArgumentException, ArithmeticException {
        return addToCalendar(calendar, amount, CalendarField.YEAR);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of months to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p>The arithmetic runs on a proleptic Gregorian calendar in {@code calendar}'s own time zone,
     * whatever calendar system and Julian/Gregorian cutover {@code calendar} itself carries, so the
     * result always matches the civil date {@link #format(Calendar)} prints &mdash; the same rule
     * {@link #round(Calendar, int)}, {@link #truncate(Calendar, int)} and {@link #ceiling(Calendar, int)}
     * follow. The result keeps {@code calendar}'s runtime type, zone and settings.</p>
     *
     * <p>The wall-clock time of day is preserved across a daylight-saving transition. A wall clock a
     * spring-forward gap removes moves forward by the length of the gap, and one an autumn overlap repeats
     * keeps the offset the input was already on (the earlier occurrence when the input was on neither), so
     * every {@code add*} field names the same instant for the same civil target.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);             // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addMonths(cal, 6), "yyyy-MM-dd HH:mm:ss", utc);    // returns "2025-07-15 10:30:45"
     * Dates.format(Dates.addMonths(cal, -2), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2024-11-15 10:30:45"
     *
     * cal.getTimeInMillis();                                                // returns 1736937045000 (original unchanged)
     * Dates.addMonths((Calendar) null, 6);                                  // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add months to, not {@code null}.
     * @param amount the amount of months to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of months added.
     * @throws IllegalArgumentException if the calendar is {@code null}.
     * @throws ArithmeticException if the resulting instant is outside the signed-long epoch-millisecond range.
     */
    public static <T extends Calendar> T addMonths(final T calendar, final int amount) throws IllegalArgumentException, ArithmeticException {
        return addToCalendar(calendar, amount, CalendarField.MONTH);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of weeks to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p>The arithmetic runs on a proleptic Gregorian calendar in {@code calendar}'s own time zone,
     * whatever calendar system and Julian/Gregorian cutover {@code calendar} itself carries, so the
     * result always matches the civil date {@link #format(Calendar)} prints &mdash; the same rule
     * {@link #round(Calendar, int)}, {@link #truncate(Calendar, int)} and {@link #ceiling(Calendar, int)}
     * follow. The result keeps {@code calendar}'s runtime type, zone and settings.</p>
     *
     * <p>The wall-clock time of day is preserved across a daylight-saving transition. A wall clock a
     * spring-forward gap removes moves forward by the length of the gap, and one an autumn overlap repeats
     * keeps the offset the input was already on (the earlier occurrence when the input was on neither), so
     * every {@code add*} field names the same instant for the same civil target.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);            // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addWeeks(cal, 2), "yyyy-MM-dd HH:mm:ss", utc);    // returns "2025-01-29 10:30:45"
     * Dates.format(Dates.addWeeks(cal, -1), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2025-01-08 10:30:45"
     *
     * cal.getTimeInMillis();                                               // returns 1736937045000 (original unchanged)
     * Dates.addWeeks((Calendar) null, 2);                                  // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add weeks to, not {@code null}.
     * @param amount the amount of weeks to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of weeks added.
     * @throws IllegalArgumentException if the calendar is {@code null}.
     * @throws ArithmeticException if the resulting instant is outside the signed-long epoch-millisecond range.
     */
    public static <T extends Calendar> T addWeeks(final T calendar, final int amount) throws IllegalArgumentException, ArithmeticException {
        return addToCalendar(calendar, amount, CalendarField.WEEK_OF_YEAR);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of days to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p>The arithmetic runs on a proleptic Gregorian calendar in {@code calendar}'s own time zone,
     * whatever calendar system and Julian/Gregorian cutover {@code calendar} itself carries, so the
     * result always matches the civil date {@link #format(Calendar)} prints &mdash; the same rule
     * {@link #round(Calendar, int)}, {@link #truncate(Calendar, int)} and {@link #ceiling(Calendar, int)}
     * follow. The result keeps {@code calendar}'s runtime type, zone and settings.</p>
     *
     * <p>Uses daylight-saving-aware calendar arithmetic: the wall-clock time of day is preserved, so the
     * epoch difference across a daylight-saving transition may differ from 24 hours by the size of the shift
     * (23 or 25 hours for the usual one-hour move; {@code Australia/Lord_Howe} shifts 30 minutes, giving 23.5
     * or 24.5 hours). This means {@code addDays(c, 1)} is <i>not</i> always equivalent to
     * {@code addHours(c, 24)} &mdash; the {@code addHours}/{@code addMinutes}/{@code addSeconds}/
     * {@code addMilliseconds} methods use plain millisecond arithmetic. A wall clock a spring-forward gap
     * removes moves forward by the length of the gap, and one an autumn overlap repeats keeps the offset
     * the input was already on (the earlier occurrence when the input was on neither), so every
     * {@code add*} field names the same instant for the same civil target and agrees with
     * {@link ZonedDateTime#plusDays(long)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);           // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addDays(cal, 1), "yyyy-MM-dd HH:mm:ss", utc);    // returns "2025-01-16 10:30:45"
     * Dates.format(Dates.addDays(cal, -5), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2025-01-10 10:30:45"
     *
     * cal.getTimeInMillis();                                              // returns 1736937045000 (original unchanged)
     * Dates.addDays((Calendar) null, 1);                                  // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add days to, not {@code null}.
     * @param amount the amount of days to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of days added.
     * @throws IllegalArgumentException if the calendar is {@code null}.
     * @throws ArithmeticException if the resulting instant is outside the signed-long epoch-millisecond range.
     */
    public static <T extends Calendar> T addDays(final T calendar, final int amount) throws IllegalArgumentException, ArithmeticException {
        return addToCalendar(calendar, amount, CalendarField.DAY_OF_MONTH);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of hours to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p>Uses plain millisecond arithmetic ({@code amount * 3_600_000}): the result is exactly
     * {@code amount} hours later in epoch terms, even across daylight-saving transitions &mdash;
     * unlike {@link #addDays(Calendar, int)}, which preserves wall-clock time of day.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);            // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addHours(cal, 5), "yyyy-MM-dd HH:mm:ss", utc);    // returns "2025-01-15 15:30:45"
     * Dates.format(Dates.addHours(cal, -3), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2025-01-15 07:30:45"
     *
     * cal.getTimeInMillis();                                               // returns 1736937045000 (original unchanged)
     * Dates.addHours((Calendar) null, 5);                                  // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add hours to, not {@code null}.
     * @param amount the amount of hours to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of hours added.
     * @throws IllegalArgumentException if the calendar is {@code null}.
     * @throws ArithmeticException if the resulting instant is outside the signed-long epoch-millisecond range.
     */
    public static <T extends Calendar> T addHours(final T calendar, final int amount) throws IllegalArgumentException, ArithmeticException {
        return addToCalendar(calendar, amount, TimeUnit.HOURS);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of minutes to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p>Uses plain millisecond arithmetic ({@code amount * 60_000}); see
     * {@link #addHours(Calendar, int)} for the daylight-saving implications.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);               // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addMinutes(cal, 15), "yyyy-MM-dd HH:mm:ss", utc);    // returns "2025-01-15 10:45:45"
     * Dates.format(Dates.addMinutes(cal, -30), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2025-01-15 10:00:45"
     *
     * cal.getTimeInMillis();                                                  // returns 1736937045000 (original unchanged)
     * Dates.addMinutes((Calendar) null, 15);                                  // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add minutes to, not {@code null}.
     * @param amount the amount of minutes to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of minutes added.
     * @throws IllegalArgumentException if the calendar is {@code null}.
     * @throws ArithmeticException if the resulting instant is outside the signed-long epoch-millisecond range.
     */
    public static <T extends Calendar> T addMinutes(final T calendar, final int amount) throws IllegalArgumentException, ArithmeticException {
        return addToCalendar(calendar, amount, TimeUnit.MINUTES);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of seconds to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p>Uses plain millisecond arithmetic ({@code amount * 1_000}); see
     * {@link #addHours(Calendar, int)} for the daylight-saving implications.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);               // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addSeconds(cal, 15), "yyyy-MM-dd HH:mm:ss", utc);    // returns "2025-01-15 10:31:00"
     * Dates.format(Dates.addSeconds(cal, -45), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2025-01-15 10:30:00"
     *
     * cal.getTimeInMillis();                                                  // returns 1736937045000 (original unchanged)
     * Dates.addSeconds((Calendar) null, 30);                                  // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add seconds to, not {@code null}.
     * @param amount the amount of seconds to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of seconds added.
     * @throws IllegalArgumentException if the calendar is {@code null}.
     * @throws ArithmeticException if the resulting instant is outside the signed-long epoch-millisecond range.
     */
    public static <T extends Calendar> T addSeconds(final T calendar, final int amount) throws IllegalArgumentException, ArithmeticException {
        return addToCalendar(calendar, amount, TimeUnit.SECONDS);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of milliseconds to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p>Uses exact epoch-millisecond arithmetic.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(1736937045000L);   // calendar at the 2025-01-15T10:30:45Z instant (default zone)
     * Dates.addMilliseconds(cal, 250).getTimeInMillis();     // returns 1736937045250
     * Dates.addMilliseconds(cal, -1000).getTimeInMillis();   // returns 1736937044000
     *
     * cal.getTimeInMillis();                                 // returns 1736937045000 (original unchanged)
     * Dates.addMilliseconds((Calendar) null, 250);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add milliseconds to, not {@code null}.
     * @param amount the amount of milliseconds to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of milliseconds added.
     * @throws IllegalArgumentException if the calendar is {@code null}.
     * @throws ArithmeticException if the resulting instant is outside the signed-long epoch-millisecond range.
     */
    public static <T extends Calendar> T addMilliseconds(final T calendar, final int amount) throws IllegalArgumentException, ArithmeticException {
        return addToCalendar(calendar, amount, TimeUnit.MILLISECONDS);
    }

    /**
     * Adds a number of hours to a calendar returning a new object, accepting a {@code long} amount.
     * The original {@code Calendar} is unchanged.
     *
     * <p>Uses plain millisecond arithmetic ({@code amount * 3_600_000}): the result is exactly
     * {@code amount} hours later in epoch terms, even across daylight-saving transitions &mdash;
     * unlike {@link #addDays(Calendar, int)}, which preserves wall-clock time of day.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);   // the instant 2025-01-15T10:30:45Z
     * Dates.addHours(cal, 3L).getTimeInMillis();                  // returns 1736947845000 (+3 hours)
     * Dates.addHours(cal, -2L).getTimeInMillis();                 // returns 1736929845000 (-2 hours)
     *
     * cal.getTimeInMillis();                        // returns 1736937045000 (original unchanged)
     * Dates.addHours((Calendar) null, 3L);          // throws IllegalArgumentException
     * Dates.addHours(cal, Long.MAX_VALUE);          // throws ArithmeticException (epoch overflow)
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add hours to, not {@code null}.
     * @param amount the amount of hours to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of hours added.
     * @throws IllegalArgumentException if the calendar is {@code null}.
     * @throws ArithmeticException if conversion to milliseconds or the resulting epoch-millisecond value overflows a {@code long}.
     * @see #addHours(Calendar, int)
     */
    public static <T extends Calendar> T addHours(final T calendar, final long amount) throws IllegalArgumentException, ArithmeticException {
        return addToCalendar(calendar, amount, TimeUnit.HOURS);
    }

    /**
     * Adds a number of minutes to a calendar returning a new object, accepting a {@code long} amount.
     * The original {@code Calendar} is unchanged.
     *
     * <p>Uses plain millisecond arithmetic ({@code amount * 60_000}); see
     * {@link #addHours(Calendar, long)} for the daylight-saving implications.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);   // the instant 2025-01-15T10:30:45Z
     * Dates.addMinutes(cal, 90L).getTimeInMillis();               // returns 1736942445000 (+90 minutes)
     * Dates.addMinutes(cal, -30L).getTimeInMillis();              // returns 1736935245000 (-30 minutes)
     *
     * cal.getTimeInMillis();                           // returns 1736937045000 (original unchanged)
     * Dates.addMinutes((Calendar) null, 90L);          // throws IllegalArgumentException
     * Dates.addMinutes(cal, Long.MAX_VALUE);           // throws ArithmeticException (epoch overflow)
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add minutes to, not {@code null}.
     * @param amount the amount of minutes to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of minutes added.
     * @throws IllegalArgumentException if the calendar is {@code null}.
     * @throws ArithmeticException if conversion to milliseconds or the resulting epoch-millisecond value overflows a {@code long}.
     * @see #addMinutes(Calendar, int)
     */
    public static <T extends Calendar> T addMinutes(final T calendar, final long amount) throws IllegalArgumentException, ArithmeticException {
        return addToCalendar(calendar, amount, TimeUnit.MINUTES);
    }

    /**
     * Adds a number of seconds to a calendar returning a new object, accepting a {@code long} amount.
     * The original {@code Calendar} is unchanged.
     *
     * <p>Uses plain millisecond arithmetic ({@code amount * 1_000}); see
     * {@link #addHours(Calendar, long)} for the daylight-saving implications.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);   // the instant 2025-01-15T10:30:45Z
     * Dates.addSeconds(cal, 15L).getTimeInMillis();               // returns 1736937060000 (+15 seconds)
     * Dates.addSeconds(cal, -45L).getTimeInMillis();              // returns 1736937000000 (-45 seconds)
     *
     * cal.getTimeInMillis();                           // returns 1736937045000 (original unchanged)
     * Dates.addSeconds((Calendar) null, 15L);          // throws IllegalArgumentException
     * Dates.addSeconds(cal, Long.MAX_VALUE);           // throws ArithmeticException (epoch overflow)
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add seconds to, not {@code null}.
     * @param amount the amount of seconds to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of seconds added.
     * @throws IllegalArgumentException if the calendar is {@code null}.
     * @throws ArithmeticException if conversion to milliseconds or the resulting epoch-millisecond value overflows a {@code long}.
     * @see #addSeconds(Calendar, int)
     */
    public static <T extends Calendar> T addSeconds(final T calendar, final long amount) throws IllegalArgumentException, ArithmeticException {
        return addToCalendar(calendar, amount, TimeUnit.SECONDS);
    }

    /**
     * Adds a number of milliseconds to a calendar returning a new object, accepting a {@code long} amount.
     * The original {@code Calendar} is unchanged.
     *
     * <p>This {@code long} overload complements {@link #addMilliseconds(Calendar, int)}: a millisecond count
     * near {@code 2^31} (about 24.8 days) overflows an {@code int}, so use this overload when the amount may
     * exceed the {@code int} range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(1736937045000L);            // the instant 2025-01-15T10:30:45Z
     * Dates.addMilliseconds(cal, 5_000_000_000L).getTimeInMillis();   // returns 1741937045000 (~57.9 days later)
     * Dates.addMilliseconds(cal, -5_000_000_000L).getTimeInMillis();  // returns 1731937045000 (~57.9 days earlier)
     *
     * cal.getTimeInMillis();                                          // returns 1736937045000 (original unchanged)
     * Dates.addMilliseconds((Calendar) null, 1L);                     // throws IllegalArgumentException
     * Dates.addMilliseconds(cal, Long.MAX_VALUE);                     // throws ArithmeticException (epoch overflow)
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add milliseconds to, not {@code null}.
     * @param amount the amount of milliseconds to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of milliseconds added.
     * @throws IllegalArgumentException if the calendar is {@code null}.
     * @throws ArithmeticException if the resulting epoch-millisecond value overflows a {@code long}.
     */
    public static <T extends Calendar> T addMilliseconds(final T calendar, final long amount) throws IllegalArgumentException, ArithmeticException {
        return addToCalendar(calendar, amount, TimeUnit.MILLISECONDS);
    }

    /**
     * Adapted from Apache Commons Lang under Apache License v2; the rounding semantics differ (nearest boundary,
     * not field-position midpoint) - see below.
     * <br />
     *
     * <p>Rounds a date, leaving the field specified as the most
     * significant field.</p>
     *
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if this was passed with HOUR, it would return
     * 28 Mar 2002 14:00:00.000. If this was passed with MONTH, it
     * would return 1 April 2002 0:00:00.000.</p>
     *
     * <p>For a date in a timezone that handles the change to daylight-saving time, rounding to Calendar.HOUR_OF_DAY will behave as follows.
     * Suppose daylight-saving time begins at 02:00 on March 30 (as in most European zones in 2003,
     * e.g. {@code Europe/Berlin}; the example is zone-dependent): the local times 02:00 through 02:59
     * never occur, so the two adjacent valid hour boundaries, 01:00 and 03:00, are exactly 60 elapsed
     * minutes apart. Measured by elapsed-millisecond distance (an exact tie rounds up):
     * </p>
     * <ul>
     * <li>March 30, 2003 01:29 rounds to March 30, 2003 01:00 (29 vs 31 elapsed minutes)</li>
     * <li>March 30, 2003 01:30 rounds to March 30, 2003 03:00 (an exact tie rounds up)</li>
     * <li>March 30, 2003 01:31 rounds to March 30, 2003 03:00 (31 vs 29 elapsed minutes)</li>
     * <li>March 30, 2003 03:29 rounds to March 30, 2003 03:00</li>
     * </ul>
     *
     * <p><b>Rounding semantics:</b> the value is moved to the nearer of the two adjacent valid
     * boundaries of the field, measured by exact elapsed-millisecond distance in the evaluating
     * calendar's zone (so daylight-saving transitions are measured in elapsed time, not wall-clock
     * fields); see <i>Daylight saving</i> below for what counts as a boundary when a transition removes
     * or repeats one. An exact tie rounds up to the later boundary. For example, in UTC,
     * {@code 2023-07-01T00:00} rounds down to {@code 2023-01-01} because it is 181 days after the
     * lower year boundary and 184 days before the upper one.</p>
     *
     * <p>This overload evaluates in the JVM default time zone; use the {@code Calendar} overloads
     * to control the zone.</p>
     *
     * <p>Boundaries are computed on the proleptic ISO calendar (a customized
     * {@link GregorianCalendar#setGregorianChange(java.util.Date) Julian/Gregorian cutover} is not
     * applied) and are resolved in the evaluating time zone, which must be one {@link ZoneId} can
     * express &mdash; the same requirement {@code parse}, {@code format} and {@code isSameDay} impose.</p>
     *
     * <p><b>Daylight saving.</b> A boundary is a <i>resolved civil</i> boundary, not merely a local time
     * whose finer fields read zero. When an overlap repeats a nominal boundary, both occurrences are
     * boundaries and the one on the required side of the value is taken: the later occurrence for a
     * truncation at or before it, the earlier one for a ceiling after it. When the replayed window straddles
     * the unit boundary ({@code America/St_Johns} fell back from 00:01 to 23:01 until 2011; {@code Pacific/Chatham}
     * falls back from 03:45 to 02:45 every April), the clock had already struck the <i>next</i> unit's boundary
     * before falling back into this one: the truncation is then the start of the value's own civil unit, so
     * across such a transition it is monotonic in civil order rather than in instant order. When a gap removes a nominal
     * boundary, it resolves to the instant the gap ends, which is what keeps the result inside the same
     * civil unit as the input &mdash; in {@code America/Sao_Paulo}, where 4 November 2018 has no
     * midnight, truncating to {@code DATE} yields 01:00 that day rather than the previous day.
     * A rendered result can therefore show non-zero finer fields when the gap end is not aligned to the
     * requested unit: {@code Pacific/Chatham} moves its clocks from 02:45 to 03:45, so truncating
     * {@code 2025-09-28T03:45+13:45} to {@code HOUR_OF_DAY} returns that same instant &mdash; the first
     * one belonging to civil hour 03. Truncation still never moves a value forward, a ceiling never
     * moves one backward, and both always land on a real instant.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // parse and format in the same (default) zone so the result is stable
     * java.util.Date date = Dates.parseToJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.round(date, Calendar.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss");    // returns "2024-11-24 11:00:00" (rounds up, 30 min)
     * Dates.format(Dates.round(date, Calendar.DAY_OF_MONTH), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-24 00:00:00" (rounds down)
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                                       // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.round((java.util.Date) null, Calendar.HOUR_OF_DAY);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date to work with, not {@code null}.
     * @param field the field from {@code Calendar} or {@link #SEMI_MONTH}. Supported values:
     *        {@code MILLISECOND}, {@code SECOND}, {@code MINUTE}, {@code HOUR}/{@code HOUR_OF_DAY},
     *        {@code AM_PM}, {@code DATE}/{@code DAY_OF_MONTH}, {@code MONTH}, {@code YEAR},
     *        and {@link #SEMI_MONTH}; any other field (including {@code ERA}) throws
     *        {@code IllegalArgumentException}.
     * @return a new date object of type T, rounded to the nearest whole unit as specified by the field;
     *         an exact tie rounds up to the later boundary (see <i>Rounding semantics</i> above).
     * @throws IllegalArgumentException if the date is {@code null}, or if {@code field} is not a supported Calendar field, or the evaluating time
     *         zone carries custom daylight-saving rules that no {@link ZoneId} can represent, or a fixed offset that is not a whole number of
     *         seconds.
     * @throws ArithmeticException if the year magnitude exceeds 280 million or the rounded
     *         epoch-millisecond value overflows.
     * @throws IllegalStateException if the registered creator, declared constructor, or {@link java.util.Date#clone()}
     *         used to build the result violates its documented runtime-type, distinct-instance, or
     *         requested-instant contract
     * @see #round(java.util.Date, CalendarField)
     * @see #truncate(java.util.Date, int)
     * @see #ceiling(java.util.Date, int)
     */
    public static <T extends java.util.Date> T round(final T date, final int field)
            throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        N.checkArgNotNull(date, cs.date);

        // A Timestamp's sub-millisecond nano fraction is part of the value being rounded: rounding to
        // MILLISECOND rounds that fraction (half up), and coarser fields are unaffected by it (a
        // sub-millisecond fraction can never tip a whole-millisecond rounding decision, and a
        // whole-millisecond tie already rounds up). The result never retains a sub-ms fraction.
        long millis = date.getTime();

        if (field == Calendar.MILLISECOND && date instanceof Timestamp && ((Timestamp) date).getNanos() % 1_000_000 >= 500_000) {
            millis = Math.incrementExact(millis);
        }

        final long resultMillis = modifiedMillis(millis, field, TimeZone.getDefault(), ModifyType.ROUND);
        return clearSubMillis(createDate(resultMillis, date), resultMillis);
    }

    /**
     * Rounds the given date to the nearest whole unit as specified by the CalendarField, using the
     * nearest-boundary semantics documented on {@link #round(java.util.Date, int)}.
     * The original date object is unchanged. This overload evaluates in the JVM default time zone;
     * use the {@code Calendar} overloads to control the zone.
     *
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if this was passed with HOUR_OF_DAY, it would return
     * 28 Mar 2002 14:00:00.000. If this was passed with MONTH, it
     * would return 1 April 2002 0:00:00.000.</p>
     *
     * <p>For a date in a timezone that handles the change to daylight-saving time, rounding to CalendarField.HOUR_OF_DAY will behave as follows.
     * Suppose daylight-saving time begins at 02:00 on March 30 (as in most European zones in 2003,
     * e.g. {@code Europe/Berlin}; the example is zone-dependent): the local times 02:00 through 02:59
     * never occur, so the two adjacent valid hour boundaries, 01:00 and 03:00, are exactly 60 elapsed
     * minutes apart. Measured by elapsed-millisecond distance (an exact tie rounds up):
     * </p>
     * <ul>
     * <li>March 30, 2003 01:29 rounds to March 30, 2003 01:00 (29 vs 31 elapsed minutes)</li>
     * <li>March 30, 2003 01:30 rounds to March 30, 2003 03:00 (an exact tie rounds up)</li>
     * <li>March 30, 2003 01:31 rounds to March 30, 2003 03:00 (31 vs 29 elapsed minutes)</li>
     * <li>March 30, 2003 03:29 rounds to March 30, 2003 03:00</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.round(date, CalendarField.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-24 11:00:00"
     * Dates.format(Dates.round(date, CalendarField.MONTH), "yyyy-MM-dd HH:mm:ss");         // returns "2024-12-01 00:00:00" (rounds up)
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                                           // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.round((java.util.Date) null, CalendarField.HOUR_OF_DAY);                       // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Accepted {@link CalendarField} values: {@code MILLISECOND}, {@code SECOND}, {@code MINUTE},
     * {@code HOUR_OF_DAY}, {@code DAY_OF_MONTH}, {@code MONTH}, and {@code YEAR}.
     * {@code WEEK_OF_YEAR} is not supported and throws {@code IllegalArgumentException}.</p>
     *
     * <p>Boundaries are computed on the proleptic ISO calendar (a customized
     * {@link GregorianCalendar#setGregorianChange(java.util.Date) Julian/Gregorian cutover} is not
     * applied) and are resolved in the evaluating time zone, which must be one {@link ZoneId} can
     * express &mdash; the same requirement {@code parse}, {@code format} and {@code isSameDay} impose.</p>
     *
     * <p><b>Daylight saving.</b> A boundary is a <i>resolved civil</i> boundary, not merely a local time
     * whose finer fields read zero. When an overlap repeats a nominal boundary, both occurrences are
     * boundaries and the one on the required side of the value is taken: the later occurrence for a
     * truncation at or before it, the earlier one for a ceiling after it. When the replayed window straddles
     * the unit boundary ({@code America/St_Johns} fell back from 00:01 to 23:01 until 2011; {@code Pacific/Chatham}
     * falls back from 03:45 to 02:45 every April), the clock had already struck the <i>next</i> unit's boundary
     * before falling back into this one: the truncation is then the start of the value's own civil unit, so
     * across such a transition it is monotonic in civil order rather than in instant order. When a gap removes a nominal
     * boundary, it resolves to the instant the gap ends, which is what keeps the result inside the same
     * civil unit as the input &mdash; in {@code America/Sao_Paulo}, where 4 November 2018 has no
     * midnight, truncating to {@code DATE} yields 01:00 that day rather than the previous day.
     * A rendered result can therefore show non-zero finer fields when the gap end is not aligned to the
     * requested unit: {@code Pacific/Chatham} moves its clocks from 02:45 to 03:45, so truncating
     * {@code 2025-09-28T03:45+13:45} to {@code HOUR_OF_DAY} returns that same instant &mdash; the first
     * one belonging to civil hour 03. Truncation still never moves a value forward, a ceiling never
     * moves one backward, and both always land on a real instant.</p>
     *
     * @param <T> the type of the date object, which must be a subclass of java.util.Date.
     * @param date the date to be rounded.
     * @param field the CalendarField to which the date is to be rounded.
     * @return a new date object of type T, rounded to the nearest whole unit as specified by the field;
     *         an exact tie rounds up to the later boundary (see {@link #round(java.util.Date, int)}).
     * @throws IllegalArgumentException if the date or field is {@code null}, or if the field is not supported, or the evaluating time zone carries
     *         custom daylight-saving rules that no {@link ZoneId} can represent, or a fixed offset that is not a whole number of seconds.
     * @throws ArithmeticException if the year magnitude exceeds 280 million or the rounded
     *         epoch-millisecond value overflows.
     * @throws IllegalStateException if the registered creator, declared constructor, or {@link java.util.Date#clone()}
     *         used to build the result violates its documented runtime-type, distinct-instance, or
     *         requested-instant contract
     * @see #round(java.util.Date, int)
     * @see #truncate(java.util.Date, CalendarField)
     * @see #ceiling(java.util.Date, CalendarField)
     */
    public static <T extends java.util.Date> T round(final T date, final CalendarField field)
            throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(field, cs.field);

        return round(date, field.value());
    }

    /**
     * Adapted from Apache Commons Lang under Apache License v2; the rounding semantics differ (nearest boundary,
     * not field-position midpoint) - see below.
     * <br />
     *
     * <p>Rounds a date, leaving the field specified as the most
     * significant field.</p>
     *
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if this was passed with HOUR, it would return
     * 28 Mar 2002 14:00:00.000. If this was passed with MONTH, it
     * would return 1 April 2002 0:00:00.000.</p>
     *
     * <p>For a date in a timezone that handles the change to daylight-saving time, rounding to Calendar.HOUR_OF_DAY will behave as follows.
     * Suppose daylight-saving time begins at 02:00 on March 30 (as in most European zones in 2003,
     * e.g. {@code Europe/Berlin}; the example is zone-dependent): the local times 02:00 through 02:59
     * never occur, so the two adjacent valid hour boundaries, 01:00 and 03:00, are exactly 60 elapsed
     * minutes apart. Measured by elapsed-millisecond distance (an exact tie rounds up):
     * </p>
     * <ul>
     * <li>March 30, 2003 01:29 rounds to March 30, 2003 01:00 (29 vs 31 elapsed minutes)</li>
     * <li>March 30, 2003 01:30 rounds to March 30, 2003 03:00 (an exact tie rounds up)</li>
     * <li>March 30, 2003 01:31 rounds to March 30, 2003 03:00 (31 vs 29 elapsed minutes)</li>
     * <li>March 30, 2003 03:29 rounds to March 30, 2003 03:00</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseToCalendar("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * Dates.format(Dates.round(cal, Calendar.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss", utc);    // returns "2024-11-24 11:00:00"
     * Dates.format(Dates.round(cal, Calendar.DAY_OF_MONTH), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2024-11-24 00:00:00"
     *
     * Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);                                       // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.round((Calendar) null, Calendar.HOUR_OF_DAY);                                  // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Boundaries are computed on the proleptic ISO calendar (a customized
     * {@link GregorianCalendar#setGregorianChange(java.util.Date) Julian/Gregorian cutover} is not
     * applied) and are resolved in the evaluating time zone, which must be one {@link ZoneId} can
     * express &mdash; the same requirement {@code parse}, {@code format} and {@code isSameDay} impose.</p>
     *
     * <p><b>Daylight saving.</b> A boundary is a <i>resolved civil</i> boundary, not merely a local time
     * whose finer fields read zero. When an overlap repeats a nominal boundary, both occurrences are
     * boundaries and the one on the required side of the value is taken: the later occurrence for a
     * truncation at or before it, the earlier one for a ceiling after it. When the replayed window straddles
     * the unit boundary ({@code America/St_Johns} fell back from 00:01 to 23:01 until 2011; {@code Pacific/Chatham}
     * falls back from 03:45 to 02:45 every April), the clock had already struck the <i>next</i> unit's boundary
     * before falling back into this one: the truncation is then the start of the value's own civil unit, so
     * across such a transition it is monotonic in civil order rather than in instant order. When a gap removes a nominal
     * boundary, it resolves to the instant the gap ends, which is what keeps the result inside the same
     * civil unit as the input &mdash; in {@code America/Sao_Paulo}, where 4 November 2018 has no
     * midnight, truncating to {@code DATE} yields 01:00 that day rather than the previous day.
     * A rendered result can therefore show non-zero finer fields when the gap end is not aligned to the
     * requested unit: {@code Pacific/Chatham} moves its clocks from 02:45 to 03:45, so truncating
     * {@code 2025-09-28T03:45+13:45} to {@code HOUR_OF_DAY} returns that same instant &mdash; the first
     * one belonging to civil hour 03. Truncation still never moves a value forward, a ceiling never
     * moves one backward, and both always land on a real instant.</p>
     *
     * @param <T> the type of the calendar object, which must extend java.util.Calendar.
     * @param calendar the calendar to work with, not {@code null}.
     * @param field the field from {@code Calendar} or {@link #SEMI_MONTH}. Supported values:
     *        {@code MILLISECOND}, {@code SECOND}, {@code MINUTE}, {@code HOUR}/{@code HOUR_OF_DAY},
     *        {@code AM_PM}, {@code DATE}/{@code DAY_OF_MONTH}, {@code MONTH}, {@code YEAR},
     *        and {@link #SEMI_MONTH}; any other field (including {@code ERA}) throws
     *        {@code IllegalArgumentException}.
     * @return a new calendar object of type T, rounded to the nearest whole unit as specified by the
     *         field; an exact tie rounds up to the later boundary (see {@link #round(java.util.Date, int)}).
     * @throws IllegalArgumentException if the calendar is {@code null}, or if {@code field} is not a supported Calendar field, or the evaluating time
     *         zone carries custom daylight-saving rules that no {@link ZoneId} can represent, or a fixed offset that is not a whole number of
     *         seconds.
     * @throws ArithmeticException if the year magnitude exceeds 280 million in either direction (the
     *         guard tests the absolute proleptic year, so the most negative epoch values are rejected
     *         as well as the largest).
     * @throws IllegalStateException if the registered creator, declared constructor, or
     *         {@link Calendar#clone()} used to build the result violates its documented runtime-type,
     *         distinct-instance, or requested-instant contract.
     * @see #round(Calendar, CalendarField)
     * @see #truncate(Calendar, int)
     * @see #ceiling(Calendar, int)
     */
    public static <T extends Calendar> T round(final T calendar, final int field) throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createCalendar(calendar, modifiedMillis(calendar, field, ModifyType.ROUND));
    }

    /**
     * Rounds the given calendar to the nearest whole unit as specified by the CalendarField, using the
     * nearest-boundary semantics documented on {@link #round(java.util.Date, int)}.
     * The original calendar object is unchanged.
     *
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if this was passed with HOUR_OF_DAY, it would return
     * 28 Mar 2002 14:00:00.000. If this was passed with MONTH, it
     * would return 1 April 2002 0:00:00.000.</p>
     *
     * <p>For a date in a timezone that handles the change to daylight-saving time, rounding to CalendarField.HOUR_OF_DAY will behave as follows.
     * Suppose daylight-saving time begins at 02:00 on March 30 (as in most European zones in 2003,
     * e.g. {@code Europe/Berlin}; the example is zone-dependent): the local times 02:00 through 02:59
     * never occur, so the two adjacent valid hour boundaries, 01:00 and 03:00, are exactly 60 elapsed
     * minutes apart. Measured by elapsed-millisecond distance (an exact tie rounds up):
     * </p>
     * <ul>
     * <li>March 30, 2003 01:29 rounds to March 30, 2003 01:00 (29 vs 31 elapsed minutes)</li>
     * <li>March 30, 2003 01:30 rounds to March 30, 2003 03:00 (an exact tie rounds up)</li>
     * <li>March 30, 2003 01:31 rounds to March 30, 2003 03:00 (31 vs 29 elapsed minutes)</li>
     * <li>March 30, 2003 03:29 rounds to March 30, 2003 03:00</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseToCalendar("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * Dates.format(Dates.round(cal, CalendarField.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2024-11-24 11:00:00"
     * Dates.format(Dates.round(cal, CalendarField.MONTH), "yyyy-MM-dd HH:mm:ss", utc);         // returns "2024-12-01 00:00:00"
     *
     * Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);                                           // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.round((Calendar) null, CalendarField.HOUR_OF_DAY);                                 // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Accepted {@link CalendarField} values: {@code MILLISECOND}, {@code SECOND}, {@code MINUTE},
     * {@code HOUR_OF_DAY}, {@code DAY_OF_MONTH}, {@code MONTH}, and {@code YEAR}.
     * {@code WEEK_OF_YEAR} is not supported and throws {@code IllegalArgumentException}.</p>
     *
     * <p>Boundaries are computed on the proleptic ISO calendar (a customized
     * {@link GregorianCalendar#setGregorianChange(java.util.Date) Julian/Gregorian cutover} is not
     * applied) and are resolved in the evaluating time zone, which must be one {@link ZoneId} can
     * express &mdash; the same requirement {@code parse}, {@code format} and {@code isSameDay} impose.</p>
     *
     * <p><b>Daylight saving.</b> A boundary is a <i>resolved civil</i> boundary, not merely a local time
     * whose finer fields read zero. When an overlap repeats a nominal boundary, both occurrences are
     * boundaries and the one on the required side of the value is taken: the later occurrence for a
     * truncation at or before it, the earlier one for a ceiling after it. When the replayed window straddles
     * the unit boundary ({@code America/St_Johns} fell back from 00:01 to 23:01 until 2011; {@code Pacific/Chatham}
     * falls back from 03:45 to 02:45 every April), the clock had already struck the <i>next</i> unit's boundary
     * before falling back into this one: the truncation is then the start of the value's own civil unit, so
     * across such a transition it is monotonic in civil order rather than in instant order. When a gap removes a nominal
     * boundary, it resolves to the instant the gap ends, which is what keeps the result inside the same
     * civil unit as the input &mdash; in {@code America/Sao_Paulo}, where 4 November 2018 has no
     * midnight, truncating to {@code DATE} yields 01:00 that day rather than the previous day.
     * A rendered result can therefore show non-zero finer fields when the gap end is not aligned to the
     * requested unit: {@code Pacific/Chatham} moves its clocks from 02:45 to 03:45, so truncating
     * {@code 2025-09-28T03:45+13:45} to {@code HOUR_OF_DAY} returns that same instant &mdash; the first
     * one belonging to civil hour 03. Truncation still never moves a value forward, a ceiling never
     * moves one backward, and both always land on a real instant.</p>
     *
     * @param <T> the type of the calendar object, which must be a subclass of java.util.Calendar.
     * @param calendar the calendar to be rounded.
     * @param field the CalendarField to which the calendar is to be rounded.
     * @return a new calendar object of type T, rounded to the nearest whole unit as specified by the
     *         field; an exact tie rounds up to the later boundary (see {@link #round(java.util.Date, int)}).
     * @throws IllegalArgumentException if the calendar or field is {@code null}, or if the field is not supported, or the evaluating time zone
     *         carries custom daylight-saving rules that no {@link ZoneId} can represent, or a fixed offset that is not a whole number of seconds.
     * @throws ArithmeticException if the year magnitude exceeds 280 million in either direction (the
     *         guard tests the absolute proleptic year, so the most negative epoch values are rejected
     *         as well as the largest).
     * @throws IllegalStateException if the registered creator, declared constructor, or
     *         {@link Calendar#clone()} used to build the result violates its documented runtime-type,
     *         distinct-instance, or requested-instant contract.
     * @see #round(Calendar, int)
     * @see #truncate(Calendar, CalendarField)
     * @see #ceiling(Calendar, CalendarField)
     */
    public static <T extends Calendar> T round(final T calendar, final CalendarField field)
            throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        N.checkArgNotNull(calendar, cs.calendar);
        N.checkArgNotNull(field, cs.field);

        return round(calendar, field.value());
    }

    //-----------------------------------------------------------------------

    /**
     * Adapted from Apache Commons Lang under Apache License v2; the boundary engine and its
     * daylight-saving resolution are this class's own - see below.
     * <br />
     *
     * <p>Truncates a date, leaving the field specified as the most
     * significant field, so every field below it reads its minimum in the evaluating time zone (see
     * <i>Daylight saving</i> below for the one case a transition makes that impossible).</p>
     *
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if you passed with HOUR, it would return 28 Mar
     * 2002 13:00:00.000.  If this was passed with MONTH, it would
     * return 1 Mar 2002 0:00:00.000.</p>
     *
     * <p>This overload evaluates in the JVM default time zone; use the {@code Calendar} overloads
     * to control the zone.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.truncate(date, Calendar.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-24 10:00:00" (clears below hour)
     * Dates.format(Dates.truncate(date, Calendar.MONTH), "yyyy-MM-dd HH:mm:ss");         // returns "2024-11-01 00:00:00"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                                         // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.truncate((java.util.Date) null, Calendar.HOUR_OF_DAY);                       // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Boundaries are computed on the proleptic ISO calendar (a customized
     * {@link GregorianCalendar#setGregorianChange(java.util.Date) Julian/Gregorian cutover} is not
     * applied) and are resolved in the evaluating time zone, which must be one {@link ZoneId} can
     * express &mdash; the same requirement {@code parse}, {@code format} and {@code isSameDay} impose.</p>
     *
     * <p><b>Daylight saving.</b> A boundary is a <i>resolved civil</i> boundary, not merely a local time
     * whose finer fields read zero. When an overlap repeats a nominal boundary, both occurrences are
     * boundaries and the one on the required side of the value is taken: the later occurrence for a
     * truncation at or before it, the earlier one for a ceiling after it. When the replayed window straddles
     * the unit boundary ({@code America/St_Johns} fell back from 00:01 to 23:01 until 2011; {@code Pacific/Chatham}
     * falls back from 03:45 to 02:45 every April), the clock had already struck the <i>next</i> unit's boundary
     * before falling back into this one: the truncation is then the start of the value's own civil unit, so
     * across such a transition it is monotonic in civil order rather than in instant order. When a gap removes a nominal
     * boundary, it resolves to the instant the gap ends, which is what keeps the result inside the same
     * civil unit as the input &mdash; in {@code America/Sao_Paulo}, where 4 November 2018 has no
     * midnight, truncating to {@code DATE} yields 01:00 that day rather than the previous day.
     * A rendered result can therefore show non-zero finer fields when the gap end is not aligned to the
     * requested unit: {@code Pacific/Chatham} moves its clocks from 02:45 to 03:45, so truncating
     * {@code 2025-09-28T03:45+13:45} to {@code HOUR_OF_DAY} returns that same instant &mdash; the first
     * one belonging to civil hour 03. Truncation still never moves a value forward, a ceiling never
     * moves one backward, and both always land on a real instant.</p>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date to work with, not {@code null}.
     * @param field the field from {@code Calendar} or {@link #SEMI_MONTH}. Supported values:
     *        {@code MILLISECOND}, {@code SECOND}, {@code MINUTE}, {@code HOUR}/{@code HOUR_OF_DAY},
     *        {@code AM_PM}, {@code DATE}/{@code DAY_OF_MONTH}, {@code MONTH}, {@code YEAR},
     *        and {@link #SEMI_MONTH}; any other field (including {@code ERA}) throws
     *        {@code IllegalArgumentException}.
     * @return a new date object of type T, truncated to the specified field.
     * @throws IllegalArgumentException if the date is {@code null}, or if {@code field} is not a supported Calendar field, or the evaluating time
     *         zone carries custom daylight-saving rules that no {@link ZoneId} can represent, or a fixed offset that is not a whole number of
     *         seconds.
     * @throws ArithmeticException if the year magnitude exceeds 280 million in either direction (the
     *         guard tests the absolute proleptic year, so the most negative epoch values are rejected
     *         as well as the largest).
     * @throws IllegalStateException if the registered creator, declared constructor, or {@link java.util.Date#clone()}
     *         used to build the result violates its documented runtime-type, distinct-instance, or
     *         requested-instant contract
     * @see #truncate(java.util.Date, CalendarField)
     * @see #round(java.util.Date, int)
     * @see #ceiling(java.util.Date, int)
     */
    public static <T extends java.util.Date> T truncate(final T date, final int field)
            throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        N.checkArgNotNull(date, cs.date);

        // createDate from whole milliseconds zeroes any sub-millisecond Timestamp nano fraction —
        // truncating to MILLISECOND (or any coarser field) must not retain one.
        final long resultMillis = modifiedMillis(date.getTime(), field, TimeZone.getDefault(), ModifyType.TRUNCATE);
        return clearSubMillis(createDate(resultMillis, date), resultMillis);
    }

    /**
     * Truncates the given date, leaving the field specified as the most significant field
     * (see <i>Daylight saving</i> below).
     * The original date object is unchanged. This overload evaluates in the JVM default time zone;
     * use the {@code Calendar} overloads to control the zone.
     *
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if you passed with HOUR_OF_DAY, it would return 28 Mar
     * 2002 13:00:00.000.  If this was passed with MONTH, it would
     * return 1 Mar 2002 0:00:00.000.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.truncate(date, CalendarField.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-24 10:00:00"
     * Dates.format(Dates.truncate(date, CalendarField.MONTH), "yyyy-MM-dd HH:mm:ss");         // returns "2024-11-01 00:00:00"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                                              // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.truncate((java.util.Date) null, CalendarField.HOUR_OF_DAY);                       // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Accepted {@link CalendarField} values: {@code MILLISECOND}, {@code SECOND}, {@code MINUTE},
     * {@code HOUR_OF_DAY}, {@code DAY_OF_MONTH}, {@code MONTH}, and {@code YEAR}.
     * {@code WEEK_OF_YEAR} is not supported and throws {@code IllegalArgumentException}.</p>
     *
     * <p>Boundaries are computed on the proleptic ISO calendar (a customized
     * {@link GregorianCalendar#setGregorianChange(java.util.Date) Julian/Gregorian cutover} is not
     * applied) and are resolved in the evaluating time zone, which must be one {@link ZoneId} can
     * express &mdash; the same requirement {@code parse}, {@code format} and {@code isSameDay} impose.</p>
     *
     * <p><b>Daylight saving.</b> A boundary is a <i>resolved civil</i> boundary, not merely a local time
     * whose finer fields read zero. When an overlap repeats a nominal boundary, both occurrences are
     * boundaries and the one on the required side of the value is taken: the later occurrence for a
     * truncation at or before it, the earlier one for a ceiling after it. When the replayed window straddles
     * the unit boundary ({@code America/St_Johns} fell back from 00:01 to 23:01 until 2011; {@code Pacific/Chatham}
     * falls back from 03:45 to 02:45 every April), the clock had already struck the <i>next</i> unit's boundary
     * before falling back into this one: the truncation is then the start of the value's own civil unit, so
     * across such a transition it is monotonic in civil order rather than in instant order. When a gap removes a nominal
     * boundary, it resolves to the instant the gap ends, which is what keeps the result inside the same
     * civil unit as the input &mdash; in {@code America/Sao_Paulo}, where 4 November 2018 has no
     * midnight, truncating to {@code DATE} yields 01:00 that day rather than the previous day.
     * A rendered result can therefore show non-zero finer fields when the gap end is not aligned to the
     * requested unit: {@code Pacific/Chatham} moves its clocks from 02:45 to 03:45, so truncating
     * {@code 2025-09-28T03:45+13:45} to {@code HOUR_OF_DAY} returns that same instant &mdash; the first
     * one belonging to civil hour 03. Truncation still never moves a value forward, a ceiling never
     * moves one backward, and both always land on a real instant.</p>
     *
     * @param <T> the type of the date object, which must be a subclass of java.util.Date.
     * @param date the date to be truncated.
     * @param field the CalendarField to which the date is to be truncated.
     * @return a new date object of type T, truncated to the specified field.
     * @throws IllegalArgumentException if the date or field is {@code null}, or if the field is not supported, or the evaluating time zone carries
     *         custom daylight-saving rules that no {@link ZoneId} can represent, or a fixed offset that is not a whole number of seconds.
     * @throws ArithmeticException if the year magnitude exceeds 280 million in either direction (the
     *         guard tests the absolute proleptic year, so the most negative epoch values are rejected
     *         as well as the largest).
     * @throws IllegalStateException if the registered creator, declared constructor, or {@link java.util.Date#clone()}
     *         used to build the result violates its documented runtime-type, distinct-instance, or
     *         requested-instant contract
     * @see #truncate(java.util.Date, int)
     * @see #round(java.util.Date, CalendarField)
     * @see #ceiling(java.util.Date, CalendarField)
     */
    public static <T extends java.util.Date> T truncate(final T date, final CalendarField field)
            throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(field, cs.field);

        return truncate(date, field.value());
    }

    /**
     * Adapted from Apache Commons Lang under Apache License v2; the boundary engine and its
     * daylight-saving resolution are this class's own - see below.
     * <br />
     *
     * <p>Truncates a date, leaving the field specified as the most
     * significant field, so every field below it reads its minimum in the evaluating time zone (see
     * <i>Daylight saving</i> below for the one case a transition makes that impossible).</p>
     *
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if you passed with HOUR, it would return 28 Mar
     * 2002 13:00:00.000.  If this was passed with MONTH, it would
     * return 1 Mar 2002 0:00:00.000.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseToCalendar("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * Dates.format(Dates.truncate(cal, Calendar.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2024-11-24 10:00:00"
     * Dates.format(Dates.truncate(cal, Calendar.MONTH), "yyyy-MM-dd HH:mm:ss", utc);         // returns "2024-11-01 00:00:00"
     *
     * Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);                                         // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.truncate((Calendar) null, Calendar.HOUR_OF_DAY);                                 // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Boundaries are computed on the proleptic ISO calendar (a customized
     * {@link GregorianCalendar#setGregorianChange(java.util.Date) Julian/Gregorian cutover} is not
     * applied) and are resolved in the evaluating time zone, which must be one {@link ZoneId} can
     * express &mdash; the same requirement {@code parse}, {@code format} and {@code isSameDay} impose.</p>
     *
     * <p><b>Daylight saving.</b> A boundary is a <i>resolved civil</i> boundary, not merely a local time
     * whose finer fields read zero. When an overlap repeats a nominal boundary, both occurrences are
     * boundaries and the one on the required side of the value is taken: the later occurrence for a
     * truncation at or before it, the earlier one for a ceiling after it. When the replayed window straddles
     * the unit boundary ({@code America/St_Johns} fell back from 00:01 to 23:01 until 2011; {@code Pacific/Chatham}
     * falls back from 03:45 to 02:45 every April), the clock had already struck the <i>next</i> unit's boundary
     * before falling back into this one: the truncation is then the start of the value's own civil unit, so
     * across such a transition it is monotonic in civil order rather than in instant order. When a gap removes a nominal
     * boundary, it resolves to the instant the gap ends, which is what keeps the result inside the same
     * civil unit as the input &mdash; in {@code America/Sao_Paulo}, where 4 November 2018 has no
     * midnight, truncating to {@code DATE} yields 01:00 that day rather than the previous day.
     * A rendered result can therefore show non-zero finer fields when the gap end is not aligned to the
     * requested unit: {@code Pacific/Chatham} moves its clocks from 02:45 to 03:45, so truncating
     * {@code 2025-09-28T03:45+13:45} to {@code HOUR_OF_DAY} returns that same instant &mdash; the first
     * one belonging to civil hour 03. Truncation still never moves a value forward, a ceiling never
     * moves one backward, and both always land on a real instant.</p>
     *
     * @param <T> the type of the calendar object, which must extend java.util.Calendar.
     * @param calendar the calendar to work with, not {@code null}.
     * @param field the field from {@code Calendar} or {@link #SEMI_MONTH}. Supported values:
     *        {@code MILLISECOND}, {@code SECOND}, {@code MINUTE}, {@code HOUR}/{@code HOUR_OF_DAY},
     *        {@code AM_PM}, {@code DATE}/{@code DAY_OF_MONTH}, {@code MONTH}, {@code YEAR},
     *        and {@link #SEMI_MONTH}; any other field (including {@code ERA}) throws
     *        {@code IllegalArgumentException}.
     * @return a new calendar object of type T, truncated to the specified field.
     * @throws IllegalArgumentException if the calendar is {@code null}, or if {@code field} is not a supported Calendar field, or the evaluating time
     *         zone carries custom daylight-saving rules that no {@link ZoneId} can represent, or a fixed offset that is not a whole number of
     *         seconds.
     * @throws ArithmeticException if the year magnitude exceeds 280 million in either direction (the
     *         guard tests the absolute proleptic year, so the most negative epoch values are rejected
     *         as well as the largest).
     * @throws IllegalStateException if the registered creator, declared constructor, or
     *         {@link Calendar#clone()} used to build the result violates its documented runtime-type,
     *         distinct-instance, or requested-instant contract.
     * @see #truncate(Calendar, CalendarField)
     * @see #round(Calendar, int)
     * @see #ceiling(Calendar, int)
     */
    public static <T extends Calendar> T truncate(final T calendar, final int field)
            throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createCalendar(calendar, modifiedMillis(calendar, field, ModifyType.TRUNCATE));
    }

    /**
     * Truncates the given calendar, leaving the field specified as the most significant field
     * (see <i>Daylight saving</i> below).
     * The original calendar object is unchanged.
     *
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if you passed with HOUR_OF_DAY, it would return 28 Mar
     * 2002 13:00:00.000.  If this was passed with MONTH, it would
     * return 1 Mar 2002 0:00:00.000.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseToCalendar("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * Dates.format(Dates.truncate(cal, CalendarField.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2024-11-24 10:00:00"
     * Dates.format(Dates.truncate(cal, CalendarField.MONTH), "yyyy-MM-dd HH:mm:ss", utc);         // returns "2024-11-01 00:00:00"
     *
     * Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);                                              // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.truncate((Calendar) null, CalendarField.HOUR_OF_DAY);                                 // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Accepted {@link CalendarField} values: {@code MILLISECOND}, {@code SECOND}, {@code MINUTE},
     * {@code HOUR_OF_DAY}, {@code DAY_OF_MONTH}, {@code MONTH}, and {@code YEAR}.
     * {@code WEEK_OF_YEAR} is not supported and throws {@code IllegalArgumentException}.</p>
     *
     * <p>Boundaries are computed on the proleptic ISO calendar (a customized
     * {@link GregorianCalendar#setGregorianChange(java.util.Date) Julian/Gregorian cutover} is not
     * applied) and are resolved in the evaluating time zone, which must be one {@link ZoneId} can
     * express &mdash; the same requirement {@code parse}, {@code format} and {@code isSameDay} impose.</p>
     *
     * <p><b>Daylight saving.</b> A boundary is a <i>resolved civil</i> boundary, not merely a local time
     * whose finer fields read zero. When an overlap repeats a nominal boundary, both occurrences are
     * boundaries and the one on the required side of the value is taken: the later occurrence for a
     * truncation at or before it, the earlier one for a ceiling after it. When the replayed window straddles
     * the unit boundary ({@code America/St_Johns} fell back from 00:01 to 23:01 until 2011; {@code Pacific/Chatham}
     * falls back from 03:45 to 02:45 every April), the clock had already struck the <i>next</i> unit's boundary
     * before falling back into this one: the truncation is then the start of the value's own civil unit, so
     * across such a transition it is monotonic in civil order rather than in instant order. When a gap removes a nominal
     * boundary, it resolves to the instant the gap ends, which is what keeps the result inside the same
     * civil unit as the input &mdash; in {@code America/Sao_Paulo}, where 4 November 2018 has no
     * midnight, truncating to {@code DATE} yields 01:00 that day rather than the previous day.
     * A rendered result can therefore show non-zero finer fields when the gap end is not aligned to the
     * requested unit: {@code Pacific/Chatham} moves its clocks from 02:45 to 03:45, so truncating
     * {@code 2025-09-28T03:45+13:45} to {@code HOUR_OF_DAY} returns that same instant &mdash; the first
     * one belonging to civil hour 03. Truncation still never moves a value forward, a ceiling never
     * moves one backward, and both always land on a real instant.</p>
     *
     * @param <T> the type of the calendar object, which must be a subclass of java.util.Calendar.
     * @param calendar the calendar to be truncated.
     * @param field the CalendarField to which the calendar is to be truncated.
     * @return a new calendar object of type T, truncated to the specified field.
     * @throws IllegalArgumentException if the calendar or field is {@code null}, or if the field is not supported, or the evaluating time zone
     *         carries custom daylight-saving rules that no {@link ZoneId} can represent, or a fixed offset that is not a whole number of seconds.
     * @throws ArithmeticException if the year magnitude exceeds 280 million in either direction (the
     *         guard tests the absolute proleptic year, so the most negative epoch values are rejected
     *         as well as the largest).
     * @throws IllegalStateException if the registered creator, declared constructor, or
     *         {@link Calendar#clone()} used to build the result violates its documented runtime-type,
     *         distinct-instance, or requested-instant contract.
     * @see #truncate(Calendar, int)
     * @see #round(Calendar, CalendarField)
     * @see #ceiling(Calendar, CalendarField)
     */
    public static <T extends Calendar> T truncate(final T calendar, final CalendarField field)
            throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        N.checkArgNotNull(calendar, cs.calendar);
        N.checkArgNotNull(field, cs.field);

        return truncate(calendar, field.value());
    }

    //-----------------------------------------------------------------------

    /**
     * Adapted from Apache Commons Lang under Apache License v2; a value already on a boundary is returned
     * unchanged - see below.
     * <br />
     *
     * <p>Gets a date ceiling, leaving the field specified as the most
     * significant field (see <i>Daylight saving</i> below for what counts as a boundary when a
     * transition removes or repeats one). A value already exactly on the requested boundary is returned
     * unchanged (unlike Apache Commons Lang {@code DateUtils.ceiling}, which always moves
     * to the next unit). This overload evaluates in the JVM default time zone; use the
     * {@code Calendar} overloads to control the zone.</p>
     *
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if you passed with HOUR, it would return 28 Mar
     * 2002 14:00:00.000.  If this was passed with MONTH, it would
     * return 1 Apr 2002 0:00:00.000.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.ceiling(date, Calendar.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss");    // returns "2024-11-24 11:00:00" (rounds up to the next hour)
     * Dates.format(Dates.ceiling(date, Calendar.DAY_OF_MONTH), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-25 00:00:00"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                                         // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.ceiling((java.util.Date) null, Calendar.HOUR_OF_DAY);                        // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Boundaries are computed on the proleptic ISO calendar (a customized
     * {@link GregorianCalendar#setGregorianChange(java.util.Date) Julian/Gregorian cutover} is not
     * applied) and are resolved in the evaluating time zone, which must be one {@link ZoneId} can
     * express &mdash; the same requirement {@code parse}, {@code format} and {@code isSameDay} impose.</p>
     *
     * <p><b>Daylight saving.</b> A boundary is a <i>resolved civil</i> boundary, not merely a local time
     * whose finer fields read zero. When an overlap repeats a nominal boundary, both occurrences are
     * boundaries and the one on the required side of the value is taken: the later occurrence for a
     * truncation at or before it, the earlier one for a ceiling after it. When the replayed window straddles
     * the unit boundary ({@code America/St_Johns} fell back from 00:01 to 23:01 until 2011; {@code Pacific/Chatham}
     * falls back from 03:45 to 02:45 every April), the clock had already struck the <i>next</i> unit's boundary
     * before falling back into this one: the truncation is then the start of the value's own civil unit, so
     * across such a transition it is monotonic in civil order rather than in instant order. When a gap removes a nominal
     * boundary, it resolves to the instant the gap ends, which is what keeps the result inside the same
     * civil unit as the input &mdash; in {@code America/Sao_Paulo}, where 4 November 2018 has no
     * midnight, truncating to {@code DATE} yields 01:00 that day rather than the previous day.
     * A rendered result can therefore show non-zero finer fields when the gap end is not aligned to the
     * requested unit: {@code Pacific/Chatham} moves its clocks from 02:45 to 03:45, so truncating
     * {@code 2025-09-28T03:45+13:45} to {@code HOUR_OF_DAY} returns that same instant &mdash; the first
     * one belonging to civil hour 03. Truncation still never moves a value forward, a ceiling never
     * moves one backward, and both always land on a real instant.</p>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date to work with, not {@code null}.
     * @param field the field from {@code Calendar} or {@link #SEMI_MONTH}. Supported values:
     *        {@code MILLISECOND}, {@code SECOND}, {@code MINUTE}, {@code HOUR}/{@code HOUR_OF_DAY},
     *        {@code AM_PM}, {@code DATE}/{@code DAY_OF_MONTH}, {@code MONTH}, {@code YEAR},
     *        and {@link #SEMI_MONTH}; any other field (including {@code ERA}) throws
     *        {@code IllegalArgumentException}.
     * @return a new date object of type T, adjusted to the ceiling of the specified field.
     * @throws IllegalArgumentException if the date is {@code null}, or if {@code field} is not a supported Calendar field, or the evaluating time
     *         zone carries custom daylight-saving rules that no {@link ZoneId} can represent, or a fixed offset that is not a whole number of
     *         seconds.
     * @throws ArithmeticException if the year magnitude exceeds 280 million or the ceiling
     *         epoch-millisecond value overflows.
     * @throws IllegalStateException if the registered creator, declared constructor, or {@link java.util.Date#clone()}
     *         used to build the result violates its documented runtime-type, distinct-instance, or
     *         requested-instant contract
     * @see #ceiling(java.util.Date, CalendarField)
     * @see #round(java.util.Date, int)
     * @see #truncate(java.util.Date, int)
     */
    public static <T extends java.util.Date> T ceiling(final T date, final int field)
            throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        N.checkArgNotNull(date, cs.date);

        // A Timestamp with a sub-millisecond nano fraction lies strictly above its whole-millisecond
        // epoch value, so it is never "already on the boundary": compute the ceiling of millis + 1
        // instead. This is exact because every field boundary falls on a whole millisecond. The result
        // never retains a sub-ms fraction.
        long millis = date.getTime();

        if (date instanceof Timestamp && ((Timestamp) date).getNanos() % 1_000_000 > 0) {
            millis = Math.incrementExact(millis);
        }

        final long resultMillis = modifiedMillis(millis, field, TimeZone.getDefault(), ModifyType.CEILING);
        return clearSubMillis(createDate(resultMillis, date), resultMillis);
    }

    /**
     * Returns a new date object of the same type as the input, but adjusted to the nearest future unit as specified by the CalendarField.
     * The original date object is unchanged. A value already exactly on the requested boundary is
     * returned unchanged. This overload evaluates in the JVM default time zone; use the
     * {@code Calendar} overloads to control the zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.ceiling(date, CalendarField.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-24 11:00:00"
     * Dates.format(Dates.ceiling(date, CalendarField.MONTH), "yyyy-MM-dd HH:mm:ss");         // returns "2024-12-01 00:00:00"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                                             // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.ceiling((java.util.Date) null, CalendarField.HOUR_OF_DAY);                       // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Accepted {@link CalendarField} values: {@code MILLISECOND}, {@code SECOND}, {@code MINUTE},
     * {@code HOUR_OF_DAY}, {@code DAY_OF_MONTH}, {@code MONTH}, and {@code YEAR}.
     * {@code WEEK_OF_YEAR} is not supported and throws {@code IllegalArgumentException}.</p>
     *
     * <p>Boundaries are computed on the proleptic ISO calendar (a customized
     * {@link GregorianCalendar#setGregorianChange(java.util.Date) Julian/Gregorian cutover} is not
     * applied) and are resolved in the evaluating time zone, which must be one {@link ZoneId} can
     * express &mdash; the same requirement {@code parse}, {@code format} and {@code isSameDay} impose.</p>
     *
     * <p><b>Daylight saving.</b> A boundary is a <i>resolved civil</i> boundary, not merely a local time
     * whose finer fields read zero. When an overlap repeats a nominal boundary, both occurrences are
     * boundaries and the one on the required side of the value is taken: the later occurrence for a
     * truncation at or before it, the earlier one for a ceiling after it. When the replayed window straddles
     * the unit boundary ({@code America/St_Johns} fell back from 00:01 to 23:01 until 2011; {@code Pacific/Chatham}
     * falls back from 03:45 to 02:45 every April), the clock had already struck the <i>next</i> unit's boundary
     * before falling back into this one: the truncation is then the start of the value's own civil unit, so
     * across such a transition it is monotonic in civil order rather than in instant order. When a gap removes a nominal
     * boundary, it resolves to the instant the gap ends, which is what keeps the result inside the same
     * civil unit as the input &mdash; in {@code America/Sao_Paulo}, where 4 November 2018 has no
     * midnight, truncating to {@code DATE} yields 01:00 that day rather than the previous day.
     * A rendered result can therefore show non-zero finer fields when the gap end is not aligned to the
     * requested unit: {@code Pacific/Chatham} moves its clocks from 02:45 to 03:45, so truncating
     * {@code 2025-09-28T03:45+13:45} to {@code HOUR_OF_DAY} returns that same instant &mdash; the first
     * one belonging to civil hour 03. Truncation still never moves a value forward, a ceiling never
     * moves one backward, and both always land on a real instant.</p>
     *
     * @param <T> the type of the date object, which must be a subclass of {@code java.util.Date}.
     * @param date the date to be adjusted.
     * @param field the CalendarField to which the date is to be adjusted.
     * @return a new date object of type T, adjusted to the nearest future unit as specified by the field.
     * @throws IllegalArgumentException if the date or field is {@code null}, or if the field is not supported, or the evaluating time zone carries
     *         custom daylight-saving rules that no {@link ZoneId} can represent, or a fixed offset that is not a whole number of seconds.
     * @throws ArithmeticException if the year magnitude exceeds 280 million or the ceiling
     *         epoch-millisecond value overflows.
     * @throws IllegalStateException if the registered creator, declared constructor, or {@link java.util.Date#clone()}
     *         used to build the result violates its documented runtime-type, distinct-instance, or
     *         requested-instant contract
     * @see #ceiling(java.util.Date, int)
     * @see #round(java.util.Date, CalendarField)
     * @see #truncate(java.util.Date, CalendarField)
     */
    public static <T extends java.util.Date> T ceiling(final T date, final CalendarField field)
            throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(field, cs.field);

        return ceiling(date, field.value());
    }

    /**
     * Adapted from Apache Commons Lang under Apache License v2; a value already on a boundary is returned
     * unchanged - see below.
     * <br />
     *
     * <p>Gets a date ceiling, leaving the field specified as the most
     * significant field (see <i>Daylight saving</i> below for what counts as a boundary when a
     * transition removes or repeats one). A value already exactly on the requested boundary is returned
     * unchanged (unlike Apache Commons Lang {@code DateUtils.ceiling}, which always moves
     * to the next unit).</p>
     *
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if you passed with HOUR, it would return 28 Mar
     * 2002 14:00:00.000.  If this was passed with MONTH, it would
     * return 1 Apr 2002 0:00:00.000.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseToCalendar("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * Dates.format(Dates.ceiling(cal, Calendar.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss", utc);    // returns "2024-11-24 11:00:00"
     * Dates.format(Dates.ceiling(cal, Calendar.DAY_OF_MONTH), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2024-11-25 00:00:00"
     *
     * Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);                                         // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.ceiling((Calendar) null, Calendar.HOUR_OF_DAY);                                  // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Boundaries are computed on the proleptic ISO calendar (a customized
     * {@link GregorianCalendar#setGregorianChange(java.util.Date) Julian/Gregorian cutover} is not
     * applied) and are resolved in the evaluating time zone, which must be one {@link ZoneId} can
     * express &mdash; the same requirement {@code parse}, {@code format} and {@code isSameDay} impose.</p>
     *
     * <p><b>Daylight saving.</b> A boundary is a <i>resolved civil</i> boundary, not merely a local time
     * whose finer fields read zero. When an overlap repeats a nominal boundary, both occurrences are
     * boundaries and the one on the required side of the value is taken: the later occurrence for a
     * truncation at or before it, the earlier one for a ceiling after it. When the replayed window straddles
     * the unit boundary ({@code America/St_Johns} fell back from 00:01 to 23:01 until 2011; {@code Pacific/Chatham}
     * falls back from 03:45 to 02:45 every April), the clock had already struck the <i>next</i> unit's boundary
     * before falling back into this one: the truncation is then the start of the value's own civil unit, so
     * across such a transition it is monotonic in civil order rather than in instant order. When a gap removes a nominal
     * boundary, it resolves to the instant the gap ends, which is what keeps the result inside the same
     * civil unit as the input &mdash; in {@code America/Sao_Paulo}, where 4 November 2018 has no
     * midnight, truncating to {@code DATE} yields 01:00 that day rather than the previous day.
     * A rendered result can therefore show non-zero finer fields when the gap end is not aligned to the
     * requested unit: {@code Pacific/Chatham} moves its clocks from 02:45 to 03:45, so truncating
     * {@code 2025-09-28T03:45+13:45} to {@code HOUR_OF_DAY} returns that same instant &mdash; the first
     * one belonging to civil hour 03. Truncation still never moves a value forward, a ceiling never
     * moves one backward, and both always land on a real instant.</p>
     *
     * @param <T> the type of the calendar object, which must extend java.util.Calendar.
     * @param calendar the calendar to work with, not {@code null}.
     * @param field the field from {@code Calendar} or {@link #SEMI_MONTH}. Supported values:
     *        {@code MILLISECOND}, {@code SECOND}, {@code MINUTE}, {@code HOUR}/{@code HOUR_OF_DAY},
     *        {@code AM_PM}, {@code DATE}/{@code DAY_OF_MONTH}, {@code MONTH}, {@code YEAR},
     *        and {@link #SEMI_MONTH}; any other field (including {@code ERA}) throws
     *        {@code IllegalArgumentException}.
     * @return a new calendar object of type T, adjusted to the ceiling of the specified field.
     * @throws IllegalArgumentException if the calendar is {@code null}, or if {@code field} is not a supported Calendar field, or the evaluating time
     *         zone carries custom daylight-saving rules that no {@link ZoneId} can represent, or a fixed offset that is not a whole number of
     *         seconds.
     * @throws ArithmeticException if the year magnitude exceeds 280 million in either direction (the
     *         guard tests the absolute proleptic year, so the most negative epoch values are rejected
     *         as well as the largest).
     * @throws IllegalStateException if the registered creator, declared constructor, or
     *         {@link Calendar#clone()} used to build the result violates its documented runtime-type,
     *         distinct-instance, or requested-instant contract.
     * @see #ceiling(Calendar, CalendarField)
     * @see #round(Calendar, int)
     * @see #truncate(Calendar, int)
     */
    public static <T extends Calendar> T ceiling(final T calendar, final int field)
            throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createCalendar(calendar, modifiedMillis(calendar, field, ModifyType.CEILING));
    }

    /**
     * Adjusts the given calendar to the ceiling of the specified field.
     * The original calendar object is unchanged; a new calendar object representing the adjusted time is returned.
     * This method can be used to round up the calendar to the nearest value of the specified field.
     * A value already exactly on the requested boundary is returned unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseToCalendar("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * Dates.format(Dates.ceiling(cal, CalendarField.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2024-11-24 11:00:00"
     * Dates.format(Dates.ceiling(cal, CalendarField.MONTH), "yyyy-MM-dd HH:mm:ss", utc);         // returns "2024-12-01 00:00:00"
     *
     * Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);                                             // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.ceiling((Calendar) null, CalendarField.HOUR_OF_DAY);                                 // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Accepted {@link CalendarField} values: {@code MILLISECOND}, {@code SECOND}, {@code MINUTE},
     * {@code HOUR_OF_DAY}, {@code DAY_OF_MONTH}, {@code MONTH}, and {@code YEAR}.
     * {@code WEEK_OF_YEAR} is not supported and throws {@code IllegalArgumentException}.</p>
     *
     * <p>Boundaries are computed on the proleptic ISO calendar (a customized
     * {@link GregorianCalendar#setGregorianChange(java.util.Date) Julian/Gregorian cutover} is not
     * applied) and are resolved in the evaluating time zone, which must be one {@link ZoneId} can
     * express &mdash; the same requirement {@code parse}, {@code format} and {@code isSameDay} impose.</p>
     *
     * <p><b>Daylight saving.</b> A boundary is a <i>resolved civil</i> boundary, not merely a local time
     * whose finer fields read zero. When an overlap repeats a nominal boundary, both occurrences are
     * boundaries and the one on the required side of the value is taken: the later occurrence for a
     * truncation at or before it, the earlier one for a ceiling after it. When the replayed window straddles
     * the unit boundary ({@code America/St_Johns} fell back from 00:01 to 23:01 until 2011; {@code Pacific/Chatham}
     * falls back from 03:45 to 02:45 every April), the clock had already struck the <i>next</i> unit's boundary
     * before falling back into this one: the truncation is then the start of the value's own civil unit, so
     * across such a transition it is monotonic in civil order rather than in instant order. When a gap removes a nominal
     * boundary, it resolves to the instant the gap ends, which is what keeps the result inside the same
     * civil unit as the input &mdash; in {@code America/Sao_Paulo}, where 4 November 2018 has no
     * midnight, truncating to {@code DATE} yields 01:00 that day rather than the previous day.
     * A rendered result can therefore show non-zero finer fields when the gap end is not aligned to the
     * requested unit: {@code Pacific/Chatham} moves its clocks from 02:45 to 03:45, so truncating
     * {@code 2025-09-28T03:45+13:45} to {@code HOUR_OF_DAY} returns that same instant &mdash; the first
     * one belonging to civil hour 03. Truncation still never moves a value forward, a ceiling never
     * moves one backward, and both always land on a real instant.</p>
     *
     * @param <T> the type of the calendar object, which must extend {@code java.util.Calendar}.
     * @param calendar the original calendar object to be adjusted.
     * @param field the field to be used for the ceiling operation, as a CalendarField.
     * @return a new calendar object representing the adjusted time.
     * @throws IllegalArgumentException if the calendar or field is {@code null}, or if the field is not supported, or the evaluating time zone
     *         carries custom daylight-saving rules that no {@link ZoneId} can represent, or a fixed offset that is not a whole number of seconds.
     * @throws ArithmeticException if the year magnitude exceeds 280 million in either direction (the
     *         guard tests the absolute proleptic year, so the most negative epoch values are rejected
     *         as well as the largest).
     * @throws IllegalStateException if the registered creator, declared constructor, or
     *         {@link Calendar#clone()} used to build the result violates its documented runtime-type,
     *         distinct-instance, or requested-instant contract.
     * @see #ceiling(Calendar, int)
     * @see #round(Calendar, CalendarField)
     * @see #truncate(Calendar, CalendarField)
     */
    public static <T extends Calendar> T ceiling(final T calendar, final CalendarField field)
            throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        N.checkArgNotNull(calendar, cs.calendar);
        N.checkArgNotNull(field, cs.field);

        return ceiling(calendar, field.value());
    }

    //-----------------------------------------------------------------------

    /**
     * The epoch millisecond {@code val} moves to, without touching {@code val}: the {@code Calendar}
     * overloads build their result through {@link #createCalendar(Calendar, long)} from it, so a
     * registered creator is honored rather than bypassed by mutating a clone. Evaluated in the
     * calendar's own zone, or the live default for the rare implementation whose zone is {@code null}.
     */
    private static long modifiedMillis(final Calendar val, final int field, final ModifyType modType) {
        final TimeZone timeZone = val.getTimeZone();

        return modifiedMillis(val.getTimeInMillis(), field, timeZone == null ? TimeZone.getDefault() : timeZone, modType);
    }

    /**
     * Moves {@code millis} to a boundary of {@code field} in {@code timeZone}: the greatest boundary at
     * or before it (TRUNCATE), the least boundary at or after it (CEILING), or the nearer of the two with
     * an exact tie going to the later one (ROUND).
     *
     * <p>Boundaries are the instants whose local date-time has every field below {@code field} at its
     * minimum, resolved in {@code timeZone} on the proleptic ISO calendar. A local date-time a
     * daylight-saving gap removes has no instant of its own and collapses onto the one the gap ends at;
     * a local date-time a daylight-saving overlap repeats yields <i>two</i> boundaries, one per
     * occurrence, and this method takes whichever lies on the required side of the value - the later
     * occurrence for a truncation at or before it, the earlier one for a ceiling after it. Together
     * those rules guarantee that a truncation never moves a value forward, a ceiling never moves one
     * backward, and both land on a real boundary.</p>
     *
     * <p>This is the whole engine, on primitives: the {@code Date} overloads and the {@code truncated*}
     * comparisons call it directly, so neither allocates a calendar to ask the question, and a
     * comparison neither invokes nor can fail in the subtype-reconstruction contract that building a
     * result would bring in.</p>
     *
     * @param millis the instant to move
     * @param field the calendar field to modify
     * @param timeZone the zone the boundaries belong to; not {@code null}
     * @param modType type to truncate, round or ceiling
     * @throws IllegalArgumentException if {@code field} is unsupported, or {@code timeZone} carries rules
     *         no {@link ZoneId} can express
     * @throws ArithmeticException if the year magnitude exceeds 280 million
     */
    private static long modifiedMillis(final long millis, final int field, final TimeZone timeZone, final ModifyType modType)
            throws IllegalArgumentException, ArithmeticException {
        if (!isSupportedModifyField(field)) {
            throw unsupportedModifyField(field);
        }

        // The guard used to read a year-of-era from a Calendar; the UTC proleptic year differs from it
        // by at most one, which cannot matter at a 280-million threshold, and the magnitude rejects
        // both extremes of the long range exactly as before.
        if (Math.abs((long) LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC).getYear()) > 280_000_000L) {
            throw new ArithmeticException("Calendar value too large for accurate calculations");
        }

        if (field == Calendar.MILLISECOND) {
            return millis;
        }

        // Boundaries are ISO-calendar and are resolved through java.time, so a zone java.time cannot
        // express is rejected here exactly as it is by parse, format and isSameDay.
        return boundaryMillis(millis, field, toZoneId(timeZone), modType);
    }

    /** Resolves the requested boundary of {@code field} around {@code inputMillis}. */
    private static long boundaryMillis(final long inputMillis, final int field, final ZoneId zone, final ModifyType modType) {
        final long lowerMillis = floorMillis(inputMillis, field, zone);

        if (modType == ModifyType.TRUNCATE || lowerMillis == inputMillis) {
            return modType == ModifyType.TRUNCATE ? lowerMillis : inputMillis;
        }

        final long upperMillis = nextBoundaryMillis(inputMillis, field, zone);

        if (modType == ModifyType.CEILING) {
            return upperMillis;
        }

        // Distances are bounded by the unit length, so exact subtraction cannot overflow here
        // (the year range is guarded above). Ties go to the upper boundary.
        return Math.subtractExact(upperMillis, inputMillis) > Math.subtractExact(inputMillis, lowerMillis) ? lowerMillis : upperMillis;
    }

    /**
     * How many offset regimes a boundary search will cross before giving up. One regime per pass, against
     * a measured worst case of four transitions in a single year across all of the IANA data - the widest
     * span any supported field covers.
     */
    private static final int MAX_OFFSET_REGIMES = 32;

    /**
     * The greatest boundary of {@code field} at or before {@code millis}.
     *
     * <p>Within one offset regime the local clock is a fixed shift of the instant timeline, so flooring
     * the local date-time and mapping it back with the same offset is exact. When that mapping escapes
     * the regime the regime's own start explains why: a daylight-saving gap removed the local boundary,
     * and it collapses onto the instant the gap ends; an overlap means the boundary is a real one in the
     * previous regime, which is where the search continues.</p>
     */
    private static long floorMillis(final long millis, final int field, final ZoneId zone) {
        final ZoneRules rules = zone.getRules();
        long cursor = millis;

        // Each pass steps back exactly one offset regime, so the bound is the number of transitions a
        // zone can put between an instant and the boundary below it. The busiest zones in the IANA data
        // manage four transitions in a year, the widest span any supported field covers.
        for (int regime = 0; regime < MAX_OFFSET_REGIMES; regime++) {
            final ZoneOffset offset = rules.getOffset(Instant.ofEpochMilli(cursor));
            final LocalDateTime local = floorLocal(localAt(cursor, offset), field);
            final long candidate = instantAt(local, offset);
            final List<ZoneOffset> validOffsets = rules.getValidOffsets(local);

            if (offset.equals(rules.getOffset(Instant.ofEpochMilli(candidate)))) {
                if (validOffsets.size() < 2) {
                    return candidate;
                }

                // A fall-back replays the local boundary, and its second occurrence may also be at or
                // before the input - in which case that later one is the floor.
                long latest = candidate;

                for (final ZoneOffset replay : validOffsets) {
                    final long occurrence = instantAt(local, replay);

                    if (occurrence <= millis && occurrence > latest) {
                        latest = occurrence;
                    }
                }

                return latest;
            }

            if (validOffsets.isEmpty()) {
                // a daylight-saving gap removed this local boundary; it collapses onto the gap's end
                return rules.getTransition(local).getInstant().toEpochMilli();
            }

            // The boundary is real, but only at another offset: the input sits in the part of its unit that a
            // fall-back replays. Its latest occurrence at or before the input is the floor - the input's OWN
            // nominal boundary - even when the previous regime's clock had already crossed a later boundary
            // before falling back (America/St_Johns fell back from 00:01 to 23:01 through 2010, so Nov 7 00:00
            // preceded the replayed Nov 6 23:xx; Pacific/Chatham falls back from 03:45 to 02:45 every April):
            // that later boundary begins the NEXT civil unit, and the value is not in it. Stepping back a
            // regime and flooring there used to return exactly that later boundary.
            long latest = Long.MIN_VALUE;

            for (final ZoneOffset other : validOffsets) {
                final long occurrence = instantAt(local, other);

                if (occurrence <= millis && occurrence > latest) {
                    latest = occurrence;
                }
            }

            if (latest != Long.MIN_VALUE) {
                return latest;
            }

            // no occurrence at or before the input (not reached for any published rule set): the boundary is in
            // an earlier regime, which is where the search continues
            final ZoneOffsetTransition regimeStart = rules.previousTransition(Instant.ofEpochMilli(cursor).plusMillis(1));

            if (regimeStart == null) {
                return candidate;
            }

            cursor = regimeStart.getInstant().toEpochMilli() - 1;
        }

        throw unresolvedBoundary("greatest boundary at or before", millis, field, zone);
    }

    /**
     * The least boundary of {@code field} strictly after {@code millis}.
     *
     * <p>The mirror of {@link #floorMillis(long, int, ZoneId)}: take the next local boundary in the
     * input's own offset regime, and if that lands outside the regime, the first boundary of the next
     * regime instead. The transition instant itself is checked first, because a gap collapses a local
     * boundary onto it and a fall-back can put the second occurrence of an already-passed local boundary
     * milliseconds after the input &mdash; both far nearer than the next local boundary.</p>
     */
    private static long nextBoundaryMillis(final long millis, final int field, final ZoneId zone) {
        final ZoneRules rules = zone.getRules();
        long cursor = millis;

        // See floorMillis: one offset regime per pass, against a worst case of four transitions a year.
        for (int regime = 0; regime < MAX_OFFSET_REGIMES; regime++) {
            final ZoneOffset offset = rules.getOffset(Instant.ofEpochMilli(cursor));
            final long candidate = instantAt(plusOneUnit(floorLocal(localAt(cursor, offset), field), field), offset);

            if (candidate > millis && offset.equals(rules.getOffset(Instant.ofEpochMilli(candidate)))) {
                return candidate;
            }

            final ZoneOffsetTransition next = rules.nextTransition(Instant.ofEpochMilli(cursor));

            if (next == null) {
                return candidate;
            }

            final long transition = next.getInstant().toEpochMilli();

            if (transition > millis && floorMillis(transition, field, zone) == transition) {
                return transition;
            }

            cursor = transition;
        }

        throw unresolvedBoundary("least boundary after", millis, field, zone);
    }

    /**
     * The regime walk exhausted its bound, so the value it would return is not a boundary. Unreachable
     * for any published zone rule set; raised rather than returned so a rule set that ever broke the
     * assumption fails visibly instead of yielding a wrong date.
     */
    private static IllegalStateException unresolvedBoundary(final String which, final long millis, final int field, final ZoneId zone) {
        return new IllegalStateException("Could not resolve the " + which + " " + Instant.ofEpochMilli(millis) + " for calendar field " + field + " in zone "
                + zone + " within " + MAX_OFFSET_REGIMES + " offset regimes");
    }

    /** The local date-time {@code millis} reads as at {@code offset}. */
    private static LocalDateTime localAt(final long millis, final ZoneOffset offset) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), offset);
    }

    /** The instant at which {@code local} occurs at {@code offset}. */
    private static long instantAt(final LocalDateTime local, final ZoneOffset offset) {
        return local.toInstant(offset).toEpochMilli();
    }

    /** Zeroes every local field below {@code field}. */
    private static LocalDateTime floorLocal(final LocalDateTime local, final int field) {
        switch (field) {
            case Calendar.SECOND:
                return local.truncatedTo(ChronoUnit.SECONDS);

            case Calendar.MINUTE:
                return local.truncatedTo(ChronoUnit.MINUTES);

            case Calendar.HOUR: // HOUR and HOUR_OF_DAY denote the same one-hour quantity
            case Calendar.HOUR_OF_DAY:
                return local.truncatedTo(ChronoUnit.HOURS);

            case Calendar.AM_PM:
                return local.toLocalDate().atTime(local.getHour() < 12 ? 0 : 12, 0);

            case Calendar.DATE: // == Calendar.DAY_OF_MONTH
                return local.toLocalDate().atStartOfDay();

            case SEMI_MONTH:
                return local.toLocalDate().withDayOfMonth(local.getDayOfMonth() < 16 ? 1 : 16).atStartOfDay();

            case Calendar.MONTH:
                return local.toLocalDate().withDayOfMonth(1).atStartOfDay();

            case Calendar.YEAR:
                return local.toLocalDate().withDayOfYear(1).atStartOfDay();

            default:
                throw unsupportedModifyField(field);
        }
    }

    /** Advances a local boundary of {@code field} to the next one. */
    private static LocalDateTime plusOneUnit(final LocalDateTime local, final int field) {
        switch (field) {
            case Calendar.SECOND:
                return local.plusSeconds(1);

            case Calendar.MINUTE:
                return local.plusMinutes(1);

            case Calendar.HOUR:
            case Calendar.HOUR_OF_DAY:
                return local.plusHours(1);

            case Calendar.AM_PM:
                return local.plusHours(12);

            case Calendar.DATE:
                return local.plusDays(1);

            case SEMI_MONTH: {
                final LocalDate date = local.toLocalDate();
                return (date.getDayOfMonth() < 16 ? date.withDayOfMonth(16) : date.withDayOfMonth(1).plusMonths(1)).atStartOfDay();
            }

            case Calendar.MONTH:
                return local.plusMonths(1);

            case Calendar.YEAR:
                return local.plusYears(1);

            default:
                throw unsupportedModifyField(field);
        }
    }

    private static boolean isSupportedModifyField(final int field) {
        return field == Calendar.YEAR || field == Calendar.MONTH || field == Calendar.DATE || field == Calendar.AM_PM || field == Calendar.HOUR
                || field == Calendar.HOUR_OF_DAY || field == Calendar.MINUTE || field == Calendar.SECOND || field == Calendar.MILLISECOND
                || field == SEMI_MONTH;
    }

    private static IllegalArgumentException unsupportedModifyField(final int field) {
        return new IllegalArgumentException("Unsupported field: " + fieldName(field)
                + ". Supported fields are Calendar.YEAR, MONTH, DATE, AM_PM, HOUR/HOUR_OF_DAY, MINUTE, SECOND, MILLISECOND and Dates.SEMI_MONTH");
    }

    private static String fieldName(final int field) {
        switch (field) {
            case Calendar.ERA:
                return "ERA";
            case Calendar.YEAR:
                return "YEAR";
            case Calendar.MONTH:
                return "MONTH";
            case Calendar.WEEK_OF_YEAR:
                return "WEEK_OF_YEAR";
            case Calendar.WEEK_OF_MONTH:
                return "WEEK_OF_MONTH";
            case Calendar.DATE:
                return "DATE";
            case Calendar.DAY_OF_YEAR:
                return "DAY_OF_YEAR";
            case Calendar.DAY_OF_WEEK:
                return "DAY_OF_WEEK";
            case Calendar.DAY_OF_WEEK_IN_MONTH:
                return "DAY_OF_WEEK_IN_MONTH";
            case Calendar.AM_PM:
                return "AM_PM";
            case Calendar.HOUR:
                return "HOUR";
            case Calendar.HOUR_OF_DAY:
                return "HOUR_OF_DAY";
            case Calendar.MINUTE:
                return "MINUTE";
            case Calendar.SECOND:
                return "SECOND";
            case Calendar.MILLISECOND:
                return "MILLISECOND";
            case SEMI_MONTH:
                return "SEMI_MONTH";
            default:
                return "unknown (" + field + ")";
        }
    }

    /**
     * Determines if two calendars are equal up to no more than the specified most significant field.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal1 = Dates.parseToCalendar("2023-03-28 13:45:30", "yyyy-MM-dd HH:mm:ss");
     * Calendar cal2 = Dates.parseToCalendar("2023-03-28 18:20:15", "yyyy-MM-dd HH:mm:ss");
     * Dates.truncatedEquals(cal1, cal2, CalendarField.DAY_OF_MONTH);              // returns true (same day)
     * Dates.truncatedEquals(cal1, cal2, CalendarField.HOUR_OF_DAY);               // returns false (13:00 vs 18:00)
     *
     * Dates.truncatedEquals(cal1, cal1, CalendarField.SECOND);                    // returns true (identical)
     * Dates.truncatedEquals((Calendar) null, cal2, CalendarField.DAY_OF_MONTH);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Accepted {@link CalendarField} values are those of {@link #truncate(Calendar, CalendarField)}:
     * {@code MILLISECOND}, {@code SECOND}, {@code MINUTE}, {@code HOUR_OF_DAY}, {@code DAY_OF_MONTH},
     * {@code MONTH}, and {@code YEAR}. {@code WEEK_OF_YEAR} throws {@code IllegalArgumentException}.</p>
     *
     * <p>Each calendar is truncated in <b>its own</b> time zone, and the two truncated instants are then
     * compared, not the civil fields. Nothing is built to do so: the comparison works on the boundary
     * instants directly, so a registered creator is neither invoked nor able to fail it.
     * Two calendars at the same instant but in different
     * zones can therefore compare unequal at {@code DAY_OF_MONTH} <i>even when both show the same civil
     * day</i>, when their day boundaries occur at different instants. Unlike
     * {@link #isSameDay(Calendar, Calendar)}, this does not require the two zones to agree; put both
     * instants in one zone when you want a single calendar-day comparison. Even in one zone, equality is
     * equality of the two boundary <i>instants</i>: on a day whose midnight a fall-back replays
     * ({@code America/Havana} every November), a value in the first 00:xx hour and one in the replayed 00:xx hour
     * truncate to different instants and compare unequal at {@code DAY_OF_MONTH}, while
     * {@link #isSameDay(Calendar, Calendar)} says they share the date.</p>
     *
     * @param cal1 the first calendar, not {@code null}.
     * @param cal2 the second calendar, not {@code null}.
     * @param field the field from {@code CalendarField} to be the most significant field for comparison.
     * @return {@code true} if cal1 and cal2 are equal up to the specified field; {@code false} otherwise.
     * @throws IllegalArgumentException if any argument is {@code null}, if {@code field} is not a
     *         supported field, or if the evaluating time zone carries custom daylight-saving rules that
     *         no {@link ZoneId} can represent.
     * @throws ArithmeticException if the year magnitude exceeds 280 million.
     * @see #truncate(Calendar, CalendarField)
     * @see #truncatedEquals(java.util.Date, java.util.Date, CalendarField)
     */
    public static boolean truncatedEquals(final Calendar cal1, final Calendar cal2, final CalendarField field)
            throws IllegalArgumentException, ArithmeticException {
        return truncatedCompareTo(cal1, cal2, field) == 0;
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Determines if two calendars are equal up to no more than the specified most significant field.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal1 = Dates.parseToCalendar("2023-03-28 13:45:30", "yyyy-MM-dd HH:mm:ss");
     * Calendar cal2 = Dates.parseToCalendar("2023-03-28 18:20:15", "yyyy-MM-dd HH:mm:ss");
     * Dates.truncatedEquals(cal1, cal2, Calendar.DAY_OF_MONTH);              // returns true (same day)
     * Dates.truncatedEquals(cal1, cal2, Calendar.HOUR_OF_DAY);               // returns false (13:00 vs 18:00)
     *
     * Dates.truncatedEquals(cal1, cal1, Calendar.SECOND);                    // returns true (identical)
     * Dates.truncatedEquals((Calendar) null, cal2, Calendar.DAY_OF_MONTH);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Each calendar is truncated in <b>its own</b> time zone, and the two truncated instants are then
     * compared, not the civil fields. Nothing is built to do so: the comparison works on the boundary
     * instants directly, so a registered creator is neither invoked nor able to fail it.
     * Two calendars at the same instant but in different
     * zones can therefore compare unequal at {@code DAY_OF_MONTH} <i>even when both show the same civil
     * day</i>, when their day boundaries occur at different instants. Unlike
     * {@link #isSameDay(Calendar, Calendar)}, this does not require the two zones to agree; put both
     * instants in one zone when you want a single calendar-day comparison. Even in one zone, equality is
     * equality of the two boundary <i>instants</i>: on a day whose midnight a fall-back replays
     * ({@code America/Havana} every November), a value in the first 00:xx hour and one in the replayed 00:xx hour
     * truncate to different instants and compare unequal at {@code DAY_OF_MONTH}, while
     * {@link #isSameDay(Calendar, Calendar)} says they share the date.</p>
     *
     * @param cal1 the first calendar, not {@code null}.
     * @param cal2 the second calendar, not {@code null}.
     * @param field the field from {@code Calendar} or {@link #SEMI_MONTH}. Supported values:
     *        {@code MILLISECOND}, {@code SECOND}, {@code MINUTE}, {@code HOUR}/{@code HOUR_OF_DAY},
     *        {@code AM_PM}, {@code DATE}/{@code DAY_OF_MONTH}, {@code MONTH}, {@code YEAR},
     *        and {@link #SEMI_MONTH}; any other field (including {@code ERA}) throws
     *        {@code IllegalArgumentException}.
     * @return {@code true} if equal; otherwise {@code false}.
     * @throws IllegalArgumentException if any argument is {@code null}, if {@code field} is not a
     *         supported field, or if the evaluating time zone carries custom daylight-saving rules that
     *         no {@link ZoneId} can represent.
     * @throws ArithmeticException if the year magnitude exceeds 280 million.
     * @see #truncate(Calendar, int)
     * @see #truncatedEquals(java.util.Date, java.util.Date, int)
     */
    public static boolean truncatedEquals(final Calendar cal1, final Calendar cal2, final int field) throws IllegalArgumentException, ArithmeticException {
        return truncatedCompareTo(cal1, cal2, field) == 0;
    }

    /**
     * Determines if two dates are equal up to no more than the specified most significant field.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date1 = Dates.parseToJUDate("2023-03-28 13:45:30", "yyyy-MM-dd HH:mm:ss");
     * java.util.Date date2 = Dates.parseToJUDate("2023-03-28 18:20:15", "yyyy-MM-dd HH:mm:ss");
     * Dates.truncatedEquals(date1, date2, CalendarField.DAY_OF_MONTH);                   // returns true (same day)
     * Dates.truncatedEquals(date1, date2, CalendarField.HOUR_OF_DAY);                    // returns false (13:00 vs 18:00)
     *
     * Dates.truncatedEquals(date1, date1, CalendarField.SECOND);                         // returns true (identical)
     * Dates.truncatedEquals((java.util.Date) null, date2, CalendarField.DAY_OF_MONTH);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Accepted {@link CalendarField} values are those of {@link #truncate(java.util.Date, CalendarField)}:
     * {@code MILLISECOND}, {@code SECOND}, {@code MINUTE}, {@code HOUR_OF_DAY}, {@code DAY_OF_MONTH},
     * {@code MONTH}, and {@code YEAR}. {@code WEEK_OF_YEAR} throws {@code IllegalArgumentException}.</p>
     *
     * <p>Equality is equality of the two boundary <i>instants</i> the values truncate to, which is not always
     * the same civil period: on a day whose midnight a fall-back replays ({@code America/Havana} every November,
     * {@code Atlantic/Azores}), both midnights are boundaries, so a value in the first 00:xx hour and one in the
     * replayed 00:xx hour truncate to different instants and compare unequal at {@code DAY_OF_MONTH} although
     * {@link #isSameDay(java.util.Date, java.util.Date)} says they share the date. Use the {@code isSame*}
     * methods for civil-date equality; this method answers whether the two values fall into the same
     * <i>resolved</i> period.</p>
     *
     * <p>Both values are truncated in the JVM default time zone; use the {@code Calendar} overloads
     * when the zone must not follow {@link TimeZone#getDefault()}. Nothing is built to compare them:
     * the comparison works on the boundary instants directly, so a registered creator is neither
     * invoked nor able to fail it.</p>
     *
     * @param date1 the first date, not {@code null}.
     * @param date2 the second date, not {@code null}.
     * @param field the field from {@code CalendarField} to be the most significant field for comparison.
     * @return {@code true} if date1 and date2 are equal up to the specified field; {@code false} otherwise.
     * @throws IllegalArgumentException if any argument is {@code null}, if {@code field} is not a
     *         supported field, or if the evaluating time zone carries custom daylight-saving rules that
     *         no {@link ZoneId} can represent.
     * @throws ArithmeticException if the year magnitude exceeds 280 million.
     * @see #truncate(java.util.Date, CalendarField)
     * @see #truncatedEquals(Calendar, Calendar, CalendarField)
     */
    public static boolean truncatedEquals(final java.util.Date date1, final java.util.Date date2, final CalendarField field)
            throws IllegalArgumentException, ArithmeticException {
        return truncatedCompareTo(date1, date2, field) == 0;
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Determines if two dates are equal up to no more than the specified
     * most significant field.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date1 = Dates.parseToJUDate("2023-03-28 13:45:30", "yyyy-MM-dd HH:mm:ss");
     * java.util.Date date2 = Dates.parseToJUDate("2023-03-28 18:20:15", "yyyy-MM-dd HH:mm:ss");
     * Dates.truncatedEquals(date1, date2, Calendar.DAY_OF_MONTH);                   // returns true (same day)
     * Dates.truncatedEquals(date1, date2, Calendar.HOUR_OF_DAY);                    // returns false (13:00 vs 18:00)
     *
     * Dates.truncatedEquals(date1, date1, Calendar.SECOND);                         // returns true (identical)
     * Dates.truncatedEquals((java.util.Date) null, date2, Calendar.DAY_OF_MONTH);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Equality is equality of the two boundary <i>instants</i> the values truncate to, which is not always
     * the same civil period: on a day whose midnight a fall-back replays ({@code America/Havana} every November,
     * {@code Atlantic/Azores}), both midnights are boundaries, so a value in the first 00:xx hour and one in the
     * replayed 00:xx hour truncate to different instants and compare unequal at {@code DAY_OF_MONTH} although
     * {@link #isSameDay(java.util.Date, java.util.Date)} says they share the date. Use the {@code isSame*}
     * methods for civil-date equality; this method answers whether the two values fall into the same
     * <i>resolved</i> period.</p>
     *
     * <p>Both values are truncated in the JVM default time zone; use the {@code Calendar} overloads
     * when the zone must not follow {@link TimeZone#getDefault()}. Nothing is built to compare them:
     * the comparison works on the boundary instants directly, so a registered creator is neither
     * invoked nor able to fail it.</p>
     *
     * @param date1 the first date, not {@code null}.
     * @param date2 the second date, not {@code null}.
     * @param field the field from {@code Calendar} or {@link #SEMI_MONTH}. Supported values:
     *        {@code MILLISECOND}, {@code SECOND}, {@code MINUTE}, {@code HOUR}/{@code HOUR_OF_DAY},
     *        {@code AM_PM}, {@code DATE}/{@code DAY_OF_MONTH}, {@code MONTH}, {@code YEAR},
     *        and {@link #SEMI_MONTH}; any other field (including {@code ERA}) throws
     *        {@code IllegalArgumentException}.
     * @return {@code true} if equal; otherwise {@code false}.
     * @throws IllegalArgumentException if any argument is {@code null}, if {@code field} is not a
     *         supported field, or if the evaluating time zone carries custom daylight-saving rules that
     *         no {@link ZoneId} can represent.
     * @throws ArithmeticException if the year magnitude exceeds 280 million.
     * @see #truncate(java.util.Date, int)
     * @see #truncatedEquals(Calendar, Calendar, int)
     */
    public static boolean truncatedEquals(final java.util.Date date1, final java.util.Date date2, final int field)
            throws IllegalArgumentException, ArithmeticException {
        return truncatedCompareTo(date1, date2, field) == 0;
    }

    /**
     * Compares two Calendar instances up to the specified field.
     * The comparison is based on the most significant field, meaning that it compares
     * the Calendar instances year by year, month by month, day by day, etc., depending on the specified field.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal1 = Dates.parseToCalendar("2023-03-28 13:45:30", "yyyy-MM-dd HH:mm:ss");
     * Calendar cal2 = Dates.parseToCalendar("2023-03-28 18:20:15", "yyyy-MM-dd HH:mm:ss");
     * Dates.truncatedCompareTo(cal1, cal2, CalendarField.DAY_OF_MONTH);                   // returns 0 (same day)
     * assert Dates.truncatedCompareTo(cal1, cal2, CalendarField.HOUR_OF_DAY) < 0;         // comparison holds (13:00 < 18:00)
     * assert Dates.truncatedCompareTo(cal2, cal1, CalendarField.HOUR_OF_DAY) > 0;         // comparison holds (18:00 > 13:00)
     *
     * Dates.truncatedCompareTo((Calendar) null, cal2, CalendarField.DAY_OF_MONTH);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Accepted {@link CalendarField} values are those of {@link #truncate(Calendar, CalendarField)}:
     * {@code MILLISECOND}, {@code SECOND}, {@code MINUTE}, {@code HOUR_OF_DAY}, {@code DAY_OF_MONTH},
     * {@code MONTH}, and {@code YEAR}. {@code WEEK_OF_YEAR} throws {@code IllegalArgumentException}.</p>
     *
     * <p>Each calendar is truncated in <b>its own</b> time zone, and the two truncated instants are then
     * compared, not the civil fields. Nothing is built to do so: the comparison works on the boundary
     * instants directly, so a registered creator is neither invoked nor able to fail it.
     * Two calendars at the same instant but in different
     * zones can therefore compare unequal at {@code DAY_OF_MONTH} <i>even when both show the same civil
     * day</i>, when their day boundaries occur at different instants. Unlike
     * {@link #isSameDay(Calendar, Calendar)}, this does not require the two zones to agree; put both
     * instants in one zone when you want a single calendar-day comparison. Even in one zone, equality is
     * equality of the two boundary <i>instants</i>: on a day whose midnight a fall-back replays
     * ({@code America/Havana} every November), a value in the first 00:xx hour and one in the replayed 00:xx hour
     * truncate to different instants and compare unequal at {@code DAY_OF_MONTH}, while
     * {@link #isSameDay(Calendar, Calendar)} says they share the date.</p>
     *
     * @param cal1 the first Calendar instance to be compared, not {@code null}.
     * @param cal2 the second Calendar instance to be compared, not {@code null}.
     * @param field the field from {@code CalendarField} to be the most significant field for comparison.
     * @return a negative integer, zero, or a positive integer as the first Calendar is less than, equal to, or greater than the second.
     * @throws IllegalArgumentException if any argument is {@code null}, if {@code field} is not a
     *         supported field, or if the evaluating time zone carries custom daylight-saving rules that
     *         no {@link ZoneId} can represent.
     * @throws ArithmeticException if the year magnitude exceeds 280 million.
     * @see #truncate(Calendar, CalendarField)
     * @see #truncatedCompareTo(java.util.Date, java.util.Date, CalendarField)
     */
    public static int truncatedCompareTo(final Calendar cal1, final Calendar cal2, final CalendarField field)
            throws IllegalArgumentException, ArithmeticException {
        return truncatedCompareTo(cal1, cal2, N.checkArgNotNull(field, cs.field).value());
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Determines how two calendars compare up to no more than the specified most significant field.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal1 = Dates.parseToCalendar("2023-03-28 13:45:30", "yyyy-MM-dd HH:mm:ss");
     * Calendar cal2 = Dates.parseToCalendar("2023-03-28 18:20:15", "yyyy-MM-dd HH:mm:ss");
     * Dates.truncatedCompareTo(cal1, cal2, Calendar.DAY_OF_MONTH);                   // returns 0 (same day)
     * assert Dates.truncatedCompareTo(cal1, cal2, Calendar.HOUR_OF_DAY) < 0;         // comparison holds (13:00 < 18:00)
     * assert Dates.truncatedCompareTo(cal2, cal1, Calendar.HOUR_OF_DAY) > 0;         // comparison holds (18:00 > 13:00)
     *
     * Dates.truncatedCompareTo((Calendar) null, cal2, Calendar.DAY_OF_MONTH);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Each calendar is truncated in <b>its own</b> time zone, and the two truncated instants are then
     * compared, not the civil fields. Nothing is built to do so: the comparison works on the boundary
     * instants directly, so a registered creator is neither invoked nor able to fail it.
     * Two calendars at the same instant but in different
     * zones can therefore compare unequal at {@code DAY_OF_MONTH} <i>even when both show the same civil
     * day</i>, when their day boundaries occur at different instants. Unlike
     * {@link #isSameDay(Calendar, Calendar)}, this does not require the two zones to agree; put both
     * instants in one zone when you want a single calendar-day comparison. Even in one zone, equality is
     * equality of the two boundary <i>instants</i>: on a day whose midnight a fall-back replays
     * ({@code America/Havana} every November), a value in the first 00:xx hour and one in the replayed 00:xx hour
     * truncate to different instants and compare unequal at {@code DAY_OF_MONTH}, while
     * {@link #isSameDay(Calendar, Calendar)} says they share the date.</p>
     *
     * @param cal1 the first calendar, not {@code null}.
     * @param cal2 the second calendar, not {@code null}.
     * @param field the field from {@code Calendar} or {@link #SEMI_MONTH}. Supported values:
     *        {@code MILLISECOND}, {@code SECOND}, {@code MINUTE}, {@code HOUR}/{@code HOUR_OF_DAY},
     *        {@code AM_PM}, {@code DATE}/{@code DAY_OF_MONTH}, {@code MONTH}, {@code YEAR},
     *        and {@link #SEMI_MONTH}; any other field (including {@code ERA}) throws
     *        {@code IllegalArgumentException}.
     * @return a negative integer, zero, or a positive integer as the first
     * calendar is less than, equal to, or greater than the second.
     * @throws IllegalArgumentException if any argument is {@code null}, if {@code field} is not a
     *         supported field, or if the evaluating time zone carries custom daylight-saving rules that
     *         no {@link ZoneId} can represent.
     * @throws ArithmeticException if the year magnitude exceeds 280 million.
     * @see #truncate(Calendar, int)
     * @see #truncatedCompareTo(java.util.Date, java.util.Date, int)
     */
    public static int truncatedCompareTo(final Calendar cal1, final Calendar cal2, final int field) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);

        return Long.compare(modifiedMillis(cal1, field, ModifyType.TRUNCATE), modifiedMillis(cal2, field, ModifyType.TRUNCATE));
    }

    /**
     * Compares two Date instances up to the specified field.
     * The comparison is based on the most significant field, meaning that it compares
     * the Date instances year by year, month by month, day by day, etc., depending on the specified field.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date1 = Dates.parseToJUDate("2023-03-28 13:45:30", "yyyy-MM-dd HH:mm:ss");
     * java.util.Date date2 = Dates.parseToJUDate("2023-03-28 18:20:15", "yyyy-MM-dd HH:mm:ss");
     * Dates.truncatedCompareTo(date1, date2, CalendarField.DAY_OF_MONTH);                        // returns 0 (same day)
     * assert Dates.truncatedCompareTo(date1, date2, CalendarField.HOUR_OF_DAY) < 0;              // comparison holds (13:00 < 18:00)
     * assert Dates.truncatedCompareTo(date2, date1, CalendarField.HOUR_OF_DAY) > 0;              // comparison holds (18:00 > 13:00)
     *
     * Dates.truncatedCompareTo((java.util.Date) null, date2, CalendarField.DAY_OF_MONTH);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Accepted {@link CalendarField} values are those of {@link #truncate(java.util.Date, CalendarField)}:
     * {@code MILLISECOND}, {@code SECOND}, {@code MINUTE}, {@code HOUR_OF_DAY}, {@code DAY_OF_MONTH},
     * {@code MONTH}, and {@code YEAR}. {@code WEEK_OF_YEAR} throws {@code IllegalArgumentException}.</p>
     *
     * <p>Equality is equality of the two boundary <i>instants</i> the values truncate to, which is not always
     * the same civil period: on a day whose midnight a fall-back replays ({@code America/Havana} every November,
     * {@code Atlantic/Azores}), both midnights are boundaries, so a value in the first 00:xx hour and one in the
     * replayed 00:xx hour truncate to different instants and compare unequal at {@code DAY_OF_MONTH} although
     * {@link #isSameDay(java.util.Date, java.util.Date)} says they share the date. Use the {@code isSame*}
     * methods for civil-date equality; this method answers whether the two values fall into the same
     * <i>resolved</i> period.</p>
     *
     * <p>Both values are truncated in the JVM default time zone; use the {@code Calendar} overloads
     * when the zone must not follow {@link TimeZone#getDefault()}. Nothing is built to compare them:
     * the comparison works on the boundary instants directly, so a registered creator is neither
     * invoked nor able to fail it.</p>
     *
     * @param date1 the first Date instance to be compared, not {@code null}.
     * @param date2 the second Date instance to be compared, not {@code null}.
     * @param field the field from {@code CalendarField} to be the most significant field for comparison.
     * @return a negative integer, zero, or a positive integer as the first Date is less than, equal to, or greater than the second.
     * @throws IllegalArgumentException if any argument is {@code null}, if {@code field} is not a
     *         supported field, or if the evaluating time zone carries custom daylight-saving rules that
     *         no {@link ZoneId} can represent.
     * @throws ArithmeticException if the year magnitude exceeds 280 million.
     * @see #truncate(java.util.Date, CalendarField)
     * @see #truncatedCompareTo(Calendar, Calendar, CalendarField)
     */
    public static int truncatedCompareTo(final java.util.Date date1, final java.util.Date date2, final CalendarField field)
            throws IllegalArgumentException, ArithmeticException {
        return truncatedCompareTo(date1, date2, N.checkArgNotNull(field, cs.field).value());
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Determines how two dates compare up to no more than the specified
     * most significant field.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date1 = Dates.parseToJUDate("2023-03-28 13:45:30", "yyyy-MM-dd HH:mm:ss");
     * java.util.Date date2 = Dates.parseToJUDate("2023-03-28 18:20:15", "yyyy-MM-dd HH:mm:ss");
     * Dates.truncatedCompareTo(date1, date2, Calendar.DAY_OF_MONTH);                        // returns 0 (same day)
     * assert Dates.truncatedCompareTo(date1, date2, Calendar.HOUR_OF_DAY) < 0;              // comparison holds (13:00 < 18:00)
     * assert Dates.truncatedCompareTo(date2, date1, Calendar.HOUR_OF_DAY) > 0;              // comparison holds (18:00 > 13:00)
     *
     * Dates.truncatedCompareTo((java.util.Date) null, date2, Calendar.DAY_OF_MONTH);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Equality is equality of the two boundary <i>instants</i> the values truncate to, which is not always
     * the same civil period: on a day whose midnight a fall-back replays ({@code America/Havana} every November,
     * {@code Atlantic/Azores}), both midnights are boundaries, so a value in the first 00:xx hour and one in the
     * replayed 00:xx hour truncate to different instants and compare unequal at {@code DAY_OF_MONTH} although
     * {@link #isSameDay(java.util.Date, java.util.Date)} says they share the date. Use the {@code isSame*}
     * methods for civil-date equality; this method answers whether the two values fall into the same
     * <i>resolved</i> period.</p>
     *
     * <p>Both values are truncated in the JVM default time zone; use the {@code Calendar} overloads
     * when the zone must not follow {@link TimeZone#getDefault()}. Nothing is built to compare them:
     * the comparison works on the boundary instants directly, so a registered creator is neither
     * invoked nor able to fail it.</p>
     *
     * @param date1 the first date, not {@code null}.
     * @param date2 the second date, not {@code null}.
     * @param field the field from {@code Calendar} or {@link #SEMI_MONTH}. Supported values:
     *        {@code MILLISECOND}, {@code SECOND}, {@code MINUTE}, {@code HOUR}/{@code HOUR_OF_DAY},
     *        {@code AM_PM}, {@code DATE}/{@code DAY_OF_MONTH}, {@code MONTH}, {@code YEAR},
     *        and {@link #SEMI_MONTH}; any other field (including {@code ERA}) throws
     *        {@code IllegalArgumentException}.
     * @return a negative integer, zero, or a positive integer as the first
     * date is less than, equal to, or greater than the second.
     * @throws IllegalArgumentException if any argument is {@code null}, if {@code field} is not a
     *         supported field, or if the evaluating time zone carries custom daylight-saving rules that
     *         no {@link ZoneId} can represent.
     * @throws ArithmeticException if the year magnitude exceeds 280 million.
     * @see #truncate(java.util.Date, int)
     * @see #truncatedCompareTo(Calendar, Calendar, int)
     */
    public static int truncatedCompareTo(final java.util.Date date1, final java.util.Date date2, final int field)
            throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(date1, cs.date1);
        N.checkArgNotNull(date2, cs.date2);

        // One read of the default, so both values are truncated in the same zone.
        final TimeZone timeZone = TimeZone.getDefault();

        return Long.compare(modifiedMillis(date1.getTime(), field, timeZone, ModifyType.TRUNCATE),
                modifiedMillis(date2.getTime(), field, timeZone, ModifyType.TRUNCATE));
    }

    /**
     * Adapted from Apache Commons Lang under Apache License v2; the {@code Date} overloads evaluate on a
     * proleptic Gregorian calendar whatever calendar system the default locale selects.
     * <br />
     *
     * <p>Returns the number of milliseconds within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the milliseconds of any date will only return the number of milliseconds
     * of the current second (resulting in a number between 0 and 999). This
     * method will retrieve the number of milliseconds for any fragment.
     * For example, if you want to calculate the number of milliseconds past today,
     * your fragment is {@link CalendarField#DAY_OF_MONTH}. The result will
     * be all milliseconds of the past hour(s), minute(s) and second(s).</p>
     *
     * <p>Accepted {@link CalendarField} fragments: {@code YEAR}, {@code MONTH}, {@code DAY_OF_MONTH},
     * {@code HOUR_OF_DAY}, {@code MINUTE}, {@code SECOND}, and {@code MILLISECOND}.
     * {@code WEEK_OF_YEAR} is not a fragment field and throws {@code IllegalArgumentException}.
     * A fragment equal to or finer than the requested unit returns 0.</p>
     *
     * <p>The result sums <i>civil field values</i>, not elapsed time: a day always counts as 24 hours,
     * so across a daylight-saving transition the figure differs from the elapsed duration by the size
     * of the shift.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with CalendarField.SECOND as fragment will return 538</li>
     *  <li>January 6, 2008 7:15:10.538 with CalendarField.SECOND as fragment will return 538</li>
     *  <li>January 6, 2008 7:15:10.538 with CalendarField.MINUTE as fragment will return 10538 (10*1000 + 538)</li>
     *  <li>January 16, 2008 7:15:10.538 with CalendarField.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in milliseconds)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2023-01-06 07:15:10.538", "yyyy-MM-dd HH:mm:ss.SSS");
     * Dates.getFragmentInMilliseconds(date, CalendarField.SECOND);                    // returns 538 (millis within the current second)
     * Dates.getFragmentInMilliseconds(date, CalendarField.MINUTE);                    // returns 10538 (10*1000 + 538)
     *
     * Dates.getFragmentInMilliseconds(date, CalendarField.MILLISECOND);               // returns 0 (cannot split a ms into ms)
     * Dates.getFragmentInMilliseconds((java.util.Date) null, CalendarField.SECOND);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code date} to calculate.
     * @return the number of milliseconds within the fragment of {@code date}.
     * @throws IllegalArgumentException if {@code date} is {@code null} or the fragment is not supported.
     */
    public static long getFragmentInMilliseconds(final java.util.Date date, final CalendarField fragment) throws IllegalArgumentException {
        return getFragment(date, N.checkArgNotNull(fragment, cs.fragment).value(), TimeUnit.MILLISECONDS);
    }

    /**
     * Adapted from Apache Commons Lang under Apache License v2; the {@code Date} overloads evaluate on a
     * proleptic Gregorian calendar whatever calendar system the default locale selects.
     * <br />
     *
     * <p>Returns the number of seconds within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the seconds of any date will only return the number of seconds
     * of the current minute (resulting in a number between 0 and 59). This
     * method will retrieve the number of seconds for any fragment.
     * For example, if you want to calculate the number of seconds past today,
     * your fragment is {@link CalendarField#DAY_OF_MONTH}. The result will
     * be all seconds of the past hour(s) and minute(s).</p>
     *
     * <p>Accepted {@link CalendarField} fragments: {@code YEAR}, {@code MONTH}, {@code DAY_OF_MONTH},
     * {@code HOUR_OF_DAY}, {@code MINUTE}, {@code SECOND}, and {@code MILLISECOND}.
     * {@code WEEK_OF_YEAR} is not a fragment field and throws {@code IllegalArgumentException}.
     * A fragment equal to or finer than the requested unit returns 0.</p>
     *
     * <p>The result sums <i>civil field values</i>, not elapsed time: a day always counts as 24 hours,
     * so across a daylight-saving transition the figure differs from the elapsed duration by the size
     * of the shift.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with CalendarField.MINUTE as fragment will return 10
     *   (equivalent to deprecated date.getSeconds())</li>
     *  <li>January 6, 2008 7:15:10.538 with CalendarField.MINUTE as fragment will return 10
     *   (equivalent to deprecated date.getSeconds())</li>
     *  <li>January 6, 2008 7:15:10.538 with CalendarField.DAY_OF_MONTH as fragment will return 26110
     *   (7*3600 + 15*60 + 10)</li>
     *  <li>January 16, 2008 7:15:10.538 with CalendarField.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in seconds)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2023-01-06 07:15:10.538", "yyyy-MM-dd HH:mm:ss.SSS");
     * Dates.getFragmentInSeconds(date, CalendarField.MINUTE);                    // returns 10 (seconds within the current minute)
     * Dates.getFragmentInSeconds(date, CalendarField.HOUR_OF_DAY);               // returns 910 (15*60 + 10)
     *
     * Dates.getFragmentInSeconds(date, CalendarField.SECOND);                    // returns 0 (fragment <= SECOND yields 0)
     * Dates.getFragmentInSeconds((java.util.Date) null, CalendarField.MINUTE);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code date} to calculate.
     * @return the number of seconds within the fragment of {@code date}.
     * @throws IllegalArgumentException if {@code date} is {@code null} or the fragment is not supported.
     */
    public static long getFragmentInSeconds(final java.util.Date date, final CalendarField fragment) throws IllegalArgumentException {
        return getFragment(date, N.checkArgNotNull(fragment, cs.fragment).value(), TimeUnit.SECONDS);
    }

    /**
     * Adapted from Apache Commons Lang under Apache License v2; the {@code Date} overloads evaluate on a
     * proleptic Gregorian calendar whatever calendar system the default locale selects.
     * <br />
     *
     * <p>Returns the number of minutes within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the minutes of any date will only return the number of minutes
     * of the current hour (resulting in a number between 0 and 59). This
     * method will retrieve the number of minutes for any fragment.
     * For example, if you want to calculate the number of minutes past this month,
     * your fragment is {@link CalendarField#MONTH}. The result will be all minutes of the
     * past day(s) and hour(s).</p>
     *
     * <p>Accepted {@link CalendarField} fragments: {@code YEAR}, {@code MONTH}, {@code DAY_OF_MONTH},
     * {@code HOUR_OF_DAY}, {@code MINUTE}, {@code SECOND}, and {@code MILLISECOND}.
     * {@code WEEK_OF_YEAR} is not a fragment field and throws {@code IllegalArgumentException}.
     * A fragment equal to or finer than the requested unit returns 0.</p>
     *
     * <p>The result sums <i>civil field values</i>, not elapsed time: a day always counts as 24 hours,
     * so across a daylight-saving transition the figure differs from the elapsed duration by the size
     * of the shift.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with CalendarField.HOUR_OF_DAY as fragment will return 15
     *   (equivalent to deprecated date.getMinutes())</li>
     *  <li>January 6, 2008 7:15:10.538 with CalendarField.HOUR_OF_DAY as fragment will return 15
     *   (equivalent to deprecated date.getMinutes())</li>
     *  <li>January 1, 2008 7:15:10.538 with CalendarField.MONTH as fragment will return 435 (7*60 + 15)</li>
     *  <li>January 6, 2008 7:15:10.538 with CalendarField.MONTH as fragment will return 7635 (5*24*60 + 7*60 + 15)</li>
     *  <li>January 16, 2008 7:15:10.538 with CalendarField.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in minutes)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2023-01-06 07:15:10.538", "yyyy-MM-dd HH:mm:ss.SSS");
     * Dates.getFragmentInMinutes(date, CalendarField.HOUR_OF_DAY);                    // returns 15 (minutes within the current hour)
     * Dates.getFragmentInMinutes(date, CalendarField.DAY_OF_MONTH);                   // returns 435 (7*60 + 15)
     *
     * Dates.getFragmentInMinutes(date, CalendarField.MINUTE);                         // returns 0 (fragment <= MINUTE yields 0)
     * Dates.getFragmentInMinutes((java.util.Date) null, CalendarField.HOUR_OF_DAY);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code date} to calculate.
     * @return the number of minutes within the fragment of {@code date}.
     * @throws IllegalArgumentException if {@code date} is {@code null} or the fragment is not supported.
     */
    public static long getFragmentInMinutes(final java.util.Date date, final CalendarField fragment) throws IllegalArgumentException {
        return getFragment(date, N.checkArgNotNull(fragment, cs.fragment).value(), TimeUnit.MINUTES);
    }

    /**
     * Adapted from Apache Commons Lang under Apache License v2; the {@code Date} overloads evaluate on a
     * proleptic Gregorian calendar whatever calendar system the default locale selects.
     * <br />
     *
     * <p>Returns the number of hours within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the hours of any date will only return the number of hours
     * of the current day (resulting in a number between 0 and 23). This
     * method will retrieve the number of hours for any fragment.
     * For example, if you want to calculate the number of hours past this month,
     * your fragment is {@link CalendarField#MONTH}. The result will be all hours of the
     * past day(s).</p>
     *
     * <p>Accepted {@link CalendarField} fragments: {@code YEAR}, {@code MONTH}, {@code DAY_OF_MONTH},
     * {@code HOUR_OF_DAY}, {@code MINUTE}, {@code SECOND}, and {@code MILLISECOND}.
     * {@code WEEK_OF_YEAR} is not a fragment field and throws {@code IllegalArgumentException}.
     * A fragment equal to or finer than the requested unit returns 0.</p>
     *
     * <p>The result sums <i>civil field values</i>, not elapsed time: a day always counts as 24 hours,
     * so across a daylight-saving transition the figure differs from the elapsed duration by the size
     * of the shift.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with CalendarField.DAY_OF_MONTH as fragment will return 7
     *   (equivalent to deprecated date.getHours())</li>
     *  <li>January 6, 2008 7:15:10.538 with CalendarField.DAY_OF_MONTH as fragment will return 7
     *   (equivalent to deprecated date.getHours())</li>
     *  <li>January 1, 2008 7:15:10.538 with CalendarField.MONTH as fragment will return 7</li>
     *  <li>January 6, 2008 7:15:10.538 with CalendarField.MONTH as fragment will return 127 (5*24 + 7)</li>
     *  <li>January 16, 2008 7:15:10.538 with CalendarField.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in hours)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2023-01-06 07:15:10.538", "yyyy-MM-dd HH:mm:ss.SSS");
     * Dates.getFragmentInHours(date, CalendarField.DAY_OF_MONTH);                    // returns 7 (hours within the current day)
     * Dates.getFragmentInHours(date, CalendarField.MONTH);                           // returns 127 (5 full days * 24 + 7)
     *
     * Dates.getFragmentInHours(date, CalendarField.HOUR_OF_DAY);                     // returns 0 (fragment <= HOUR yields 0)
     * Dates.getFragmentInHours((java.util.Date) null, CalendarField.DAY_OF_MONTH);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code date} to calculate.
     * @return the number of hours within the fragment of {@code date}.
     * @throws IllegalArgumentException if {@code date} is {@code null} or the fragment is not supported.
     */
    public static long getFragmentInHours(final java.util.Date date, final CalendarField fragment) throws IllegalArgumentException {
        return getFragment(date, N.checkArgNotNull(fragment, cs.fragment).value(), TimeUnit.HOURS);
    }

    /**
     * Adapted from Apache Commons Lang under Apache License v2; the {@code Date} overloads evaluate on a
     * proleptic Gregorian calendar whatever calendar system the default locale selects.
     * <br />
     *
     * <p>Returns the number of days within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
     *
     * <p>With {@link CalendarField#MONTH}, the result is the one-based day of month;
     * with {@link CalendarField#YEAR}, it is the one-based day of year. A
     * {@link CalendarField#DAY_OF_MONTH} or finer fragment returns 0 because the
     * fragment is equal to or finer than the requested unit.</p>
     *
     * <p>Accepted {@link CalendarField} fragments: {@code YEAR}, {@code MONTH}, {@code DAY_OF_MONTH},
     * {@code HOUR_OF_DAY}, {@code MINUTE}, {@code SECOND}, and {@code MILLISECOND}.
     * {@code WEEK_OF_YEAR} is not a fragment field and throws {@code IllegalArgumentException}.
     * A fragment equal to or finer than the requested unit returns 0.</p>
     *
     * <p>The result sums <i>civil field values</i>, not elapsed time: a day always counts as 24 hours,
     * so across a daylight-saving transition the figure differs from the elapsed duration by the size
     * of the shift.</p>
     *
     * <ul>
     *  <li>January 28, 2008 with CalendarField.MONTH as fragment will return 28
     *   (equivalent to deprecated date.getDate())</li>
     *  <li>February 28, 2008 with CalendarField.MONTH as fragment will return 28
     *   (equivalent to deprecated date.getDate())</li>
     *  <li>January 28, 2008 with CalendarField.YEAR as fragment will return 28</li>
     *  <li>February 28, 2008 with CalendarField.YEAR as fragment will return 59</li>
     *  <li>January 28, 2008 with CalendarField.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in days)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseToJUDate("2023-02-28 12:00:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.getFragmentInDays(date, CalendarField.MONTH);                   // returns 28 (day-of-month)
     * Dates.getFragmentInDays(date, CalendarField.YEAR);                    // returns 59 (31 in Jan + 28 = day-of-year)
     *
     * Dates.getFragmentInDays(date, CalendarField.DAY_OF_MONTH);            // returns 0 (fragment <= DAY yields 0)
     * Dates.getFragmentInDays((java.util.Date) null, CalendarField.YEAR);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code date} to calculate.
     * @return the number of days within the fragment of {@code date}.
     * @throws IllegalArgumentException if {@code date} is {@code null} or the fragment is not supported.
     */
    public static long getFragmentInDays(final java.util.Date date, final CalendarField fragment) throws IllegalArgumentException {
        return getFragment(date, N.checkArgNotNull(fragment, cs.fragment).value(), TimeUnit.DAYS);
    }

    /**
     * Adapted from Apache Commons Lang under Apache License v2; the fields are read from a proleptic
     * Gregorian calendar whatever calendar system the default locale selects.
     * <br />
     *
     * Gets a Date fragment for any unit.
     *
     * @param date the date to work with, not {@code null}.
     * @param fragment the {@code Calendar} field part of {@code date} to calculate.
     * @param unit the time unit.
     * @return the number of units within the fragment of {@code date}.
     * @throws IllegalArgumentException if {@code date} is {@code null} or the specified fragment is not supported.
     */
    private static long getFragment(final java.util.Date date, final int fragment, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        // The fragment fields must be proleptic Gregorian, as this method's contract promises: a
        // Buddhist or Japanese-imperial calendar from the default locale restarts DAY_OF_YEAR at an era
        // change, and the legacy 1582 cutover reported a pre-cutover DAY_OF_YEAR that disagreed with the
        // civil date format() prints - by an era-dependent amount that is NOT bounded by ten (ten days just
        // before the cutover, nine for 1500-06-15, eight for 1400-06-15, five for 1000-06-15, zero near
        // AD 300, then a two-day excess by AD 100 and seventeen by 2000 BC).
        // legacyRenderingZone adds the zone axis, where java.util.TimeZone
        // has no history before 1900. The Calendar overloads deliberately keep the caller's own calendar.
        final Calendar calendar = newProlepticGregorianCalendar(legacyRenderingZone(TimeZone.getDefault(), date.getTime()));
        calendar.setTime(date);
        return getFragment(calendar, fragment, unit);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of milliseconds within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the milliseconds of any date will only return the number of milliseconds
     * of the current second (resulting in a number between 0 and 999). This
     * method will retrieve the number of milliseconds for any fragment.
     * For example, if you want to calculate the number of milliseconds past today,
     * your fragment is {@link CalendarField#DAY_OF_MONTH}. The result will
     * be all milliseconds of the past hour(s), minute(s) and second(s).</p>
     *
     * <p>Accepted {@link CalendarField} fragments: {@code YEAR}, {@code MONTH}, {@code DAY_OF_MONTH},
     * {@code HOUR_OF_DAY}, {@code MINUTE}, {@code SECOND}, and {@code MILLISECOND}.
     * {@code WEEK_OF_YEAR} is not a fragment field and throws {@code IllegalArgumentException}.
     * A fragment equal to or finer than the requested unit returns 0.</p>
     *
     * <p>The result sums <i>civil field values</i>, not elapsed time: a day always counts as 24 hours,
     * so across a daylight-saving transition the figure differs from the elapsed duration by the size
     * of the shift.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with CalendarField.SECOND as fragment will return 538
     *   (equivalent to calendar.get(Calendar.MILLISECOND))</li>
     *  <li>January 6, 2008 7:15:10.538 with CalendarField.SECOND as fragment will return 538
     *   (equivalent to calendar.get(Calendar.MILLISECOND))</li>
     *  <li>January 6, 2008 7:15:10.538 with CalendarField.MINUTE as fragment will return 10538
     *   (10*1000 + 538)</li>
     *  <li>January 16, 2008 7:15:10.538 with CalendarField.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in milliseconds)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseToCalendar("2023-01-06 07:15:10.538", "yyyy-MM-dd HH:mm:ss.SSS", utc);
     * Dates.getFragmentInMilliseconds(cal, CalendarField.SECOND);               // returns 538 (millis within the current second)
     * Dates.getFragmentInMilliseconds(cal, CalendarField.MINUTE);               // returns 10538 (10*1000 + 538)
     *
     * Dates.getFragmentInMilliseconds(cal, CalendarField.MILLISECOND);          // returns 0 (cannot split a ms into ms)
     * Dates.getFragmentInMilliseconds((Calendar) null, CalendarField.SECOND);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p>The fragment fields are read from {@code calendar} itself, so they follow <i>its</i> calendar
     * system <i>and its Julian/Gregorian cutover</i>: a {@code JapaneseImperialCalendar} restarts
     * {@code DAY_OF_YEAR} at an era change, and a {@link GregorianCalendar} left on the JDK default
     * cutover reports a pre-1582 {@code DAY_OF_YEAR} that does not match the date
     * {@link #format(Calendar)} prints for the same instant &mdash; ten days below it just before the 1582
     * cutover, shrinking by roughly a day per century for earlier dates, crossing zero around AD 300 and
     * then growing again as an excess that is not bounded by ten (eleven days by 1200 BC, seventeen by
     * 2000 BC). The {@code java.util.Date} overloads are always proleptic Gregorian,
     * having no calendar of their own to follow; pass the value as a {@code java.util.Date}, or use a
     * calendar from {@link #parseToCalendar(String)}, for the proleptic figure.</p>
     *
     * @param calendar the calendar to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code calendar} to calculate.
     * @return the number of milliseconds within the fragment of {@code calendar}.
     * @throws IllegalArgumentException if {@code calendar} is {@code null} or the fragment is not supported.
     */
    public static long getFragmentInMilliseconds(final Calendar calendar, final CalendarField fragment) throws IllegalArgumentException {
        return getFragment(calendar, N.checkArgNotNull(fragment, cs.fragment).value(), TimeUnit.MILLISECONDS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of seconds within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the seconds of any date will only return the number of seconds
     * of the current minute (resulting in a number between 0 and 59). This
     * method will retrieve the number of seconds for any fragment.
     * For example, if you want to calculate the number of seconds past today,
     * your fragment is {@link CalendarField#DAY_OF_MONTH}. The result will
     * be all seconds of the past hour(s) and minute(s).</p>
     *
     * <p>Accepted {@link CalendarField} fragments: {@code YEAR}, {@code MONTH}, {@code DAY_OF_MONTH},
     * {@code HOUR_OF_DAY}, {@code MINUTE}, {@code SECOND}, and {@code MILLISECOND}.
     * {@code WEEK_OF_YEAR} is not a fragment field and throws {@code IllegalArgumentException}.
     * A fragment equal to or finer than the requested unit returns 0.</p>
     *
     * <p>The result sums <i>civil field values</i>, not elapsed time: a day always counts as 24 hours,
     * so across a daylight-saving transition the figure differs from the elapsed duration by the size
     * of the shift.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with CalendarField.MINUTE as fragment will return 10
     *   (equivalent to calendar.get(Calendar.SECOND))</li>
     *  <li>January 6, 2008 7:15:10.538 with CalendarField.MINUTE as fragment will return 10
     *   (equivalent to calendar.get(Calendar.SECOND))</li>
     *  <li>January 6, 2008 7:15:10.538 with CalendarField.DAY_OF_MONTH as fragment will return 26110
     *   (7*3600 + 15*60 + 10)</li>
     *  <li>January 16, 2008 7:15:10.538 with CalendarField.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in seconds)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseToCalendar("2023-01-06 07:15:10.538", "yyyy-MM-dd HH:mm:ss.SSS", utc);
     * Dates.getFragmentInSeconds(cal, CalendarField.MINUTE);               // returns 10 (seconds within the current minute)
     * Dates.getFragmentInSeconds(cal, CalendarField.DAY_OF_MONTH);         // returns 26110 (7*3600 + 15*60 + 10)
     *
     * Dates.getFragmentInSeconds(cal, CalendarField.SECOND);               // returns 0 (fragment <= SECOND yields 0)
     * Dates.getFragmentInSeconds((Calendar) null, CalendarField.MINUTE);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p>The fragment fields are read from {@code calendar} itself, so they follow <i>its</i> calendar
     * system <i>and its Julian/Gregorian cutover</i>: a {@code JapaneseImperialCalendar} restarts
     * {@code DAY_OF_YEAR} at an era change, and a {@link GregorianCalendar} left on the JDK default
     * cutover reports a pre-1582 {@code DAY_OF_YEAR} that does not match the date
     * {@link #format(Calendar)} prints for the same instant &mdash; ten days below it just before the 1582
     * cutover, shrinking by roughly a day per century for earlier dates, crossing zero around AD 300 and
     * then growing again as an excess that is not bounded by ten (eleven days by 1200 BC, seventeen by
     * 2000 BC). The {@code java.util.Date} overloads are always proleptic Gregorian,
     * having no calendar of their own to follow; pass the value as a {@code java.util.Date}, or use a
     * calendar from {@link #parseToCalendar(String)}, for the proleptic figure.</p>
     *
     * @param calendar the calendar to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code calendar} to calculate.
     * @return the number of seconds within the fragment of {@code calendar}.
     * @throws IllegalArgumentException if {@code calendar} is {@code null} or the fragment is not supported.
     */
    public static long getFragmentInSeconds(final Calendar calendar, final CalendarField fragment) throws IllegalArgumentException {
        return getFragment(calendar, N.checkArgNotNull(fragment, cs.fragment).value(), TimeUnit.SECONDS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of minutes within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the minutes of any date will only return the number of minutes
     * of the current hour (resulting in a number between 0 and 59). This
     * method will retrieve the number of minutes for any fragment.
     * For example, if you want to calculate the number of minutes past this month,
     * your fragment is {@link CalendarField#MONTH}. The result will be all minutes of the
     * past day(s) and hour(s).</p>
     *
     * <p>Accepted {@link CalendarField} fragments: {@code YEAR}, {@code MONTH}, {@code DAY_OF_MONTH},
     * {@code HOUR_OF_DAY}, {@code MINUTE}, {@code SECOND}, and {@code MILLISECOND}.
     * {@code WEEK_OF_YEAR} is not a fragment field and throws {@code IllegalArgumentException}.
     * A fragment equal to or finer than the requested unit returns 0.</p>
     *
     * <p>The result sums <i>civil field values</i>, not elapsed time: a day always counts as 24 hours,
     * so across a daylight-saving transition the figure differs from the elapsed duration by the size
     * of the shift.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with CalendarField.HOUR_OF_DAY as fragment will return 15
     *   (equivalent to calendar.get(Calendar.MINUTE))</li>
     *  <li>January 6, 2008 7:15:10.538 with CalendarField.HOUR_OF_DAY as fragment will return 15
     *   (equivalent to calendar.get(Calendar.MINUTE))</li>
     *  <li>January 1, 2008 7:15:10.538 with CalendarField.MONTH as fragment will return 435 (7*60 + 15)</li>
     *  <li>January 6, 2008 7:15:10.538 with CalendarField.MONTH as fragment will return 7635 (5*24*60 + 7*60 + 15)</li>
     *  <li>January 16, 2008 7:15:10.538 with CalendarField.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in minutes)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseToCalendar("2023-01-06 07:15:10.538", "yyyy-MM-dd HH:mm:ss.SSS", utc);
     * Dates.getFragmentInMinutes(cal, CalendarField.HOUR_OF_DAY);               // returns 15 (minutes within the current hour)
     * Dates.getFragmentInMinutes(cal, CalendarField.MONTH);                     // returns 7635 (5*1440 + 7*60 + 15)
     *
     * Dates.getFragmentInMinutes(cal, CalendarField.MINUTE);                    // returns 0 (fragment <= MINUTE yields 0)
     * Dates.getFragmentInMinutes((Calendar) null, CalendarField.HOUR_OF_DAY);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p>The fragment fields are read from {@code calendar} itself, so they follow <i>its</i> calendar
     * system <i>and its Julian/Gregorian cutover</i>: a {@code JapaneseImperialCalendar} restarts
     * {@code DAY_OF_YEAR} at an era change, and a {@link GregorianCalendar} left on the JDK default
     * cutover reports a pre-1582 {@code DAY_OF_YEAR} that does not match the date
     * {@link #format(Calendar)} prints for the same instant &mdash; ten days below it just before the 1582
     * cutover, shrinking by roughly a day per century for earlier dates, crossing zero around AD 300 and
     * then growing again as an excess that is not bounded by ten (eleven days by 1200 BC, seventeen by
     * 2000 BC). The {@code java.util.Date} overloads are always proleptic Gregorian,
     * having no calendar of their own to follow; pass the value as a {@code java.util.Date}, or use a
     * calendar from {@link #parseToCalendar(String)}, for the proleptic figure.</p>
     *
     * @param calendar the calendar to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code calendar} to calculate.
     * @return the number of minutes within the fragment of {@code calendar}.
     * @throws IllegalArgumentException if {@code calendar} is {@code null} or the fragment is not supported.
     */
    public static long getFragmentInMinutes(final Calendar calendar, final CalendarField fragment) throws IllegalArgumentException {
        return getFragment(calendar, N.checkArgNotNull(fragment, cs.fragment).value(), TimeUnit.MINUTES);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of hours within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the hours of any date will only return the number of hours
     * of the current day (resulting in a number between 0 and 23). This
     * method will retrieve the number of hours for any fragment.
     * For example, if you want to calculate the number of hours past this month,
     * your fragment is {@link CalendarField#MONTH}. The result will be all hours of the
     * past day(s).</p>
     *
     * <p>Accepted {@link CalendarField} fragments: {@code YEAR}, {@code MONTH}, {@code DAY_OF_MONTH},
     * {@code HOUR_OF_DAY}, {@code MINUTE}, {@code SECOND}, and {@code MILLISECOND}.
     * {@code WEEK_OF_YEAR} is not a fragment field and throws {@code IllegalArgumentException}.
     * A fragment equal to or finer than the requested unit returns 0.</p>
     *
     * <p>The result sums <i>civil field values</i>, not elapsed time: a day always counts as 24 hours,
     * so across a daylight-saving transition the figure differs from the elapsed duration by the size
     * of the shift.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with CalendarField.DAY_OF_MONTH as fragment will return 7
     *   (equivalent to calendar.get(Calendar.HOUR_OF_DAY))</li>
     *  <li>January 6, 2008 7:15:10.538 with CalendarField.DAY_OF_MONTH as fragment will return 7
     *   (equivalent to calendar.get(Calendar.HOUR_OF_DAY))</li>
     *  <li>January 1, 2008 7:15:10.538 with CalendarField.MONTH as fragment will return 7</li>
     *  <li>January 6, 2008 7:15:10.538 with CalendarField.MONTH as fragment will return 127 (5*24 + 7)</li>
     *  <li>January 16, 2008 7:15:10.538 with CalendarField.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in hours)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseToCalendar("2023-01-06 07:15:10.538", "yyyy-MM-dd HH:mm:ss.SSS", utc);
     * Dates.getFragmentInHours(cal, CalendarField.DAY_OF_MONTH);               // returns 7 (hours within the current day)
     * Dates.getFragmentInHours(cal, CalendarField.MONTH);                      // returns 127 (5 full days * 24 + 7)
     *
     * Dates.getFragmentInHours(cal, CalendarField.HOUR_OF_DAY);                // returns 0 (fragment <= HOUR yields 0)
     * Dates.getFragmentInHours((Calendar) null, CalendarField.DAY_OF_MONTH);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p>The fragment fields are read from {@code calendar} itself, so they follow <i>its</i> calendar
     * system <i>and its Julian/Gregorian cutover</i>: a {@code JapaneseImperialCalendar} restarts
     * {@code DAY_OF_YEAR} at an era change, and a {@link GregorianCalendar} left on the JDK default
     * cutover reports a pre-1582 {@code DAY_OF_YEAR} that does not match the date
     * {@link #format(Calendar)} prints for the same instant &mdash; ten days below it just before the 1582
     * cutover, shrinking by roughly a day per century for earlier dates, crossing zero around AD 300 and
     * then growing again as an excess that is not bounded by ten (eleven days by 1200 BC, seventeen by
     * 2000 BC). The {@code java.util.Date} overloads are always proleptic Gregorian,
     * having no calendar of their own to follow; pass the value as a {@code java.util.Date}, or use a
     * calendar from {@link #parseToCalendar(String)}, for the proleptic figure.</p>
     *
     * @param calendar the calendar to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code calendar} to calculate.
     * @return the number of hours within the fragment of {@code calendar}.
     * @throws IllegalArgumentException if {@code calendar} is {@code null} or the fragment is not supported.
     */
    public static long getFragmentInHours(final Calendar calendar, final CalendarField fragment) throws IllegalArgumentException {
        return getFragment(calendar, N.checkArgNotNull(fragment, cs.fragment).value(), TimeUnit.HOURS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of days within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
     *
     * <p>With {@link CalendarField#MONTH}, the result is the one-based day of month;
     * with {@link CalendarField#YEAR}, it is the one-based day of year. A
     * {@link CalendarField#DAY_OF_MONTH} or finer fragment returns 0 because the
     * fragment is equal to or finer than the requested unit.</p>
     *
     * <p>Accepted {@link CalendarField} fragments: {@code YEAR}, {@code MONTH}, {@code DAY_OF_MONTH},
     * {@code HOUR_OF_DAY}, {@code MINUTE}, {@code SECOND}, and {@code MILLISECOND}.
     * {@code WEEK_OF_YEAR} is not a fragment field and throws {@code IllegalArgumentException}.
     * A fragment equal to or finer than the requested unit returns 0.</p>
     *
     * <p>The result sums <i>civil field values</i>, not elapsed time: a day always counts as 24 hours,
     * so across a daylight-saving transition the figure differs from the elapsed duration by the size
     * of the shift.</p>
     *
     * <ul>
     *  <li>January 28, 2008 with CalendarField.MONTH as fragment will return 28
     *   (equivalent to calendar.get(Calendar.DAY_OF_MONTH))</li>
     *  <li>February 28, 2008 with CalendarField.MONTH as fragment will return 28
     *   (equivalent to calendar.get(Calendar.DAY_OF_MONTH))</li>
     *  <li>January 28, 2008 with CalendarField.YEAR as fragment will return 28
     *   (equivalent to calendar.get(Calendar.DAY_OF_YEAR))</li>
     *  <li>February 28, 2008 with CalendarField.YEAR as fragment will return 59
     *   (equivalent to calendar.get(Calendar.DAY_OF_YEAR))</li>
     *  <li>January 28, 2008 with CalendarField.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in days)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseToCalendar("2023-02-28 12:00:00", "yyyy-MM-dd HH:mm:ss", utc);
     * Dates.getFragmentInDays(cal, CalendarField.MONTH);              // returns 28 (day-of-month)
     * Dates.getFragmentInDays(cal, CalendarField.YEAR);               // returns 59 (31 in Jan + 28 = day-of-year)
     *
     * Dates.getFragmentInDays(cal, CalendarField.DAY_OF_MONTH);       // returns 0 (fragment <= DAY yields 0)
     * Dates.getFragmentInDays((Calendar) null, CalendarField.YEAR);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p>The fragment fields are read from {@code calendar} itself, so they follow <i>its</i> calendar
     * system <i>and its Julian/Gregorian cutover</i>: a {@code JapaneseImperialCalendar} restarts
     * {@code DAY_OF_YEAR} at an era change, and a {@link GregorianCalendar} left on the JDK default
     * cutover reports a pre-1582 {@code DAY_OF_YEAR} that does not match the date
     * {@link #format(Calendar)} prints for the same instant &mdash; ten days below it just before the 1582
     * cutover, shrinking by roughly a day per century for earlier dates, crossing zero around AD 300 and
     * then growing again as an excess that is not bounded by ten (eleven days by 1200 BC, seventeen by
     * 2000 BC). The {@code java.util.Date} overloads are always proleptic Gregorian,
     * having no calendar of their own to follow; pass the value as a {@code java.util.Date}, or use a
     * calendar from {@link #parseToCalendar(String)}, for the proleptic figure.</p>
     *
     * @param calendar the calendar to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code calendar} to calculate.
     * @return the number of days within the fragment of {@code calendar}.
     * @throws IllegalArgumentException if {@code calendar} is {@code null} or the fragment is not supported.
     */
    public static long getFragmentInDays(final Calendar calendar, final CalendarField fragment) throws IllegalArgumentException {
        return getFragment(calendar, N.checkArgNotNull(fragment, cs.fragment).value(), TimeUnit.DAYS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Gets a Calendar fragment for any unit.
     *
     * @param calendar the calendar to work with, not {@code null}.
     * @param fragment the {@code Calendar} field part of {@code calendar} to calculate.
     * @param unit the time unit.
     * @return the number of units within the fragment of {@code calendar}.
     * @throws IllegalArgumentException if {@code calendar} is {@code null} or the specified fragment is not
     *         supported.
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    private static long getFragment(final Calendar calendar, final int fragment, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        long result = 0;

        final int offset = (unit == TimeUnit.DAYS) ? 0 : 1;

        // Fragments bigger than a day require a breakdown to days
        switch (fragment) {
            case Calendar.YEAR:
                result += unit.convert(calendar.get(Calendar.DAY_OF_YEAR) - offset, TimeUnit.DAYS); //NOSONAR
                break;
            case Calendar.MONTH:
                result += unit.convert(calendar.get(Calendar.DAY_OF_MONTH) - offset, TimeUnit.DAYS); //NOSONAR
                break;
            default:
                break;
        }

        switch (fragment) {
            // Number of days already calculated for these cases
            case Calendar.YEAR:
            case Calendar.MONTH:

                // The rest of the valid cases. No DAY_OF_YEAR case: CalendarField has no such constant,
                // so the public fragment methods can never reach it.
            case Calendar.DATE:
                result += unit.convert(calendar.get(Calendar.HOUR_OF_DAY), TimeUnit.HOURS);
                //$FALL-THROUGH$
            case Calendar.HOUR_OF_DAY:
                result += unit.convert(calendar.get(Calendar.MINUTE), TimeUnit.MINUTES);
                //$FALL-THROUGH$
            case Calendar.MINUTE:
                result += unit.convert(calendar.get(Calendar.SECOND), TimeUnit.SECONDS);
                //$FALL-THROUGH$
            case Calendar.SECOND:
                result += unit.convert(calendar.get(Calendar.MILLISECOND), TimeUnit.MILLISECONDS);
                break;
            case Calendar.MILLISECOND:
                break; //never useful
            default:
                throw new IllegalArgumentException("The fragment " + fieldName(fragment) + " is not supported");
        }
        return result;
    }

    //-----------------------------------------------------------------------

    /**
     * Checks if two date objects are on the same civil day in the live default time zone, ignoring time-of-day.
     * Use {@link #isSameDay(java.util.Date, java.util.Date, ZoneId)} when the zone must not follow
     * {@link TimeZone#getDefault()}.
     *
     * <p>28 Mar 2002 13:45 and 28 Mar 2002 06:01 would return {@code true}.
     * 28 Mar 2002 13:45 and 12 Mar 2002 13:45 would return {@code false}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date d1 = Dates.parseToJUDate("2023-03-28 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * java.util.Date d2 = Dates.parseToJUDate("2023-03-28 06:01:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameDay(d1, d2);   // returns true (same day in the live default zone)
     *
     * java.util.Date d3 = Dates.parseToJUDate("2023-03-12 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameDay(d1, d3);                      // returns false (different day)
     * Dates.isSameDay((java.util.Date) null, d2);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param date1 the first date, not altered, not {@code null}.
     * @param date2 the second date, not altered, not {@code null}.
     * @return {@code true} if they represent the same civil day in the live default time zone.
     * @throws IllegalArgumentException if either date is {@code null}, or the ID of the live default
     *         time zone is not one {@link ZoneId} recognizes.
     * @see #isSameDay(java.util.Date, java.util.Date, ZoneId)
     */
    public static boolean isSameDay(final java.util.Date date1, final java.util.Date date2) throws IllegalArgumentException {
        // defaultZoneId() rather than the TimeZone overload: see isLastDayOfMonth for why the implicit
        // default zone must not be put through toZoneId's round-trip validation.
        return isSameDay(date1, date2, defaultZoneId());
    }

    /**
     * Checks if two date objects are on the same civil day in {@code timeZone}, ignoring time-of-day.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // 23:00 UTC on Jan 15 and 02:00 UTC on Jan 16 - one civil day apart in UTC,
     * // but both already Jan 16 in Asia/Kolkata (+05:30)
     * java.util.Date d1 = Dates.parseToJUDate("2025-01-15T23:00:00Z");
     * java.util.Date d2 = Dates.parseToJUDate("2025-01-16T02:00:00Z");
     * Dates.isSameDay(d1, d2, TimeZone.getTimeZone("Asia/Kolkata"));   // returns true (both are Jan 16 there)
     * Dates.isSameDay(d1, d2, TimeZone.getTimeZone("UTC"));            // returns false (Jan 15 vs Jan 16)
     *
     * Dates.isSameDay(d1, d1, TimeZone.getTimeZone("UTC"));            // returns true (same instant)
     * Dates.isSameDay(d1, d2, (TimeZone) null);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param date1 the first date, not altered, not {@code null}.
     * @param date2 the second date, not altered, not {@code null}.
     * @param timeZone the zone in which the civil day is taken, not {@code null}.
     * @return {@code true} if they represent the same civil day in {@code timeZone}.
     * @throws IllegalArgumentException if any argument is {@code null}, or {@code timeZone} cannot be
     *         represented as a {@link ZoneId}.
     * @see #isSameDay(java.util.Date, java.util.Date, ZoneId)
     */
    public static boolean isSameDay(final java.util.Date date1, final java.util.Date date2, final TimeZone timeZone) throws IllegalArgumentException {
        N.checkArgNotNull(timeZone, cs.timeZone);

        return isSameDay(date1, date2, toZoneId(timeZone));
    }

    /**
     * Checks if two date objects are on the same civil day in {@code zone}, ignoring time-of-day.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // 23:00 UTC on Jan 15 and 02:00 UTC on Jan 16 - one civil day apart in UTC,
     * // but both already Jan 16 in Asia/Kolkata (+05:30)
     * java.util.Date d1 = Dates.parseToJUDate("2025-01-15T23:00:00Z");
     * java.util.Date d2 = Dates.parseToJUDate("2025-01-16T02:00:00Z");
     * Dates.isSameDay(d1, d2, ZoneId.of("Asia/Kolkata"));   // returns true (both are Jan 16 there)
     * Dates.isSameDay(d1, d2, ZoneOffset.UTC);              // returns false (Jan 15 vs Jan 16)
     *
     * Dates.isSameDay(d1, d1, ZoneOffset.UTC);              // returns true (same instant)
     * Dates.isSameDay(d1, d2, (ZoneId) null);               // throws IllegalArgumentException
     * }</pre>
     *
     * @param date1 the first date, not altered, not {@code null}.
     * @param date2 the second date, not altered, not {@code null}.
     * @param zone the zone in which the civil day is taken, not {@code null}.
     * @return {@code true} if they represent the same civil day in {@code zone}.
     * @throws IllegalArgumentException if any argument is {@code null}.
     */
    public static boolean isSameDay(final java.util.Date date1, final java.util.Date date2, final ZoneId zone) throws IllegalArgumentException {
        N.checkArgNotNull(date1, cs.date1);
        N.checkArgNotNull(date2, cs.date2);
        N.checkArgNotNull(zone, cs.zone);

        return localDateAt(date1.getTime(), zone).equals(localDateAt(date2.getTime(), zone));
    }

    /**
     * Checks if two calendar instants fall on the same ISO civil day in their shared time zone.
     * Both calendars must share equivalent time-zone rules; pass an explicit
     * {@link #isSameDay(Calendar, Calendar, ZoneId) zone} to compare two instants in one zone.
     * Calendar chronology is ignored: a {@code GregorianCalendar} and a Buddhist calendar at the
     * same instant compare equal here, matching {@link #isSameDay(Calendar, Calendar, ZoneId)}.
     *
     * <p>28 Mar 2002 13:45 and 28 Mar 2002 06:01 would return {@code true}.
     * 28 Mar 2002 13:45 and 12 Mar 2002 13:45 would return {@code false}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar c1 = Dates.parseToCalendar("2023-03-28 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Calendar c2 = Dates.parseToCalendar("2023-03-28 06:01:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameDay(c1, c2);   // returns true (same day, time ignored)
     *
     * Calendar c3 = Dates.parseToCalendar("2023-03-12 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameDay(c1, c3);                // returns false (different day)
     * Dates.isSameDay((Calendar) null, c2);   // throws IllegalArgumentException
     *
     * Calendar utc = Dates.createCalendar(c1.getTimeInMillis(), TimeZone.getTimeZone("UTC"));
     * Calendar tokyo = Dates.createCalendar(c1.getTimeInMillis(), TimeZone.getTimeZone("Asia/Tokyo"));
     * Dates.isSameDay(utc, tokyo);            // throws IllegalArgumentException (inequivalent zones)
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not {@code null}.
     * @param cal2 the second calendar, not altered, not {@code null}.
     * @return {@code true} if they represent the same ISO civil day in their shared time zone.
     * @throws IllegalArgumentException if either calendar is {@code null}, the calendars are in
     *         inequivalent time zones, or the shared time zone cannot be represented as a {@link ZoneId}. A
     *         calendar whose {@code getTimeZone()} answers {@code null} is read in the live default zone.
     * @see #isSameDay(Calendar, Calendar, ZoneId)
     * @see #isSameDay(java.util.Date, java.util.Date)
     */
    public static boolean isSameDay(final Calendar cal1, final Calendar cal2) throws IllegalArgumentException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);
        requireCompatibleTimeZones(cal1, cal2);

        return isSameDay(cal1, cal2, toZoneId(zoneOf(cal1)));
    }

    /**
     * Checks if two calendar instants fall on the same civil day in {@code timeZone}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // 23:00 UTC on Jan 15 and 02:00 UTC on Jan 16 - one civil day apart in UTC,
     * // but both already Jan 16 in Asia/Kolkata (+05:30)
     * Calendar c1 = Dates.parseToCalendar("2025-01-15T23:00:00Z");
     * Calendar c2 = Dates.parseToCalendar("2025-01-16T02:00:00Z");
     * Dates.isSameDay(c1, c2, TimeZone.getTimeZone("Asia/Kolkata"));   // returns true (both are Jan 16 there)
     * Dates.isSameDay(c1, c2, TimeZone.getTimeZone("UTC"));            // returns false (Jan 15 vs Jan 16)
     *
     * Dates.isSameDay(c1, c1, TimeZone.getTimeZone("UTC"));            // returns true (same instant)
     * Dates.isSameDay(c1, c2, (TimeZone) null);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not {@code null}.
     * @param cal2 the second calendar, not altered, not {@code null}.
     * @param timeZone the zone in which the civil day is taken, not {@code null}.
     * @return {@code true} if both instants are on the same civil day in {@code timeZone}.
     * @throws IllegalArgumentException if any argument is {@code null}, or {@code timeZone} cannot be
     *         represented as a {@link ZoneId}.
     */
    public static boolean isSameDay(final Calendar cal1, final Calendar cal2, final TimeZone timeZone) throws IllegalArgumentException {
        N.checkArgNotNull(timeZone, cs.timeZone);

        return isSameDay(cal1, cal2, toZoneId(timeZone));
    }

    /**
     * Checks if two calendar instants fall on the same civil day in {@code zone}. Each calendar's own
     * time zone is ignored for the comparison.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // 23:00 UTC on Jan 15 and 02:00 UTC on Jan 16 - one civil day apart in UTC,
     * // but both already Jan 16 in Asia/Kolkata (+05:30)
     * Calendar c1 = Dates.parseToCalendar("2025-01-15T23:00:00Z");
     * Calendar c2 = Dates.parseToCalendar("2025-01-16T02:00:00Z");
     * Dates.isSameDay(c1, c2, ZoneId.of("Asia/Kolkata"));   // returns true (both are Jan 16 there)
     * Dates.isSameDay(c1, c2, ZoneOffset.UTC);              // returns false (Jan 15 vs Jan 16)
     *
     * Dates.isSameDay(c1, c1, ZoneOffset.UTC);              // returns true (same instant)
     * Dates.isSameDay(c1, c2, (ZoneId) null);               // throws IllegalArgumentException
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not {@code null}.
     * @param cal2 the second calendar, not altered, not {@code null}.
     * @param zone the zone in which the civil day is taken, not {@code null}.
     * @return {@code true} if both instants are on the same civil day in {@code zone}.
     * @throws IllegalArgumentException if any argument is {@code null}.
     */
    public static boolean isSameDay(final Calendar cal1, final Calendar cal2, final ZoneId zone) throws IllegalArgumentException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);
        N.checkArgNotNull(zone, cs.zone);

        return localDateAt(cal1.getTimeInMillis(), zone).equals(localDateAt(cal2.getTimeInMillis(), zone));
    }

    /**
     * Checks if two date objects are in the same civil month and year in the live default time zone.
     * Use {@link #isSameMonth(java.util.Date, java.util.Date, ZoneId)} when the zone must not follow
     * {@link TimeZone#getDefault()}.
     *
     * <p>15 Mar 2023 13:45 and 28 Mar 2023 06:01 would return {@code true}.
     * 15 Mar 2023 13:45 and 15 Apr 2023 13:45 would return {@code false}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date d1 = Dates.parseToJUDate("2023-03-15 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * java.util.Date d2 = Dates.parseToJUDate("2023-03-28 06:01:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameMonth(d1, d2);   // returns true (same month and year in the live default zone)
     *
     * java.util.Date d3 = Dates.parseToJUDate("2023-04-15 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameMonth(d1, d3);                      // returns false (April vs March)
     * Dates.isSameMonth((java.util.Date) null, d2);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param date1 the first date, not altered, not {@code null}.
     * @param date2 the second date, not altered, not {@code null}.
     * @return {@code true} if they represent the same month of the same year in the live default time zone.
     * @throws IllegalArgumentException if either date is {@code null}, or the ID of the live default
     *         time zone is not one {@link ZoneId} recognizes.
     * @see #isSameMonth(java.util.Date, java.util.Date, ZoneId)
     */
    public static boolean isSameMonth(final java.util.Date date1, final java.util.Date date2) throws IllegalArgumentException {
        // defaultZoneId() rather than the TimeZone overload: see isLastDayOfMonth for why the implicit
        // default zone must not be put through toZoneId's round-trip validation.
        return isSameMonth(date1, date2, defaultZoneId());
    }

    /**
     * Checks if two date objects are in the same civil month and year in {@code timeZone}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // 23:00 UTC on Jan 31 and 02:00 UTC on Feb 1 - different months in UTC,
     * // but both already Feb 1 in Asia/Kolkata (+05:30)
     * java.util.Date d1 = Dates.parseToJUDate("2025-01-31T23:00:00Z");
     * java.util.Date d2 = Dates.parseToJUDate("2025-02-01T02:00:00Z");
     * Dates.isSameMonth(d1, d2, TimeZone.getTimeZone("Asia/Kolkata"));   // returns true (both are February there)
     * Dates.isSameMonth(d1, d2, TimeZone.getTimeZone("UTC"));            // returns false (January vs February)
     *
     * Dates.isSameMonth(d1, d1, TimeZone.getTimeZone("UTC"));            // returns true (same instant)
     * Dates.isSameMonth(d1, d2, (TimeZone) null);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param date1 the first date, not altered, not {@code null}.
     * @param date2 the second date, not altered, not {@code null}.
     * @param timeZone the zone in which the civil month is taken, not {@code null}.
     * @return {@code true} if they represent the same month of the same year in {@code timeZone}.
     * @throws IllegalArgumentException if any argument is {@code null}, or {@code timeZone} cannot be
     *         represented as a {@link ZoneId}.
     * @see #isSameMonth(java.util.Date, java.util.Date, ZoneId)
     */
    public static boolean isSameMonth(final java.util.Date date1, final java.util.Date date2, final TimeZone timeZone) throws IllegalArgumentException {
        N.checkArgNotNull(timeZone, cs.timeZone);

        return isSameMonth(date1, date2, toZoneId(timeZone));
    }

    /**
     * Checks if two date objects are in the same civil month and year in {@code zone}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // 23:00 UTC on Jan 31 and 02:00 UTC on Feb 1 - different months in UTC,
     * // but both already Feb 1 in Asia/Kolkata (+05:30)
     * java.util.Date d1 = Dates.parseToJUDate("2025-01-31T23:00:00Z");
     * java.util.Date d2 = Dates.parseToJUDate("2025-02-01T02:00:00Z");
     * Dates.isSameMonth(d1, d2, ZoneId.of("Asia/Kolkata"));   // returns true (both are February there)
     * Dates.isSameMonth(d1, d2, ZoneOffset.UTC);              // returns false (January vs February)
     *
     * Dates.isSameMonth(d1, d1, ZoneOffset.UTC);              // returns true (same instant)
     * Dates.isSameMonth(d1, d2, (ZoneId) null);               // throws IllegalArgumentException
     * }</pre>
     *
     * @param date1 the first date, not altered, not {@code null}.
     * @param date2 the second date, not altered, not {@code null}.
     * @param zone the zone in which the civil month is taken, not {@code null}.
     * @return {@code true} if they represent the same month of the same year in {@code zone}.
     * @throws IllegalArgumentException if any argument is {@code null}.
     */
    public static boolean isSameMonth(final java.util.Date date1, final java.util.Date date2, final ZoneId zone) throws IllegalArgumentException {
        N.checkArgNotNull(date1, cs.date1);
        N.checkArgNotNull(date2, cs.date2);
        N.checkArgNotNull(zone, cs.zone);

        return isSameYearMonth(localDateAt(date1.getTime(), zone), localDateAt(date2.getTime(), zone));
    }

    /**
     * Checks if two calendar instants fall in the same ISO civil month and year in their shared time zone.
     * Both calendars must share equivalent time-zone rules; pass an explicit
     * {@link #isSameMonth(Calendar, Calendar, ZoneId) zone} to compare two instants in one zone.
     * Calendar chronology is ignored: a {@code GregorianCalendar} and a Buddhist calendar at the
     * same instant compare equal here, matching {@link #isSameMonth(Calendar, Calendar, ZoneId)}.
     *
     * <p>15 Mar 2023 13:45 and 28 Mar 2023 06:01 would return {@code true}.
     * 15 Mar 2023 13:45 and 15 Apr 2023 13:45 would return {@code false}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar c1 = Dates.parseToCalendar("2023-03-15 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Calendar c2 = Dates.parseToCalendar("2023-03-28 06:01:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameMonth(c1, c2);   // returns true (same month and year)
     *
     * Calendar c3 = Dates.parseToCalendar("2023-04-15 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameMonth(c1, c3);                // returns false (April vs March)
     * Dates.isSameMonth((Calendar) null, c2);   // throws IllegalArgumentException
     *
     * Calendar utc = Dates.createCalendar(c1.getTimeInMillis(), TimeZone.getTimeZone("UTC"));
     * Calendar tokyo = Dates.createCalendar(c1.getTimeInMillis(), TimeZone.getTimeZone("Asia/Tokyo"));
     * Dates.isSameMonth(utc, tokyo);           // throws IllegalArgumentException (inequivalent zones)
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not {@code null}.
     * @param cal2 the second calendar, not altered, not {@code null}.
     * @return {@code true} if they represent the same ISO civil month of the same year in their shared time zone.
     * @throws IllegalArgumentException if either calendar is {@code null}, the calendars are in
     *         inequivalent time zones, or the shared time zone cannot be represented as a {@link ZoneId}. A
     *         calendar whose {@code getTimeZone()} answers {@code null} is read in the live default zone.
     * @see #isSameMonth(Calendar, Calendar, ZoneId)
     */
    public static boolean isSameMonth(final Calendar cal1, final Calendar cal2) throws IllegalArgumentException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);
        requireCompatibleTimeZones(cal1, cal2);

        return isSameMonth(cal1, cal2, toZoneId(zoneOf(cal1)));
    }

    /**
     * Checks if two calendar instants fall in the same civil month and year in {@code timeZone}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // 23:00 UTC on Jan 31 and 02:00 UTC on Feb 1 - different months in UTC,
     * // but both already Feb 1 in Asia/Kolkata (+05:30)
     * Calendar c1 = Dates.parseToCalendar("2025-01-31T23:00:00Z");
     * Calendar c2 = Dates.parseToCalendar("2025-02-01T02:00:00Z");
     * Dates.isSameMonth(c1, c2, TimeZone.getTimeZone("Asia/Kolkata"));   // returns true (both are February there)
     * Dates.isSameMonth(c1, c2, TimeZone.getTimeZone("UTC"));            // returns false (January vs February)
     *
     * Dates.isSameMonth(c1, c1, TimeZone.getTimeZone("UTC"));            // returns true (same instant)
     * Dates.isSameMonth(c1, c2, (TimeZone) null);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not {@code null}.
     * @param cal2 the second calendar, not altered, not {@code null}.
     * @param timeZone the zone in which the civil month is taken, not {@code null}.
     * @return {@code true} if both instants are in the same month of the same year in {@code timeZone}.
     * @throws IllegalArgumentException if any argument is {@code null}, or {@code timeZone} cannot be
     *         represented as a {@link ZoneId}.
     */
    public static boolean isSameMonth(final Calendar cal1, final Calendar cal2, final TimeZone timeZone) throws IllegalArgumentException {
        N.checkArgNotNull(timeZone, cs.timeZone);

        return isSameMonth(cal1, cal2, toZoneId(timeZone));
    }

    /**
     * Checks if two calendar instants fall in the same civil month and year in {@code zone}. Each
     * calendar's own time zone is ignored for the comparison.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // 23:00 UTC on Jan 31 and 02:00 UTC on Feb 1 - different months in UTC,
     * // but both already Feb 1 in Asia/Kolkata (+05:30)
     * Calendar c1 = Dates.parseToCalendar("2025-01-31T23:00:00Z");
     * Calendar c2 = Dates.parseToCalendar("2025-02-01T02:00:00Z");
     * Dates.isSameMonth(c1, c2, ZoneId.of("Asia/Kolkata"));   // returns true (both are February there)
     * Dates.isSameMonth(c1, c2, ZoneOffset.UTC);              // returns false (January vs February)
     *
     * Dates.isSameMonth(c1, c1, ZoneOffset.UTC);              // returns true (same instant)
     * Dates.isSameMonth(c1, c2, (ZoneId) null);               // throws IllegalArgumentException
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not {@code null}.
     * @param cal2 the second calendar, not altered, not {@code null}.
     * @param zone the zone in which the civil month is taken, not {@code null}.
     * @return {@code true} if both instants are in the same month of the same year in {@code zone}.
     * @throws IllegalArgumentException if any argument is {@code null}.
     */
    public static boolean isSameMonth(final Calendar cal1, final Calendar cal2, final ZoneId zone) throws IllegalArgumentException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);
        N.checkArgNotNull(zone, cs.zone);

        return isSameYearMonth(localDateAt(cal1.getTimeInMillis(), zone), localDateAt(cal2.getTimeInMillis(), zone));
    }

    /**
     * Checks if two date objects are in the same civil year in the live default time zone.
     * Use {@link #isSameYear(java.util.Date, java.util.Date, ZoneId)} when the zone must not follow
     * {@link TimeZone#getDefault()}.
     *
     * <p>15 Mar 2023 13:45 and 28 Nov 2023 06:01 would return {@code true}.
     * 15 Mar 2023 13:45 and 15 Mar 2024 13:45 would return {@code false}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date d1 = Dates.parseToJUDate("2023-03-15 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * java.util.Date d2 = Dates.parseToJUDate("2023-11-28 06:01:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameYear(d1, d2);   // returns true (same year in the live default zone)
     *
     * java.util.Date d3 = Dates.parseToJUDate("2024-03-15 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameYear(d1, d3);                      // returns false (2024 vs 2023)
     * Dates.isSameYear((java.util.Date) null, d2);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param date1 the first date, not altered, not {@code null}.
     * @param date2 the second date, not altered, not {@code null}.
     * @return {@code true} if they represent the same civil year in the live default time zone.
     * @throws IllegalArgumentException if either date is {@code null}, or the ID of the live default
     *         time zone is not one {@link ZoneId} recognizes.
     * @see #isSameYear(java.util.Date, java.util.Date, ZoneId)
     */
    public static boolean isSameYear(final java.util.Date date1, final java.util.Date date2) throws IllegalArgumentException {
        // defaultZoneId() rather than the TimeZone overload: see isLastDayOfMonth for why the implicit
        // default zone must not be put through toZoneId's round-trip validation.
        return isSameYear(date1, date2, defaultZoneId());
    }

    /**
     * Checks if two date objects are in the same civil year in {@code timeZone}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // 23:00 UTC on Dec 31 and 02:00 UTC on Jan 1 - different years in UTC,
     * // but both already 2025 in Asia/Kolkata (+05:30)
     * java.util.Date d1 = Dates.parseToJUDate("2024-12-31T23:00:00Z");
     * java.util.Date d2 = Dates.parseToJUDate("2025-01-01T02:00:00Z");
     * Dates.isSameYear(d1, d2, TimeZone.getTimeZone("Asia/Kolkata"));   // returns true (both are 2025 there)
     * Dates.isSameYear(d1, d2, TimeZone.getTimeZone("UTC"));            // returns false (2024 vs 2025)
     *
     * Dates.isSameYear(d1, d1, TimeZone.getTimeZone("UTC"));            // returns true (same instant)
     * Dates.isSameYear(d1, d2, (TimeZone) null);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param date1 the first date, not altered, not {@code null}.
     * @param date2 the second date, not altered, not {@code null}.
     * @param timeZone the zone in which the civil year is taken, not {@code null}.
     * @return {@code true} if they represent the same civil year in {@code timeZone}.
     * @throws IllegalArgumentException if any argument is {@code null}, or {@code timeZone} cannot be
     *         represented as a {@link ZoneId}.
     * @see #isSameYear(java.util.Date, java.util.Date, ZoneId)
     */
    public static boolean isSameYear(final java.util.Date date1, final java.util.Date date2, final TimeZone timeZone) throws IllegalArgumentException {
        N.checkArgNotNull(timeZone, cs.timeZone);

        return isSameYear(date1, date2, toZoneId(timeZone));
    }

    /**
     * Checks if two date objects are in the same civil year in {@code zone}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // 23:00 UTC on Dec 31 and 02:00 UTC on Jan 1 - different years in UTC,
     * // but both already 2025 in Asia/Kolkata (+05:30)
     * java.util.Date d1 = Dates.parseToJUDate("2024-12-31T23:00:00Z");
     * java.util.Date d2 = Dates.parseToJUDate("2025-01-01T02:00:00Z");
     * Dates.isSameYear(d1, d2, ZoneId.of("Asia/Kolkata"));   // returns true (both are 2025 there)
     * Dates.isSameYear(d1, d2, ZoneOffset.UTC);              // returns false (2024 vs 2025)
     *
     * Dates.isSameYear(d1, d1, ZoneOffset.UTC);              // returns true (same instant)
     * Dates.isSameYear(d1, d2, (ZoneId) null);               // throws IllegalArgumentException
     * }</pre>
     *
     * @param date1 the first date, not altered, not {@code null}.
     * @param date2 the second date, not altered, not {@code null}.
     * @param zone the zone in which the civil year is taken, not {@code null}.
     * @return {@code true} if they represent the same civil year in {@code zone}.
     * @throws IllegalArgumentException if any argument is {@code null}.
     */
    public static boolean isSameYear(final java.util.Date date1, final java.util.Date date2, final ZoneId zone) throws IllegalArgumentException {
        N.checkArgNotNull(date1, cs.date1);
        N.checkArgNotNull(date2, cs.date2);
        N.checkArgNotNull(zone, cs.zone);

        return localDateAt(date1.getTime(), zone).getYear() == localDateAt(date2.getTime(), zone).getYear();
    }

    /**
     * Checks if two calendar instants fall in the same ISO civil year in their shared time zone.
     * Both calendars must share equivalent time-zone rules; pass an explicit
     * {@link #isSameYear(Calendar, Calendar, ZoneId) zone} to compare two instants in one zone.
     * Calendar chronology is ignored: a {@code GregorianCalendar} and a Buddhist calendar at the
     * same instant compare equal here, matching {@link #isSameYear(Calendar, Calendar, ZoneId)}.
     *
     * <p>15 Mar 2023 13:45 and 28 Nov 2023 06:01 would return {@code true}.
     * 15 Mar 2023 13:45 and 15 Mar 2024 13:45 would return {@code false}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar c1 = Dates.parseToCalendar("2023-03-15 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Calendar c2 = Dates.parseToCalendar("2023-11-28 06:01:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameYear(c1, c2);   // returns true (same year, month ignored)
     *
     * Calendar c3 = Dates.parseToCalendar("2024-03-15 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameYear(c1, c3);                // returns false (2024 vs 2023)
     * Dates.isSameYear((Calendar) null, c2);   // throws IllegalArgumentException
     *
     * Calendar utc = Dates.createCalendar(c1.getTimeInMillis(), TimeZone.getTimeZone("UTC"));
     * Calendar tokyo = Dates.createCalendar(c1.getTimeInMillis(), TimeZone.getTimeZone("Asia/Tokyo"));
     * Dates.isSameYear(utc, tokyo);            // throws IllegalArgumentException (inequivalent zones)
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not {@code null}.
     * @param cal2 the second calendar, not altered, not {@code null}.
     * @return {@code true} if they represent the same ISO civil year in their shared time zone.
     * @throws IllegalArgumentException if either calendar is {@code null}, the calendars are in
     *         inequivalent time zones, or the shared time zone cannot be represented as a {@link ZoneId}. A
     *         calendar whose {@code getTimeZone()} answers {@code null} is read in the live default zone.
     * @see #isSameYear(Calendar, Calendar, ZoneId)
     */
    public static boolean isSameYear(final Calendar cal1, final Calendar cal2) throws IllegalArgumentException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);
        requireCompatibleTimeZones(cal1, cal2);

        return isSameYear(cal1, cal2, toZoneId(zoneOf(cal1)));
    }

    /**
     * Checks if two calendar instants fall in the same civil year in {@code timeZone}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // 23:00 UTC on Dec 31 and 02:00 UTC on Jan 1 - different years in UTC,
     * // but both already 2025 in Asia/Kolkata (+05:30)
     * Calendar c1 = Dates.parseToCalendar("2024-12-31T23:00:00Z");
     * Calendar c2 = Dates.parseToCalendar("2025-01-01T02:00:00Z");
     * Dates.isSameYear(c1, c2, TimeZone.getTimeZone("Asia/Kolkata"));   // returns true (both are 2025 there)
     * Dates.isSameYear(c1, c2, TimeZone.getTimeZone("UTC"));            // returns false (2024 vs 2025)
     *
     * Dates.isSameYear(c1, c1, TimeZone.getTimeZone("UTC"));            // returns true (same instant)
     * Dates.isSameYear(c1, c2, (TimeZone) null);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not {@code null}.
     * @param cal2 the second calendar, not altered, not {@code null}.
     * @param timeZone the zone in which the civil year is taken, not {@code null}.
     * @return {@code true} if both instants are in the same civil year in {@code timeZone}.
     * @throws IllegalArgumentException if any argument is {@code null}, or {@code timeZone} cannot be
     *         represented as a {@link ZoneId}.
     */
    public static boolean isSameYear(final Calendar cal1, final Calendar cal2, final TimeZone timeZone) throws IllegalArgumentException {
        N.checkArgNotNull(timeZone, cs.timeZone);

        return isSameYear(cal1, cal2, toZoneId(timeZone));
    }

    /**
     * Checks if two calendar instants fall in the same civil year in {@code zone}. Each calendar's own
     * time zone is ignored for the comparison.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // 23:00 UTC on Dec 31 and 02:00 UTC on Jan 1 - different years in UTC,
     * // but both already 2025 in Asia/Kolkata (+05:30)
     * Calendar c1 = Dates.parseToCalendar("2024-12-31T23:00:00Z");
     * Calendar c2 = Dates.parseToCalendar("2025-01-01T02:00:00Z");
     * Dates.isSameYear(c1, c2, ZoneId.of("Asia/Kolkata"));   // returns true (both are 2025 there)
     * Dates.isSameYear(c1, c2, ZoneOffset.UTC);              // returns false (2024 vs 2025)
     *
     * Dates.isSameYear(c1, c1, ZoneOffset.UTC);              // returns true (same instant)
     * Dates.isSameYear(c1, c2, (ZoneId) null);               // throws IllegalArgumentException
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not {@code null}.
     * @param cal2 the second calendar, not altered, not {@code null}.
     * @param zone the zone in which the civil year is taken, not {@code null}.
     * @return {@code true} if both instants are in the same civil year in {@code zone}.
     * @throws IllegalArgumentException if any argument is {@code null}.
     */
    public static boolean isSameYear(final Calendar cal1, final Calendar cal2, final ZoneId zone) throws IllegalArgumentException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);
        N.checkArgNotNull(zone, cs.zone);

        return localDateAt(cal1.getTimeInMillis(), zone).getYear() == localDateAt(cal2.getTimeInMillis(), zone).getYear();
    }

    private static LocalDate localDateAt(final long epochMillis, final ZoneId zone) {
        return Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate();
    }

    /**
     * The implicit zone of the civil-field queries that deliberately accept ID-derived rules.
     *
     * <p>{@link ZoneId#systemDefault()} throws {@code ZoneRulesException} - a {@code DateTimeException},
     * not an {@code IllegalArgumentException} - when the default {@code TimeZone} carries an ID
     * {@code java.time} does not know (a hand-built {@link SimpleTimeZone}, say). Every other zone
     * rejection in this class is an {@code IllegalArgumentException}, and these methods document that,
     * so translate rather than leak a second exception type out of one class.</p>
     */
    private static ZoneId defaultZoneId() {
        // One read of the default: ZoneId.systemDefault() is TimeZone.getDefault().toZoneId(), so reading
        // it again for the message could name a zone other than the one that failed.
        final TimeZone defaultTimeZone = TimeZone.getDefault();

        try {
            return defaultTimeZone.toZoneId();
        } catch (final DateTimeException e) {
            // As in toZoneId: a fixed offset is a fixed offset whatever its ID, and the class-level
            // policy accepts one everywhere.
            if (!defaultTimeZone.useDaylightTime() && defaultTimeZone.getDSTSavings() == 0) {
                return toZoneOffset(defaultTimeZone, defaultTimeZone.getRawOffset());
            }

            throw new IllegalArgumentException("The default time zone '" + defaultTimeZone.getID() + "' has an ID that java.time does not recognize", e);
        }
    }

    private static boolean isSameYearMonth(final LocalDate left, final LocalDate right) {
        return left.getYear() == right.getYear() && left.getMonth() == right.getMonth();
    }

    private static void requireCompatibleTimeZones(final Calendar cal1, final Calendar cal2) {
        final TimeZone tz1 = zoneOf(cal1);
        final TimeZone tz2 = zoneOf(cal2);

        if (!haveSameRules(tz1, tz2)) {
            throw new IllegalArgumentException("Calendars must share equivalent time-zone rules to compare civil fields; got '" + tz1.getID() + "' and '"
                    + tz2.getID() + "'. Pass an explicit ZoneId or TimeZone to compare both instants in one zone.");
        }
    }

    /**
     * The zone a {@code Calendar} overload works in when no zone is supplied: the calendar's own, or the live
     * default for the rare implementation whose {@code getTimeZone()} answers {@code null} - the fallback the
     * class contract promises, which the civil-field comparisons used to refuse with an exception.
     */
    private static TimeZone zoneOf(final Calendar calendar) {
        final TimeZone zone = calendar.getTimeZone();

        return zone == null ? TimeZone.getDefault() : zone;
    }

    /**
     * Whether two zones carry the same rules. The reciprocal {@code hasSameRules} check is kept as the
     * cheap first answer, but it is class-dependent ({@code SimpleTimeZone.hasSameRules} is {@code false}
     * for any operand that is not a {@code SimpleTimeZone}), so a calendar in {@code UTC} and one in
     * {@code new SimpleTimeZone(0, "Any")} were reported as inequivalent although their rules are
     * identical. When it says no, the {@link ZoneRules} the two zones convert to decide: those compare
     * by content (a fixed offset equals the registered {@code UTC}/{@code Etc/UTC}/{@code GMT} rules,
     * {@code Asia/Kolkata} equals {@code Asia/Calcutta}), and a zone whose rules no {@link ZoneId} can
     * express is simply not equivalent to anything but itself.
     */
    private static boolean haveSameRules(final TimeZone tz1, final TimeZone tz2) {
        if (tz1.hasSameRules(tz2) && tz2.hasSameRules(tz1)) {
            return true;
        }

        try {
            return toZoneId(tz1).getRules().equals(toZoneId(tz2).getRules());
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    //-----------------------------------------------------------------------

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Checks if two date objects represent the same instant in time.</p>
     *
     * <p>This method compares complete instants. A {@link Timestamp} contributes its nanosecond
     * fraction; other {@code Date} implementations have millisecond precision.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.isSameInstant(new java.util.Date(1000L), new java.util.Date(1000L));       // returns true (same millis)
     * Dates.isSameInstant(new java.util.Date(1000L), new java.util.Date(2000L));       // returns false (different millis)
     *
     * // a java.util.Date and a java.sql.Timestamp at the same instant are equal here
     * Dates.isSameInstant(new java.util.Date(1000L), new java.sql.Timestamp(1000L));   // returns true
     * Dates.isSameInstant((java.util.Date) null, new java.util.Date(1000L));           // throws IllegalArgumentException
     * }</pre>
     *
     * @param date1 the first date, not altered, not {@code null}.
     * @param date2 the second date, not altered, not {@code null}.
     * @return {@code true} if they represent the same instant at the precision available from each value.
     * @throws IllegalArgumentException if either date is {@code null}.
     * @see #isSameInstant(Calendar, Calendar)
     */
    public static boolean isSameInstant(final java.util.Date date1, final java.util.Date date2) throws IllegalArgumentException {
        N.checkArgNotNull(date1, cs.date1);
        N.checkArgNotNull(date2, cs.date2);

        return compareInstants(date1, date2) == 0;
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Checks if two calendar objects represent the same instant in time.</p>
     *
     * <p>This method compares the long millisecond time of the two objects.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar c1 = Dates.createCalendar(1672585530123L);
     * Calendar c2 = Dates.createCalendar(1672585530123L);
     * Dates.isSameInstant(c1, c2);   // returns true (same millisecond instant)
     *
     * Calendar c3 = Dates.createCalendar(1672585530124L);
     * Dates.isSameInstant(c1, c3);                // returns false (1 ms apart)
     * Dates.isSameInstant((Calendar) null, c2);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not {@code null}.
     * @param cal2 the second calendar, not altered, not {@code null}.
     * @return {@code true} if they represent the same millisecond instant.
     * @throws IllegalArgumentException if either calendar is {@code null}.
     * @see #isSameInstant(java.util.Date, java.util.Date)
     */
    public static boolean isSameInstant(final Calendar cal1, final Calendar cal2) throws IllegalArgumentException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);

        return cal1.getTimeInMillis() == cal2.getTimeInMillis();
    }

    //-----------------------------------------------------------------------

    /**
     * Checks if two {@code java.util.Date} objects represent the same local time, comparing their
     * wall-clock fields in the default time zone.
     *
     * <p>The two dates are converted to {@link Calendar} instances in the default time zone and compared
     * field by field (millisecond, second, minute, hour, day, year, era); this is the
     * {@code java.util.Date} counterpart of {@link #isSameLocalTime(Calendar, Calendar)}.</p>
     *
     * <p>Note: this is <b>not</b> equivalent to {@link #isSameInstant(java.util.Date, java.util.Date)}.
     * During a DST fall-back overlap, two different instants render as the same wall-clock time
     * (e.g. in America/Los_Angeles, both 2025-11-02T08:30Z and 2025-11-02T09:30Z display as 01:30);
     * this method returns {@code true} for such a pair while {@code isSameInstant} returns
     * {@code false}. Use {@code isSameInstant} when comparing absolute time.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date d1 = new java.util.Date(1672585530123L);
     * java.util.Date d2 = new java.util.Date(1672585530123L);
     * Dates.isSameLocalTime(d1, d2);                      // returns true (same instant, same local fields)
     * Dates.isSameLocalTime((java.util.Date) null, d2);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p>The civil fields are read on a proleptic Gregorian calendar, so they are the ones
     * {@link #format(java.util.Date)} prints: neither the default locale's calendar system nor the legacy
     * 1582 Julian/Gregorian cutover can change the answer. Both values are read on the same calendar, so
     * the same-runtime-class requirement of {@link #isSameLocalTime(Calendar, Calendar)} is always met
     * here.</p>
     *
     * @param date1 the first date, not altered, not {@code null}.
     * @param date2 the second date, not altered, not {@code null}.
     * @return {@code true} if they represent the same local time in the default time zone.
     * @throws IllegalArgumentException if either date is {@code null}.
     * @see #isSameLocalTime(Calendar, Calendar)
     */
    public static boolean isSameLocalTime(final java.util.Date date1, final java.util.Date date2) throws IllegalArgumentException {
        N.checkArgNotNull(date1, cs.date1);
        N.checkArgNotNull(date2, cs.date2);

        // newProlepticGregorianCalendar, not Calendar.getInstance: the civil fields compared below must
        // be the ones format() prints, so they must not follow the default locale's calendar system or the
        // legacy 1582 cutover - nor, before 1900, the legacy zone table: each value gets the rendering zone
        // format() uses for that instant (see legacyRenderingZone), so two instants that print the same
        // local time compare equal. Both calendars are the same runtime class, so the getClass() clause of
        // the Calendar overload is always satisfied here. The compared fields carry no locale-dependent
        // week conventions, so no locale is needed.
        final TimeZone zone = TimeZone.getDefault();
        final Calendar cal1 = newProlepticGregorianCalendar(legacyRenderingZone(zone, date1.getTime()));
        cal1.setTime(date1);

        final Calendar cal2 = newProlepticGregorianCalendar(legacyRenderingZone(zone, date2.getTime()));
        cal2.setTime(date2);

        return isSameLocalTime(cal1, cal2);
    }

    //-----------------------------------------------------------------------

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Checks if two calendar objects represent the same local time.</p>
     *
     * <p>This method compares the values of the fields of the two objects.
     * In addition, both calendars must be of the same type.</p>
     *
     * <p>Each calendar's fields are read in <b>its own</b> time zone and the zones themselves are not
     * compared, so two calendars in different zones that show the same wall clock are the same local
     * time here. That differs from {@link #isSameDay(Calendar, Calendar)},
     * {@link #isSameMonth(Calendar, Calendar)} and {@link #isSameYear(Calendar, Calendar)}, which
     * reject calendars whose zone rules disagree; use {@link #isSameInstant(Calendar, Calendar)} to
     * compare absolute time.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar c1 = Dates.createCalendar(1672585530123L);
     * Calendar c2 = Dates.createCalendar(1672585530123L);
     * Dates.isSameLocalTime(c1, c2);                        // returns true (identical fields and type)
     *
     * Calendar c3 = Dates.createCalendar(1672585531123L);   // 1 second later
     * Dates.isSameLocalTime(c1, c3);                        // returns false (different second field)
     * Dates.isSameLocalTime((Calendar) null, c2);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not {@code null}.
     * @param cal2 the second calendar, not altered, not {@code null}.
     * @return {@code true} if they show the same local time, each read in its own time zone, and are
     *         of the same runtime type.
     * @throws IllegalArgumentException if either calendar is {@code null}.
     * @see #isSameLocalTime(java.util.Date, java.util.Date)
     * @see #isSameInstant(Calendar, Calendar)
     */
    public static boolean isSameLocalTime(final Calendar cal1, final Calendar cal2) throws IllegalArgumentException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);

        return cal1.get(Calendar.MILLISECOND) == cal2.get(Calendar.MILLISECOND) && cal1.get(Calendar.SECOND) == cal2.get(Calendar.SECOND)
                && cal1.get(Calendar.MINUTE) == cal2.get(Calendar.MINUTE) && cal1.get(Calendar.HOUR_OF_DAY) == cal2.get(Calendar.HOUR_OF_DAY)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) && cal1.getClass() == cal2.getClass();
    }

    private static DateFormat getSDF(final String format, final TimeZone timeZone) {
        return getSDF(format, timeZone, Locale.US);
    }

    /**
     * @throws IllegalArgumentException if {@code locale} is {@code null} or the date-format pattern is invalid.
     */
    private static DateFormat getSDF(final String format, final TimeZone timeZone, final Locale locale) throws IllegalArgumentException {
        N.checkArgNotNull(locale, cs.locale);

        // Predefined machine formats must remain ASCII/English and round-trip independently of a
        // caller locale whose number system or calendar differs from ISO/Gregorian conventions.
        final Locale effectiveLocale = isPredefinedLegacyFormat(format) ? Locale.US : locale;
        DateFormat sdf = null;

        if (Locale.US.equals(effectiveLocale) && UTC_TIME_ZONE.equals(timeZone)) {
            //noinspection ConditionCoveredByFurtherCondition
            if ((format.length() == 28) && format.equals(ISO_8601_TIMESTAMP_FORMAT)) {
                sdf = utcTimestampDFPool.poll();

                if (sdf == null) {
                    sdf = new SimpleDateFormat(format, Locale.US);
                    sdf.setLenient(false);
                    sdf.setTimeZone(timeZone);
                }

                useProlepticGregorianCalendar(sdf, timeZone, effectiveLocale);
                return sdf;
            } else //noinspection ConditionCoveredByFurtherCondition
            if ((format.length() == 24) && format.equals(ISO_8601_DATE_TIME_FORMAT)) {
                sdf = utcDateTimeDFPool.poll();

                if (sdf == null) {
                    sdf = new SimpleDateFormat(format, Locale.US);
                    sdf.setLenient(false);
                    sdf.setTimeZone(timeZone);
                }

                useProlepticGregorianCalendar(sdf, timeZone, effectiveLocale);
                return sdf;
            }
        }

        // Look the pool up without creating an entry. Admitting one here, before the SimpleDateFormat
        // exists, let an invalid pattern - which always throws below - consume one of the
        // MAX_POOLED_FORMATS slots with a queue that could never hold anything; 64 such patterns
        // exhausted the remaining admission slots for the life of the process. The entry is admitted
        // once the pattern has proved constructible and configurable, at the end of this method.
        final Queue<DateFormat> pooled = dfPool.get(new DateFormatKey(format, effectiveLocale));

        if (pooled != null) {
            sdf = pooled.poll();
        }

        if (sdf == null) {
            // Non-lenient so out-of-range input (e.g. month 13) is rejected instead of rolled over.
            sdf = new SimpleDateFormat(format, effectiveLocale);
            sdf.setLenient(false);
        }

        sdf.setTimeZone(timeZone);
        useProlepticGregorianCalendar(sdf, timeZone, effectiveLocale);

        // HTTP-date is always evaluated in GMT; the literal in the pattern must describe the fields
        // that were actually rendered.
        if (HTTP_DATE_FORMAT.equals(format)) {
            sdf.setTimeZone(GMT_TIME_ZONE);
        }

        if (pooled == null) {
            admitDateFormatQueue(format, effectiveLocale);
        }

        return sdf;
    }

    /** Makes legacy formatters agree with java.time and modern civil-date expectations before 1582. */
    private static void useProlepticGregorianCalendar(final DateFormat dateFormat, final TimeZone timeZone, final Locale locale) {
        GregorianCalendar calendar;

        // Exactly GregorianCalendar, not instanceof: sun.util.BuddhistCalendar EXTENDS
        // GregorianCalendar, so an instanceof test silently kept it and a Thai-Buddhist locale printed
        // Buddhist years (2568 for 2025) from a custom pattern, while a Japanese-imperial locale -
        // whose calendar is not a GregorianCalendar - was normalized. The calendar system belongs to
        // this class's proleptic-ISO contract, not to the caller's locale; the locale still chooses
        // month and weekday text and the digit shapes, which is what the locale overloads are for.
        if (dateFormat.getCalendar().getClass() == GregorianCalendar.class) {
            calendar = (GregorianCalendar) dateFormat.getCalendar();
        } else {
            calendar = newProlepticGregorianCalendar(timeZone, locale);
            calendar.setLenient(dateFormat.isLenient());
            dateFormat.setCalendar(calendar);
        }

        calendar.setGregorianChange(new java.util.Date(Long.MIN_VALUE));
    }

    /**
     * Returns whether {@code format} is one of this class's predefined pattern constants.
     *
     * <p>These carry semantics beyond their pattern text: they are machine-readable, so they ignore the
     * caller's locale and stay US/ASCII, and they promise one grammar and one civil view on every entry
     * point, so {@link #formatDate} renders them through the zone rules {@link #legacyRenderingZone}
     * aligns rather than through the JDK's truncated legacy zone table.
     */
    private static boolean isPredefinedLegacyFormat(final String format) {
        return LOCAL_YEAR_FORMAT.equals(format) || LOCAL_MONTH_DAY_FORMAT.equals(format) || LOCAL_DATE_FORMAT.equals(format) || LOCAL_TIME_FORMAT.equals(format)
                || LOCAL_DATE_TIME_FORMAT.equals(format) || LOCAL_TIMESTAMP_FORMAT.equals(format) || ISO_LOCAL_DATE_TIME_FORMAT.equals(format)
                || ISO_OFFSET_DATE_TIME_FORMAT.equals(format) || ISO_OFFSET_TIMESTAMP_FORMAT.equals(format) || ISO_ZONED_DATE_TIME_FORMAT.equals(format)
                || ISO_8601_DATE_TIME_FORMAT.equals(format) || ISO_8601_TIMESTAMP_FORMAT.equals(format) || ISO_LOCAL_TIMESTAMP_FORMAT.equals(format)
                || HTTP_DATE_FORMAT.equals(format);
    }

    private static void recycleSDF(final String format, final TimeZone timeZone, final DateFormat sdf) {
        recycleSDF(format, timeZone, Locale.US, sdf);
    }

    private static void recycleSDF(final String format, final TimeZone timeZone, final Locale locale, final DateFormat sdf) {
        final Locale effectiveLocale = isPredefinedLegacyFormat(format) ? Locale.US : locale;

        if (Locale.US.equals(effectiveLocale) && UTC_TIME_ZONE.equals(timeZone)) {
            //noinspection ConditionCoveredByFurtherCondition
            if ((format.length() == 28) && format.equals(ISO_8601_TIMESTAMP_FORMAT)) {
                utcTimestampDFPool.offer(sdf);
            } else //noinspection ConditionCoveredByFurtherCondition
            if ((format.length() == 24) && format.equals(ISO_8601_DATE_TIME_FORMAT)) {
                utcDateTimeDFPool.offer(sdf);
            } else {
                offerToDateFormatQueue(format, effectiveLocale, sdf);
            }
        } else {
            offerToDateFormatQueue(format, effectiveLocale, sdf);
        }
    }

    /**
     * Admits a pooling queue for a pattern that has just been constructed and configured successfully.
     * Callers must not create the entry earlier: an invalid pattern never yields a {@code DateFormat},
     * so an entry created ahead of construction is a permanently empty queue occupying one of the
     * {@link #MAX_POOLED_FORMATS} slots. Entries are never removed, so admission is the only bound.
     */
    private static void admitDateFormatQueue(final String format, final Locale locale) {
        final DateFormatKey key = new DateFormatKey(format, locale);

        // Once the pool has saturated, admit nothing and - more importantly - do not take the lock.
        // Without this, a service handing out many one-off patterns serialized every format and parse
        // call on one monitor.
        if (dfPool.size() >= MAX_POOLED_FORMATS || dfPool.containsKey(key)) {
            return;
        }

        // The bound check and the insert must be atomic: sizing and inserting separately lets
        // concurrent callers push the pool past MAX_POOLED_FORMATS.
        synchronized (dfPool) {
            if (dfPool.size() < MAX_POOLED_FORMATS) {
                dfPool.putIfAbsent(key, new ArrayBlockingQueue<>(POOL_SIZE));
            }
        }
    }

    private static void offerToDateFormatQueue(final String format, final Locale locale, final DateFormat sdf) {
        final Queue<DateFormat> queue = dfPool.get(new DateFormatKey(format, locale));

        if (queue != null) {
            queue.offer(sdf);
        }
    }

    /**
     * Resolves the effective pattern for the java.time-backed {@code parseTo*} entry points. An explicit
     * {@code format} wins; otherwise the pattern is auto-detected from the text. Bare numeric text is
     * rejected as ambiguous (epoch milliseconds vs. a numeric date) and undetectable text fails with a
     * clear message.
     */
    private static String requireDetectedFormat(final String text, final String format) {
        String detected = checkDateFormat(text, format);

        if (Strings.isEmpty(detected)) {
            // ISO-8601 shapes beyond the legacy table: bracketed region IDs and offsets with seconds
            // (the latter are emitted by the XXXXX-based DTF constants, so auto-detection must read
            // back what this class writes). Only reachable when no explicit format was supplied.
            detected = detectExtendedIsoFormat(text);
        }

        if (Strings.isEmpty(detected)) {
            if (isPossibleLong(text)) {
                throw new IllegalArgumentException("Ambiguous numeric date/time text; supply an explicit format or use parseEpochMillis: \"" + text + "\"");
            }

            throw new IllegalArgumentException("Cannot detect a date/time format for: \"" + text + "\"; supply an explicit format");
        }

        checkFixedFourDigitYearText(text, detected);

        // An explicitly supplied legacy constant keeps that constant's SimpleDateFormat grammar on
        // every static Dates entry point. DTF.ISO_OFFSET_DATE_TIME is deliberately broader (XXXXX)
        // so it can preserve historical offset seconds, but Dates.ISO_OFFSET_DATE_TIME_FORMAT is XXX.
        if (Strings.isNotEmpty(format)) {
            checkIsoOffsetText(text, detected);
        }

        return detected;
    }

    /**
     * DateTimeFormatter's colon-based offset patterns do not accept the compact {@code +HHmm}
     * compatibility form historically accepted by the legacy Dates parser. Normalize only that
     * already-validated standard shape before routing a static Dates call through DTF.
     */
    private static String normalizeCompactIsoOffsetText(final String text, final String format) {
        if (ISO_ZONED_DATE_TIME_FORMAT.equals(format)) {
            // The offset sits before the bracketed region; the ISO zoned grammar insists on the colon as well.
            final int bracket = text.lastIndexOf('[');
            final int sign = bracket > 16 ? Math.max(text.lastIndexOf('+', bracket - 1), text.lastIndexOf('-', bracket - 1)) : -1;

            if (sign > 10 && bracket - sign == 5 && isAllDigits(text, sign + 1, bracket)) {
                return text.substring(0, sign + 3) + ':' + text.substring(sign + 3);
            }

            return text;
        }

        int offsetStart = isoOffsetStart(format);

        if (offsetStart < 0) {
            return text;
        }

        // Auto-detected offset text may carry a 1-9 digit fraction the constant itself does not declare
        // (detectExtendedIsoFormat maps it to ISO_OFFSET_DATE_TIME_FORMAT), and the offset then starts
        // after that fraction. The explicit constants never reach here with a fraction they do not
        // declare - checkIsoOffsetText has rejected it - so the last sign in the text is the offset's.
        if (offsetStart == 19 && text.length() > 20 && text.charAt(19) == '.') {
            offsetStart = Math.max(text.lastIndexOf('+'), text.lastIndexOf('-'));

            if (offsetStart <= 19) {
                return text;
            }
        }

        if (text.length() == offsetStart + 5 && (text.charAt(offsetStart) == '+' || text.charAt(offsetStart) == '-')
                && isAllDigits(text, offsetStart + 1, offsetStart + 5)) {
            return text.substring(0, offsetStart + 3) + ':' + text.substring(offsetStart + 3);
        }

        return text;
    }

    /** Detects ISO-8601 text with a bracketed region ID or a seconds-precision offset, else returns {@code null}. */
    private static String detectExtendedIsoFormat(final String text) {
        final int len = text.length();

        if (len < 20 || text.charAt(4) != '-' || text.charAt(7) != '-' || text.charAt(10) != 'T') {
            return null;
        }

        // '[' sits at 17 in the seconds-less "2025-01-15T10:30Z[UTC]" and at 20 or later otherwise. The offset before
        // the region must be Z or one of the shapes every other path accepts: the ISO zoned grammar alone would
        // also take an hour-only "-05", which the same text without a region is refused for.
        final int bracket = text.lastIndexOf('[');

        if (text.charAt(len - 1) == ']' && bracket > 16) {
            return isIsoOffsetOrZuluBefore(text, bracket) ? ISO_ZONED_DATE_TIME_FORMAT : null;
        }

        if (isFractionalIsoOffsetDateTime(text)) {
            return ISO_OFFSET_DATE_TIME_FORMAT;
        }

        if (len == 28 && (text.charAt(19) == '+' || text.charAt(19) == '-') && text.charAt(22) == ':' && text.charAt(25) == ':') {
            return ISO_OFFSET_DATE_TIME_FORMAT;
        }

        return null;
    }

    /**
     * Returns {@code true} when {@code text} is a Java {@code null} or the case-insensitive
     * {@code "null"} marker written by {@code formatTo} for a null value.
     */
    private static boolean isNullParseInput(final CharSequence text) {
        return text == null || (text.length() == 4 && "null".equalsIgnoreCase(text.toString()));
    }

    /** Rejects empty text; {@code null} references and the {@code "null"} marker are handled by the caller. */
    private static void rejectEmptyDateTime(final CharSequence text) {
        if (Strings.isEmpty(text)) {
            throw new IllegalArgumentException("Cannot parse empty date/time text: \"" + text + "\"");
        }
    }

    /** Rejects predefined partial formats for instant-producing parsing; an instant needs a complete local date. */
    private static void checkCompleteInstantFormat(final String format, final String text) {
        if (LOCAL_YEAR_FORMAT.equals(format) || LOCAL_MONTH_DAY_FORMAT.equals(format) || LOCAL_TIME_FORMAT.equals(format)) {
            throw new IllegalArgumentException("Format '" + format + "' does not contain a complete local date, so no instant can be resolved from: \"" + text
                    + "\"; use a parseToLocal* method and convert explicitly");
        }
    }

    /**
     * Rejects legacy patterns that would synthesize a date from SimpleDateFormat's 1970-01-01 base.
     * Instant-bearing targets accept a complete date with an optional time, but not year-only,
     * month-day, or time-only input.
     */
    private static void checkCompleteLegacyDateFormat(final String text, final String format, final String targetMethod) {
        final String effectiveFormat = checkDateFormat(text, format);

        // An undetected auto shape is delegated to the ISO parser, whose supported forms all carry a
        // complete date. It will produce the authoritative parse error if the value is malformed.
        if (Strings.isEmpty(effectiveFormat) || hasCompleteLegacyDateFields(effectiveFormat)) {
            return;
        }

        throw new IllegalArgumentException("Format '" + effectiveFormat + "' does not contain a complete date required by " + targetMethod + ": \"" + text
                + "\"; use a parseToLocal* method and complete the missing fields explicitly");
    }

    /**
     * SQL Time is an instant-bearing target, but additionally supports a genuinely time-only pattern
     * anchored to 1970-01-01. A pattern containing only part of a date, or lacking enough time fields
     * to identify a clock hour, is rejected rather than allowing SimpleDateFormat to synthesize its
     * missing fields from the epoch base.
     */
    private static void checkCompleteOrTimeOnlyLegacyDateFormat(final String text, final String format, final String targetMethod) {
        final String effectiveFormat = checkDateFormat(text, format);

        // An undetected auto shape is left to the authoritative parser. Otherwise accept a complete
        // date, or a pattern with no date fields and enough fields to identify a clock hour. A
        // zone/literal-only or minute/second-only pattern must not synthesize midnight from
        // SimpleDateFormat's epoch base.
        if (Strings.isEmpty(effectiveFormat) || hasCompleteLegacyDateFields(effectiveFormat)
                || (!hasAnyLegacyDateFields(effectiveFormat) && hasResolvableLegacyTimeFields(effectiveFormat))) {
            return;
        }

        throw new IllegalArgumentException("Format '" + effectiveFormat + "' does not contain a complete date or a resolvable time required by " + targetMethod
                + ": \"" + text + "\"; supply a complete date or use a time-only pattern that identifies a clock hour");
    }

    /** Returns whether a SimpleDateFormat pattern contains any unquoted date field. */
    private static boolean hasAnyLegacyDateFields(final String pattern) {
        return hasAnyUnquotedLegacyPatternLetter(pattern, "GyYMLwWDdFEu");
    }

    /** Returns whether a SimpleDateFormat pattern has enough time-only fields to identify a clock hour. */
    private static boolean hasResolvableLegacyTimeFields(final String pattern) {
        return hasAnyUnquotedLegacyPatternLetter(pattern, "Hk")
                || (hasAnyUnquotedLegacyPatternLetter(pattern, "Kh") && hasAnyUnquotedLegacyPatternLetter(pattern, "a"));
    }

    private static boolean hasAnyUnquotedLegacyPatternLetter(final String pattern, final String patternLetters) {
        boolean inQuote = false;

        for (int i = 0; i < pattern.length(); i++) {
            final char ch = pattern.charAt(i);

            if (ch == '\'') {
                if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '\'') {
                    i++;
                } else {
                    inQuote = !inQuote;
                }
            } else if (!inQuote && patternLetters.indexOf(ch) >= 0) {
                return true;
            }
        }

        return false;
    }

    /** Returns whether a SimpleDateFormat pattern contains enough unquoted fields to identify a date. */
    private static boolean hasCompleteLegacyDateFields(final String pattern) {
        boolean inQuote = false;
        boolean year = false;
        boolean weekYear = false;
        boolean month = false;
        boolean dayOfMonth = false;
        boolean dayOfYear = false;
        boolean weekOfYear = false;
        boolean weekOfMonth = false;
        boolean dayOfWeekInMonth = false;
        boolean dayOfWeek = false;

        for (int i = 0; i < pattern.length(); i++) {
            final char ch = pattern.charAt(i);

            if (ch == '\'') {
                if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '\'') {
                    i++;
                } else {
                    inQuote = !inQuote;
                }

                continue;
            }

            if (inQuote) {
                continue;
            }

            switch (ch) {
                case 'y':
                    year = true;
                    break;
                // 'Y' is the week-based year, which identifies a date only together with week fields:
                // SimpleDateFormat resolves "YYYY-MM-dd" from its unset YEAR, so "2025-01-15" was read
                // as 1970-01-15 while this method reported the pattern as a complete date.
                case 'Y':
                    weekYear = true;
                    break;
                case 'M':
                case 'L':
                    month = true;
                    break;
                case 'd':
                    dayOfMonth = true;
                    break;
                case 'D':
                    dayOfYear = true;
                    break;
                case 'w':
                    weekOfYear = true;
                    break;
                case 'W':
                    weekOfMonth = true;
                    break;
                case 'F':
                    dayOfWeekInMonth = true;
                    break;
                case 'E':
                case 'u':
                    dayOfWeek = true;
                    break;
                default:
                    break;
            }
        }

        return (year && ((month && dayOfMonth) || dayOfYear))
                || ((year || weekYear) && ((weekOfYear && dayOfWeek) || (month && (weekOfMonth || dayOfWeekInMonth) && dayOfWeek)));
    }

    /**
     * Maps a predefined format constant (or a detected/custom pattern) to the {@link DTF} carrying its
     * exact semantics: fixed UTC/GMT zones, proleptic years, and offset-second preservation. This is the
     * semantic mapping layer between legacy pattern strings and java.time formatters &mdash; the quoted
     * {@code 'Z'}/{@code 'GMT'} literals in the legacy constants must become real zone semantics, so the
     * predefined constants map to their {@code DTF} counterparts rather than to {@code DTF.of(pattern)}.
     *
     * <p>The named {@code .SSS} timestamp constants mean exactly three fraction digits when supplied
     * explicitly. When the format was auto-detected instead, local and ISO timestamp text uses the shared
     * 1&ndash;9 digit fraction-of-second grammar, so the timestamp constants then map to variable-fraction
     * formatters.</p>
     */
    private static DTF dtfForParsing(final String format, final boolean autoDetected) {
        if (LOCAL_DATE_FORMAT.equals(format)) {
            return DTF.LOCAL_DATE;
        } else if (LOCAL_TIME_FORMAT.equals(format)) {
            return DTF.LOCAL_TIME;
        } else if (LOCAL_DATE_TIME_FORMAT.equals(format)) {
            return DTF.LOCAL_DATE_TIME;
        } else if (LOCAL_TIMESTAMP_FORMAT.equals(format)) {
            return autoDetected ? DTF.AUTO_LOCAL_TIMESTAMP : DTF.of("uuuu-MM-dd HH:mm:ss.SSS");
        } else if (ISO_LOCAL_DATE_TIME_FORMAT.equals(format)) {
            return DTF.ISO_LOCAL_DATE_TIME;
        } else if (ISO_OFFSET_DATE_TIME_FORMAT.equals(format)) {
            return autoDetected ? DTF.AUTO_ISO_OFFSET_DATE_TIME : DTF.ISO_OFFSET_DATE_TIME;
        } else if (ISO_OFFSET_TIMESTAMP_FORMAT.equals(format)) {
            return DTF.ISO_OFFSET_TIMESTAMP;
        } else if (ISO_ZONED_DATE_TIME_FORMAT.equals(format)) {
            return autoDetected ? DTF.AUTO_ISO_ZONED_DATE_TIME : DTF.ISO_ZONED_DATE_TIME;
        } else if (ISO_8601_DATE_TIME_FORMAT.equals(format)) {
            return DTF.ISO_8601_DATE_TIME;
        } else if (ISO_8601_TIMESTAMP_FORMAT.equals(format)) {
            return autoDetected ? DTF.AUTO_ISO_8601_TIMESTAMP : DTF.ISO_8601_TIMESTAMP;
        } else if (ISO_LOCAL_TIMESTAMP_FORMAT.equals(format)) {
            return autoDetected ? DTF.AUTO_ISO_LOCAL_TIMESTAMP : DTF.of("uuuu-MM-dd'T'HH:mm:ss.SSS");
        } else if (HTTP_DATE_FORMAT.equals(format)) {
            return DTF.HTTP_DATE;
        }

        // Custom patterns retain DateTimeFormatter semantics exactly: in particular, 'y' remains
        // year-of-era and 'u' remains proleptic year. Only the named legacy constants above map to
        // their deliberately proleptic DTF counterparts, with their 0001-9999 contract checked first.
        return DTF.of(format);
    }

    private static String checkDateFormat(final String str, final String format) {
        if (Strings.isEmpty(format)) {
            final int len = str.length();

            // No `len == 4 -> LOCAL_YEAR_FORMAT` case: a 4-digit (or any bare numeric) string is
            // rejected as ambiguous by the parseTo* contract before format detection runs, so such
            // a branch would be unreachable for digits and would contradict the numeric-string contract.
            if (len == 8 && str.charAt(2) == ':' && str.charAt(5) == ':') {
                return LOCAL_TIME_FORMAT;
            } else if (len == 5 && str.charAt(2) == '-') {
                return LOCAL_MONTH_DAY_FORMAT;
            } else if (len > 4 && str.charAt(4) == '-') {
                if (len == 10) {
                    return LOCAL_DATE_FORMAT;
                }

                if (len == 19) {
                    return str.charAt(10) == 'T' ? ISO_LOCAL_DATE_TIME_FORMAT : LOCAL_DATE_TIME_FORMAT;
                }

                if (len == 20) {
                    // A trailing Z belongs to the T-separated form: classifying "2025-01-15 10:30:45Z" as it made the
                    // failure name a shape the text never claimed.
                    return str.charAt(19) == 'Z' && str.charAt(10) == 'T' ? ISO_8601_DATE_TIME_FORMAT : null;
                }

                if (len > 20 && len <= 30 && str.charAt(19) == '.') {
                    final char separator = str.charAt(10);

                    if (separator != ' ' && separator != 'T') {
                        return null;
                    }

                    final boolean zulu = str.charAt(len - 1) == 'Z';
                    final int fractionEnd = zulu ? len - 1 : len;
                    final int fractionDigits = fractionEnd - 20;

                    if (fractionDigits < 1 || fractionDigits > 9 || !isAllDigits(str, 20, fractionEnd)) {
                        return null;
                    }

                    if (separator == ' ') {
                        return zulu ? null : LOCAL_TIMESTAMP_FORMAT;
                    }

                    return zulu ? ISO_8601_TIMESTAMP_FORMAT : ISO_LOCAL_TIMESTAMP_FORMAT;
                }

                if ((len == 24 || len == 25) && (str.charAt(19) == '-' || str.charAt(19) == '+')) {
                    return ISO_OFFSET_DATE_TIME_FORMAT;
                }

                return null;
            } else if (len >= 4 && str.charAt(3) == ',') {
                return HTTP_DATE_FORMAT;
            }
        }

        return format;
    }

    private static TimeZone checkTimeZone(final String dateTime, final String format, final TimeZone timeZone) {
        return checkTimeZone(dateTime, format, timeZone, false, false);
    }

    /**
     * Resolves the zone a fixed-zone format renders or parses in, and rejects a caller-supplied zone
     * that contradicts a fixed-zone format the caller chose.
     *
     * @param zoneIsDefaultSnapshot {@code true} when {@code timeZone} is the caller's captured live default
     *        rather than an explicitly supplied zone. A default snapshot never conflicts with a fixed
     *        UTC/GMT format: the conflict check exists to catch caller mistakes, and the caller did not
     *        choose this zone.
     * @param formatAutoDetected {@code true} when {@code format} was detected from the text rather than
     *        supplied. A designator the <i>text</i> carries ({@code Z}, {@code GMT}) is data, not a caller
     *        choice, so it wins over the fallback zone exactly as a numeric offset or a bracketed region
     *        does; only an explicitly named fixed-zone constant makes a different zone a conflict.
     */
    private static TimeZone checkTimeZone(final String dateTime, final String format, final TimeZone timeZone, final boolean zoneIsDefaultSnapshot,
            final boolean formatAutoDetected) {
        // Snapshot a caller-owned mutable zone before inspecting it, so validation and use observe one
        // coherent rule set even if another thread mutates the original concurrently.
        final TimeZone suppliedTimeZone = timeZone == null ? null : (TimeZone) timeZone.clone();
        final boolean checkConflict = suppliedTimeZone != null && !zoneIsDefaultSnapshot && !formatAutoDetected;

        if (HTTP_DATE_FORMAT.equals(format)) {
            if (checkConflict && !isUtcEquivalent(suppliedTimeZone)) {
                throw new IllegalArgumentException("HTTP-date requires a GMT/UTC-equivalent time zone; " + (dateTime == null ? "" : "input: " + dateTime + ", ")
                        + "format: " + format + ", time zone: " + timeZoneForError(suppliedTimeZone));
            }

            return GMT_TIME_ZONE;
        }

        // 'Z' is a UTC designator only in the two predefined UTC format constants and in an
        // auto-detected ISO-8601 value (no explicit format given). A quoted 'Z' in any other pattern
        // is a plain literal with no zone semantics — never infer semantics from a pattern suffix.
        final boolean utcDesignator = ISO_8601_DATE_TIME_FORMAT.equals(format) || ISO_8601_TIMESTAMP_FORMAT.equals(format)
                || (Strings.isEmpty(format) && Strings.isNotEmpty(dateTime) && dateTime.endsWith("Z"));

        if (utcDesignator) {
            // The UTC designator 'Z' fixes the zone to UTC. Reject a conflicting non-UTC zone rather than
            // silently overriding it, so the format side matches the parse side (both throw IAE on the
            // same caller mistake). A null or zero-offset (UTC-equivalent) zone is accepted and resolved to UTC.
            if (checkConflict && !isUtcEquivalent(suppliedTimeZone)) {
                throw new IllegalArgumentException("The UTC designator 'Z' requires a UTC-equivalent time zone; "
                        + (dateTime == null ? "" : "input: " + dateTime + ", ") + "format: " + format + ", time zone: " + timeZoneForError(suppliedTimeZone));
            }

            return UTC_TIME_ZONE;
        }

        // Calendar and DateFormat retain their TimeZone reference, so never retain a caller-owned
        // instance in the internal pools — a mutable zone (e.g. SimpleTimeZone.setRawOffset) would
        // corrupt pooled instances. TimeZone.getDefault() already returns a fresh clone per call.
        return suppliedTimeZone == null ? TimeZone.getDefault() : suppliedTimeZone;
    }

    /**
     * Whether {@code timeZone} names UTC under any spelling: a registered zone whose rules are a fixed
     * zero offset ({@code UTC}, {@code GMT}, {@code Etc/UTC}, ...) or a fixed zero-offset zone of any
     * class under any ID. This is the one UTC-equivalence test the class uses; {@link DTF#parseZoned}
     * applies the same {@code normalized()} rule to the {@link ZoneId} it already holds.
     *
     * <p>Not {@code timeZone.hasSameRules(UTC_TIME_ZONE)}: that test depends on the runtime class of the
     * receiver. {@code SimpleTimeZone.hasSameRules} is {@code false} for any operand that is not itself a
     * {@code SimpleTimeZone}, so {@code new SimpleTimeZone(0, "UTC")} was rejected as not UTC-equivalent,
     * while {@code ZoneInfo} and the base {@code TimeZone} implementation compare raw offset and
     * daylight-saving use and accepted the very same zone when the operands were swapped, and accepted a
     * hand-written zero-offset {@code TimeZone} subclass outright. Whether a zone is UTC is a property of
     * its rules, not of its class.</p>
     */
    private static boolean isUtcEquivalent(final TimeZone timeZone) {
        try {
            return ZoneOffset.UTC.equals(toZoneId(timeZone).normalized());
        } catch (final IllegalArgumentException e) {
            // toZoneId refuses custom daylight-saving rules, a sub-second or out-of-range offset, and a
            // fixed offset under an ID that names a registered region with different rules; none of those
            // is a fixed zero offset, so the answer is no in every case.
            return false;
        }
    }

    /** Returns a stable, compact zone description for diagnostics without relying on {@link TimeZone#toString()}. */
    private static String timeZoneForError(final TimeZone timeZone) {
        final int rawOffsetMillis = timeZone.getRawOffset();
        final String offset;

        if (rawOffsetMillis == 0) {
            offset = "+00:00"; // ZoneOffset.UTC.getId() is the bare designator "Z", which reads oddly after "raw offset"
        } else if (rawOffsetMillis % 1000 == 0 && Math.abs((long) rawOffsetMillis) <= 18L * 60 * 60 * 1000) {
            offset = ZoneOffset.ofTotalSeconds(rawOffsetMillis / 1000).getId();
        } else {
            offset = rawOffsetMillis + " ms";
        }

        return timeZone.getID() + " (raw offset " + offset + ")";
    }

    /** Ensures that an instant can be represented by HTTP's unsigned four-digit year field. */
    private static void checkHttpDateYear(final Instant instant) {
        final int year = instant.atZone(GMT_ZONE_ID).getYear();

        if (year < 1 || year > 9999) {
            throw new IllegalArgumentException("HTTP-date supports Common Era years 0001 through 9999; got proleptic year " + year);
        }
    }

    /** Returns whether a predefined legacy pattern promises an unsigned four-digit leading year. */
    private static boolean hasFixedFourDigitLeadingYear(final String format) {
        return LOCAL_YEAR_FORMAT.equals(format) || LOCAL_DATE_FORMAT.equals(format) || LOCAL_DATE_TIME_FORMAT.equals(format)
                || LOCAL_TIMESTAMP_FORMAT.equals(format) || ISO_LOCAL_DATE_TIME_FORMAT.equals(format) || ISO_OFFSET_DATE_TIME_FORMAT.equals(format)
                || ISO_OFFSET_TIMESTAMP_FORMAT.equals(format) || ISO_ZONED_DATE_TIME_FORMAT.equals(format) || ISO_8601_DATE_TIME_FORMAT.equals(format)
                || ISO_8601_TIMESTAMP_FORMAT.equals(format) || ISO_LOCAL_TIMESTAMP_FORMAT.equals(format);
    }

    /** Returns whether a format is fixed to UTC/GMT (the two ISO-8601 {@code 'Z'} constants and HTTP-date). */
    private static boolean isZoneFixedFormat(final String format) {
        return ISO_8601_DATE_TIME_FORMAT.equals(format) || ISO_8601_TIMESTAMP_FORMAT.equals(format) || HTTP_DATE_FORMAT.equals(format);
    }

    /**
     * The fallback zone a {@link DTF} parse receives on the static {@code java.time} entry points: none
     * when a fixed-zone shape was <i>detected</i> from the text. The {@code Z} or {@code GMT} the text
     * carries is then data that names the zone, so the fixed-zone formatter must not treat the caller's
     * fallback as a conflict; an explicitly supplied fixed-zone constant keeps that check, exactly as
     * {@link #checkTimeZone} does on the legacy paths.
     */
    private static TimeZone fallbackZoneFor(final String format, final String effectiveFormat, final TimeZone timeZone) {
        return Strings.isEmpty(format) && isZoneFixedFormat(effectiveFormat) ? null : timeZone;
    }

    /** Rejects non-canonical text before SimpleDateFormat can consume more than four year digits. */
    private static void checkFixedFourDigitYearText(final String dateTime, final String format) {
        if (!hasFixedFourDigitLeadingYear(format)) {
            return;
        }

        final boolean valid = dateTime.length() >= 4 && isAllDigits(dateTime, 0, 4)
                && (format.length() == 4 ? dateTime.length() == 4 : dateTime.length() > 4 && dateTime.charAt(4) == format.charAt(4));

        if (!valid || dateTime.startsWith("0000")) {
            throw new IllegalArgumentException(
                    "Format '" + format + "' requires a Common Era year from 0001 through 9999 written as exactly four digits: \"" + dateTime + "\"");
        }
    }

    /**
     * Rejects a non-canonical field width in the predefined fixed-width legacy patterns.
     *
     * <p>{@code SimpleDateFormat} reads {@code MM}, {@code dd}, {@code HH}, {@code mm} and {@code ss} as
     * <i>one or two</i> digits while parsing, so {@code "2025-1-15"} was accepted for
     * {@link #LOCAL_DATE_FORMAT} on the legacy targets even though every {@code java.time} target
     * rejected the same text with the same constant, and auto-detection - which keys on the exact
     * length - rejected it too. The leading year, the fractional second, the ISO offset and the
     * HTTP-date grammar were already width-checked; this closes the remaining eight constants so that
     * one predefined constant means one grammar on every entry point.</p>
     *
     * <p>The three {@code .SSS} constants are checked head-only ({@code yyyy-MM-dd}, the separator and
     * {@code HH:mm:ss}): their tail length is grammar-dependent, because auto-detection accepts a
     * 1&ndash;9 digit fraction where an explicitly supplied constant requires exactly three. The tail is
     * owned by {@link #checkDateFormat(String, String)} on the auto path and by
     * {@link #checkNamedTimestampFraction(String, String)} on the explicit one.</p>
     *
     * <p>Variable-width input is still parseable: supply the variable-width pattern named in the
     * rejection message, which carries no such promise.</p>
     */
    private static void checkFixedWidthLegacyText(final String dateTime, final String format) {
        final int expectedLength; // -1: only the fixed 19-character head is checked here
        final char dateTimeSeparator; // 0 when the pattern has no time part after the date

        if (LOCAL_DATE_FORMAT.equals(format)) {
            expectedLength = 10;
            dateTimeSeparator = 0;
        } else if (LOCAL_TIME_FORMAT.equals(format)) {
            expectedLength = 8;
            dateTimeSeparator = 0;
        } else if (LOCAL_DATE_TIME_FORMAT.equals(format)) {
            expectedLength = 19;
            dateTimeSeparator = ' ';
        } else if (ISO_LOCAL_DATE_TIME_FORMAT.equals(format)) {
            expectedLength = 19;
            dateTimeSeparator = 'T';
        } else if (ISO_8601_DATE_TIME_FORMAT.equals(format)) {
            expectedLength = 20;
            dateTimeSeparator = 'T';
        } else if (LOCAL_TIMESTAMP_FORMAT.equals(format)) {
            expectedLength = -1;
            dateTimeSeparator = ' ';
        } else if (ISO_LOCAL_TIMESTAMP_FORMAT.equals(format) || ISO_8601_TIMESTAMP_FORMAT.equals(format)) {
            expectedLength = -1;
            dateTimeSeparator = 'T';
        } else {
            // The remaining predefined constants already have an exact-shape check of their own
            // (checkIsoOffsetText, the HTTP-date round trip), and ISO_ZONED_DATE_TIME_FORMAT is
            // resolved by the strict java.time engine.
            return;
        }

        if (expectedLength < 0 && (dateTime.length() < 20 || dateTime.charAt(19) != '.')) {
            // Not even a fraction-bearing shape: leave the diagnostic to the fraction/detection checks
            // rather than reporting a field width for text that has no fields at those positions.
            return;
        }

        boolean valid = expectedLength < 0 || dateTime.length() == expectedLength;

        if (valid && LOCAL_TIME_FORMAT.equals(format)) {
            valid = dateTime.charAt(2) == ':' && dateTime.charAt(5) == ':' && isAllDigits(dateTime, 0, 2) && isAllDigits(dateTime, 3, 5)
                    && isAllDigits(dateTime, 6, 8);
        } else if (valid) {
            valid = dateTime.charAt(4) == '-' && dateTime.charAt(7) == '-' && isAllDigits(dateTime, 0, 4) && isAllDigits(dateTime, 5, 7)
                    && isAllDigits(dateTime, 8, 10);

            if (valid && dateTimeSeparator != 0) {
                valid = dateTime.charAt(10) == dateTimeSeparator && dateTime.charAt(13) == ':' && dateTime.charAt(16) == ':' && isAllDigits(dateTime, 11, 13)
                        && isAllDigits(dateTime, 14, 16) && isAllDigits(dateTime, 17, 19) && (expectedLength <= 19 || dateTime.charAt(19) == 'Z');
            }
        }

        if (!valid) {
            throw new IllegalArgumentException("Format '" + format + "' requires its exact canonical shape, every field written as digits at its full width: \""
                    + dateTime + "\"; use a variable-width pattern such as \"" + variableWidthEquivalent(format) + "\" to accept shorter fields");
        }
    }

    /**
     * The variable-width pattern a caller should switch to when {@link #checkFixedWidthLegacyText} rejects
     * their text. One suggestion for every constant that check covers; {@code "yyyy-M-d"} is only right for
     * the date-only one, and naming it for {@link #LOCAL_TIME_FORMAT} told the caller to parse a time with a
     * pattern that has no time field at all.
     *
     * <p>The two UTC-designator constants are suggested with {@code XXX}, not a quoted {@code 'Z'}. In a
     * custom pattern a quoted {@code 'Z'} is a plain literal with no zone semantics (see
     * {@link #checkTimeZone}), so the suggested pattern resolved the text in the parse zone - the one the
     * caller hands {@code parseTo*}, or the JVM default when none is supplied - and silently shifted the
     * instant by that zone's offset, eight hours in {@code America/Los_Angeles}, while still parsing without
     * complaint. {@code XXX} reads the {@code Z} as the zero offset, so the suggestion
     * is the drop-in replacement it claims to be. (The earlier suggestion looked correct under test because
     * the tests pass {@code UTC} explicitly, where the two spellings agree.)</p>
     */
    private static String variableWidthEquivalent(final String format) {
        if (LOCAL_TIME_FORMAT.equals(format)) {
            return "H:m:s";
        } else if (LOCAL_DATE_TIME_FORMAT.equals(format)) {
            return "yyyy-M-d H:m:s";
        } else if (ISO_LOCAL_DATE_TIME_FORMAT.equals(format)) {
            return "yyyy-M-d'T'H:m:s";
        } else if (ISO_8601_DATE_TIME_FORMAT.equals(format)) {
            return "yyyy-M-d'T'H:m:sXXX";
        } else if (LOCAL_TIMESTAMP_FORMAT.equals(format)) {
            return "yyyy-M-d H:m:s.SSS";
        } else if (ISO_LOCAL_TIMESTAMP_FORMAT.equals(format)) {
            return "yyyy-M-d'T'H:m:s.SSS";
        } else if (ISO_8601_TIMESTAMP_FORMAT.equals(format)) {
            return "yyyy-M-d'T'H:m:s.SSSXXX";
        }

        return "yyyy-M-d";
    }

    /** Ensures that a value fits the unsigned four-digit year promised by a predefined pattern. */
    private static void checkFixedFourDigitYear(final java.util.Date date, final TimeZone timeZone, final String format) {
        // Read the year through java.time: that is what renders ISO_ZONED_DATE_TIME_FORMAT and the
        // Calendar default, and what the legacy formatter agrees with once legacyRenderingZone has
        // aligned it. Reading it from a legacy Calendar let Africa/Monrovia's -00:43:08 local mean time
        // print year 0000 for an instant this very check had just accepted as year 0001.
        final int year = civilYear(date, timeZone);

        if (year < 1 || year > 9999) {
            throw new IllegalArgumentException("Format '" + format + "' supports Common Era years from 0001 through 9999; got instant " + exactInstant(date));
        }
    }

    /**
     * The proleptic ISO year {@code date} falls in when read in {@code timeZone}: 1 for 1 CE, 0 for
     * 1 BCE and negative before that, so a Common Era check is simply {@code year >= 1}.
     */
    private static int civilYear(final java.util.Date date, final TimeZone timeZone) {
        try {
            return Instant.ofEpochMilli(date.getTime()).atZone(toZoneId(timeZone)).getYear();
        } catch (final IllegalArgumentException e) {
            // The zone carries rules no ZoneId can express; the legacy civil view is then the only one
            // available, and it is also the one that will render the value.
            final GregorianCalendar calendar = newProlepticGregorianCalendar(timeZone);
            calendar.setTime(date);

            return calendar.get(Calendar.ERA) == GregorianCalendar.AD ? calendar.get(Calendar.YEAR) : 1 - calendar.get(Calendar.YEAR);
        }
    }

    /** Enforces the exact offset grammar promised by the predefined legacy ISO offset pattern. */
    private static void checkIsoOffsetText(final String dateTime, final String format) {
        final int offsetStart = isoOffsetStart(format);

        if (offsetStart < 0) {
            return;
        }

        final int len = dateTime.length();
        final boolean validBase = len > offsetStart && dateTime.charAt(4) == '-' && dateTime.charAt(7) == '-' && dateTime.charAt(10) == 'T'
                && dateTime.charAt(13) == ':' && dateTime.charAt(16) == ':' && isAllDigits(dateTime, 0, 4) && isAllDigits(dateTime, 5, 7)
                && isAllDigits(dateTime, 8, 10) && isAllDigits(dateTime, 11, 13) && isAllDigits(dateTime, 14, 16) && isAllDigits(dateTime, 17, 19)
                && (offsetStart == 19 || (dateTime.charAt(19) == '.' && isAllDigits(dateTime, 20, 23)));
        final boolean utc = validBase && len == offsetStart + 1 && dateTime.charAt(offsetStart) == 'Z';
        final boolean numeric = validBase && (dateTime.charAt(offsetStart) == '+' || dateTime.charAt(offsetStart) == '-')
                && (len == offsetStart + 5 ? isAllDigits(dateTime, offsetStart + 1, offsetStart + 5)
                        : len == offsetStart + 6 && dateTime.charAt(offsetStart + 3) == ':' && isAllDigits(dateTime, offsetStart + 1, offsetStart + 3)
                                && isAllDigits(dateTime, offsetStart + 4, offsetStart + 6));

        if (!utc && !numeric) {
            throw new IllegalArgumentException("Format '" + format + "' requires yyyy-MM-dd'T'HH:mm:ss" + (offsetStart == 19 ? "" : ".SSS")
                    + " followed by Z or a [+-]HH:mm (or [+-]HHmm) offset: \"" + dateTime + "\"");
        }

        if (numeric) {
            final int hours = parseInt(dateTime, offsetStart + 1, offsetStart + 3);
            final int minutes = len == offsetStart + 5 ? parseInt(dateTime, offsetStart + 3, offsetStart + 5)
                    : parseInt(dateTime, offsetStart + 4, offsetStart + 6);

            if (minutes > 59 || hours > 18 || hours == 18 && minutes != 0) {
                throw new IllegalArgumentException("UTC offset must be in the range -18:00 through +18:00: \"" + dateTime + "\"");
            }
        }
    }

    /** The index at which the offset field of a predefined ISO offset pattern starts, or -1. */
    private static int isoOffsetStart(final String format) {
        if (ISO_OFFSET_DATE_TIME_FORMAT.equals(format)) {
            return 19;
        }

        return ISO_OFFSET_TIMESTAMP_FORMAT.equals(format) ? 23 : -1;
    }

    /** Ensures that the predefined legacy ISO offset formatter can emit its effective offset exactly. */
    private static void checkIsoOffsetAtInstant(final java.util.Date date, final TimeZone timeZone) {
        final int offsetMillis = timeZone.getOffset(date.getTime());

        if (offsetMillis % 60_000 != 0) {
            throw new IllegalArgumentException("ISO offset formatting requires a whole-minute UTC offset; got " + offsetMillis + " ms for " + timeZone.getID());
        }

        final int totalMinutes = offsetMillis / 60_000;

        if (Math.abs(totalMinutes) > 18 * 60) {
            throw new IllegalArgumentException(
                    "ISO offset formatting requires an offset from -18:00 through +18:00; got " + offsetMillis + " ms for " + timeZone.getID());
        }
    }

    /**
     * Resolves the five fixed-shape zone-less predefined patterns without {@link SimpleDateFormat}:
     * {@link #LOCAL_DATE_FORMAT}, {@link #LOCAL_TIME_FORMAT}, {@link #LOCAL_DATE_TIME_FORMAT},
     * {@link #ISO_LOCAL_DATE_TIME_FORMAT} and the two {@code .SSS} local timestamp constants. The
     * shape has already been pinned by {@link #checkFixedWidthLegacyText},
     * {@link #checkFixedFourDigitYearText} and {@link #checkNamedTimestampFraction}, so the fields sit
     * at fixed offsets.
     *
     * <p>The local value is resolved through {@link #resolveLocalMillis}, i.e. through the same
     * {@link ZoneId} rules that rounding, the same-day comparisons and every {@code java.time} parser
     * use. A legacy {@link Calendar} resolved it on the JDK's zone table instead, which starts at
     * {@link #LEGACY_ZONE_HISTORY_START}: for a pre-1900 value that returned a different instant than
     * {@code parseToTimestamp} - whose JDBC path has always used java.time - produced from the very
     * same text. The ISO_8601 and offset-bearing formats never reach this method; their callers route
     * them to {@link #parseISO8601} or to the strict java.time engine first.
     *
     * @return the resolved epoch milliseconds, or {@link Long#MIN_VALUE} when {@code str} is not this
     *         format's canonical shape or cannot be resolved, so the caller falls through to the
     *         strict {@code SimpleDateFormat} path and reports one uniform parse failure
     */
    private static long fastDateParse(final String str, final String format, final TimeZone timeZone) {
        final int len = str.length();
        final boolean dateOnly = LOCAL_DATE_FORMAT.equals(format);
        final boolean timeOnly = LOCAL_TIME_FORMAT.equals(format);
        final boolean withFraction = LOCAL_TIMESTAMP_FORMAT.equals(format) || ISO_LOCAL_TIMESTAMP_FORMAT.equals(format);
        final boolean dateTime = LOCAL_DATE_TIME_FORMAT.equals(format) || ISO_LOCAL_DATE_TIME_FORMAT.equals(format);

        if (!(dateOnly || timeOnly || withFraction || dateTime) || len != (dateOnly ? 10 : timeOnly ? 8 : withFraction ? 23 : 19)) {
            return Long.MIN_VALUE;
        }

        if (!hasExpectedFastDateParseSeparators(str, format)) {
            return Long.MIN_VALUE;
        }

        // A time-only value is anchored to 1970-01-01, exactly as SimpleDateFormat's epoch base did.
        final int year = timeOnly ? 1970 : parseInt(str, 0, 4);
        final int month = timeOnly ? 1 : parseInt(str, 5, 7);
        final int day = timeOnly ? 1 : parseInt(str, 8, 10);
        final int timeStart = timeOnly ? 0 : 11;
        final int hourOfDay = dateOnly ? 0 : parseInt(str, timeStart, timeStart + 2);
        final int minute = dateOnly ? 0 : parseInt(str, timeStart + 3, timeStart + 5);
        final int second = dateOnly ? 0 : parseInt(str, timeStart + 6, timeStart + 8);
        final int milliSecond = withFraction ? parseInt(str, 20, 23) : 0;

        // Coarse range check so grossly out-of-range input reaches the strict fallback below instead of
        // producing a java.time message unlike the one every other malformed value gets.
        if (year < 1 || month < 1 || month > 12 || day < 1 || day > 31 || hourOfDay < 0 || hourOfDay > 23 || minute < 0 || minute > 59 || second < 0
                || second > 59 || milliSecond < 0) {
            return Long.MIN_VALUE; // malformed input, fall back to the strict SimpleDateFormat path
        }

        try {
            return resolveLocalMillis(LocalDateTime.of(year, month, day, hourOfDay, minute, second), timeZone) + milliSecond;
        } catch (final DateTimeException | IllegalArgumentException e) {
            // An invalid field combination (30 February) or an unresolvable wall time: fall through to
            // the strict SimpleDateFormat path so the caller gets a consistent "cannot be parsed" error.
            // For the four formats checkGapAndOverlap covers, that check has already thrown a message
            // naming the nonexistent or ambiguous local date-time.
            return Long.MIN_VALUE;
        }
    }

    private static boolean hasExpectedFastDateParseSeparators(final String str, final String format) {
        if (LOCAL_TIME_FORMAT.equals(format)) {
            return str.charAt(2) == ':' && str.charAt(5) == ':';
        }

        if (str.charAt(4) != '-' || str.charAt(7) != '-') {
            return false;
        }

        if (LOCAL_DATE_FORMAT.equals(format)) {
            return true;
        }

        final char separator = ISO_LOCAL_DATE_TIME_FORMAT.equals(format) || ISO_LOCAL_TIMESTAMP_FORMAT.equals(format) ? 'T' : ' ';

        return str.charAt(10) == separator && str.charAt(13) == ':' && str.charAt(16) == ':' && (str.length() == 19 || str.charAt(19) == '.');
    }

    private static int parseInt(final String str, int fromIndex, final int toIndex) {
        int result = 0;

        while (fromIndex < toIndex) {
            final char ch = str.charAt(fromIndex++);

            if (ch < '0' || ch > '9') {
                return -1;
            }

            result = (result * 10) + (ch - '0');
        }

        return result;
    }

    /**
     * Re-applies the sub-millisecond nanoseconds of {@code source} to {@code result} when both are
     * {@link Timestamp}s. Arithmetic in this class round-trips through whole milliseconds, which
     * would otherwise silently zero the sub-millisecond part of a Timestamp's nanos.
     */
    private static <T extends java.util.Date> T preserveSubMillis(final T result, final java.util.Date source) {
        if (result instanceof Timestamp && source instanceof Timestamp) {
            final int subMillis = ((Timestamp) source).getNanos() % 1_000_000;
            final long expectedMillis = result.getTime();
            final Timestamp ts = (Timestamp) result;
            ts.setNanos(((int) Math.floorMod(expectedMillis, 1000L) * 1_000_000) + subMillis);

            if (ts.getTime() != expectedMillis) {
                throw new IllegalStateException(
                        "Restoring Timestamp nanoseconds changed the requested epoch millisecond from " + expectedMillis + " to " + ts.getTime());
            }
        }

        return result;
    }

    /** Removes any sub-millisecond fraction injected by a custom Timestamp creator. */
    private static <T extends java.util.Date> T clearSubMillis(final T result, final long expectedMillis) {
        if (result instanceof Timestamp) {
            final Timestamp ts = (Timestamp) result;
            ts.setNanos((int) Math.floorMod(expectedMillis, 1000L) * 1_000_000);

            if (ts.getTime() != expectedMillis) {
                throw new IllegalStateException(
                        "Clearing Timestamp nanoseconds changed the requested epoch millisecond from " + expectedMillis + " to " + ts.getTime());
            }
        }

        return result;
    }

    private static <T extends java.util.Date> T createDate(final long millis, final T source) {
        final Class<? extends java.util.Date> cls = source.getClass();
        final LongFunction<? extends java.util.Date> creator = dateCreatorPool.get(cls);
        final java.util.Date result;

        if (creator != null) {
            result = creator.apply(millis);

            if (result == null || result.getClass() != cls) {
                throw new IllegalStateException("The creator registered for " + cls.getName() + " returned "
                        + (result == null ? "null" : "an instance of " + result.getClass().getName()));
            }
        } else {
            result = constructOrCloneDate(cls, millis, source);
        }

        if (result == source) {
            throw new IllegalStateException("Creating " + cls.getName() + " returned the source instance; creators must return a distinct object");
        }

        if (result.getTime() != millis) {
            throw new IllegalStateException("Creating " + cls.getName() + " produced an instance at " + result.getTime() + " ms, not the requested " + millis
                    + " ms; register a correct creator via Dates.registerDateCreator");
        }

        return (T) result;
    }

    /**
     * Builds another {@code cls} instance at {@code millis}: a declared {@code (long)} constructor when
     * one exists and can be invoked here, otherwise {@code clone()} of the source. Cloning is what makes
     * subclasses without a usable constructor work at all, and it carries state a fresh construction
     * would discard. An exception thrown by the constructor <i>body</i> is a defect in that class and is
     * not swallowed.
     */
    private static java.util.Date constructOrCloneDate(final Class<? extends java.util.Date> cls, final long millis, final java.util.Date source) {
        final Constructor<? extends java.util.Date> constructor = ClassUtil.getDeclaredConstructor(cls, long.class);

        if (constructor != null) {
            try {
                return ClassUtil.invokeConstructor(constructor, millis);
            } catch (final UncheckedReflectiveOperationException e) {
                // Declared but not invocable from here - an unexported module or a security manager.
                // An exception thrown by the constructor body is a defect in that class and propagates.
            }
        }

        final java.util.Date cloned = cloneDate(source);
        cloned.setTime(millis);

        return cloned;
    }

    /**
     * Returns an independent clone with the same runtime type. Custom date implementations are supported
     * by this class, so a broken or hostile {@code clone()} must be rejected before the clone is mutated
     * or returned to the caller.
     */
    private static java.util.Date cloneDate(final java.util.Date source) {
        final Object cloned = source.clone();

        if (!(cloned instanceof java.util.Date) || cloned == source || cloned.getClass() != source.getClass()) {
            throw new IllegalStateException("Date.clone() for " + source.getClass().getName() + " must return a distinct Date of the same runtime class");
        }

        return (java.util.Date) cloned;
    }

    private static <T extends Calendar> T createCalendar(final T source, final long millis) {
        final Class<T> cls = (Class<T>) source.getClass();
        final LongObjFunction<? super Calendar, ? extends java.util.Calendar> creator = calendarCreatorPool.get(cls);

        if (creator != null) {
            // A creator is third-party code. Give it an isolated template so even a creator that
            // configures or clears that argument cannot mutate the caller's source calendar.
            final Calendar template = cloneCalendar(source);
            final java.util.Calendar result = creator.apply(millis, template);

            if (result == null || result.getClass() != cls) {
                throw new IllegalStateException("The creator registered for " + cls.getName() + " returned "
                        + (result == null ? "null" : "an instance of " + result.getClass().getName()));
            }

            if (result == source) {
                throw new IllegalStateException(
                        "The creator registered for " + cls.getName() + " returned the source calendar; creators must return a distinct object");
            }

            // The template's settings are part of the contract: reapply them so a creator that forgot
            // (or deliberately discarded) them cannot silently alter calendar semantics.
            copyCalendarSettings(source, result);

            if (result.getTimeInMillis() != millis) {
                throw new IllegalStateException("The creator registered for " + cls.getName() + " returned an instance at " + result.getTimeInMillis()
                        + " ms, not the requested " + millis + " ms");
            }

            return (T) result;
        } else {
            final T result = constructOrCloneCalendar(cls, millis, source);

            copyCalendarSettings(source, result);

            if (result.getTimeInMillis() != millis) {
                throw new IllegalStateException("Creating " + cls.getName() + " produced an instance at " + result.getTimeInMillis() + " ms, not the requested "
                        + millis + " ms; register a correct creator via Dates.registerCalendarCreator");
            }

            return result;
        }
    }

    /**
     * Builds another {@code cls} calendar at {@code millis}: a declared {@code (long)} or no-arg
     * constructor when one exists and can be invoked here, otherwise {@code clone()} of the source.
     *
     * <p>Cloning is not a last resort but the only strategy that works for the JDK's own alternate
     * calendars &mdash; {@code Calendar.getInstance()} returns a {@code JapaneseImperialCalendar} or a
     * {@code BuddhistCalendar} under some default locales, and neither can be constructed reflectively
     * nor registered through {@link #registerCalendarCreator(Class, LongObjFunction)}, which refuses
     * built-in packages. An exception thrown by a constructor <i>body</i> is a defect in that class and
     * is not swallowed.</p>
     */
    private static <T extends Calendar> T constructOrCloneCalendar(final Class<T> cls, final long millis, final T source) {
        Constructor<T> constructor = ClassUtil.getDeclaredConstructor(cls, long.class);

        if (constructor != null) {
            try {
                return ClassUtil.invokeConstructor(constructor, millis);
            } catch (final UncheckedReflectiveOperationException e) {
                // Declared but not invocable from here; try the no-arg form, then clone(). An exception
                // thrown by the constructor body is a defect in that class and propagates.
            }
        }

        constructor = ClassUtil.getDeclaredConstructor(cls);

        if (constructor != null) {
            try {
                final T result = ClassUtil.invokeConstructor(constructor);
                result.setTimeInMillis(millis);

                return result;
            } catch (final UncheckedReflectiveOperationException e) {
                // Declared but not invocable from here; fall back to clone().
            }
        }

        final T cloned = (T) cloneCalendar(source);
        cloned.setTimeInMillis(millis);

        return cloned;
    }

    private enum ModifyType {
        /**
         * Truncation.
         */
        TRUNCATE,

        /**
         * Rounding.
         */
        ROUND,

        /**
         * Ceiling.
         */
        CEILING
    }

    /**
     * Checks if the provided instant is the last date of its proleptic ISO month, evaluated in the
     * live JVM default time zone. The legacy {@link GregorianCalendar} cutover is not applied.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.isLastDayOfMonth(Dates.parseToDate("2023-01-31"));   // returns true (Jan has 31 days)
     * Dates.isLastDayOfMonth(Dates.parseToDate("2024-02-29"));   // returns true (Feb 29 in a leap year)
     *
     * Dates.isLastDayOfMonth(Dates.parseToDate("2023-01-15"));   // returns false (mid-month)
     * Dates.isLastDayOfMonth((java.util.Date) null);             // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to check.
     * @return {@code true} if the provided date is the last date of its month.
     * @throws IllegalArgumentException if the date is {@code null}, or if the ID of the live default
     *         time zone is not one {@link ZoneId} recognizes. A default zone that merely customizes the
     *         rules of a known ID is accepted, and the known ID's rules are used.
     */
    public static boolean isLastDayOfMonth(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        // defaultZoneId(), not toZoneId(TimeZone.getDefault()): this is a civil-field query, so the
        // ID-derived rules are good enough and a default zone that merely customizes its rules must not
        // make it fail. toZoneId's round-trip validation is kept for caller-supplied zones and for
        // instant resolution, where silently substituting different rules would move the instant. A
        // default zone whose ID java.time does not know at all is still rejected - as an
        // IllegalArgumentException, which is what defaultZoneId() adds over ZoneId.systemDefault().
        final LocalDate localDate = Instant.ofEpochMilli(date.getTime()).atZone(defaultZoneId()).toLocalDate();
        return localDate.getDayOfMonth() == localDate.lengthOfMonth();
    }

    /**
     * Checks if the provided instant is the last date of its proleptic ISO year, evaluated in the
     * live JVM default time zone. The legacy {@link GregorianCalendar} cutover is not applied.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.isLastDayOfYear(Dates.parseToDate("2023-12-31"));   // returns true (Dec 31)
     * Dates.isLastDayOfYear(Dates.parseToDate("2024-12-31"));   // returns true (Dec 31 of a leap year)
     *
     * Dates.isLastDayOfYear(Dates.parseToDate("2023-12-15"));   // returns false (mid-December)
     * Dates.isLastDayOfYear((java.util.Date) null);             // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to check.
     * @return {@code true} if the provided date is the last date of its year.
     * @throws IllegalArgumentException if the date is {@code null}, or if the ID of the live default
     *         time zone is not one {@link ZoneId} recognizes. A default zone that merely customizes the
     *         rules of a known ID is accepted, and the known ID's rules are used.
     */
    public static boolean isLastDayOfYear(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        final LocalDate localDate = Instant.ofEpochMilli(date.getTime()).atZone(defaultZoneId()).toLocalDate();
        return localDate.getDayOfYear() == localDate.lengthOfYear();
    }

    /**
     * Returns the number of days in the proleptic ISO month containing the given instant
     * (28&ndash;31), following {@link LocalDate#lengthOfMonth()} semantics. Evaluated in the live JVM
     * default time zone; the legacy {@link GregorianCalendar} cutover is not applied.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.lengthOfMonth(Dates.parseToDate("2023-01-15"));   // returns 31 (January)
     * Dates.lengthOfMonth(Dates.parseToDate("2023-02-15"));   // returns 28 (February, non-leap year)
     *
     * Dates.lengthOfMonth(Dates.parseToDate("2024-02-15"));   // returns 29 (February, leap year)
     * Dates.lengthOfMonth((java.util.Date) null);             // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to be evaluated.
     * @return the number of days in the month of the given date.
     * @throws IllegalArgumentException if the date is {@code null}, or if the ID of the live default
     *         time zone is not one {@link ZoneId} recognizes. A default zone that merely customizes the
     *         rules of a known ID is accepted, and the known ID's rules are used.
     */
    public static int lengthOfMonth(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return Instant.ofEpochMilli(date.getTime()).atZone(defaultZoneId()).toLocalDate().lengthOfMonth();
    }

    /**
     * Returns the number of days in the proleptic ISO year containing the given instant (365 or 366),
     * following {@link LocalDate#lengthOfYear()} semantics. Evaluated in the live JVM default time
     * zone; the legacy {@link GregorianCalendar} cutover is not applied.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.lengthOfYear(Dates.parseToDate("2023-01-15"));   // returns 365 (non-leap year)
     * Dates.lengthOfYear(Dates.parseToDate("2024-02-15"));   // returns 366 (leap year)
     *
     * Dates.lengthOfYear(Dates.parseToDate("2023-12-31"));   // returns 365 (any date in the year)
     * Dates.lengthOfYear((java.util.Date) null);             // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to be evaluated.
     * @return the number of days in the year of the given date.
     * @throws IllegalArgumentException if the date is {@code null}, or if the ID of the live default
     *         time zone is not one {@link ZoneId} recognizes. A default zone that merely customizes the
     *         rules of a known ID is accepted, and the known ID's rules are used.
     */
    public static int lengthOfYear(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return Instant.ofEpochMilli(date.getTime()).atZone(defaultZoneId()).toLocalDate().lengthOfYear();
    }

    /**
     * Checks if two date ranges overlap.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date d1 = new java.util.Date(1000L), d5 = new java.util.Date(5000L);
     * java.util.Date d10 = new java.util.Date(10000L), d15 = new java.util.Date(15000L);
     * java.util.Date d20 = new java.util.Date(20000L);
     *
     * Dates.isOverlapping(d1, d10, d5, d15);     // returns true (overlap between 5000 and 10000)
     * Dates.isOverlapping(d1, d10, d15, d20);    // returns false (disjoint ranges)
     * Dates.isOverlapping(d1, d10, d10, d20);    // returns false (adjacent; endpoints are exclusive)
     *
     * Dates.isOverlapping(null, d10, d5, d15);   // throws IllegalArgumentException (null argument)
     * }</pre>
     *
     * @param startDate1 start of the first range, not {@code null}.
     * @param endDate1 end of the first range, not {@code null}.
     * @param startDate2 start of the second range, not {@code null}.
     * @param endDate2 end of the second range, not {@code null}.
     * @return {@code true} if the two date ranges overlap.
     *         Ranges are half-open {@code [start, end)}; an empty range ({@code start == end})
     *         contains no instant and therefore never overlaps.
     *         {@code Timestamp} endpoints are compared at nanosecond precision; other
     *         {@code Date} implementations contribute millisecond precision.
     * @throws IllegalArgumentException if any argument is {@code null}, or if a start date is after its
     *         corresponding end date.
     * @see #isBetween(java.util.Date, java.util.Date, java.util.Date)
     */
    public static boolean isOverlapping(final java.util.Date startDate1, final java.util.Date endDate1, final java.util.Date startDate2,
            final java.util.Date endDate2) throws IllegalArgumentException {
        N.checkArgNotNull(startDate1, cs.startDate1);
        N.checkArgNotNull(endDate1, cs.endDate1);
        N.checkArgNotNull(startDate2, cs.startDate2);
        N.checkArgNotNull(endDate2, cs.endDate2);

        final Instant start1 = exactInstant(startDate1);
        final Instant end1 = exactInstant(endDate1);
        final Instant start2 = exactInstant(startDate2);
        final Instant end2 = exactInstant(endDate2);
        final int range1 = start1.compareTo(end1);
        final int range2 = start2.compareTo(end2);

        if (range1 > 0 || range2 > 0) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }

        // Ranges are half-open [start, end). An empty range (start == end) contains no instant,
        // so it cannot overlap anything.
        if (range1 == 0 || range2 == 0) {
            return false;
        }

        return start1.compareTo(end2) < 0 && start2.compareTo(end1) < 0;
    }

    /**
     * Checks if two calendar ranges overlap. Ranges are half-open {@code [start, end)}, so ranges that
     * merely touch at an endpoint do not overlap. This is the {@link Calendar} counterpart of
     * {@link #isOverlapping(java.util.Date, java.util.Date, java.util.Date, java.util.Date)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar c1 = Dates.createCalendar(1000L), c5 = Dates.createCalendar(5000L);
     * Calendar c10 = Dates.createCalendar(10000L), c15 = Dates.createCalendar(15000L);
     * Dates.isOverlapping(c1, c10, c5, c15);    // returns true (overlap between 5000 and 10000)
     * Dates.isOverlapping(c1, c10, c10, c15);   // returns false (adjacent; endpoints are exclusive)
     *
     * Dates.isOverlapping(c5, c5, c1, c10);     // returns false (an empty range never overlaps)
     * Dates.isOverlapping(null, c10, c5, c15);  // throws IllegalArgumentException
     * Dates.isOverlapping(c10, c1, c5, c15);    // throws IllegalArgumentException (start is after end)
     * }</pre>
     *
     * @param startDate1 start of the first range, not {@code null}.
     * @param endDate1 end of the first range, not {@code null}.
     * @param startDate2 start of the second range, not {@code null}.
     * @param endDate2 end of the second range, not {@code null}.
     * @return {@code true} if the two calendar ranges overlap.
     *         Ranges are half-open {@code [start, end)}; an empty range ({@code start == end})
     *         contains no instant and therefore never overlaps.
     * @throws IllegalArgumentException if any argument is {@code null}, or if a start is after its corresponding
     *         end.
     * @see #isOverlapping(java.util.Date, java.util.Date, java.util.Date, java.util.Date)
     */
    public static boolean isOverlapping(final Calendar startDate1, final Calendar endDate1, final Calendar startDate2, final Calendar endDate2)
            throws IllegalArgumentException {
        N.checkArgNotNull(startDate1, cs.startDate1);
        N.checkArgNotNull(endDate1, cs.endDate1);
        N.checkArgNotNull(startDate2, cs.startDate2);
        N.checkArgNotNull(endDate2, cs.endDate2);

        if (startDate1.after(endDate1) || startDate2.after(endDate2)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }

        // Ranges are half-open [start, end). An empty range (start == end) contains no instant,
        // so it cannot overlap anything.
        if (!startDate1.before(endDate1) || !startDate2.before(endDate2)) {
            return false;
        }

        return startDate1.before(endDate2) && startDate2.before(endDate1);
    }

    /**
     * Checks if the given date is between the specified start date and end date, inclusive.
     * It means {@code startDate <= date <= endDate}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date start = new java.util.Date(1000L), end = new java.util.Date(3000L);
     *
     * Dates.isBetween(new java.util.Date(2000L), start, end);   // returns true (within the range)
     * Dates.isBetween(start, start, end);                       // returns true (start boundary is inclusive)
     * Dates.isBetween(end, start, end);                         // returns true (end boundary is inclusive)
     *
     * Dates.isBetween(new java.util.Date(4000L), start, end);   // returns false (after the end)
     * Dates.isBetween(null, start, end);                        // throws IllegalArgumentException (null argument)
     * }</pre>
     *
     * <p><b>Endpoints are inclusive on purpose</b>, unlike the half-open {@code [start, end)} ranges of
     * {@link #isOverlapping(java.util.Date, java.util.Date, java.util.Date, java.util.Date)}. The two
     * answer different questions: a range pair either shares an interval of time or it does not, while a
     * single instant is naturally tested against a closed period ("was it within the campaign?").
     * Compose {@code isBetween(x, start, end) && !Dates.isSameInstant(x, end)} for a half-open test.
     * {@code equals} is not usable there because it compares more than the instant this method
     * compares: {@link Timestamp#equals(Object)} rejects a plain {@code java.util.Date} at the same
     * instant, and {@link Calendar#equals(Object)} also compares time zone, leniency and week rules.</p>
     *
     * <p>{@link Timestamp} values are compared at nanosecond precision; other {@code Date}
     * implementations contribute millisecond precision.</p>
     *
     * @param date the date to check.
     * @param startDate the start of the range (inclusive).
     * @param endDate the end of the range (inclusive).
     * @return {@code true} if the date is within the specified range (inclusive).
     * @throws IllegalArgumentException if any argument is {@code null}, or if {@code startDate} is after
     *         {@code endDate}.
     * @see N#geAndLe(Comparable, Comparable, Comparable)
     * @see N#gtAndLt(Comparable, Comparable, Comparable)
     */
    // @ai-ignore isBetween endpoint convention - the inclusive [start, end] bound is deliberate and
    // differs from isOverlapping's half-open [start, end) ranges by design. Do not suggest aligning them.
    public static boolean isBetween(final java.util.Date date, final java.util.Date startDate, final java.util.Date endDate) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(startDate, cs.startDate);
        N.checkArgNotNull(endDate, cs.endDate);

        if (compareInstants(startDate, endDate) > 0) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }

        return compareInstants(date, startDate) >= 0 && compareInstants(date, endDate) <= 0;
    }

    /**
     * Checks if the given calendar is between the specified start and end calendars, inclusive.
     * It means {@code startDate <= date <= endDate}. This is the {@link Calendar} counterpart of
     * {@link #isBetween(java.util.Date, java.util.Date, java.util.Date)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar start = Dates.createCalendar(1000L), end = Dates.createCalendar(3000L);
     * Dates.isBetween(Dates.createCalendar(2000L), start, end);   // returns true (within the range)
     * Dates.isBetween(start, start, end);                         // returns true (start boundary is inclusive)
     * Dates.isBetween(end, start, end);                           // returns true (end boundary is inclusive)
     *
     * Dates.isBetween(Dates.createCalendar(4000L), start, end);   // returns false (after the end)
     * Dates.isBetween(null, start, end);                          // throws IllegalArgumentException
     * Dates.isBetween(start, end, start);                         // throws IllegalArgumentException (start is after end)
     * }</pre>
     *
     * <p><b>Endpoints are inclusive on purpose</b>, unlike the half-open {@code [start, end)} ranges of
     * {@link #isOverlapping(java.util.Date, java.util.Date, java.util.Date, java.util.Date)}. The two
     * answer different questions: a range pair either shares an interval of time or it does not, while a
     * single instant is naturally tested against a closed period ("was it within the campaign?").
     * Compose {@code isBetween(x, start, end) && !Dates.isSameInstant(x, end)} for a half-open test.
     * {@code equals} is not usable there because it compares more than the instant this method
     * compares: {@link Timestamp#equals(Object)} rejects a plain {@code java.util.Date} at the same
     * instant, and {@link Calendar#equals(Object)} also compares time zone, leniency and week rules.</p>
     *
     * @param date the calendar to check.
     * @param startDate the start of the range (inclusive).
     * @param endDate the end of the range (inclusive).
     * @return {@code true} if the calendar is within the specified range (inclusive).
     * @throws IllegalArgumentException if any argument is {@code null}, or if {@code startDate} is after
     *         {@code endDate}.
     * @see #isBetween(java.util.Date, java.util.Date, java.util.Date)
     */
    // @ai-ignore isBetween endpoint convention - see isBetween(java.util.Date, java.util.Date, java.util.Date).
    public static boolean isBetween(final Calendar date, final Calendar startDate, final Calendar endDate) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(startDate, cs.startDate);
        N.checkArgNotNull(endDate, cs.endDate);

        if (startDate.after(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }

        return N.geAndLe(date, startDate, endDate);
    }

    /**
     * Appends the literal string {@code "null"} to {@code appendable} — the {@code formatTo} counterpart
     * of {@code format(null)} returning {@code null}, since an Appendable cannot "return null".
     * @throws IllegalArgumentException if {@code appendable} is {@code null}.
     * @throws UncheckedIOException if appending the text fails.
     */
    static void formatToForNull(final Appendable appendable) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(appendable, cs.appendable);

        try {
            appendable.append(Strings.NULL);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Immutable formatter facade for the predefined patterns above and caller-supplied
     * {@link DateTimeFormatter} patterns. It formats both modern and legacy temporal types and provides
     * conversions to commonly used {@code java.time}, {@code java.util}, and {@code java.sql} types.
     *
     * <p><b>Key Features:</b>
     * <ul>
     *   <li><b>Standard Format Constants:</b> Pre-defined formatters for common date/time patterns</li>
     *   <li><b>ISO-8601 Formats:</b> Predefined local, offset, zoned, and UTC patterns</li>
     *   <li><b>Type-Safe Parsing:</b> Dedicated methods for each temporal type</li>
     *   <li><b>Null Safety:</b> Graceful handling of {@code null} inputs with {@code @MayReturnNull} annotations</li>
     *   <li><b>Reuse:</b> Each instance retains one immutable formatter, and {@link #of(String)} caches a
     *       bounded number of distinct patterns, so a repeated pattern normally returns the same instance.
     *       Past that bound it returns an equal but distinct one, so compare formatters with
     *       {@code equals} rather than {@code ==}. Instances have value equality and may be used as map
     *       keys</li>
     *   <li><b>Time Zone Support:</b> Handling of offsets and zone IDs where the selected pattern supplies them</li>
     *   <li><b>Strict Parsing:</b> All parsing resolves with {@link ResolverStyle#STRICT}; the pattern is
     *       authoritative, so purely numeric input is matched against the pattern like any other text
     *       (it is <i>not</i> treated as epoch milliseconds &mdash; use {@link Instant#ofEpochMilli(long)}
     *       or the {@code Dates.create*} methods for epoch input). Instant-producing parsers reject
     *       daylight-saving gaps and ambiguous overlaps unless an explicit valid offset disambiguates
     *       an overlap</li>
     * </ul>
     *
     * <p><b>Year patterns:</b> Date-bearing predefined formatters use {@code uuuu}, the signed
     * proleptic ISO year, and therefore round-trip year zero and BCE years. The enclosing class's
     * {@code Dates.*_FORMAT} strings target legacy {@link SimpleDateFormat} and retain {@code yyyy};
     * passing one to {@link #of(String)} preserves that year-of-era pattern exactly.</p>
     *
     * <p><b>Shared result contract:</b> Where {@code Dates} and {@code DTF} expose the same target,
     * they use the same civil-field versus instant semantics, textual-zone precedence, null-reference
     * and {@code "null"}-marker handling (both parse to Java {@code null}), and SQL {@code Date}/{@code Time} epoch-millisecond retention. In
     * particular, {@code parseToTime} accepts either a complete date or a genuinely time-only pattern
     * that resolves an actual clock time; it rejects a partial date, a zone/literal-only pattern, or
     * unresolved time fields in both APIs. Corresponding predefined formats resolve identically
     * within the legacy constants' representable year and offset domain.
     * Custom-pattern parsing still belongs to each formatter engine: legacy {@code Dates} targets use
     * non-lenient {@link SimpleDateFormat} (which chooses one side of a DST overlap), while {@code DTF}
     * and the static {@code Dates.parseToLocalXxx/OffsetDateTime/ZonedDateTime/Instant} methods use strict
     * {@link DateTimeFormatter} resolution; their instant-producing methods reject an overlap unless an
     * offset disambiguates it.
     * The legacy {@link Dates#ISO_OFFSET_DATE_TIME_FORMAT} is an {@code XXX} pattern (with the historical
     * compact {@code +HHmm} parsing extension), while {@link #ISO_OFFSET_DATE_TIME} uses {@code XXXXX}
     * so offset seconds can round-trip without loss.</p>
     *
     * <p><b>Legacy time zones:</b> Methods accepting {@link TimeZone} support registered IDs and
     * fixed-offset zones representable by {@link ZoneOffset}, whatever their ID. They reject a zone
     * with daylight-saving rules under an unregistered ID, or one that reuses a registered ID with
     * different rules, because those rules cannot be represented faithfully by a {@link ZoneId}. The
     * fallback zone is consulted only for zone-less text: a zone or offset written in the text does
     * not require the fallback to be representable, and the fallback is not converted at all in that
     * case. Only the fixed-zone formatters ({@link #ISO_8601_DATE_TIME}, {@link #ISO_8601_TIMESTAMP},
     * {@link #HTTP_DATE}) always check a supplied zone, because a non-UTC-equivalent one is a caller
     * mistake whatever the text says.</p>
     *
     * <p><b>Supported Formats:</b>
     * <ul>
     *   <li><b>Zoned DateTime:</b> {@code ISO_ZONED_DATE_TIME_FORMAT} with timezone ID</li>
     *   <li><b>Offset DateTime:</b> {@code ISO_OFFSET_DATE_TIME_FORMAT} with UTC offset</li>
     *   <li><b>Local DateTime:</b> Standard local date-time without timezone</li>
     *   <li><b>Local Date:</b> Date-only formats without time component</li>
     *   <li><b>Local Time:</b> Time-only formats without date component</li>
     *   <li><b>Instant:</b> UTC-based instant representation</li>
     * </ul>
     *
     * <p><b>Core Operations:</b>
     * <ul>
     *   <li><b>Parsing:</b> {@code parseTo*} methods for converting strings to temporal objects</li>
     *   <li><b>Formatting:</b> {@code format} methods for temporal object to string conversion</li>
     *   <li><b>Input Handling:</b> Parse methods return {@code null} for a {@code null} reference or the
     *       case-insensitive literal {@code "null"}; empty text throws {@code IllegalArgumentException};
     *       other invalid input is rejected</li>
     *   <li><b>Predefined Formatters:</b> Public constants for common patterns, plus {@link DTF#of(String)}
     *       for custom patterns</li>
     * </ul>
     *
     * <p><b>Common Usage Patterns:</b>
     * <pre>{@code
     * // Parsing different temporal types (each parser is invoked on a DTF instance)
     * ZonedDateTime zdt = DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime("2023-12-25T15:30:45-05:00[America/New_York]");
     * OffsetDateTime odt = DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime("2023-12-25T15:30:45+05:30");
     * LocalDateTime ldt = DTF.ISO_LOCAL_DATE_TIME.parseToLocalDateTime("2023-12-25T15:30:45");
     * LocalDate ld = DTF.LOCAL_DATE.parseToLocalDate("2023-12-25");
     * LocalTime lt = DTF.LOCAL_TIME.parseToLocalTime("15:30:45");
     * Instant instant = DTF.ISO_8601_DATE_TIME.parseToInstant("2023-12-25T20:30:45Z");
     *
     * // Formatting temporal objects
     * String formatted = DTF.ISO_ZONED_DATE_TIME.format(ZonedDateTime.now());
     * String localFormatted = DTF.LOCAL_DATE_TIME.format(LocalDateTime.now());
     *
     * // Custom patterns
     * DTF custom = DTF.of("MM/dd/yyyy HH:mm");
     * String customFormatted = custom.format(LocalDateTime.now());
     * }</pre>
     *
     * <p><b>Relationship to Enclosing Class:</b>
     * This class complements {@link Dates} by providing modern Java Time API support,
     * while the enclosing class focuses on legacy date/time types. Use this class for:
     * <ul>
     *   <li>New code requiring Java 8+ time types</li>
     *   <li>Applications needing strong type safety</li>
     *   <li>Systems requiring immutable temporal objects</li>
     *   <li>APIs exposing time-zone aware operations</li>
     * </ul>
     *
     * <p><b>Thread Safety:</b>
     * {@code DTF} instances are immutable and thread-safe (the backing {@link DateTimeFormatter} is
     * immutable), so a single instance can be safely shared across threads without synchronization. The
     * {@code formatTo} methods write into a caller-supplied {@link Appendable}, whose own thread-safety
     * is the caller's responsibility.
     *
     * <p><b>DTF ==> Date Time Formatter</b></p>
     *
     * @see DateTimeFormatter
     * @see ZonedDateTime
     * @see OffsetDateTime
     * @see LocalDateTime
     * @see LocalDate
     * @see LocalTime
     * @see Instant
     * @see Dates
     */
    public static final class DTF {

        // java.time uses 'u' for the signed proleptic ISO year. The legacy public pattern constants
        // intentionally retain SimpleDateFormat's 'yyyy'; sharing those strings made the built-in DTFs
        // lose year zero and BCE years on a format/parse round trip.
        private static final String PROLEPTIC_LOCAL_DATE_FORMAT = "uuuu-MM-dd";
        private static final String PROLEPTIC_LOCAL_DATE_TIME_FORMAT = "uuuu-MM-dd HH:mm:ss";
        private static final String PROLEPTIC_ISO_LOCAL_DATE_TIME_FORMAT = "uuuu-MM-dd'T'HH:mm:ss";
        // Five X letters, not three: 'XXX' silently drops non-zero offset seconds when formatting
        // (e.g. historical Europe/Amsterdam +00:17:30), which corrupts the instant and breaks
        // format/parse round trips. 'XXXXX' appends ':ss' only when the offset has non-zero seconds,
        // so output for whole-minute offsets is unchanged and all previously parseable text still parses.
        private static final String PROLEPTIC_ISO_OFFSET_DATE_TIME_FORMAT = "uuuu-MM-dd'T'HH:mm:ssXXXXX";
        private static final String PROLEPTIC_ISO_OFFSET_TIMESTAMP_FORMAT = "uuuu-MM-dd'T'HH:mm:ss.SSSXXXXX";
        private static final String PROLEPTIC_ISO_ZONED_DATE_TIME_FORMAT = "uuuu-MM-dd'T'HH:mm:ssXXXXX'['VV']'";
        private static final String PROLEPTIC_ISO_8601_DATE_TIME_FORMAT = "uuuu-MM-dd'T'HH:mm:ss'Z'";
        private static final String PROLEPTIC_ISO_8601_TIMESTAMP_FORMAT = "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'";
        // `format` carries the legacy pattern these grammars stand in for, not their display name: it is
        // read as a pattern (containsAnyDatePatternField), so a name there answered that question by
        // coincidence. `displayName` is what error messages show.
        private static final DTF AUTO_ISO_OFFSET_DATE_TIME = new DTF(Dates.ISO_OFFSET_DATE_TIME_FORMAT, "ISO_OFFSET_DATE_TIME", false,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.withResolverStyle(ResolverStyle.STRICT));
        private static final DTF AUTO_ISO_ZONED_DATE_TIME = new DTF(Dates.ISO_ZONED_DATE_TIME_FORMAT, "ISO_ZONED_DATE_TIME", false,
                DateTimeFormatter.ISO_ZONED_DATE_TIME.withResolverStyle(ResolverStyle.STRICT));

        // The auto-detected timestamp grammars accept 1-9 fraction digits where the named constants
        // require exactly three. Built once: they were previously rebuilt on every auto-detected parse.
        private static final DTF AUTO_LOCAL_TIMESTAMP = ofVariableFraction(Dates.LOCAL_TIMESTAMP_FORMAT, "uuuu-MM-dd HH:mm:ss", false);
        private static final DTF AUTO_ISO_LOCAL_TIMESTAMP = ofVariableFraction(Dates.ISO_LOCAL_TIMESTAMP_FORMAT, "uuuu-MM-dd'T'HH:mm:ss", false);
        private static final DTF AUTO_ISO_8601_TIMESTAMP = ofVariableFraction(Dates.ISO_8601_TIMESTAMP_FORMAT, "uuuu-MM-dd'T'HH:mm:ss", true);

        // Patterns are caller controlled. Bound the cache so a service that receives many one-off
        // patterns cannot grow it indefinitely; past the bound every call builds a fresh formatter.
        private static final int MAX_CACHED_PATTERNS = 128;

        private static final Map<DateFormatKey, DTF> patternCache = new ConcurrentCacheMap<>(MAX_CACHED_PATTERNS);

        /**
         * Date/Time format: {@code uuuu-MM-dd} ({@code uuuu} is the signed proleptic ISO year).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.LOCAL_DATE.format(LocalDate.of(2023, 12, 25));   // returns "2023-12-25"
         * DTF.LOCAL_DATE.parseToLocalDate("2023-12-25");       // returns 2023-12-25
         *
         * // a legacy value is rendered in the live default zone
         * DTF.LOCAL_DATE.format(Dates.parseToJUDate("2023-12-25 00:00:00", "yyyy-MM-dd HH:mm:ss"));
         *                                                       // returns "2023-12-25"
         * DTF.LOCAL_DATE.format((java.util.Date) null);         // returns null
         * }</pre>
         *
         * @see Dates#LOCAL_DATE_FORMAT
         */
        public static final DTF LOCAL_DATE = new DTF(PROLEPTIC_LOCAL_DATE_FORMAT);

        /**
         * Date/Time format: {@code HH:mm:ss}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.LOCAL_TIME.format(LocalTime.of(15, 30, 45));   // returns "15:30:45"
         * DTF.LOCAL_TIME.parseToLocalTime("14:25:30");       // returns 14:25:30
         *
         * DTF.LOCAL_TIME.format(LocalTime.of(15, 30, 45, 123_000_000));
         *                                                     // returns "15:30:45" (the pattern carries no fraction)
         * DTF.LOCAL_TIME.format((java.util.Date) null);       // returns null
         * }</pre>
         *
         * @see Dates#LOCAL_TIME_FORMAT
         */
        public static final DTF LOCAL_TIME = new DTF(Dates.LOCAL_TIME_FORMAT);

        /**
         * Date/Time format: {@code uuuu-MM-dd HH:mm:ss}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.LOCAL_DATE_TIME.format(LocalDateTime.of(2023, 12, 25, 15, 30, 45));
         *                                                                // returns "2023-12-25 15:30:45"
         * DTF.LOCAL_DATE_TIME.parseToLocalDateTime("2023-12-25 14:25:30");
         *                                                                // returns 2023-12-25T14:25:30
         *
         * DTF.LOCAL_DATE_TIME.format((java.util.Calendar) null);         // returns null
         * }</pre>
         *
         * @see Dates#LOCAL_DATE_TIME_FORMAT
         */
        public static final DTF LOCAL_DATE_TIME = new DTF(PROLEPTIC_LOCAL_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code uuuu-MM-dd'T'HH:mm:ss}.
         *
         * <p>ISO 8601 format without timezone information. Useful for standard date-time representation
         * in APIs and data exchange where local time (without offset) is sufficient.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_LOCAL_DATE_TIME.format(LocalDateTime.of(2023, 12, 25, 15, 30, 45));
         *                                                                // returns "2023-12-25T15:30:45"
         * DTF.ISO_LOCAL_DATE_TIME.parseToLocalDateTime("2023-12-25T14:25:30");
         *                                                                // returns 2023-12-25T14:25:30
         *
         * DTF.ISO_LOCAL_DATE_TIME.format((java.util.Date) null);         // returns null
         * }</pre>
         *
         * @see Dates#ISO_LOCAL_DATE_TIME_FORMAT
         */
        public static final DTF ISO_LOCAL_DATE_TIME = new DTF(PROLEPTIC_ISO_LOCAL_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code uuuu-MM-dd'T'HH:mm:ssXXXXX}.
         *
         * <p>ISO 8601 format with UTC offset. Useful for representing a moment in time with offset
         * information, but without timezone ID. Format includes offset like +05:30, -08:00, or Z for UTC.
         * Non-zero offset seconds are preserved (e.g. {@code +00:19:32}); whole-minute offsets keep the
         * shorter {@code ±HH:mm} form, so previously formatted text still parses.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.of(2023, 12, 25, 15, 30, 45, 0, ZoneOffset.of("+05:30")));
         *                                                       // returns "2023-12-25T15:30:45+05:30"
         * DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime("2023-12-25T14:25:30-08:00");
         *                                                       // returns 2023-12-25T14:25:30-08:00
         *
         * DTF.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.of(2023, 12, 25, 15, 30, 45, 0, ZoneOffset.UTC));
         *                                                       // returns "2023-12-25T15:30:45Z" (a zero offset writes Z)
         * DTF.ISO_OFFSET_DATE_TIME.format((java.util.Date) null);   // returns null
         * }</pre>
         *
         * @see Dates#ISO_OFFSET_DATE_TIME_FORMAT
         */
        public static final DTF ISO_OFFSET_DATE_TIME = new DTF(PROLEPTIC_ISO_OFFSET_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code uuuu-MM-dd'T'HH:mm:ss.SSSXXXXX}.
         *
         * <p>The millisecond-bearing sibling of {@link #ISO_OFFSET_DATE_TIME}, and the {@code java.time}
         * counterpart of {@link Dates#ISO_OFFSET_TIMESTAMP_FORMAT} &mdash; the format
         * {@code Dates.format(value, null, timeZone)} writes for {@code java.sql.Date}, {@link Time} and
         * {@link Timestamp}. Non-zero offset seconds are preserved; whole-minute offsets keep the shorter
         * {@code ±HH:mm} form.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_OFFSET_TIMESTAMP.format(OffsetDateTime.of(2025, 1, 15, 16, 0, 45, 123_000_000, ZoneOffset.of("+05:30")));
         *                                                       // returns "2025-01-15T16:00:45.123+05:30"
         * DTF.ISO_OFFSET_TIMESTAMP.parseToTimestamp("2025-01-15T16:00:45.123+05:30").getTime();
         *                                                       // returns 1736937045123
         *
         * DTF.ISO_OFFSET_TIMESTAMP.format(new java.sql.Timestamp(1736937045123L));   // rendered in the live default zone
         * DTF.ISO_OFFSET_TIMESTAMP.format((java.util.Date) null);                    // returns null
         * }</pre>
         *
         * @see Dates#ISO_OFFSET_TIMESTAMP_FORMAT
         * @see #ISO_OFFSET_DATE_TIME
         */
        public static final DTF ISO_OFFSET_TIMESTAMP = new DTF(PROLEPTIC_ISO_OFFSET_TIMESTAMP_FORMAT);

        /**
         * Date/Time format: {@code uuuu-MM-dd'T'HH:mm:ssXXXXX'['VV']'}.
         *
         * <p>ISO 8601 format with timezone ID. Preserves the offset and timezone identifier
         * with whole-second precision; fractional seconds are omitted.
         * Non-zero offset seconds are preserved (e.g. historical {@code Europe/Amsterdam} offsets such as
         * {@code +00:17:30}), so formattable values round-trip through its own strict parser
         * after their fractional seconds are discarded.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_ZONED_DATE_TIME.format(ZonedDateTime.of(2023, 12, 25, 15, 30, 45, 0, ZoneId.of("Asia/Kolkata")));
         *                                                       // returns "2023-12-25T15:30:45+05:30[Asia/Kolkata]"
         * DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime("2023-12-25T14:25:30-08:00[America/Los_Angeles]");
         *                                                       // returns 2023-12-25T14:25:30-08:00[America/Los_Angeles]
         *
         * DTF.ISO_ZONED_DATE_TIME.format((java.util.Date) null);   // returns null
         * }</pre>
         *
         * @see Dates#ISO_ZONED_DATE_TIME_FORMAT
         */
        public static final DTF ISO_ZONED_DATE_TIME = new DTF(PROLEPTIC_ISO_ZONED_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code uuuu-MM-dd'T'HH:mm:ss'Z'}.
         *
         * <p>ISO 8601 UTC format (Zulu time). Always represents UTC time with the 'Z' suffix.
         * Useful for API communication and logging where UTC time is preferred.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_8601_DATE_TIME.parseToInstant("2023-12-25T14:25:30Z");   // returns 2023-12-25T14:25:30Z
         * DTF.ISO_8601_DATE_TIME.format(ZonedDateTime.of(2023, 12, 25, 15, 30, 45, 0, ZoneId.of("UTC")));
         *                                                                  // returns "2023-12-25T15:30:45Z"
         *
         * // the fixed 'Z' formatter renders in UTC whatever the default zone is
         * DTF.ISO_8601_DATE_TIME.format(new java.util.Date(1703514645000L));   // returns "2023-12-25T14:30:45Z"
         * DTF.ISO_8601_DATE_TIME.format((java.util.Date) null);                // returns null
         * }</pre>
         *
         * @see Dates#ISO_8601_DATE_TIME_FORMAT
         */
        public static final DTF ISO_8601_DATE_TIME = new DTF(PROLEPTIC_ISO_8601_DATE_TIME_FORMAT, true, false);

        /**
         * Date/Time format: {@code uuuu-MM-dd'T'HH:mm:ss.SSS'Z'}.
         *
         * <p>ISO 8601 UTC timestamp format with milliseconds. Provides higher precision than ISO_8601_DATE_TIME
         * by including milliseconds. Ideal for high-precision logging and timing operations.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_8601_TIMESTAMP.parseToInstant("2023-12-25T14:25:30.456Z");   // returns 2023-12-25T14:25:30.456Z
         * DTF.ISO_8601_TIMESTAMP.format(Dates.parseToJUDate("2023-12-25T15:30:45.123Z"));
         *                                                                      // returns "2023-12-25T15:30:45.123Z"
         *
         * DTF.ISO_8601_TIMESTAMP.format((java.util.Date) null);                // returns null
         * }</pre>
         *
         * @see Dates#ISO_8601_TIMESTAMP_FORMAT
         */
        public static final DTF ISO_8601_TIMESTAMP = new DTF(PROLEPTIC_ISO_8601_TIMESTAMP_FORMAT, true, false);

        /**
         * Strict HTTP-date format: {@code EEE, dd MMM yyyy HH:mm:ss 'GMT'}.
         *
         * <p>The instant is always converted to GMT. Parsing accepts the literal {@code GMT} only;
         * named zones such as {@code PST} and numeric offsets are rejected. Formatting rejects years
         * outside the Common Era range 0001 through 9999 required by HTTP's four-digit grammar.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // the value suitable for a Date, Last-Modified, or Expires header
         * DTF.HTTP_DATE.format(ZonedDateTime.of(2023, 12, 25, 15, 30, 45, 0, ZoneId.of("GMT")));
         *                                                     // returns "Mon, 25 Dec 2023 15:30:45 GMT"
         * DTF.HTTP_DATE.parseToZonedDateTime("Mon, 25 Dec 2023 14:25:30 GMT");
         *                                                     // returns 2023-12-25T14:25:30Z[GMT]
         *
         * // the instant is always converted to GMT, whatever zone the value carries
         * DTF.HTTP_DATE.format(new java.util.Date(1703514645000L));   // returns "Mon, 25 Dec 2023 14:30:45 GMT"
         * DTF.HTTP_DATE.format((java.util.Date) null);                // returns null
         * }</pre>
         *
         * @see Dates#HTTP_DATE_FORMAT
         */
        public static final DTF HTTP_DATE = new DTF(Dates.HTTP_DATE_FORMAT, false, true);

        /**
         * Alias for {@link #HTTP_DATE}.
         *
         * @deprecated use {@link #HTTP_DATE}.
         */
        @Deprecated
        public static final DTF RFC_1123_DATE_TIME = HTTP_DATE;

        private final String format;
        /** What error messages and {@link #toString()} show; differs from {@code format} only where the
         * real grammar is wider than the pattern text (the auto-detected variable-fraction forms). */
        private final String displayName;
        private final boolean utcZFormat; // one of the two predefined UTC constants: values are UTC instants
        private final boolean httpDateFormat;
        private final DateTimeFormatter dateTimeFormatter;
        /** Locale used for textual fields and for week settings on Calendar parse results. */
        private final Locale locale;

        DTF(final String format) {
            this(format, false, false);
        }

        private DTF(final String format, final boolean utcZFormat, final boolean httpDateFormat) {
            this(format, Locale.US, utcZFormat, httpDateFormat);
        }

        private DTF(final String format, final Locale locale, final boolean utcZFormat, final boolean httpDateFormat) {
            this.format = format;
            this.displayName = format;
            this.locale = locale;
            // Fixed-zone behavior is an explicit property of the named predefined constants. It is
            // never inferred from pattern text: quoted 'Z' and 'GMT' remain ordinary literals for DTF.of.
            this.utcZFormat = utcZFormat;
            this.httpDateFormat = httpDateFormat;

            final DateTimeFormatter dtf;

            {
                final DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().appendPattern(format);

                if (containsYearOfEra(format) && !containsProlepticYear(format)) {
                    builder.parseDefaulting(ChronoField.ERA, IsoEra.CE.getValue());
                }

                dtf = builder.toFormatter(locale).withResolverStyle(ResolverStyle.STRICT);
            }

            // The 'Z' in the UTC constants is a quoted literal meaning UTC: instant-bearing temporals
            // (Zoned/Offset/Instant) are converted to UTC when formatting, not stamped with 'Z' on
            // their local wall-clock fields. Zone-less temporals are rejected by format(TemporalAccessor).
            dateTimeFormatter = utcZFormat ? dtf.withZone(UTC_ZONE_ID) : (httpDateFormat ? dtf.withZone(GMT_ZONE_ID) : dtf);
        }

        /** Parsing-only instance carrying a pre-built formatter (see {@link #ofVariableFraction}). */
        private DTF(final String format, final String displayName, final boolean utcZFormat, final DateTimeFormatter dateTimeFormatter) {
            this.format = format;
            this.displayName = displayName;
            this.utcZFormat = utcZFormat;
            this.httpDateFormat = false;
            this.dateTimeFormatter = dateTimeFormatter;
            this.locale = Locale.US;
        }

        /**
         * Builds a parsing-only {@code DTF} for auto-detected timestamp text, whose fraction follows the
         * shared auto-detection grammar: 1&ndash;9 digits read as a fraction of a second, matching JDBC
         * fraction semantics, with trailing zeros trimmed ({@code java.sql.Timestamp.toString()} writes
         * {@code .5} for half a second and {@code .12} for 120&nbsp;ms). A fixed-width {@code SSS} pattern
         * under {@link ResolverStyle#STRICT} would reject
         * such text, so the fraction is parsed with {@link DateTimeFormatterBuilder#appendFraction}
         * instead. Used only for auto-detection; the named {@code .SSS} constants keep their strict
         * exactly-three-digits meaning when supplied explicitly.
         *
         * @param basePattern the pattern up to and including the seconds field, without any fraction
         * @param utcZLiteral whether a literal {@code 'Z'} (meaning UTC) follows the fraction
         */
        private static DTF ofVariableFraction(final String format, final String basePattern, final boolean utcZLiteral) {
            final DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().appendPattern(basePattern)
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true);

            if (utcZLiteral) {
                builder.appendLiteral('Z');
            }

            DateTimeFormatter formatter = builder.toFormatter(Locale.US).withResolverStyle(ResolverStyle.STRICT);

            if (utcZLiteral) {
                formatter = formatter.withZone(UTC_ZONE_ID);
            }

            return new DTF(format, format + " with a 1-9 digit fraction of a second" + (utcZLiteral ? " and a 'Z' designator" : ""), utcZLiteral, formatter);
        }

        /**
         * Whether {@code pattern} contains an unquoted {@code 'y'} (year-of-era) pattern letter.
         * Quoted literals (and the {@code ''} escape) are skipped.
         */
        private static boolean containsYearOfEra(final String pattern) {
            return containsUnquotedPatternLetter(pattern, 'y');
        }

        private static boolean containsProlepticYear(final String pattern) {
            return containsUnquotedPatternLetter(pattern, 'u');
        }

        /** Returns whether a DateTimeFormatter pattern contains any unquoted date field. */
        private static boolean containsAnyDatePatternField(final String pattern) {
            return containsAnyUnquotedPatternLetter(pattern, "GuyYQqMLwWEecFdgD");
        }

        private static boolean containsUnquotedPatternLetter(final String pattern, final char patternLetter) {
            return containsAnyUnquotedPatternLetter(pattern, String.valueOf(patternLetter));
        }

        private static boolean containsAnyUnquotedPatternLetter(final String pattern, final String patternLetters) {
            boolean inQuote = false;

            for (int i = 0; i < pattern.length(); i++) {
                final char ch = pattern.charAt(i);

                if (ch == '\'') {
                    if (inQuote && i + 1 < pattern.length() && pattern.charAt(i + 1) == '\'') {
                        i++; // escaped quote inside a literal
                    } else {
                        inQuote = !inQuote;
                    }
                } else if (!inQuote && patternLetters.indexOf(ch) >= 0) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Parses {@code text} with this formatter, wrapping any failure in the uniform
         * {@link IllegalArgumentException} contract (input, pattern, and error index in the message;
         * the underlying {@link DateTimeException} retained as cause). When the text carries both an
         * offset and a zone, their consistency is validated for every caller, regardless of which
         * temporal type is requested.
         */
        private TemporalAccessor parseRaw(final CharSequence text) {
            final TemporalAccessor parsed;

            if (httpDateFormat && text.length() != 29) {
                throw parseFailure(text,
                        new DateTimeParseException("HTTP-date must use the canonical 29-character IMF-fixdate form", text, Math.min(text.length(), 29)));
            }

            try {
                parsed = dateTimeFormatter.parse(text);
            } catch (final DateTimeException | IllegalArgumentException e) {
                throw parseFailure(text, e);
            }

            try {
                validateOffsetAgainstZone(parsed);
            } catch (final DateTimeException e) {
                throw parseFailure(text, e);
            }

            if (httpDateFormat) {
                final String canonical;

                try {
                    canonical = dateTimeFormatter.format(parsed);
                } catch (final DateTimeException e) {
                    throw parseFailure(text, e);
                }

                if (!canonical.contentEquals(text)) {
                    throw parseFailure(text, new DateTimeParseException("Non-canonical or contradictory HTTP-date", text, 0));
                }
            }

            return parsed;
        }

        /**
         * When the text carries both an offset and a zone, the offset must be valid for the zone at the
         * parsed local date-time (e.g. {@code 12:00-08:00[America/Los_Angeles]} in July is rejected).
         * A missing time defaults to midnight for this validation; instant-producing parsers separately
         * require a complete date.
         */
        private void validateOffsetAgainstZone(final TemporalAccessor parsed) {
            if (!parsed.isSupported(ChronoField.OFFSET_SECONDS)) {
                return;
            }

            final ZoneId zone = parsed.query(TemporalQueries.zone());

            if (zone == null) {
                return;
            }

            final LocalDate date = parsed.query(TemporalQueries.localDate());
            final LocalTime time = parsed.query(TemporalQueries.localTime());

            if (date == null && time == null) {
                return;
            }

            final ZoneOffset parsedOffset = ZoneOffset.ofTotalSeconds(parsed.get(ChronoField.OFFSET_SECONDS));
            final LocalDateTime localDateTime = LocalDateTime.of(date == null ? LocalDate.ofEpochDay(0) : date, time == null ? LocalTime.MIDNIGHT : time);

            resolveLocalDateTimeStrict(localDateTime, zone, parsedOffset);
        }

        /**
         * As {@link #parseRaw(CharSequence)} but reduces the parsed fields with {@code query}
         * (e.g. {@code LocalDate::from}).
         */
        private <R> R parseWith(final CharSequence text, final TemporalQuery<R> query) {
            final TemporalAccessor parsed = parseRaw(text);

            try {
                return query.queryFrom(parsed);
            } catch (final DateTimeException e) {
                throw parseFailure(text, e);
            }
        }

        /**
         * Resolves {@code text} to a {@code ZonedDateTime}. Offset/zone information in the text always
         * wins (their consistency was validated by {@link #parseRaw}); zone-less values are interpreted
         * in {@code zone}, or the live default zone when {@code zone} is {@code null}. A complete local
         * date is required (time-only input is rejected); a missing time defaults to midnight.
         * Nonexistent local times in a DST gap and ambiguous local times in an
         * overlap are rejected unless an offset in the text disambiguates the overlap.
         */
        private ZonedDateTime parseZoned(final CharSequence text, final TimeZone fallbackZone) {
            return parseZoned(text, fallbackZone, parseRaw(text));
        }

        /**
         * Resolves an already parsed value; used when a caller also needs its zone provenance.
         *
         * <p>{@code fallbackZone} is converted to a {@link ZoneId} only where it is used. A zone or offset
         * written in the text makes it irrelevant, so a fallback no {@code ZoneId} can express must not
         * fail such a parse: converting it up front made {@code parseToCalendar} reject text that
         * {@code parseToJUDate} accepted under the same default zone, and it charged every zone-bearing
         * text the most expensive step of a short parse for a zone it never used. The fixed-zone conflict
         * check still reads it eagerly: that check exists to catch a caller mistake, whatever the text
         * says.</p>
         */
        private ZonedDateTime parseZoned(final CharSequence text, final TimeZone fallbackZone, final TemporalAccessor parsed) {
            if ((utcZFormat || httpDateFormat) && fallbackZone != null) {
                final ZoneId zone;

                try {
                    zone = fallbackZoneId(fallbackZone);
                } catch (final IllegalArgumentException e) {
                    // Wrapped exactly as the same failure is wrapped inside the block below, so every zone
                    // diagnosis leaves this class in one shape: the cause. Left bare, a diagnosis carrying
                    // a DateTimeException of its own was restated by the static entry points as a failure
                    // to parse the text, while the same zone on zone-less text was reported as the zone.
                    throw parseFailure(text, e);
                }

                if (utcZFormat && !ZoneOffset.UTC.equals(zone.normalized())) {
                    throw new IllegalArgumentException(
                            "Pattern '" + displayName + "' carries the UTC designator 'Z' and requires a UTC-equivalent zone; got: " + zone);
                }

                if (httpDateFormat && !ZoneOffset.UTC.equals(zone.normalized())) {
                    throw new IllegalArgumentException("HTTP-date has fixed GMT semantics and requires a GMT/UTC-equivalent zone; got: " + zone);
                }
            }

            try {
                final LocalDate date = parsed.query(TemporalQueries.localDate());
                final LocalTime time = parsed.query(TemporalQueries.localTime());

                if (date == null) {
                    // An instant requires a complete local date: time-only input has no date on which
                    // to determine zone/DST rules, so it is rejected rather than silently placed on the
                    // epoch day.
                    throw new DateTimeException("No date fields in \"" + text + "\"; a complete local date is required to resolve an instant");
                }

                final LocalDateTime localDateTime = LocalDateTime.of(date, time == null ? LocalTime.MIDNIGHT : time);
                final ZoneOffset parsedOffset = parsed.isSupported(ChronoField.OFFSET_SECONDS)
                        ? ZoneOffset.ofTotalSeconds(parsed.get(ChronoField.OFFSET_SECONDS))
                        : null;
                ZoneId effectiveZone = parsed.query(TemporalQueries.zone());

                if (effectiveZone == null && parsedOffset != null) {
                    effectiveZone = parsedOffset;
                }

                if (effectiveZone == null) {
                    effectiveZone = utcZFormat ? UTC_ZONE_ID : fallbackZoneId(fallbackZone);
                }

                return resolveLocalDateTimeStrict(localDateTime, effectiveZone, parsedOffset);
            } catch (final DateTimeException | IllegalArgumentException e) {
                throw parseFailure(text, e);
            }
        }

        /**
         * The {@link ZoneId} a zone-less value resolves in: {@code fallbackZone}, or the live default when
         * it is {@code null}. Snapshots a caller-owned mutable zone before reading it.
         */
        private static ZoneId fallbackZoneId(final TimeZone fallbackZone) {
            return toZoneId(fallbackZone == null ? TimeZone.getDefault() : (TimeZone) fallbackZone.clone());
        }

        /** Returns a zone/offset supplied by the parsed value (including a fixed formatter zone). */
        private static ZoneId parsedZone(final TemporalAccessor parsed) {
            ZoneId result = parsed.query(TemporalQueries.zone());

            if (result == null && parsed.isSupported(ChronoField.OFFSET_SECONDS)) {
                result = ZoneOffset.ofTotalSeconds(parsed.get(ChronoField.OFFSET_SECONDS));
            }

            return result;
        }

        private IllegalArgumentException parseFailure(final CharSequence text, final Exception cause) {
            String msg = "Cannot parse \"" + text + "\" with pattern '" + displayName + "'";

            if (cause instanceof DateTimeParseException && ((DateTimeParseException) cause).getErrorIndex() >= 0) {
                msg += " at index " + ((DateTimeParseException) cause).getErrorIndex();
            }

            return new IllegalArgumentException(msg + ": " + cause.getMessage(), cause);
        }

        /**
         * Creates a {@code DTF} formatter for the specified date/time pattern.
         *
         * <p>The pattern syntax is that of {@link DateTimeFormatter#ofPattern(String)}, and the returned
         * instance formats/parses with {@code Locale.US} for stable {@code EEE}/{@code MMM}/{@code a} text.
         * Instances are immutable and thread-safe, so formatters for frequently-used patterns should be
         * created once and reused.</p>
         *
         * <p>All format and parse operations are backed by the pattern's {@link DateTimeFormatter}
         * with {@link ResolverStyle#STRICT} resolution: invalid values (Feb 30, 24:00) are rejected,
         * {@code yyyy} means year-of-era (CE is assumed only when the pattern has no proleptic-year
         * field &mdash; use {@code uuuu} for a signed ISO year), and offset/zone combinations are
         * validated. Parsers that produce an instant reject DST gaps and ambiguous overlaps unless an
         * offset in the text disambiguates the overlap.</p>
         *
         * <p>Note: purely numeric text is parsed according to the pattern like any other input; it is
         * <i>not</i> treated as epoch milliseconds (use {@link Instant#ofEpochMilli(long)} or the
         * {@code Dates.create*} methods for epoch input).</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF dtf = DTF.of("MM/dd/yyyy HH:mm");
         * dtf.format(LocalDateTime.of(2023, 12, 25, 15, 30));   // returns "12/25/2023 15:30"
         * dtf.parseToLocalDateTime("12/25/2023 15:30");         // returns 2023-12-25T15:30
         *
         * assert DTF.of("uuuu-MM-dd") == DTF.LOCAL_DATE;        // returns true (a predefined pattern is reused)
         * DTF.of("");                                           // throws IllegalArgumentException
         * DTF.of((String) null);                                // throws IllegalArgumentException
         * DTF.of("not-a-pattern");                              // throws IllegalArgumentException (unknown pattern letter)
         * }</pre>
         *
         * <p>Quoted text has no hidden semantics. In particular, a quoted {@code 'Z'} or {@code 'GMT'}
         * is only a literal in a formatter created here. Use {@link #ISO_8601_DATE_TIME},
         * {@link #ISO_8601_TIMESTAMP}, or {@link #HTTP_DATE} when fixed UTC/GMT conversion is required.</p>
         *
         * @param pattern the date/time pattern as defined by {@link DateTimeFormatter#ofPattern(String)}
         * @return a {@code DTF} instance backed by exactly the supplied pattern. A predefined constant
         *         may be reused only when its pattern and zone semantics are identical.
         * @throws IllegalArgumentException if {@code pattern} is {@code null} or empty, or is not a valid date/time
         *         pattern.
         * @see DateTimeFormatter#ofPattern(String)
         */
        public static DTF of(final String pattern) throws IllegalArgumentException {
            N.checkArgNotEmpty(pattern, cs.pattern);

            // Reuse only predefined formatters whose pattern and zone semantics are identical. Fixed
            // UTC/GMT behavior is intentionally not inferred from quoted pattern literals.
            switch (pattern) {
                case PROLEPTIC_LOCAL_DATE_FORMAT:
                    return LOCAL_DATE;
                case Dates.LOCAL_TIME_FORMAT:
                    return LOCAL_TIME;
                case PROLEPTIC_LOCAL_DATE_TIME_FORMAT:
                    return LOCAL_DATE_TIME;
                case PROLEPTIC_ISO_LOCAL_DATE_TIME_FORMAT:
                    return ISO_LOCAL_DATE_TIME;
                case PROLEPTIC_ISO_OFFSET_DATE_TIME_FORMAT:
                    return ISO_OFFSET_DATE_TIME;
                case PROLEPTIC_ISO_OFFSET_TIMESTAMP_FORMAT:
                    return ISO_OFFSET_TIMESTAMP;
                case PROLEPTIC_ISO_ZONED_DATE_TIME_FORMAT:
                    return ISO_ZONED_DATE_TIME;
                default:
                    return cached(pattern, Locale.US);
            }
        }

        /**
         * Returns the instance for {@code (pattern, locale)}, shared with earlier callers while the cache
         * has room for it and freshly built once past the bound. Instances are immutable, so sharing one is
         * always safe; the cache exists because building a {@link DateTimeFormatter} dominates the cost of a
         * short parse.
         */
        private static DTF cached(final String pattern, final Locale locale) {
            final DateFormatKey key = new DateFormatKey(pattern, locale);
            DTF result = patternCache.get(key);

            if (result == null) {
                result = new DTF(pattern, locale, false, false);

                // Entries are never removed, so a full cache stays full: skip the monitor entirely once
                // it saturates, rather than serializing every miss on it (see admitDateFormatQueue).
                if (patternCache.size() >= MAX_CACHED_PATTERNS) {
                    return result;
                }

                // Admission must be atomic with the bound check: sizing and inserting separately let
                // concurrent callers push the cache past MAX_CACHED_PATTERNS.
                synchronized (patternCache) {
                    final DTF existing = patternCache.get(key);

                    if (existing != null) {
                        result = existing;
                    } else if (patternCache.size() < MAX_CACHED_PATTERNS) {
                        patternCache.put(key, result);
                    }
                }
            }

            return result;
        }

        /**
         * Creates a formatter for a custom {@link DateTimeFormatter} pattern using an explicit locale.
         * This is the locale-aware counterpart to {@link #of(String)}; parsing and formatting remain
         * strict and the returned instance is immutable and thread-safe. Fixed UTC/GMT semantics are
         * not inferred from quoted literals.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * LocalDate date = LocalDate.of(2025, 1, 15);
         * DTF.of("dd MMM yyyy", Locale.US).format(date);                           // returns "15 Jan 2025"
         * DTF.of("dd MMM yyyy", Locale.GERMAN).format(date);                       // returns "15 Jan. 2025"
         * DTF.of("dd MMM yyyy", Locale.GERMAN).parseToLocalDate("15 Jan. 2025");   // returns 2025-01-15
         *
         * assert DTF.of("uuuu-MM-dd", Locale.US) == DTF.LOCAL_DATE;   // returns true (Locale.US delegates to of(String))
         * DTF.of("dd MMM yyyy", (Locale) null);                       // throws IllegalArgumentException
         * DTF.of(null, Locale.GERMAN);                                // throws IllegalArgumentException
         * }</pre>
         *
         * @param pattern the non-empty {@code DateTimeFormatter} pattern.
         * @param locale the locale for textual fields such as {@code MMM}, {@code EEE}, and {@code a};
         *        must not be {@code null}.
         * @return a locale-aware formatter.
         * @throws IllegalArgumentException if {@code pattern} is null/empty, {@code locale} is null, or
         *         the pattern is invalid.
         * @see #of(String)
         * @see DateTimeFormatter#ofPattern(String, Locale)
         */
        public static DTF of(final String pattern, final Locale locale) throws IllegalArgumentException {
            N.checkArgNotEmpty(pattern, cs.pattern);
            N.checkArgNotNull(locale, cs.locale);

            return Locale.US.equals(locale) ? of(pattern) : cached(pattern, locale);
        }

        /**
         * @throws IllegalArgumentException if {@code appendable} is {@code null}.
         * @throws UncheckedIOException if appending the text fails.
         */
        private static void appendFormatted(final Appendable appendable, final String str) throws IllegalArgumentException, UncheckedIOException {
            N.checkArgNotNull(appendable, cs.appendable);

            try {
                appendable.append(str);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /**
         * Formats the provided {@code java.util.Date} instance into a string representation using this formatter's pattern.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_8601_DATE_TIME.format(new java.util.Date(0L));               // returns "1970-01-01T00:00:00Z" (UTC 'Z' format)
         * DTF.ISO_8601_DATE_TIME.format(new java.util.Date(1736937045000L));   // returns "2025-01-15T10:30:45Z"
         *
         * DTF.ISO_8601_DATE_TIME.format((java.util.Date) null);                // returns null
         * }</pre>
         *
         * @param date the {@code java.util.Date} instance to format; may be {@code null}.
         * @return a string representation of the provided date, or {@code null} if {@code date} is {@code null}.
         * @throws IllegalArgumentException if this is {@link #HTTP_DATE} and the instant's GMT year is
         *         outside Common Era 0001 through 9999, or if this is not a fixed UTC/GMT formatter and the
         *         live default time zone carries custom rules no {@link ZoneId} can represent (the zone is
         *         converted whatever fields the pattern has).
         * @throws DateTimeException if the formatter requires an unavailable temporal field or cannot represent its value
         * @see DateTimeFormatter#format(TemporalAccessor)
         */
        @MayReturnNull
        public String format(final java.util.Date date) throws IllegalArgumentException, DateTimeException {
            if (date == null) {
                return null;
            }

            // Fixed-zone UTC/HTTP formatters carry an override; other patterns render the instant in
            // the live default zone (see the class-level default-zone note).
            // java.sql.Date and java.sql.Time deliberately throw from toInstant(); their getTime()
            // value still identifies the instant to format. Timestamp needs its override to retain nanos.
            final Instant instant = date instanceof Timestamp ? ((Timestamp) date).toInstant() : Instant.ofEpochMilli(date.getTime());

            if (httpDateFormat) {
                checkHttpDateYear(instant);
            }

            final ZoneId displayZone = utcZFormat ? UTC_ZONE_ID : (httpDateFormat ? GMT_ZONE_ID : toZoneId(TimeZone.getDefault()));
            return dateTimeFormatter.format(ZonedDateTime.ofInstant(instant, displayZone));
        }

        /**
         * Formats the provided java.util.Calendar instance into a string representation.
         *
         * <p>The instant is rendered in the calendar's own time zone, falling back to the live default
         * zone for the rare {@code Calendar} implementation whose {@link Calendar#getTimeZone()} returns
         * {@code null}. {@link #ISO_8601_DATE_TIME}, {@link #ISO_8601_TIMESTAMP} and {@link #HTTP_DATE}
         * override that with their own fixed UTC/GMT zone.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Calendar cal = Dates.createCalendar(0L);
         * DTF.ISO_8601_DATE_TIME.format(cal);                                    // returns "1970-01-01T00:00:00Z"
         * DTF.ISO_8601_DATE_TIME.format(Dates.createCalendar(1736937045000L));   // returns "2025-01-15T10:30:45Z"
         *
         * DTF.LOCAL_DATE_TIME.format((java.util.Calendar) null);                 // returns null
         * }</pre>
         *
         * @param calendar the {@code java.util.Calendar} instance to format; may be {@code null}.
         * @return a string representation of the provided calendar, or {@code null} if {@code calendar} is {@code null}.
         * @throws IllegalArgumentException if {@code calendar} is non-lenient and contains invalid fields,
         *         if this is {@link #HTTP_DATE} and the instant's GMT year is
         *         outside Common Era 0001 through 9999, or if this is not a fixed UTC/GMT formatter and the
         *         calendar's own time zone carries custom rules no {@link ZoneId} can represent (the zone
         *         is converted whatever fields the pattern has).
         * @throws DateTimeException if the formatter requires an unavailable temporal field or cannot represent its value
         * @see DateTimeFormatter#format(TemporalAccessor)
         */
        @MayReturnNull
        public String format(final java.util.Calendar calendar) throws IllegalArgumentException, DateTimeException {
            if (calendar == null) {
                return null;
            }

            // Render the instant in the calendar's own zone (fixed-zone UTC/HTTP formatters override it).
            final Instant instant = calendar.toInstant();

            if (httpDateFormat) {
                checkHttpDateYear(instant);
            }

            final ZoneId displayZone;

            if (utcZFormat) {
                displayZone = UTC_ZONE_ID;
            } else if (httpDateFormat) {
                displayZone = GMT_ZONE_ID;
            } else {
                // Read the zone once - a Calendar implementation may answer differently on a second call -
                // and fall back to the live default zone for the rare one whose getTimeZone() returns null,
                // which is what the class-level contract promises and every other Calendar operation does.
                final TimeZone calendarZone = calendar.getTimeZone();
                displayZone = toZoneId(calendarZone == null ? TimeZone.getDefault() : calendarZone);
            }

            return dateTimeFormatter.format(ZonedDateTime.ofInstant(instant, displayZone));
        }

        /**
         * Formats the provided TemporalAccessor instance into a string representation.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.LOCAL_DATE_TIME.format(LocalDateTime.of(2023, 12, 25, 14, 30, 45));   // returns "2023-12-25 14:30:45"
         * DTF.LOCAL_DATE.format(LocalDate.of(2023, 12, 25));                        // returns "2023-12-25"
         *
         * DTF.LOCAL_DATE_TIME.format((TemporalAccessor) null);                      // returns null
         *
         * // Note: the temporal must supply every field the pattern needs; e.g. a bare LocalDate cannot be
         * // formatted with a HH:mm:ss pattern and throws DateTimeException. For the UTC 'Z' formats
         * // (ISO_8601_DATE_TIME, ISO_8601_TIMESTAMP) the temporal must be instant-bearing (Instant,
         * // OffsetDateTime, ZonedDateTime) &mdash; zone-less values are rejected with
         * // IllegalArgumentException rather than mislabeled as UTC.
         * }</pre>
         *
         * @param temporal the {@code TemporalAccessor} instance to format; may be {@code null}.
         * @return a string representation of the provided temporal, or {@code null} if {@code temporal} is {@code null}.
         * @throws IllegalArgumentException if this formatter has fixed UTC/GMT semantics and
         *         {@code temporal} is not instant-bearing, or if this is {@link #HTTP_DATE} and the
         *         instant's GMT year is outside Common Era 0001 through 9999.
         * @throws DateTimeException if the formatter requires an unavailable temporal field or cannot represent its value
         * @see DateTimeFormatter#format(TemporalAccessor)
         */
        @MayReturnNull
        public String format(final TemporalAccessor temporal) throws IllegalArgumentException, DateTimeException {
            if (temporal == null) {
                return null;
            }

            checkInstantBearingForFixedZoneFormat(temporal);

            if (httpDateFormat) {
                checkHttpDateYear(Instant.from(temporal));
            }

            return dateTimeFormatter.format(temporal);
        }

        /**
         * Fixed UTC/GMT formatters append a literal zone claim. Formatting a zone-less temporal (e.g.
         * {@link LocalDateTime}) would mislabel local wall-clock fields as an absolute instant, so the
         * caller must convert explicitly (e.g. {@code atZone(...)}).
         */
        private void checkInstantBearingForFixedZoneFormat(final TemporalAccessor temporal) {
            if ((utcZFormat || httpDateFormat) && !temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
                throw new IllegalArgumentException("Pattern '" + displayName + "' has fixed " + (httpDateFormat ? "GMT" : "UTC")
                        + " semantics and can only format instant-bearing temporals"
                        + " (Instant, OffsetDateTime, ZonedDateTime); convert zone-less values first (e.g. localDateTime.atZone(zone)). Got: "
                        + temporal.getClass().getName());
            }
        }

        /**
         * Formats the provided java.util.Date instance into a string representation and appends it to the provided Appendable.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * StringBuilder sb = new StringBuilder();
         * DTF.ISO_8601_DATE_TIME.formatTo(new java.util.Date(0L), sb);
         * sb.toString();                                  // returns "1970-01-01T00:00:00Z"
         *
         * StringBuilder nb = new StringBuilder();
         * DTF.ISO_8601_DATE_TIME.formatTo((java.util.Date) null, nb);
         * nb.toString();                                  // returns "null" (literal appended for null input)
         * }</pre>
         *
         * @param date the {@code java.util.Date} instance to format; if {@code null}, the string {@code "null"} is appended.
         * @param appendable the Appendable to which the formatted string will be appended; must not be {@code null}.
         * @throws IllegalArgumentException if {@code appendable} is {@code null}, or this is {@link #HTTP_DATE} and the instant's GMT year is outside
         *         Common Era 0001 through 9999, or if the zone the value is rendered in cannot be expressed as a {@link ZoneId} (custom
         *         daylight-saving rules, or a fixed offset that is sub-second or beyond +/-18:00).
         * @throws DateTimeException if the formatter requires an unavailable temporal field or cannot represent its value
         * @throws UncheckedIOException if writing the formatted date/time text or null marker to {@code appendable} fails
         * @see Dates#formatTo(java.util.Date, String, Appendable)
         */
        public void formatTo(final java.util.Date date, final Appendable appendable) throws IllegalArgumentException, DateTimeException, UncheckedIOException {
            N.checkArgNotNull(appendable, cs.appendable);

            if (date == null) {
                formatToForNull(appendable);
                return;
            }

            appendFormatted(appendable, format(date));
        }

        /**
         * Formats the provided java.util.Calendar instance into a string representation and appends it to the provided Appendable.
         * The zone rules are those of {@link #format(java.util.Calendar)}, which this delegates to.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * StringBuilder sb = new StringBuilder();
         * DTF.ISO_8601_DATE_TIME.formatTo(Dates.createCalendar(0L), sb);
         * sb.toString();                                  // returns "1970-01-01T00:00:00Z"
         *
         * StringBuilder nb = new StringBuilder();
         * DTF.ISO_8601_DATE_TIME.formatTo((java.util.Calendar) null, nb);
         * nb.toString();                                  // returns "null" (literal appended for null input)
         * }</pre>
         *
         * @param calendar the {@code java.util.Calendar} instance to format; if {@code null}, the string {@code "null"} is appended.
         * @param appendable the Appendable to which the formatted string will be appended; must not be {@code null}.
         * @throws IllegalArgumentException if {@code appendable} is null, {@code calendar} is non-lenient and contains invalid fields,
         *         or this is {@link #HTTP_DATE} and the instant's GMT year is outside
         *         Common Era 0001 through 9999, or if the zone the value is rendered in cannot be expressed as a {@link ZoneId} (custom
         *         daylight-saving rules, or a fixed offset that is sub-second or beyond +/-18:00).
         * @throws DateTimeException if the formatter requires an unavailable temporal field or cannot represent its value
         * @throws UncheckedIOException if writing the formatted date/time text or null marker to {@code appendable} fails
         * @see #format(java.util.Calendar)
         * @see Dates#formatTo(java.util.Calendar, String, Appendable)
         */
        public void formatTo(final java.util.Calendar calendar, final Appendable appendable)
                throws IllegalArgumentException, DateTimeException, UncheckedIOException {
            N.checkArgNotNull(appendable, cs.appendable);

            if (calendar == null) {
                formatToForNull(appendable);
                return;
            }

            appendFormatted(appendable, format(calendar));
        }

        /**
         * Formats the provided TemporalAccessor instance into a string representation and appends it to the provided Appendable.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * StringBuilder sb = new StringBuilder();
         * DTF.LOCAL_DATE_TIME.formatTo(LocalDateTime.of(2023, 12, 25, 14, 30, 45), sb);
         * sb.toString();                                  // returns "2023-12-25 14:30:45"
         *
         * StringBuilder nb = new StringBuilder();
         * DTF.LOCAL_DATE.formatTo((TemporalAccessor) null, nb);
         * nb.toString();                                  // returns "null" (literal appended for null input)
         * }</pre>
         *
         * @param temporal the {@code TemporalAccessor} instance to format; if {@code null}, the string {@code "null"} is appended.
         * @param appendable the Appendable to which the formatted string will be appended; must not be {@code null}.
         * @throws IllegalArgumentException if {@code appendable} is {@code null}, or if this formatter
         *         has fixed UTC/GMT semantics and {@code temporal} is not instant-bearing, or when this is
         *         {@link #HTTP_DATE} and the instant's GMT year is outside Common Era 0001 through 9999.
         * @throws DateTimeException if the formatter requires an unavailable temporal field or cannot represent its value
         * @throws UncheckedIOException if the appendable throws an {@code IOException}.
         * @see DateTimeFormatter#formatTo(TemporalAccessor, Appendable)
         */
        public void formatTo(final TemporalAccessor temporal, final Appendable appendable)
                throws IllegalArgumentException, DateTimeException, UncheckedIOException {
            N.checkArgNotNull(appendable, cs.appendable);

            if (temporal == null) {
                formatToForNull(appendable);
                return;
            }

            appendFormatted(appendable, format(temporal));
        }

        /**
         * Parses the provided CharSequence into a LocalDate instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.LOCAL_DATE.parseToLocalDate("2023-12-25");          // returns LocalDate 2023-12-25
         *
         * DTF.LOCAL_DATE.parseToLocalDate("");                    // throws IllegalArgumentException
         * DTF.LOCAL_DATE.parseToLocalDate("null");                // returns null (the formatTo null-token)
         * DTF.LOCAL_DATE.parseToLocalDate((CharSequence) null);   // returns null
         * }</pre>
         *
         * <p>Parsing is backed by this formatter's {@link DateTimeFormatter} with
         * {@link ResolverStyle#STRICT} resolution, so invalid values (e.g. {@code 2023-02-30}) are
         * rejected rather than adjusted. The result is the wall-clock date exactly as written in the
         * text; for the UTC {@code 'Z'} formats ({@code ISO_8601_DATE_TIME}, {@code ISO_8601_TIMESTAMP})
         * that is the UTC value, not the value shifted into the default time zone.</p>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @return a LocalDate instance representing the parsed date, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed with this
         *         formatter's pattern (including a purely numeric value, which is <i>not</i> treated as
         *         epoch milliseconds &mdash; use {@link Dates#parseEpochMillis(String)} for epoch-millisecond text).
         * @see LocalDate#from(TemporalAccessor)
         */
        @MayReturnNull
        public LocalDate parseToLocalDate(final CharSequence text) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            return parseWith(text, LocalDate::from);
        }

        /**
         * Parses the provided CharSequence into a LocalTime instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.LOCAL_TIME.parseToLocalTime("14:30:45");            // returns LocalTime 14:30:45
         *
         * DTF.LOCAL_TIME.parseToLocalTime("");                    // throws IllegalArgumentException
         * DTF.LOCAL_TIME.parseToLocalTime((CharSequence) null);   // returns null
         * }</pre>
         *
         * <p>Parsing is backed by this formatter's {@link DateTimeFormatter} with
         * {@link ResolverStyle#STRICT} resolution, so invalid values (e.g. {@code 24:00:00}) are
         * rejected rather than adjusted. The result is the wall-clock time exactly as written in the
         * text; input without time fields is rejected rather than synthesized as midnight.
         * For the UTC {@code 'Z'} formats ({@code ISO_8601_DATE_TIME}, {@code ISO_8601_TIMESTAMP}) the
         * result is the UTC value, not the value shifted into the default time zone.</p>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @return a LocalTime instance representing the parsed time, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed with this
         *         formatter's pattern (including a purely numeric value, which is <i>not</i> treated as
         *         epoch milliseconds &mdash; use {@link Dates#parseEpochMillis(String)} for epoch-millisecond text).
         * @see LocalTime#from(TemporalAccessor)
         */
        @MayReturnNull
        public LocalTime parseToLocalTime(final CharSequence text) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            return parseWith(text, LocalTime::from);
        }

        /**
         * Parses the provided CharSequence into a LocalDateTime instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.LOCAL_DATE_TIME.parseToLocalDateTime("2023-12-25 14:30:45");   // returns LocalDateTime 2023-12-25T14:30:45
         *
         * DTF.LOCAL_DATE_TIME.parseToLocalDateTime("");                      // throws IllegalArgumentException
         * DTF.LOCAL_DATE_TIME.parseToLocalDateTime((CharSequence) null);     // returns null
         * }</pre>
         *
         * <p>Parsing is backed by this formatter's {@link DateTimeFormatter} with
         * {@link ResolverStyle#STRICT} resolution, so invalid values (e.g. {@code 2023-12-25 24:00:00})
         * are rejected rather than adjusted. The result is the wall-clock date-time exactly as written;
         * input without both complete date and time fields is rejected rather than completed with
         * synthetic midnight fields.
         * For the UTC {@code 'Z'} formats ({@code ISO_8601_DATE_TIME}, {@code ISO_8601_TIMESTAMP}) the
         * result is the UTC value, not the value shifted into the default time zone.</p>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @return a LocalDateTime instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed with this
         *         formatter's pattern (including a purely numeric value, which is <i>not</i> treated as
         *         epoch milliseconds &mdash; use {@link Dates#parseEpochMillis(String)} for epoch-millisecond text).
         * @see LocalDateTime#from(TemporalAccessor)
         */
        @MayReturnNull
        public LocalDateTime parseToLocalDateTime(final CharSequence text) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            return parseWith(text, LocalDateTime::from);
        }

        /**
         * Parses the provided CharSequence into an OffsetDateTime instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime("2023-12-25T14:30:45+05:30");
         *                                                  // returns OffsetDateTime 2023-12-25T14:30:45+05:30
         *
         * DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime("");                    // throws IllegalArgumentException
         * DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime((CharSequence) null);   // returns null
         * }</pre>
         *
         * <p>Parsing is backed by this formatter's {@link DateTimeFormatter} with
         * {@link ResolverStyle#STRICT} resolution. An offset written in the text is always preserved
         * (and validated against a bracketed zone when both are present); the fixed UTC/GMT formatters
         * ({@code ISO_8601_DATE_TIME}, {@code ISO_8601_TIMESTAMP}, and {@code HTTP_DATE}) produce the
         * {@link ZoneOffset#UTC} offset (rendered as {@code Z}). Any other zone-less pattern is interpreted
         * in the live default time zone and carries that zone's offset.</p>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @return an OffsetDateTime instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed with this
         *         formatter's pattern (including a purely numeric value, which is <i>not</i> treated as
         *         epoch milliseconds &mdash; use {@link Dates#parseEpochMillis(String)} for epoch-millisecond text).
         * @see OffsetDateTime#from(TemporalAccessor)
         */
        @MayReturnNull
        public OffsetDateTime parseToOffsetDateTime(final CharSequence text) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            return parseZoned(text, null).toOffsetDateTime();
        }

        /**
         * Parses the provided CharSequence into a ZonedDateTime instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime("2023-12-25T14:30:45+05:30[Asia/Kolkata]");
         *                                                  // returns ZonedDateTime 2023-12-25T14:30:45+05:30[Asia/Kolkata]
         *
         * DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime("");                    // throws IllegalArgumentException
         * DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime((CharSequence) null);   // returns null
         * }</pre>
         *
         * <p>Parsing is backed by this formatter's {@link DateTimeFormatter} with
         * {@link ResolverStyle#STRICT} resolution. A zone or offset written in the text always wins,
         * and an offset inconsistent with its bracketed zone is rejected. The UTC {@code 'Z'} formats
         * carry UTC and {@link #HTTP_DATE} carries GMT; any other zone-less pattern is interpreted in
         * the live default time zone.</p>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @return a ZonedDateTime instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed with this
         *         formatter's pattern (including a purely numeric value, which is <i>not</i> treated as
         *         epoch milliseconds &mdash; use {@link Dates#parseEpochMillis(String)} for epoch-millisecond text).
         * @see ZonedDateTime#from(TemporalAccessor)
         */
        @MayReturnNull
        public ZonedDateTime parseToZonedDateTime(final CharSequence text) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            return parseZoned(text, null);
        }

        /**
         * Parses the provided CharSequence into an Instant instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_8601_DATE_TIME.parseToInstant("2023-12-25T14:30:45Z").toEpochMilli();   // returns 1703514645000
         *
         * DTF.ISO_8601_DATE_TIME.parseToInstant("");                                      // throws IllegalArgumentException
         * DTF.ISO_8601_DATE_TIME.parseToInstant((CharSequence) null);                     // returns null
         * }</pre>
         *
         * <p>Parsing is backed by this formatter's {@link DateTimeFormatter} with
         * {@link ResolverStyle#STRICT} resolution. A zone or offset written in the text always wins;
         * zone-less values are interpreted in the live default time zone (UTC/GMT for the fixed-zone
         * predefined formatters). Time-only input is rejected because no date fields exist.</p>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @return an Instant instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed with this
         *         formatter's pattern (including a purely numeric value, which is <i>not</i> treated as
         *         epoch milliseconds &mdash; use {@link Dates#parseEpochMillis(String)} for epoch-millisecond text).
         * @see Instant#from(TemporalAccessor)
         */
        @MayReturnNull
        public Instant parseToInstant(final CharSequence text) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            return parseZoned(text, null).toInstant();
        }

        /**
         * Parses the provided CharSequence into an OffsetDateTime, interpreting zone-less text in the
         * supplied zone instead of the live default. An offset written in the text always wins.
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @param tz the zone to interpret zone-less text in; if {@code null}, the live default zone is used.
         * @return an OffsetDateTime instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text cannot be parsed with this formatter's pattern, or
         *         {@code tz} conflicts with a fixed UTC/GMT format.
         */
        @MayReturnNull
        public OffsetDateTime parseToOffsetDateTime(final CharSequence text, final TimeZone tz) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            return parseZoned(text, tz).toOffsetDateTime();
        }

        /**
         * Parses the provided CharSequence into a ZonedDateTime, interpreting zone-less text in the
         * supplied zone instead of the live default. A zone or offset written in the text always wins.
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @param tz the zone to interpret zone-less text in; if {@code null}, the live default zone is used.
         * @return a ZonedDateTime instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text cannot be parsed with this formatter's pattern, or
         *         {@code tz} conflicts with a fixed UTC/GMT format.
         */
        @MayReturnNull
        public ZonedDateTime parseToZonedDateTime(final CharSequence text, final TimeZone tz) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            return parseZoned(text, tz);
        }

        /**
         * Parses the provided CharSequence into an Instant, interpreting zone-less text in the supplied
         * zone instead of the live default. A zone or offset written in the text always wins.
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @param tz the zone to interpret zone-less text in; if {@code null}, the live default zone is used.
         * @return an Instant instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text cannot be parsed with this formatter's pattern, or
         *         {@code tz} conflicts with a fixed UTC/GMT format.
         */
        @MayReturnNull
        public Instant parseToInstant(final CharSequence text, final TimeZone tz) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            return parseZoned(text, tz).toInstant();
        }

        /**
         * Parses the provided CharSequence into a {@code java.util.Date} instance using this formatter's pattern.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_8601_DATE_TIME.parseToJUDate("2023-12-25T14:30:45Z").getTime();   // returns 1703514645000
         *
         * DTF.ISO_8601_DATE_TIME.parseToJUDate("");                                 // throws IllegalArgumentException
         * DTF.ISO_8601_DATE_TIME.parseToJUDate((CharSequence) null);                // returns null
         * }</pre>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @return a {@code java.util.Date} instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed with this
         *         formatter's pattern.
         * @see #parseToJUDate(CharSequence, TimeZone)
         */
        @MayReturnNull
        public java.util.Date parseToJUDate(final CharSequence text) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            return java.util.Date.from(parseZoned(text, null).toInstant());
        }

        /**
         * Parses the provided CharSequence into a java.util.Date instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * TimeZone utc = TimeZone.getTimeZone("UTC");
         * DTF.LOCAL_DATE_TIME.parseToJUDate("2023-12-25 14:30:45", utc).getTime();   // returns 1703514645000
         *
         * DTF.LOCAL_DATE_TIME.parseToJUDate("", utc);                                // throws IllegalArgumentException
         * DTF.LOCAL_DATE_TIME.parseToJUDate((CharSequence) null, utc);               // returns null
         * }</pre>
         *
         * <p>Note: a zone or offset written in the text always wins over {@code tz}; {@code tz} is used
         * only to interpret zone-less text. For a fixed UTC/GMT predefined formatter, {@code tz} must
         * be {@code null} or UTC-equivalent.</p>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @param tz the time zone to interpret zone-less text in; if {@code null}, the default time zone is used.
         * @return a {@code java.util.Date} instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed with this
         *         formatter's pattern, or if a fixed UTC/GMT formatter is combined with a non-UTC-equivalent zone.
         * @see #parseToJUDate(CharSequence)
         */
        @MayReturnNull
        public java.util.Date parseToJUDate(final CharSequence text, final TimeZone tz) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            return java.util.Date.from(parseZoned(text, tz).toInstant());
        }

        /**
         * Parses the provided CharSequence into a {@code java.sql.Date}. The text is resolved to an
         * instant (a zone or offset written in the text takes precedence; zone-less text is interpreted in
         * the live default zone — or the fixed UTC/GMT zone for those predefined formatters), and that
         * instant's epoch milliseconds are retained unchanged. No truncation to midnight is performed.
         * To create a conventional JDBC date from the textual civil fields, use
         * {@link #parseToLocalDate(CharSequence)} with {@link java.sql.Date#valueOf(LocalDate)} instead.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.LOCAL_DATE.parseToDate("2023-12-25");                           // returns 2023-12-25 (midnight in the default zone)
         * assert DTF.LOCAL_DATE_TIME.parseToDate("2023-12-25 14:30:45").getTime()
         *     == DTF.LOCAL_DATE_TIME.parseToJUDate("2023-12-25 14:30:45").getTime();   // returns true
         *
         * DTF.LOCAL_DATE.parseToDate("");                                     // throws IllegalArgumentException
         * DTF.LOCAL_DATE.parseToDate((CharSequence) null);                    // returns null
         * }</pre>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @return a {@code java.sql.Date} instance representing the parsed date, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text is empty or cannot be parsed with
         *         this formatter's pattern.
         * @see #parseToDate(CharSequence, TimeZone)
         * @see #parseToLocalDate(CharSequence)
         */
        @MayReturnNull
        public java.sql.Date parseToDate(final CharSequence text) throws IllegalArgumentException {
            return parseToDate(text, null);
        }

        /**
         * Parses the provided CharSequence into a {@code java.sql.Date} using {@code tz} as the fallback
         * for zone-less text. A zone or offset written in the text always takes precedence. The resolved
         * instant's epoch milliseconds are retained unchanged. If {@code tz} is {@code null}, the live
         * default zone is used. For a fixed UTC/GMT predefined formatter, {@code tz} must be {@code null}
         * or UTC-equivalent.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * TimeZone utc = TimeZone.getTimeZone("UTC");
         * DTF.LOCAL_DATE.parseToDate("2023-12-25", utc).getTime();              // returns 1703462400000 (midnight UTC)
         *
         * DTF.LOCAL_DATE.parseToDate("", utc);                                  // throws IllegalArgumentException
         * DTF.LOCAL_DATE.parseToDate((CharSequence) null, utc);                 // returns null
         * }</pre>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @param tz the fallback zone for interpreting zone-less text;
         *        if {@code null}, the live default zone is used.
         * @return a {@code java.sql.Date} instance representing the parsed date, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text is empty, cannot be parsed with
         *         this formatter's pattern, or the zone conflicts with a fixed UTC/GMT formatter.
         * @see #parseToDate(CharSequence)
         * @see #parseToLocalDate(CharSequence)
         */
        @MayReturnNull
        public java.sql.Date parseToDate(final CharSequence text, final TimeZone tz) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            return Dates.createDate(parseZoned(text, tz).toInstant().toEpochMilli());
        }

        /**
         * Parses the provided CharSequence into a {@code java.sql.Time}. The text is resolved to an
         * instant (a zone or offset written in the text takes precedence; zone-less text is interpreted in
         * the live default zone — or the fixed UTC/GMT zone for those predefined formatters), and that
         * instant's epoch milliseconds are retained unchanged. Dated input is not rebased to 1970-01-01;
         * time-only text (e.g. via {@link #LOCAL_TIME}) is anchored to 1970-01-01 in the authoritative
         * zone. A formatter carrying only part of a date, or no date and no resolvable clock time, is
         * rejected rather than having missing fields synthesized from the epoch date. To create a
         * conventional JDBC time from the textual civil fields, use
         * {@link #parseToLocalTime(CharSequence)} with {@link java.sql.Time#valueOf(LocalTime)} instead.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.LOCAL_TIME.parseToTime("14:30:45");                                 // anchored to 1970-01-01 in the default zone
         * DTF.ISO_8601_DATE_TIME.parseToTime("2023-12-25T14:30:45Z").getTime();   // returns 1703514645000
         *
         * DTF.LOCAL_TIME.parseToTime("");                                     // throws IllegalArgumentException
         * DTF.LOCAL_TIME.parseToTime((CharSequence) null);                    // returns null
         * }</pre>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @return a {@code java.sql.Time} instance representing the parsed time, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text is empty, contains only a partial
         *         date, lacks a resolvable clock time when no date is present, or cannot be parsed with
         *         this formatter's pattern.
         * @see #parseToTime(CharSequence, TimeZone)
         * @see #parseToLocalTime(CharSequence)
         */
        @MayReturnNull
        public Time parseToTime(final CharSequence text) throws IllegalArgumentException {
            return parseToTime(text, null);
        }

        /**
         * Parses the provided CharSequence into a {@code java.sql.Time} using {@code tz} as the fallback
         * for zone-less and time-only text. A zone or offset written in the text always takes precedence
         * (also as the anchoring zone for time-only text). The resolved instant's epoch milliseconds are
         * retained unchanged. If {@code tz} is {@code null}, the live default zone is used. For a fixed
         * UTC/GMT predefined formatter, {@code tz} must be {@code null} or UTC-equivalent. A formatter
         * containing only a partial date, or no date and no resolvable clock time, is rejected.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * TimeZone utc = TimeZone.getTimeZone("UTC");
         * DTF.LOCAL_TIME.parseToTime("14:30:45", utc).getTime();                // returns 52245000 (14:30:45 on 1970-01-01 in UTC)
         *
         * DTF.LOCAL_TIME.parseToTime("", utc);                                  // throws IllegalArgumentException
         * DTF.LOCAL_TIME.parseToTime((CharSequence) null, utc);                 // returns null
         * }</pre>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @param tz the fallback zone for interpreting zone-less text and anchoring time-only text;
         *        if {@code null}, the live default zone is used.
         * @return a {@code java.sql.Time} instance representing the parsed time, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text is empty, contains only a partial
         *         date, lacks a resolvable clock time when no date is present, cannot be parsed with this
         *         formatter's pattern, or the zone conflicts with a fixed UTC/GMT formatter.
         * @see #parseToTime(CharSequence)
         * @see #parseToLocalTime(CharSequence)
         */
        @MayReturnNull
        public Time parseToTime(final CharSequence text, final TimeZone tz) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            // parseZoned rejects time-only text (no date to resolve an instant on); SQL Time wants exactly
            // the wall time, resolved on the 1970-01-01 epoch date in its written or fallback zone.
            final TemporalAccessor parsed = parseRaw(text);

            if (!parsed.isSupported(ChronoField.EPOCH_DAY)) {
                try {
                    if (containsAnyDatePatternField(format)) {
                        throw new DateTimeException("A partial date cannot be anchored by parseToTime; supply a complete date or use a time-only pattern");
                    }

                    final LocalTime localTime = LocalTime.from(parsed);

                    // A zone or offset written in the text takes precedence as the anchoring zone
                    // (matching legacy parseZoned semantics); otherwise use the supplied/default zone.
                    final ZoneOffset parsedOffset = parsed.isSupported(ChronoField.OFFSET_SECONDS)
                            ? ZoneOffset.ofTotalSeconds(parsed.get(ChronoField.OFFSET_SECONDS))
                            : null;
                    ZoneId anchorZone = parsed.query(TemporalQueries.zone());

                    if (anchorZone == null && parsedOffset != null) {
                        anchorZone = parsedOffset;
                    }

                    if (anchorZone == null) {
                        anchorZone = fallbackZoneId(tz);
                    }

                    final LocalDateTime anchoredDateTime = localTime.atDate(LocalDate.of(1970, 1, 1));
                    final long anchoredMillis = resolveLocalDateTimeStrict(anchoredDateTime, anchorZone, parsedOffset).toInstant().toEpochMilli();
                    return Dates.createTime(anchoredMillis);
                } catch (final DateTimeException | IllegalArgumentException e) {
                    throw parseFailure(text, e);
                }
            }

            return Dates.createTime(parseZoned(text, tz, parsed).toInstant().toEpochMilli());
        }

        /**
         * Parses the provided CharSequence into a java.sql.Timestamp instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_8601_TIMESTAMP.parseToTimestamp("2023-12-25T14:30:45.123Z").getTime();   // returns 1703514645123
         *
         * DTF.ISO_8601_TIMESTAMP.parseToTimestamp("");                                     // throws IllegalArgumentException
         * DTF.ISO_8601_TIMESTAMP.parseToTimestamp((CharSequence) null);                    // returns null
         * }</pre>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @return a {@code java.sql.Timestamp} instance representing the parsed date and time
         *         (sub-millisecond nanoseconds preserved), or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed with this
         *         formatter's pattern.
         * @see #parseToTimestamp(CharSequence, TimeZone)
         */
        @MayReturnNull
        public Timestamp parseToTimestamp(final CharSequence text) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            return Timestamp.from(parseZoned(text, null).toInstant());
        }

        /**
         * Parses the provided CharSequence into a java.sql.Timestamp instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * TimeZone utc = TimeZone.getTimeZone("UTC");
         * DTF.ISO_8601_TIMESTAMP.parseToTimestamp("2023-12-25T14:30:45.123Z", utc).getTime();   // returns 1703514645123
         *
         * DTF.ISO_8601_TIMESTAMP.parseToTimestamp("", utc);                                     // throws IllegalArgumentException
         * DTF.ISO_8601_TIMESTAMP.parseToTimestamp((CharSequence) null, utc);                    // returns null
         * }</pre>
         *
         * <p>Note: a zone or offset written in the text always wins over {@code tz}; {@code tz} is used
         * only to interpret zone-less text. For a fixed UTC/GMT predefined formatter, {@code tz} must
         * be {@code null} or UTC-equivalent.</p>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @param tz the time zone to interpret zone-less text in; if {@code null}, the default time zone is used.
         * @return a {@code java.sql.Timestamp} instance representing the parsed date and time
         *         (sub-millisecond nanoseconds preserved), or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed with this
         *         formatter's pattern, or if a fixed UTC/GMT formatter is combined with a non-UTC-equivalent zone.
         * @see #parseToTimestamp(CharSequence)
         */
        @MayReturnNull
        public Timestamp parseToTimestamp(final CharSequence text, final TimeZone tz) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            return Timestamp.from(parseZoned(text, tz).toInstant());
        }

        /**
         * Parses the provided CharSequence into a proleptic {@link GregorianCalendar} instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Dates.format(DTF.LOCAL_DATE_TIME.parseToCalendar("2023-12-25 14:30:45"), "yyyy-MM-dd HH:mm:ss");
         *                                                  // returns "2023-12-25 14:30:45" (default zone round-trip)
         *
         * DTF.LOCAL_DATE_TIME.parseToCalendar("");                    // throws IllegalArgumentException
         * DTF.LOCAL_DATE_TIME.parseToCalendar((CharSequence) null);   // returns null
         * }</pre>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @return a proleptic {@code GregorianCalendar} representing the parsed date and time, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed with this
         *         formatter's pattern.
         * @see #parseToCalendar(CharSequence, TimeZone)
         */
        @MayReturnNull
        public Calendar parseToCalendar(final CharSequence text) throws IllegalArgumentException {
            return parseToCalendar(text, null);
        }

        /**
         * Parses the provided CharSequence into a proleptic {@link GregorianCalendar} instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * TimeZone utc = TimeZone.getTimeZone("UTC");
         * DTF.LOCAL_DATE_TIME.parseToCalendar("2023-12-25 14:30:45", utc).getTimeInMillis();   // returns 1703514645000
         *
         * DTF.LOCAL_DATE_TIME.parseToCalendar("", utc);                                        // throws IllegalArgumentException
         * DTF.LOCAL_DATE_TIME.parseToCalendar((CharSequence) null, utc);                       // returns null
         * }</pre>
         *
         * <p>A zone or offset written in the text is authoritative and is preserved by the returned
         * calendar. {@code tz} is used for both instant resolution and the result only when the text is
         * zone-less. For a fixed UTC/GMT predefined formatter, {@code tz} must be {@code null} or
         * UTC-equivalent. Week settings ({@code firstDayOfWeek}, {@code minimalDaysInFirstWeek}) come
         * from this formatter's locale, matching {@link Dates#parseToCalendar(String, String, TimeZone, Locale)}
         * for the same locale — they are not forced to the ISO Monday/4 convention.</p>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @param tz the fallback zone for zone-less text; if {@code null}, the live default zone is used.
         * @return a proleptic {@code GregorianCalendar} representing the parsed date and time, or {@code null} if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed with this
         *         formatter's pattern, or if a fixed UTC/GMT formatter is combined with a non-UTC-equivalent zone.
         * @see #parseToCalendar(CharSequence)
         */
        @MayReturnNull
        public Calendar parseToCalendar(final CharSequence text, final TimeZone tz) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            final boolean noZoneSupplied = tz == null;
            final TimeZone effectiveZone = noZoneSupplied ? TimeZone.getDefault() : (TimeZone) tz.clone();
            // A default snapshot is a display choice, never a conflict with a fixed-zone formatter.
            final TimeZone parsingZone = noZoneSupplied && (utcZFormat || httpDateFormat) ? null : effectiveZone;
            final TemporalAccessor parsed = parseRaw(text);
            final ZonedDateTime zdt = parseZoned(text, parsingZone, parsed);

            // Locale week settings match Dates.parseToCalendar(..., locale) for this formatter's locale.
            // A textual zone/offset is preserved; zone-less text keeps the caller-supplied TimeZone
            // (including custom rules/IDs that reduce to a fixed ZoneOffset for java.time resolution).
            final TimeZone resultZone = parsedZone(parsed) != null ? TimeZone.getTimeZone(zdt.getZone()) : effectiveZone;
            return createParsedGregorianCalendar(zdt.toInstant().toEpochMilli(), resultZone, locale);
        }

        /**
         * Parses the provided CharSequence into a TemporalAccessor instance using the underlying DateTimeFormatter.
         * This is a low-level parsing method that returns a TemporalAccessor, which can represent various temporal types
         * depending on the format pattern (e.g., LocalDate, LocalDateTime, ZonedDateTime).
         *
         * <p>This method is typically used internally or when you need direct access to the parsed temporal fields
         * without converting to a specific date/time type. For most use cases, prefer the more specific parse methods
         * like {@link #parseToLocalDateTime(CharSequence)}, {@link #parseToZonedDateTime(CharSequence)}, etc.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Parse to a TemporalAccessor and query specific fields
         * DTF formatter = DTF.LOCAL_DATE_TIME;   // pattern "uuuu-MM-dd HH:mm:ss"
         * TemporalAccessor temporal = formatter.parseToTemporalAccessor("2023-12-25 15:30:45");
         * int year = temporal.get(ChronoField.YEAR);             // returns 2023
         * int month = temporal.get(ChronoField.MONTH_OF_YEAR);   // returns 12
         * int day = temporal.get(ChronoField.DAY_OF_MONTH);      // returns 25
         *
         * // Convert to specific type if needed
         * LocalDateTime ldt = LocalDateTime.from(temporal);
         *
         * formatter.parseToTemporalAccessor("");                    // throws IllegalArgumentException
         * formatter.parseToTemporalAccessor("null");                // returns null (the formatTo null-token)
         * formatter.parseToTemporalAccessor((CharSequence) null);   // returns null
         * }</pre>
         *
         * @param text the CharSequence to parse; may be {@code null}.
         * @return a TemporalAccessor instance representing the parsed date and time, or {@code null}
         *         if {@code text} is {@code null} or the case-insensitive marker {@code "null"}.
         *         For the UTC {@code 'Z'} constants, the accessor carries an override UTC zone
         *         (see {@link DateTimeFormatter#withZone}) not present in the text.
         * @throws IllegalArgumentException if the text is empty, cannot be parsed according to the
         *         format pattern, or contains an offset inconsistent with its zone. The underlying
         *         {@link DateTimeException} is retained as the cause.
         * @see DateTimeFormatter#parse(CharSequence)
         * @see TemporalAccessor
         * @see #parseToLocalDateTime(CharSequence)
         * @see #parseToZonedDateTime(CharSequence)
         */
        @MayReturnNull
        public TemporalAccessor parseToTemporalAccessor(final CharSequence text) throws IllegalArgumentException {
            if (isNullParseInput(text)) {
                return null;
            }

            rejectEmptyDateTime(text);

            return parseRaw(text);
        }

        /**
         * Two formatters are equal when they parse and format identically: same pattern, same locale, and
         * same fixed-zone and fraction-grammar semantics.
         *
         * @param obj the object to compare with
         * @return {@code true} if {@code obj} is an equivalent {@code DTF}
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }

            if (!(obj instanceof DTF)) {
                return false;
            }

            final DTF other = (DTF) obj;

            return utcZFormat == other.utcZFormat && httpDateFormat == other.httpDateFormat && format.equals(other.format)
                    && displayName.equals(other.displayName) && locale.equals(other.locale);
        }

        /**
         * @return a hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            int h = format.hashCode();
            h = 31 * h + displayName.hashCode();
            h = 31 * h + locale.hashCode();
            h = 31 * h + (utcZFormat ? 1 : 0);
            return 31 * h + (httpDateFormat ? 1 : 0);
        }

        /**
         * Returns this formatter's underlying date/time pattern string, or a description of the grammar
         * where that grammar is wider than a single pattern (the auto-detected variable-fraction forms).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.LOCAL_DATE_TIME.toString();      // returns "uuuu-MM-dd HH:mm:ss"
         * DTF.LOCAL_DATE.toString();           // returns "uuuu-MM-dd"
         * DTF.LOCAL_TIME.toString();           // returns "HH:mm:ss"
         * DTF.ISO_8601_TIMESTAMP.toString();   // returns "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'"
         * }</pre>
         *
         * @return the pattern string backing this {@code DTF}, or a description of its grammar.
         */
        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * A concrete, named subclass of {@link Dates} that exposes the full set of inherited
     * static date/time utility methods under the alternative name {@code DateUtil}.
     *
     * <p>It is the sole permitted subtype of the sealed {@code Dates} class and adds no
     * behavior of its own; it exists purely so callers may reference these utilities as
     * {@code DateUtil} (for example, in code bases that already use that name) while sharing
     * a single implementation. This class cannot be instantiated.</p>
     *
     * <p>{@code Dates} is the canonical name: prefer {@code Dates} in new code and reserve this
     * alias for code bases already standardized on the {@code DateUtil} name.</p>
     *
     * @see Dates
     */
    @Beta
    public static final class DateUtil extends Dates {

        private DateUtil() {
            // Utility class - prevent instantiation
        }
    }

    /**
     * Converts {@code duration} in {@code unit} to milliseconds. Unlike {@link TimeUnit#toMillis(long)},
     * which saturates on overflow, this throws {@link ArithmeticException}; sub-millisecond units are
     * truncated toward zero (inherently lossy — "exact" refers to the overflow behavior).
     * @throws IllegalArgumentException if {@code unit} is {@code null}.
     * @throws ArithmeticException if conversion to milliseconds overflows.
     */
    private static long toMillisExact(final long duration, final TimeUnit unit) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(unit, cs.unit);

        switch (unit) {
            case NANOSECONDS:
                return duration / 1_000_000L;
            case MICROSECONDS:
                return duration / 1_000L;
            case MILLISECONDS:
                return duration;
            case SECONDS:
                return Math.multiplyExact(duration, 1_000L);
            case MINUTES:
                return Math.multiplyExact(duration, 60_000L);
            case HOURS:
                return Math.multiplyExact(duration, 3_600_000L);
            case DAYS:
                return Math.multiplyExact(duration, 86_400_000L);
            default:
                throw new AssertionError(unit);
        }
    }
}
