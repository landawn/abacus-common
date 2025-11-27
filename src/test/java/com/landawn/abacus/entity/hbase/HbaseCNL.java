package com.landawn.abacus.entity.hbase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface HbaseCNL {
    String _DN = "hbase".intern();

    public interface AccountCNL {
        String __ = "account".intern();

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

        List<String> _CNL = Collections
                .unmodifiableList(Arrays.asList(ID, GUI, EMAIL_ADDRESS, NAME, STATUS, LAST_UPDATE_TIME, CREATE_TIME, CONTACT, STR_SET, STR_MAP));

        List<String> _PNL = Collections.unmodifiableList(Arrays.asList("Account.id".intern(), "Account.gui".intern(), "Account.emailAddress".intern(),
                "Account.name".intern(), "Account.status".intern(), "Account.lastUpdateTime".intern(), "Account.createdTime".intern(),
                "Account.contact".intern(), "Account.strSet".intern(), "Account.strMap".intern()));
    }

    public interface NameCNL {
        String __ = "Name".intern();

        String FIRST_NAME = (__ + ".firstName").intern();

        String MIDDLE_NAME = (__ + ".middleName").intern();

        String LAST_NAME = (__ + ".lastName").intern();

        List<String> _CNL = Collections.unmodifiableList(Arrays.asList(FIRST_NAME, MIDDLE_NAME, LAST_NAME));

        List<String> _PNL = Collections.unmodifiableList(Arrays.asList("Name.firstName".intern(), "Name.middleName".intern(), "Name.lastName".intern()));
    }

    public interface AccountContactCNL {
        String __ = "account_contact".intern();

        String ID = (__ + ".id").intern();

        String ACCOUNT_ID = (__ + ".accountId").intern();

        String TELEPHONE = (__ + ".telephone").intern();

        String CITY = (__ + ".city").intern();

        String STATE = (__ + ".state").intern();

        String ZIP_CODE = (__ + ".zipCode").intern();

        String STATUS = (__ + ".status").intern();

        String LAST_UPDATE_TIME = (__ + ".lastUpdateTime").intern();

        String CREATE_TIME = (__ + ".createdTime").intern();

        List<String> _CNL = Collections
                .unmodifiableList(Arrays.asList(ID, ACCOUNT_ID, TELEPHONE, CITY, STATE, ZIP_CODE, STATUS, LAST_UPDATE_TIME, CREATE_TIME));

        List<String> _PNL = Collections.unmodifiableList(Arrays.asList("AccountContact.id".intern(), "AccountContact.accountId".intern(),
                "AccountContact.telephone".intern(), "AccountContact.city".intern(), "AccountContact.state".intern(), "AccountContact.zipCode".intern(),
                "AccountContact.status".intern(), "AccountContact.lastUpdateTime".intern(), "AccountContact.createdTime".intern()));
    }

    public interface AccountDeviceCNL {
        String __ = "account_device".intern();

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

        List<String> _CNL = Collections.unmodifiableList(Arrays.asList(ID, ACCOUNT_ID, NAME, UDID, PLATFORM, MODEL, MANUFACTURER, PRODUCE_TIME, CATEGORY,
                DESCRIPTION, STATUS, LAST_UPDATE_TIME, CREATE_TIME));

        List<String> _PNL = Collections
                .unmodifiableList(Arrays.asList("AccountDevice.id".intern(), "AccountDevice.accountId".intern(), "AccountDevice.name".intern(),
                        "AccountDevice.UDID".intern(), "AccountDevice.platform".intern(), "AccountDevice.model".intern(), "AccountDevice.manufacturer".intern(),
                        "AccountDevice.produceTime".intern(), "AccountDevice.category".intern(), "AccountDevice.description".intern(),
                        "AccountDevice.status".intern(), "AccountDevice.lastUpdateTime".intern(), "AccountDevice.createdTime".intern()));
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
