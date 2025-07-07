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

import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.Strings;

@SuppressWarnings("java:S2160")
public class MapEntityType extends AbstractType<MapEntity> {

    public static final String MAP_ENTITY = MapEntity.class.getSimpleName();

    private final Class<MapEntity> typeClass;

    MapEntityType() {
        super(MAP_ENTITY);

        typeClass = MapEntity.class;
    }

    /**
     * Returns the Class object representing the MapEntity type.
     *
     * @return The Class object for MapEntity
     */
    @Override
    public Class<MapEntity> clazz() {
        return typeClass;
    }

    /**
     * Indicates whether this type represents a MapEntity.
     * For MapEntityType, this always returns true.
     *
     * @return true, indicating that this type represents a MapEntity
     */
    @Override
    public boolean isMapEntity() {
        return true;
    }

    /**
     * Indicates whether instances of this type can be serialized.
     * MapEntity objects are not directly serializable through this type handler.
     *
     * @return false, indicating that MapEntity is not serializable through this type
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Gets the serialization type category for MapEntity.
     * This indicates how the MapEntity should be treated during serialization processes.
     *
     * @return SerializationType.MAP_ENTITY
     */
    @Override
    public SerializationType getSerializationType() {
        return SerializationType.MAP_ENTITY;
    }

    /**
     * Converts a MapEntity object to its JSON string representation.
     * The MapEntity is serialized using the configured JSON parser with default settings.
     *
     * @param x The MapEntity object to convert
     * @return The JSON string representation of the MapEntity, or null if the input is null
     */
    @Override
    public String stringOf(final MapEntity x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Parses a JSON string to create a MapEntity object.
     * The string should be a valid JSON object representation that can be deserialized into a MapEntity.
     *
     * @param str The JSON string to parse
     * @return The parsed MapEntity object, or null if the input is null or empty
     */
    @Override
    public MapEntity valueOf(final String str) {
        return Strings.isEmpty(str) ? null : Utils.jsonParser.deserialize(str, Utils.jdc, MapEntity.class);
    }
}