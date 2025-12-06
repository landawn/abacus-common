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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.Exclusion;
import com.landawn.abacus.parser.XMLSerializationConfig;
import com.landawn.abacus.parser.XMLSerializationConfig.XSC;
import com.landawn.abacus.type.Type;

/**
 * Utility class for working with properties files and XML configuration files.
 * This class provides methods to load, store, and convert between different property formats,
 * as well as support for automatic refresh of properties when files are modified.
 * 
 * <p>Key features include:</p>
 * <ul>
 *   <li>Loading properties from files, streams, and readers</li>
 *   <li>Storing properties to files, streams, and writers</li>
 *   <li>Loading/storing properties from/to XML format</li>
 *   <li>Auto-refresh capability for properties when source files are modified</li>
 *   <li>Converting XML configuration to Java classes</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Load properties from file
 * Properties<String, String> props = PropertiesUtil.load(new File("config.properties"));
 * 
 * // Load with auto-refresh
 * Properties<String, String> autoProps = PropertiesUtil.load(new File("config.properties"), true);
 * 
 * // Load from XML
 * Properties<String, Object> xmlProps = PropertiesUtil.loadFromXml(new File("config.xml"));
 * }</pre>
 * 
 * @see Properties
 */
@SuppressWarnings("java:S1192")
public final class PropertiesUtil {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);

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

    private static final String TYPE = "type";

    private static final XMLSerializationConfig xsc = XSC.create()
            .tagByPropertyName(true)
            .writeTypeInfo(false)
            .setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME)
            .setExclusion(Exclusion.NONE)
            .setIgnoredPropNames((Map<Class<?>, Set<String>>) null);

    private static final ScheduledExecutorService scheduledExecutor;

    static {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.setRemoveOnCancelPolicy(true);
        scheduledExecutor = MoreExecutors.getExitingScheduledExecutorService(executor);
    }

    private static final Map<Resource, Properties<String, ?>> registeredAutoRefreshProperties = new ConcurrentHashMap<>(256);

    static {
        final Runnable refreshTask = new TimerTask() {
            @Override
            public void run() {
                synchronized (registeredAutoRefreshProperties) {
                    Properties<String, ?> properties = null;
                    Resource resource = null;
                    File file = null;

                    for (final Map.Entry<Resource, Properties<String, ?>> entry : registeredAutoRefreshProperties.entrySet()) {
                        resource = entry.getKey();
                        properties = entry.getValue();

                        file = resource.getFile();

                        if ((file != null) && (file.lastModified() > resource.getLastLoadTime())) {
                            final long lastLoadTime = file.lastModified();
                            Reader reader = null;

                            if (logger.isWarnEnabled()) {
                                logger.warn("Start to refresh properties with the updated file: " + file.getAbsolutePath());
                                logger.warn("[PROPERTIES]" + properties);
                            }

                            try {
                                reader = IOUtil.newFileReader(resource.getFile());

                                if (resource.getType() == ResourceType.PROPERTIES) {
                                    merge(load(reader), (Properties<String, String>) properties);
                                } else {
                                    merge(loadFromXml(reader, (Class<Properties<String, Object>>) properties.getClass()),
                                            (Properties<String, Object>) properties);
                                }

                                resource.setLastLoadTime(lastLoadTime);
                            } catch (final Exception e) {
                                logger.error("Failed to refresh properties: " + properties, e);
                            } finally {
                                IOUtil.close(reader);
                            }

                            if (logger.isWarnEnabled()) {
                                logger.warn("End to refresh properties with the updated file: " + file.getAbsolutePath());
                                logger.warn("[NEW PROPERTIES]" + properties);
                            }
                        }
                    }
                }
            }
        };

        scheduledExecutor.scheduleWithFixedDelay(refreshTask, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    private PropertiesUtil() {
        // singleton.
    }

    /**
     * Gets a list of common configuration paths where configuration files are typically located.
     * This method returns absolute paths to existing directories from the predefined list of
     * common configuration locations. The search includes Maven/Gradle build directories,
     * resources directories, and standard config/conf directories at both current and parent levels.
     * 
     * <p><b>Usage Examples:</b></p>
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

    private static File getCurrentSourceCodeLocation() {
        File dir = new File(ClassUtil.getSourceCodeLocation(PropertiesUtil.class));

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
     * Formats a file path by replacing URL-encoded spaces (%20) with actual spaces.
     * If the original file doesn't exist but a file with decoded spaces does exist,
     * returns the decoded file. This method helps handle file paths that may have
     * been URL-encoded.
     * 
     * <p><b>Usage Examples:</b></p>
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
     * Finds the directory with the specified configuration directory name.
     * This method searches for the directory in the classpath and file system.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File configDir = PropertiesUtil.findDir("config");
     * if (configDir != null && configDir.isDirectory()) {
     *     // Directory found
     * }
     * }</pre>
     *
     * @param configDir the name of the configuration directory to find
     * @return the File object representing the found directory, or {@code null} if not found
     */
    @MayReturnNull
    public static File findDir(final String configDir) {
        return findFile(configDir, true, null);
    }

    /**
     * Finds the file with the specified configuration file name.
     * This method searches for the file in the classpath and file system.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File configFile = PropertiesUtil.findFile("application.properties");
     * if (configFile != null && configFile.exists()) {
     *     // File found
     * }
     * }</pre>
     *
     * @param configFileName the name of the configuration file to find
     * @return the File object representing the found file, or {@code null} if not found
     */
    @MayReturnNull
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

        for (final String configPath : getCommonConfigPath()) {
            configurationFile = findFileInDir(folderPrefix, simpleConfigFileName, new File(configPath), isDir, foundDir);

            if (configurationFile != null && configurationFile.exists()) {
                configFilePathPool.put(configFileName, configurationFile.getAbsolutePath());

                return configurationFile;
            }
        }

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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File mainConfig = new File("/app/config/main.xml");
     * File dbConfig = Configuration.findFileByFile(mainConfig, "database.xml");
     * // First looks in /app/config/, then searches common paths
     * }</pre>
     *
     * @param srcFile the source file whose directory will be used as the starting point
     * @param targetFileName the name of the file to find
     * @return the found file, or {@code null} if not found
     */
    @MayReturnNull
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File rootDir = new File("/app");
     * File configFile = Configuration.findFileInDir("config/database.xml", rootDir, false);
     * // Searches for database.xml in config subdirectories under /app
     * }</pre>
     *
     * @param configFileName the name of the file or directory to find (can include relative path)
     * @param dir the directory to search in
     * @param isDir {@code true} if searching for a directory, {@code false} for a file
     * @return the found file or directory, or {@code null} if not found
     * @throws RuntimeException if the target file name is empty or null
     */
    @MayReturnNull
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

        dir = PropertiesUtil.formatPath(dir);

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
     * Loads properties from the specified file.
     * The properties are loaded as key-value pairs of strings in the standard Java properties format
     * (key=value pairs, one per line, with support for comments starting with # or !).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Properties<String, String> props = PropertiesUtil.load(new File("config.properties"));
     * String dbUrl = props.get("database.url");
     * }</pre>
     *
     * @param source the file from which to load the properties.
     * @return a Properties object containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     */
    public static Properties<String, String> load(final File source) {
        return load(source, false);
    }

    /**
     * Loads properties from the specified file with an option for auto-refresh.
     * When auto-refresh is enabled, the properties will be automatically updated when the file is modified.
     * A background thread checks the file's last modification time every second.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Properties that auto-update when file changes
     * Properties<String, String> props = PropertiesUtil.load(new File("config.properties"), true);
     * // Properties will be automatically refreshed when the file is modified
     * }</pre>
     *
     * @param source the file from which to load the properties.
     * @param autoRefresh if {@code true}, the properties will be automatically refreshed when the file is modified.
     *                    A background thread checks the file last modification time every second.
     * @return a Properties object containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     */
    public static Properties<String, String> load(final File source, final boolean autoRefresh) {
        Properties<String, String> properties = null;

        Reader reader = null;
        try {
            reader = IOUtil.newFileReader(source);

            if (autoRefresh) {
                final Resource resource = new Resource(Properties.class, source, ResourceType.PROPERTIES);
                resource.setLastLoadTime(source.lastModified());

                synchronized (registeredAutoRefreshProperties) {
                    properties = (Properties<String, String>) registeredAutoRefreshProperties.get(resource);

                    if (properties == null) {
                        properties = load(reader);
                        registeredAutoRefreshProperties.put(resource, properties);
                    }
                }
            } else {
                properties = load(reader);
            }

            return properties;
        } finally {
            IOUtil.close(reader);
        }
    }

    /**
     * Loads properties from the specified InputStream.
     * The stream should contain properties in the standard Java properties format
     * (key=value pairs, one per line, with support for comments starting with # or !).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("config.properties")) {
     *     Properties<String, String> props = PropertiesUtil.load(is);
     * }
     * }</pre>
     *
     * @param source the InputStream from which to load the properties.
     * @return a Properties object containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs while reading the stream
     */
    public static Properties<String, String> load(final InputStream source) {
        final java.util.Properties tmp = new java.util.Properties();

        try {
            tmp.load(source);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        final Properties<String, String> result = new Properties<>();

        merge(tmp, result);

        return result;
    }

    /**
     * Loads properties from the specified Reader.
     * The reader should provide properties in the standard Java properties format
     * (key=value pairs, one per line, with support for comments starting with # or !).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("config.properties")) {
     *     Properties<String, String> props = PropertiesUtil.load(reader);
     * }
     * }</pre>
     *
     * @param source the Reader from which to load the properties.
     * @return a Properties object containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs while reading from the reader
     */
    public static Properties<String, String> load(final Reader source) {
        final java.util.Properties tmp = new java.util.Properties();

        try {
            tmp.load(source);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        final Properties<String, String> result = new Properties<>();

        merge(tmp, result);

        return result;
    }

    /**
     * Merges the source properties into the target properties.
     *
     * @param srcProperties the source properties to merge from.
     * @param targetProperties the target properties to merge into. If {@code null}, a new Properties object will be created.
     * @return a Properties object containing the merged properties.
     */
    @SuppressWarnings("rawtypes")
    private static void merge(final java.util.Properties srcProperties, final Properties<String, String> targetProperties) {

        targetProperties.reset(new LinkedHashMap<>((Map) srcProperties));
    }

    private static <K, V> void merge(final Properties<? extends K, ? extends V> srcProperties, final Properties<K, V> targetProperties) {

        targetProperties.reset(new LinkedHashMap<>(srcProperties.values));
    }

    /**
     * Loads properties from the specified XML file.
     * The XML structure should have property names as element names and property values as element content.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Properties<String, Object> props = PropertiesUtil.loadFromXml(new File("config.xml"));
     * String dbUrl = (String) props.get("database.url");
     * }</pre>
     *
     * @param source the XML file from which to load the properties.
     * @return a Properties object containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     * @throws ParseException if the XML cannot be parsed or has invalid structure
     */
    public static Properties<String, Object> loadFromXml(final File source) {
        return loadFromXml(source, false);
    }

    /**
     * Loads properties from the specified XML file with an option for auto-refresh.
     * When auto-refresh is enabled, the properties will be automatically updated when the file is modified.
     * A background thread checks the file's last modification time every second.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Auto-refreshing XML properties
     * Properties<String, Object> props = PropertiesUtil.loadFromXml(new File("config.xml"), true);
     * }</pre>
     *
     * @param source the XML file from which to load the properties.
     * @param autoRefresh if {@code true}, the properties will be automatically refreshed when the file is modified.
     *                    A background thread checks the file last modification time every second.
     * @return a Properties object containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     * @throws ParseException if the XML cannot be parsed or has invalid structure
     */
    public static Properties<String, Object> loadFromXml(final File source, final boolean autoRefresh) {
        return loadFromXml(source, autoRefresh, Properties.class);
    }

    /**
     * Loads properties from the specified XML InputStream.
     * The XML structure should have property names as element names and property values as element content.
     * Optional {@code type} attributes can specify the data type for property values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("config.xml")) {
     *     Properties<String, Object> props = PropertiesUtil.loadFromXml(is);
     * }
     * }</pre>
     *
     * @param source the InputStream from which to load the properties.
     * @return a Properties object containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs while reading the stream
     * @throws ParseException if the XML cannot be parsed or has invalid structure
     */
    public static Properties<String, Object> loadFromXml(final InputStream source) {
        return loadFromXml(source, Properties.class);
    }

    /**
     * Loads properties from the specified XML Reader.
     * The XML structure should have property names as element names and property values as element content.
     * Optional {@code type} attributes can specify the data type for property values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("config.xml")) {
     *     Properties<String, Object> props = PropertiesUtil.loadFromXml(reader);
     * }
     * }</pre>
     *
     * @param source the Reader from which to load the properties.
     * @return a Properties object containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs while reading from the reader
     * @throws ParseException if the XML cannot be parsed or has invalid structure
     */
    public static Properties<String, Object> loadFromXml(final Reader source) {
        return loadFromXml(source, Properties.class);
    }

    /**
     * Loads properties from the specified XML file into the target properties class.
     * This allows loading into custom Properties subclasses.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * public class MyProperties extends Properties<String, Object> {
     *     // Custom properties class
     * }
     *
     * MyProperties props = PropertiesUtil.loadFromXml(new File("config.xml"), MyProperties.class);
     * }</pre>
     *
     * @param <T> the type of the target properties class.
     * @param source the XML file from which to load the properties.
     * @param targetClass the class of the target properties.
     * @return an instance of the target properties class containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     * @throws ParseException if the XML cannot be parsed or has invalid structure
     */
    public static <T extends Properties<String, Object>> T loadFromXml(final File source, final Class<? extends T> targetClass) {
        return loadFromXml(source, false, targetClass);
    }

    /**
     * Loads properties from the specified XML file into the target properties class with an option for auto-refresh.
     * When auto-refresh is enabled, the properties will be automatically updated when the file is modified.
     * A background thread checks the file's last modification time every second.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * public class DatabaseConfig extends Properties<String, Object> {
     *     // Custom config class
     * }
     * DatabaseConfig config = PropertiesUtil.loadFromXml(
     *     new File("db-config.xml"), true, DatabaseConfig.class);
     * // Config auto-refreshes when file changes
     * }</pre>
     *
     * @param <T> the type of the target properties class, must extend Properties&lt;String, Object&gt;.
     * @param source the XML file from which to load the properties.
     * @param autoRefresh if {@code true}, the properties will be automatically refreshed when the file is modified.
     *                    A background thread checks the file last modification time every second.
     * @param targetClass the class of the target properties.
     * @return an instance of the target properties class containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs reading the file
     * @throws ParseException if the XML cannot be parsed or has invalid structure
     */
    public static <T extends Properties<String, Object>> T loadFromXml(final File source, final boolean autoRefresh, final Class<? extends T> targetClass) {
        T properties = null;
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            if (autoRefresh) {
                final Resource resource = new Resource(targetClass, source, ResourceType.XML);
                resource.setLastLoadTime(source.lastModified());

                synchronized (registeredAutoRefreshProperties) {
                    properties = (T) registeredAutoRefreshProperties.get(resource);

                    if (properties == null) {
                        properties = loadFromXml(reader, targetClass);

                        registeredAutoRefreshProperties.put(resource, properties);
                    }
                }
            } else {
                properties = loadFromXml(reader, targetClass);
            }

            return properties;
        } finally {
            IOUtil.close(reader);
        }
    }

    /**
     * Loads properties from the specified XML InputStream into the target properties class.
     * This method parses the XML structure and creates an instance of the target class with the loaded properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("config.xml")) {
     *     MyConfig config = PropertiesUtil.loadFromXml(is, MyConfig.class);
     * }
     * }</pre>
     *
     * @param <T> the type of the target properties class, must extend Properties&lt;String, Object&gt;.
     * @param source the InputStream from which to load the properties.
     * @param targetClass the class of the target properties.
     * @return an instance of the target properties class containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs reading the stream
     * @throws ParseException if the XML cannot be parsed or has invalid structure
     */
    public static <T extends Properties<String, Object>> T loadFromXml(final InputStream source, final Class<? extends T> targetClass) {
        final DocumentBuilder docBuilder = XmlUtil.createDOMParser(true, true);

        Document doc;
        try {
            doc = docBuilder.parse(source);
        } catch (final SAXException e) {
            throw new ParseException(e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        final Node node = doc.getFirstChild();

        return loadFromXml(node, null, true, null, targetClass);
    }

    /**
     * Loads properties from the specified XML Reader into the target properties class.
     * This method parses the XML structure and creates an instance of the target class with the loaded properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("config.xml")) {
     *     AppConfig config = PropertiesUtil.loadFromXml(reader, AppConfig.class);
     * }
     * }</pre>
     *
     * @param <T> the type of the target properties class, must extend Properties&lt;String, Object&gt;.
     * @param source the Reader from which to load the properties.
     * @param targetClass the class of the target properties.
     * @return an instance of the target properties class containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs reading from the reader
     * @throws ParseException if the XML cannot be parsed or has invalid structure
     */
    public static <T extends Properties<String, Object>> T loadFromXml(final Reader source, final Class<? extends T> targetClass) {
        final DocumentBuilder docBuilder = XmlUtil.createDOMParser(true, true);

        Document doc;
        try {
            doc = docBuilder.parse(new InputSource(source));
        } catch (final SAXException e) {
            throw new ParseException(e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        final Node node = doc.getFirstChild();

        return loadFromXml(node, null, true, null, targetClass);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T extends Properties<String, Object>> T loadFromXml(final Node source, Method propSetMethod, final boolean isFirstCall, final T output,
            final Class<T> inputClass) {

        // TODO it's difficult to support duplicated property and may be misused.
        if (hasDuplicatedPropName(source)) {
            throw new RuntimeException("The source xml document contains duplicated properties which has same node tag name in the same root.");
        }

        Class<?> targetClass = null;

        if (isFirstCall) {
            targetClass = output == null ? (inputClass == null ? Properties.class : inputClass) : output.getClass();
        } else {
            targetClass = (propSetMethod == null) ? Properties.class : propSetMethod.getParameterTypes()[0];
        }

        final T properties = (T) (output == null ? N.newInstance(targetClass) : output);

        final NodeList propNodes = source.getChildNodes();
        @SuppressWarnings("ConstantValue")
        final int propNodeLength = (propNodes == null) ? 0 : propNodes.getLength();
        final Set<String> newKeySet = N.newHashSet();
        Node propNode = null;
        String typeAttr = null;
        String propName = null;
        Object propValue = null;

        for (int i = 0; i < propNodeLength; i++) {
            propNode = propNodes.item(i);

            if (propNode.getNodeType() != Document.ELEMENT_NODE) {
                continue;
            }

            propName = Beans.formalizePropName(propNode.getNodeName());
            newKeySet.add(propName);

            typeAttr = XmlUtil.getAttribute(propNode, TYPE);
            propSetMethod = Beans.getPropSetMethod(targetClass, propName);

            if (XmlUtil.isTextElement(propNode)) {
                if (Strings.isEmpty(typeAttr)) {
                    propValue = Strings.strip(XmlUtil.getTextContent(propNode));
                } else {
                    propValue = Type.of(typeAttr).valueOf(Strings.strip(XmlUtil.getTextContent(propNode)));
                }
            } else {
                // TODO it's difficult to support duplicated property and may be misused.
                // How to get target property value for auto-refresh if it's list of Properties or entities.
                final T targetPropValue = (T) properties.get(propName);
                final Class<T> propClass = (Class<T>) (propSetMethod == null ? Properties.class : propSetMethod.getParameterTypes()[0]);
                propValue = loadFromXml(propNode, propSetMethod, false, targetPropValue, propClass);
            }

            final Object oldPropValue = properties.get(propName);

            if (oldPropValue != null && oldPropValue.getClass().equals(propValue.getClass())
                    && (oldPropValue instanceof Collection || oldPropValue instanceof Map) && !(oldPropValue instanceof Properties)) {
                if (oldPropValue instanceof Collection) {
                    ((Collection) oldPropValue).clear();
                    ((Collection) oldPropValue).addAll((Collection) propValue);
                } else if (oldPropValue instanceof Map) {
                    ((Map) oldPropValue).clear();
                    ((Map) oldPropValue).putAll((Map) propValue);
                }
            } else {
                if (propSetMethod == null) {
                    properties.set(propName, propValue);
                } else {
                    final Class<?> parameterType = propSetMethod.getParameterTypes()[0];

                    if (Strings.isEmpty(propValue.toString()) && Properties.class.isAssignableFrom(parameterType)) {
                        propValue = N.newInstance(parameterType);
                    }

                    Beans.setPropValue(properties, propSetMethod, propValue);
                }
            }
        }

        if (output != null) {
            final Set<String> oldKeySet = N.newHashSet(properties.keySet());
            Method removeMethod = null;
            for (final String key : oldKeySet) {
                if (!newKeySet.contains(key)) {
                    removeMethod = ClassUtil.getDeclaredMethod(properties.getClass(), "remove" + Strings.capitalize(key));

                    if (removeMethod == null) {
                        properties.remove(key);
                    } else {
                        ClassUtil.invokeMethod(properties, removeMethod);
                    }
                }
            }
        }

        return properties;
    }

    /**
     * Stores the specified properties to the given file with optional comments.
     * The properties are written in the standard Java properties format.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Properties<String, String> props = new Properties<>();
     * props.put("name", "John");
     * props.put("age", "30");
     * PropertiesUtil.store(props, "User configuration", new File("user.properties"));
     * }</pre>
     *
     * @param properties the properties to store.
     * @param comments the comments to include in the stored file.
     * @param output the file to which the properties will be stored.
     * @throws UncheckedIOException if an I/O error occurs while writing to the file
     */
    public static void store(final Properties<?, ?> properties, final String comments, final File output) {
        Writer writer = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            writer = IOUtil.newFileWriter(output);

            store(properties, comments, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * Stores the specified properties to the given OutputStream with optional comments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = new FileOutputStream("config.properties")) {
     *     PropertiesUtil.store(properties, "Configuration", os);
     * }
     * }</pre>
     *
     * @param properties the properties to store.
     * @param comments the comments to include in the stored output.
     * @param output the OutputStream to which the properties will be stored.
     * @throws UncheckedIOException if an I/O error occurs while writing to the stream
     */
    public static void store(final Properties<?, ?> properties, final String comments, final OutputStream output) {
        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            store(properties, comments, bw);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * Stores the specified properties to the given Writer with optional comments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("config.properties")) {
     *     PropertiesUtil.store(properties, "Configuration", writer);
     * }
     * }</pre>
     *
     * @param properties the properties to store.
     * @param comments the comments to include in the stored output.
     * @param output the Writer to which the properties will be stored.
     * @throws UncheckedIOException if an I/O error occurs while writing to the writer
     */
    public static void store(final Properties<?, ?> properties, final String comments, final Writer output) {
        final java.util.Properties tmp = new java.util.Properties();

        tmp.putAll(properties);

        try {
            tmp.store(output, comments);

            output.flush();

        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Stores the specified properties to the given XML file.
     * The properties are written in XML format with the specified root element name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Properties<String, Object> props = new Properties<>();
     * props.put("database.url", "jdbc:mysql://localhost/mydb");
     * props.put("database.user", "admin");
     * PropertiesUtil.storeToXml(props, "configuration", true, new File("config.xml"));
     * }</pre>
     *
     * @param properties the properties to store.
     * @param rootElementName the name of the root element in the XML.
     * @param writeTypeInfo if {@code true}, type information will be written as attributes in the XML.
     *                      For example: {@code <port type="int">8080</port>} or {@code <enabled type="boolean">true</enabled>}.
     *                      When {@code false}, all values are written as plain text without type attributes.
     * @param output the file to which the properties will be stored.
     * @throws UncheckedIOException if an I/O error occurs while writing to the file
     */
    public static void storeToXml(final Properties<?, ?> properties, final String rootElementName, final boolean writeTypeInfo, final File output) {
        Writer writer = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            writer = IOUtil.newFileWriter(output);

            storeToXml(properties, rootElementName, writeTypeInfo, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * Stores the specified properties to the given XML OutputStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = new FileOutputStream("config.xml")) {
     *     PropertiesUtil.storeToXml(properties, "settings", false, os);
     * }
     * }</pre>
     *
     * @param properties the properties to store.
     * @param rootElementName the name of the root element in the XML.
     * @param writeTypeInfo if {@code true}, type information will be written as attributes in the XML.
     *                      For example: {@code <port type="int">8080</port>} or {@code <enabled type="boolean">true</enabled>}.
     *                      When {@code false}, all values are written as plain text without type attributes.
     * @param output the OutputStream to which the properties will be stored.
     * @throws UncheckedIOException if an I/O error occurs while writing to the stream
     */
    public static void storeToXml(final Properties<?, ?> properties, final String rootElementName, final boolean writeTypeInfo, final OutputStream output)
            throws UncheckedIOException {
        storeToXml(properties, rootElementName, writeTypeInfo, true, IOUtil.newOutputStreamWriter(output));
    }

    /**
     * Stores the specified properties to the given XML Writer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("config.xml")) {
     *     PropertiesUtil.storeToXml(properties, "config", true, writer);
     * }
     * }</pre>
     *
     * @param properties the properties to store.
     * @param rootElementName the name of the root element in the XML.
     * @param writeTypeInfo if {@code true}, type information will be written as attributes in the XML.
     *                      For example: {@code <port type="int">8080</port>} or {@code <enabled type="boolean">true</enabled>}.
     *                      When {@code false}, all values are written as plain text without type attributes.
     * @param output the Writer to which the properties will be stored.
     * @throws UncheckedIOException if an I/O error occurs while writing to the writer
     */
    public static void storeToXml(final Properties<?, ?> properties, final String rootElementName, final boolean writeTypeInfo, final Writer output)
            throws UncheckedIOException {
        storeToXml(properties, rootElementName, writeTypeInfo, true, output);
    }

    /**
     * Stores the specified properties to the given XML Writer.
     * This is an internal method used recursively for nested properties.
     *
     * @param properties the properties to store.
     * @param rootElementName the name of the root element in the XML.
     * @param writeTypeInfo if {@code true}, type information will be written as attributes in the XML.
     * @param isFirstCall if {@code true}, this is the first call (writes XML declaration).
     * @param output the Writer to which the properties will be stored.
     * @throws UncheckedIOException if an I/O error occurs while writing
     */
    private static void storeToXml(final Properties<?, ?> properties, final String rootElementName, final boolean writeTypeInfo, final boolean isFirstCall,
            final Writer output) throws UncheckedIOException {
        final BufferedXMLWriter bw = Objectory.createBufferedXMLWriter(output);

        try {
            if (isFirstCall) {
                bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            }

            if ((isFirstCall || !writeTypeInfo) || !properties.getClass().equals(Properties.class)) {
                bw.write("<" + rootElementName + ">");
            } else {
                bw.write("<" + rootElementName + " type=\"Properties\">");
            }

            String propName = null;
            String listPropName = null;
            String elementPropName = null;
            Object propValue = null;
            Object listPropValue;
            Type<Object> type = null;
            for (final Map.Entry<?, ?> entry : properties.entrySet()) { //NOSONAR
                propName = entry.getKey().toString();
                propValue = entry.getValue();

                elementPropName = propName;

                if (elementPropName.endsWith("List")) {
                    elementPropName = elementPropName.substring(0, elementPropName.length() - 4);
                }

                listPropName = elementPropName + "List";

                listPropValue = properties.get(listPropName);

                if ((propValue == null) || (listPropValue instanceof List && ((List<?>) listPropValue).size() > 0)) {
                    continue;
                }

                if (propValue instanceof List && properties.containsKey(elementPropName)) {
                    for (final Object e : ((List<?>) propValue)) {
                        if (e == null) {
                            // continue;
                        } else if (e instanceof Properties) {
                            bw.flush();

                            storeToXml((Properties<?, ?>) e, elementPropName, writeTypeInfo, false, output);
                        } else {
                            type = Type.of(e.getClass());

                            if (writeTypeInfo) {
                                if (ClassUtil.isPrimitiveWrapper(type.clazz())) {
                                    bw.write("<" + elementPropName + " type=\"" + ClassUtil.getSimpleClassName(ClassUtil.unwrap(type.clazz())) + "\">");
                                } else {
                                    bw.write("<" + elementPropName + " type=\"" + type.declaringName() + "\">");
                                }
                            } else {
                                bw.write("<" + elementPropName + ">");
                            }

                            type.writeCharacter(bw, e, xsc);

                            bw.write("</" + elementPropName + ">");
                        }
                    }

                } else if (propValue instanceof Properties) {
                    bw.flush();

                    storeToXml((Properties<?, ?>) propValue, propName, writeTypeInfo, false, output);
                } else {
                    type = Type.of(propValue.getClass());

                    if (writeTypeInfo) {
                        if (ClassUtil.isPrimitiveWrapper(type.clazz())) {
                            bw.write("<" + propName + " type=\"" + ClassUtil.getSimpleClassName(ClassUtil.unwrap(type.clazz())) + "\">");
                        } else {
                            bw.write("<" + propName + " type=\"" + type.declaringName() + "\">");
                        }
                    } else {
                        bw.write("<" + propName + ">");
                    }

                    type.writeCharacter(bw, propValue, xsc);

                    bw.write("</" + propName + ">");
                }
            }

            bw.write("</" + rootElementName + ">");

            bw.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * Generate Java code from the specified XML string.
     * This method analyzes the XML structure and generates a corresponding Java class hierarchy that mirrors it,
     * with typed getters and setters for each property. The generated class extends Properties&lt;String, Object&gt;.
     *
     * <p>The generated code includes:</p>
     * <ul>
     *   <li>Nested static classes for complex properties</li>
     *   <li>Type-safe getter and setter methods</li>
     *   <li>Automatic handling of property types based on XML type attributes</li>
     *   <li>Support for nested and list properties</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<config><database><url>jdbc:mysql://localhost</url></database></config>";
     * PropertiesUtil.xml2Java(xml, "src/main/java", "com.example", "Config", false);
     * // Generates Config.java with typed getters/setters
     * }</pre>
     *
     * @param xml the XML content as a string.
     * @param srcPath the source path where the generated Java code will be saved (e.g., "src/main/java").
     * @param packageName the package name for the generated Java classes.
     * @param className the name of the generated Java class.
     * @param isPublicField if {@code true}, the fields in the generated Java class will be public; otherwise private.
     * @throws RuntimeException if XML parsing fails, has duplicated property names, or file I/O error occurs
     */
    public static void xml2Java(final String xml, final String srcPath, final String packageName, final String className, final boolean isPublicField) {
        xml2Java(IOUtil.string2InputStream(xml), srcPath, packageName, className, isPublicField);
    }

    /**
     * Generate Java code from the specified XML file.
     * This method analyzes the XML structure and generates a corresponding Java class hierarchy that mirrors it,
     * with typed getters and setters for each property. The generated class extends Properties&lt;String, Object&gt;.
     *
     * <p>The generated code includes:</p>
     * <ul>
     *   <li>Nested static classes for complex properties</li>
     *   <li>Type-safe getter and setter methods</li>
     *   <li>Automatic handling of property types based on XML type attributes</li>
     *   <li>Support for nested and list properties</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File xmlFile = new File("config.xml");
     * PropertiesUtil.xml2Java(xmlFile, "src/main/java", "com.example", "AppConfig", false);
     * }</pre>
     *
     * @param xml the XML file from which to generate Java code.
     * @param srcPath the source path where the generated Java code will be saved (e.g., "src/main/java").
     * @param packageName the package name for the generated Java classes.
     * @param className the name of the generated Java class.
     * @param isPublicField if {@code true}, the fields in the generated Java class will be public; otherwise private.
     * @throws RuntimeException if XML parsing fails, has duplicated property names, or file I/O error occurs
     */
    public static void xml2Java(final File xml, final String srcPath, final String packageName, final String className, final boolean isPublicField) {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(xml);

            xml2Java(reader, srcPath, packageName, className, isPublicField);
        } finally {
            IOUtil.close(reader);
        }
    }

    /**
     * Generates Java code from the specified XML InputStream.
     * This method analyzes the XML structure and generates a corresponding Java class hierarchy that mirrors it,
     * with typed getters and setters for each property. The generated class extends Properties&lt;String, Object&gt;.
     *
     * <p>The generated code includes:</p>
     * <ul>
     *   <li>Nested static classes for complex properties</li>
     *   <li>Type-safe getter and setter methods</li>
     *   <li>Automatic handling of property types based on XML type attributes</li>
     *   <li>Support for nested and list properties</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("schema.xml")) {
     *     PropertiesUtil.xml2Java(is, "src/main/java", "com.example", "Schema", false);
     * }
     * }</pre>
     *
     * @param xml the InputStream from which to generate Java code.
     * @param srcPath the source path where the generated Java code will be saved (e.g., "src/main/java").
     * @param packageName the package name for the generated Java classes.
     * @param className the name of the generated Java class.
     * @param isPublicField if {@code true}, the fields in the generated Java class will be public; otherwise private.
     * @throws RuntimeException if XML parsing fails, has duplicated property names, or file I/O error occurs
     */
    public static void xml2Java(final InputStream xml, final String srcPath, final String packageName, final String className, final boolean isPublicField) {
        xml2Java(IOUtil.newInputStreamReader(xml), srcPath, packageName, className, isPublicField);
    }

    /**
     * Generates Java code from the specified XML Reader.
     * This method parses the XML and generates a Java class hierarchy that mirrors the XML structure,
     * with typed getters and setters for each property. The generated class extends Properties&lt;String, Object&gt;.
     *
     * <p>The generated code includes:</p>
     * <ul>
     *   <li>Nested static classes for complex properties</li>
     *   <li>Type-safe getter and setter methods</li>
     *   <li>Automatic handling of property types based on XML type attributes</li>
     *   <li>Support for nested and list properties</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("schema.xml")) {
     *     PropertiesUtil.xml2Java(reader, "src/main/java", "com.example.config", null, false);
     *     // Generates a Java class using the XML root element name
     * }
     * }</pre>
     *
     * @param xml the Reader from which to generate Java code.
     * @param srcPath the source path where the generated Java code will be saved (e.g., "src/main/java").
     * @param packageName the package name for the generated Java classes.
     * @param className the name of the generated Java class. If {@code null}, uses the root element name from XML.
     * @param isPublicField if {@code true}, the fields in the generated Java class will be public; otherwise private.
     * @throws RuntimeException if XML parsing fails, has duplicated property names, or file I/O error occurs
     */
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    public static void xml2Java(final Reader xml, final String srcPath, final String packageName, String className, final boolean isPublicField) {
        final DocumentBuilder docBuilder = XmlUtil.createDOMParser(true, true);
        Writer writer = null;

        try { //NOSONAR
            final Document doc = docBuilder.parse(new InputSource(xml));
            final Node root = doc.getFirstChild();

            // TODO it's difficult to support duplicated property and may be misused.
            if (hasDuplicatedPropName(root)) {
                throw new RuntimeException("The source xml document contains duplicated properties which has same node tag name in the same root.");
            }

            if (className == null) {
                className = Strings.capitalize(root.getNodeName());
            }

            final String classFilePath = ClassUtil.makeFolderForPackage(srcPath, packageName);
            final File classFile = new File(classFilePath + className + ".java");

            IOUtil.deleteIfExists(classFile);

            IOUtil.createNewFileIfNotExists(classFile);

            writer = IOUtil.newFileWriter(classFile, Charsets.DEFAULT);
            writer.write("package " + packageName + ";" + IOUtil.LINE_SEPARATOR_UNIX);

            writer.write(IOUtil.LINE_SEPARATOR_UNIX);
            writer.write(IOUtil.LINE_SEPARATOR_UNIX);

            final Set<String> importType = getImportType(root);

            if (hasDuplicatedPropName(root)) {
                importType.add(List.class.getCanonicalName());
                importType.add(java.util.ArrayList.class.getCanonicalName());
                importType.add(java.util.Collections.class.getCanonicalName());
            }

            importType.add(Map.class.getCanonicalName());

            for (final String clsName : importType) {
                writer.write("import " + clsName + ";" + IOUtil.LINE_SEPARATOR_UNIX);
            }

            writer.write(IOUtil.LINE_SEPARATOR_UNIX);
            writer.write("import " + Properties.class.getCanonicalName() + ";" + IOUtil.LINE_SEPARATOR_UNIX);
            writer.write(IOUtil.LINE_SEPARATOR_UNIX);

            xmlProperties2Java(root, className, isPublicField, "", true, writer);

            writer.flush();
        } catch (final Exception e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            IOUtil.close(writer);
        }
    }

    private static void xmlProperties2Java(final Node xmlNode, String className, final boolean isPublicField, final String spaces, final boolean isRoot,
            final Writer output) throws IOException {
        if (className == null) {
            className = Strings.capitalize(xmlNode.getNodeName());
        }

        output.write(IOUtil.LINE_SEPARATOR_UNIX);

        if (isRoot) {
            output.write(spaces + "/**" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(spaces + " * Auto-generated by Abacus." + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(spaces + " */" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(
                    spaces + "public class " + className + " extends " + Properties.class.getSimpleName() + "<String, Object> {" + IOUtil.LINE_SEPARATOR_UNIX);
        } else {
            output.write(spaces + "public static class " + className + " extends " + Properties.class.getSimpleName() + "<String, Object> {"
                    + IOUtil.LINE_SEPARATOR_UNIX);
        }

        final NodeList childNodes = xmlNode.getChildNodes();

        //noinspection ConstantValue
        if ((childNodes != null) && (childNodes.getLength() > 0)) {
            final Set<String> duplicatedPropNameSet = getDuplicatedPropNameSet(xmlNode);
            final Set<String> propNameSet = N.newHashSet();

            Node childNode = null;
            String propName = null;

            for (int i = 0; i < childNodes.getLength(); i++) {
                childNode = childNodes.item(i);

                if (childNode.getNodeType() != Document.ELEMENT_NODE) {
                    continue;
                }

                propName = Beans.formalizePropName(childNode.getNodeName());

                if (propNameSet.contains(propName)) {
                    continue;
                }

                propNameSet.add(propName);

            }

            propNameSet.clear();

            final String methodSpace = spaces + "    ";
            String typeName = null;

            for (int i = 0; i < childNodes.getLength(); i++) {
                childNode = childNodes.item(i);

                if (childNode.getNodeType() != Document.ELEMENT_NODE) {
                    continue;
                }

                propName = Beans.formalizePropName(childNode.getNodeName());

                if (propNameSet.contains(propName)) {
                    continue;
                }

                propNameSet.add(propName);

                output.write(IOUtil.LINE_SEPARATOR_UNIX);

                typeName = getTypeName(childNode, propName);

                writeMethod(methodSpace, propName, typeName, duplicatedPropNameSet, output);
            }

            // disable put/put/all/set/remove method
            output.write(IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "public " + className + " set(String propName, Object propValue) {" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "    " + "return (" + className + ") super.set(propName, propValue);" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR_UNIX);

            output.write(IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "public Object put(String propName, Object propValue) {" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "    " + "return super.put(propName, propValue);" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR_UNIX);

            output.write(IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "public void putAll(Map<? extends String, ? extends Object> m) {" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "    " + "super.putAll(m);" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR_UNIX);

            output.write(IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "public Object remove(Object propName) {" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "    " + "return super.remove(propName);" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR_UNIX);

            //    output.write(IOUtil.LINE_SEPARATOR_UNIX);
            //    output.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR_UNIX);
            //    output.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR_UNIX);
            //    output.write(methodSpace + "public void clear() {" + IOUtil.LINE_SEPARATOR_UNIX);
            //    output.write(methodSpace + "    " + "super.clear();" + IOUtil.LINE_SEPARATOR_UNIX);
            //    output.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR_UNIX);

            propNameSet.clear();

            for (int i = 0; i < childNodes.getLength(); i++) {
                childNode = childNodes.item(i);

                if (childNode.getNodeType() != Document.ELEMENT_NODE) {
                    continue;
                }

                propName = Beans.formalizePropName(childNode.getNodeName());

                if (propNameSet.contains(propName) || Strings.isNotEmpty(XmlUtil.getAttribute(childNode, TYPE))) {
                    continue;
                }

                propNameSet.add(propName);

                if (childNode.getChildNodes().getLength() > 1) {
                    xmlProperties2Java(childNode, null, isPublicField, spaces + "    ", false, output);
                }
            }
        }

        output.write(spaces + "}" + IOUtil.LINE_SEPARATOR_UNIX);
    }

    private static Set<String> getImportType(final Node node) {
        final Set<String> result = N.newLinkedHashSet();
        final NodeList childNodes = node.getChildNodes();

        //noinspection ConstantValue
        if ((childNodes == null) || (childNodes.getLength() == 0)) {
            return result;
        }

        Node childNode = null;
        String attr = null;
        Type<Object> type = null;

        for (int i = 0; i < childNodes.getLength(); i++) {
            childNode = childNodes.item(i);

            if (childNode.getNodeType() != Document.ELEMENT_NODE) {
                continue;
            }

            attr = XmlUtil.getAttribute(childNode, TYPE);

            if (Strings.isNotEmpty(attr)) {
                type = Type.of(attr);
                if (type != null) {
                    final Class<?> typeClass = type.clazz();
                    if (typeClass.getCanonicalName().startsWith("java.lang") || ClassUtil.isPrimitiveType(typeClass)
                            || (typeClass.isArray() && ClassUtil.isPrimitiveType(typeClass.getComponentType()))) {
                        // ignore
                    } else {
                        result.add(type.clazz().getCanonicalName());
                    }

                }
            }

            if (childNode.getChildNodes().getLength() > 1) {
                result.addAll(getImportType(childNode));
            }
        }

        return result;
    }

    private static void writeMethod(final String spaces, final String propName, final String typeName, final Set<String> duplicatedPropNameSet,
            final Writer output) throws IOException {
        final String listPropName = propName + "List";
        final String elementTypeName = Type.of(typeName).isPrimitiveType() ? ClassUtil.getSimpleClassName(ClassUtil.wrap(Type.of(typeName).clazz())) : typeName;

        output.write(spaces + "public " + typeName + " get" + Strings.capitalize(propName) + "() {" + IOUtil.LINE_SEPARATOR_UNIX);
        output.write(spaces + "    " + "return (" + typeName + ") super.get(\"" + propName + "\");" + IOUtil.LINE_SEPARATOR_UNIX);
        output.write(spaces + "}" + IOUtil.LINE_SEPARATOR_UNIX);

        output.write(IOUtil.LINE_SEPARATOR_UNIX);

        output.write(spaces + "public void set" + Strings.capitalize(propName) + "(" + typeName + " " + propName + ") {" + IOUtil.LINE_SEPARATOR_UNIX);
        output.write(spaces + "    " + "super.put(\"" + propName + "\", " + propName + ");" + IOUtil.LINE_SEPARATOR_UNIX);
        // output.write(spaces + "    " + "this." + propName + " = " + propName + ";" + IOUtil.LINE_SEPARATOR_UNIX);

        if (duplicatedPropNameSet.contains(propName)) {
            output.write(spaces + "    " + "put(\"" + listPropName + "\", " + listPropName + ");" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(spaces + "    " + "this." + listPropName + ".add(" + propName + ");" + IOUtil.LINE_SEPARATOR_UNIX);
        }

        output.write(spaces + "}" + IOUtil.LINE_SEPARATOR_UNIX);

        output.write(IOUtil.LINE_SEPARATOR_UNIX);

        output.write(spaces + "public void remove" + Strings.capitalize(propName) + "() {" + IOUtil.LINE_SEPARATOR_UNIX);
        output.write(spaces + "    " + "super.remove(\"" + propName + "\");" + IOUtil.LINE_SEPARATOR_UNIX);
        // output.write(spaces + "    " + "this." + propName + " = " + Type.of(typeName).defaultValue() + ";" + IOUtil.LINE_SEPARATOR_UNIX);

        // TODO it's difficult to support duplicated property and may be misused.
        //        if (duplicatedPropNameSet.contains(propName)) {
        //            writer.write(spaces + "    " + "remove(\"" + listPropName + "\", " + listPropName + ");" + N.LINE_SEPARATOR);
        //            writer.write(spaces + "    " + "this." + listPropName + ".remove(" + propName + ");" + N.LINE_SEPARATOR);
        //        }

        output.write(spaces + "}" + IOUtil.LINE_SEPARATOR_UNIX);

        if (duplicatedPropNameSet.contains(propName)) {
            output.write(IOUtil.LINE_SEPARATOR_UNIX);

            output.write(spaces + "public List<" + elementTypeName + "> get" + Strings.capitalize(listPropName) + "() {" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(spaces + "    " + "return " + listPropName + ";" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(spaces + "}" + IOUtil.LINE_SEPARATOR_UNIX);

            output.write(IOUtil.LINE_SEPARATOR_UNIX);

            output.write(spaces + "public void set" + Strings.capitalize(listPropName) + "(List<" + elementTypeName + "> " + listPropName + ") {"
                    + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(spaces + "    " + "super.put(\"" + listPropName + "\", " + listPropName + ");" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(spaces + "    " + "this." + listPropName + " = " + listPropName + ";" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(spaces + "}" + IOUtil.LINE_SEPARATOR_UNIX);
        }
    }

    private static String getTypeName(final Node node, final String propName) {
        String typeName = node.getChildNodes().getLength() > 1 ? Strings.capitalize(propName) : "String";
        final String typeAttr = XmlUtil.getAttribute(node, TYPE);

        if (Strings.isNotEmpty(typeAttr)) {
            if (typeAttr.equals("Properties")) {
                typeName = "Properties<String, Object>";
            } else {
                final Type<?> type = Type.of(typeAttr);
                if (type != null) {
                    typeName = type.clazz().getSimpleName();
                }
            }
        }

        return typeName;
    }

    private static boolean hasDuplicatedPropName(final Node node) {
        final NodeList childNodes = node.getChildNodes();

        //noinspection ConstantValue
        if ((childNodes == null) || (childNodes.getLength() == 0)) {
            return false;
        }

        String propName = null;
        Node childNode = null;

        final Set<String> propNameSet = N.newHashSet();

        for (int i = 0; i < childNodes.getLength(); i++) {
            childNode = childNodes.item(i);

            if (childNode.getNodeType() != Document.ELEMENT_NODE) {
                continue;
            }

            propName = Beans.formalizePropName(childNode.getNodeName());

            if (propNameSet.contains(propName) || ((childNode.getChildNodes().getLength() > 1) && hasDuplicatedPropName(childNode))) {
                return true;
            } else {
                propNameSet.add(propName);
            }
        }

        return false;
    }

    private static Set<String> getDuplicatedPropNameSet(final Node node) {
        final NodeList childNodes = node.getChildNodes();
        //noinspection ConstantValue
        if (childNodes == null || childNodes.getLength() == 0) {
            return N.newHashSet();
        }

        final Set<String> propNameSet = N.newHashSet();
        final Set<String> duplicatedPropNameSet = N.newHashSet();

        Node childNode = null;
        String propName = null;

        for (int i = 0; i < childNodes.getLength(); i++) {
            childNode = childNodes.item(i);

            if (childNode.getNodeType() != Document.ELEMENT_NODE) {
                continue;
            }

            propName = Beans.formalizePropName(childNode.getNodeName());

            if (propNameSet.contains(propName)) {
                duplicatedPropNameSet.add(propName);
            } else {
                propNameSet.add(propName);
            }
        }

        return duplicatedPropNameSet;
    }

    /**
     * The Class ConfigBean.
     */
    static final class ConfigBean { // NOSONAR

        /** The id. */
        private long id;

        /** The name. */
        private String name;

        /** The content. */
        private String content;

        /** The included servers. */
        private List<String> includedServers;

        /** The excluded servers. */
        private List<String> excludedServers;

        /** The status. */
        private Status status;

        /** The description. */
        private String description;

        /** The last update time. */
        private Timestamp lastUpdateTime;

        /** The created time. */
        private Timestamp createdTime;

        /**
         * Gets the id.
         *
         * @return the id value
         */
        public long getId() {
            return id;
        }

        /**
         * Sets the id.
         *
         * @param id the new id
         */
        public void setId(final long id) {
            this.id = id;
        }

        /**
         * Gets the name.
         *
         * @return the name, or {@code null} if not set
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the name.
         *
         * @param name the new name
         */
        public void setName(final String name) {
            this.name = name;
        }

        /**
         * Gets the content.
         *
         * @return the content, or {@code null} if not set
         */
        public String getContent() {
            return content;
        }

        /**
         * Sets the content.
         *
         * @param content the new content
         */
        public void setContent(final String content) {
            this.content = content;
        }

        /**
         * Gets the included servers.
         *
         * @return the list of included servers, or {@code null} if not set
         */
        public List<String> getIncludedServers() {
            return includedServers;
        }

        /**
         * Sets the included servers.
         *
         * @param includedServers the new included servers
         */
        public void setIncludedServers(final List<String> includedServers) {
            this.includedServers = includedServers;
        }

        /**
         * Gets the excluded servers.
         *
         * @return the list of excluded servers, or {@code null} if not set
         */
        public List<String> getExcludedServers() {
            return excludedServers;
        }

        /**
         * Sets the excluded servers.
         *
         * @param excludedServers the new excluded servers
         */
        public void setExcludedServers(final List<String> excludedServers) {
            this.excludedServers = excludedServers;
        }

        /**
         * Gets the status.
         *
         * @return the status, or {@code null} if not set
         */
        public Status getStatus() {
            return status;
        }

        /**
         * Sets the status.
         *
         * @param status the new status
         */
        public void setStatus(final Status status) {
            this.status = status;
        }

        /**
         * Gets the description.
         *
         * @return the description, or {@code null} if not set
         */
        public String getDescription() {
            return description;
        }

        /**
         * Sets the description.
         *
         * @param description the new description
         */
        public void setDescription(final String description) {
            this.description = description;
        }

        /**
         * Gets the last update time.
         *
         * @return the last update time, or {@code null} if not set
         */
        public Timestamp getLastUpdateTime() {
            return lastUpdateTime;
        }

        /**
         * Sets the last update time.
         *
         * @param lastUpdateTime the new last update time
         */
        public void setLastUpdateTime(final Timestamp lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }

        /**
         * Gets the creates the time.
         *
         * @return the created time, or {@code null} if not set
         */
        public Timestamp getCreatedTime() {
            return createdTime;
        }

        /**
         * Sets the creation the time.
         *
         * @param createdTime the new creates the time
         */
        public void setCreatedTime(final Timestamp createdTime) {
            this.createdTime = createdTime;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, content, includedServers, excludedServers, status, description, lastUpdateTime, createdTime);
        }

        @SuppressFBWarnings
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof ConfigBean other) {
                return N.equals(other.id, id) && N.equals(other.name, name) && N.equals(other.content, content)
                        && N.equals(other.includedServers, includedServers) && N.equals(other.excludedServers, excludedServers)
                        && N.equals(other.status, status) && N.equals(other.description, description) && N.equals(other.lastUpdateTime, lastUpdateTime)
                        && N.equals(other.createdTime, createdTime);

            }

            return false;
        }

        @Override
        public String toString() {
            return "{id=" + id + ", name=" + name + ", content=" + content + ", includedServers=" + includedServers + ", excludedServers=" + excludedServers
                    + ", status=" + status + ", description=" + description + ", lastUpdateTime=" + lastUpdateTime + ", createdTime=" + createdTime + "}";
        }

    }

    enum ResourceType {
        PROPERTIES, XML
    }

    /**
     * The Class Resource.
     */
    static class Resource {

        /** The target class. */
        private final Class<?> targetClass;

        /** The file. */
        private final File file;

        /** The file path. */
        private final String filePath;

        /** The last load time. */
        private long lastLoadTime;

        /** The resource type. */
        private final ResourceType resourceType;

        /**
         * Instantiates a new resource.
         *
         * @param cls the target class for the resource
         * @param file the file representing the resource
         * @param resourceType the type of the resource
         */
        public Resource(final Class<?> cls, final File file, final ResourceType resourceType) {
            targetClass = cls;
            this.file = file;
            filePath = file.getPath();
            this.resourceType = resourceType;
        }

        /**
         * Gets the last load time.
         *
         * @return the last load time in milliseconds since epoch
         */
        public long getLastLoadTime() {
            return lastLoadTime;
        }

        /**
         * Sets the last load time.
         *
         * @param lastLoadTime the new last load time
         */
        public void setLastLoadTime(final long lastLoadTime) {
            this.lastLoadTime = lastLoadTime;
        }

        /**
         * Gets the file.
         *
         * @return the File object associated with this resource
         */
        public File getFile() {
            return file;
        }

        /**
         * Gets the type.
         *
         * @return the ResourceType (PROPERTIES or XML)
         */
        public ResourceType getType() {
            return resourceType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(targetClass);
            return prime * result + N.hashCode(filePath);
        }

        @SuppressFBWarnings
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Resource other) {
                return N.equals(other.targetClass, targetClass) && N.equals(other.filePath, filePath);

            }

            return false;
        }

        @Override
        public String toString() {
            return "{file=" + file + "}";
        }
    }
}
