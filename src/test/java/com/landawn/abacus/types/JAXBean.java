/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JAXBean {

    public JAXBean() {
    }

    private List<String> cityList;

    public List<String> getCityList() {
        if (cityList == null) {
            cityList = new ArrayList<>();
        }

        return cityList;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cityList);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        JAXBean other = (JAXBean) obj;

        if (!Objects.equals(cityList, other.cityList)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "{cityList=" + cityList + "}";
    }
}
