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
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.Exclusion;
import com.landawn.abacus.parser.XmlSerConfig;
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
 * <p>This is a final utility class and cannot be instantiated. All methods are static.
 * When auto-refresh is enabled for a loaded resource, a single shared background scheduler
 * polls registered files once per second and reloads any file whose last-modified timestamp
 * has advanced.</p>
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

    private static final XmlSerConfig xsc = XmlSerConfig.create()
            .setTagByPropertyName(true)
            .setWriteTypeInfo(false)
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
                            InputStream is = null;

                            if (logger.isDebugEnabled()) {
                                logger.debug("Start to refresh properties with the updated file: {}; propertyCount={}", file.getAbsolutePath(),
                                        properties.size());
                            }

                            try {
                                is = IOUtil.newFileInputStream(resource.getFile());

                                if (resource.getType() == ResourceType.PROPERTIES) {
                                    merge(load(is), (Properties<String, String>) properties);
                                } else {
                                    merge(loadFromXml(is, (Class<Properties<String, Object>>) properties.getClass()), (Properties<String, Object>) properties);
                                }

                                resource.setLastLoadTime(lastLoadTime);
                            } catch (final Exception e) {
                                logger.error(e, "Failed to refresh properties from file: {}; propertyCount={}", file.getAbsolutePath(), properties.size());
                            } finally {
                                IOUtil.close(is);
                            }

                            if (logger.isDebugEnabled()) {
                                logger.debug("End to refresh properties with the updated file: {}; propertyCount={}", file.getAbsolutePath(),
                                        properties.size());
                            }
                        }
                    }
                }
            }
        };

        scheduledExecutor.scheduleWithFixedDelay(refreshTask, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    private PropertiesUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets a list of common configuration paths where configuration files are typically located.
     * This method returns absolute paths to existing directories from the predefined list of
     * common configuration locations. The search includes Maven/Gradle build directories,
     * resources directories, and standard config/conf directories at both current and parent levels.
     *
     * <p>Each candidate is resolved twice: first relative to the location this class was loaded from,
     * then relative to the current working directory. Paths resolved by both passes therefore appear
     * twice in the returned list, and the list is ordered by search priority rather than alphabetically.
     * Directories that do not exist are skipped, so the result may be empty.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> paths = PropertiesUtil.getCommonConfigPaths();
     * for (String path : paths) {
     *     System.out.println("Config path: " + path);
     * }
     * }</pre>
     *
     * @return a list of absolute paths to existing configuration directories
     */
    public static List<String> getCommonConfigPaths() {
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
        File dir = new File(ClassUtil.getClassLocation(PropertiesUtil.class));

        if (dir.isFile() && dir.getParentFile().exists()) {
            dir = dir.getParentFile();
        }

        final String path = dir.getAbsolutePath().replace('\\', '/');

        // if the class/library is loaded from local maven repository.
        if (path.contains("/.m2/repository/com/landawn/abacus-core/") || path.contains("/.m2/repository/com/landawn/abacus-common/")
                || path.contains("/.m2/repository/com/landawn/abacus-common-se/") || path.contains("/.m2/repository/com/landawn/abacus-common-se-jdk7/")) {
            return new File(IOUtil.CURRENT_DIR);
        }

        return dir;
    }

    /**
     * Formats a file path by replacing URL-encoded spaces ({@code %20}) with actual spaces.
     * If the original file does not exist but a file at the decoded path does exist,
     * the decoded {@link File} is returned; otherwise the original {@code file} is returned unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("/path/with%20spaces/file.xml");
     * File formatted = PropertiesUtil.formatPath(file);
     * // Returns File with path "/path/with spaces/file.xml" if that path exists
     * }</pre>
     *
     * @param file the file whose path should be decoded
     * @return a {@code File} with {@code %20} replaced by spaces if the decoded path exists;
     *         the original {@code file} otherwise
     * @throws NullPointerException if {@code file} is {@code null}
     */
    public static File formatPath(File file) {
        if (!file.exists()) {
            final String formattedPath = file.getAbsolutePath().replace("%20", " ");
            final File formattedFile = new File(formattedPath); //NOSONAR

            if (formattedFile.exists()) {
                return formattedFile;
            }
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
     * @throws RuntimeException if {@code configDir} is {@code null} or empty
     * @see #findFile(String)
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
     * @throws RuntimeException if {@code configFileName} is {@code null} or empty
     * @see #findDir(String)
     * @see #findFileRelativeTo(File, String)
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
            throw new RuntimeException("target file name cannot be empty or null: " + configFileName);
        }

        if (logger.isInfoEnabled()) {
            logger.info("start to find file " + configFileName);
        }

        // find out the configuration file
        File configurationFile = new File(configFileName);

        if (configurationFile.exists()) {
            if (configurationFile.isDirectory() == isDir) {
                if (logger.isInfoEnabled()) {
                    logger.info("Found configuration file: " + configurationFile.getAbsolutePath());
                }

                return configurationFile;
            } else if (configurationFile.isAbsolute()) {
                // An exact absolute path exists but denotes the wrong kind of entry. Do not
                // return a regular file from findDir (or a directory from findFile), and do
                // not turn an absolute path into an unrelated recursive name search.
                return null;
            }
        }

        final String cachedPath = configFilePathPool.get(configFileName);

        if (cachedPath != null) {
            final File file = new File(cachedPath);

            if (file.exists() && file.isDirectory() == isDir) {
                return file;
            } else {
                configFilePathPool.remove(configFileName);
            }
        }

        String folderPrefix = null;
        String simpleConfigFileName = configFileName.trim().replace('\\', File.separatorChar).replace('/', File.separatorChar);

        final int index = simpleConfigFileName.lastIndexOf(File.separatorChar);

        if (index > -1) {
            folderPrefix = normalizeFolderPrefix(simpleConfigFileName.substring(0, index));
            folderPrefix = folderPrefix.replace(".." + File.separatorChar, "");

            simpleConfigFileName = simpleConfigFileName.substring(index + 1);
        }

        if (foundDir == null) {
            foundDir = N.newHashSet();
        }

        for (final String configPath : getCommonConfigPaths()) {
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
     * File dbConfig = PropertiesUtil.findFileRelativeTo(mainConfig, "database.xml");
     * // First looks in /app/config/, then searches common paths
     * }</pre>
     *
     * @param srcFile the source file whose directory will be used as the starting point
     * @param targetFileName the name of the file to find
     * @return the found file, or {@code null} if not found
     * @throws RuntimeException if {@code targetFileName} is {@code null} or empty
     */
    @MayReturnNull
    public static File findFileRelativeTo(final File srcFile, final String targetFileName) {
        File targetFile = new File(targetFileName);

        if (!targetFile.isFile()) {
            if ((srcFile != null) && srcFile.exists()) {
                targetFile = findFileInDir(targetFileName, srcFile.getParentFile(), false);
            }

            if (targetFile == null || !targetFile.isFile()) {
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
     * File configFile = PropertiesUtil.findFileInDir("config/database.xml", rootDir, false);
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
            throw new RuntimeException("target file name cannot be empty or null: " + configFileName);
        }

        String folderPrefix = null;
        String simpleConfigFileName = configFileName.trim().replace('\\', File.separatorChar).replace('/', File.separatorChar);

        final int index = simpleConfigFileName.lastIndexOf(File.separatorChar);

        if (index > -1) {
            folderPrefix = normalizeFolderPrefix(simpleConfigFileName.substring(0, index));

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

        dir = normalizeFilePath(PropertiesUtil.formatPath(dir));
        final String directoryPath = dir.getPath();

        if (foundDir == null) {
            foundDir = N.newHashSet();
        } else if (foundDir.contains(directoryPath)) {
            return null;
        }

        foundDir.add(directoryPath);

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

        if (Strings.isEmpty(folderPrefix) || pathEndsWithFolderPrefix(absolutePath, folderPrefix)) {
            for (final File file : files) {
                if (file.getName().equalsIgnoreCase(configFileName)) {
                    if ((isDir && file.isDirectory()) || (!isDir && !file.isDirectory())) { //NOSONAR

                        if (logger.isInfoEnabled()) {
                            logger.info("Found configuration file [" + file.getAbsolutePath() + "]");
                        }

                        return file;
                    }
                }
            }
        }

        for (final File file : files) {
            if (file.isDirectory()) {
                final File result = findFileInDir(folderPrefix, configFileName, file, isDir, foundDir);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Removes only an explicit current-directory prefix ({@code ./} or {@code .\}), preserving
     * legitimate hidden-directory names such as {@code .config}.
     */
    private static String normalizeFolderPrefix(final String folderPrefix) {
        if (Strings.isEmpty(folderPrefix) || ".".equals(folderPrefix)) {
            return Strings.EMPTY;
        }

        final String currentDirectoryPrefix = "." + File.separatorChar;
        return folderPrefix.startsWith(currentDirectoryPrefix) ? folderPrefix.substring(currentDirectoryPrefix.length()) : folderPrefix;
    }

    /**
     * Tests a directory suffix on a path-segment boundary. For example, {@code config}
     * matches {@code /app/config} but not {@code /app/myconfig}.
     */
    private static boolean pathEndsWithFolderPrefix(final String path, final String folderPrefix) {
        final int prefixStart = path.length() - folderPrefix.length();

        if (prefixStart < 0 || !path.regionMatches(true, prefixStart, folderPrefix, 0, folderPrefix.length())) {
            return false;
        }

        return prefixStart == 0 || isPathSeparator(path.charAt(prefixStart - 1)) || isPathSeparator(folderPrefix.charAt(0));
    }

    private static boolean isPathSeparator(final char ch) {
        return ch == '/' || ch == '\\';
    }

    /**
     * Returns a stable absolute representation so path aliases and symbolic-link cycles do not
     * create duplicate resources or unbounded recursive searches.
     */
    private static File normalizeFilePath(final File file) {
        try {
            return file.getCanonicalFile();
        } catch (final IOException e) {
            return file.getAbsoluteFile().toPath().normalize().toFile();
        }
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
     * @see #load(File, boolean)
     * @see #load(InputStream)
     * @see #load(Reader)
     */
    public static Properties<String, String> load(final File source) {
        return load(source, false);
    }

    /**
     * Loads properties from the specified file with an option for auto-refresh.
     * When auto-refresh is enabled, the returned instance is registered with a shared
     * background scheduler that reloads it whenever the source file's last-modified
     * timestamp advances. If a properties instance for the same file has already been
     * registered for auto-refresh, that same instance is returned instead of loading a new one.
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
     * @see #load(File)
     */
    public static Properties<String, String> load(final File source, final boolean autoRefresh) {
        InputStream is = null;

        try {
            if (autoRefresh) {
                final Resource resource = new Resource(Properties.class, source, ResourceType.PROPERTIES);

                synchronized (registeredAutoRefreshProperties) {
                    final Properties<String, String> registered = (Properties<String, String>) registeredAutoRefreshProperties.get(resource);

                    // Looking up the normalized resource before opening the file is important: callers are
                    // promised the already-registered live object, even if the backing file is temporarily
                    // unavailable while an editor replaces it.
                    if (registered != null) {
                        return registered;
                    }

                    is = IOUtil.newFileInputStream(resource.getFile());
                    final Properties<String, String> properties = load(is);
                    resource.setLastLoadTime(resource.getFile().lastModified());
                    registeredAutoRefreshProperties.put(resource, properties);

                    return properties;
                }
            }

            is = IOUtil.newFileInputStream(source);
            return load(is);
        } finally {
            IOUtil.close(is);
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
     * @param source the InputStream from which to load the properties; it is not closed by this method.
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
     * @param source the Reader from which to load the properties; it is not closed by this method.
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
     * @param targetProperties the target properties to merge into.
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
     * An optional {@code type} attribute on an element specifies the data type to convert its text content to;
     * if absent, the value is kept as a (stripped) {@code String}. Elements with child elements are loaded
     * recursively into nested {@link Properties} instances. Duplicated sibling element names are not supported.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Properties<String, Object> props = PropertiesUtil.loadFromXml(new File("config.xml"));
     * String url = (String) props.get("url");
     * }</pre>
     *
     * @param source the XML file from which to load the properties.
     * @return a Properties object containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     * @throws ParsingException if the XML cannot be parsed or has no document element
     * @throws RuntimeException if sibling element names collide after property-name normalization
     * @see #loadFromXml(File, boolean)
     * @see #loadFromXml(File, Class)
     */
    public static Properties<String, Object> loadFromXml(final File source) {
        return loadFromXml(source, false);
    }

    /**
     * Loads properties from the specified XML file with an option for auto-refresh.
     * When auto-refresh is enabled, the returned instance is registered with a shared
     * background scheduler that reloads it whenever the source file's last-modified
     * timestamp advances. If a properties instance for the same file has already been
     * registered for auto-refresh, that same instance is returned instead of loading a new one.
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
     * @throws ParsingException if the XML cannot be parsed or has no document element
     * @throws RuntimeException if sibling element names collide after property-name normalization
     * @see #loadFromXml(File)
     */
    public static Properties<String, Object> loadFromXml(final File source, final boolean autoRefresh) {
        return loadFromXml(source, autoRefresh, Properties.class);
    }

    /**
     * Loads properties from the specified XML InputStream.
     * The XML structure should have property names as element names and property values as element content.
     * An optional {@code type} attribute on an element specifies the data type to convert its text content to;
     * if absent, the value is kept as a (stripped) {@code String}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("config.xml")) {
     *     Properties<String, Object> props = PropertiesUtil.loadFromXml(is);
     * }
     * }</pre>
     *
     * @param source the InputStream from which to load the properties; it is not closed by this method.
     * @return a Properties object containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs while reading the stream
     * @throws ParsingException if the XML cannot be parsed or has no document element
     * @throws RuntimeException if sibling element names collide after property-name normalization
     * @see #loadFromXml(InputStream, Class)
     */
    public static Properties<String, Object> loadFromXml(final InputStream source) {
        return loadFromXml(source, Properties.class);
    }

    /**
     * Loads properties from the specified XML Reader.
     * The XML structure should have property names as element names and property values as element content.
     * An optional {@code type} attribute on an element specifies the data type to convert its text content to;
     * if absent, the value is kept as a (stripped) {@code String}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("config.xml")) {
     *     Properties<String, Object> props = PropertiesUtil.loadFromXml(reader);
     * }
     * }</pre>
     *
     * @param source the Reader from which to load the properties; it is not closed by this method.
     * @return a Properties object containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs while reading from the reader
     * @throws ParsingException if the XML cannot be parsed or has no document element
     * @throws RuntimeException if sibling element names collide after property-name normalization
     * @see #loadFromXml(Reader, Class)
     */
    public static Properties<String, Object> loadFromXml(final Reader source) {
        return loadFromXml(source, Properties.class);
    }

    /**
     * Loads properties from the specified XML file into the target properties class.
     * This allows loading into custom Properties subclasses.
     * An optional {@code type} attribute on an element specifies the data type to convert its text content to;
     * if absent, the value is kept as a (stripped) {@code String}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Example {
     *     public static class MyProperties extends Properties<String, Object> {
     *         public MyProperties() {
     *         }
     *     }
     *
     *     MyProperties load() {
     *         return PropertiesUtil.loadFromXml(new File("config.xml"), MyProperties.class);
     *     }
     * }
     * }</pre>
     *
     * @param <T> the type of the target properties class, must extend Properties&lt;String, Object&gt;.
     * @param source the XML file from which to load the properties.
     * @param targetClass the class of the target properties.
     * @return an instance of the target properties class containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     * @throws ParsingException if the XML cannot be parsed or has no document element
     * @throws RuntimeException if sibling element names collide after property-name normalization
     * @see #loadFromXml(File)
     * @see #loadFromXml(File, boolean, Class)
     */
    public static <T extends Properties<String, Object>> T loadFromXml(final File source, final Class<? extends T> targetClass) {
        return loadFromXml(source, false, targetClass);
    }

    /**
     * Loads properties from the specified XML file into the target properties class with an option for auto-refresh.
     * When auto-refresh is enabled, the returned instance is registered with a shared background
     * scheduler that reloads it whenever the source file's last-modified timestamp advances.
     * If a properties instance for the same file and target class has already been registered for
     * auto-refresh, that same instance is returned instead of loading a new one.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Example {
     *     public static class DatabaseConfig extends Properties<String, Object> {
     *         public DatabaseConfig() {
     *         }
     *     }
     *
     *     DatabaseConfig load() {
     *         // The returned object auto-refreshes when the file changes.
     *         return PropertiesUtil.loadFromXml(new File("db-config.xml"), true, DatabaseConfig.class);
     *     }
     * }
     * }</pre>
     *
     * @param <T> the type of the target properties class, must extend Properties&lt;String, Object&gt;.
     * @param source the XML file from which to load the properties.
     * @param autoRefresh if {@code true}, the properties will be automatically refreshed when the file is modified.
     *                    A background thread checks the file last modification time every second.
     * @param targetClass the class of the target properties.
     * @return an instance of the target properties class containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs reading the file
     * @throws ParsingException if the XML cannot be parsed or has no document element
     * @throws RuntimeException if sibling element names collide after property-name normalization
     * @see #loadFromXml(File, Class)
     */
    public static <T extends Properties<String, Object>> T loadFromXml(final File source, final boolean autoRefresh, final Class<? extends T> targetClass) {
        InputStream is = null;

        try {
            if (autoRefresh) {
                final Resource resource = new Resource(targetClass, source, ResourceType.XML);

                synchronized (registeredAutoRefreshProperties) {
                    final T registered = (T) registeredAutoRefreshProperties.get(resource);

                    if (registered != null) {
                        return registered;
                    }

                    is = IOUtil.newFileInputStream(resource.getFile());
                    final T properties = loadFromXml(is, targetClass);
                    resource.setLastLoadTime(resource.getFile().lastModified());
                    registeredAutoRefreshProperties.put(resource, properties);

                    return properties;
                }
            }

            is = IOUtil.newFileInputStream(source);
            return loadFromXml(is, targetClass);
        } finally {
            IOUtil.close(is);
        }
    }

    /**
     * Loads properties from the specified XML InputStream into the target properties class.
     * This method parses the XML structure and creates an instance of the target class with the loaded properties.
     * An optional {@code type} attribute on an element specifies the data type to convert its text content to;
     * if absent, the value is kept as a (stripped) {@code String}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Example {
     *     public static class MyConfig extends Properties<String, Object> {
     *         public MyConfig() {
     *         }
     *     }
     *
     *     MyConfig load() throws IOException {
     *         try (InputStream is = new FileInputStream("config.xml")) {
     *             return PropertiesUtil.loadFromXml(is, MyConfig.class);
     *         }
     *     }
     * }
     * }</pre>
     *
     * @param <T> the type of the target properties class, must extend Properties&lt;String, Object&gt;.
     * @param source the InputStream from which to load the properties; it is not closed by this method.
     * @param targetClass the class of the target properties.
     * @return an instance of the target properties class containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs reading the stream
     * @throws ParsingException if the XML cannot be parsed or has no document element
     * @throws RuntimeException if sibling element names collide after property-name normalization
     * @see #loadFromXml(InputStream)
     */
    public static <T extends Properties<String, Object>> T loadFromXml(final InputStream source, final Class<? extends T> targetClass) {
        final DocumentBuilder docBuilder = XmlUtil.createDOMParser(true, true);

        Document doc;
        try {
            doc = docBuilder.parse(source);
        } catch (final SAXException e) {
            throw new ParsingException(e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        final Node node = doc.getDocumentElement();

        if (node == null) {
            throw new ParsingException("No document element found in XML source");
        }

        return loadFromXml(node, null, true, null, targetClass);
    }

    /**
     * Loads properties from the specified XML Reader into the target properties class.
     * This method parses the XML structure and creates an instance of the target class with the loaded properties.
     * An optional {@code type} attribute on an element specifies the data type to convert its text content to;
     * if absent, the value is kept as a (stripped) {@code String}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Example {
     *     public static class AppConfig extends Properties<String, Object> {
     *         public AppConfig() {
     *         }
     *     }
     *
     *     AppConfig load() throws IOException {
     *         try (Reader reader = new FileReader("config.xml")) {
     *             return PropertiesUtil.loadFromXml(reader, AppConfig.class);
     *         }
     *     }
     * }
     * }</pre>
     *
     * @param <T> the type of the target properties class, must extend Properties&lt;String, Object&gt;.
     * @param source the Reader from which to load the properties; it is not closed by this method.
     * @param targetClass the class of the target properties.
     * @return an instance of the target properties class containing the loaded properties.
     * @throws UncheckedIOException if an I/O error occurs reading from the reader
     * @throws ParsingException if the XML cannot be parsed or has no document element
     * @throws RuntimeException if sibling element names collide after property-name normalization
     * @see #loadFromXml(Reader)
     */
    public static <T extends Properties<String, Object>> T loadFromXml(final Reader source, final Class<? extends T> targetClass) {
        final DocumentBuilder docBuilder = XmlUtil.createDOMParser(true, true);

        Document doc;
        try {
            doc = docBuilder.parse(new InputSource(source));
        } catch (final SAXException e) {
            throw new ParsingException(e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        final Node node = doc.getDocumentElement();

        if (node == null) {
            throw new ParsingException("No document element found in XML source");
        }

        return loadFromXml(node, null, true, null, targetClass);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T extends Properties<String, Object>> T loadFromXml(final Node source, Method propSetMethod, final boolean isFirstCall, final T output,
            final Class<T> inputClass) {

        // Normalized sibling names are map keys and therefore cannot be represented independently.
        if (hasDuplicatedPropName(source)) {
            throw new RuntimeException("The source XML document contains sibling element names that collide after property-name normalization.");
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

            if (propNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            propName = Beans.normalizePropName(propNode.getNodeName());
            newKeySet.add(propName);

            typeAttr = XmlUtil.getAttribute(propNode, TYPE);
            propSetMethod = getPropSetterForXml(targetClass, propName);

            if (XmlUtil.isTextElement(propNode)) {
                if (Strings.isEmpty(typeAttr)) {
                    propValue = Strings.strip(XmlUtil.getTextContent(propNode));
                } else {
                    propValue = Type.of(typeAttr).valueOf(Strings.strip(XmlUtil.getTextContent(propNode)));
                }
            } else {
                // Reuse an existing nested Properties value when present so recursive refreshes can
                // preserve its identity; duplicate/list-style sibling properties are rejected above.
                final T targetPropValue = (T) properties.get(propName);
                final Class<T> propClass = (Class<T>) (propSetMethod == null ? Properties.class : propSetMethod.getParameterTypes()[0]);
                propValue = loadFromXml(propNode, propSetMethod, false, targetPropValue, propClass);
            }

            final Object oldPropValue = properties.get(propName);

            if (oldPropValue != null && propValue != null && oldPropValue.getClass().equals(propValue.getClass())
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

                    if ((propValue == null || Strings.isEmpty(propValue.toString())) && Properties.class.isAssignableFrom(parameterType)) {
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

    private static Method getPropSetterForXml(final Class<?> targetClass, final String propName) {
        if (targetClass == null || targetClass.equals(Properties.class)) {
            return null;
        }

        if (Properties.class.isAssignableFrom(targetClass)) {
            try {
                return Beans.getPropSetter(targetClass, propName);
            } catch (final IllegalArgumentException e) {
                return null;
            }
        }

        return Beans.isBeanClass(targetClass) ? Beans.getPropSetter(targetClass, propName) : null;
    }

    /**
     * Stores the specified properties to the given file with optional comments.
     * The properties are written in the standard Java properties format. Non-string keys and values
     * are converted with {@link String#valueOf(Object)}; null keys or values are rejected because the
     * standard {@link java.util.Properties} representation does not support them.
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
     * @param comments the comments to include as a leading comment line in the stored file; may be {@code null} for no comment.
     * @param output the file to which the properties will be stored. The file is created if it does not already exist.
     * @throws NullPointerException if {@code properties}, {@code output}, or any key or value is {@code null}
     * @throws UncheckedIOException if an I/O error occurs while writing to the file
     * @see #store(Properties, String, OutputStream)
     * @see #store(Properties, String, Writer)
     */
    public static void store(final Properties<?, ?> properties, final String comments, final File output) {
        OutputStream os = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            os = IOUtil.newFileOutputStream(output);
            store(properties, comments, os);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     * Stores the specified properties to the given OutputStream with optional comments.
     * Non-string keys and values are converted with {@link String#valueOf(Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Properties<String, String> properties = new Properties<>();
     * properties.put("mode", "production");
     * try (OutputStream os = new FileOutputStream("config.properties")) {
     *     PropertiesUtil.store(properties, "Configuration", os);
     * }
     * }</pre>
     *
     * @param properties the properties to store.
     * @param comments the comments to include as a leading comment line in the stored output; may be {@code null} for no comment.
     * @param output the OutputStream to which the properties will be stored. The stream is flushed but not closed.
     * @throws NullPointerException if {@code properties}, {@code output}, or any key or value is {@code null}
     * @throws UncheckedIOException if an I/O error occurs while writing to the stream
     */
    public static void store(final Properties<?, ?> properties, final String comments, final OutputStream output) {
        final java.util.Properties tmp = toJavaProperties(properties);

        try {
            tmp.store(output, comments);
            output.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Stores the specified properties to the given Writer with optional comments.
     * Non-string keys and values are converted with {@link String#valueOf(Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Properties<String, String> properties = new Properties<>();
     * properties.put("mode", "production");
     * try (Writer writer = new FileWriter("config.properties")) {
     *     PropertiesUtil.store(properties, "Configuration", writer);
     * }
     * }</pre>
     *
     * @param properties the properties to store.
     * @param comments the comments to include as a leading comment line in the stored output; may be {@code null} for no comment.
     * @param output the Writer to which the properties will be stored. The writer is flushed but not closed.
     * @throws NullPointerException if {@code properties}, {@code output}, or any key or value is {@code null}
     * @throws UncheckedIOException if an I/O error occurs while writing to the writer
     */
    public static void store(final Properties<?, ?> properties, final String comments, final Writer output) {
        final java.util.Properties tmp = toJavaProperties(properties);

        try {
            tmp.store(output, comments);

            output.flush();

        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static java.util.Properties toJavaProperties(final Properties<?, ?> properties) {
        Objects.requireNonNull(properties, "properties");

        final java.util.Properties result = new java.util.Properties();

        for (final Map.Entry<?, ?> entry : properties.entrySet()) {
            final Object key = Objects.requireNonNull(entry.getKey(), "property key");
            final Object value = Objects.requireNonNull(entry.getValue(), "property value for key: " + key);
            result.setProperty(String.valueOf(key), String.valueOf(value));
        }

        return result;
    }

    /**
     * Stores the specified properties to the given XML file.
     * The properties are written in XML format with the specified root element name.
     * Each non-null mapping is written under its own key; in particular, keys such as
     * {@code item} and {@code itemList} remain distinct. Null-valued mappings are omitted.
     * Type information is required to reconstruct non-string scalar and collection values
     * when the XML is loaded again.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Properties<String, Object> props = new Properties<>();
     * props.put("url", "jdbc:mysql://localhost/mydb");
     * props.put("user", "admin");
     * PropertiesUtil.storeToXml(props, "configuration", true, new File("config.xml"));
     * }</pre>
     *
     * @param properties the properties to store. Each entry key becomes an XML element name nested under the root element.
     * @param rootElementName the name of the root element in the XML.
     * @param writeTypeInfo if {@code true}, type information will be written as attributes in the XML.
     *                      For example: {@code <port type="int">8080</port>} or {@code <enabled type="boolean">true</enabled>}.
     *                      When {@code false}, all values are written as plain text without type attributes.
     * @param output the file to which the properties will be stored, encoded as UTF-8. The file is created if it does not already exist.
     * @throws NullPointerException if {@code properties}, {@code output}, or a key for a non-null value is {@code null}
     * @throws IllegalArgumentException if the root name or a property key is not a usable, namespace-free XML element name,
     *         or if nested {@code Properties} instances contain a reference cycle
     * @throws UncheckedIOException if an I/O error occurs while writing to the file
     * @see #storeToXml(Properties, String, boolean, OutputStream)
     * @see #storeToXml(Properties, String, boolean, Writer)
     * @see #loadFromXml(File)
     */
    public static void storeToXml(final Properties<?, ?> properties, final String rootElementName, final boolean writeTypeInfo, final File output) {
        validateXmlStructure(properties, rootElementName);

        OutputStream os = null;
        Writer writer = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            os = IOUtil.newFileOutputStream(output);
            writer = IOUtil.newOutputStreamWriter(os, Charsets.UTF_8);

            storeToXml(properties, rootElementName, writeTypeInfo, true, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(writer);
            IOUtil.close(os);
        }
    }

    /**
     * Stores the specified properties to the given XML OutputStream.
     * Each non-null mapping is written under its own key nested inside the root element;
     * null-valued mappings are omitted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Properties<String, Object> properties = new Properties<>();
     * properties.put("theme", "dark");
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
     * @param output the OutputStream to which the properties will be stored, encoded as UTF-8. The stream is flushed but not closed.
     * @throws NullPointerException if {@code properties}, {@code output}, or a key for a non-null value is {@code null}
     * @throws IllegalArgumentException if the root name or a property key is not a usable, namespace-free XML element name,
     *         or if nested {@code Properties} instances contain a reference cycle
     * @throws UncheckedIOException if an I/O error occurs while writing to the stream
     * @see #storeToXml(Properties, String, boolean, File)
     * @see #loadFromXml(InputStream)
     */
    public static void storeToXml(final Properties<?, ?> properties, final String rootElementName, final boolean writeTypeInfo, final OutputStream output)
            throws UncheckedIOException {
        validateXmlStructure(properties, rootElementName);

        final java.io.OutputStreamWriter writer = IOUtil.newOutputStreamWriter(output, Charsets.UTF_8);
        try {
            storeToXml(properties, rootElementName, writeTypeInfo, true, writer);
            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Stores the specified properties to the given XML Writer.
     * Each non-null mapping is written under its own key nested inside the root element;
     * null-valued mappings are omitted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Properties<String, Object> properties = new Properties<>();
     * properties.put("port", 8080);
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
     * @param output the Writer to which the properties will be stored. The writer is flushed but not closed.
     * @throws NullPointerException if {@code properties}, {@code output}, or a key for a non-null value is {@code null}
     * @throws IllegalArgumentException if the root name or a property key is not a usable, namespace-free XML element name,
     *         or if nested {@code Properties} instances contain a reference cycle
     * @throws UncheckedIOException if an I/O error occurs while writing to the writer
     * @see #storeToXml(Properties, String, boolean, File)
     * @see #loadFromXml(Reader)
     */
    public static void storeToXml(final Properties<?, ?> properties, final String rootElementName, final boolean writeTypeInfo, final Writer output)
            throws UncheckedIOException {
        validateXmlStructure(properties, rootElementName);
        storeToXml(properties, rootElementName, writeTypeInfo, true, output);
    }

    private static void validateXmlStructure(final Properties<?, ?> properties, final String rootElementName) {
        Objects.requireNonNull(properties, "properties");
        final Document document;

        try {
            // Use the JDK DOM factory directly. XML storing has no reason to acquire the optional
            // parser/JAXB dependencies used by XmlUtil's deserialization paths.
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (final ParserConfigurationException e) {
            throw new IllegalStateException("No DOM implementation is available for validating XML element names", e);
        }

        validateXmlElementName(rootElementName, "rootElementName", document);

        final Set<Properties<?, ?>> ancestors = Collections.newSetFromMap(new IdentityHashMap<>());
        validateXmlProperties(properties, document, ancestors);
    }

    private static void validateXmlProperties(final Properties<?, ?> properties, final Document document, final Set<Properties<?, ?>> ancestors) {
        if (!ancestors.add(properties)) {
            throw new IllegalArgumentException("Nested properties contain a reference cycle");
        }

        try {
            for (final Map.Entry<?, ?> entry : properties.entrySet()) {
                final Object value = entry.getValue();

                // Null-valued entries are deliberately omitted, so their keys are never emitted either.
                if (value == null) {
                    continue;
                }

                final Object key = Objects.requireNonNull(entry.getKey(), "property key");
                validateXmlElementName(String.valueOf(key), "property key", document);

                if (value instanceof Properties) {
                    validateXmlProperties((Properties<?, ?>) value, document, ancestors);
                }
            }
        } finally {
            ancestors.remove(properties);
        }
    }

    private static void validateXmlElementName(final String name, final String argumentName, final Document document) {
        if (Strings.isEmpty(name) || name.indexOf(':') >= 0) {
            throw new IllegalArgumentException(argumentName + " must be a non-empty, namespace-free XML element name: " + name);
        }

        try {
            document.createElement(name);
        } catch (final RuntimeException e) {
            throw new IllegalArgumentException(argumentName + " must be a valid XML element name: " + name, e);
        }
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
        final BufferedXmlWriter bw = Objectory.createBufferedXmlWriter(output);

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
            Object propValue = null;
            Type<Object> type = null;
            for (final Map.Entry<?, ?> entry : properties.entrySet()) { //NOSONAR
                propValue = entry.getValue();

                if (propValue == null) {
                    continue;
                }

                propName = entry.getKey().toString();

                if (propValue instanceof Properties) {
                    bw.flush();

                    storeToXml((Properties<?, ?>) propValue, propName, writeTypeInfo, false, output);
                } else {
                    type = Type.of(propValue.getClass());

                    if (writeTypeInfo) {
                        if (ClassUtil.isPrimitiveWrapper(type.javaType())) {
                            bw.write("<" + propName + " type=\"" + ClassUtil.getSimpleClassName(ClassUtil.unwrap(type.javaType())) + "\">");
                        } else {
                            // escape: parameterized declaring names contain '<'/'>', which are
                            // illegal inside an XML attribute value (the output couldn't be re-parsed).
                            bw.write("<" + propName + " type=\"" + escapeTypeAttr(type.declaringName()) + "\">");
                        }
                    } else {
                        bw.write("<" + propName + ">");
                    }

                    type.serializeTo(bw, propValue, xsc);

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
     * Escapes characters that are illegal inside an XML attribute value. Parameterized type
     * declaring names (e.g. {@code List<Object>}) contain markup characters; written raw
     * they can make the stored XML unparseable. The XML parser unescapes them on reload, so
     * {@code Type.of(...)} still receives the original name.
     *
     * @param typeName the type name to escape
     * @return the escaped attribute value
     */
    private static String escapeTypeAttr(final String typeName) {
        if (typeName.indexOf('<') < 0 && typeName.indexOf('>') < 0 && typeName.indexOf('&') < 0 && typeName.indexOf('"') < 0) {
            return typeName;
        }

        return typeName.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * Generates Java code from the specified XML string.
     * This method analyzes the XML structure and generates a corresponding Java class hierarchy that mirrors it,
     * with typed getters and setters backed by runtime casts. The generated class extends Properties&lt;String, Object&gt;.
     *
     * <p>The generated code includes:</p>
     * <ul>
     *   <li>Nested static classes for complex properties</li>
     *   <li>Getter and setter signatures derived from XML type attributes</li>
     *   <li>Automatic handling of property types based on XML type attributes</li>
     *   <li>Support for nested properties and container types recognized by {@link Type}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<config><database><url>jdbc:mysql://localhost</url></database></config>";
     * PropertiesUtil.xmlToJava(xml, "src/main/java", "com.example", "Config", false);
     * // Generates Config.java with typed getters/setters
     * }</pre>
     *
     * @param xml the XML content as a string.
     * @param srcPath the source path where the generated Java code will be saved (e.g., "src/main/java").
     * @param packageName the package name for the generated Java class, or {@code null}/empty for the default package.
     * @param className the name of the generated Java class; if {@code null}, the normalized and capitalized XML root name is used.
     * @param isPublicField currently has NO effect on the generated source: properties are stored in the inherited {@code Properties} map and no fields are emitted, so the generated class is identical for {@code true} and {@code false}.
     * @throws IllegalArgumentException if a source path or generated Java identifier is invalid, normalized sibling property names are duplicated,
     *         an unsupported type is declared, or a nested class would have the same name as an enclosing class
     * @throws RuntimeException if XML parsing or file I/O fails
     */
    public static void xmlToJava(final String xml, final String srcPath, final String packageName, final String className, final boolean isPublicField) {
        xmlToJava(IOUtil.stringToInputStream(xml), srcPath, packageName, className, isPublicField);
    }

    /**
     * Generates Java code from the specified XML file.
     * This method analyzes the XML structure and generates a corresponding Java class hierarchy that mirrors it,
     * with typed getters and setters backed by runtime casts. The generated class extends Properties&lt;String, Object&gt;.
     *
     * <p>The generated code includes:</p>
     * <ul>
     *   <li>Nested static classes for complex properties</li>
     *   <li>Getter and setter signatures derived from XML type attributes</li>
     *   <li>Automatic handling of property types based on XML type attributes</li>
     *   <li>Support for nested properties and container types recognized by {@link Type}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File xmlFile = new File("config.xml");
     * PropertiesUtil.xmlToJava(xmlFile, "src/main/java", "com.example", "AppConfig", false);
     * }</pre>
     *
     * @param xml the XML file from which to generate Java code.
     * @param srcPath the source path where the generated Java code will be saved (e.g., "src/main/java").
     * @param packageName the package name for the generated Java class, or {@code null}/empty for the default package.
     * @param className the name of the generated Java class; if {@code null}, the normalized and capitalized XML root name is used.
     * @param isPublicField currently has NO effect on the generated source: properties are stored in the inherited {@code Properties} map and no fields are emitted, so the generated class is identical for {@code true} and {@code false}.
     * @throws IllegalArgumentException if a source path or generated Java identifier is invalid, normalized sibling property names are duplicated,
     *         an unsupported type is declared, or a nested class would have the same name as an enclosing class
     * @throws RuntimeException if XML parsing or file I/O fails
     */
    public static void xmlToJava(final File xml, final String srcPath, final String packageName, final String className, final boolean isPublicField) {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(xml);

            xmlToJava(reader, srcPath, packageName, className, isPublicField);
        } finally {
            IOUtil.close(reader);
        }
    }

    /**
     * Generates Java code from the specified XML InputStream.
     * This method analyzes the XML structure and generates a corresponding Java class hierarchy that mirrors it,
     * with typed getters and setters backed by runtime casts. The generated class extends Properties&lt;String, Object&gt;.
     *
     * <p>The generated code includes:</p>
     * <ul>
     *   <li>Nested static classes for complex properties</li>
     *   <li>Getter and setter signatures derived from XML type attributes</li>
     *   <li>Automatic handling of property types based on XML type attributes</li>
     *   <li>Support for nested properties and container types recognized by {@link Type}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("schema.xml")) {
     *     PropertiesUtil.xmlToJava(is, "src/main/java", "com.example", "Schema", false);
     * }
     * }</pre>
     *
     * @param xml the InputStream from which to generate Java code; it is not closed by this method.
     * @param srcPath the source path where the generated Java code will be saved (e.g., "src/main/java").
     * @param packageName the package name for the generated Java class, or {@code null}/empty for the default package.
     * @param className the name of the generated Java class; if {@code null}, the normalized and capitalized XML root name is used.
     * @param isPublicField currently has NO effect on the generated source: properties are stored in the inherited {@code Properties} map and no fields are emitted, so the generated class is identical for {@code true} and {@code false}.
     * @throws IllegalArgumentException if a source path or generated Java identifier is invalid, normalized sibling property names are duplicated,
     *         an unsupported type is declared, or a nested class would have the same name as an enclosing class
     * @throws RuntimeException if XML parsing or file I/O fails
     */
    public static void xmlToJava(final InputStream xml, final String srcPath, final String packageName, final String className, final boolean isPublicField) {
        xmlToJava(IOUtil.newInputStreamReader(xml), srcPath, packageName, className, isPublicField);
    }

    /**
     * Generates Java code from the specified XML Reader.
     * This method parses the XML and generates a Java class hierarchy that mirrors the XML structure,
     * with typed getters and setters backed by runtime casts. The generated class extends Properties&lt;String, Object&gt;.
     *
     * <p>The generated code includes:</p>
     * <ul>
     *   <li>Nested static classes for complex properties</li>
     *   <li>Getter and setter signatures derived from XML type attributes</li>
     *   <li>Automatic handling of property types based on XML type attributes</li>
     *   <li>Support for nested properties and container types recognized by {@link Type}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("schema.xml")) {
     *     PropertiesUtil.xmlToJava(reader, "src/main/java", "com.example.config", null, false);
     *     // Generates a Java class using the XML root element name
     * }
     * }</pre>
     *
     * @param xml the Reader from which to generate Java code; it is not closed by this method.
     * @param srcPath the source path where the generated Java code will be saved (e.g., "src/main/java").
     * @param packageName the package name for the generated Java class, or {@code null}/empty for the default package.
     * @param className the name of the generated Java class. If {@code null}, uses the normalized and capitalized root element name.
     * @param isPublicField currently has NO effect on the generated source: properties are stored in the inherited {@code Properties} map and no fields are emitted, so the generated class is identical for {@code true} and {@code false}.
     * @throws IllegalArgumentException if a source path or generated Java identifier is invalid, normalized sibling property names are duplicated,
     *         an unsupported type is declared, or a nested class would have the same name as an enclosing class
     * @throws RuntimeException if XML parsing or file I/O fails
     */
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    public static void xmlToJava(final Reader xml, final String srcPath, final String packageName, String className, final boolean isPublicField) {
        N.checkArgNotEmpty(srcPath, "srcPath");
        validatePackageName(packageName);

        final DocumentBuilder docBuilder = XmlUtil.createDOMParser(true, true);
        Writer writer = null;

        try { //NOSONAR
            final Document doc = docBuilder.parse(new InputSource(xml));
            final Node root = doc.getDocumentElement();

            if (root == null) {
                throw new RuntimeException("No document element found in XML source");
            }

            // Normalized sibling names would generate duplicate accessors and map keys.
            if (hasDuplicatedPropName(root)) {
                throw new RuntimeException("The source XML document contains sibling element names that collide after property-name normalization.");
            }

            if (className == null) {
                className = generatedClassName(root);
            } else {
                checkJavaIdentifier(className, "className");
            }

            validateGeneratedStructure(root, className, N.newHashSet());

            // Generate completely in memory first. Invalid type declarations or identifiers must not
            // delete/truncate an existing source file before generation has succeeded.
            final java.io.StringWriter generatedSource = new java.io.StringWriter();

            if (Strings.isNotEmpty(packageName)) {
                generatedSource.write("package " + packageName + ";" + IOUtil.LINE_SEPARATOR_UNIX);
                generatedSource.write(IOUtil.LINE_SEPARATOR_UNIX);
                generatedSource.write(IOUtil.LINE_SEPARATOR_UNIX);
            }

            xmlPropertiesToJava(root, className, isPublicField, "", true, generatedSource);

            final String classFilePath = ClassUtil.makeFolderForPackage(srcPath, Strings.isEmpty(packageName) ? null : packageName);
            final File classFile = new File(classFilePath + className + ".java");

            writer = IOUtil.newFileWriter(classFile, Charsets.UTF_8);
            writer.write(generatedSource.toString());
        } catch (final Exception e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            IOUtil.close(writer);
        }
    }

    private static void validatePackageName(final String packageName) {
        if (Strings.isEmpty(packageName)) {
            return;
        }

        for (final String identifier : packageName.split("\\.", -1)) {
            checkJavaIdentifier(identifier, "packageName");
        }
    }

    private static void checkJavaIdentifier(final String identifier, final String argumentName) {
        if (!Strings.isValidJavaIdentifier(identifier)) {
            throw new IllegalArgumentException(argumentName + " must be a valid Java identifier: " + identifier);
        }
    }

    private static String generatedClassName(final Node node) {
        final String className = Strings.capitalize(Beans.normalizePropName(node.getNodeName()));
        checkJavaIdentifier(className, "XML element name");
        return className;
    }

    private static void validateGeneratedStructure(final Node node, final String className, final Set<String> enclosingClassNames) {
        checkJavaIdentifier(className, "generated class name");

        if (!enclosingClassNames.add(className)) {
            throw new IllegalArgumentException("A generated nested class has the same name as an enclosing class: " + className);
        }

        try {
            final NodeList childNodes = node.getChildNodes();

            for (int i = 0, len = childNodes.getLength(); i < len; i++) {
                final Node childNode = childNodes.item(i);

                if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                final String propName = Beans.normalizePropName(childNode.getNodeName());
                checkJavaIdentifier(propName, "normalized XML property name");

                if (!XmlUtil.isTextElement(childNode) && Strings.isEmpty(XmlUtil.getAttribute(childNode, TYPE))) {
                    validateGeneratedStructure(childNode, generatedClassName(childNode), enclosingClassNames);
                }
            }
        } finally {
            enclosingClassNames.remove(className);
        }
    }

    private static void xmlPropertiesToJava(final Node xmlNode, String className, final boolean isPublicField, final String spaces, final boolean isRoot,
            final Writer output) throws IOException {
        if (className == null) {
            className = generatedClassName(xmlNode);
        }

        output.write(IOUtil.LINE_SEPARATOR_UNIX);

        if (isRoot) {
            output.write(spaces + "/**" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(spaces + " * Auto-generated by Abacus." + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(spaces + " */" + IOUtil.LINE_SEPARATOR_UNIX);
            output.write(spaces + "public class " + className + " extends " + Properties.class.getCanonicalName() + "<String, Object> {"
                    + IOUtil.LINE_SEPARATOR_UNIX);
        } else {
            output.write(spaces + "public static class " + className + " extends " + Properties.class.getCanonicalName() + "<String, Object> {"
                    + IOUtil.LINE_SEPARATOR_UNIX);
        }

        final NodeList childNodes = xmlNode.getChildNodes();

        //noinspection ConstantValue
        if ((childNodes != null) && (childNodes.getLength() > 0)) {
            final Set<String> propNameSet = N.newHashSet();

            Node childNode = null;
            String propName = null;

            final String methodSpace = spaces + "    ";
            String typeName = null;

            for (int i = 0; i < childNodes.getLength(); i++) {
                childNode = childNodes.item(i);

                if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                propName = Beans.normalizePropName(childNode.getNodeName());

                if (propNameSet.contains(propName)) {
                    continue;
                }

                propNameSet.add(propName);

                output.write(IOUtil.LINE_SEPARATOR_UNIX);

                typeName = getTypeName(childNode, propName);

                writeMethod(methodSpace, propName, typeName, output);
            }

            // Retain generic mutation methods for compatibility, but deprecate them in favor of
            // the generated property-specific accessors.
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
            output.write(methodSpace + "public void putAll(java.util.Map<? extends String, ? extends Object> m) {" + IOUtil.LINE_SEPARATOR_UNIX);
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

                if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                propName = Beans.normalizePropName(childNode.getNodeName());

                if (propNameSet.contains(propName) || Strings.isNotEmpty(XmlUtil.getAttribute(childNode, TYPE))) {
                    continue;
                }

                propNameSet.add(propName);

                // !isTextElement = has at least one element child. Don't use getChildNodes().getLength() > 1:
                // in compact XML (no whitespace text nodes, e.g. the output of storeToXml) a nested element
                // with a single child element has length 1, while loadFromXml still treats it as a nested
                // Properties (XmlUtil.isTextElement). The two must agree or the generated accessors are wrong.
                if (!XmlUtil.isTextElement(childNode)) {
                    xmlPropertiesToJava(childNode, null, isPublicField, spaces + "    ", false, output);
                }
            }
        }

        output.write(spaces + "}" + IOUtil.LINE_SEPARATOR_UNIX);
    }

    private static void writeMethod(final String spaces, final String propName, final String typeName, final Writer output) throws IOException {
        output.write(spaces + "public " + typeName + " get" + Strings.capitalize(propName) + "() {" + IOUtil.LINE_SEPARATOR_UNIX);
        output.write(spaces + "    " + "return (" + typeName + ") super.get(\"" + propName + "\");" + IOUtil.LINE_SEPARATOR_UNIX);
        output.write(spaces + "}" + IOUtil.LINE_SEPARATOR_UNIX);

        output.write(IOUtil.LINE_SEPARATOR_UNIX);

        output.write(spaces + "public void set" + Strings.capitalize(propName) + "(" + typeName + " " + propName + ") {" + IOUtil.LINE_SEPARATOR_UNIX);
        output.write(spaces + "    " + "super.put(\"" + propName + "\", " + propName + ");" + IOUtil.LINE_SEPARATOR_UNIX);

        output.write(spaces + "}" + IOUtil.LINE_SEPARATOR_UNIX);

        output.write(IOUtil.LINE_SEPARATOR_UNIX);

        output.write(spaces + "public void remove" + Strings.capitalize(propName) + "() {" + IOUtil.LINE_SEPARATOR_UNIX);
        output.write(spaces + "    " + "super.remove(\"" + propName + "\");" + IOUtil.LINE_SEPARATOR_UNIX);
        output.write(spaces + "}" + IOUtil.LINE_SEPARATOR_UNIX);
    }

    private static String getTypeName(final Node node, final String propName) {
        // A node with an element child is a nested Properties type; a text-only node is a String property.
        // Must match loadFromXml's detection (XmlUtil.isTextElement) — see comment in xmlPropertiesToJava.
        String typeName = XmlUtil.isTextElement(node) ? "String" : Strings.capitalize(propName);
        final String typeAttr = XmlUtil.getAttribute(node, TYPE);

        if (Strings.isNotEmpty(typeAttr)) {
            if (typeAttr.equals("Properties")) {
                typeName = Properties.class.getCanonicalName() + "<String, Object>";
            } else {
                final Type<?> type = Type.of(typeAttr);
                if (type == null || type.javaType() == void.class) {
                    throw new IllegalArgumentException("Unsupported XML property type: " + typeAttr);
                }

                typeName = type.javaType().getCanonicalName();

                if (typeName == null) {
                    throw new IllegalArgumentException("The XML property type has no canonical Java source name: " + typeAttr);
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

            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            propName = Beans.normalizePropName(childNode.getNodeName());

            // !isTextElement (instead of getChildNodes().getLength() > 1) so duplicates nested below a
            // compact single-child chain are still found — xmlToJava only checks the document root.
            if (propNameSet.contains(propName) || (!XmlUtil.isTextElement(childNode) && hasDuplicatedPropName(childNode))) {
                return true;
            } else {
                propNameSet.add(propName);
            }
        }

        return false;
    }

    /**
     * Internal value object used to represent a configuration bean with common metadata fields
     * such as id, name, content, server inclusion/exclusion lists, status, and timestamps.
     * This class is used internally for configuration management and is not part of the public API.
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
        private UnifiedStatus unifiedStatus;

        /** The description. */
        private String description;

        /** The last update time. */
        private Timestamp lastUpdateTime;

        /** The created time. */
        private Timestamp createdTime;

        /**
         * Returns the id.
         *
         * @return the id value
         */
        public long getId() {
            return id;
        }

        /**
         * Sets the id.
         *
         * @param id the id value to set
         */
        public void setId(final long id) {
            this.id = id;
        }

        /**
         * Returns the name.
         *
         * @return the name, or {@code null} if not set
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the name.
         *
         * @param name the name to set
         */
        public void setName(final String name) {
            this.name = name;
        }

        /**
         * Returns the content.
         *
         * @return the content, or {@code null} if not set
         */
        public String getContent() {
            return content;
        }

        /**
         * Sets the content.
         *
         * @param content the content to set
         */
        public void setContent(final String content) {
            this.content = content;
        }

        /**
         * Returns the included servers.
         *
         * @return the list of included servers, or {@code null} if not set
         */
        public List<String> getIncludedServers() {
            return includedServers;
        }

        /**
         * Sets the included servers.
         *
         * @param includedServers the list of included servers to set
         */
        public void setIncludedServers(final List<String> includedServers) {
            this.includedServers = includedServers;
        }

        /**
         * Returns the excluded servers.
         *
         * @return the list of excluded servers, or {@code null} if not set
         */
        public List<String> getExcludedServers() {
            return excludedServers;
        }

        /**
         * Sets the excluded servers.
         *
         * @param excludedServers the list of excluded servers to set
         */
        public void setExcludedServers(final List<String> excludedServers) {
            this.excludedServers = excludedServers;
        }

        /**
         * Returns the status.
         *
         * @return the status, or {@code null} if not set
         */
        public UnifiedStatus getStatus() {
            return unifiedStatus;
        }

        /**
         * Sets the status.
         *
         * @param unifiedStatus the status to set
         */
        public void setStatus(final UnifiedStatus unifiedStatus) {
            this.unifiedStatus = unifiedStatus;
        }

        /**
         * Returns the description.
         *
         * @return the description, or {@code null} if not set
         */
        public String getDescription() {
            return description;
        }

        /**
         * Sets the description.
         *
         * @param description the description to set
         */
        public void setDescription(final String description) {
            this.description = description;
        }

        /**
         * Returns the last update time.
         *
         * @return the last update time, or {@code null} if not set
         */
        public Timestamp getLastUpdateTime() {
            return lastUpdateTime;
        }

        /**
         * Sets the last update time.
         *
         * @param lastUpdateTime the last update time to set
         */
        public void setLastUpdateTime(final Timestamp lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }

        /**
         * Returns the created time.
         *
         * @return the created time, or {@code null} if not set
         */
        public Timestamp getCreatedTime() {
            return createdTime;
        }

        /**
         * Sets the created time.
         *
         * @param createdTime the created time to set
         */
        public void setCreatedTime(final Timestamp createdTime) {
            this.createdTime = createdTime;
        }

        /**
         * Returns the hash code for this ConfigBean, computed from all fields.
         *
         * @return the hash code value for this object
         */
        @Override
        public int hashCode() {
            return Objects.hash(id, name, content, includedServers, excludedServers, unifiedStatus, description, lastUpdateTime, createdTime);
        }

        /**
         * Compares this ConfigBean to the specified object for equality.
         * Two ConfigBean instances are equal if all their fields are equal.
         *
         * @param obj the object to compare with
         * @return {@code true} if the specified object is a ConfigBean with equal field values
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof ConfigBean other) {
                return N.equals(other.id, id) && N.equals(other.name, name) && N.equals(other.content, content)
                        && N.equals(other.includedServers, includedServers) && N.equals(other.excludedServers, excludedServers)
                        && N.equals(other.unifiedStatus, unifiedStatus) && N.equals(other.description, description)
                        && N.equals(other.lastUpdateTime, lastUpdateTime) && N.equals(other.createdTime, createdTime);

            }

            return false;
        }

        /**
         * Returns a string representation of this ConfigBean containing all field values.
         *
         * @return a string representation of this object
         */
        @Override
        public String toString() {
            return "{id=" + id + ", name=" + name + ", content=" + content + ", includedServers=" + includedServers + ", excludedServers=" + excludedServers
                    + ", status=" + unifiedStatus + ", description=" + description + ", lastUpdateTime=" + lastUpdateTime + ", createdTime=" + createdTime
                    + "}";
        }

    }

    /**
     * Enumerates the supported source formats for properties that can be loaded and auto-refreshed.
     */
    enum ResourceType {
        /** Standard Java {@code .properties} format (key=value lines). */
        PROPERTIES,
        /** XML format, loaded via {@link PropertiesUtil#loadFromXml}. */
        XML
    }

    /**
     * Internal descriptor for a registered auto-refresh resource, tracking the source file,
     * the target Properties class, and the timestamp of the last successful load.
     * Two Resource instances are considered equal when their target class, normalized file path,
     * and resource type all match.
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
         * Constructs a new {@code Resource}.
         *
         * @param cls the target class for the resource
         * @param file the file representing the resource
         * @param resourceType the type of the resource
         */
        public Resource(final Class<?> cls, final File file, final ResourceType resourceType) {
            targetClass = cls;
            this.file = normalizeFilePath(file);
            filePath = this.file.getPath();
            this.resourceType = resourceType;
        }

        /**
         * Returns the last load time.
         *
         * @return the last load time in milliseconds since epoch
         */
        public long getLastLoadTime() {
            return lastLoadTime;
        }

        /**
         * Sets the last load time.
         *
         * @param lastLoadTime the last load time to set
         */
        public void setLastLoadTime(final long lastLoadTime) {
            this.lastLoadTime = lastLoadTime;
        }

        /**
         * Returns the file.
         *
         * @return the File object associated with this resource
         */
        public File getFile() {
            return file;
        }

        /**
         * Returns the type.
         *
         * @return the ResourceType (PROPERTIES or XML)
         */
        public ResourceType getType() {
            return resourceType;
        }

        /**
         * Returns the hash code for this Resource, based on target class, file path, and resource type.
         *
         * @return the hash code value for this object
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(targetClass);
            result = prime * result + N.hashCode(filePath);
            return prime * result + N.hashCode(resourceType);
        }

        /**
         * Compares this Resource to the specified object for equality.
         * Two Resource instances are equal when their target class, file path, and resource type all match.
         *
         * @param obj the object to compare with
         * @return {@code true} if the specified object is a Resource with equal target class, file path, and type
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Resource other) {
                return N.equals(other.targetClass, targetClass) && N.equals(other.filePath, filePath) && N.equals(other.resourceType, resourceType);

            }

            return false;
        }

        /**
         * Returns a string representation of this Resource showing the associated file.
         *
         * @return a string representation of this object
         */
        @Override
        public String toString() {
            return "{file=" + file + "}";
        }
    }
}
