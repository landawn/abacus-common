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
 * A logging facade with adapters for commonly used logging frameworks.
 *
 * <p>{@link com.landawn.abacus.logging.LoggerFactory} detects an available backend and creates cached
 * {@link com.landawn.abacus.logging.Logger} instances. Supported backends include SLF4J, Log4j 2,
 * and {@link java.util.logging}, with platform-specific logging available through optional modules.</p>
 */
package com.landawn.abacus.logging;
