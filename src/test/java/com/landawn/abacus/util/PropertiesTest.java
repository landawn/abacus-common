/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.properties.MyProperties;
import com.landawn.abacus.util.PropertiesUtil.ConfigBean;
import com.landawn.abacus.util.PropertiesUtil.Resource;
import com.landawn.abacus.util.PropertiesUtil.ResourceType;

public class PropertiesTest extends AbstractTest {

    @Test
    public void test_keyValue() {
        final Properties<String, String> properties = new Properties<>();
        properties.put("key1", "value1");
        N.println(properties);

        //    assertThrows(NullPointerException.class, () -> properties.put("key2", null));
        //    assertThrows(NullPointerException.class, () -> properties.put(null, "value1"));
        //    assertThrows(NullPointerException.class, () -> properties.put(null, null));
    }

    @Test
    public void test_configBean() {
        final ConfigBean bean = new ConfigBean();
        bean.setName("myTestConfig");
        bean.setContent("abc123");
        bean.setStatus(Status.ACTIVE);
        bean.setLastUpdateTime(Dates.currentTimestamp());
        bean.setCreatedTime(Dates.currentTimestamp());

        final ConfigBean bean2 = Beans.copy(bean);
        assertEquals(bean, bean2);
        final Set<ConfigBean> set = CommonUtil.asSet(bean);
        assertTrue(set.contains(bean2));

        N.println(bean);
    }

    //    @Test
    //    public void test_loadProperties_fromDB() throws Exception {
    //        File file = PropertiesUtil.findFile("./src/test/resources/jdbc.properties");
    //
    //        Properties<String, String> props0 = PropertiesUtil.load(file);
    //
    //        N.println(props0);
    //
    //        file = new File("./src/test/resources/jdbc2.xml");
    //        if (file.exists()) {
    //            file.delete();
    //        }
    //
    //        PropertiesUtil.storeToXml(props0, file, "jdbc", false);
    //
    //        N.println(IOUtil.readString(file));
    //
    //        ConfigBean bean = new ConfigBean();
    //        bean.setName("myTestXMLConfig");
    //        bean.setContent(IOUtil.readString(file));
    //        bean.setStatus(Status.ACTIVE);
    //        bean.setLastUpdateTime(DateUtil.currentTimestamp());
    //        bean.setCreatedTime(DateUtil.currentTimestamp());
    //
    //        String sql = "delete from config where name = ?";
    //        sqlExecutor.update(sql, bean.getName());
    //
    //        sql = "insert into config(name, content, last_update_time, create_time) values (#{name}, #{content}, #{lastUpdateTime}, #{createdTime})";
    //        sqlExecutor.insert(sql, bean);
    //
    //        sql = "update config set status = 'ACTIVE' where name = ?";
    //        sqlExecutor.update(sql, bean.getName());
    //
    //        sql = "select * from config where name = 'myTestXMLConfig'";
    //
    //        Properties<String, String> xmlProps = PropertiesUtil.loadFromXml(sqlExecutor, sql, true);
    //
    //        N.println(xmlProps);
    //
    //        String userName = xmlProps.get("user");
    //        xmlProps.set("user", "root2");
    //
    //        N.println(IOUtil.readString(file));
    //
    //        PropertiesUtil.storeToXml(xmlProps, file, "jdbc", true);
    //
    //        N.println(IOUtil.readString(file));
    //
    //        xmlProps.set("user", userName);
    //
    //        N.sleep(1000);
    //        sql = "update config set content = ? where name = ?";
    //        sqlExecutor.update(sql, IOUtil.readString(file), "myTestXMLConfig");
    //
    //        sql = "select * from config where name = ?";
    //        ConfigBean bean2 = sqlExecutor.findFirst(ConfigBean.class, sql, "myTestXMLConfig").get();
    //        N.println(bean2);
    //        N.println(bean2.getContent());
    //
    //        N.sleep(3000);
    //
    //        assertEquals("root2", xmlProps.get("user"));
    //    }
    //
    //    @Test
    //    public void test_loadProperties_fromDB_2() throws Exception {
    //        File file = PropertiesUtil.findFile("./src/test/resources/jdbc.properties");
    //
    //        Properties<String, String> props0 = PropertiesUtil.load(file);
    //
    //        N.println(props0);
    //
    //        file = new File("./src/test/resources/myJdbc.properties");
    //
    //        if (file.exists()) {
    //            file.delete();
    //        }
    //
    //        PropertiesUtil.store(props0, file, "nothing to say");
    //
    //        N.println(IOUtil.readString(file));
    //
    //        ConfigBean bean = new ConfigBean();
    //        bean.setName("myTestPropertiesConfig");
    //        bean.setContent(IOUtil.readString(file));
    //        bean.setStatus(Status.ACTIVE);
    //        bean.setLastUpdateTime(DateUtil.currentTimestamp());
    //        bean.setCreatedTime(DateUtil.currentTimestamp());
    //
    //        String sql = "delete from config where name = ?";
    //        sqlExecutor.update(sql, bean.getName());
    //
    //        sql = "insert into config(name, content, last_update_time, create_time) values (#{name}, #{content}, #{lastUpdateTime}, #{createdTime})";
    //        sqlExecutor.insert(sql, bean);
    //
    //        sql = "update config set status = 'ACTIVE' where name = ?";
    //        sqlExecutor.update(sql, bean.getName());
    //
    //        sql = "select * from config where name = 'myTestPropertiesConfig'";
    //
    //        Properties<String, String> dbProps = PropertiesUtil.load(sqlExecutor, sql, true);
    //
    //        N.println(dbProps);
    //
    //        String userName = dbProps.get("user");
    //        dbProps.set("user", "root2");
    //
    //        N.println(IOUtil.readString(file));
    //
    //        PropertiesUtil.store(dbProps, file, "nothing to say");
    //
    //        N.println(IOUtil.readString(file));
    //
    //        dbProps.set("user", userName);
    //
    //        N.sleep(1000);
    //        sql = "update config set content = ? where name = ?";
    //        sqlExecutor.update(sql, IOUtil.readString(file), "myTestPropertiesConfig");
    //
    //        sql = "select * from config where name = ?";
    //        ConfigBean bean2 = sqlExecutor.findFirst(ConfigBean.class, sql, "myTestPropertiesConfig").get();
    //        N.println(bean2);
    //        N.println(bean2.getContent());
    //
    //        N.sleep(3000);
    //
    //        assertEquals("root2", dbProps.get("user"));
    //
    //        file.delete();
    //    }

    @Test
    public void test_loadProperties() throws Exception {
        {
            File file = PropertiesUtil.findFile("./src/test/resources/jdbc.properties");

            final Properties<String, String> properties = PropertiesUtil.load(file);

            N.println(properties);

            file = new File("./src/test/resources/jdbc2.xml");
            if (file.exists()) {
                file.delete();
            }

            PropertiesUtil.storeToXml(properties, "jdbc", false, file);

            N.println(IOUtil.readAllToString(file));

            final Properties<String, Object> properties2 = PropertiesUtil.loadFromXml(file);

            N.println(properties2);

            assertEquals(properties, properties2);
        }

        {
            File file = PropertiesUtil.findFile("./src/test/resources/jdbc.properties");
            final Reader reader = new FileReader(file);

            final Properties<String, String> properties = PropertiesUtil.load(reader);
            reader.close();

            N.println(properties);

            file = new File("./src/test/resources/jdbc2.xml");
            if (file.exists()) {
                file.delete();
            }

            PropertiesUtil.storeToXml(properties, "jdbc", false, file);

            N.println(IOUtil.readAllToString(file));

            final Properties<String, Object> properties2 = PropertiesUtil.loadFromXml(file);

            N.println(properties2);

            assertEquals(properties, properties2);
        }

        //        if (file.exists()) {
        //            file.delete();
        //        }
    }

    @Test
    public void test_xml2Java() throws Exception {
        N.println(int.class);

        File file = new File("./src/test/resources/myProperties.xml");
        final String srcPath = "./src/test/java";
        final String packageName = "com.landawn.abacus.properties";
        final String className = "MyProperties";
        PropertiesUtil.xml2Java(file, srcPath, packageName, className, false);
        final MyProperties myProperties = PropertiesUtil.loadFromXml(file, MyProperties.class);

        N.println(myProperties);

        file = new File("./src/test/resources/myProperties2.xml");
        if (file.exists()) {
            file.delete();
        }

        PropertiesUtil.storeToXml(myProperties, "myPropertiesNew", false, file);

        N.println(IOUtil.readAllToString(file));

        final Properties<String, Object> myProperties2 = PropertiesUtil.loadFromXml(file, Properties.class);
        N.println(myProperties2);

    }

    @Test
    public void test_autoRefresh() throws Exception {
        N.println(int.class);

        final File file = new File("./src/test/resources/myProperties.xml");
        final MyProperties myProperties = PropertiesUtil.loadFromXml(file, true, MyProperties.class);

        N.println(myProperties);

        final String oldValue = myProperties.getMProps4().getStrProp();
        N.println(oldValue);

        final String newValue = Strings.uuid();
        myProperties.getMProps4().setStrProp(newValue);

        N.sleep(1000);

        PropertiesUtil.storeToXml(myProperties, "MyProperties", false, file);
        myProperties.getMProps4().setStrProp(oldValue);

        N.sleep(3000);

        assertEquals(newValue, myProperties.getMProps4().getStrProp());

        myProperties.getMProps4().setStrProp(oldValue);
        PropertiesUtil.storeToXml(myProperties, "MyProperties", false, file);
        N.sleep(2000);

        assertEquals(oldValue, myProperties.getMProps4().getStrProp());
    }

    @Test
    public void test_Resource() throws Exception {
        final File file = PropertiesUtil.findFile("./src/test/resources/jdbc.properties");
        final Resource resource1 = new Resource(Properties.class, file, ResourceType.PROPERTIES);
        final Resource resource2 = new Resource(Properties.class, file, ResourceType.PROPERTIES);

        N.println(resource1);

        assertTrue(CommonUtil.asSet(resource1).contains(resource2));

    }

}
