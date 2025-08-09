package com.landawn.abacus.util;

import java.util.Objects;

public class MyEntity_1 {
    private Range<Float> range;

    public Range<Float> getRange() {
        return range;
    }

    public void setRange(Range<Float> range) {
        this.range = range;
    }

    @Override
    public int hashCode() {
        return Objects.hash(range);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        MyEntity_1 other = (MyEntity_1) obj;
        if (!Objects.equals(range, other.range)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MyBean_1 [range=" + range + "]";
    }
}
