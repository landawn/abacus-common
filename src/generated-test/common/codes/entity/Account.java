package codes.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.NonUpdatable;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.annotation.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("account")
public class Account {

    @Id
    @NonUpdatable
    @Column("ID")
    private long id;

    @Column("FIRST_NAME")
    private String firstName;

    @Column("LAST_NAME")
    private String lastName;

    @Column("EMAIL_ADDRESS")
    private String emailAddress;

    @ReadOnly
    @Column("CREATE_TIME")
    private java.sql.Timestamp createTime;

    /**
     * Auto-generated class for property(field) name table.
     */
    public interface x { // NOSONAR

        /** Property(field) name {@code "id"} */
        String id = "id";

        /** Property(field) name {@code "firstName"} */
        String firstName = "firstName";

        /** Property(field) name {@code "lastName"} */
        String lastName = "lastName";

        /** Property(field) name {@code "emailAddress"} */
        String emailAddress = "emailAddress";

        /** Property(field) name {@code "createTime"} */
        String createTime = "createTime";

    }

}
