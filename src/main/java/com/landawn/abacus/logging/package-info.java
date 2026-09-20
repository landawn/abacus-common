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

/**
 * A logging facade with automatic backend detection.
 *
 * <p>{@link LoggerFactory#getLogger(Class)} and {@link LoggerFactory#getLogger(String)} return cached
 * {@link Logger} instances. The factory initially detects a backend in this order, then reuses that
 * selection unless creating a later logger requires falling back again:</p>
 * <ol>
 *   <li>Android logger, loaded reflectively from a separate module when running on an Android JVM</li>
 *   <li>SLF4J</li>
 *   <li>Log4j 2</li>
 *   <li>{@link java.util.logging.Logger JDK java.util.logging} (always available fallback)</li>
 * </ol>
 *
 * <p>Failure to initialize an optional backend causes the factory to try the next one.
 * {@link VirtualMachineError} and {@link ThreadDeath} are propagated rather than treated as a
 * logging fallback. Application code uses {@link Logger}; {@link AbstractLogger} is the base class
 * for backend adapters and is not the usual extension point.</p>
 *
 * <p>{@link Logger} supports TRACE through ERROR, SLF4J-style {@code {}} and printf {@code %s}
 * placeholders, up to seven substitution arguments, {@link java.util.function.Supplier}-based lazy
 * messages, and both {@code (Throwable, String)} and {@code (String, Throwable)} parameter orders.</p>
 *
 * @see LoggerFactory
 * @see Logger
 */
package com.landawn.abacus.logging;
