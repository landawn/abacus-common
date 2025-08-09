/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.parser.adapter;

import java.util.Date;

import com.landawn.abacus.util.Dates;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class DateAdapter extends XmlAdapter<String, Date> {

    @Override
    public String marshal(Date v) throws Exception {
        return Dates.format(v, Dates.ISO_8601_TIMESTAMP_FORMAT);
    }

    @Override
    public Date unmarshal(String v) throws Exception {
        return Dates.parseJUDate(v);
    }
}
