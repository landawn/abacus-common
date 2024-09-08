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

import static com.landawn.abacus.util.WD.COMMA;
import static com.landawn.abacus.util.WD._PARENTHESES_L;
import static com.landawn.abacus.util.WD._PARENTHESES_R;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class TypeAttrParser {

    private final String className;

    private final String[] typeParameters;

    private final String[] parameters;

    private TypeAttrParser(final String className, final String[] typeParameters, final String[] parameters) {
        this.className = className;

        if (typeParameters == null) {
            this.typeParameters = new String[0];
        } else {
            this.typeParameters = typeParameters;
        }

        if (parameters == null) {
            this.parameters = new String[0];
        } else {
            this.parameters = parameters;
        }
    }

    /**
     * Gets the class name.
     *
     * @return
     */
    public String getClassName() {
        return className;
    }

    /**
     * Gets the type parameters.
     *
     * @return
     */
    public String[] getTypeParameters() {
        return typeParameters;
    }

    /**
     * Gets the parameters.
     *
     * @return
     */
    public String[] getParameters() {
        return parameters;
    }

    /**
     *
     * @param attr
     * @return
     */
    public static TypeAttrParser parse(final String attr) {
        String className = null;
        String[] typeParameters = null;
        String[] parameters = null;

        int beginIndex = attr.indexOf('<');
        if (beginIndex >= 0) {
            final int endIndex = attr.lastIndexOf('>');

            className = attr.substring(0, beginIndex).trim();
            final List<String> typeParameterList = new ArrayList<>();

            int bracketNum = 0;

            for (int idx = beginIndex + 1, previousIndex = idx; idx < endIndex; idx++) {
                final char ch = attr.charAt(idx);

                if (ch == '<') {
                    bracketNum++;

                    continue;
                }

                if ((bracketNum > 0) && (ch == '>')) {
                    bracketNum--;
                }

                if (bracketNum == 0 && (ch == ',' || idx == endIndex - 1)) {
                    typeParameterList.add(Strings.trim(ch == ',' ? attr.substring(previousIndex, idx) : attr.substring(previousIndex, idx + 1)));

                    previousIndex = idx + 1;
                }
            }

            typeParameters = typeParameterList.toArray(new String[typeParameterList.size()]);

            beginIndex = endIndex;
        }

        beginIndex = attr.indexOf(_PARENTHESES_L, N.max(0, beginIndex));

        if (beginIndex >= 0) {
            if (className == null) {
                className = attr.substring(0, beginIndex);
            }

            final String str = attr.substring(beginIndex + 1, attr.lastIndexOf(_PARENTHESES_R));
            parameters = COMMA.equals(str.trim()) ? new String[] { COMMA } : Splitter.with(WD.COMMA).trimResults().splitToArray(str);
        }

        if (className == null) {
            className = attr;
        }

        return new TypeAttrParser(className, typeParameters, parameters);
    }

    /**
     *
     * @param <T>
     * @param cls
     * @param attr
     * @param args
     * @return
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T> T newInstance(Class<?> cls, final String attr, final Object... args) {
        final TypeAttrParser attrResult = TypeAttrParser.parse(attr);
        final String className = attrResult.getClassName();
        final String[] attrTypeParameters = attrResult.getTypeParameters();
        final String[] attrParameters = attrResult.getParameters();

        if (cls == null) {
            cls = ClassUtil.forClass(className);
        }

        int parameterLength = attrTypeParameters.length + attrParameters.length + (args.length / 2);

        if (parameterLength > 0) {
            Class<?>[] parameterTypes = new Class[parameterLength];
            Object[] paramters = new Object[parameterLength];

            for (int i = 0; i < args.length; i++) {
                parameterTypes[i / 2] = (Class<?>) args[i];
                paramters[i / 2] = args[++i];
            }

            for (int i = 0; i < attrTypeParameters.length; i++) {
                parameterTypes[i + (args.length / 2)] = String.class;
                paramters[i + (args.length / 2)] = attrTypeParameters[i];
            }

            for (int i = 0; i < attrParameters.length; i++) {
                parameterTypes[i + (args.length / 2) + attrTypeParameters.length] = String.class;
                paramters[i + (args.length / 2) + attrTypeParameters.length] = attrParameters[i];
            }

            Constructor<?> constructor = ClassUtil.getDeclaredConstructor(cls, parameterTypes);

            if (constructor == null) {
                parameterLength = attrTypeParameters.length + ((attrParameters.length > 1) ? 1 : 0) + (args.length / 2);

                if (parameterLength > 0) {
                    parameterTypes = new Class[parameterLength];
                    paramters = new Object[parameterLength];

                    for (int i = 0; i < args.length; i++) {
                        parameterTypes[i / 2] = (Class<?>) args[i];
                        paramters[i / 2] = args[++i];
                    }

                    for (int i = 0; i < attrTypeParameters.length; i++) {
                        parameterTypes[i + (args.length / 2)] = String.class;
                        paramters[i + (args.length / 2)] = attrTypeParameters[i];
                    }

                    if (attrParameters.length > 1) {
                        parameterTypes[parameterTypes.length - 1] = String[].class;
                        paramters[paramters.length - 1] = attrParameters;
                    }
                }

                constructor = ClassUtil.getDeclaredConstructor(cls, parameterTypes);
            }

            if (constructor == null) {
                throw new IllegalArgumentException(
                        "No constructor found with parameters: " + N.toString(parameterTypes) + ". in class: " + cls.getCanonicalName());
            }

            ClassUtil.setAccessibleQuietly(constructor, true);

            return (T) ClassUtil.invokeConstructor(constructor, paramters);
        } else {
            return (T) N.newInstance(cls);
        }
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return "{className=" + className + ", typeParameters=" + Arrays.toString(typeParameters) + ", parameters=" + Arrays.toString(parameters) + "}";
    }
}
