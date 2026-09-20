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

/**
 * Serialization and deserialization for JSON, XML, Avro, Kryo, and JAXB.
 *
 * <p>{@link ParserFactory} is the entry point. JSON is always available.
 * XML (standard and Abacus), Avro, Kryo, and JAXB are optional: call
 * {@code isXmlParserAvailable()}, {@code isAbacusXmlParserAvailable()},
 * {@code isAvroParserAvailable()}, {@code isKryoParserAvailable()}, or
 * {@code isJaxbParserAvailable()} before creating those parsers. Kryo classes can be
 * registered on the factory for stable IDs and custom serializers.</p>
 *
 * <h2>Contracts</h2>
 * <ul>
 *   <li>{@link Parser} &mdash; thread-safe serialize / deserialize to String, File, stream, and reader/writer.
 *       JSON and the standard/Abacus XML implementations honor {@link com.landawn.abacus.annotation.JsonXmlField} and
 *       {@link com.landawn.abacus.annotation.JsonXmlConfig}; other formats use their own mapping rules.</li>
 *   <li>{@link JsonParser} &mdash; JSON with {@code parse} (String convenience, including unbracketed
 *       array-like input), {@code deserialize} (String/File/stream/reader), {@code stream} for large
 *       arrays, and {@code parseInto} overloads that populate a caller-supplied array, collection, or map.</li>
 *   <li>{@link XmlParser} &mdash; XML including DOM {@link org.w3c.dom.Node} input. JAXB is exposed as
 *       an {@code XmlParser} from {@link ParserFactory#createJaxbParser()}.</li>
 *   <li>{@link AvroParser} / {@link KryoParser} &mdash; binary formats, created only when their
 *       libraries are present.</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>{@link ParserConfig} is the shared base. {@link SerializationConfig} and
 * {@link DeserializationConfig} add ser/deser options; {@link JsonXmlSerConfig} is the JSON/XML
 * serialization branch. Concrete types are {@link JsonSerConfig}, {@link JsonDeserConfig},
 * {@link XmlSerConfig}, {@link XmlDeserConfig}, {@link AvroSerConfig}, {@link AvroDeserConfig},
 * {@link KryoSerConfig}, and {@link KryoDeserConfig}. {@link Exclusion} controls whether
 * {@code null} and default property values are omitted during serialization.</p>
 *
 * <p>{@link ParserUtil} exposes bean and property metadata ({@code BeanInfo}, {@code PropInfo}) used
 * by the parsers; it is marked {@link com.landawn.abacus.annotation.Internal}.
 * {@link XmlConstants} holds shared XML names.</p>
 *
 * @see ParserFactory
 * @see Parser
 * @see JsonParser
 * @see XmlParser
 */
package com.landawn.abacus.parser;
