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

package com.landawn.abacus.parser;

/**
 * Package-private enumeration of XML parser strategies supported by the parser implementations
 * in this package. Each constant represents a different underlying XML parsing technology with
 * distinct memory and performance characteristics; the value is supplied to a parser
 * implementation's constructor (see {@link AbacusXmlParserImpl} and {@link XmlParserImpl}) to
 * select the strategy used by that instance.
 *
 * <p>Available parser types:</p>
 * <ul>
 *   <li>{@link #SAX} &mdash; Simple API for XML (event-driven, memory efficient)</li>
 *   <li>{@link #DOM} &mdash; Document Object Model (tree-based, loads the entire document)</li>
 *   <li>{@link #StAX} &mdash; Streaming API for XML (pull-based, balances performance and ease of use)</li>
 * </ul>
 *
 * <p>Parser characteristics:</p>
 * <ul>
 *   <li><b>SAX</b>: Event-driven, forward-only, memory efficient for large documents,
 *       but requires a more complex programming model.</li>
 *   <li><b>DOM</b>: Loads the entire document into memory as a tree structure,
 *       allowing random access and modification, but is memory intensive.</li>
 *   <li><b>StAX</b>: Pull-based streaming, the application controls parsing flow,
 *       providing a good balance between performance and ease of use.</li>
 * </ul>
 *
 * <p>Note: not every implementation supports every parser type. {@link XmlParserImpl}
 * supports {@link #StAX} and {@link #DOM}; {@link AbacusXmlParserImpl} additionally supports
 * {@link #SAX}.</p>
 *
 * @see AbacusXmlParserImpl
 * @see XmlParserImpl
 */
enum XmlParserType {

    /**
     * SAX (Simple API for XML) parser type.
     * Event-driven, push-based XML parser that processes documents sequentially.
     * Most memory efficient but requires handling events as they occur.
     */
    SAX,

    /**
     * DOM (Document Object Model) parser type.
     * Tree-based parser that loads the entire XML document into memory.
     * Provides random access to elements but requires more memory.
     */
    DOM,

    /**
     * StAX (Streaming API for XML) parser type.
     * Pull-based streaming parser that gives control to the application.
     * Balances performance and ease of use, recommended for most applications.
     */
    StAX
}
