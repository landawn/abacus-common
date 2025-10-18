package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class SheetTest extends AbstractTest {

    @Test
    public void test_01() {
        Dataset.empty().println();

        Sheet.empty().println();

        Sheet<String, String, Object> sheet = Sheet.rows(CommonUtil.asList("r1", "r2", "r3"), CommonUtil.asList("c1", "c2"),
                new Object[][] { { 1, "a" }, { null, "b" }, { 5, "c" } });

        sheet.println();
        sheet.toDatasetH().println();
        sheet.toDatasetV().println();

        sheet.rows().map(it -> Pair.of(it.left(), it.right().join(","))).forEach(Fn.println());
        sheet.columns().map(it -> Pair.of(it.left(), it.right().join(","))).forEach(Fn.println());

        N.println(sheet.toString());

        N.println(N.toJson(sheet));
        N.println(N.toJson(sheet, true));

        assertEquals(sheet, N.fromJson(N.toJson(sheet), Sheet.class));

        N.println(sheet.countOfNonNullValue());

        sheet = new Sheet<>(CommonUtil.asList("r1", "r2", "r3"), CommonUtil.asList("c1", "c2"));
        sheet.forEachH((r, c, v) -> N.println(r + ": " + c + ": " + v));

        sheet = Sheet.rows(CommonUtil.emptyList(), CommonUtil.emptyList(), CommonUtil.emptyList());
        N.println(sheet);
        N.println(N.toJson(sheet));

        sheet = Sheet.rows((List<String>) null, (List<String>) null, (List<List<Object>>) null);
        sheet.println();
        N.println(sheet);
        N.println(N.toJson(sheet));

        assertEquals(sheet, sheet.clone());

    }

}
