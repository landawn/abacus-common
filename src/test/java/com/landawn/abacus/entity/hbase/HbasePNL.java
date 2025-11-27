package com.landawn.abacus.entity.hbase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface HbasePNL {
    String _DN = "hbase".intern();

    public interface AccountPNL {
        String __ = "Account".intern();

        String ID = (__ + ".id").intern();

        String GUI = (__ + ".gui").intern();

        String EMAIL_ADDRESS = (__ + ".emailAddress").intern();

        String NAME = (__ + ".name").intern();

        String STATUS = (__ + ".status").intern();

        String LAST_UPDATE_TIME = (__ + ".lastUpdateTime").intern();

        String CREATE_TIME = (__ + ".createdTime").intern();

        String CONTACT = (__ + ".contact").intern();

        String STR_SET = (__ + ".strSet").intern();

        String STR_MAP = (__ + ".strMap").intern();

        List<String> _PNL = Collections
                .unmodifiableList(Arrays.asList(ID, GUI, EMAIL_ADDRESS, NAME, STATUS, LAST_UPDATE_TIME, CREATE_TIME, CONTACT, STR_SET, STR_MAP));

        List<String> _CNL = Collections.unmodifiableList(Arrays.asList("account.id".intern(), "account.gui".intern(), "account.emailAddress".intern(),
                "account.name".intern(), "account.status".intern(), "account.lastUpdateTime".intern(), "account.createdTime".intern(),
                "account.contact".intern(), "account.strSet".intern(), "account.strMap".intern()));
    }

    public interface NamePNL {
        String __ = "Name".intern();

        String FIRST_NAME = (__ + ".firstName").intern();

        String MIDDLE_NAME = (__ + ".middleName").intern();

        String LAST_NAME = (__ + ".lastName").intern();

        List<String> _PNL = Collections.unmodifiableList(Arrays.asList(FIRST_NAME, MIDDLE_NAME, LAST_NAME));

        List<String> _CNL = Collections.unmodifiableList(Arrays.asList("Name.firstName".intern(), "Name.middleName".intern(), "Name.lastName".intern()));
    }

    public interface AccountContactPNL {
        String __ = "AccountContact".intern();

        String ID = (__ + ".id").intern();

        String ACCOUNT_ID = (__ + ".accountId").intern();

        String TELEPHONE = (__ + ".telephone").intern();

        String CITY = (__ + ".city").intern();

        String STATE = (__ + ".state").intern();

        String ZIP_CODE = (__ + ".zipCode").intern();

        String STATUS = (__ + ".status").intern();

        String LAST_UPDATE_TIME = (__ + ".lastUpdateTime").intern();

        String CREATE_TIME = (__ + ".createdTime").intern();

        List<String> _PNL = Collections
                .unmodifiableList(Arrays.asList(ID, ACCOUNT_ID, TELEPHONE, CITY, STATE, ZIP_CODE, STATUS, LAST_UPDATE_TIME, CREATE_TIME));

        List<String> _CNL = Collections.unmodifiableList(Arrays.asList("account_contact.id".intern(), "account_contact.accountId".intern(),
                "account_contact.telephone".intern(), "account_contact.city".intern(), "account_contact.state".intern(), "account_contact.zipCode".intern(),
                "account_contact.status".intern(), "account_contact.lastUpdateTime".intern(), "account_contact.createdTime".intern()));
    }

    public interface AccountDevicePNL {
        String __ = "AccountDevice".intern();

        String ID = (__ + ".id").intern();

        String ACCOUNT_ID = (__ + ".accountId").intern();

        String NAME = (__ + ".name").intern();

        String UDID = (__ + ".UDID").intern();

        String PLATFORM = (__ + ".platform").intern();

        String MODEL = (__ + ".model").intern();

        String MANUFACTURER = (__ + ".manufacturer").intern();

        String PRODUCE_TIME = (__ + ".produceTime").intern();

        String CATEGORY = (__ + ".category").intern();

        String DESCRIPTION = (__ + ".description").intern();

        String STATUS = (__ + ".status").intern();

        String LAST_UPDATE_TIME = (__ + ".lastUpdateTime").intern();

        String CREATE_TIME = (__ + ".createdTime").intern();

        List<String> _PNL = Collections.unmodifiableList(Arrays.asList(ID, ACCOUNT_ID, NAME, UDID, PLATFORM, MODEL, MANUFACTURER, PRODUCE_TIME, CATEGORY,
                DESCRIPTION, STATUS, LAST_UPDATE_TIME, CREATE_TIME));

        List<String> _CNL = Collections.unmodifiableList(
                Arrays.asList("account_device.id".intern(), "account_device.accountId".intern(), "account_device.name".intern(), "account_device.UDID".intern(),
                        "account_device.platform".intern(), "account_device.model".intern(), "account_device.manufacturer".intern(),
                        "account_device.produceTime".intern(), "account_device.category".intern(), "account_device.description".intern(),
                        "account_device.status".intern(), "account_device.lastUpdateTime".intern(), "account_device.createdTime".intern()));
    }

    String ACCOUNT_ID = "accountId".intern();

    String CATEGORY = "category".intern();

    String CITY = "city".intern();

    String CONTACT = "contact".intern();

    String CREATE_TIME = "createdTime".intern();

    String DESCRIPTION = "description".intern();

    String EMAIL_ADDRESS = "emailAddress".intern();

    String FIRST_NAME = "firstName".intern();

    String GUI = "gui".intern();

    String ID = "id".intern();

    String LAST_NAME = "lastName".intern();

    String LAST_UPDATE_TIME = "lastUpdateTime".intern();

    String MANUFACTURER = "manufacturer".intern();

    String MIDDLE_NAME = "middleName".intern();

    String MODEL = "model".intern();

    String NAME = "name".intern();

    String PLATFORM = "platform".intern();

    String PRODUCE_TIME = "produceTime".intern();

    String STATE = "state".intern();

    String STATUS = "status".intern();

    String STR_MAP = "strMap".intern();

    String STR_SET = "strSet".intern();

    String TELEPHONE = "telephone".intern();

    String UDID = "UDID".intern();

    String ZIP_CODE = "zipCode".intern();
}
