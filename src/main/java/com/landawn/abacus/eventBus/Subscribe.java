/*
 * Copyright (C) 2016 HaiYang Li
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

package com.landawn.abacus.eventBus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.landawn.abacus.util.ThreadMode;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {
    ThreadMode threadMode() default ThreadMode.DEFAULT;

    /**
     * Only accept the events which have extract same class type as the method parameter if it's true.
     * Otherwise, accept all the events which can be assigned to the parameter type.
     * The precondition for both <code>true</code> and <code>false</code> is the event id has to match.
     * 
     * @return
     */
    boolean strictEventType() default false;

    /**
     * If true, delivers the most recent sticky event (posted with
     * {@link EventBus#postSticky(Object)}) to this subscriber (if event available).
     * 
     * @return
     */
    boolean sticky() default false;

    /**
     * Only subscribe the events which are posted with the specified event id.
     * 
     * @return
     */
    String eventId() default "";

    /**
     * The event will be ignored if the interval between this event and last event is less than the specified <code>interval</code>.
     * 
     * @return
     */
    long interval() default 0; // Unit is milliseconds.

    /**
     * Ignore next event if it's same as previous one.
     * 
     * @return
     */
    boolean deduplicate() default false;
}
