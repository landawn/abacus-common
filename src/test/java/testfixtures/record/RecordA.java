package testfixtures.record;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Record;

import lombok.Value;
import lombok.experimental.Accessors;

@Record
@Value
@Accessors(fluent = true)
public class RecordA extends TestBase {

    private final int field1;
    private final String field2;

}
