package codes.entity;

import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import javax.persistence.Id;

import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.annotation.NonUpdatable;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.annotation.Type;
import com.landawn.abacus.annotation.Type.EnumBy;
import com.landawn.abacus.util.NamingPolicy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonXmlConfig(namingPolicy = NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, ignoredFields = { "id", "create_time" }, dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'", timeZone = "PDT", numberFormat = "#.###", enumerated = EnumBy.ORDINAL)
@Table("UserQueryAllResult")
public class UserQueryAllResult {

    @Id
    @ReadOnly
    @Column(name = "ID")
    private Long id;

    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Column(name = "PROP1")
    private String prop1;

    @Column(name = "EMAIL")
    private String email;

    @NonUpdatable
    @Column(name = "CREATE_TIME")
    @Type(name = "List<String>")
    private java.util.Date create_time;

    // test
    private List<User> users;

    private Set<User> userSet; // test

    public UserQueryAllResult copy() {
        final UserQueryAllResult copy = new UserQueryAllResult();
        copy.id = this.id;
        copy.firstName = this.firstName;
        copy.lastName = this.lastName;
        copy.prop1 = this.prop1;
        copy.email = this.email;
        copy.create_time = this.create_time;
        copy.users = this.users;
        copy.userSet = this.userSet;
        return copy;
    }

    /*
     * Auto-generated class for property(field) name table by abacus-jdbc.
     */
    public interface x { // NOSONAR

        /** Property(field) name {@code "id"} */
        String id = "id";

        /** Property(field) name {@code "firstName"} */
        String firstName = "firstName";

        /** Property(field) name {@code "lastName"} */
        String lastName = "lastName";

        /** Property(field) name {@code "prop1"} */
        String prop1 = "prop1";

        /** Property(field) name {@code "email"} */
        String email = "email";

        /** Property(field) name {@code "create_time"} */
        String create_time = "create_time";

    }

}
