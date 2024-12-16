/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

/**
 * The Enum DateTimeFormat.
 *
 */
public enum DateTimeFormat {
    /**
     * It's long number.
     */
    LONG,

    /** The format is: yyyy-MM-dd'T'HH:mm:ss'Z'. */
    ISO_8601_DATE_TIME,
    /**
     * The format is: yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
     */
    ISO_8601_TIMESTAMP
}
