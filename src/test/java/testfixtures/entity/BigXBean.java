package testfixtures.entity;

import java.util.ArrayList;
import java.util.List;

import com.landawn.abacus.TestBase;

public class BigXBean extends TestBase {
    protected List<XBean> xbeanList;

    public List<XBean> getXBeanList() {
        if (xbeanList == null) {
            xbeanList = new ArrayList<>();
        }

        return this.xbeanList;
    }
}
