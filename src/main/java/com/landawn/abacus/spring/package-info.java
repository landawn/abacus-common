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
 * Integration between Abacus JSON serialization and the Spring Framework.
 *
 * <p>{@link JsonHttpMessageConverter} extends Spring's {@code AbstractJsonHttpMessageConverter}
 * and delegates JSON read/write to Abacus {@link com.landawn.abacus.util.N} and
 * {@link com.landawn.abacus.type.TypeFactory}. Register it in Spring MVC or on a
 * {@code RestTemplate} message-converter list to use Abacus JSON for
 * {@code application/json}.</p>
 *
 * <p>Root JDK and Abacus {@code Optional} values, Abacus {@code Nullable}, and {@code Holder} use
 * the JSON representation of their contained value. Entries, pairs, tuples, indexed/timed values,
 * primitive lists, and custom value objects keep their type-handler representation. Spring must be
 * on the classpath.</p>
 *
 * @see JsonHttpMessageConverter
 * @see com.landawn.abacus.parser.ParserFactory
 */
package com.landawn.abacus.spring;
