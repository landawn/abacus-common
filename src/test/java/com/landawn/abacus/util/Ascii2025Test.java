package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Ascii2025Test extends TestBase {

    @Test
    public void testNullCharacter() {
        assertEquals(0, Ascii.NUL);
        assertEquals('\0', (char) Ascii.NUL);
    }

    @Test
    public void testStartOfHeading() {
        assertEquals(1, Ascii.SOH);
    }

    @Test
    public void testStartOfText() {
        assertEquals(2, Ascii.STX);
    }

    @Test
    public void testEndOfText() {
        assertEquals(3, Ascii.ETX);
    }

    @Test
    public void testEndOfTransmission() {
        assertEquals(4, Ascii.EOT);
    }

    @Test
    public void testEnquiry() {
        assertEquals(5, Ascii.ENQ);
    }

    @Test
    public void testAcknowledge() {
        assertEquals(6, Ascii.ACK);
    }

    @Test
    public void testBell() {
        assertEquals(7, Ascii.BEL);
        assertEquals('\u0007', (char) Ascii.BEL);
    }

    @Test
    public void testBackspace() {
        assertEquals(8, Ascii.BS);
        assertEquals('\b', (char) Ascii.BS);
    }

    @Test
    public void testHorizontalTab() {
        assertEquals(9, Ascii.HT);
        assertEquals('\t', (char) Ascii.HT);
    }

    @Test
    public void testLineFeed() {
        assertEquals(10, Ascii.LF);
        assertEquals('\n', (char) Ascii.LF);
    }

    @Test
    public void testNewLine() {
        assertEquals(10, Ascii.NL);
        assertEquals(Ascii.LF, Ascii.NL);
    }

    @Test
    public void testVerticalTab() {
        assertEquals(11, Ascii.VT);
        assertEquals('\u000B', (char) Ascii.VT);
    }

    @Test
    public void testFormFeed() {
        assertEquals(12, Ascii.FF);
        assertEquals('\f', (char) Ascii.FF);
    }

    @Test
    public void testCarriageReturn() {
        assertEquals(13, Ascii.CR);
        assertEquals('\r', (char) Ascii.CR);
    }

    @Test
    public void testShiftOut() {
        assertEquals(14, Ascii.SO);
    }

    @Test
    public void testShiftIn() {
        assertEquals(15, Ascii.SI);
    }

    @Test
    public void testDataLinkEscape() {
        assertEquals(16, Ascii.DLE);
    }

    @Test
    public void testDeviceControl1() {
        assertEquals(17, Ascii.DC1);
    }

    @Test
    public void testTransmissionOn() {
        assertEquals(17, Ascii.XON);
        assertEquals(Ascii.DC1, Ascii.XON);
    }

    @Test
    public void testDeviceControl2() {
        assertEquals(18, Ascii.DC2);
    }

    @Test
    public void testDeviceControl3() {
        assertEquals(19, Ascii.DC3);
    }

    @Test
    public void testTransmissionOff() {
        assertEquals(19, Ascii.XOFF);
        assertEquals(Ascii.DC3, Ascii.XOFF);
    }

    @Test
    public void testDeviceControl4() {
        assertEquals(20, Ascii.DC4);
    }

    @Test
    public void testNegativeAcknowledge() {
        assertEquals(21, Ascii.NAK);
    }

    @Test
    public void testSynchronousIdle() {
        assertEquals(22, Ascii.SYN);
    }

    @Test
    public void testEndOfTransmissionBlock() {
        assertEquals(23, Ascii.ETB);
    }

    @Test
    public void testCancel() {
        assertEquals(24, Ascii.CAN);
    }

    @Test
    public void testEndOfMedium() {
        assertEquals(25, Ascii.EM);
    }

    @Test
    public void testSubstitute() {
        assertEquals(26, Ascii.SUB);
    }

    @Test
    public void testEscape() {
        assertEquals(27, Ascii.ESC);
        assertEquals('\u001B', (char) Ascii.ESC);
    }

    @Test
    public void testFileSeparator() {
        assertEquals(28, Ascii.FS);
    }

    @Test
    public void testGroupSeparator() {
        assertEquals(29, Ascii.GS);
    }

    @Test
    public void testRecordSeparator() {
        assertEquals(30, Ascii.RS);
    }

    @Test
    public void testUnitSeparator() {
        assertEquals(31, Ascii.US);
    }

    @Test
    public void testSpace() {
        assertEquals(32, Ascii.SP);
        assertEquals(' ', (char) Ascii.SP);
    }

    @Test
    public void testSpaceAlternate() {
        assertEquals(32, Ascii.SPACE);
        assertEquals(Ascii.SP, Ascii.SPACE);
        assertEquals(' ', (char) Ascii.SPACE);
    }

    @Test
    public void testDelete() {
        assertEquals(127, Ascii.DEL);
    }

    @Test
    public void testMinValue() {
        assertEquals(0, Ascii.MIN);
    }

    @Test
    public void testMaxValue() {
        assertEquals(127, Ascii.MAX);
    }

    @Test
    public void testAsciiRangeValidation() {
        assertTrue(Ascii.NUL >= Ascii.MIN && Ascii.NUL <= Ascii.MAX);
        assertTrue(Ascii.SOH >= Ascii.MIN && Ascii.SOH <= Ascii.MAX);
        assertTrue(Ascii.STX >= Ascii.MIN && Ascii.STX <= Ascii.MAX);
        assertTrue(Ascii.ETX >= Ascii.MIN && Ascii.ETX <= Ascii.MAX);
        assertTrue(Ascii.EOT >= Ascii.MIN && Ascii.EOT <= Ascii.MAX);
        assertTrue(Ascii.ENQ >= Ascii.MIN && Ascii.ENQ <= Ascii.MAX);
        assertTrue(Ascii.ACK >= Ascii.MIN && Ascii.ACK <= Ascii.MAX);
        assertTrue(Ascii.BEL >= Ascii.MIN && Ascii.BEL <= Ascii.MAX);
        assertTrue(Ascii.BS >= Ascii.MIN && Ascii.BS <= Ascii.MAX);
        assertTrue(Ascii.HT >= Ascii.MIN && Ascii.HT <= Ascii.MAX);
        assertTrue(Ascii.LF >= Ascii.MIN && Ascii.LF <= Ascii.MAX);
        assertTrue(Ascii.NL >= Ascii.MIN && Ascii.NL <= Ascii.MAX);
        assertTrue(Ascii.VT >= Ascii.MIN && Ascii.VT <= Ascii.MAX);
        assertTrue(Ascii.FF >= Ascii.MIN && Ascii.FF <= Ascii.MAX);
        assertTrue(Ascii.CR >= Ascii.MIN && Ascii.CR <= Ascii.MAX);
        assertTrue(Ascii.SO >= Ascii.MIN && Ascii.SO <= Ascii.MAX);
        assertTrue(Ascii.SI >= Ascii.MIN && Ascii.SI <= Ascii.MAX);
        assertTrue(Ascii.DLE >= Ascii.MIN && Ascii.DLE <= Ascii.MAX);
        assertTrue(Ascii.DC1 >= Ascii.MIN && Ascii.DC1 <= Ascii.MAX);
        assertTrue(Ascii.XON >= Ascii.MIN && Ascii.XON <= Ascii.MAX);
        assertTrue(Ascii.DC2 >= Ascii.MIN && Ascii.DC2 <= Ascii.MAX);
        assertTrue(Ascii.DC3 >= Ascii.MIN && Ascii.DC3 <= Ascii.MAX);
        assertTrue(Ascii.XOFF >= Ascii.MIN && Ascii.XOFF <= Ascii.MAX);
        assertTrue(Ascii.DC4 >= Ascii.MIN && Ascii.DC4 <= Ascii.MAX);
        assertTrue(Ascii.NAK >= Ascii.MIN && Ascii.NAK <= Ascii.MAX);
        assertTrue(Ascii.SYN >= Ascii.MIN && Ascii.SYN <= Ascii.MAX);
        assertTrue(Ascii.ETB >= Ascii.MIN && Ascii.ETB <= Ascii.MAX);
        assertTrue(Ascii.CAN >= Ascii.MIN && Ascii.CAN <= Ascii.MAX);
        assertTrue(Ascii.EM >= Ascii.MIN && Ascii.EM <= Ascii.MAX);
        assertTrue(Ascii.SUB >= Ascii.MIN && Ascii.SUB <= Ascii.MAX);
        assertTrue(Ascii.ESC >= Ascii.MIN && Ascii.ESC <= Ascii.MAX);
        assertTrue(Ascii.FS >= Ascii.MIN && Ascii.FS <= Ascii.MAX);
        assertTrue(Ascii.GS >= Ascii.MIN && Ascii.GS <= Ascii.MAX);
        assertTrue(Ascii.RS >= Ascii.MIN && Ascii.RS <= Ascii.MAX);
        assertTrue(Ascii.US >= Ascii.MIN && Ascii.US <= Ascii.MAX);
        assertTrue(Ascii.SP >= Ascii.MIN && Ascii.SP <= Ascii.MAX);
        assertTrue(Ascii.SPACE >= Ascii.MIN && Ascii.SPACE <= Ascii.MAX);
        assertTrue(Ascii.DEL >= Ascii.MIN && Ascii.DEL <= Ascii.MAX);
    }

    @Test
    public void testControlCharactersSequence() {
        assertEquals(Ascii.NUL + 1, Ascii.SOH);
        assertEquals(Ascii.SOH + 1, Ascii.STX);
        assertEquals(Ascii.STX + 1, Ascii.ETX);
        assertEquals(Ascii.ETX + 1, Ascii.EOT);
        assertEquals(Ascii.EOT + 1, Ascii.ENQ);
        assertEquals(Ascii.ENQ + 1, Ascii.ACK);
        assertEquals(Ascii.ACK + 1, Ascii.BEL);
        assertEquals(Ascii.BEL + 1, Ascii.BS);
        assertEquals(Ascii.BS + 1, Ascii.HT);
        assertEquals(Ascii.HT + 1, Ascii.LF);
        assertEquals(Ascii.LF + 1, Ascii.VT);
        assertEquals(Ascii.VT + 1, Ascii.FF);
        assertEquals(Ascii.FF + 1, Ascii.CR);
        assertEquals(Ascii.CR + 1, Ascii.SO);
        assertEquals(Ascii.SO + 1, Ascii.SI);
        assertEquals(Ascii.SI + 1, Ascii.DLE);
        assertEquals(Ascii.DLE + 1, Ascii.DC1);
        assertEquals(Ascii.DC1 + 1, Ascii.DC2);
        assertEquals(Ascii.DC2 + 1, Ascii.DC3);
        assertEquals(Ascii.DC3 + 1, Ascii.DC4);
        assertEquals(Ascii.DC4 + 1, Ascii.NAK);
        assertEquals(Ascii.NAK + 1, Ascii.SYN);
        assertEquals(Ascii.SYN + 1, Ascii.ETB);
        assertEquals(Ascii.ETB + 1, Ascii.CAN);
        assertEquals(Ascii.CAN + 1, Ascii.EM);
        assertEquals(Ascii.EM + 1, Ascii.SUB);
        assertEquals(Ascii.SUB + 1, Ascii.ESC);
        assertEquals(Ascii.ESC + 1, Ascii.FS);
        assertEquals(Ascii.FS + 1, Ascii.GS);
        assertEquals(Ascii.GS + 1, Ascii.RS);
        assertEquals(Ascii.RS + 1, Ascii.US);
        assertEquals(Ascii.US + 1, Ascii.SP);
    }

    @Test
    public void testSeparatorsHierarchy() {
        assertTrue(Ascii.FS < Ascii.GS);
        assertTrue(Ascii.GS < Ascii.RS);
        assertTrue(Ascii.RS < Ascii.US);
    }

    @Test
    public void testFlowControlCharacters() {
        assertEquals(Ascii.DC1, Ascii.XON);
        assertEquals(Ascii.DC3, Ascii.XOFF);
        assertTrue(Ascii.XON < Ascii.XOFF);
    }

    @Test
    public void testPrintableSpaceCharacter() {
        assertEquals(32, Ascii.SPACE);
        assertTrue(Ascii.SPACE > Ascii.US);
    }

    @Test
    public void testDeleteIsLastAscii() {
        assertEquals(127, Ascii.DEL);
        assertEquals(Ascii.MAX, Ascii.DEL);
    }

    @Test
    public void testByteRangeConversion() {
        byte nul = Ascii.NUL;
        byte del = Ascii.DEL;
        assertEquals(0, nul);
        assertEquals(127, del);
    }

    @Test
    public void testCharRangeConversion() {
        char min = Ascii.MIN;
        char max = Ascii.MAX;
        assertEquals(0, min);
        assertEquals(127, max);
    }

    @Test
    public void testCommonWhitespaceCharacters() {
        assertEquals(9, Ascii.HT);
        assertEquals(10, Ascii.LF);
        assertEquals(13, Ascii.CR);
        assertEquals(32, Ascii.SP);
    }

    @Test
    public void testAliasesEquality() {
        assertEquals(Ascii.LF, Ascii.NL);
        assertEquals(Ascii.SP, Ascii.SPACE);
        assertEquals(Ascii.DC1, Ascii.XON);
        assertEquals(Ascii.DC3, Ascii.XOFF);
    }

    @Test
    public void testUsageInStringBuilding() {
        String csvLine = "field1" + (char) Ascii.HT + "field2" + (char) Ascii.HT + "field3";
        assertTrue(csvLine.contains("\t"));

        String textWithNewline = "line1" + (char) Ascii.LF + "line2";
        assertTrue(textWithNewline.contains("\n"));
    }

    @Test
    public void testByteCasting() {
        for (byte b = Ascii.MIN; b <= Ascii.MAX && b >= 0; b++) {
            assertTrue(b >= 0 && b <= 127);
            if (b == Ascii.MAX)
                break;
        }
    }
}
