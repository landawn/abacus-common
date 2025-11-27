package com.landawn.abacus.parser.adapter;

import java.util.Calendar;

import com.landawn.abacus.util.Dates;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class CalendarAdapter extends XmlAdapter<String, Calendar> {

    @Override
    public String marshal(Calendar v) throws Exception {
        return Dates.format(v);
    }

    @Override
    public Calendar unmarshal(String v) throws Exception {
        return Dates.parseCalendar(v);
    }
}
