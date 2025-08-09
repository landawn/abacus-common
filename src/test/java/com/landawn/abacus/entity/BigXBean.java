package com.landawn.abacus.entity;

import java.util.ArrayList;
import java.util.List;

public class BigXBean {
    protected List<XBean> xbeanList;

    public List<XBean> getXBeanList() {
        if (xbeanList == null) {
            xbeanList = new ArrayList<>();
        }

        return this.xbeanList;
    }
}
