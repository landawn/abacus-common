package com.landawn.abacus.entity.extendDirty.basic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface ExtendDirtyBasicPNL {
    String _DN = "extendDirtyBasic".intern();

    public interface AccountPNL {
        String __ = "Account".intern();

        String ID = (__ + ".id").intern();

        String GUI = (__ + ".gui").intern();

        String EMAIL_ADDRESS = (__ + ".emailAddress").intern();

        String FIRST_NAME = (__ + ".firstName").intern();

        String MIDDLE_NAME = (__ + ".middleName").intern();

        String LAST_NAME = (__ + ".lastName").intern();

        String BIRTH_DATE = (__ + ".birthDate").intern();

        String STATUS = (__ + ".status").intern();

        String LAST_UPDATE_TIME = (__ + ".lastUpdateTime").intern();

        String CREATE_TIME = (__ + ".createdTime").intern();

        String CONTACT = (__ + ".contact").intern();

        String DEVICES = (__ + ".devices").intern();

        List<String> _PNL = Collections.unmodifiableList(
                Arrays.asList(ID, GUI, EMAIL_ADDRESS, FIRST_NAME, MIDDLE_NAME, LAST_NAME, BIRTH_DATE, STATUS, LAST_UPDATE_TIME, CREATE_TIME, CONTACT, DEVICES));

        List<String> _CNL = Collections.unmodifiableList(Arrays.asList("account.id".intern(), "account.gui".intern(), "account.email_address".intern(),
                "account.first_name".intern(), "account.middle_name".intern(), "account.last_name".intern(), "account.birth_date".intern(),
                "account.status".intern(), "account.last_update_time".intern(), "account.create_time".intern()));
    }

    public interface Account1PNL {
        String __ = "Account1";
    }

    public interface Account2PNL {
        String __ = "Account2";
    }

    public interface AccountContactPNL {
        String __ = "AccountContact".intern();

        String ID = (__ + ".id").intern();

        String ACCOUNT_ID = (__ + ".accountId").intern();

        String MOBILE = (__ + ".mobile").intern();

        String TELEPHONE = (__ + ".telephone").intern();

        String EMAIL = (__ + ".email").intern();

        String ADDRESS = (__ + ".address").intern();

        String ADDRESS_2 = (__ + ".address2").intern();

        String CITY = (__ + ".city").intern();

        String STATE = (__ + ".state").intern();

        String COUNTRY = (__ + ".country").intern();

        String ZIP_CODE = (__ + ".zipCode").intern();

        String CATEGORY = (__ + ".category").intern();

        String DESCRIPTION = (__ + ".description").intern();

        String STATUS = (__ + ".status").intern();

        String LAST_UPDATE_TIME = (__ + ".lastUpdateTime").intern();

        String CREATE_TIME = (__ + ".createdTime").intern();

        List<String> _PNL = Collections.unmodifiableList(Arrays.asList(ID, ACCOUNT_ID, MOBILE, TELEPHONE, EMAIL, ADDRESS, ADDRESS_2, CITY, STATE, COUNTRY,
                ZIP_CODE, CATEGORY, DESCRIPTION, STATUS, LAST_UPDATE_TIME, CREATE_TIME));

        List<String> _CNL = Collections.unmodifiableList(Arrays.asList("account_contact.id".intern(), "account_contact.account_id".intern(),
                "account_contact.mobile".intern(), "account_contact.telephone".intern(), "account_contact.email".intern(), "account_contact.address".intern(),
                "account_contact.address_2".intern(), "account_contact.city".intern(), "account_contact.state".intern(), "account_contact.country".intern(),
                "account_contact.zip_code".intern(), "account_contact.category".intern(), "account_contact.description".intern(),
                "account_contact.status".intern(), "account_contact.last_update_time".intern(), "account_contact.create_time".intern()));
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

        List<String> _CNL = Collections.unmodifiableList(Arrays.asList("account_device.id".intern(), "account_device.account_id".intern(),
                "account_device.name".intern(), "account_device.udid".intern(), "account_device.platform".intern(), "account_device.model".intern(),
                "account_device.manufacturer".intern(), "account_device.produce_time".intern(), "account_device.category".intern(),
                "account_device.description".intern(), "account_device.status".intern(), "account_device.last_update_time".intern(),
                "account_device.create_time".intern()));
    }

    public interface AclGroupPNL {
        String __ = "AclGroup".intern();

        String ID = (__ + ".id").intern();

        String GUI = (__ + ".gui").intern();

        String NAME = (__ + ".name").intern();

        String DESCRIPTION = (__ + ".description").intern();

        String STATUS = (__ + ".status").intern();

        String LAST_UPDATE_TIME = (__ + ".lastUpdateTime").intern();

        String CREATE_TIME = (__ + ".createdTime").intern();

        String USER_LIST = (__ + ".userList").intern();

        List<String> _PNL = Collections.unmodifiableList(Arrays.asList(ID, GUI, NAME, DESCRIPTION, STATUS, LAST_UPDATE_TIME, CREATE_TIME, USER_LIST));

        List<String> _CNL = Collections.unmodifiableList(Arrays.asList("acl_group.id".intern(), "acl_group.gui".intern(), "acl_group.name".intern(),
                "acl_group.description".intern(), "acl_group.status".intern(), "acl_group.last_update_time".intern(), "acl_group.create_time".intern()));
    }

    public interface AclTargetPNL {
        String __ = "AclTarget".intern();

        String ID = (__ + ".id").intern();

        String GUI = (__ + ".gui").intern();

        String NAME = (__ + ".name").intern();

        String CATEGORY = (__ + ".category").intern();

        String SUB_CATEGORY = (__ + ".subCategory").intern();

        String TYPE = (__ + ".type").intern();

        String SUB_TYPE = (__ + ".subType").intern();

        String DESCRIPTION = (__ + ".description").intern();

        String STATUS = (__ + ".status").intern();

        String LAST_UPDATE_TIME = (__ + ".lastUpdateTime").intern();

        String CREATE_TIME = (__ + ".createdTime").intern();

        List<String> _PNL = Collections
                .unmodifiableList(Arrays.asList(ID, GUI, NAME, CATEGORY, SUB_CATEGORY, TYPE, SUB_TYPE, DESCRIPTION, STATUS, LAST_UPDATE_TIME, CREATE_TIME));

        List<String> _CNL = Collections.unmodifiableList(Arrays.asList("acl_target.id".intern(), "acl_target.gui".intern(), "acl_target.name".intern(),
                "acl_target.category".intern(), "acl_target.sub_category".intern(), "acl_target.type".intern(), "acl_target.sub_type".intern(),
                "acl_target.description".intern(), "acl_target.status".intern(), "acl_target.last_update_time".intern(), "acl_target.create_time".intern()));
    }

    public interface AclUgTargetRelationshipPNL {
        String __ = "AclUgTargetRelationship".intern();

        String ID = (__ + ".id").intern();

        String UG_GUI = (__ + ".ugGui").intern();

        String TARGET_GUI = (__ + ".targetGui").intern();

        String PRIVILEGE = (__ + ".privilege").intern();

        String DESCRIPTION = (__ + ".description").intern();

        String STATUS = (__ + ".status").intern();

        String LAST_UPDATE_TIME = (__ + ".lastUpdateTime").intern();

        String CREATE_TIME = (__ + ".createdTime").intern();

        List<String> _PNL = Collections.unmodifiableList(Arrays.asList(ID, UG_GUI, TARGET_GUI, PRIVILEGE, DESCRIPTION, STATUS, LAST_UPDATE_TIME, CREATE_TIME));

        List<String> _CNL = Collections.unmodifiableList(Arrays.asList("acl_ug_target_relationship.id".intern(), "acl_ug_target_relationship.ug_gui".intern(),
                "acl_ug_target_relationship.target_gui".intern(), "acl_ug_target_relationship.privilege".intern(),
                "acl_ug_target_relationship.description".intern(), "acl_ug_target_relationship.status".intern(),
                "acl_ug_target_relationship.last_update_time".intern(), "acl_ug_target_relationship.create_time".intern()));
    }

    public interface AclUserPNL {
        String __ = "AclUser".intern();

        String ID = (__ + ".id").intern();

        String GUI = (__ + ".gui").intern();

        String NAME = (__ + ".name").intern();

        String DESCRIPTION = (__ + ".description").intern();

        String STATUS = (__ + ".status").intern();

        String LAST_UPDATE_TIME = (__ + ".lastUpdateTime").intern();

        String CREATE_TIME = (__ + ".createdTime").intern();

        String GROUP_LIST = (__ + ".groupList").intern();

        List<String> _PNL = Collections.unmodifiableList(Arrays.asList(ID, GUI, NAME, DESCRIPTION, STATUS, LAST_UPDATE_TIME, CREATE_TIME, GROUP_LIST));

        List<String> _CNL = Collections.unmodifiableList(Arrays.asList("acl_user.id".intern(), "acl_user.gui".intern(), "acl_user.name".intern(),
                "acl_user.description".intern(), "acl_user.status".intern(), "acl_user.last_update_time".intern(), "acl_user.create_time".intern()));
    }

    public interface AclUserGroupRelationshipPNL {
        String __ = "AclUserGroupRelationship".intern();

        String ID = (__ + ".id").intern();

        String USER_GUI = (__ + ".userGUI").intern();

        String GROUP_GUI = (__ + ".groupGUI").intern();

        String DESCRIPTION = (__ + ".description").intern();

        String STATUS = (__ + ".status").intern();

        String LAST_UPDATE_TIME = (__ + ".lastUpdateTime").intern();

        String CREATE_TIME = (__ + ".createdTime").intern();

        List<String> _PNL = Collections.unmodifiableList(Arrays.asList(ID, USER_GUI, GROUP_GUI, DESCRIPTION, STATUS, LAST_UPDATE_TIME, CREATE_TIME));

        List<String> _CNL = Collections.unmodifiableList(Arrays.asList("acl_user_group_relationship.id".intern(),
                "acl_user_group_relationship.user_gui".intern(), "acl_user_group_relationship.group_gui".intern(),
                "acl_user_group_relationship.description".intern(), "acl_user_group_relationship.status".intern(),
                "acl_user_group_relationship.last_update_time".intern(), "acl_user_group_relationship.create_time".intern()));
    }

    public interface DataTypePNL {
        String __ = "DataType".intern();

        String BYTE_TYPE = (__ + ".byteType").intern();

        String CHAR_TYPE = (__ + ".charType").intern();

        String BOOLEAN_TYPE = (__ + ".booleanType").intern();

        String SHORT_TYPE = (__ + ".shortType").intern();

        String INT_TYPE = (__ + ".intType").intern();

        String LONG_TYPE = (__ + ".longType").intern();

        String FLOAT_TYPE = (__ + ".floatType").intern();

        String DOUBLE_TYPE = (__ + ".doubleType").intern();

        String BIG_INTEGER_TYPE = (__ + ".bigIntegerType").intern();

        String BIG_DECIMAL_TYPE = (__ + ".bigDecimalType").intern();

        String STRING_TYPE = (__ + ".stringType").intern();

        String BYTE_ARRAY_TYPE = (__ + ".byteArrayType").intern();

        String CHARACTER_STREAM_TYPE = (__ + ".characterStreamType").intern();

        String BINARY_STREAM_TYPE = (__ + ".binaryStreamType").intern();

        String CLOB_TYPE = (__ + ".clobType").intern();

        String BLOB_TYPE = (__ + ".blobType").intern();

        String DATE_TYPE = (__ + ".dateType").intern();

        String TIME_TYPE = (__ + ".timeType").intern();

        String TIMESTAMP_TYPE = (__ + ".timestampType").intern();

        String LONG_DATE_TYPE = (__ + ".longDateType").intern();

        String LONG_TIME_TYPE = (__ + ".longTimeType").intern();

        String LONG_TIMESTAMP_TYPE = (__ + ".longTimestampType").intern();

        String ENUM_TYPE = (__ + ".enumType").intern();

        String STRING_ARRAY_LIST_TYPE = (__ + ".stringArrayListType").intern();

        String BOOLEAN_LINKED_LIST_TYPE = (__ + ".booleanLinkedListType").intern();

        String DOUBLE_LIST_TYPE = (__ + ".doubleListType").intern();

        String DATE_ARRAY_LIST_TYPE = (__ + ".dateArrayListType").intern();

        String TIMESTAMP_ARRAY_LIST_TYPE = (__ + ".timestampArrayListType").intern();

        String BIG_DECIMAL_ARRAY_LIST_TYPE = (__ + ".bigDecimalArrayListType").intern();

        String STRING_HASH_SET_TYPE = (__ + ".stringHashSetType").intern();

        String BOOLEAN_LINKED_HASH_SET_TYPE = (__ + ".booleanLinkedHashSetType").intern();

        String DATE_HASH_SET_TYPE = (__ + ".dateHashSetType").intern();

        String TIMESTAMP_HASH_SET_TYPE = (__ + ".timestampHashSetType").intern();

        String BIG_DECIMAL_HASH_SET_TYPE = (__ + ".bigDecimalHashSetType").intern();

        String STRING_HASH_MAP_TYPE = (__ + ".stringHashMapType").intern();

        String BOOLEAN_LINKED_HASH_MAP_TYPE = (__ + ".booleanLinkedHashMapType").intern();

        String FLOAT_HASH_MAP_TYPE = (__ + ".floatHashMapType").intern();

        String DATE_HASH_MAP_TYPE = (__ + ".dateHashMapType").intern();

        String TIMESTAMP_HASH_MAP_TYPE = (__ + ".timestampHashMapType").intern();

        String BIG_DECIMAL_HASH_MAP_TYPE = (__ + ".bigDecimalHashMapType").intern();

        String STRING_VECTOR_TYPE = (__ + ".stringVectorType").intern();

        String STRING_CONCURRENT_HASH_MAP_TYPE = (__ + ".stringConcurrentHashMapType").intern();

        String JSON_TYPE = (__ + ".jsonType").intern();

        String XML_TYPE = (__ + ".xmlType").intern();

        List<String> _PNL = Collections.unmodifiableList(Arrays.asList(BYTE_TYPE, CHAR_TYPE, BOOLEAN_TYPE, SHORT_TYPE, INT_TYPE, LONG_TYPE, FLOAT_TYPE,
                DOUBLE_TYPE, BIG_INTEGER_TYPE, BIG_DECIMAL_TYPE, STRING_TYPE, BYTE_ARRAY_TYPE, CHARACTER_STREAM_TYPE, BINARY_STREAM_TYPE, CLOB_TYPE, BLOB_TYPE,
                DATE_TYPE, TIME_TYPE, TIMESTAMP_TYPE, LONG_DATE_TYPE, LONG_TIME_TYPE, LONG_TIMESTAMP_TYPE, ENUM_TYPE, STRING_ARRAY_LIST_TYPE,
                BOOLEAN_LINKED_LIST_TYPE, DOUBLE_LIST_TYPE, DATE_ARRAY_LIST_TYPE, TIMESTAMP_ARRAY_LIST_TYPE, BIG_DECIMAL_ARRAY_LIST_TYPE, STRING_HASH_SET_TYPE,
                BOOLEAN_LINKED_HASH_SET_TYPE, DATE_HASH_SET_TYPE, TIMESTAMP_HASH_SET_TYPE, BIG_DECIMAL_HASH_SET_TYPE, STRING_HASH_MAP_TYPE,
                BOOLEAN_LINKED_HASH_MAP_TYPE, FLOAT_HASH_MAP_TYPE, DATE_HASH_MAP_TYPE, TIMESTAMP_HASH_MAP_TYPE, BIG_DECIMAL_HASH_MAP_TYPE, STRING_VECTOR_TYPE,
                STRING_CONCURRENT_HASH_MAP_TYPE, JSON_TYPE, XML_TYPE));

        List<String> _CNL = Collections.unmodifiableList(Arrays.asList("data_type.bytetype".intern(), "data_type.chartype".intern(),
                "data_type.booleantype".intern(), "data_type.shorttype".intern(), "data_type.inttype".intern(), "data_type.longtype".intern(),
                "data_type.floattype".intern(), "data_type.doubletype".intern(), "data_type.bigIntegerType".intern(), "data_type.bigdecimaltype".intern(),
                "data_type.stringtype".intern(), "data_type.bytearraytype".intern(), "data_type.characterstreamtype".intern(),
                "data_type.binarystreamtype".intern(), "data_type.clobtype".intern(), "data_type.blobtype".intern(), "data_type.datetype".intern(),
                "data_type.timetype".intern(), "data_type.timestamptype".intern(), "data_type.longDateType".intern(), "data_type.longTimeType".intern(),
                "data_type.longTimestampType".intern(), "data_type.enumType".intern(), "data_type.stringarraylisttype".intern(),
                "data_type.booleanlinkedlisttype".intern(), "data_type.doublearraylisttype".intern(), "data_type.datearraylisttype".intern(),
                "data_type.timestamparraylisttype".intern(), "data_type.bigdecimalarraylisttype".intern(), "data_type.stringhashsettype".intern(),
                "data_type.booleanlinkedhashsettype".intern(), "data_type.datehashsettype".intern(), "data_type.timestamphashsettype".intern(),
                "data_type.bigdecimalhashsettype".intern(), "data_type.stringhashmaptype".intern(), "data_type.booleanlinkedhashmaptype".intern(),
                "data_type.floathashmaptype".intern(), "data_type.datehashmaptype".intern(), "data_type.timestamphashmaptype".intern(),
                "data_type.bigdecimalhashmaptype".intern(), "data_type.stringvectortype".intern(), "data_type.stringconcurrenthashmaptype".intern(),
                "data_type.jsonType".intern(), "data_type.xmlType".intern()));
    }

    public interface LoginPNL {
        String __ = "Login".intern();

        String ID = (__ + ".id").intern();

        String ACCOUNT_ID = (__ + ".accountId").intern();

        String LOGIN_ID = (__ + ".loginId").intern();

        String LOGIN_PASSWORD = (__ + ".loginPassword").intern();

        String STATUS = (__ + ".status").intern();

        String LAST_UPDATE_TIME = (__ + ".lastUpdateTime").intern();

        String CREATE_TIME = (__ + ".createdTime").intern();

        List<String> _PNL = Collections.unmodifiableList(Arrays.asList(ID, ACCOUNT_ID, LOGIN_ID, LOGIN_PASSWORD, STATUS, LAST_UPDATE_TIME, CREATE_TIME));

        List<String> _CNL = Collections.unmodifiableList(Arrays.asList("login.id".intern(), "login.account_id".intern(), "login.login_id".intern(),
                "login.login_password".intern(), "login.status".intern(), "login.last_update_time".intern(), "login.create_time".intern()));
    }

    String ACCOUNT_ID = "accountId".intern();

    String ADDRESS = "address".intern();

    String ADDRESS_2 = "address2".intern();

    String BIG_DECIMAL_ARRAY_LIST_TYPE = "bigDecimalArrayListType".intern();

    String BIG_DECIMAL_HASH_MAP_TYPE = "bigDecimalHashMapType".intern();

    String BIG_DECIMAL_HASH_SET_TYPE = "bigDecimalHashSetType".intern();

    String BIG_DECIMAL_TYPE = "bigDecimalType".intern();

    String BIG_INTEGER_TYPE = "bigIntegerType".intern();

    String BINARY_STREAM_TYPE = "binaryStreamType".intern();

    String BIRTH_DATE = "birthDate".intern();

    String BLOB_TYPE = "blobType".intern();

    String BOOLEAN_LINKED_HASH_MAP_TYPE = "booleanLinkedHashMapType".intern();

    String BOOLEAN_LINKED_HASH_SET_TYPE = "booleanLinkedHashSetType".intern();

    String BOOLEAN_LINKED_LIST_TYPE = "booleanLinkedListType".intern();

    String BOOLEAN_TYPE = "booleanType".intern();

    String BYTE_ARRAY_TYPE = "byteArrayType".intern();

    String BYTE_TYPE = "byteType".intern();

    String CATEGORY = "category".intern();

    String CHARACTER_STREAM_TYPE = "characterStreamType".intern();

    String CHAR_TYPE = "charType".intern();

    String CITY = "city".intern();

    String CLOB_TYPE = "clobType".intern();

    String CONTACT = "contact".intern();

    String COUNTRY = "country".intern();

    String CREATE_TIME = "createdTime".intern();

    String DATE_ARRAY_LIST_TYPE = "dateArrayListType".intern();

    String DATE_HASH_MAP_TYPE = "dateHashMapType".intern();

    String DATE_HASH_SET_TYPE = "dateHashSetType".intern();

    String DATE_TYPE = "dateType".intern();

    String DESCRIPTION = "description".intern();

    String DEVICES = "devices".intern();

    String DOUBLE_LIST_TYPE = "doubleListType".intern();

    String DOUBLE_TYPE = "doubleType".intern();

    String EMAIL = "email".intern();

    String EMAIL_ADDRESS = "emailAddress".intern();

    String ENUM_TYPE = "enumType".intern();

    String FIRST_NAME = "firstName".intern();

    String FLOAT_HASH_MAP_TYPE = "floatHashMapType".intern();

    String FLOAT_TYPE = "floatType".intern();

    String GROUP_GUI = "groupGUI".intern();

    String GROUP_LIST = "groupList".intern();

    String GUI = "gui".intern();

    String ID = "id".intern();

    String INT_TYPE = "intType".intern();

    String JSON_TYPE = "jsonType".intern();

    String LAST_NAME = "lastName".intern();

    String LAST_UPDATE_TIME = "lastUpdateTime".intern();

    String LOGIN_ID = "loginId".intern();

    String LOGIN_PASSWORD = "loginPassword".intern();

    String LONG_DATE_TYPE = "longDateType".intern();

    String LONG_TIMESTAMP_TYPE = "longTimestampType".intern();

    String LONG_TIME_TYPE = "longTimeType".intern();

    String LONG_TYPE = "longType".intern();

    String MANUFACTURER = "manufacturer".intern();

    String MIDDLE_NAME = "middleName".intern();

    String MOBILE = "mobile".intern();

    String MODEL = "model".intern();

    String NAME = "name".intern();

    String PLATFORM = "platform".intern();

    String PRIVILEGE = "privilege".intern();

    String PRODUCE_TIME = "produceTime".intern();

    String SHORT_TYPE = "shortType".intern();

    String STATE = "state".intern();

    String STATUS = "status".intern();

    String STRING_ARRAY_LIST_TYPE = "stringArrayListType".intern();

    String STRING_CONCURRENT_HASH_MAP_TYPE = "stringConcurrentHashMapType".intern();

    String STRING_HASH_MAP_TYPE = "stringHashMapType".intern();

    String STRING_HASH_SET_TYPE = "stringHashSetType".intern();

    String STRING_TYPE = "stringType".intern();

    String STRING_VECTOR_TYPE = "stringVectorType".intern();

    String SUB_CATEGORY = "subCategory".intern();

    String SUB_TYPE = "subType".intern();

    String TARGET_GUI = "targetGui".intern();

    String TELEPHONE = "telephone".intern();

    String TIMESTAMP_ARRAY_LIST_TYPE = "timestampArrayListType".intern();

    String TIMESTAMP_HASH_MAP_TYPE = "timestampHashMapType".intern();

    String TIMESTAMP_HASH_SET_TYPE = "timestampHashSetType".intern();

    String TIMESTAMP_TYPE = "timestampType".intern();

    String TIME_TYPE = "timeType".intern();

    String TYPE = "type".intern();

    String UDID = "UDID".intern();

    String UG_GUI = "ugGui".intern();

    String USER_GUI = "userGUI".intern();

    String USER_LIST = "userList".intern();

    String XML_TYPE = "xmlType".intern();

    String ZIP_CODE = "zipCode".intern();
}
