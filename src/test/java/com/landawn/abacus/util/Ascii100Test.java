package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Ascii100Test extends TestBase {

    @Test
    public void testControlCharacterConstants() {
        Assertions.assertEquals(0, Ascii.NUL);
        Assertions.assertEquals(1, Ascii.SOH);
        Assertions.assertEquals(2, Ascii.STX);
        Assertions.assertEquals(3, Ascii.ETX);
        Assertions.assertEquals(4, Ascii.EOT);
        Assertions.assertEquals(5, Ascii.ENQ);
        Assertions.assertEquals(6, Ascii.ACK);
        Assertions.assertEquals(7, Ascii.BEL);
        Assertions.assertEquals(8, Ascii.BS);
        Assertions.assertEquals(9, Ascii.HT);
        Assertions.assertEquals(10, Ascii.LF);
        Assertions.assertEquals(10, Ascii.NL);
        Assertions.assertEquals(11, Ascii.VT);
        Assertions.assertEquals(12, Ascii.FF);
        Assertions.assertEquals(13, Ascii.CR);
        Assertions.assertEquals(14, Ascii.SO);
        Assertions.assertEquals(15, Ascii.SI);
        Assertions.assertEquals(16, Ascii.DLE);
        Assertions.assertEquals(17, Ascii.DC1);
        Assertions.assertEquals(17, Ascii.XON);
        Assertions.assertEquals(18, Ascii.DC2);
        Assertions.assertEquals(19, Ascii.DC3);
        Assertions.assertEquals(19, Ascii.XOFF);
        Assertions.assertEquals(20, Ascii.DC4);
        Assertions.assertEquals(21, Ascii.NAK);
        Assertions.assertEquals(22, Ascii.SYN);
        Assertions.assertEquals(23, Ascii.ETB);
        Assertions.assertEquals(24, Ascii.CAN);
        Assertions.assertEquals(25, Ascii.EM);
        Assertions.assertEquals(26, Ascii.SUB);
        Assertions.assertEquals(27, Ascii.ESC);
        Assertions.assertEquals(28, Ascii.FS);
        Assertions.assertEquals(29, Ascii.GS);
        Assertions.assertEquals(30, Ascii.RS);
        Assertions.assertEquals(31, Ascii.US);
        Assertions.assertEquals(32, Ascii.SP);
        Assertions.assertEquals(32, Ascii.SPACE);
        Assertions.assertEquals(127, Ascii.DEL);
    }

    @Test
    public void testMinMaxValues() {
        Assertions.assertEquals(0, Ascii.MIN);
        Assertions.assertEquals(127, Ascii.MAX);
    }

    @Test
    public void testAsciiRange() {
        for (int i = Ascii.MIN; i <= Ascii.MAX; i++) {
            Assertions.assertTrue(i >= 0 && i <= 127);
        }
    }
}
