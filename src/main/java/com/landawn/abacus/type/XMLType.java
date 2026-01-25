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

import java.util.List;
import java.util.Map;

import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type implementation for XML serialization and deserialization of objects.
 * <p>
 * This type allows objects to be converted to and from XML format. It supports
 * various target classes including Map, List, and custom classes.
 * </p>
 *
 * @param <T> the type of objects handled by this XMLType
 */
@SuppressWarnings("java:S2160")
public class XMLType<T> extends AbstractType<T> {

    public static final String XML = "XML";

    private final String declaringName;

    private final Class<T> typeClass;
    //    private final Type<T>[] parameterTypes;
    //    private final Type<T> elementType;

    /**
     * Constructs an XMLType instance for the specified class.
     * This constructor is package-private and should only be called by TypeFactory.
     *
     * @param clsName the name of the class to be handled by this XML type
     */
    @SuppressWarnings("unchecked")
    XMLType(final String clsName) {
        super(XML + WD.LESS_THAN + TypeFactory.getType(clsName).name() + WD.GREATER_THAN);

        declaringName = XML + WD.LESS_THAN + TypeFactory.getType(clsName).declaringName() + WD.GREATER_THAN;
        typeClass = (Class<T>) ("Map".equalsIgnoreCase(clsName) ? Map.class : ("List".equalsIgnoreCase(clsName) ? List.class : ClassUtil.forName(clsName)));
        //        this.parameterTypes = new Type[] { TypeFactory.getType(clsName) };
        //        this.elementType = parameterTypes[0];
    }

    /**
     * Returns the declaring name of this XML type.
     * <p>
     * The declaring name includes the "XML" prefix and the type parameter in angle brackets,
     * for example: "XML&lt;Map&gt;" or "XML&lt;User&gt;".
     * </p>
     *
     * @return the declaring name of this XML type
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the target class for XML conversion.
     * <p>
     * This is the class of objects that this XMLType can serialize to and deserialize from XML.
     * </p>
     *
     * @return the Class object for the target type
     */
    @Override
    public Class<T> clazz() {
        return typeClass;
    }

    /**
     * Converts an object to its XML string representation.
     * <p>
     * This method uses the XML parser to serialize the provided object to an XML string.
     * If the input object is {@code null}, this method returns {@code null}.
     * </p>
     *
     * @param x the object to convert to XML
     * @return the XML string representation of the object, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final T x) {
        return (x == null) ? null : Utils.xmlParser.serialize(x);
    }

    /**
     * Converts an XML string to an object of the target type.
     * <p>
     * This method uses the XML parser to deserialize the provided XML string into an
     * object of the target class. If the string is {@code null} or empty, this method returns {@code null}.
     * </p>
     *
     * @param str the XML string to deserialize
     * @return an object of type T deserialized from the XML string, or {@code null} if the string is empty
     * @throws RuntimeException if the XML parsing fails or the XML doesn't match the target type
     */
    @Override
    public T valueOf(final String str) {
        return Strings.isEmpty(str) ? null : Utils.xmlParser.deserialize(str, typeClass);
    }
}
