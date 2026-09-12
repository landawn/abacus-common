package testfixtures.record;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Entity;

import lombok.Value;
import lombok.experimental.Accessors;

@Entity
@Value
@Accessors(fluent = true)
public class RecordB extends TestBase {

    private final int field1;
    private final String field2;

}
