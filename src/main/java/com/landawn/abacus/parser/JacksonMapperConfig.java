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

package com.landawn.abacus.parser;

import java.util.Locale;
import java.util.function.Function;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.BaseSettings;
import com.fasterxml.jackson.databind.cfg.CoercionConfigs;
import com.fasterxml.jackson.databind.cfg.ConfigOverrides;
import com.fasterxml.jackson.databind.cfg.DatatypeFeatures;
import com.fasterxml.jackson.databind.cfg.DefaultCacheProvider;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.DefaultAccessorNamingStrategy;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.SimpleMixInResolver;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.RootNameLookup;
import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class JacksonMapperConfig extends MapperConfig {
    // copied from Jackson(https://github.com/FasterXML/jackson)

    // 16-May-2009, tatu: Ditto ^^^
    protected static final AnnotationIntrospector DEFAULT_ANNOTATION_INTROSPECTOR = new JacksonAnnotationIntrospector();

    protected static final VisibilityChecker<?> STD_VISIBILITY_CHECKER = VisibilityChecker.Std.defaultInstance();

    /**
     * Base settings contain defaults used for all {@link ObjectMapper} instances.
     */
    protected static final BaseSettings DEFAULT_BASE = new BaseSettings(null, // cannot share global ClassIntrospector any more (2.5+)
            DEFAULT_ANNOTATION_INTROSPECTOR, null, TypeFactory.defaultInstance(), null, StdDateFormat.instance, null, Locale.getDefault(), null, // to indicate "use Jackson default TimeZone" (UTC since Jackson 2.7)
            Base64Variants.getDefaultVariant(),
            // Only for 2.x; 3.x will use more restrictive default
            LaissezFaireSubTypeValidator.instance,
            // Since 2.12:
            new DefaultAccessorNamingStrategy.Provider(),
            // Since 2.16:
            DefaultCacheProvider.defaultInstance());

    private SerializationConfig serializationConfig;

    private DeserializationConfig deserializationConfig;

    JacksonMapperConfig() {
        this(null, null);
    }

    /**
     *
     * @param serializationConfig
     * @param deserializationConfig
     */
    JacksonMapperConfig(final SerializationConfig serializationConfig, final DeserializationConfig deserializationConfig) {
        this.serializationConfig = serializationConfig;
        this.deserializationConfig = deserializationConfig;
    }

    /**
     * Gets the serialization config.
     *
     * @return
     */
    public SerializationConfig getSerializationConfig() {
        initSerializationConfig();

        return serializationConfig;
    }

    /**
     * Gets the deserialization config.
     *
     * @return
     */
    public DeserializationConfig getDeserializationConfig() {
        initDeserializationConfig();

        return deserializationConfig;
    }

    /**
     *
     * @param feature
     * @return
     */
    public JacksonMapperConfig with(SerializationFeature feature) {
        initSerializationConfig();

        serializationConfig = serializationConfig.with(feature);

        return this;
    }

    /**
     *
     * @param features
     * @return
     */
    public JacksonMapperConfig with(SerializationFeature... features) {
        initSerializationConfig();

        serializationConfig = serializationConfig.withFeatures(features);

        return this;
    }

    /**
     *
     * @param feature
     * @return
     */
    public JacksonMapperConfig without(SerializationFeature feature) {
        initSerializationConfig();

        serializationConfig = serializationConfig.without(feature);

        return this;
    }

    /**
     *
     * @param feature
     * @return
     */
    public JacksonMapperConfig without(SerializationFeature... features) {
        initSerializationConfig();

        serializationConfig = serializationConfig.withoutFeatures(features);

        return this;
    }

    /**
     *
     * @param setter
     * @return
     */
    public JacksonMapperConfig setSerializationConfig(final Function<? super SerializationConfig, SerializationConfig> setter) {
        initDeserializationConfig();

        this.serializationConfig = setter.apply(this.serializationConfig);

        return this;
    }

    /**
     *
     * @param feature
     * @return
     */
    public JacksonMapperConfig with(DeserializationFeature feature) {
        initDeserializationConfig();

        deserializationConfig = deserializationConfig.with(feature);

        return this;
    }

    /**
     *
     * @param features
     * @return
     */
    public JacksonMapperConfig with(DeserializationFeature... features) {
        initDeserializationConfig();

        deserializationConfig = deserializationConfig.withFeatures(features);

        return this;
    }

    /**
     *
     * @param feature
     * @return
     */
    public JacksonMapperConfig without(DeserializationFeature feature) {
        initDeserializationConfig();

        deserializationConfig = deserializationConfig.without(feature);

        return this;
    }

    /**
     *
     * @param features
     * @return
     */
    public JacksonMapperConfig without(DeserializationFeature... features) {
        initDeserializationConfig();

        deserializationConfig = deserializationConfig.withoutFeatures(features);

        return this;
    }

    /**
     *
     * @param setter
     * @return
     */
    public JacksonMapperConfig setDeserializationConfig(final Function<? super DeserializationConfig, DeserializationConfig> setter) {
        initDeserializationConfig();

        this.deserializationConfig = setter.apply(this.deserializationConfig);

        return this;
    }

    private void initSerializationConfig() {
        if (serializationConfig == null) {
            final BaseSettings base = DEFAULT_BASE.withClassIntrospector(new BasicClassIntrospector());
            final SubtypeResolver subtypeResolver = new StdSubtypeResolver();
            final SimpleMixInResolver mixins = new SimpleMixInResolver(null);
            final RootNameLookup rootNames = new RootNameLookup();

            serializationConfig = new SerializationConfig(base, subtypeResolver, mixins, rootNames, new ConfigOverrides(), DatatypeFeatures.defaultFeatures());
        }
    }

    private void initDeserializationConfig() {
        if (deserializationConfig == null) {
            final BaseSettings base = DEFAULT_BASE.withClassIntrospector(new BasicClassIntrospector());
            final SubtypeResolver subtypeResolver = new StdSubtypeResolver();
            final SimpleMixInResolver mixins = new SimpleMixInResolver(null);
            final RootNameLookup rootNames = new RootNameLookup();

            deserializationConfig = new DeserializationConfig(base, subtypeResolver, mixins, rootNames, new ConfigOverrides(), new CoercionConfigs(),
                    DatatypeFeatures.defaultFeatures());
        }
    }

    /**
     * The Class JMC.
     */
    public static class JMC extends JacksonMapperConfig {

        /**
         *
         * @return
         */
        public static JacksonMapperConfig create() {
            return new JacksonMapperConfig();
        }

        public static JacksonMapperConfig create(SerializationConfig serializationConfig) {
            return new JacksonMapperConfig(serializationConfig, null);
        }

        public static JacksonMapperConfig create(DeserializationConfig deserializationConfig) {
            return new JacksonMapperConfig(null, deserializationConfig);
        }

        public static JacksonMapperConfig create(SerializationConfig serializationConfig, DeserializationConfig deserializationConfig) {
            return new JacksonMapperConfig(serializationConfig, deserializationConfig);
        }
    }
}
