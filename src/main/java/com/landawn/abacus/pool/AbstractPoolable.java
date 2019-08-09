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

package com.landawn.abacus.pool;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractPoolable.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class AbstractPoolable implements Poolable {

    /** The activity print. */
    final ActivityPrint activityPrint;

    /**
     * Instantiates a new abstract poolable.
     *
     * @param liveTime the live time
     * @param maxIdleTime the max idle time
     */
    protected AbstractPoolable(long liveTime, long maxIdleTime) {
        activityPrint = new ActivityPrint(liveTime, maxIdleTime);
    }

    /**
     * Activity print.
     *
     * @return the activity print
     */
    @Override
    public ActivityPrint activityPrint() {
        return activityPrint;
    }
}
