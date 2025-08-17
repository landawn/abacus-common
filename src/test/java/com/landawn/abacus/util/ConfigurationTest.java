/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.landawn.abacus.AbstractTest;

public class ConfigurationTest extends AbstractTest {

    @Test
    public void testFindFile() {
        assertNotNull(Configuration.findFile("Configuration.java"));
    }

    @Test
    public void testReadWriteAttr() throws IOException {
        final File databaseXml = new File("./config/abacus-entity-manager.xml");
        Document doc = Configuration.parse(databaseXml);

        Map<String, String> elementMap = XmlUtil.readElement(doc.getDocumentElement());
        N.println(elementMap);

        final InputStream is = new FileInputStream(databaseXml);
        doc = Configuration.parse(is);
        is.close();

        final Configuration config = new Configuration(doc.getDocumentElement(), null) {
            @Override
            protected void complexElement2Attr(final Element element) {
                N.println(element.getTagName());
            }
        };

        N.println(config.toString());
        assertEquals(config, config);
    }

    @Test
    public void testGetCommonConfigPath() {
        String st = "..\\..\\abc\\abc.txt";
        N.println(st.replaceAll("\\.\\.\\" + File.separatorChar, ""));
        N.println(st.replaceAll("\\.\\.\\" + '\\', ""));
        N.println(st.replaceAll("\\.\\.\\" + '/', ""));

        st = "../../abc/abc.txt";
        N.println(st.replaceAll("\\.\\.\\" + File.separatorChar, ""));
        N.println(st.replaceAll("\\.\\.\\" + '\\', ""));
        N.println(st.replaceAll("\\.\\.\\" + '/', ""));

        N.println(Configuration.getCommonConfigPath());

        String path = "classes/com/landawn/abacus/EntityId.class";
        N.println(Configuration.findFile(path));

        path = "./abacus/EntityId.class";
        N.println(Configuration.findFile(path));

        path = "./../../abacus/EntityId.class";
        N.println(Configuration.findFile(path));

        path = "classes\\com\\landawn\\abacus\\EntityId.class";
        N.println(Configuration.findFile(path));

        path = ".\\abacus\\EntityId.class";
        N.println(Configuration.findFile(path));

        path = ".\\..\\..\\abacus\\EntityId.class";
        N.println(Configuration.findFile(path));
    }

    @Test
    public void testReadTimeValue() {
        N.println(Configuration.readTimeInMillis(null));
        N.println(Configuration.readTimeInMillis("9032802349890"));

        assertEquals(24 * 3600 * 1000, Configuration.readTimeInMillis("24 * 3600 * 1000"));
        assertEquals(30 * 7 * 24 * 3600 * 1000L, Configuration.readTimeInMillis("30* 7 * 24 * 3600 * 1000L"));
    }
}
