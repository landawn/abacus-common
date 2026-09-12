package testfixtures.entity;

import java.util.ArrayList;
import java.util.List;

import com.landawn.abacus.TestBase;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PersonsType", propOrder = { "person" })
@XmlRootElement
public class PersonsType extends TestBase {

    @XmlElement(required = true)
    protected List<PersonType> person;

    public List<PersonType> getPerson() {
        if (person == null) {
            person = new ArrayList<>();
        }
        return this.person;
    }

}
