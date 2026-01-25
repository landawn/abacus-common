package com.landawn.abacus.entity;

import java.util.List;
import java.util.Set;

import javax.persistence.Id;

import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.annotation.NonUpdatable;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.annotation.Type;
import com.landawn.abacus.util.EnumType;
import com.landawn.abacus.util.NamingPolicy;

import jakarta.persistence.Column;
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
@JsonXmlConfig(namingPolicy = NamingPolicy.SCREAMING_SNAKE_CASE, ignoredFields = { "id",
        "create_time" }, dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'", timeZone = "PDT", numberFormat = "#.###", enumerated = EnumType.ORDINAL)
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

    private List<User> users;

    private Set<User> userSet;

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

    public interface x {

        String id = "id";

        String firstName = "firstName";

        String lastName = "lastName";

        String prop1 = "prop1";

        String email = "email";

        String create_time = "create_time";

    }

}
