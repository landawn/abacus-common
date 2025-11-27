package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.properties.MyProperties;
import com.landawn.abacus.util.PropertiesUtil.ConfigBean;
import com.landawn.abacus.util.PropertiesUtil.Resource;
import com.landawn.abacus.util.PropertiesUtil.ResourceType;

@Tag("old-test")
public class PropertiesTest extends AbstractTest {

    @Test
    public void test_keyValue() {
        final Properties<String, String> properties = new Properties<>();
        properties.put("key1", "value1");
        N.println(properties);

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
