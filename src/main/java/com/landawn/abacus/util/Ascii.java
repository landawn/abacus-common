/*
 * Copyright (C) 2010 The Guava Authors
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

/**
 * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
 *
 * Static methods pertaining to ASCII characters (those in the range of values {@code 0x00} through
 * {@code 0x7F}), and to strings containing such characters.
 *
 * <p>ASCII utilities also exist in other classes of this package:
 * <ul>
 * <!-- TODO(kevinb): how can we make this not produce a warning when building gwt javadoc? -->
 * <li>{@link Charsets#US_ASCII} specifies the {@code Charset} of ASCII characters.
 * <li>{@link com.google.common.base.CharMatcher#ascii} matches ASCII characters and provides text processing methods which
 * operate only on the ASCII characters of a string.
 * </ul>
 * 
 * <p>This class provides constants for all ASCII control characters as defined in RFC 20,
 * making it easy to work with low-level ASCII protocols and data formats.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Check if a character is ASCII
 * if (ch >= Ascii.MIN && ch <= Ascii.MAX) {
 *     // Character is ASCII
 * }
 * 
 * // Use ASCII control characters
 * String csvLine = "field1" + Ascii.HT + "field2" + Ascii.HT + "field3";
 * }</pre>
 *
 * @author Craig Berry
 * @author Gregory Kick
 * @since 1.0
 */
public final class Ascii {

    private Ascii() {
    }

    /* The ASCII control characters, per RFC 20. */
    /**
     * Null ('\0'): The all-zeros character which may serve to accomplish time fill and media fill.
     * Normally used as a C string terminator.
     *
     * <p>Although RFC 20 names this as "Null", note that it is distinct from the C/C++ "NULL"
     * pointer.
     *
     */
    public static final byte NUL = 0;

    /**
     * Start of Heading: A communication control character used at the beginning of a sequence of
     * characters which constitute a machine-sensible address or routing information. Such a sequence
     * is referred to as the "heading." An STX character has the effect of terminating a heading.
     *
     */
    public static final byte SOH = 1;

    /**
     * Start of Text: A communication control character which precedes a sequence of characters that
     * is to be treated as an entity and entirely transmitted through to the ultimate destination.
     * Such a sequence is referred to as "text." STX may be used to terminate a sequence of characters
     * started by SOH.
     *
     */
    public static final byte STX = 2;

    /**
     * End of Text: A communication control character used to terminate a sequence of characters
     * started with STX and transmitted as an entity.
     *
     */
    public static final byte ETX = 3;

    /**
     * End of Transmission: A communication control character used to indicate the conclusion of a
     * transmission, which may have contained one or more texts and any associated headings.
     *
     */
    public static final byte EOT = 4;

    /**
     * Enquiry: A communication control character used in data communication systems as a request for
     * a response from a remote station. It may be used as a "Who Are You" (WRU) to obtain
     * identification, or may be used to obtain station status, or both.
     *
     */
    public static final byte ENQ = 5;

    /**
     * Acknowledge: A communication control character transmitted by a receiver as an affirmative
     * response to a sender.
     *
     */
    public static final byte ACK = 6;

    /**
     * Bell ('\a'): A character for use when there is a need to call for human attention. It may
     * control alarm or attention devices.
     *
     */
    public static final byte BEL = 7;

    /**
     * Backspace ('\b'): A format effector which controls the movement of the printing position one
     * printing space backward on the same printing line. (Applicable also to display devices.)
     *
     */
    public static final byte BS = 8;

    /**
     * Horizontal Tabulation ('\t'): A format effector which controls the movement of the printing
     * position to the next in a series of predetermined positions along the printing line.
     * (Applicable also to display devices and the skip function on punched cards.)
     *
     */
    public static final byte HT = 9;

    /**
     * Line Feed ('\n'): A format effector which controls the movement of the printing position to the
     * next printing line. (Applicable also to display devices.) Where appropriate, this character may
     * have the meaning "New Line" (NL), a format effector which controls the movement of the printing
     * point to the first printing position on the next printing line. Use of this convention requires
     * agreement between sender and recipient of data.
     *
     */
    public static final byte LF = 10;

    /**
     * Alternate name for {@link #LF}. ({@code LF} is preferred.)
     *
     */
    public static final byte NL = 10;

    /**
     * Vertical Tabulation ('\v'): A format effector which controls the movement of the printing
     * position to the next in a series of predetermined printing lines. (Applicable also to display
     * devices.)
     *
     */
    public static final byte VT = 11;

    /**
     * Form Feed ('\f'): A format effector which controls the movement of the printing position to the
     * first pre-determined printing line on the next form or page. (Applicable also to display
     * devices.)
     *
     */
    public static final byte FF = 12;

    /**
     * Carriage Return ('\r'): A format effector which controls the movement of the printing position
     * to the first printing position on the same printing line. (Applicable also to display devices.)
     *
     */
    public static final byte CR = 13;

    /**
     * Shift Out: A control character indicating that the code combinations which follow shall be
     * interpreted as outside the character set of the standard code table until a Shift In
     * character is reached.
     *
     */
    public static final byte SO = 14;

    /**
     * Shift In: A control character indicating that the code combinations which follow shall be
     * interpreted according to the standard code table.
     *
     */
    public static final byte SI = 15;

    /**
     * Data Link Escape: A communication control character which will change the meaning of a limited
     * number of the contiguously following characters. It is used exclusively to provide supplementary
     * controls on data communication networks.
     *
     */
    public static final byte DLE = 16;

    /**
     * Device Control 1. Characters for the control of ancillary devices associated with data
     * processing or telecommunication systems, more especially switching devices "on" or "off." (If a
     * single "stop" control is required to interrupt or turn off ancillary devices, DC4 is the
     * preferred assignment.)
     *
     */
    public static final byte DC1 = 17; // aka XON

    /**
     * Transmission On: Although originally defined as DC1, this ASCII control character is now better
     * known as the XON code used for software flow control in serial communications. The main use is
     * restarting the transmission after the communication has been stopped by the XOFF control code.
     *
     */
    public static final byte XON = 17; // aka DC1

    /**
     * Device Control 2. Characters for the control of ancillary devices associated with data
     * processing or telecommunication systems, more especially switching devices "on" or "off." (If a
     * single "stop" control is required to interrupt or turn off ancillary devices, DC4 is the
     * preferred assignment.)
     *
     */
    public static final byte DC2 = 18;

    /**
     * Device Control 3. Characters for the control of ancillary devices associated with data
     * processing or telecommunication systems, more especially switching devices "on" or "off." (If a
     * single "stop" control is required to interrupt or turn off ancillary devices, DC4 is the
     * preferred assignment.)
     *
     */
    public static final byte DC3 = 19; // aka XOFF

    /**
     * Transmission off. See {@link #XON} for explanation.
     *
     */
    public static final byte XOFF = 19; // aka DC3

    /**
     * Device Control 4. Characters for the control of ancillary devices associated with data
     * processing or telecommunication systems, more especially switching devices "on" or "off." (If a
     * single "stop" control is required to interrupt or turn off ancillary devices, DC4 is the
     * preferred assignment.)
     *
     */
    public static final byte DC4 = 20;

    /**
     * Negative Acknowledge: A communication control character transmitted by a receiver as a negative
     * response to the sender.
     *
     */
    public static final byte NAK = 21;

    /**
     * Synchronous Idle: A communication control character used by a synchronous transmission system
     * in the absence of any other character to provide a signal from which synchronism may be
     * achieved or retained.
     *
     */
    public static final byte SYN = 22;

    /**
     * End of Transmission Block: A communication control character used to indicate the end of a
     * block of data for communication purposes. ETB is used for blocking data where the block
     * structure is not necessarily related to the processing format.
     *
     */
    public static final byte ETB = 23;

    /**
     * Cancel: A control character used to indicate that the data with which it is sent is in error or
     * is to be disregarded.
     *
     */
    public static final byte CAN = 24;

    /**
     * End of Medium: A control character associated with the sent data which may be used to identify
     * the physical end of the medium, or the end of the used, or wanted, portion of information
     * recorded on a medium. (The position of this character does not necessarily correspond to the
     * physical end of the medium.)
     *
     */
    public static final byte EM = 25;

    /**
     * Substitute: A character that may be substituted for a character which is determined to be
     * invalid or in error.
     *
     */
    public static final byte SUB = 26;

    /**
     * Escape: A control character intended to provide code extension (supplementary characters) in
     * general information interchange. The Escape character itself is a prefix affecting the
     * interpretation of a limited number of contiguously the following characters.
     *
     */
    public static final byte ESC = 27;

    /**
     * File Separator: These four information separators may be used within data in optional fashion,
     * except that their hierarchical relationship shall be: FS is the most inclusive, then GS, then
     * RS, and US is least inclusive. (The content and length of a File, Group, Record, or Unit are
     * not specified.)
     *
     */
    public static final byte FS = 28;

    /**
     * Group Separator: These four information separators may be used within data in optional fashion,
     * except that their hierarchical relationship shall be: FS is the most inclusive, then GS, then
     * RS, and US is least inclusive. (The content and length of a File, Group, Record, or Unit are
     * not specified.)
     *
     */
    public static final byte GS = 29;

    /**
     * Record Separator: These four information separators may be used within data in optional
     * fashion, except that their hierarchical relationship shall be: FS is the most inclusive, then
     * GS, then RS, and US is least inclusive. (The content and length of a File, Group, Record, or
     * Unit are not specified.)
     *
     */
    public static final byte RS = 30;

    /**
     * Unit Separator: These four information separators may be used within data in optional fashion,
     * except that their hierarchical relationship shall be: FS is the most inclusive, then GS, then
     * RS, and US is least inclusive. (The content and length of a File, Group, Record, or Unit are
     * not specified.)
     *
     */
    public static final byte US = 31;

    /**
     * Space: A normally non-printing graphic character used to separate words. It is also a format
     * effector that controls the movement of the printing position, one printing position forward.
     * (Applicable also to display devices.)
     *
     */
    public static final byte SP = 32;

    /**
     * Alternate name for {@link #SP}.
     *
     */
    public static final byte SPACE = 32;

    /**
     * Delete: This character is used primarily to "erase" or "obliterate" erroneous or unwanted
     * characters in perforated tape.
     *
     */
    public static final byte DEL = 127;

    /**
     * The minimum value of an ASCII character.
     *
     */
    public static final char MIN = 0;

    /**
     * The maximum value of an ASCII character.
     *
     */
    public static final char MAX = 127;
}