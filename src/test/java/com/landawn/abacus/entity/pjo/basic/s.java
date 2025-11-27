package com.landawn.abacus.entity.pjo.basic;

import java.util.List;

public interface s {

    String accountId = "accountId";

    String address = "address";

    String address2 = "address2";

    String bigDecimalArrayListType = "bigDecimalArrayListType";

    String bigDecimalHashMapType = "bigDecimalHashMapType";

    String bigDecimalHashSetType = "bigDecimalHashSetType";

    String bigDecimalType = "bigDecimalType";

    String bigIntegerType = "bigIntegerType";

    String binaryStreamType = "binaryStreamType";

    String birthDate = "birthDate";

    String blobType = "blobType";

    String booleanLinkedHashMapType = "booleanLinkedHashMapType";

    String booleanLinkedHashSetType = "booleanLinkedHashSetType";

    String booleanLinkedListType = "booleanLinkedListType";

    String booleanType = "booleanType";

    String byteArrayType = "byteArrayType";

    String byteType = "byteType";

    String category = "category";

    String charType = "charType";

    String characterStreamType = "characterStreamType";

    String city = "city";

    String clobType = "clobType";

    String contact = "contact";

    String country = "country";

    String createdTime = "createdTime";

    String dateArrayListType = "dateArrayListType";

    String dateHashMapType = "dateHashMapType";

    String dateHashSetType = "dateHashSetType";

    String dateType = "dateType";

    String description = "description";

    String devices = "devices";

    String doubleListType = "doubleListType";

    String doubleType = "doubleType";

    String email = "email";

    String emailAddress = "emailAddress";

    String enumType = "enumType";

    String firstName = "firstName";

    String floatHashMapType = "floatHashMapType";

    String floatType = "floatType";

    String groupGUI = "groupGUI";

    String groupList = "groupList";

    String gui = "gui";

    String id = "id";

    String intType = "intType";

    String jsonType = "jsonType";

    String lastName = "lastName";

    String lastUpdateTime = "lastUpdateTime";

    String loginId = "loginId";

    String loginPassword = "loginPassword";

    String longDateType = "longDateType";

    String longTimeType = "longTimeType";

    String longTimestampType = "longTimestampType";

    String longType = "longType";

    String manufacturer = "manufacturer";

    String middleName = "middleName";

    String mobile = "mobile";

    String model = "model";

    String name = "name";

    String platform = "platform";

    String privilege = "privilege";

    String produceTime = "produceTime";

    String shortType = "shortType";

    String state = "state";

    String status = "status";

    String stringArrayListType = "stringArrayListType";

    String stringConcurrentHashMapType = "stringConcurrentHashMapType";

    String stringHashMapType = "stringHashMapType";

    String stringHashSetType = "stringHashSetType";

    String stringType = "stringType";

    String stringVectorType = "stringVectorType";

    String subCategory = "subCategory";

    String subType = "subType";

    String targetGui = "targetGui";

    String telephone = "telephone";

    String timeType = "timeType";

    String timestampArrayListType = "timestampArrayListType";

    String timestampHashMapType = "timestampHashMapType";

    String timestampHashSetType = "timestampHashSetType";

    String timestampType = "timestampType";

    String type = "type";

    String udid = "udid";

    String ugGui = "ugGui";

    String userGUI = "userGUI";

    String userList = "userList";

    String xmlType = "xmlType";

    String zipCode = "zipCode";

    List<String> accountPropNameList = List.of(birthDate, contact, createdTime, devices, emailAddress, firstName, gui, id, lastName, lastUpdateTime, middleName,
            status);

    List<String> aclUgTargetRelationshipPropNameList = List.of(createdTime, description, id, lastUpdateTime, privilege, status, targetGui, ugGui);

    List<String> aclTargetPropNameList = List.of(category, createdTime, description, gui, id, lastUpdateTime, name, status, subCategory, subType, type);

    List<String> accountDevicePropNameList = List.of(accountId, category, createdTime, description, id, lastUpdateTime, manufacturer, model, name, platform,
            produceTime, status, udid);

    List<String> dataTypePropNameList = List.of(bigDecimalArrayListType, bigDecimalHashMapType, bigDecimalHashSetType, bigDecimalType, bigIntegerType,
            binaryStreamType, blobType, booleanLinkedHashMapType, booleanLinkedHashSetType, booleanLinkedListType, booleanType, byteArrayType, byteType,
            charType, characterStreamType, clobType, dateArrayListType, dateHashMapType, dateHashSetType, dateType, doubleListType, doubleType, enumType,
            floatHashMapType, floatType, intType, jsonType, longDateType, longTimeType, longTimestampType, longType, shortType, stringArrayListType,
            stringConcurrentHashMapType, stringHashMapType, stringHashSetType, stringType, stringVectorType, timeType, timestampArrayListType,
            timestampHashMapType, timestampHashSetType, timestampType, xmlType);

    List<String> aclUserPropNameList = List.of(createdTime, description, groupList, gui, id, lastUpdateTime, name, status);

    List<String> accountContactPropNameList = List.of(accountId, address, address2, category, city, country, createdTime, description, email, id,
            lastUpdateTime, mobile, state, status, telephone, zipCode);

    List<String> aclGroupPropNameList = List.of(createdTime, description, gui, id, lastUpdateTime, name, status, userList);

    List<String> loginPropNameList = List.of(accountId, createdTime, id, lastUpdateTime, loginId, loginPassword, status);

    List<String> aclUserGroupRelationshipPropNameList = List.of(createdTime, description, groupGUI, id, lastUpdateTime, status, userGUI);

    public interface sl {

        String accountId = "account_id";

        String address = "address";

        String address2 = "address2";

        String bigDecimalArrayListType = "big_decimal_array_list_type";

        String bigDecimalHashMapType = "big_decimal_hash_map_type";

        String bigDecimalHashSetType = "big_decimal_hash_set_type";

        String bigDecimalType = "big_decimal_type";

        String bigIntegerType = "big_integer_type";

        String binaryStreamType = "binary_stream_type";

        String birthDate = "birth_date";

        String blobType = "blob_type";

        String booleanLinkedHashMapType = "boolean_linked_hash_map_type";

        String booleanLinkedHashSetType = "boolean_linked_hash_set_type";

        String booleanLinkedListType = "boolean_linked_list_type";

        String booleanType = "boolean_type";

        String byteArrayType = "byte_array_type";

        String byteType = "byte_type";

        String category = "category";

        String charType = "char_type";

        String characterStreamType = "character_stream_type";

        String city = "city";

        String clobType = "clob_type";

        String contact = "contact";

        String country = "country";

        String createdTime = "created_time";

        String dateArrayListType = "date_array_list_type";

        String dateHashMapType = "date_hash_map_type";

        String dateHashSetType = "date_hash_set_type";

        String dateType = "date_type";

        String description = "description";

        String devices = "devices";

        String doubleListType = "double_list_type";

        String doubleType = "double_type";

        String email = "email";

        String emailAddress = "email_address";

        String enumType = "enum_type";

        String firstName = "first_name";

        String floatHashMapType = "float_hash_map_type";

        String floatType = "float_type";

        String groupGUI = "group_gui";

        String groupList = "group_list";

        String gui = "gui";

        String id = "id";

        String intType = "int_type";

        String jsonType = "json_type";

        String lastName = "last_name";

        String lastUpdateTime = "last_update_time";

        String loginId = "login_id";

        String loginPassword = "login_password";

        String longDateType = "long_date_type";

        String longTimeType = "long_time_type";

        String longTimestampType = "long_timestamp_type";

        String longType = "long_type";

        String manufacturer = "manufacturer";

        String middleName = "middle_name";

        String mobile = "mobile";

        String model = "model";

        String name = "name";

        String platform = "platform";

        String privilege = "privilege";

        String produceTime = "produce_time";

        String shortType = "short_type";

        String state = "state";

        String status = "status";

        String stringArrayListType = "string_array_list_type";

        String stringConcurrentHashMapType = "string_concurrent_hash_map_type";

        String stringHashMapType = "string_hash_map_type";

        String stringHashSetType = "string_hash_set_type";

        String stringType = "string_type";

        String stringVectorType = "string_vector_type";

        String subCategory = "sub_category";

        String subType = "sub_type";

        String targetGui = "target_gui";

        String telephone = "telephone";

        String timeType = "time_type";

        String timestampArrayListType = "timestamp_array_list_type";

        String timestampHashMapType = "timestamp_hash_map_type";

        String timestampHashSetType = "timestamp_hash_set_type";

        String timestampType = "timestamp_type";

        String type = "type";

        String udid = "udid";

        String ugGui = "ug_gui";

        String userGUI = "user_gui";

        String userList = "user_list";

        String xmlType = "xml_type";

        String zipCode = "zip_code";

        List<String> accountPropNameList = List.of(birthDate, contact, createdTime, devices, emailAddress, firstName, gui, id, lastName, lastUpdateTime,
                middleName, status);

        List<String> aclUgTargetRelationshipPropNameList = List.of(createdTime, description, id, lastUpdateTime, privilege, status, targetGui, ugGui);

        List<String> aclTargetPropNameList = List.of(category, createdTime, description, gui, id, lastUpdateTime, name, status, subCategory, subType, type);

        List<String> accountDevicePropNameList = List.of(accountId, category, createdTime, description, id, lastUpdateTime, manufacturer, model, name, platform,
                produceTime, status, udid);

        List<String> dataTypePropNameList = List.of(bigDecimalArrayListType, bigDecimalHashMapType, bigDecimalHashSetType, bigDecimalType, bigIntegerType,
                binaryStreamType, blobType, booleanLinkedHashMapType, booleanLinkedHashSetType, booleanLinkedListType, booleanType, byteArrayType, byteType,
                charType, characterStreamType, clobType, dateArrayListType, dateHashMapType, dateHashSetType, dateType, doubleListType, doubleType, enumType,
                floatHashMapType, floatType, intType, jsonType, longDateType, longTimeType, longTimestampType, longType, shortType, stringArrayListType,
                stringConcurrentHashMapType, stringHashMapType, stringHashSetType, stringType, stringVectorType, timeType, timestampArrayListType,
                timestampHashMapType, timestampHashSetType, timestampType, xmlType);

        List<String> aclUserPropNameList = List.of(createdTime, description, groupList, gui, id, lastUpdateTime, name, status);

        List<String> accountContactPropNameList = List.of(accountId, address, address2, category, city, country, createdTime, description, email, id,
                lastUpdateTime, mobile, state, status, telephone, zipCode);

        List<String> aclGroupPropNameList = List.of(createdTime, description, gui, id, lastUpdateTime, name, status, userList);

        List<String> loginPropNameList = List.of(accountId, createdTime, id, lastUpdateTime, loginId, loginPassword, status);

        List<String> aclUserGroupRelationshipPropNameList = List.of(createdTime, description, groupGUI, id, lastUpdateTime, status, userGUI);

    }

    public interface sau {

        String accountId = "ACCOUNT_ID";

        String address = "ADDRESS";

        String address2 = "ADDRESS2";

        String bigDecimalArrayListType = "BIG_DECIMAL_ARRAY_LIST_TYPE";

        String bigDecimalHashMapType = "BIG_DECIMAL_HASH_MAP_TYPE";

        String bigDecimalHashSetType = "BIG_DECIMAL_HASH_SET_TYPE";

        String bigDecimalType = "BIG_DECIMAL_TYPE";

        String bigIntegerType = "BIG_INTEGER_TYPE";

        String binaryStreamType = "BINARY_STREAM_TYPE";

        String birthDate = "BIRTH_DATE";

        String blobType = "BLOB_TYPE";

        String booleanLinkedHashMapType = "BOOLEAN_LINKED_HASH_MAP_TYPE";

        String booleanLinkedHashSetType = "BOOLEAN_LINKED_HASH_SET_TYPE";

        String booleanLinkedListType = "BOOLEAN_LINKED_LIST_TYPE";

        String booleanType = "BOOLEAN_TYPE";

        String byteArrayType = "BYTE_ARRAY_TYPE";

        String byteType = "BYTE_TYPE";

        String category = "CATEGORY";

        String charType = "CHAR_TYPE";

        String characterStreamType = "CHARACTER_STREAM_TYPE";

        String city = "CITY";

        String clobType = "CLOB_TYPE";

        String contact = "CONTACT";

        String country = "COUNTRY";

        String createdTime = "CREATED_TIME";

        String dateArrayListType = "DATE_ARRAY_LIST_TYPE";

        String dateHashMapType = "DATE_HASH_MAP_TYPE";

        String dateHashSetType = "DATE_HASH_SET_TYPE";

        String dateType = "DATE_TYPE";

        String description = "DESCRIPTION";

        String devices = "DEVICES";

        String doubleListType = "DOUBLE_LIST_TYPE";

        String doubleType = "DOUBLE_TYPE";

        String email = "EMAIL";

        String emailAddress = "EMAIL_ADDRESS";

        String enumType = "ENUM_TYPE";

        String firstName = "FIRST_NAME";

        String floatHashMapType = "FLOAT_HASH_MAP_TYPE";

        String floatType = "FLOAT_TYPE";

        String groupGUI = "GROUP_GUI";

        String groupList = "GROUP_LIST";

        String gui = "GUI";

        String id = "ID";

        String intType = "INT_TYPE";

        String jsonType = "JSON_TYPE";

        String lastName = "LAST_NAME";

        String lastUpdateTime = "LAST_UPDATE_TIME";

        String loginId = "LOGIN_ID";

        String loginPassword = "LOGIN_PASSWORD";

        String longDateType = "LONG_DATE_TYPE";

        String longTimeType = "LONG_TIME_TYPE";

        String longTimestampType = "LONG_TIMESTAMP_TYPE";

        String longType = "LONG_TYPE";

        String manufacturer = "MANUFACTURER";

        String middleName = "MIDDLE_NAME";

        String mobile = "MOBILE";

        String model = "MODEL";

        String name = "NAME";

        String platform = "PLATFORM";

        String privilege = "PRIVILEGE";

        String produceTime = "PRODUCE_TIME";

        String shortType = "SHORT_TYPE";

        String state = "STATE";

        String status = "STATUS";

        String stringArrayListType = "STRING_ARRAY_LIST_TYPE";

        String stringConcurrentHashMapType = "STRING_CONCURRENT_HASH_MAP_TYPE";

        String stringHashMapType = "STRING_HASH_MAP_TYPE";

        String stringHashSetType = "STRING_HASH_SET_TYPE";

        String stringType = "STRING_TYPE";

        String stringVectorType = "STRING_VECTOR_TYPE";

        String subCategory = "SUB_CATEGORY";

        String subType = "SUB_TYPE";

        String targetGui = "TARGET_GUI";

        String telephone = "TELEPHONE";

        String timeType = "TIME_TYPE";

        String timestampArrayListType = "TIMESTAMP_ARRAY_LIST_TYPE";

        String timestampHashMapType = "TIMESTAMP_HASH_MAP_TYPE";

        String timestampHashSetType = "TIMESTAMP_HASH_SET_TYPE";

        String timestampType = "TIMESTAMP_TYPE";

        String type = "TYPE";

        String udid = "UDID";

        String ugGui = "UG_GUI";

        String userGUI = "USER_GUI";

        String userList = "USER_LIST";

        String xmlType = "XML_TYPE";

        String zipCode = "ZIP_CODE";

        List<String> accountPropNameList = List.of(birthDate, contact, createdTime, devices, emailAddress, firstName, gui, id, lastName, lastUpdateTime,
                middleName, status);

        List<String> aclUgTargetRelationshipPropNameList = List.of(createdTime, description, id, lastUpdateTime, privilege, status, targetGui, ugGui);

        List<String> aclTargetPropNameList = List.of(category, createdTime, description, gui, id, lastUpdateTime, name, status, subCategory, subType, type);

        List<String> accountDevicePropNameList = List.of(accountId, category, createdTime, description, id, lastUpdateTime, manufacturer, model, name, platform,
                produceTime, status, udid);

        List<String> dataTypePropNameList = List.of(bigDecimalArrayListType, bigDecimalHashMapType, bigDecimalHashSetType, bigDecimalType, bigIntegerType,
                binaryStreamType, blobType, booleanLinkedHashMapType, booleanLinkedHashSetType, booleanLinkedListType, booleanType, byteArrayType, byteType,
                charType, characterStreamType, clobType, dateArrayListType, dateHashMapType, dateHashSetType, dateType, doubleListType, doubleType, enumType,
                floatHashMapType, floatType, intType, jsonType, longDateType, longTimeType, longTimestampType, longType, shortType, stringArrayListType,
                stringConcurrentHashMapType, stringHashMapType, stringHashSetType, stringType, stringVectorType, timeType, timestampArrayListType,
                timestampHashMapType, timestampHashSetType, timestampType, xmlType);

        List<String> aclUserPropNameList = List.of(createdTime, description, groupList, gui, id, lastUpdateTime, name, status);

        List<String> accountContactPropNameList = List.of(accountId, address, address2, category, city, country, createdTime, description, email, id,
                lastUpdateTime, mobile, state, status, telephone, zipCode);

        List<String> aclGroupPropNameList = List.of(createdTime, description, gui, id, lastUpdateTime, name, status, userList);

        List<String> loginPropNameList = List.of(accountId, createdTime, id, lastUpdateTime, loginId, loginPassword, status);

        List<String> aclUserGroupRelationshipPropNameList = List.of(createdTime, description, groupGUI, id, lastUpdateTime, status, userGUI);

    }

    public interface f {

        String min_address = "min(address)";

        String min_address2 = "min(address2)";

        String min_bigDecimalType = "min(bigDecimalType)";

        String min_bigIntegerType = "min(bigIntegerType)";

        String min_birthDate = "min(birthDate)";

        String min_category = "min(category)";

        String min_city = "min(city)";

        String min_country = "min(country)";

        String min_createdTime = "min(createdTime)";

        String min_dateType = "min(dateType)";

        String min_description = "min(description)";

        String min_email = "min(email)";

        String min_emailAddress = "min(emailAddress)";

        String min_enumType = "min(enumType)";

        String min_firstName = "min(firstName)";

        String min_groupGUI = "min(groupGUI)";

        String min_gui = "min(gui)";

        String min_lastName = "min(lastName)";

        String min_lastUpdateTime = "min(lastUpdateTime)";

        String min_loginId = "min(loginId)";

        String min_loginPassword = "min(loginPassword)";

        String min_manufacturer = "min(manufacturer)";

        String min_middleName = "min(middleName)";

        String min_mobile = "min(mobile)";

        String min_model = "min(model)";

        String min_name = "min(name)";

        String min_platform = "min(platform)";

        String min_produceTime = "min(produceTime)";

        String min_state = "min(state)";

        String min_stringType = "min(stringType)";

        String min_subCategory = "min(subCategory)";

        String min_subType = "min(subType)";

        String min_targetGui = "min(targetGui)";

        String min_telephone = "min(telephone)";

        String min_timeType = "min(timeType)";

        String min_timestampType = "min(timestampType)";

        String min_type = "min(type)";

        String min_udid = "min(udid)";

        String min_ugGui = "min(ugGui)";

        String min_userGUI = "min(userGUI)";

        String min_zipCode = "min(zipCode)";

        String max_address = "max(address)";

        String max_address2 = "max(address2)";

        String max_bigDecimalType = "max(bigDecimalType)";

        String max_bigIntegerType = "max(bigIntegerType)";

        String max_birthDate = "max(birthDate)";

        String max_category = "max(category)";

        String max_city = "max(city)";

        String max_country = "max(country)";

        String max_createdTime = "max(createdTime)";

        String max_dateType = "max(dateType)";

        String max_description = "max(description)";

        String max_email = "max(email)";

        String max_emailAddress = "max(emailAddress)";

        String max_enumType = "max(enumType)";

        String max_firstName = "max(firstName)";

        String max_groupGUI = "max(groupGUI)";

        String max_gui = "max(gui)";

        String max_lastName = "max(lastName)";

        String max_lastUpdateTime = "max(lastUpdateTime)";

        String max_loginId = "max(loginId)";

        String max_loginPassword = "max(loginPassword)";

        String max_manufacturer = "max(manufacturer)";

        String max_middleName = "max(middleName)";

        String max_mobile = "max(mobile)";

        String max_model = "max(model)";

        String max_name = "max(name)";

        String max_platform = "max(platform)";

        String max_produceTime = "max(produceTime)";

        String max_state = "max(state)";

        String max_stringType = "max(stringType)";

        String max_subCategory = "max(subCategory)";

        String max_subType = "max(subType)";

        String max_targetGui = "max(targetGui)";

        String max_telephone = "max(telephone)";

        String max_timeType = "max(timeType)";

        String max_timestampType = "max(timestampType)";

        String max_type = "max(type)";

        String max_udid = "max(udid)";

        String max_ugGui = "max(ugGui)";

        String max_userGUI = "max(userGUI)";

        String max_zipCode = "max(zipCode)";

    }

}
