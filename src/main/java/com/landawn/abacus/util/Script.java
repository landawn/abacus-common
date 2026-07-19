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

/**
 * An empty compatibility shell that exposes no script execution functionality.
 *
 * <p>This type does not evaluate, execute, or sandbox scripts and must not be treated as
 * an execution or security API. It is retained as a non-instantiable public type for compatibility.
 *
 * <p>The class is declared {@code final} to prevent subclassing. The {@code //NOSONAR} comment
 * suppresses SonarQube warnings about the otherwise-empty class body.
 */
public final class Script { //NOSONAR

    /**
     * Private constructor to prevent instantiation.
     */
    private Script() {
    }

}
