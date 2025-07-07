package codes.entity;

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
@Table("user1")
public class User {

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

    public User copy() {
        final User copy = new User();
        copy.id = this.id;
        copy.firstName = this.firstName;
        copy.lastName = this.lastName;
        copy.prop1 = this.prop1;
        copy.email = this.email;
        copy.create_time = this.create_time;
        return copy;
    }

}
