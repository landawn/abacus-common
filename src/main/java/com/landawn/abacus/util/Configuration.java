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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@SuppressWarnings({ "java:S1694" })
public abstract class Configuration {

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    private static final String CVS_NAME = ".cvs";

    private static final String SVN_NAME = ".svn";

    private static final String GIT_NAME = ".git";

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

    private static final Map<String, String> configFilePathPool = new ConcurrentHashMap<>();

    protected final Map<String, String> attrs = new HashMap<>();

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
     * @param element
     * @param props
     */
    protected Configuration(final Element element, final Map<String, String> props) {
        if (props != null) {
            this.props.putAll(props);
        }

        init(); // NOSONAR

        if (element != null) {
            for (final Map.Entry<String, String> entry : XMLUtil.readAttributes(element).entrySet()) {
                setAttribute(entry.getKey(), entry.getValue()); // NOSONAR
            }

            final NodeList childNodeList = element.getChildNodes();

            for (int childNodeIndex = 0; childNodeIndex < childNodeList.getLength(); childNodeIndex++) {
                final Node childNode = childNodeList.item(childNodeIndex);

                if (childNode instanceof Element) {
                    if (isTextElement(childNode)) {
                        final String attrName = childNode.getNodeName();
                        final String attrValue = getTextContent(childNode);
                        setAttribute(attrName, attrValue); // NOSONAR
                    } else {
                        complexElement2Attr((Element) childNode); // NOSONAR
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
     *
     * @param element
     */
    protected void complexElement2Attr(final Element element) {
        throw new RuntimeException("Unknow element: " + element.getNodeName());
    }

    /**
     * Gets the source code location.
     *
     * @param clazz
     * @return
     */
    public static String getSourceCodeLocation(final Class<?> clazz) {
        return clazz.getProtectionDomain().getCodeSource().getLocation().getPath().replace("%20", " "); //NOSONAR
    }

    /**
     * Gets the common config path.
     *
     * @return
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

        for (final String path : COMMON_CONFIG_PATH) {
            file = new File(currentLocation + path);

            if (file.exists() && file.isDirectory()) {
                result.add(file.getAbsolutePath());
            }
        }

        for (final String path : COMMON_CONFIG_PATH) {
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
     * @param configDir
     * @return File
     */
    public static File findDir(final String configDir) {
        return findFile(configDir, true, null);
    }

    /**
     * All the folders named with '.cvs', '.svn', '.git' will be ignored in file/folder find/search method.
     *
     * @param configFileName
     * @return File
     */
    public static File findFile(final String configFileName) {
        return findFile(configFileName, false, null);
    }

    /**
     *
     * @param configFileName
     * @param isDir
     * @param foundDir
     * @return File
     */
    private static File findFile(final String configFileName, final boolean isDir, Set<String> foundDir) {
        if (Strings.isEmpty(configFileName)) {
            throw new RuntimeException("target file name can't be empty or null: " + configFileName);
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

        final String cachedPath = configFilePathPool.get(configFileName);

        if (cachedPath != null) {
            final File file = new File(cachedPath);

            if (file.exists()) {
                return file;
            } else {
                configFilePathPool.remove(configFileName);
            }
        }

        String folderPrefix = null;
        String simpleConfigFileName = configFileName.trim().replace('\\', File.separatorChar).replace('/', File.separatorChar);

        final int index = simpleConfigFileName.lastIndexOf(File.separatorChar);

        if (index > -1) {
            folderPrefix = simpleConfigFileName.substring(0, index);
            folderPrefix = folderPrefix.charAt(0) == '.' ? folderPrefix.substring(1) : folderPrefix;
            folderPrefix = folderPrefix.replaceAll("\\.\\.\\" + File.separatorChar, "");

            simpleConfigFileName = simpleConfigFileName.substring(index + 1);
        }

        if (foundDir == null) {
            foundDir = N.newHashSet();
        }

        for (final String configPath : Configuration.getCommonConfigPath()) {
            configurationFile = findFileInDir(folderPrefix, simpleConfigFileName, new File(configPath), isDir, foundDir);

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

        final File dir = new File(IOUtil.CURRENT_DIR);

        if (logger.isInfoEnabled()) {
            logger.info("start to find simplified file: '" + simpleConfigFileName + "' from source path: '" + dir.getAbsolutePath()
                    + "'. current folder identified by './' is: '" + IOUtil.CURRENT_DIR + "'.");
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
     * @param srcFile
     * @param file
     * @return
     */
    public static File findFileByFile(final File srcFile, final String file) {
        File targetFile = new File(file);

        if (!targetFile.exists()) {
            if ((srcFile != null) && srcFile.exists()) {
                targetFile = findFileInDir(file, srcFile.getParentFile(), false);
            }

            if (targetFile == null || !targetFile.exists()) {
                return findFile(file);
            }
        }

        return targetFile;
    }

    /**
     * Find file in dir.
     *
     * @param configFileName
     * @param dir
     * @param isDir
     * @return
     */
    public static File findFileInDir(final String configFileName, final File dir, final boolean isDir) {
        if (Strings.isEmpty(configFileName)) {
            throw new RuntimeException("target file name can't be empty or null: " + configFileName);
        }

        String folderPrefix = null;
        String simpleConfigFileName = configFileName.trim().replace('\\', File.separatorChar).replace('/', File.separatorChar);

        final int index = simpleConfigFileName.lastIndexOf(File.separatorChar);

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
     * @param folderPrefix
     * @param configFileName
     * @param dir
     * @param isDir
     * @param foundDir
     * @return
     */
    private static File findFileInDir(final String folderPrefix, final String configFileName, File dir, final boolean isDir, Set<String> foundDir) {
        if (dir == null) {
            return null;
        }

        dir = Configuration.formatPath(dir);

        if (foundDir == null) {
            foundDir = N.newHashSet();
        } else if (foundDir.contains(dir.getAbsolutePath())) {
            return null;
        }

        foundDir.add(dir.getAbsolutePath());

        final String absolutePath = dir.getAbsolutePath().replace("%20", " "); //NOSONAR

        if (logger.isInfoEnabled()) {
            logger.info("finding file [" + configFileName + "] in directory [" + absolutePath + "] ...");
        }

        if (SVN_NAME.equals(dir.getName()) || GIT_NAME.equals(dir.getName()) || CVS_NAME.equals(dir.getName())) {
            return null;
        }

        final File[] files = dir.listFiles();

        if ((files == null) || (files.length == 0)) {
            return null;
        }

        if (Strings.isEmpty(folderPrefix) || ((absolutePath.length() > folderPrefix.length())
                && absolutePath.substring(absolutePath.length() - folderPrefix.length()).equalsIgnoreCase(folderPrefix))) {
            for (final File file : files) {
                if (file.getName().equalsIgnoreCase(configFileName)) {
                    if ((isDir && file.isDirectory()) || (!isDir && !file.isDirectory())) { //NOSONAR

                        if (logger.isWarnEnabled()) {
                            logger.warn("found file [" + file.getAbsolutePath() + "]");
                        }

                        return file;
                    }
                }
            }
        }

        for (final File file : files) {
            if (file.isDirectory() && !foundDir.contains(file.getAbsolutePath())) {
                final File result = findFileInDir(folderPrefix, configFileName, file, isDir, foundDir);

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
     * @return
     */
    private static File getCurrrentSourceCodeLocation() {
        File dir = new File(getSourceCodeLocation(Configuration.class));

        if (dir.isFile() && dir.getParentFile().exists()) {
            dir = dir.getParentFile();
        }

        final String path = dir.getAbsolutePath().replace('\\', '/');

        // if the class/library is loaded from local maven repository.
        if (path.indexOf("/.m2/repository/com/landawn/abacus-common/") > 0 || path.indexOf("/.m2/repository/com/landawn/abacus-common-se/") > 0 //NOSONAR
                || path.indexOf("/.m2/repository/com/landawn/abacus-common-se-jdk7/") > 0) { //NOSONAR
            return new File(IOUtil.CURRENT_DIR);
        }

        return dir;
    }

    /**
     *
     * @param file
     * @return Document
     */
    public static Document parse(final File file) {
        try {
            return XMLUtil.createDOMParser(true, true).parse(file);
        } catch (final SAXException e) {
            throw new ParseException(e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param is
     * @return Document
     */
    public static Document parse(final InputStream is) {
        try {
            return XMLUtil.createDOMParser(true, true).parse(is);
        } catch (final SAXException e) {
            throw new ParseException(e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param file
     * @return
     */
    public static File formatPath(File file) {
        if (!file.exists() && (new File(file.getAbsolutePath().replace("%20", " "))).exists()) { //NOSONAR
            file = new File(file.getAbsolutePath().replace("%20", " ")); //NOSONAR
        }

        return file;
    }

    /**
     *
     * @param node
     * @return boolean
     */
    public static boolean isTextElement(final Node node) {
        final NodeList childNodeList = node.getChildNodes();

        for (int i = 0; i < childNodeList.getLength(); i++) {
            if (childNodeList.item(i).getNodeType() == Document.ELEMENT_NODE) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param node
     * @return String
     */
    public static String getTextContent(final Node node) {
        return XMLUtil.getTextContent(node, true);
    }

    /**
     *
     * @param element
     * @return
     */
    public static Map<String, String> readElement(final Element element) {
        final Map<String, String> result = XMLUtil.readAttributes(element);

        if (isTextElement(element)) {
            final String attrName = element.getNodeName();
            final String attrValue = getTextContent(element);
            result.put(attrName, attrValue);
        }

        final NodeList childNodeList = element.getChildNodes();

        for (int childNodeIndex = 0; childNodeIndex < childNodeList.getLength(); childNodeIndex++) {
            final Node childNode = childNodeList.item(childNodeIndex);

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
     * @param value
     * @return
     */
    public static long readTimeInMillis(String value) {
        value = value == null ? value : value.trim();

        if (Strings.isEmpty(value)) {
            return 0;
        }

        final char lastChar = value.charAt(value.length() - 1);

        if (lastChar == 'l' || lastChar == 'L') {
            value = value.substring(0, value.length() - 1);
        }

        long multiple = 1L;

        if (value.endsWith("ms") || value.endsWith("MS")) {
            value = value.substring(0, value.length() - 2);
        } else if (value.endsWith("s") || value.endsWith("S")) {
            value = value.substring(0, value.length() - 1);
            multiple = 1000L;
        } else if (value.endsWith("m") || value.endsWith("M")) {
            value = value.substring(0, value.length() - 1);
            multiple = 60 * 1000L;
        } else if (value.endsWith("h") || value.endsWith("H")) {
            value = value.substring(0, value.length() - 1);
            multiple = 60 * 60 * 1000L;
        } else if (value.endsWith("d") || value.endsWith("D")) {
            value = value.substring(0, value.length() - 1);
            multiple = 24 * 60 * 60 * 1000L;
        } else if (value.endsWith("w") || value.endsWith("W")) {
            value = value.substring(0, value.length() - 1);
            multiple = 7 * 24 * 60 * 60 * 1000L;
        }

        long timeInMilliSeconds = 0;

        if (value.contains(WD.ASTERISK)) {
            long result = 1;
            final String[] strs = Splitter.with(Strings.BACKSLASH_ASTERISK).splitToArray(value);

            for (final String str : strs) {
                result *= Numbers.toLong(str.trim());
            }

            timeInMilliSeconds = result;
        } else {
            timeInMilliSeconds = Numbers.toLong(value);
        }

        return timeInMilliSeconds * multiple;
    }

    /**
     *
     * @return Collection<String>
     */
    public Collection<String> getAttrNames() {
        return attrs.keySet();
    }

    /**
     *
     * @param attrName
     * @return String
     */
    public String getAttribute(final String attrName) {
        return attrs.get(attrName);
    }

    /**
     *
     * @param attrName
     * @return boolean
     */
    public boolean hasAttribute(final String attrName) {
        return attrs.containsKey(attrName);
    }

    /**
     * Gets the attributes.
     *
     * @return
     */
    public Map<String, String> getAttributes() {
        return attrs;
    }

    /**
     *
     * @param attrName
     * @param attrValue
     * @return String
     */
    protected String setAttribute(final String attrName, String attrValue) {
        if (attrValue != null) {
            String attrVal = attrValue;
            attrVal = attrVal.trim();

            if ((props.size() > 0) && attrVal.startsWith("${") && attrVal.endsWith("}")) {
                attrVal = props.get(attrVal.substring(2, attrVal.length() - 1));

                if (attrVal != null) {
                    attrValue = attrVal;
                }
            }
        }

        return attrs.put(attrName, attrValue);
    }

    /**
     *
     * @param attrName
     * @return String
     */
    protected String removeAttribute(final String attrName) {
        return attrs.remove(attrName);
    }

    /**
     * String 2 array.
     *
     * @param st
     * @return
     */
    protected String[] string2Array(final String st) {
        return Splitter.with(WD.COMMA).trimResults().splitToArray(st);
    }

    /**
     * String 2 list.
     *
     * @param st
     * @return
     */
    protected List<String> string2List(final String st) {
        return Splitter.with(WD.COMMA).trimResults().split(st);
    }

    /**
     * String 2 set.
     *
     * @param st
     * @return
     */
    protected Set<String> string2Set(final String st) {
        final Set<String> result = N.newHashSet();

        Splitter.with(WD.COMMA).trimResults().split(st, result);

        return result;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return attrs.hashCode();
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof Configuration && N.equals(((Configuration) obj).attrs, attrs));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return attrs.toString();
    }
}
