/*
 * Copyright (c) 2015, Haiyang Li.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.HashMap;
import java.util.Map;

import com.landawn.abacus.annotation.Internal;

// TODO: Auto-generated Javadoc
/**
 * The Enum SQLOperation.
 *
 * @author Haiyang Li
 * @since 0.8
 */
@Internal
public enum SQLOperation {
    /**
     * Field SELECT.
     */
    SELECT(WD.SELECT),
    /**
     * Field INSERT.
     */
    INSERT(WD.INSERT),
    /**
     * Field UPDATE.
     */
    UPDATE(WD.UPDATE),
    /**
     * Field DELETE.
     */
    DELETE(WD.DELETE),
    /**
     * Field CREATE.
     */
    CREATE(WD.CREATE),
    /**
     * Field DROP.
     */
    DROP(WD.DROP),
    /**
     * Field SHOW.
     */
    SHOW(WD.SHOW),
    /**
     * Field DESCRIBE.
     */
    DESCRIBE(WD.DESCRIBE),
    /**
     * Field ALTER.
     */
    ALTER(WD.ALTER),
    /**
     * Field USE.
     */
    USE(WD.USE),
    /**
     * Field RENAME.
     */
    RENAME(WD.RENAME),
    /**
     * Field BEGIN_TRANSACTION.
     */
    BEGIN_TRANSACTION(WD.BEGIN_TRANSACTION),
    /**
     * Field COMMIT.
     */
    COMMIT(WD.COMMIT),
    /**
     * Field ROLLBACK.
     */
    ROLLBACK(WD.ROLLBACK);

    /** The name. */
    private String name;

    /**
     * Instantiates a new SQL operation.
     *
     * @param name
     */
    SQLOperation(String name) {
        this.name = name;
    }

    /** The Constant operationMap. */
    private final static Map<String, SQLOperation> operationMap = new HashMap<String, SQLOperation>();

    static {
        SQLOperation[] values = SQLOperation.values();

        for (int i = 0; i < values.length; i++) {
            operationMap.put(values[i].name, values[i]);
        }
    }

    /**
     * Gets the operation.
     *
     * @param name
     * @return
     */
    public static SQLOperation getOperation(String name) {
        return operationMap.get(name);
    }

    /**
     * Gets the name.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return name;
    }
}
