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

package com.landawn.abacus.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

// TODO: Auto-generated Javadoc
/**
 *
 * @author Haiyang Li
 * @param <R>
 * @param <C>
 * @param <E>
 * @since 0.8
 */
@SuppressWarnings("java:S2160")
public class SheetType<R, C, E> extends AbstractType<Sheet<R, C, E>> {

    private static final String ROW_KEY_SET = "rowKeySet";

    private static final String COLUMN_KEY_SET = "columnKeySet";

    /**
     * @deprecated
     */
    @Deprecated
    private static final String ROW_LIST = "rowList";

    private static final String COLUMN_LIST = "columnList";

    private final String declaringName;

    private final Class<Sheet<R, C, E>> typeClass;

    private final Type<?>[] parameterTypes;

    private final JSONDeserializationConfig jdc;

    /**
     *
     *
     * @param rowKeyTypeName
     * @param columnKeyTypeName
     * @param elementTypeName
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public SheetType(String rowKeyTypeName, String columnKeyTypeName, String elementTypeName) {
        super(getTypeName(Sheet.class, rowKeyTypeName, columnKeyTypeName, elementTypeName, false));

        this.declaringName = getTypeName(Sheet.class, rowKeyTypeName, columnKeyTypeName, elementTypeName, true);

        this.typeClass = (Class) Sheet.class;
        this.parameterTypes = new Type[] { TypeFactory.getType(rowKeyTypeName), TypeFactory.getType(columnKeyTypeName), TypeFactory.getType(elementTypeName) };

        final Type<?> rowKeyListType = TypeFactory.getType("List<" + rowKeyTypeName + ">");
        final Type<?> columnKeyListType = TypeFactory.getType("List<" + columnKeyTypeName + ">");
        final Type<?> rowListType = TypeFactory.getType("List<List<" + elementTypeName + ">>");
        this.jdc = JDC.create().setValueType(ROW_KEY_SET, rowKeyListType).setValueType(COLUMN_KEY_SET, columnKeyListType).setValueType(ROW_LIST, rowListType);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<Sheet<R, C, E>> clazz() {
        return typeClass;
    }

    /**
     * Gets the parameter types.
     *
     * @return
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Checks if is generic type.
     *
     * @return true, if is generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Checks if is serializable.
     *
     * @return true, if is serializable
     */
    @Override
    public boolean isSerializable() {
        return true;
    }

    /**
     *
     * @param x
     * @return {@code null} if {@code (x == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public String stringOf(Sheet<R, C, E> x) {
        if (x == null) {
            return null; // NOSONAR
        }

        final Map<Object, Object> m = new LinkedHashMap<>();
        m.put(ROW_KEY_SET, x.rowKeySet());
        m.put(COLUMN_KEY_SET, x.columnKeySet());

        //    final List<List<?>> rowList = new ArrayList<>(x.rowLength());
        //
        //    for (R rowKey : x.rowKeySet()) {
        //        rowList.add(x.getRow(rowKey));
        //    }
        //
        //    m.put(ROW_LIST, rowList);

        final List<List<E>> columnList = new ArrayList<>(x.rowLength());

        for (C columnKey : x.columnKeySet()) {
            columnList.add(x.getColumn(columnKey));
        }

        m.put(COLUMN_LIST, columnList);

        return Utils.jsonParser.serialize(m, Utils.jsc);
    }

    /**
     *
     * @param str
     * @return {@code null} if {@code (Strings.isEmpty(str))}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public Sheet<R, C, E> valueOf(String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Map<String, Object> m = Utils.jsonParser.deserialize(str, jdc, Map.class);
        final List<R> rowKeySet = (List<R>) m.get(ROW_KEY_SET);
        final List<C> columnKeySet = (List<C>) m.get(COLUMN_KEY_SET);
        final List<List<E>> rowList = (List<List<E>>) m.get(ROW_LIST);
        final List<List<E>> columnList = (List<List<E>>) m.get(COLUMN_LIST);

        Sheet<R, C, E> sheet = null;

        if (typeClass.equals(Sheet.class)) {
            sheet = new Sheet<>(rowKeySet, columnKeySet);
        } else {
            sheet = ClassUtil.invokeConstructor(ClassUtil.getDeclaredConstructor(typeClass, Collection.class, Collection.class), rowKeySet, columnKeySet);
        }

        if (N.notEmpty(rowList)) {
            int i = 0;
            for (R rowKey : rowKeySet) {
                sheet.setRow(rowKey, rowList.get(i++));
            }
        } else if (N.notEmpty(columnList)) {
            int i = 0;
            for (C columnKey : columnKeySet) {
                sheet.setColumn(columnKey, columnList.get(i++));
            }
        }

        return sheet;
    }

    /**
     * Gets the type name.
     *
     * @param typeClass
     * @param rowKeyTypeName
     * @param columnKeyTypeName
     * @param elementTypeName
     * @param isDeclaringName
     * @return
     */
    protected static String getTypeName(Class<?> typeClass, String rowKeyTypeName, String columnKeyTypeName, String elementTypeName, boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(rowKeyTypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(columnKeyTypeName).declaringName() + WD.COMMA_SPACE + TypeFactory.getType(elementTypeName).declaringName()
                    + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(rowKeyTypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(columnKeyTypeName).name() + WD.COMMA_SPACE + TypeFactory.getType(elementTypeName).name() + WD.GREATER_THAN;

        }
    }
}
