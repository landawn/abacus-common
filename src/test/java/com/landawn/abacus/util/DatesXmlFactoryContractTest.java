package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.SimpleTimeZone;
import java.util.TimeZone;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;

/** Date resolution and diagnostic regression contracts. */
public class DatesXmlFactoryContractTest extends DatesResolutionTestSupport {

    // ---------------------------------------------------------------------------------------------
    // C-128: createXMLGregorianCalendar(long, TimeZone) still snapshots a supplied mutable zone and
    // reads the live default when none is supplied
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c128_xmlFactorySnapshotsASuppliedZoneAndReadsTheDefaultWhenNoneIsSupplied() {
        final SimpleTimeZone mutable = new SimpleTimeZone(3_600_000, "Mutable/Zone");
        final XMLGregorianCalendar snapshot = Dates.createXMLGregorianCalendar(1736937045000L, mutable);
        mutable.setRawOffset(7_200_000);
        assertEquals(60, snapshot.getTimezone());
        assertEquals(11, snapshot.getHour());
        assertEquals(1736937045000L, snapshot.toGregorianCalendar().getTimeInMillis());

        final XMLGregorianCalendar fromDefault = Dates.createXMLGregorianCalendar(1736937045000L, null);
        assertEquals(TimeZone.getDefault().getOffset(1736937045000L) / 60_000, fromDefault.getTimezone());
        assertEquals(1736937045000L, fromDefault.toGregorianCalendar().getTimeInMillis());
        assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar(1736937045000L, new SimpleTimeZone(500, "Sub/Second")));
    }
}
