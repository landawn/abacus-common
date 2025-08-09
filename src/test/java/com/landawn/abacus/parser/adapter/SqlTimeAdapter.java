/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.parser.adapter;

import java.sql.Time;

import com.landawn.abacus.util.Dates;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class SqlTimeAdapter extends XmlAdapter<String, Time> {

    @Override
    public String marshal(Time v) throws Exception {
        return Dates.format(v, Dates.ISO_8601_TIMESTAMP_FORMAT);
    }

    @Override
    public Time unmarshal(String v) throws Exception {
        return Dates.parseTime(v);
    }
}
