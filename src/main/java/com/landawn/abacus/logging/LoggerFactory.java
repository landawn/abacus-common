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

package com.landawn.abacus.logging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.landawn.abacus.annotation.SuppressFBWarnings;

/**
 * A factory for creating Logger objects with automatic detection of the logging framework.
 * 
 * <p>This factory automatically detects and initializes the appropriate logging implementation
 * in the following order of preference:</p>
 * <ol>
 *   <li>Android Logger (on Android platform)</li>
 *   <li>SLF4J Logger</li>
 *   <li>Log4j v2 Logger</li>
 *   <li>JDK Logger (fallback)</li>
 * </ol>
 * 
 * <p>The factory maintains a cache of logger instances to avoid creating multiple instances
 * for the same logger name.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Logger logger = LoggerFactory.getLogger(MyClass.class);
 * logger.info("Application started");
 * }</pre>
 * 
 * @since 1.0
 */
public final class LoggerFactory {

    private static final String JAVA_VENDOR = System.getProperty("java.vendor");

    private static final String JAVA_VM_VENDOR = System.getProperty("java.vm.vendor");

    private static final boolean IS_ANDROID_PLATFORM = JAVA_VENDOR.toUpperCase().contains("ANDROID") || JAVA_VM_VENDOR.contains("ANDROID");

    private static final Logger jdkLogger = new JDKLogger(LoggerFactory.class.getName());

    private static final Map<String, Logger> namedLoggers = new ConcurrentHashMap<>();

    private static volatile int logType = 0;

    private static volatile boolean initialized = false;

    private LoggerFactory() {
        // singleton.
    }

    /**
     * Gets a logger instance for the specified class.
     * 
     * <p>This method creates a logger with the fully qualified class name. If a logger
     * with the same name already exists, the cached instance is returned.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * public class MyService {
     *     private static final Logger logger = LoggerFactory.getLogger(MyService.class);
     *     
     *     public void doSomething() {
     *         logger.debug("Starting operation");
     *     }
     * }
     * }</pre>
     *
     * @param clazz the class for which to get the logger
     * @return a Logger instance for the specified class
     */
    public static synchronized Logger getLogger(final Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * Gets a logger instance for the specified name.
     * 
     * <p>This method returns a cached logger if one exists for the given name, otherwise
     * it creates a new logger using the detected logging framework. The detection happens
     * only once during the first logger creation.</p>
     * 
     * <p>The logging framework detection order is:</p>
     * <ol>
     *   <li>Android Logger (only on Android platform)</li>
     *   <li>SLF4J Logger</li>
     *   <li>Log4j v2 Logger</li>
     *   <li>JDK Logger (fallback)</li>
     * </ol>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Logger logger = LoggerFactory.getLogger("com.mycompany.MyComponent");
     * logger.warn("Configuration not found, using defaults");
     * }</pre>
     *
     * @param name the name of the logger
     * @return a Logger instance for the specified name
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    @SuppressWarnings("fallthrough")
    public static synchronized Logger getLogger(final String name) {
        Logger logger = namedLoggers.get(name);

        if (logger == null) {
            switch (logType) {
                case 0:
                    if (IS_ANDROID_PLATFORM) {
                        try {
                            logger = (Logger) Class.forName("com.landawn.abacus.logging.AndroidLogger").getDeclaredConstructor(String.class).newInstance(name);

                            if (!initialized) {
                                jdkLogger.info("Initialized with Android Logger");
                                logger.info("Initialized with Android Logger");
                            }

                            logType = 0;
                            initialized = true;

                            break;
                        } catch (final Throwable e) {
                            // ignore
                        }
                    }

                case 1:
                    if (logger == null) {
                        try {
                            logger = new SLF4JLogger(name);

                            if (!initialized) {
                                jdkLogger.info("Initialized with SLF4J Logger");
                                logger.info("Initialized with SLF4J Logger");
                            }

                            logType = 1;
                            initialized = true;

                            break;
                        } catch (final Throwable e) {
                            // ignore
                        }
                    }

                case 2:
                    if (logger == null) {
                        try {
                            logger = new Log4Jv2Logger(name);

                            if (!initialized) {
                                jdkLogger.info("Initialized with Log4j v2 Logger");
                                logger.info("Initialized with Log4j v2 Logger");
                            }

                            logType = 2;
                            initialized = true;

                            break;
                        } catch (final Throwable e) {
                            // ignore
                        }
                    }

                case 3:
                default:
                    if (logger == null) {
                        logger = new JDKLogger(name);

                        if (!initialized) {
                            jdkLogger.info("Initialized with JDK Logger");
                            logger.info("Initialized with JDK Logger");
                        }

                        logType = 3;
                        initialized = true;

                        break;
                    }
            }

            namedLoggers.put(name, logger);
        }

        return logger;
    }
}