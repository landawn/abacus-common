package com.landawn.abacus.util;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.annotation.JsonXmlField;

import lombok.Builder;
import lombok.Data;

@Tag("old-test")
public class CsvUtilTest extends AbstractTest {
    @Test
    public void test_001() throws Exception {

        String csv = """
                id,name,date
                123,nameA,01-JAN-2025 12:00:00
                456,    \\"nameB, 01-FEB-2025 12:00:00
                ,,
                ,

                ,,
                789,    "\""nameC", 01-MAR-2025 12:00:00
                """;

        File file = new File("./src/test/resources/test_001.csv");

        IOUtil.write(csv, file);
        Dataset dataset = CsvUtil.load(file, ClassA.class);
        dataset.println();

        N.println(Strings.repeat("=", 80));

        dataset = CsvUtil.load(file, CommonUtil.asList("name", "date"), ClassA.class);
        dataset.println();

        N.println(Strings.repeat("=", 80));

        dataset = CsvUtil.load(file, CommonUtil.asList("id", "date"), ClassA.class);
        dataset.println();

        N.println(Strings.repeat("=", 80));

        List<ClassA> list = CsvUtil.stream(file, CommonUtil.asList("name"), ClassA.class).toList();
        list.forEach(Fn.println());

        N.println(Strings.repeat("=", 80));

        dataset = CsvUtil.stream(file, CommonUtil.asList("name"), ClassA.class).toDataset();
        dataset.println();

        IOUtil.deleteIfExists(file);
    }

    @Test
    public void test_load() throws Exception {
        File file = new File("./src/test/resources/test_a.csv");

        Dataset dataset = CsvUtil.load(file, ClassA.class);

        dataset.println();

        CsvUtil.stream(file, ClassA.class).forEach(Fn.println());

        CsvUtil.stream(file, CommonUtil.asList("name"), ClassA.class).forEach(Fn.println());

        CsvUtil.stream(file, CommonUtil.asSet("date"), ClassA.class).forEach(Fn.println());

    }

    @Builder
    @Data
    public static class ClassA {
        private long id;
        private String name;
        @JsonXmlField(dateFormat = "dd-MMM-yyyy HH:mm:ss")
        private Date date;
    }

}
