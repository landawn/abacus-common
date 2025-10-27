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
 * Enumeration of XML parser types supported by the framework.
 * Each parser type represents a different approach to XML processing with distinct characteristics.
 * 
 * <p>Available parser types:
 * <ul>
 *   <li>{@link #SAX} - Simple API for XML (event-driven, memory efficient)</li>
 *   <li>{@link #DOM} - Document Object Model (tree-based, loads entire document)</li>
 *   <li>{@link #StAX} - Streaming API for XML (pull-based, balances performance and ease of use)</li>
 * </ul>
 * 
 * <p>Parser characteristics:
 * <ul>
 *   <li><b>SAX</b>: Event-driven, forward-only, memory efficient for large documents,
 *       but requires more complex programming model</li>
 *   <li><b>DOM</b>: Loads entire document into memory as a tree structure,
 *       allows random access and modification, but memory intensive</li>
 *   <li><b>StAX</b>: Pull-based streaming, developer controls parsing flow,
 *       good balance between performance and ease of use</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create parser with specific type
 * XMLParser staxParser = new XMLParserImpl(XMLParserType.StAX);
 * XMLParser domParser = new XMLParserImpl(XMLParserType.DOM);
 * 
 * // Choose parser based on requirements
 * XMLParserType parserType = largeFile ? XMLParserType.StAX : XMLParserType.DOM;
 * XMLParser parser = new XMLParserImpl(parserType);
 * }</pre>
 * 
 * @see XMLParserImpl
 * @since 1.0
 */
enum XMLParserType {

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
