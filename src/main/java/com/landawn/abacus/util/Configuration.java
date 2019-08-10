/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

// TODO: Auto-generated Javadoc
/**
 * The Class Configuration.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class Configuration {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    /** The Constant CVS_NAME. */
    private static final String CVS_NAME = ".cvs";

    /** The Constant SVN_NAME. */
    private static final String SVN_NAME = ".svn";

    /** The Constant GIT_NAME. */
    private static final String GIT_NAME = ".git";

    /** The Constant COMMON_CONFIG_PATH. */
    private static final List<String> COMMON_CONFIG_PATH = new ArrayList<>();

    static {
        COMMON_CONFIG_PATH.add("./config");
        COMMON_CONFIG_PATH.add("./conf");
        COMMON_CONFIG_PATH.add("./classes");
        COMMON_CONFIG_PATH.add("./classes/config");
        COMMON_CONFIG_PATH.add("./classes/conf");
        COMMON_CONFIG_PATH.add("./target/classes/config");
        COMMON_CONFIG_PATH.add("./target/classes/conf");
        COMMON_CONFIG_PATH.add("./build/classes/config");
        COMMON_CONFIG_PATH.add("./build/classes/conf");
        COMMON_CONFIG_PATH.add("./bin/classes/config");
        COMMON_CONFIG_PATH.add("./bin/classes/conf");
        COMMON_CONFIG_PATH.add("./target/classes");
        COMMON_CONFIG_PATH.add("./build/classes");
        COMMON_CONFIG_PATH.add("./bin/classes");
        COMMON_CONFIG_PATH.add("./../config");
        COMMON_CONFIG_PATH.add("./../conf");
        COMMON_CONFIG_PATH.add("./../classes");
        COMMON_CONFIG_PATH.add("./../classes/config");
        COMMON_CONFIG_PATH.add("./../classes/conf");
        COMMON_CONFIG_PATH.add("./../target/classes/config");
        COMMON_CONFIG_PATH.add("./../target/classes/conf");
        COMMON_CONFIG_PATH.add("./../build/classes/config");
        COMMON_CONFIG_PATH.add("./../build/classes/conf");
        COMMON_CONFIG_PATH.add("./../bin/classes/config");
        COMMON_CONFIG_PATH.add("./../bin/classes/conf");
        COMMON_CONFIG_PATH.add("./../target/classes");
        COMMON_CONFIG_PATH.add("./../build/classes");
        COMMON_CONFIG_PATH.add("./../bin/classes");

        COMMON_CONFIG_PATH.add("./resources/config");
        COMMON_CONFIG_PATH.add("./resources/conf");
        COMMON_CONFIG_PATH.add("./resources");
        COMMON_CONFIG_PATH.add("./../resources/config");
        COMMON_CONFIG_PATH.add("./../resources/conf");
        COMMON_CONFIG_PATH.add("./../resources");
    }

    /** The Constant configFilePathPool. */
    private static final Map<String, String> configFilePathPool = new ConcurrentHashMap<>();

    /** The attrs. */
    protected final Map<String, String> attrs = new HashMap<>();

    /** The props. */
    protected final Map<String, String> props = new HashMap<>();

    /**
     * Constructor for Configuration.
     */
    protected Configuration() {
        this(null, null);
    }

    /**
     * Constructor for Configuration.
     *
     * @param element the element
     * @param props the props
     */
    protected Configuration(Element element, Map<String, String> props) {
        if (props != null) {
            this.props.putAll(props);
        }

        init();

        if (element != null) {
            for (Map.Entry<String, String> entry : XMLUtil.readAttributes(element).entrySet()) {
                setAttribute(entry.getKey(), entry.getValue());
            }

            NodeList childNodeList = element.getChildNodes();

            for (int childNodeIndex = 0; childNodeIndex < childNodeList.getLength(); childNodeIndex++) {
                Node childNode = childNodeList.item(childNodeIndex);

                if (childNode instanceof Element) {
                    if (isTextElement(childNode)) {
                        String attrName = childNode.getNodeName();
                        String attrValue = getTextContent(childNode);
                        setAttribute(attrName, attrValue);
                    } else {
                        complexElement2Attr((Element) childNode);
                    }
                }
            }
        }
    }

    /**
     * Inits the.
     */
    protected void init() {
    }

    /**
     * Method element2Attr.
     *
     * @param element the element
     */
    protected void complexElement2Attr(Element element) {
        throw new AbacusException("Unknow element: " + element.getNodeName());
    }

    /**
     * Gets the source code location.
     *
     * @param clazz the clazz
     * @return the source code location
     */
    public static String getSourceCodeLocation(Class<?> clazz) {
        return clazz.getProtectionDomain().getCodeSource().getLocation().getPath().replaceAll("%20", " ");
    }

    /**
     * Gets the common config path.
     *
     * @return the common config path
     */
    public static List<String> getCommonConfigPath() {
        String currentLocation = getCurrrentSourceCodeLocation().getAbsolutePath();

        if (logger.isInfoEnabled()) {
            logger.info("current source location: " + currentLocation);
        }

        if (!(currentLocation.endsWith("/") || currentLocation.endsWith("\\"))) {
            currentLocation = currentLocation + File.separatorChar;
        }

        final List<String> result = new ArrayList<>();
        File file = null;

        for (String path : COMMON_CONFIG_PATH) {
            file = new File(currentLocation + path);

            if (file.exists() && file.isDirectory()) {
                result.add(file.getAbsolutePath());
            }
        }

        for (String path : COMMON_CONFIG_PATH) {
            file = new File(path);

            if (file.exists() && file.isDirectory()) {
                result.add(file.getAbsolutePath());
            }
        }

        return result;
    }

    /**
     * All the folders named with '.cvs', '.svn', '.git' will be ignored in file/folder find/search method.
     *
     * @param configDir the config dir
     * @return File
     */
    public static File findDir(String configDir) {
        return findFile(configDir, true, null);
    }

    /**
     * All the folders named with '.cvs', '.svn', '.git' will be ignored in file/folder find/search method.
     *
     * @param configFileName the config file name
     * @return File
     */
    public static File findFile(String configFileName) {
        return findFile(configFileName, false, null);
    }

    /**
     * Find file.
     *
     * @param configFileName the config file name
     * @param isDir the is dir
     * @param foundDir the found dir
     * @return File
     */
    private static File findFile(String configFileName, boolean isDir, Set<String> foundDir) {
        if (N.isNullOrEmpty(configFileName)) {
            throw new AbacusException("target file name can't be empty or null: " + configFileName);
        }

        if (logger.isInfoEnabled()) {
            logger.info("start to find file " + configFileName);
        }

        // find out configuration file
        File configurationFile = new File(configFileName);

        if (configurationFile.exists()) {
            if (logger.isWarnEnabled()) {
                logger.warn("found file " + configurationFile.getAbsolutePath());
            }

            return configurationFile;
        }

        String cachedPath = configFilePathPool.get(configFileName);

        if (cachedPath != null) {
            File file = new File(cachedPath);

            if (file.exists()) {
                return file;
            } else {
                configFilePathPool.remove(configFileName);
            }
        }

        String folderPrefix = null;
        String simpleConfigFileName = configFileName.trim().replace('\\', File.separatorChar).replace('/', File.separatorChar);

        int index = simpleConfigFileName.lastIndexOf(File.separatorChar);

        if (index > -1) {
            folderPrefix = simpleConfigFileName.substring(0, index);
            folderPrefix = folderPrefix.charAt(0) == '.' ? folderPrefix.substring(1) : folderPrefix;
            folderPrefix = folderPrefix.replaceAll("\\.\\.\\" + File.separatorChar, "");

            simpleConfigFileName = simpleConfigFileName.substring(index + 1);
        }

        if (foundDir == null) {
            foundDir = new HashSet<>();
        }

        for (String configPath : Configuration.getCommonConfigPath()) {
            findFileInDir(folderPrefix, simpleConfigFileName, new File(configPath), isDir, foundDir);

            if (configurationFile != null && configurationFile.exists()) {
                configFilePathPool.put(configFileName, configurationFile.getAbsolutePath());

                return configurationFile;
            }
        }

        //    File dir = getCurrrentSourceCodeLocation();
        //
        //    if (logger.isInfoEnabled()) {
        //        logger.info("start to find simplified file: '" + simpleConfigFileName + "' from source path: '" + dir.getAbsolutePath()
        //                + "'. current folder identified by './' is: '" + IOUtil.CURRENT_PATH + "'.");
        //    }
        //
        //    int i = 0;
        //
        //    do {
        //        configurationFile = findFileInDir(folderPrefix, simpleConfigFileName, dir, isDir, foundDir);
        //
        //        if (configurationFile != null && configurationFile.exists()) {
        //            configFilePathPool.put(configFileName, configurationFile.getAbsolutePath());
        //
        //            return configurationFile;
        //        }
        //
        //        dir = dir.getParentFile();
        //    } while (i++ < 3 && (dir != null) && !dir.getName().endsWith(File.separator));

        File dir = new File(IOUtil.CURRENT_PATH);

        if (logger.isInfoEnabled()) {
            logger.info("start to find simplified file: '" + simpleConfigFileName + "' from source path: '" + dir.getAbsolutePath()
                    + "'. current folder identified by './' is: '" + IOUtil.CURRENT_PATH + "'.");
        }

        configurationFile = findFileInDir(folderPrefix, simpleConfigFileName, dir, isDir, foundDir);

        if (configurationFile != null && configurationFile.exists()) {
            configFilePathPool.put(configFileName, configurationFile.getAbsolutePath());

            return configurationFile;
        }

        return null;
    }

    /**
     * Find file by file.
     *
     * @param srcFile the src file
     * @param file the file
     * @return the file
     */
    public static File findFileByFile(File srcFile, String file) {
        File targetFile = new File(file);

        if (targetFile.exists()) {
            return targetFile;
        } else {
            if ((srcFile != null) && srcFile.exists()) {
                targetFile = findFileInDir(file, srcFile.getParentFile(), false);
            }

            if (targetFile == null || !targetFile.exists()) {
                return findFile(file);
            }

            return targetFile;
        }
    }

    /**
     * Find file in dir.
     *
     * @param configFileName the config file name
     * @param dir the dir
     * @param isDir the is dir
     * @return the file
     */
    public static File findFileInDir(String configFileName, File dir, boolean isDir) {
        if (N.isNullOrEmpty(configFileName)) {
            throw new AbacusException("target file name can't be empty or null: " + configFileName);
        }

        String folderPrefix = null;
        String simpleConfigFileName = configFileName.trim().replace('\\', File.separatorChar).replace('/', File.separatorChar);

        int index = simpleConfigFileName.lastIndexOf(File.separatorChar);

        if (index > -1) {
            folderPrefix = simpleConfigFileName.substring(0, index);
            folderPrefix = folderPrefix.charAt(0) == '.' ? folderPrefix.substring(1) : folderPrefix;

            simpleConfigFileName = simpleConfigFileName.substring(index + 1);
        }

        return findFileInDir(folderPrefix, simpleConfigFileName, dir, isDir, null);
    }

    /**
     * Find file in dir.
     *
     * @param folderPrefix the folder prefix
     * @param configFileName the config file name
     * @param dir the dir
     * @param isDir the is dir
     * @param foundDir the found dir
     * @return the file
     */
    private static File findFileInDir(String folderPrefix, String configFileName, File dir, boolean isDir, Set<String> foundDir) {
        if (dir == null) {
            return null;
        }

        dir = Configuration.formatPath(dir);

        if (foundDir == null) {
            foundDir = new HashSet<>();
        } else if (foundDir.contains(dir.getAbsolutePath())) {
            return null;
        }

        foundDir.add(dir.getAbsolutePath());

        String absolutePath = dir.getAbsolutePath().replaceAll("%20", " ");

        if (logger.isInfoEnabled()) {
            logger.info("finding file [" + configFileName + "] in directory [" + ((dir == null) ? "null" : absolutePath) + "] ...");
        }

        if (SVN_NAME.equals(dir.getName()) || GIT_NAME.equals(dir.getName()) || CVS_NAME.equals(dir.getName())) {
            return null;
        }

        final File[] files = dir.listFiles();

        if ((files == null) || (files.length == 0)) {
            return null;
        }

        if (N.isNullOrEmpty(folderPrefix) || ((absolutePath.length() > folderPrefix.length())
                && absolutePath.substring(absolutePath.length() - folderPrefix.length()).equalsIgnoreCase(folderPrefix))) {
            for (File file : files) {
                if (file.getName().equalsIgnoreCase(configFileName)) {
                    if ((isDir && file.isDirectory()) || (!isDir && !file.isDirectory())) {

                        if (logger.isWarnEnabled()) {
                            logger.warn("found file [" + file.getAbsolutePath() + "]");
                        }

                        return file;
                    }
                }
            }
        }

        for (File file : files) {
            if (file.isDirectory() && !foundDir.contains(file.getAbsolutePath())) {
                File result = findFileInDir(folderPrefix, configFileName, file, isDir, foundDir);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Gets the currrent source code location.
     *
     * @return the currrent source code location
     */
    private static File getCurrrentSourceCodeLocation() {
        File dir = new File(getSourceCodeLocation(Configuration.class));

        if (dir.isFile() && dir.getParentFile().exists()) {
            dir = dir.getParentFile();
        }

        final String path = dir.getAbsolutePath().replace('\\', '/');

        // if the class/library is loaded from local maven repository.
        if (path.indexOf("/.m2/repository/com/landawn/abacus-util/") > 0 || path.indexOf("/.m2/repository/com/landawn/abacus-util-se/") > 0) {
            return new File(IOUtil.CURRENT_PATH);
        }

        return dir;
    }

    /**
     * Method newDocument.
     *
     * @param file the file
     * @return Document
     */
    public static Document parse(File file) {
        try {
            return XMLUtil.createDOMParser(true, true).parse(file);
        } catch (SAXException e) {
            throw new ParseException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Method createDocument.
     *
     * @param is the is
     * @return Document
     */
    public static Document parse(InputStream is) {
        try {
            return XMLUtil.createDOMParser(true, true).parse(is);
        } catch (SAXException e) {
            throw new ParseException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Format path.
     *
     * @param file the file
     * @return the file
     */
    public static File formatPath(File file) {
        if (!file.exists() && (new File(file.getAbsolutePath().replaceAll("%20", " "))).exists()) {
            file = new File(file.getAbsolutePath().replaceAll("%20", " "));
        }

        return file;
    }

    /**
     * Method isTextElement.
     *
     * @param node the node
     * @return boolean
     */
    public static boolean isTextElement(Node node) {
        NodeList childNodeList = node.getChildNodes();

        for (int i = 0; i < childNodeList.getLength(); i++) {
            if (childNodeList.item(i).getNodeType() == Document.ELEMENT_NODE) {
                return false;
            }
        }

        return true;
    }

    /**
     * Method getTextElementContent.
     *
     * @param node the node
     * @return String
     */
    public static String getTextContent(Node node) {
        return XMLUtil.getTextContent(node, true);
    }

    /**
     * Read element.
     *
     * @param element the element
     * @return the map
     */
    public static Map<String, String> readElement(Element element) {
        Map<String, String> result = XMLUtil.readAttributes(element);

        if (isTextElement(element)) {
            String attrName = element.getNodeName();
            String attrValue = getTextContent(element);
            result.put(attrName, attrValue);
        }

        NodeList childNodeList = element.getChildNodes();

        for (int childNodeIndex = 0; childNodeIndex < childNodeList.getLength(); childNodeIndex++) {
            Node childNode = childNodeList.item(childNodeIndex);

            if (childNode instanceof Element) {
                result.putAll(readElement((Element) childNode));
            }
        }

        return result;
    }

    /**
     * Supports the time property/attribute value which is multiplied by '*'.
     * For example: 3 * 1000 or 3 * 1000L
     *
     * @param value the value
     * @return the long
     */
    public static long readTimeValue(String value) {
        value = value == null ? value : value.trim();

        if (N.isNullOrEmpty(value)) {
            return 0;
        }

        char lastChar = value.charAt(value.length() - 1);
        if (lastChar == 'l' || lastChar == 'L') {
            value = value.substring(0, value.length() - 1);
        }

        if (value.contains(WD.ASTERISK)) {
            long result = 1;
            final String[] strs = Splitter.with(N.BACKSLASH_ASTERISK).splitToArray(value);

            for (String str : strs) {
                result *= N.parseLong(str.trim());
            }

            return result;
        } else {
            return N.parseLong(value);
        }
    }

    /**
     * Method getAttrNames.
     * 
     * @return Collection<String>
     */
    public Collection<String> getAttrNames() {
        return attrs.keySet();
    }

    /**
     * Method getAttribute.
     *
     * @param attrName the attr name
     * @return String
     */
    public String getAttribute(String attrName) {
        return attrs.get(attrName);
    }

    /**
     * Method hasAttribute.
     *
     * @param attrName the attr name
     * @return boolean
     */
    public boolean hasAttribute(String attrName) {
        return attrs.containsKey(attrName);
    }

    /**
     * Gets the attributes.
     *
     * @return the attributes
     */
    public Map<String, String> getAttributes() {
        return attrs;
    }

    /**
     * Method addAttribute.
     *
     * @param attrName the attr name
     * @param attrValue the attr value
     * @return String
     */
    protected String setAttribute(String attrName, String attrValue) {
        if (attrValue != null) {
            String var = attrValue;
            var = var.trim();

            if ((props.size() > 0) && var.startsWith("${") && var.endsWith("}")) {
                var = props.get(var.substring(2, var.length() - 1));

                if (var != null) {
                    attrValue = var;
                }
            }
        }

        return attrs.put(attrName, attrValue);
    }

    /**
     * Method removeAttribute.
     *
     * @param attrName the attr name
     * @return String
     */
    protected String removeAttribute(String attrName) {
        return attrs.remove(attrName);
    }

    /**
     * String 2 array.
     *
     * @param st the st
     * @return the string[]
     */
    protected String[] string2Array(String st) {
        return Splitter.with(WD.COMMA).trim(true).splitToArray(st);
    }

    /**
     * String 2 list.
     *
     * @param st the st
     * @return the list
     */
    protected List<String> string2List(String st) {
        return Splitter.with(WD.COMMA).trim(true).split(st);
    }

    /**
     * String 2 set.
     *
     * @param st the st
     * @return the sets the
     */
    protected Set<String> string2Set(String st) {
        return Splitter.with(WD.COMMA).trim(true).split(new HashSet<String>(), st);
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return attrs.hashCode();
    }

    /**
     * Equals.
     *
     * @param obj the obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof Configuration && N.equals(((Configuration) obj).attrs, attrs));
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return attrs.toString();
    }
}
