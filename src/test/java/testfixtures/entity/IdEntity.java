package testfixtures.entity;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Id;

import lombok.Data;

@Data
public class IdEntity extends TestBase {

    @Id
    private long id;

    @javax.persistence.Id
    private String guid;

}
