/*
 * Copyright (C) 2019 HaiYang Li
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.DecimalFormat;

import com.landawn.abacus.annotation.Type.EnumBy;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonXmlField {

    String name() default "";

    String[] alias() default {};

    String type() default "";

    EnumBy enumerated() default EnumBy.NAME;

    String dateFormat() default "";

    String timeZone() default "";

    /**
     *
     * @return
     * @see DecimalFormat
     */
    String numberFormat() default "";

    /**
     *
     * @return {@code true}, if successful
     */
    boolean ignore() default false;

    /**
     *
     * @return {@code true} if the target field type is a JSON String.
     */
    boolean isJsonRawValue() default false;

    //    /**
    //     *
    //     * @return
    //     * @deprecated should use the order of fields defined.
    //     */
    //    @Deprecated
    //    int ordinal() default -1;

    Expose expose() default Expose.DEFAULT;

    enum Expose {
        /**
         * @deprecated don't need to set it. It's {@code DEFAULT} by default.
         */
        @Deprecated
        DEFAULT,

        SERIALIZE_ONLY,

        DESERIALIZE_ONLY
    }
}
