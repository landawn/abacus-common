/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
 * Abstract base class for configuration management that provides XML-based configuration loading
 * and attribute management functionality. This class supports loading configuration from XML files,
 * parsing XML elements into attributes, and provides utilities for finding configuration files
 * in various standard locations.
 * 
 * <p>Key features:
 * <ul>
 *   <li>XML configuration file parsing and loading</li>
 *   <li>Automatic configuration file discovery in standard locations</li>
 *   <li>Support for property placeholders in configuration values</li>
 *   <li>Time duration parsing with unit suffixes (ms, s, m, h, d, w)</li>
 *   <li>Hierarchical attribute management</li>
 * </ul>
 * 
 * <p>Configuration files are searched in the following standard locations:
 * <ul>
 *   <li>./config, ./conf, ./classes</li>
 *   <li>./target/classes, ./build/classes, ./bin/classes</li>
 *   <li>../config, ../conf, ../classes</li>
 *   <li>./resources, ../resources</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * public class MyConfig extends Configuration {
 *     public MyConfig(Element element) {
 *         super(element, null);
 *     }
 *     
 *     public String getDatabaseUrl() {
 *         return getAttribute("database.url");
 *     }
 *     
 *     public long getTimeout() {
 *         return readTimeInMillis(getAttribute("timeout"));
 *     }
 * }
 * 
 * // Load configuration
 * File configFile = Configuration.findFile("myapp.xml");
 * Document doc = Configuration.parse(configFile);
 * MyConfig config = new MyConfig(doc.getDocumentElement());
 * }</pre>
 * 
 * @see PropertiesUtil
 * @see XmlUtil
 * @see FilenameUtil
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
     * Default constructor for Configuration.
     * Creates an empty configuration with no attributes or properties.
     */
    protected Configuration() {
        this(null, null);
    }

    /**
     * Constructs a Configuration from an XML element with optional properties.
     * This constructor parses the XML element and its children, converting them to configuration attributes.
     * Property placeholders in the format ${propertyName} are replaced with values from the props map.
     *
     * @param element the XML element to parse for configuration
     * @param props optional map of properties for placeholder replacement
     */
    protected Configuration(final Element element, final Map<String, String> props) {
        if (props != null) {
            this.props.putAll(props);
        }

        init(); // NOSONAR

        if (element != null) {
            for (final Map.Entry<String, String> entry : XmlUtil.readAttributes(element).entrySet()) {
                setAttribute(entry.getKey(), entry.getValue()); // NOSONAR
            }

            final NodeList childNodeList = element.getChildNodes();

            for (int childNodeIndex = 0; childNodeIndex < childNodeList.getLength(); childNodeIndex++) {
                final Node childNode = childNodeList.item(childNodeIndex);

                if (childNode instanceof Element) {
                    if (XmlUtil.isTextElement(childNode)) {
                        final String attrName = childNode.getNodeName();
                        final String attrValue = Strings.strip(XmlUtil.getTextContent(childNode));
                        setAttribute(attrName, attrValue); // NOSONAR
                    } else {
                        complexElement2Attr((Element) childNode); // NOSONAR
                    }
                }
            }
        }
    }

    /**
     * Initialization method called during construction.
     * Subclasses can override this method to perform custom initialization.
     * This method is called before any XML parsing occurs.
     */
    protected void init() {
    }

    /**
     * Processes complex XML elements that are not simple text elements.
     * Subclasses should override this method to handle custom complex elements.
     * By default, this method throws a RuntimeException for unknown elements.
     *
     * @param element the complex XML element to process
     * @throws RuntimeException if the element is not recognized
     */
    protected void complexElement2Attr(final Element element) {
        throw new RuntimeException("Unknown element: " + element.getNodeName());
    }

    /**
     * Gets the source code location of the specified class.
     * This method returns the file system path where the class was loaded from.
     * URLs with %20 encoding for spaces are automatically decoded.
     * 
     * <p>Example usage:
     * <pre>{@code
     * String location = Configuration.getSourceCodeLocation(MyClass.class);
     * // Returns: "/path/to/myapp/classes" or "/path/to/myapp.jar"
     * }</pre>
     *
     * @param clazz the class whose source code location is to be retrieved
     * @return the path to the source code location of the specified class, with URL encoding removed
     */
    public static String getSourceCodeLocation(final Class<?> clazz) {
        return clazz.getProtectionDomain().getCodeSource().getLocation().getPath().replace("%20", " "); //NOSONAR
    }

    /**
     * Gets a list of common configuration paths where configuration files are typically located.
     * This method returns absolute paths to existing directories from the predefined list of
     * common configuration locations. The search includes Maven/Gradle build directories,
     * resources directories, and standard config/conf directories at both current and parent levels.
     * 
     * <p>Example usage:
     * <pre>{@code
     * List<String> paths = Configuration.getCommonConfigPath();
     * for (String path : paths) {
     *     System.out.println("Config path: " + path);
     * }
     * }</pre>
     *
     * @return a list of absolute paths to existing configuration directories
     */
    public static List<String> getCommonConfigPath() {
        String currentLocation = getCurrentSourceCodeLocation().getAbsolutePath();

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
     * Finds a directory with the specified name in common configuration paths.
     * The search is performed recursively in all common configuration locations.
     * Directories named .cvs, .svn, and .git are ignored during the search.
     * Results are cached for improved performance on subsequent lookups.
     * 
     * <p>Example usage:
     * <pre>{@code
     * File configDir = Configuration.findDir("config/database");
     * if (configDir != null && configDir.exists()) {
     *     // Found the configuration directory
     * }
     * }</pre>
     *
     * @param configDir the name of the configuration directory to find
     * @return the found directory as a File object, or null if not found
     */
    public static File findDir(final String configDir) {
        return findFile(configDir, true, null);
    }

    /**
     * Finds a file with the specified name in common configuration paths.
     * The search is performed recursively in all common configuration locations.
     * Directories named .cvs, .svn, and .git are ignored during the search.
     * Results are cached for improved performance on subsequent lookups.
     * 
     * <p>Example usage:
     * <pre>{@code
     * File configFile = Configuration.findFile("database.xml");
     * if (configFile != null) {
     *     Document doc = Configuration.parse(configFile);
     * }
     * }</pre>
     *
     * @param configFileName the name of the configuration file to find
     * @return the found file as a File object, or null if not found
     */
    public static File findFile(final String configFileName) {
        return findFile(configFileName, false, null);
    }

    /**
     * Finds the file specified by the given configuration file name.
     *
     * @param configFileName The name of the configuration file to be searched.
     * @param isDir Indicates whether the target is a directory.
     * @param foundDir A set of directories that have already been searched.
     * @return The found file as a File object, or {@code null} if the file is not found.
     * @throws RuntimeException if the target file name is empty or {@code null}.
     */
    private static File findFile(final String configFileName, final boolean isDir, Set<String> foundDir) {
        if (Strings.isEmpty(configFileName)) {
            throw new RuntimeException("target file name can't be empty or null: " + configFileName);
        }

        if (logger.isInfoEnabled()) {
            logger.info("start to find file " + configFileName);
        }

        // find out the configuration file
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
            folderPrefix = folderPrefix.replace("\\.\\.\\" + File.separatorChar, "");

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

        //    File dir = getCurrentSourceCodeLocation();
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
     * Finds a file by searching from the directory of a source file.
     * The search starts in the parent directory of the source file, then falls back
     * to common configuration paths if not found. This method is useful for finding
     * related configuration files that are referenced from within another configuration file.
     * 
     * <p>Example usage:
     * <pre>{@code
     * File mainConfig = new File("/app/config/main.xml");
     * File dbConfig = Configuration.findFileByFile(mainConfig, "database.xml");
     * // First looks in /app/config/, then searches common paths
     * }</pre>
     *
     * @param srcFile the source file whose directory will be used as the starting point
     * @param targetFileName the name of the file to find
     * @return the found file, or null if not found
     */
    public static File findFileByFile(final File srcFile, final String targetFileName) {
        File targetFile = new File(targetFileName);

        if (!targetFile.exists()) {
            if ((srcFile != null) && srcFile.exists()) {
                targetFile = findFileInDir(targetFileName, srcFile.getParentFile(), false);
            }

            if (targetFile == null || !targetFile.exists()) {
                return findFile(targetFileName);
            }
        }

        return targetFile;
    }

    /**
     * Finds a file or directory within a specified directory.
     * The search is recursive and will search all subdirectories.
     * Directories named .cvs, .svn, and .git are ignored during the search.
     * The file name can include a relative path which will be preserved during the search.
     * 
     * <p>Example usage:
     * <pre>{@code
     * File rootDir = new File("/app");
     * File configFile = Configuration.findFileInDir("config/database.xml", rootDir, false);
     * // Searches for database.xml in config subdirectories under /app
     * }</pre>
     *
     * @param configFileName the name of the file or directory to find (can include relative path)
     * @param dir the directory to search in
     * @param isDir true if searching for a directory, {@code false} for a file
     * @return the found file or directory, or null if not found
     * @throws RuntimeException if the target file name is empty or null
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
     * Finds the file specified by the given configuration file name in the specified directory.
     *
     * @param folderPrefix The prefix of the folder where the search should start.
     * @param configFileName The name of the configuration file to be searched.
     * @param dir The directory in which to search for the file.
     * @param isDir Indicates whether the target is a directory.
     * @param foundDir A set of directories that have already been searched.
     * @return The found file as a File object, or {@code null} if the file is not found.
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
     * Gets the current source code location.
     *
     * @return
     */
    private static File getCurrentSourceCodeLocation() {
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
     * Parses an XML file into a Document object.
     * The parser is configured with validation and namespace awareness enabled.
     * This method handles SAXException and IOException, wrapping them in appropriate
     * unchecked exceptions for easier handling.
     * 
     * <p>Example usage:
     * <pre>{@code
     * File configFile = new File("config.xml");
     * Document doc = Configuration.parse(configFile);
     * Element root = doc.getDocumentElement();
     * }</pre>
     *
     * @param file the XML file to parse
     * @return the parsed Document object
     * @throws ParseException if a parsing error occurs
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static Document parse(final File file) throws ParseException, UncheckedIOException {
        try {
            return XmlUtil.createDOMParser(true, true).parse(file);
        } catch (final SAXException e) {
            throw new ParseException(e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Parses an XML input stream into a Document object.
     * The parser is configured with validation and namespace awareness enabled.
     * This method handles SAXException and IOException, wrapping them in appropriate
     * unchecked exceptions for easier handling.
     * 
     * <p>Example usage:
     * <pre>{@code
     * try (InputStream is = new FileInputStream("config.xml")) {
     *     Document doc = Configuration.parse(is);
     *     Element root = doc.getDocumentElement();
     * }
     * }</pre>
     *
     * @param is the input stream containing XML data
     * @return the parsed Document object
     * @throws ParseException if a parsing error occurs
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static Document parse(final InputStream is) throws ParseException, UncheckedIOException {
        try {
            return XmlUtil.createDOMParser(true, true).parse(is);
        } catch (final SAXException e) {
            throw new ParseException(e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Formats a file path by replacing URL-encoded spaces (%20) with actual spaces.
     * If the original file doesn't exist but a file with decoded spaces does exist,
     * returns the decoded file. This method helps handle file paths that may have
     * been URL-encoded.
     * 
     * <p>Example usage:
     * <pre>{@code
     * File file = new File("/path/with%20spaces/file.xml");
     * File formatted = Configuration.formatPath(file);
     * // Returns File with path "/path/with spaces/file.xml"
     * }</pre>
     *
     * @param file the file whose path should be formatted
     * @return a File object with properly formatted path
     */
    public static File formatPath(File file) {
        final String formattedPath = file.getAbsolutePath().replace("%20", " ");

        if (!file.exists() || (new File(formattedPath)).exists()) { //NOSONAR
            file = new File(formattedPath); //NOSONAR
        }

        return file;
    }

    /**
     * Reads a time duration value from a string and converts it to milliseconds.
     * The value can include a time unit suffix:
     * <ul>
     *   <li>ms - milliseconds (default if no suffix)</li>
     *   <li>s - seconds</li>
     *   <li>m - minutes</li>
     *   <li>h - hours</li>
     *   <li>d - days</li>
     *   <li>w - weeks</li>
     * </ul>
     * 
     * <p>The method also supports multiplication expressions using asterisk (*).
     * Case-insensitive for unit suffixes. Trailing 'l' or 'L' (for long) is ignored.
     * 
     * <p>Example values:
     * <ul>
     *   <li>"1000" or "1000ms" - 1000 milliseconds</li>
     *   <li>"30s" - 30 seconds (30000 milliseconds)</li>
     *   <li>"5m" - 5 minutes (300000 milliseconds)</li>
     *   <li>"2h" - 2 hours (7200000 milliseconds)</li>
     *   <li>"1d" - 1 day (86400000 milliseconds)</li>
     *   <li>"1w" - 1 week (604800000 milliseconds)</li>
     *   <li>"3 * 1000" - 3000 milliseconds</li>
     *   <li>"60 * 60 * 1000" - 3600000 milliseconds (1 hour)</li>
     * </ul>
     * 
     * <p>Example usage:
     * <pre>{@code
     * long timeout = Configuration.readTimeInMillis("30s"); // 30000
     * long delay = Configuration.readTimeInMillis("5m");    // 300000
     * long expr = Configuration.readTimeInMillis("2 * 60 * 1000"); // 120000
     * }</pre>
     *
     * @param value the string value representing time duration
     * @return the time duration in milliseconds, or 0 if value is null or empty
     */
    public static long readTimeInMillis(String value) {
        value = Strings.trimToNull(value);

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
     * Gets all attribute names in this configuration.
     * The returned collection is backed by the configuration, so changes to
     * the configuration are reflected in the collection. However, the collection
     * itself should not be modified.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Collection<String> names = config.getAttrNames();
     * for (String name : names) {
     *     System.out.println(name + " = " + config.getAttribute(name));
     * }
     * }</pre>
     *
     * @return a collection of all attribute names
     */
    public Collection<String> getAttrNames() {
        return attrs.keySet();
    }

    /**
     * Gets the value of the specified attribute.
     * Returns null if the attribute doesn't exist. Attribute values are always
     * returned as strings, even if they were originally numeric in the XML.
     * 
     * <p>Example usage:
     * <pre>{@code
     * String dbUrl = config.getAttribute("database.url");
     * String timeout = config.getAttribute("connection.timeout");
     * if (timeout != null) {
     *     long timeoutMs = Configuration.readTimeInMillis(timeout);
     * }
     * }</pre>
     *
     * @param attrName the name of the attribute
     * @return the attribute value, or null if the attribute doesn't exist
     */
    public String getAttribute(final String attrName) {
        return attrs.get(attrName);
    }

    /**
     * Checks if an attribute with the specified name exists.
     * This method can be used to distinguish between an attribute with a null/empty
     * value and an attribute that doesn't exist at all.
     * 
     * <p>Example usage:
     * <pre>{@code
     * if (config.hasAttribute("optional.setting")) {
     *     String value = config.getAttribute("optional.setting");
     *     // Process the optional setting
     * }
     * }</pre>
     *
     * @param attrName the name of the attribute to check
     * @return {@code true} if the attribute exists, {@code false} otherwise
     */
    public boolean hasAttribute(final String attrName) {
        return attrs.containsKey(attrName);
    }

    /**
     * Gets all attributes as a map.
     * The returned map is the internal attribute map, so modifications to it
     * will affect the configuration. Use with caution.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Map<String, String> allAttrs = config.getAttributes();
     * // Create a defensive copy if needed
     * Map<String, String> copy = new HashMap<>(allAttrs);
     * }</pre>
     *
     * @return a map containing all attribute names and their values
     */
    public Map<String, String> getAttributes() {
        return attrs;
    }

    /**
     * Sets an attribute value.
     * If the value contains a property placeholder in the format ${propertyName},
     * it will be replaced with the corresponding property value if available.
     * The attribute value is trimmed of leading and trailing whitespace.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Direct value
     * config.setAttribute("timeout", "30s");
     * 
     * // With property placeholder
     * config.props.put("db.host", "localhost");
     * config.setAttribute("url", "${db.host}:3306");
     * // Results in url = "localhost:3306"
     * }</pre>
     *
     * @param attrName the name of the attribute
     * @param attrValue the value of the attribute (will be trimmed)
     * @return the previous value of the attribute, or null if it didn't exist
     */
    @SuppressWarnings("UnusedReturnValue")
    protected String setAttribute(final String attrName, String attrValue) {
        attrValue = Strings.trimToEmpty(attrValue);

        if ((!props.isEmpty()) && attrValue.startsWith("${") && attrValue.endsWith("}")) {
            attrValue = props.get(attrValue.substring(2, attrValue.length() - 1));
        }

        return attrs.put(attrName, attrValue);
    }

    /**
     * Removes an attribute from the configuration.
     * This method is typically used by subclasses to manage configuration state.
     * 
     * <p>Example usage:
     * <pre>{@code
     * String oldValue = config.removeAttribute("deprecated.setting");
     * if (oldValue != null) {
     *     // Migrate to new setting
     * }
     * }</pre>
     *
     * @param attrName the name of the attribute to remove
     * @return the value of the removed attribute, or null if it didn't exist
     */
    protected String removeAttribute(final String attrName) {
        return attrs.remove(attrName);
    }

    /**
     * Converts a comma-separated string to an array of trimmed strings.
     * Each element in the resulting array is trimmed of leading and trailing whitespace.
     * Empty elements after trimming are preserved.
     * 
     * <p>Example usage:
     * <pre>{@code
     * String[] hosts = config.string2Array("host1, host2, host3");
     * // Result: ["host1", "host2", "host3"]
     * 
     * String[] values = config.string2Array("a, , c");
     * // Result: ["a", "", "c"]
     * }</pre>
     *
     * @param st the comma-separated string
     * @return an array of trimmed strings
     */
    protected String[] string2Array(final String st) {
        return Splitter.with(WD.COMMA).trimResults().splitToArray(st);
    }

    /**
     * Converts a comma-separated string to a list of trimmed strings.
     * Each element in the resulting list is trimmed of leading and trailing whitespace.
     * Empty elements after trimming are preserved. The returned list is mutable.
     * 
     * <p>Example usage:
     * <pre>{@code
     * List<String> features = config.string2List("feature1, feature2, feature3");
     * features.add("feature4"); // Can modify the list
     * }</pre>
     *
     * @param st the comma-separated string
     * @return a list of trimmed strings
     */
    protected List<String> string2List(final String st) {
        return Splitter.with(WD.COMMA).trimResults().split(st);
    }

    /**
     * Converts a comma-separated string to a set of trimmed strings.
     * Each element in the resulting set is trimmed of leading and trailing whitespace.
     * Duplicate values are automatically removed (set behavior).
     * The returned set is mutable and uses hash-based implementation.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Set<String> tags = config.string2Set("java, java, xml, config");
     * // Result: Set containing ["java", "xml", "config"]
     * }</pre>
     *
     * @param st the comma-separated string
     * @return a set of trimmed strings with duplicates removed
     */
    protected Set<String> string2Set(final String st) {
        final Set<String> result = N.newHashSet();

        Splitter.with(WD.COMMA).trimResults().split(st, result);

        return result;
    }

    /**
     * Returns the hash code of this configuration based on its attributes.
     * Two configurations with identical attributes will have the same hash code.
     *
     * @return the hash code computed from the attributes map
     */
    @Override
    public int hashCode() {
        return attrs.hashCode();
    }

    /**
     * Checks if this configuration is equal to another object.
     * Two configurations are equal if they have the same attributes.
     * The properties map is not considered in equality comparison.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Configuration config1 = new MyConfig(element1);
     * Configuration config2 = new MyConfig(element2);
     * if (config1.equals(config2)) {
     *     // Both configurations have identical attributes
     * }
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof Configuration && N.equals(((Configuration) obj).attrs, attrs));
    }

    /**
     * Returns a string representation of this configuration showing its attributes.
     * The format is the same as Map.toString(), showing all attribute key-value pairs.
     * 
     * <p>Example output:
     * <pre>{@code
     * {database.url=jdbc:mysql://localhost:3306/mydb, timeout=30s, pool.size=10}
     * }</pre>
     *
     * @return a string representation of the attributes map
     */
    @Override
    public String toString() {
        return attrs.toString();
    }
}