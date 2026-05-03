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
 * Type handler for {@link EntityId} values.
 * {@link EntityId} is a composite identifier used to uniquely identify entities in the framework.
 *
 * <p>EntityId instances are serialized to and from JSON format. The serialization preserves all
 * component key-value pairs that make up the composite identifier.
 *
 * <p>Note: {@link #isSerializable()} returns {@code false}; the type system routes EntityId
 * serialization through {@link SerializationType#ENTITY_ID} rather than the generic serializable path.
 *
 * @see AbstractType
 * @see EntityId
 */
@SuppressWarnings("java:S2160")
public class EntityIdType extends AbstractType<EntityId> {

    /**
     * The type name constant for EntityId type identification, equal to {@code "EntityId"}.
     */
    public static final String ENTITY_ID = EntityId.class.getSimpleName();

    private final Class<EntityId> typeClass;

    /**
     * Package-private constructor for {@code EntityIdType}.
     * Instances are created by the {@code TypeFactory}.
     */
    EntityIdType() {
        super(ENTITY_ID);

        typeClass = EntityId.class;
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code EntityId.class}
     */
    @Override
    public Class<EntityId> javaType() {
        return typeClass;
    }

    /**
     * Indicates whether this type represents an {@link EntityId}.
     *
     * @return {@code true}, always, because this handler is dedicated to {@link EntityId} objects
     */
    @Override
    public boolean isEntityId() {
        return true;
    }

    /**
     * Indicates whether this type is handled by the generic serializable path.
     * {@link EntityId} uses a dedicated serialization path ({@link SerializationType#ENTITY_ID}).
     *
     * @return {@code false}, always
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Returns the serialization type category for {@link EntityId} objects.
     *
     * @return {@link SerializationType#ENTITY_ID}
     */
    @Override
    public SerializationType serializationType() {
        return SerializationType.ENTITY_ID;
    }

    /**
     * Serializes an {@link EntityId} to its JSON string representation, preserving all
     * component key-value pairs.
     *
     * @param x the {@link EntityId} to serialize; may be {@code null}
     * @return the JSON string, or {@code null} if {@code x} is {@code null}
     */
    @Override
    public String stringOf(final EntityId x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Deserializes a JSON string back into an {@link EntityId} object.
     * The JSON must represent a valid {@link EntityId} with all required component fields.
     *
     * @param str the JSON string to parse; may be {@code null} or empty
     * @return the deserialized {@link EntityId}, or {@code null} if {@code str} is {@code null} or empty
     */
    @Override
    public EntityId valueOf(final String str) {
        return (Strings.isEmpty(str)) ? null : Utils.jsonParser.deserialize(str, typeClass);
    }
}
