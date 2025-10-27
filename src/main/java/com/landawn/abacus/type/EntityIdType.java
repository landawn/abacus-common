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

package com.landawn.abacus.type;

import com.landawn.abacus.util.EntityId;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for EntityId values.
 * This class provides serialization and deserialization for EntityId objects,
 * which are composite identifiers used to uniquely identify entities.
 * EntityIds are serialized to and from JSON format.
 */
@SuppressWarnings("java:S2160")
public class EntityIdType extends AbstractType<EntityId> {

    public static final String ENTITY_ID = EntityId.class.getSimpleName();

    private final Class<EntityId> typeClass;

    EntityIdType() {
        super(ENTITY_ID);

        typeClass = EntityId.class;
    }

    /**
     * Returns the Java class type handled by this type handler.
     *
     * @return The Class object representing EntityId.class
     */
    @Override
    public Class<EntityId> clazz() {
        return typeClass;
    }

    /**
     * Indicates whether this type represents an EntityId.
     * Always returns {@code true} for EntityIdType.
     *
     * @return {@code true}, as this type handler specifically handles EntityId objects
     */
    @Override
    public boolean isEntityId() {
        return true;
    }

    /**
     * Indicates whether this EntityId type is serializable in the type system.
     * EntityIds require special JSON serialization handling.
     *
     * @return {@code false}, indicating EntityIds are not simply serializable
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Returns the serialization type category for EntityId objects.
     *
     * @return SerializationType.ENTITY_ID, indicating special EntityId serialization handling
     */
    @Override
    public SerializationType getSerializationType() {
        return SerializationType.ENTITY_ID;
    }

    /**
     * Converts an EntityId to its JSON string representation.
     * The EntityId is serialized with all its component fields.
     *
     * @param x the EntityId to convert. Can be {@code null}.
     * @return A JSON string representation of the EntityId, or {@code null} if input is null
     @MayReturnNull
     */
    @Override
    public String stringOf(final EntityId x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Converts a JSON string representation back to an EntityId object.
     * The string should contain a valid JSON representation of an EntityId
     * with all required fields.
     *
     * @param str the JSON string to parse. Can be {@code null} or empty.
     * @return An EntityId parsed from the JSON string, or {@code null} if input is null/empty
     @MayReturnNull
     */
    @Override
    public EntityId valueOf(final String str) {
        return (Strings.isEmpty(str)) ? null : Utils.jsonParser.deserialize(str, typeClass);
    }
}
