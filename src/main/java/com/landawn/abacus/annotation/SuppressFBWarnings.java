/*
 * Copyright (C) 2020 HaiYang Li
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation used to suppress FindBugs warnings on annotated program elements.
 * FindBugs is a static analysis tool that detects potential bugs and security vulnerabilities
 * in Java code. This annotation allows developers to suppress specific warnings when they
 * are false positives or when the code is intentionally designed a certain way.
 * 
 * <p>This annotation should be used judiciously and only when:</p>
 * <ul>
 *   <li>You have thoroughly analyzed the warning and determined it's a false positive</li>
 *   <li>The code design intentionally violates a FindBugs rule for valid reasons</li>
 *   <li>The warning cannot be fixed without significant architectural changes</li>
 *   <li>You have provided proper justification for suppressing the warning</li>
 * </ul>
 * 
 * <p><b>Best practices:</b></p>
 * <ul>
 *   <li>Always provide a meaningful justification when suppressing warnings</li>
 *   <li>Be as specific as possible with warning categories or patterns</li>
 *   <li>Apply the annotation to the smallest scope possible</li>
 *   <li>Regularly review suppressed warnings to ensure they're still valid</li>
 *   <li>Consider fixing the underlying issue instead of suppressing when feasible</li>
 * </ul>
 * 
 * <p><b>Common FindBugs warning categories:</b></p>
 * <ul>
 *   <li>SECURITY - Security-related vulnerabilities</li>
 *   <li>PERFORMANCE - Performance issues</li>
 *   <li>CORRECTNESS - Correctness concerns</li>
 *   <li>MT_CORRECTNESS - Multi-threading issues</li>
 *   <li>BAD_PRACTICE - Violations of recommended coding practices</li>
 *   <li>STYLE - Code style issues</li>
 * </ul>
 * 
 * <p><b>Example usage:</b></p>
 * <pre>
 * // Suppress a specific bug pattern
 * {@literal @}SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH", 
 *                         justification = "Null check performed by validation framework")
 * public void processData(String data) {
 *     // Method implementation
 * }
 * 
 * // Suppress multiple warnings
 * {@literal @}SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
 *                         justification = "Intentional exposure for performance reasons")
 * public Date[] getDates() {
 *     return this.dates;
 * }
 * 
 * // Suppress by category
 * {@literal @}SuppressFBWarnings(value = "SECURITY", 
 *                         justification = "Security handled by external framework")
 * public class LegacyAuthenticator {
 *     // Class implementation
 * }
 * </pre>
 * 
 * @author HaiYang Li
 * @since 2020
 * @see <a href="https://spotbugs.readthedocs.io/en/stable/">SpotBugs Documentation</a>
 * @see <a href="https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html">Bug Descriptions</a>
 */
@Retention(RetentionPolicy.CLASS)
public @interface SuppressFBWarnings {

    /**
     * The set of FindBugs warnings that are to be suppressed in an annotated element.
     * The value can be a bug category, kind, or specific pattern.
     * 
     * <p><b>Value types:</b></p>
     * <ul>
     *   <li><b>Bug categories:</b> "SECURITY", "PERFORMANCE", "CORRECTNESS", etc.</li>
     *   <li><b>Bug patterns:</b> "NP_NULL_ON_SOME_PATH", "EI_EXPOSE_REP", etc.</li>
     *   <li><b>Bug kinds:</b> "SECURITY", "STYLE", "MT_CORRECTNESS", etc.</li>
     * </ul>
     * 
     * <p><b>Examples:</b></p>
     * <ul>
     *   <li>{"NP_NULL_ON_SOME_PATH"} - Suppress specific null pointer warnings</li>
     *   <li>{"SECURITY"} - Suppress all security-related warnings</li>
     *   <li>{"EI_EXPOSE_REP", "EI_EXPOSE_REP2"} - Suppress representation exposure warnings</li>
     * </ul>
     * 
     * @return array of FindBugs warning identifiers to suppress
     */
    String[] value() default {};

    /**
     * Optional documentation of the reason why the warning is suppressed.
     * This field should provide a clear explanation of why suppressing the warning is appropriate.
     * 
     * <p><b>Best practices for justifications:</b></p>
     * <ul>
     *   <li>Explain why the warning is a false positive</li>
     *   <li>Describe the intentional design decision</li>
     *   <li>Reference external validation or security measures</li>
     *   <li>Note if the issue is addressed elsewhere in the codebase</li>
     * </ul>
     * 
     * <p><b>Example justifications:</b></p>
     * <ul>
     *   <li>"Null check performed by validation framework"</li>
     *   <li>"Intentional exposure for API compatibility"</li>
     *   <li>"Thread safety guaranteed by external synchronization"</li>
     *   <li>"Performance critical code, checked thoroughly in tests"</li>
     * </ul>
     * 
     * @return the justification for suppressing the warning, empty string if not provided
     */
    String justification() default "";
}
