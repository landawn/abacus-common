package com.landawn.abacus.parser.adapter;

import java.sql.Timestamp;

import com.landawn.abacus.util.Dates;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class SqlTimestampAdapter extends XmlAdapter<String, Timestamp> {

    @Override
    public String marshal(Timestamp v) throws Exception {
        return Dates.format(v, Dates.ISO_8601_TIMESTAMP_FORMAT);
    }

    @Override
    public Timestamp unmarshal(String v) throws Exception {
        return Dates.parseTimestamp(v);
    }
}
