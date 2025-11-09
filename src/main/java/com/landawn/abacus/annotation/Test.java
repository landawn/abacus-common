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

package com.landawn.abacus.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks elements that are intended for testing purposes only and should not be used in production code.
 * This annotation serves as a clear indicator that the annotated element exists solely to support
 * testing infrastructure and may have relaxed access modifiers or special behavior for testability.
 * 
 * <p>Elements marked with {@code @Test} may include:</p>
 * <ul>
 *   <li>Methods or constructors with package-private or public access for test access</li>
 *   <li>Fields exposed for test verification</li>
 *   <li>Classes designed as test doubles or mocks</li>
 *   <li>Special test-only implementations</li>
 * </ul>
 * 
 * <p>This annotation helps maintain clear boundaries between production and test code,
 * making it easier to identify code that should not be used in production contexts.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 *
 * public class UserService {
 *     private ConcurrentHashMap<String, User> cache = new ConcurrentHashMap<>();
 *
 *     @Test
 *     void resetForTesting() {
 *         // Method exposed only for test cleanup
 *         cache.clear();
 *     }
 *
 *     @Test
 *     UserService(CacheProvider provider) {
 *         // Constructor with package-private access for testing
 *         this.cache = provider.getCache();
 *     }
 * }
 *
 * @Test
 * public class MockUserRepository implements UserRepository {
 *     // Test double implementation
 *     private List<User> testData = new ArrayList<>();
 *
 *     @Override
 *     public User findById(Long id) {
 *         return testData.stream()
 *             .filter(u -> u.getId().equals(id))
 *             .findFirst()
 *             .orElse(null);
 *     }
 * }
 * }</pre>
 *
 * @see Internal
 * @see Beta
 */
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = { ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface Test {

}
