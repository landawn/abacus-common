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
 * <h2>XML parser: known limitations and fixed issues</h2>
 * <p><i>XML parser: known limitations, and the issues fixed in the review rounds of 2026-10-05 to 2026-10-07 (r47 to r57, r62 and r63)</i>.
 * The same content is kept as plain text in {@code docs/xml_parser_limitation.txt} of the project.</p>
 * <h3>Scope and notation</h3>
 * <p>Scope: the deserialization behavior of the two XML parsers, XmlParserImpl (standard XML, StAX and DOM backends) and AbacusXmlParserImpl
 * (Abacus XML, StAX, DOM and SAX backends), for compact JSON text, type metadata and XmlDeserConfig. Every issue below was reproduced on the
 * five backends, fixed, and pinned by a test in CodexXmlParserTest (I44 .. I55: in AbacusXmlParserImplTest and XmlParserImplTest; the method
 * names are listed per issue). The "before" and "after" columns are the outputs of the sample documents on the main classes of the revision
 * before the fix and of the revision with the fix, obtained by running one probe against each revision r46 .. r57 (I44 .. I48: against r61 and
 * r62; I49 .. I55: against r62 and r63). Part 1 lists the known limitations and the remaining issues (L01 .. L16; L12 was fixed and moved to
 * part 2 as I50) with their current behavior; part 2 is the history of the fixed issues (I01 .. I55). I33 .. I40 are the third review round
 * (r56), I41 .. I43 the fourth (r57). I44 .. I48 are the backend-agreement fixes that followed the performance review of 2026-10-07 (its
 * ledger: scripts/cross_review/Parser_perf_memory_review_ledger_2026-10-07.md, rounds 2 to 4), committed as r62: their "before" column is r61
 * and their "after" column r62. I49 .. I55 are the fixes of the recheck of those rounds (the "recheck R5" tests of AbacusXmlParserImplTest),
 * committed as r63: their "before" column is r62 and their "after" column r63.</p>
 * <p>Revisions: r47 = 2026-10-05 21:20, r48 = 22:30, r49 = 23:17, r50 = 2026-10-06 00:04, r51 = 00:36, r52 = 04:56, r53 = 07:36, r54 = 19:08
 * (this file), r55 = 19:47 (slot refinement: refineSlot, narrowedArgument), r56 = 20:46 (the third round), r57 = 21:39 (the fourth round), r58
 * = 2026-10-07 02:19 and r59 = 05:52 (performance fixes), r61 = 20:44 (the performance review), r62 = 23:09 (the backend-agreement fixes I44
 * .. I48), r63 = 2026-10-08 00:10 (the recheck fixes I49 .. I55).</p>
 * <p>Notation: "XML StAX/DOM" are the XmlParserImpl backends, "Abacus StAX/DOM/SAX" the AbacusXmlParserImpl backends. Results are shown as
 * class&#123;content&#125;: LinkedHashMap&#123;a="1"&#125; is a LinkedHashMap holding the String "1" under "a", &#123;a=[1]&#125; a map
 * holding a List of the Integer 1, Long[]&#123;1L, 2L&#125; a Long[] of 1 and 2, Object[]&#123;"1", "2"&#125; an Object[] of two Strings. "all
 * five" = the same result on every backend. Type attributes are shown unescaped and with simple names (LinkedMultiValueMap stands for
 * org.springframework.util.LinkedMultiValueMap, written in full and XML-escaped in a real document). The standard-XML form of a map value is
 * &lt;map&gt;&lt;k ...&gt;body&lt;/k&gt;&lt;/map&gt;; the Abacus form of the same document is
 * &lt;map&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value ...&gt;body&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;.</p>
 * <h3>Shared rules the fixes established</h3>
 * <p>Shared rules the fixes established (see the XmlDeserConfig class Javadoc for the complete contract):</p>
 * <ul>
 *   <li>Compact JSON text accepts surrounding Unicode whitespace and JSON string escapes, at the root and inside properties, on every
 *   backend.</li>
 *   <li>Map implementation metadata never breaks the declared value shape: a list-valued map (Spring MultiValueMap) that cannot hold the
 *   declared, configured or overridden values falls back to an ordinary LinkedHashMap; a declared reference array keeps its class, metadata
 *   types the items.</li>
 *   <li>Per-key value types apply literally to any key by its text, inside compact text at any nesting level and to child elements alike; a
 *   scalar override on a declared list or array value names its element type.</li>
 *   <li>In a declared map slot: a declared key or value type &gt; the global map key/value types &gt; the metadata's arguments. At the ROOT an
 *   explicit global map key or value type overrides the requested type's own arguments (tested contract).</li>
 *   <li>Element configuration types the open components of a slot at every nesting level; metadata may supply the container implementation but
 *   may not narrow a declared open Object[] while configuration is present; a primitive component always keeps its codec.</li>
 *   <li>Without type information, the children of an open Object slot are read as the writer named them; a bean-shaped element in an open slot
 *   is a map of its properties on every backend. With type information, a typed child is the value, read with its type.</li>
 *   <li>A configured container component (List&lt;Object&gt;, Object[]) types a slot's open elements once; a declared container element
 *   (Object[] in Object[][] or List&lt;Object[]&gt;) is read as that element with the configuration applied inside it, on every backend.</li>
 *   <li>The global map key and value types fill the open arguments of the maps nested at any depth of compact text, as of child elements; a
 *   map or list value under a scalar global value type is rejected as the JSON reader rejects it (L10).</li>
 *   <li>With type information, an approved scalar type attribute on an Object bean property types the property's text on every backend (I44);
 *   an attribute naming a class whose values cannot be read from text is a ParsingException naming the attribute (I48), also when it names the
 *   property's declared class (I49); the value errors of a working codec keep the codec's own exception, as in JSON.</li>
 *   <li>A value type configured for a bean property types its text on every backend and wins over a scalar attribute; an Object configuration
 *   keeps scalar text and adopts container metadata; a formatted property reads through its format (I50). An element without content is the
 *   empty text, read with the same rule (I55).</li>
 *   <li>On the Abacus readers a nested map key or an explicit-Object-override value carrying an approved bean class is read as that bean, an
 *   explicit Object override keeps scalar text (I45, I46), also under bean metadata (I51), container metadata on such a value or on a nested
 *   key is the container its nested element must fit (I52), a String key or value slot reads a nested element with its container metadata as
 *   an open slot does (I53), and a bean-shaped element nested in another reads as nested maps (I47).</li>
 * </ul>
 * <h3>Index of known limitations and remaining issues (current behavior, not changed)</h3>
 * <ul>
 *   <li>L01  An Optional&lt;Object&gt; bean property is written as compact JSON text even with type information  [not changed (writer
 *   limitation)]</li>
 *   <li>L02  Optional wrappers and uninstantiable immutable collections in open map values do not survive a round trip  [not changed (by
 *   design)]</li>
 *   <li>L03  Compact text of a List&lt;Object&gt; with mixed scalars loses Long and Character in standard XML  [not changed (format
 *   limitation)]</li>
 *   <li>L04  A Date whose epoch text has four digits or fewer cannot be read back  [not changed (codec rule)]</li>
 *   <li>L05  Bean-property map slots: the ordinary-fallback map class and the metadata's value argument differ between backends  [remaining
 *   difference]</li>
 *   <li>L06  A text-only element of an unknown name inside an open slot  [remaining difference (by design)]</li>
 *   <li>L07  The root contract: an explicit global map key or value type overrides the requested type's own arguments at the root
 *   [intentional (tested contract)]</li>
 *   <li>L08  Compact JSON text keeps JSON typing while element text is String  [intentional]</li>
 *   <li>L09  Element configuration never types map values  [intentional]</li>
 *   <li>L10  A map or list value under a global scalar map value type is rejected in compact text  [intentional (JSON rule)]</li>
 *   <li>L11  Without type information, the standard StAX reader adopts a scalar type attribute on an Object bean property; the other four keep
 *   the text  [remaining difference]</li>
 *   <li>L12  A configured scalar value type for a bean property is honored by three of the five backends  [fixed in r63: moved to part 2 as
 *   I50]</li>
 *   <li>L13  Type attributes on the properties of a bean-shaped element read as a map differ per Abacus backend  [remaining difference]</li>
 *   <li>L14  An explicit Object override with a scalar type attribute: the Abacus readers keep the text, the standard readers adopt the
 *   attribute  [remaining difference]</li>
 *   <li>L15  Without type information, an Optional type attribute on an Object bean property is adopted by the StAX and SAX readers and kept
 *   as text by the DOM readers  [remaining difference]</li>
 *   <li>L16  The standard StAX reader reads a bean property element without content with its own fallback  [remaining difference]</li>
 * </ul>
 * <h3>Index of fixed issues (history)</h3>
 * <ul>
 *   <li>I01  Unicode padding around compact JSON text of reference arrays and collections  [fixed in r47]</li>
 *   <li>I02  List-valued map metadata (Spring MultiValueMap) on a map whose declared or configured values are scalars  [fixed in r47]</li>
 *   <li>I03  A named String override inside a list-valued map, in compact text and in child elements, at any nesting level  [fixed in r47 (r48
 *   for the fallback class of child elements)]</li>
 *   <li>I04  An explicit Object override is applied literally: open scalar text, never a list  [fixed in r47]</li>
 *   <li>I05  Nested open array components honor element configuration in compact text  [fixed in r47]</li>
 *   <li>I06  A map written as compact JSON text inside a list element  [fixed in r47]</li>
 *   <li>I07  Primitive-list text (IntList, CharList, ...) reads quoted numbers and null defaults through the array codecs  [fixed in r47]</li>
 *   <li>I08  A primitive array adopted from metadata ignores element configuration  [fixed in r48]</li>
 *   <li>I09  A scalar per-key override on a declared list value names its element type  [fixed in r48]</li>
 *   <li>I10  A nested &lt;array&gt; item of a list value with the array metadata on the list, under String element configuration  [fixed in
 *   r47 (StAX), r48 (DOM)]</li>
 *   <li>I11  A standard-XML map value needs its &lt;map&gt; wrapper  [fixed in r48]</li>
 *   <li>I12  List and array metadata naming a specialized map item applies to child elements  [fixed in r48]</li>
 *   <li>I13  Nested open array items keep their declared open form under element configuration (child elements)  [fixed in r47 (matrix), r48
 *   (list on DOM)]</li>
 *   <li>I14  Untyped children of an open Object slot are read as the writer named them (standard XML)  [fixed in r49]</li>
 *   <li>I15  The standard writer's untyped output for nested containers in open slots reads back  [fixed in r49]</li>
 *   <li>I16  Abacus XML: metadata on the &lt;list&gt; or &lt;array&gt; element alone types the items  [fixed in r49 (array on r47/r48
 *   partially)]</li>
 *   <li>I17  With type information, typed items of an open list keep their types (standard XML)  [fixed in r50]</li>
 *   <li>I18  Without type information, a bean in an open Object slot is read as a map of its properties (Abacus XML)  [fixed in r50 (standard
 *   DOM in r49)]</li>
 *   <li>I19  With type information, the typed wrapper child of an Object property is the value  [fixed in r51 (map child in r49)]</li>
 *   <li>I20  An empty element is the empty string; a null marker is null  [fixed in r51]</li>
 *   <li>I21  Without type information, bean-shaped elements inside arrays and nested values are read as maps (Abacus XML)  [fixed in r50
 *   (nested value), r51 (arrays)]</li>
 *   <li>I22  Per-key value types reach the compact text of a root array or list  [fixed in r52]</li>
 *   <li>I23  An explicit Object override on a value that carries map metadata adopts the metadata  [fixed in r52]</li>
 *   <li>I24  Per-key value types reach the compact text of an array value with an open component  [fixed in r52]</li>
 *   <li>I25  The global map value type fills the open values of maps inside compact text at any nesting level  [fixed in r52]</li>
 *   <li>I26  A compact map entry keeps its declared scalar type when it meets a JSON array (as JSON does)  [fixed in r53]</li>
 *   <li>I27  A root array keeps the requested array class under metadata; a per-key override falls back to an ordinary map  [fixed in
 *   r53]</li>
 *   <li>I28  A present key whose override is a scalar type moves a list-valued map to ordinary construction even for a null value  [fixed in
 *   r53]</li>
 *   <li>I29  In a declared map slot: declared argument &gt; global map key/value type &gt; metadata argument  [fixed in r53]</li>
 *   <li>I30  A root map written as compact JSON text is read by every backend, with the root's key and value types  [fixed in r53]</li>
 *   <li>I31  The configured component types the innermost open elements of nested containers at parse time  [fixed in r53]</li>
 *   <li>I32  A blank root array element is an empty array of the requested class  [fixed in r53]</li>
 *   <li>I33  A configured container component types the slot's open elements once, never its own elements  [fixed in r56]</li>
 *   <li>I34  A scalar per-key override on a declared array or Set value keeps the declared container shape in compact text  [fixed in
 *   r56]</li>
 *   <li>I35  A per-key array type names a map entry's array on the Abacus DOM and SAX readers  [fixed in r56]</li>
 *   <li>I36  A root array or list whose requested element is a container keeps it, and the configured component reaches its open elements
 *   [fixed in r56]</li>
 *   <li>I37  At the root, a global map key or value type never replaces a requested container argument  [fixed in r56]</li>
 *   <li>I38  A present null under any non-List override moves a list-valued map to ordinary construction  [fixed in r56]</li>
 *   <li>I39  A global scalar map value type on a root map whose value holds a structured child  [fixed in r56, by I37]</li>
 *   <li>I40  A declared Number[] value whose metadata names Long[] holds Long elements on every backend  [fixed in r56, found by the
 *   differential]</li>
 *   <li>I41  A declared container element keeps the element configuration inside it, on every backend  [fixed in r57]</li>
 *   <li>I42  The standard readers read the structured items of a nested array slot with the slot's declared element  [fixed in r57]</li>
 *   <li>I43  The global map value type reaches the maps nested in compact text  [fixed in r57]</li>
 *   <li>I44  With type information, a scalar type attribute on an Object bean property is adopted by the Abacus StAX and SAX readers too
 *   [fixed in r62]</li>
 *   <li>I45  A nested map key whose type attribute names a bean class is read as that bean on the Abacus DOM reader too  [fixed in r62]</li>
 *   <li>I46  An explicit Object override keeps scalar text on the Abacus SAX reader, and a nested bean under it is read on the Abacus DOM
 *   reader  [fixed in r62]</li>
 *   <li>I47  A bean-shaped element nested in a bean-shaped element is read on the Abacus SAX reader  [fixed in r62]</li>
 *   <li>I48  An adopted type attribute naming a class whose values cannot be read from text is a ParsingException  [fixed in r62]</li>
 *   <li>I49  A Number bean property whose type attribute names its own declared class is a ParsingException on the Abacus StAX and SAX readers
 *   too  [fixed in r63]</li>
 *   <li>I50  A value type configured for a bean property types its text on the Abacus StAX and SAX readers too (formerly L12)  [fixed in
 *   r63]</li>
 *   <li>I51  An explicit Object override keeps the text of a value whose type attribute names a bean class on the Abacus SAX reader  [fixed in
 *   r63]</li>
 *   <li>I52  Container metadata on an explicit-Object-override value or a nested map key types the nested element on every Abacus reader
 *   [fixed in r63]</li>
 *   <li>I53  A nested element in a String map key or value slot is read with its container metadata on the Abacus StAX and DOM readers too
 *   [fixed in r63]</li>
 *   <li>I54  A nested Object[] item of an Object[] property keeps its class under String element configuration on the Abacus StAX and DOM
 *   readers  [fixed in r63]</li>
 *   <li>I55  A bean property element without content is read as empty text on the Abacus StAX reader  [fixed in r63]</li>
 * </ul>
 * <h3>Part 1: Known limitations and remaining issues</h3>
 * <p>Current behavior on the five backends, obtained with the same probe harness as part 2. "Not changed" entries are writer or format
 * limitations and design rules that were reported and deliberately left as they are; "remaining difference" entries are backend differences
 * that are still open; "intentional" entries are documented rules that reviewers keep flagging and that differ from the JSON reader. L12 was
 * fixed in r63 and moved to part 2 as I50; its number is not reused.</p>
 *
 * <h4>L01  An Optional&lt;Object&gt; bean property is written as compact JSON text even with type information</h4>
 * <p><b>Status:</b> not changed (writer limitation)</p>
 * <p><b>Problem.</b> Both writers put the value of a bean property declared java.util.Optional&lt;Object&gt;, u.Optional&lt;Object&gt; or
 * u.Nullable&lt;Object&gt; as compact JSON text without a type attribute, so the readers hand back the wrapper around a String. An empty
 * wrapper is not written at all and reads back as null. A plain Object property gets a typed wrapper child and round-trips (I19); map values
 * wrap Optionals properly.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * ObjectBean bean = new ObjectBean(); bean.jdk = Optional.of(person); bean.optional = u.Optional.of(person); bean.nullable = u.Nullable.of(person);
 * String xml = parser.serialize(bean, XmlSerConfig.create().setWriteTypeInfo(true));   // same on both writers:
 * &lt;objectBean type="...ObjectBean"&gt;
 *     &lt;jdk&gt;&#123;&amp;quot;name&amp;quot;: &amp;quot;n&amp;quot;, &amp;quot;age&amp;quot;: 3&#125;&lt;/jdk&gt;
 *     &lt;optional&gt;&#123;&amp;quot;name&amp;quot;: &amp;quot;n&amp;quot;, &amp;quot;age&amp;quot;: 3&#125;&lt;/optional&gt;
 *     &lt;nullable&gt;&#123;&amp;quot;name&amp;quot;: &amp;quot;n&amp;quot;, &amp;quot;age&amp;quot;: 3&#125;&lt;/nullable&gt;
 *     &lt;/objectBean&gt;
 * ObjectBean read = parser.deserialize(xml, XmlDeserConfig.create(), Type.of(ObjectBean.class));
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of L01 on the backends</caption>
 * <tr><th scope="col">Payload</th><th scope="col">Current output (all five)</th><th scope="col">Expected</th></tr>
 * <tr>
 * <td>Optional.of(Person(n,3)) in jdk, optional and nullable</td>
 * <td>jdk=Optional["&#123;\"name\": \"n\", \"age\": 3&#125;"], optional and nullable likewise: a String payload</td>
 * <td>Optional[Person(n,3)]</td>
 * </tr>
 * <tr>
 * <td>Optional.of(ArrayList["1", 2])</td>
 * <td>written as &lt;jdk&gt;[&amp;quot;1&amp;quot;, 2]&lt;/jdk&gt;; read: Optional["[\"1\", 2]"]</td>
 * <td>Optional[ArrayList["1", 2]]</td>
 * </tr>
 * <tr>
 * <td>Optional.empty() / u.Optional.empty() / u.Nullable.empty()</td>
 * <td>the properties are omitted (&lt;objectBean type="..."&gt;&lt;/objectBean&gt;); read: null</td>
 * <td>the empty wrapper</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The declared Object payload gives the writer no element type to record; a fix needs the writer to record the runtime class on a
 *   wrapper child, as it does for a plain Object property (&lt;values type="Object"&gt;&lt;person type="..."&gt;), and to write an empty
 *   wrapper explicitly.</li>
 * </ul>
 *
 * <h4>L02  Optional wrappers and uninstantiable immutable collections in open map values do not survive a round trip</h4>
 * <p><b>Status:</b> not changed (by design)</p>
 * <p><b>Problem.</b> In an open Map&lt;String,Object&gt; value the writer records the payload's class, not the Optional wrapper: a present
 * Optional is written as its payload and an empty one as a null marker. An uninstantiable implementation
 * (java.util.ImmutableCollections.List12, Map1) is recorded in the type attribute, and the readers construct the mutable fallback. The content
 * is preserved on every backend.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * Map&lt;String,Object&gt; source: optBean=Optional.of(Person(n,3)), optEmpty=Optional.empty(), optList=Optional.of(List.of("1","2")),
 *                            immutableList=List.of("a","b"), immutableMap=Map.of("k","v")
 * String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(true));   // standard XML:
 * &lt;map type="LinkedHashMap&amp;lt;Object, Object&amp;gt;"&gt;&lt;optBean type="...Person"&gt;
 *     &lt;person type="...Person"&gt;...&lt;/person&gt;&lt;/optBean&gt;&lt;optEmpty isNull="true" /&gt;
 *     &lt;optList type="java.util.ImmutableCollections.List12&amp;lt;Object&amp;gt;"&gt;[&amp;quot;1&amp;quot;, &amp;quot;2&amp;quot;]&lt;/optList&gt;
 *     &lt;immutableList type="java.util.ImmutableCollections.List12&amp;lt;Object&amp;gt;"&gt;[&amp;quot;a&amp;quot;, &amp;quot;b&amp;quot;]&lt;/immutableList&gt;
 *     &lt;immutableMap type="java.util.ImmutableCollections.Map1&amp;lt;Object, Object&amp;gt;"&gt;
 *     &lt;map type="...Map1&amp;lt;Object, Object&amp;gt;"&gt;&lt;k type="String"&gt;v&lt;/k&gt;&lt;/map&gt;&lt;/immutableMap&gt;&lt;/map&gt;
 * Map&lt;String,Object&gt; read = parser.deserialize(xml, XmlDeserConfig.create(), Type.of("Map&lt;String,Object&gt;"));
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of L02 on the backends</caption>
 * <tr><th scope="col">Value</th><th scope="col">Current output (all five)</th><th scope="col">Original</th></tr>
 * <tr><td>optBean</td><td>Person(n,3) (the wrapper is not restored)</td><td>Optional[Person(n,3)]</td></tr>
 * <tr><td>optEmpty</td><td>null</td><td>Optional.empty</td></tr>
 * <tr><td>optList</td><td>ArrayList["1", "2"]</td><td>Optional[List12["1", "2"]]</td></tr>
 * <tr><td>immutableList</td><td>ArrayList["a", "b"] (mutable fallback)</td><td>List12["a", "b"]</td></tr>
 * <tr><td>immutableMap</td><td>LinkedHashMap&#123;k="v"&#125; (mutable fallback)</td><td>Map1&#123;k="v"&#125;</td></tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>A declared Optional&lt;...&gt; slot restores the wrapper; only an open Object slot cannot know that the payload was wrapped.</li>
 * </ul>
 *
 * <h4>L03  Compact text of a List&lt;Object&gt; with mixed scalars loses Long and Character in standard XML</h4>
 * <p><b>Status:</b> not changed (format limitation)</p>
 * <p><b>Problem.</b> The standard writer writes an open list as compact JSON text, which has no Long or Character tokens: 2L is written as 2
 * and 'c' as "c". Abacus XML writes one &lt;e type="..."&gt; per item and keeps both. Without type information the standard readers return the
 * whole text as one String (the text of an open slot is text), the Abacus readers a list of Strings.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * Map&lt;String,Object&gt; source = &#123;scalars=[1, 2L, 3.5, true, 'c']&#125;;
 * standard, with type info:    &lt;map type="LinkedHashMap&amp;lt;Object, Object&amp;gt;"&gt;
 *     &lt;scalars type="...ListN&amp;lt;Object&amp;gt;"&gt;[1, 2, 3.5, true, &amp;quot;c&amp;quot;]&lt;/scalars&gt;&lt;/map&gt;
 * Abacus, with type info:      &lt;map type="..."&gt;&lt;entry&gt;&lt;key type="String"&gt;scalars&lt;/key&gt;
 *     &lt;value type="...ListN&amp;lt;Object&amp;gt;"&gt;&lt;list type="..."&gt;&lt;e type="Integer"&gt;1&lt;/e&gt;
 *     &lt;e type="Long"&gt;2&lt;/e&gt;&lt;e type="Double"&gt;3.5&lt;/e&gt;&lt;e type="Boolean"&gt;true&lt;/e&gt;
 *     &lt;e type="Character"&gt;c&lt;/e&gt;&lt;/list&gt;&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;
 * standard, without type info: &lt;map&gt;&lt;scalars&gt;[1, 2, 3.5, true, &amp;quot;c&amp;quot;]&lt;/scalars&gt;&lt;/map&gt;
 * read as Map&lt;String,Object&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of L03 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">XML StAX/DOM</th><th scope="col">Abacus StAX/DOM/SAX</th></tr>
 * <tr>
 * <td>with type information</td>
 * <td>ArrayList[1 (Integer), 2 (Integer), 3.5, true, "c"]</td>
 * <td>ArrayList[1 (Integer), 2L, 3.5, true, 'c']</td>
 * </tr>
 * <tr><td>without type information</td><td>"[1, 2, 3.5, true, \"c\"]" (one String)</td><td>ArrayList["1", "2", "3.5", "true", "c"]</td></tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Compact JSON text keeps JSON typing (L08): an unquoted integer token is an Integer, a quoted token a String.</li>
 * </ul>
 *
 * <h4>L04  A Date whose epoch text has four digits or fewer cannot be read back</h4>
 * <p><b>Status:</b> not changed (codec rule)</p>
 * <p><b>Problem.</b> The writers emit a java.util.Date as its epoch milliseconds. The date codecs treat numeric text of four digits or fewer
 * as ambiguous (it could be a year) and reject it, so dates within the first ten seconds of the epoch do not round-trip; JSON uses the same
 * codecs.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * Map&lt;String,Object&gt; source = &#123;date=new Date(0)&#125;;   // also new Date(9999) and new Date(10000)
 * &lt;map type="LinkedHashMap&amp;lt;Object, Object&amp;gt;"&gt;&lt;date type="JUDate"&gt;0&lt;/date&gt;&lt;/map&gt;
 * read as Map&lt;String,Object&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of L04 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Current output (all five)</th></tr>
 * <tr>
 * <td>new Date(0)</td>
 * <td>IllegalArgumentException: Ambiguous numeric date/time text; supply an explicit format or use parseEpochMillis: "0"</td>
 * </tr>
 * <tr>
 * <td>new Date(9999)</td>
 * <td>IllegalArgumentException: Ambiguous numeric date/time text; supply an explicit format or use parseEpochMillis: "9999"</td>
 * </tr>
 * <tr><td>new Date(10000)</td><td>Date 1970-01-01T00:00:10Z (five digits read as epoch milliseconds)</td></tr>
 * </table>
 *
 * <h4>L05  Bean-property map slots: the ordinary-fallback map class and the metadata's value argument differ between backends</h4>
 * <p><b>Status:</b> remaining difference</p>
 * <p><b>Problem.</b> For a map-typed BEAN PROPERTY the readers do not agree on the class of the ordinary fallback map (LinkedHashMap on XML
 * StAX, HashMap on the others) nor on whether a type attribute's value argument types the child elements: the StAX readers honor
 * LinkedHashMap&lt;Integer,Integer&gt;, the DOM and SAX readers read the values as text; without configuration Abacus StAX constructs an
 * ordinary map holding the list values of LinkedMultiValueMap metadata. Map values of a map (I29) and compact text agree on all five.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * public static class MapBean &#123; public Map&lt;Object,Object&gt; open; public Map&lt;String,Object&gt; values; /* getters and setters *&#47; &#125;
 * &lt;mapBean&gt;&lt;open type="java.util.LinkedHashMap&lt;Integer,Integer&gt;"&gt;&#123;"1":"1"&#125;&lt;/open&gt;
 *     &lt;/mapBean&gt;                 with setMapKeyType(Long.class)
 * &lt;mapBean&gt;&lt;open type="java.util.LinkedHashMap&lt;Integer,Integer&gt;"&gt;&lt;map&gt;&lt;e name="1"&gt;1&lt;/e&gt;&lt;/map&gt;
 *     &lt;/open&gt;&lt;/mapBean&gt;   same, child elements
 * &lt;mapBean&gt;&lt;values type="LinkedMultiValueMap&lt;String,Integer&gt;"&gt;&lt;map&gt;&lt;a&gt;1&lt;/a&gt;&lt;/map&gt;&lt;/values&gt;
 *     &lt;/mapBean&gt;            with setMapValueType(String.class), and without
 * &lt;mapBean&gt;&lt;values type="LinkedMultiValueMap&lt;String,Integer&gt;"&gt;&#123;"a":"1"&#125;&lt;/values&gt;
 *     &lt;/mapBean&gt;                    with setMapValueType(String.class)
 * (Abacus form of the entries: &lt;map&gt;&lt;entry&gt;&lt;key&gt;1&lt;/key&gt;&lt;value&gt;1&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of L05 on the backends</caption>
 * <tr>
 * <th scope="col">Document</th>
 * <th scope="col">XML StAX</th>
 * <th scope="col">XML DOM</th>
 * <th scope="col">Abacus StAX</th>
 * <th scope="col">Abacus DOM / SAX</th>
 * </tr>
 * <tr>
 * <td>open, compact, global key Long</td>
 * <td>LinkedHashMap&#123;1L=1&#125;</td>
 * <td>HashMap&#123;1L="1"&#125;</td>
 * <td>LinkedHashMap&#123;1L=1&#125;</td>
 * <td>LinkedHashMap&#123;1L=1&#125;</td>
 * </tr>
 * <tr>
 * <td>open, child elements, global key Long</td>
 * <td>LinkedHashMap&#123;1L=1&#125;</td>
 * <td>HashMap&#123;1L="1"&#125;</td>
 * <td>HashMap&#123;1L=1&#125;</td>
 * <td>HashMap&#123;1L="1"&#125;</td>
 * </tr>
 * <tr>
 * <td>values, child elements, global String</td>
 * <td>LinkedHashMap&#123;a="1"&#125;</td>
 * <td>HashMap&#123;a="1"&#125;</td>
 * <td>HashMap&#123;a="1"&#125;</td>
 * <td>HashMap&#123;a="1"&#125;</td>
 * </tr>
 * <tr>
 * <td>values, compact, global String</td>
 * <td>HashMap&#123;a="1"&#125;</td>
 * <td>HashMap&#123;a="1"&#125;</td>
 * <td>HashMap&#123;a="1"&#125;</td>
 * <td>HashMap&#123;a="1"&#125;</td>
 * </tr>
 * <tr>
 * <td>values, child elements, no configuration</td>
 * <td>LinkedMultiValueMap&#123;a=[1]&#125;</td>
 * <td>HashMap&#123;a="1"&#125;</td>
 * <td>HashMap&#123;a=[1]&#125;</td>
 * <td>HashMap&#123;a="1"&#125;</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The content agrees whenever the configuration decides; the differences appear when the metadata on a bean property has to decide. The
 *   compact root map under list-valued metadata (I02) likewise yields a HashMap on four backends and a LinkedHashMap on Abacus SAX.</li>
 * </ul>
 *
 * <h4>L06  A text-only element of an unknown name inside an open slot</h4>
 * <p><b>Status:</b> remaining difference (by design)</p>
 * <p><b>Problem.</b> Without type information a single child of an open Object slot whose name is not a container name is a fallback map on
 * the standard readers (an empty one: the element's text is not a property), while the Abacus readers reject it, because a text-only element
 * is not bean-shaped and names no bean class. Two such children give a list of two empty maps on the standard readers.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map&gt;&lt;k&gt;&lt;a&gt;1&lt;/a&gt;&lt;/k&gt;&lt;/map&gt;              read as Map&lt;String,Object&gt;
 * &lt;map&gt;&lt;k&gt;&lt;a&gt;1&lt;/a&gt;&lt;b&gt;2&lt;/b&gt;&lt;/k&gt;&lt;/map&gt;
 * &lt;map&gt;&lt;k&gt;&lt;map&gt;&lt;a&gt;1&lt;/a&gt;&lt;/map&gt;&lt;/k&gt;&lt;/map&gt;   the serializer's shape for a nested map (control)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of L06 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">XML StAX/DOM</th><th scope="col">Abacus StAX/DOM/SAX</th></tr>
 * <tr>
 * <td>&lt;a&gt;1&lt;/a&gt;</td>
 * <td>HashMap&#123;&#125; (empty fallback map)</td>
 * <td>ParsingException: No bean class found for node name: a in package of class: java.util.Map</td>
 * </tr>
 * <tr>
 * <td>&lt;a&gt;1&lt;/a&gt;&lt;b&gt;2&lt;/b&gt;</td>
 * <td>ArrayList[HashMap&#123;&#125;, HashMap&#123;&#125;]</td>
 * <td>StAX/SAX: the same ParsingException; DOM: ParsingException: Only one child element is supported</td>
 * </tr>
 * <tr>
 * <td>control: &lt;map&gt;&lt;a&gt;1&lt;/a&gt;&lt;/map&gt;</td>
 * <td>HashMap&#123;a="1"&#125;</td>
 * <td>LinkedHashMap&#123;a="1"&#125;</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>A bean-shaped element (one with child elements) in an open slot is read as a map of its properties on every backend (I18).</li>
 * </ul>
 *
 * <h4>L07  The root contract: an explicit global map key or value type overrides the requested type's own arguments at the root</h4>
 * <p><b>Status:</b> intentional (tested contract)</p>
 * <p><b>Problem.</b> At the root, setMapKeyType / setMapValueType take precedence over the requested type's scalar, bean or open arguments,
 * for child elements and compact text alike; a requested container argument (array, collection, map) keeps its shape with the global type
 * applied inside it (I37), and below the root the declared arguments keep their precedence. The JSON reader does the opposite for the same
 * text and configuration (&#123;1=1&#125; with Integer key and value). A global key type therefore applies to the root map's own keys as well,
 * which fails when they are not convertible.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setMapKeyType(Long.class).setMapValueType(String.class);
 * &lt;map&gt;&lt;e name="1"&gt;1&lt;/e&gt;&lt;/map&gt;            read as Map&lt;Integer,Integer&gt;   (Abacus: &lt;map&gt;&lt;entry&gt;
 *     &lt;key&gt;1&lt;/key&gt;&lt;value&gt;1&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;)
 * &lt;map&gt;&#123;"1":1&#125;&lt;/map&gt;                      read as Map&lt;Integer,Integer&gt;
 * &lt;list&gt;&lt;e&gt;&#123;"1":1&#125;&lt;/e&gt;&lt;/list&gt;             read as List&lt;Map&lt;Integer,Integer&gt;&gt;
 * &lt;map&gt;&lt;k&gt;&#123;"1":1&#125;&lt;/k&gt;
 *     &lt;/map&gt;               read as Map&lt;String,Map&lt;Integer,Integer&gt;&gt; with setMapKeyType(Long.class) only
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of L07 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Current output (all five)</th><th scope="col">Note</th></tr>
 * <tr><td>root map, child elements</td><td>HashMap&#123;1L="1"&#125;</td><td>the configuration wins over Map&lt;Integer,Integer&gt;</td></tr>
 * <tr><td>root map, compact text</td><td>HashMap&#123;1L="1"&#125;</td><td>same as the child elements since r53 (I30)</td></tr>
 * <tr>
 * <td>map below a list root</td>
 * <td>ArrayList[HashMap&#123;1 (Integer)=1 (Integer)&#125;]</td>
 * <td>declared arguments win below the root</td>
 * </tr>
 * <tr>
 * <td>map value under a global key type</td>
 * <td>NumberFormatException: k is not a valid Long.</td>
 * <td>the root key "k" is read as a Long too</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Pinned by XmlParserImplTest and AbacusXmlParserImplTest testRootGenericTypesPreserveExplicitConfigAndNestedValues; a change was tried
 *   during round 5 and withdrawn because it broke those tests and the MultiValueMap guard cells.</li>
 * </ul>
 *
 * <h4>L08  Compact JSON text keeps JSON typing while element text is String</h4>
 * <p><b>Status:</b> intentional</p>
 * <p><b>Problem.</b> Inside compact JSON text an unquoted token keeps its JSON type (1 is an Integer, true a Boolean) and a quoted token is a
 * String; the text of an &lt;e&gt; element, or of any element read without a type, is a String. A compact map in an element of an open list is
 * the element's text, a String, unless the element type names a map.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map&gt;&lt;k&gt;[1, "2", true]&lt;/k&gt;&lt;/map&gt;                 read as Map&lt;String,List&lt;Object&gt;&gt;
 * &lt;map&gt;&lt;k&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;e&gt;true&lt;/e&gt;&lt;/k&gt;
 *     &lt;/map&gt;   same, child elements (Abacus: &lt;value&gt;&lt;list&gt;&lt;e&gt;1&lt;/e&gt;...&lt;/list&gt;&lt;/value&gt;)
 * &lt;list&gt;&lt;e&gt;&#123;"a":1&#125;&lt;/e&gt;
 *     &lt;/list&gt;                     read as List&lt;Object&gt; and as List&lt;Map&lt;String,Object&gt;&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of L08 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Current output (all five)</th></tr>
 * <tr><td>compact [1, "2", true] as List&lt;Object&gt;</td><td>ArrayList[1 (Integer), "2", true (Boolean)]</td></tr>
 * <tr>
 * <td>child elements &lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;e&gt;true&lt;/e&gt; as List&lt;Object&gt;</td>
 * <td>ArrayList["1", "2", "true"]</td>
 * </tr>
 * <tr>
 * <td>&lt;e&gt;&#123;"a":1&#125;&lt;/e&gt; as List&lt;Object&gt;</td>
 * <td>ArrayList["&#123;\"a\":1&#125;"] (the text of an open element)</td>
 * </tr>
 * <tr>
 * <td>&lt;e&gt;&#123;"a":1&#125;&lt;/e&gt; as List&lt;Map&lt;String,Object&gt;&gt;</td>
 * <td>ArrayList[HashMap&#123;a=1 (Integer)&#125;]</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Property values of an untyped bean-shaped element are Strings for the same reason (I18: age="3").</li>
 * </ul>
 *
 * <h4>L09  Element configuration never types map values</h4>
 * <p><b>Status:</b> intentional</p>
 * <p><b>Problem.</b> setElementType applies to the elements of lists and arrays and to the open components nested in them; a map's key and
 * value types are schema and come from the declaration, the metadata, setMapKeyType / setMapValueType and the per-key setValueType. A
 * Map&lt;String,Long&gt; element type on a root map changes nothing.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map&gt;&lt;a&gt;1&lt;/a&gt;
 *     &lt;/map&gt;       read as Map&lt;String,Object&gt; with setElementType(Type.of("Map&lt;String,Long&gt;")), and with setElementType(Long.class)
 * &lt;map&gt;&#123;"a":1&#125;&lt;/map&gt;       read as Map&lt;String,Object&gt; with setElementType(Type.of("Map&lt;String,Long&gt;"))
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of L09 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Current output (all five)</th></tr>
 * <tr><td>child element, element type Map&lt;String,Long&gt;</td><td>HashMap&#123;a="1"&#125;</td></tr>
 * <tr><td>child element, element type Long</td><td>HashMap&#123;a="1"&#125;</td></tr>
 * <tr><td>compact text, element type Map&lt;String,Long&gt;</td><td>HashMap&#123;a=1 (Integer)&#125; (JSON typing, L08)</td></tr>
 * </table>
 *
 * <h4>L10  A map or list value under a global scalar map value type is rejected in compact text</h4>
 * <p><b>Status:</b> intentional (JSON rule)</p>
 * <p><b>Problem.</b> A global setMapValueType names the type of every open map value, at the root and, since I43, at any depth of compact
 * text. Compact text follows the JSON reader: a value that is a JSON object or array where a scalar value type is configured is rejected with
 * a NumberFormatException, at the root (unchanged since r53; the JSON reader does the same) and inside nested maps, where r56 silently dropped
 * the configuration. Child elements read such a value structurally and apply the scalar type to its leaves.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig longs = XmlDeserConfig.create().setMapValueType(Long.class);
 * &lt;map&gt;&#123;"a":&#123;"b":1&#125;&#125;&lt;/map&gt;                  read as Map&lt;String,Object&gt;
 * &lt;map&gt;&#123;"a":[1,2]&#125;&lt;/map&gt;                    read as Map&lt;String,Object&gt;
 * &lt;map&gt;&#123;"k":&#123;"a":[1,2]&#125;&#125;&lt;/map&gt;              read as Map&lt;String,Map&lt;String,Object&gt;&gt;
 * &lt;map&gt;&lt;a&gt;&lt;map&gt;&lt;b&gt;1&lt;/b&gt;&lt;/map&gt;&lt;/a&gt;&lt;/map&gt;    child elements (Abacus: &lt;map&gt;&lt;entry&gt;
 *     &lt;key&gt;a&lt;/key&gt;&lt;value&gt;&lt;map&gt;..&lt;/map&gt;&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of L10 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Current output (all five)</th></tr>
 * <tr>
 * <td>compact &#123;"a":&#123;"b":1&#125;&#125; as Map&lt;String,Object&gt;</td>
 * <td>NumberFormatException: &#123;"b": 1&#125; is not a valid Long (the JSON reader: the same)</td>
 * </tr>
 * <tr><td>compact &#123;"a":[1,2]&#125; as Map&lt;String,Object&gt;</td><td>NumberFormatException: [1, 2] is not a valid Long</td></tr>
 * <tr>
 * <td>compact &#123;"k":&#123;"a":[1,2]&#125;&#125; as Map&lt;String,Map&lt;String,Object&gt;&gt;</td>
 * <td>NumberFormatException: [1, 2] is not a valid Long (r56: HashMap&#123;k=HashMap&#123;a=[1, 2]&#125;&#125; with Integers, the
 *     configuration lost)</td>
 * </tr>
 * <tr>
 * <td>child elements &lt;a&gt;&lt;map&gt;&lt;b&gt;1&lt;/b&gt;&lt;/map&gt;&lt;/a&gt; as Map&lt;String,Object&gt;</td>
 * <td>XML StAX/DOM: HashMap&#123;a=[HashMap&#123;b=1L&#125;]&#125; (a nested container child of a typed slot forms a list); Abacus
 *     StAX/DOM/SAX: HashMap&#123;a=LinkedHashMap&#123;b=1L&#125;&#125;</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>A scalar global value type says that every open value is that scalar. Maps that hold structured values take a container value type, a
 *   per-key type (setValueType) or a declared value argument instead.</li>
 * </ul>
 *
 * <h4>L11  Without type information, the standard StAX reader adopts a scalar type attribute on an Object bean property; the other four keep the text</h4>
 * <p><b>Status:</b> remaining difference</p>
 * <p><b>Problem.</b> When the root element carries no type attribute (type information is ignored), a scalar type attribute on a bean property
 * declared Object is read as the declared Object's text by the standard DOM reader and by the three Abacus readers, but the standard StAX
 * reader adopts it and converts the text. With type information all five adopt it (I44). The Abacus readers also differ from the standard ones
 * for a primitive name: type="int" on an Object property is Integer 5 on XML StAX/DOM and the text "5" on Abacus StAX/DOM/SAX (a primitive
 * name does not fit an Object slot there), with and without type information.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * public static class Holder &#123; public Object open; /* getter and setter *&#47; &#125;   // the root element carries NO type attribute
 * &lt;holder&gt;&lt;open type="Long"&gt;5&lt;/open&gt;&lt;/holder&gt;       read as Holder, also with type="Integer"
 * &lt;holder type="...Holder"&gt;&lt;open type="int"&gt;5&lt;/open&gt;&lt;/holder&gt;   with type information, a primitive name
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of L11 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">XML StAX</th><th scope="col">XML DOM</th><th scope="col">Abacus StAX/DOM/SAX</th></tr>
 * <tr><td>untyped root, type="Long"</td><td>5L</td><td>"5"</td><td>"5"</td></tr>
 * <tr><td>untyped root, type="Integer"</td><td>5 (Integer)</td><td>"5"</td><td>"5"</td></tr>
 * <tr><td>typed root, type="int"</td><td>5 (Integer)</td><td>5 (Integer)</td><td>"5"</td></tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Pinned per backend by AbacusXmlParserImplTest.testReadObjectProperty_unadoptedScalarAttributeShapesPinned (the Abacus readers'
 *   untyped-root results) so that no side moves silently; the standard StAX/DOM difference needs a ruling (it is the same shape the Abacus
 *   readers agreed on in I44).</li>
 * </ul>
 *
 * <h4>L13  Type attributes on the properties of a bean-shaped element read as a map differ per Abacus backend</h4>
 * <p><b>Status:</b> remaining difference</p>
 * <p><b>Problem.</b> A bean-shaped element in an open slot is read as a map of its properties on every Abacus backend (I18, I21, I47) when its
 * properties carry no type attributes. With attributes the three Abacus readers disagree: a scalar attribute is text on StAX and DOM and a
 * typed value on SAX; a container attribute with compact JSON text stays text on StAX and is a list on DOM and SAX; a container attribute with
 * child elements gives String items on StAX and DOM and typed items on SAX; a bean class on a nested child gives a map on StAX and DOM and the
 * bean on SAX. The same with and without type information on the root. The standard readers do not read a bean-shaped &lt;e&gt; item at all:
 * its content is the element's text (L08).</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;list&gt;&lt;e&gt;&lt;thing&gt;&lt;a type="Integer"&gt;1&lt;/a&gt;&lt;b&gt;2&lt;/b&gt;&lt;/thing&gt;&lt;/e&gt;
 *     &lt;/list&gt;                           read as List&lt;Object&gt;
 * &lt;list&gt;&lt;e&gt;&lt;thing&gt;&lt;a type="List&lt;Integer&gt;"&gt;[1, 2]&lt;/a&gt;&lt;b&gt;2&lt;/b&gt;&lt;/thing&gt;&lt;/e&gt;
 *     &lt;/list&gt;
 * &lt;list&gt;&lt;e&gt;&lt;thing&gt;&lt;a type="List&lt;Integer&gt;"&gt;&lt;list&gt;&lt;e&gt;1&lt;/e&gt;&lt;/list&gt;&lt;/a&gt;
 *     &lt;b&gt;2&lt;/b&gt;&lt;/thing&gt;&lt;/e&gt;&lt;/list&gt;
 * &lt;list&gt;&lt;e&gt;&lt;thing&gt;&lt;a type="...Child"&gt;&lt;child&gt;&lt;name&gt;k&lt;/name&gt;&lt;count&gt;1&lt;/count&gt;&lt;/child&gt;
 *     &lt;/a&gt;&lt;b&gt;2&lt;/b&gt;&lt;/thing&gt;&lt;/e&gt;&lt;/list&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of L13 on the backends</caption>
 * <tr>
 * <th scope="col">Attribute on property a</th>
 * <th scope="col">Abacus StAX</th>
 * <th scope="col">Abacus DOM</th>
 * <th scope="col">Abacus SAX</th>
 * <th scope="col">XML StAX/DOM</th>
 * </tr>
 * <tr>
 * <td>Integer, text 1</td>
 * <td>LinkedHashMap&#123;a="1", b="2"&#125;</td>
 * <td>LinkedHashMap&#123;a="1", b="2"&#125;</td>
 * <td>LinkedHashMap&#123;a=1 (Integer), b="2"&#125;</td>
 * <td>ArrayList["12"] (the &lt;e&gt; text)</td>
 * </tr>
 * <tr>
 * <td>List&lt;Integer&gt;, compact [1, 2]</td>
 * <td>LinkedHashMap&#123;a="[1, 2]", b="2"&#125;</td>
 * <td>LinkedHashMap&#123;a=ArrayList[1, 2], b="2"&#125;</td>
 * <td>LinkedHashMap&#123;a=ArrayList[1, 2], b="2"&#125;</td>
 * <td>ArrayList["[1, 2]2"]</td>
 * </tr>
 * <tr>
 * <td>List&lt;Integer&gt;, child &lt;list&gt;&lt;e&gt;1&lt;/e&gt;&lt;/list&gt;</td>
 * <td>LinkedHashMap&#123;a=ArrayList["1"], b="2"&#125;</td>
 * <td>LinkedHashMap&#123;a=ArrayList["1"], b="2"&#125;</td>
 * <td>LinkedHashMap&#123;a=ArrayList[1 (Integer)], b="2"&#125;</td>
 * <td>ArrayList["12"]</td>
 * </tr>
 * <tr>
 * <td>...Child, child &lt;child&gt;...&lt;/child&gt;</td>
 * <td>LinkedHashMap&#123;a=LinkedHashMap&#123;name="k", count="1"&#125;, b="2"&#125;</td>
 * <td>LinkedHashMap&#123;a=LinkedHashMap&#123;name="k", count="1"&#125;, b="2"&#125;</td>
 * <td>LinkedHashMap&#123;a=Child(k,1), b="2"&#125;</td>
 * <td>ArrayList["k12"]</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Reported as R3-07 in the ledger of the 2026-10-07 review; the SAX handler types a sink property from its attribute at the end of
 *   startElement, the StAX and DOM readers read it with the declared Object. Two or more text children directly under such a property are
 *   rejected everywhere, with the two messages of L06.</li>
 *   <li>AbacusXmlParserImplTest.testReadBeanShapedElement_nestedBeanShapedElementOnAllBackends pins attribute-free shapes only.</li>
 * </ul>
 *
 * <h4>L14  An explicit Object override with a scalar type attribute: the Abacus readers keep the text, the standard readers adopt the attribute</h4>
 * <p><b>Status:</b> remaining difference</p>
 * <p><b>Problem.</b> A per-key setValueType("k", Object.class) is an open scalar override (I04): on the three Abacus readers the text of a map
 * value written as &lt;value type="Integer"&gt;2&lt;/value&gt; stays the String "2" (I46 aligned the SAX reader), while the standard StAX and
 * DOM readers adopt the scalar attribute and return Integer 2. Container metadata (a list) and a nested bean class are adopted on all five;
 * without the override every backend adopts the scalar attribute; a configured scalar type converts on all five. The same split holds for a
 * bean class on the value's text (the Abacus readers keep the text, I51; the standard readers convert it with the bean class) and for a bean
 * property configured as Object (setValueType("open", Object.class), I50): its scalar and Optional attributes are text on the Abacus readers
 * and adopted by the standard readers.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setValueType("k", Type.of(Object.class));
 * &lt;map type="HashMap&lt;String, Object&gt;"&gt;&lt;k type="Integer"&gt;2&lt;/k&gt;
 *     &lt;/map&gt;                               read as Map&lt;String,Object&gt;
 * (Abacus form: &lt;map type="..."&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value type="Integer"&gt;2&lt;/value&gt;&lt;/entry&gt;
 *     &lt;/map&gt;)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of L14 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">XML StAX/DOM</th><th scope="col">Abacus StAX/DOM/SAX</th></tr>
 * <tr><td>override, type="Integer", text 2</td><td>2 (Integer)</td><td>"2"</td></tr>
 * <tr><td>override, no attribute</td><td>"2"</td><td>"2"</td></tr>
 * <tr><td>override, type="List&lt;Integer&gt;", compact [1, 2]</td><td>ArrayList[1, 2]</td><td>ArrayList[1, 2]</td></tr>
 * <tr><td>override, type="...Child", child element</td><td>Child(k,1)</td><td>Child(k,1)</td></tr>
 * <tr><td>override, type="...Child", text 5</td><td>ParsingException: Can't parse: 5</td><td>"5"</td></tr>
 * <tr><td>bean property configured as Object, type="Integer", typed root</td><td>5 (Integer)</td><td>"5"</td></tr>
 * <tr><td>bean property configured as Object, type="Optional&lt;Long&gt;", typed root</td><td>Optional[5]</td><td>"5"</td></tr>
 * <tr><td>no override, type="Integer"</td><td>2 (Integer)</td><td>2 (Integer)</td></tr>
 * <tr><td>setValueType("k", Long.class), type="Integer"</td><td>2L</td><td>2L</td></tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The Abacus rule follows I04 (an explicit Object override is open scalar text); which family should move is a ruling for the
 *   owner.</li>
 *   <li>Before I50 and I51 (at r62) the Abacus SAX reader converted the bean text (ParsingException: Can't parse: 5) and adopted the Optional
 *   attribute of a bean property configured as Object (Optional[5]).</li>
 * </ul>
 *
 * <h4>L15  Without type information, an Optional type attribute on an Object bean property is adopted by the StAX and SAX readers and kept as text by the DOM readers</h4>
 * <p><b>Status:</b> remaining difference</p>
 * <p><b>Problem.</b> When the root element carries no type attribute, the Abacus readers keep a scalar type attribute on an Object property as
 * text and adopt container metadata (L11). An Optional attribute (Optional&lt;Long&gt;, Optional&lt;Object&gt;, ...) falls between the two
 * rules: the Abacus StAX and SAX readers and the standard StAX reader adopt it (Optional[5], NumberFormatException for the text x,
 * Optional.empty for an element without content), the Abacus DOM reader and the standard DOM reader keep the text ("5", "x", ""). Container
 * metadata with compact text is adopted by four backends and kept as text by the standard DOM reader. With type information all five adopt
 * both.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * public static class Holder &#123; public Object open; /* getter and setter *&#47; &#125;   // the root element carries NO type attribute
 * &lt;holder&gt;&lt;open type="Optional&lt;Long&gt;"&gt;5&lt;/open&gt;
 *     &lt;/holder&gt;     read as Holder; also Optional&lt;Integer&gt;, Optional&lt;Object&gt;, Optional&lt;String&gt;
 * &lt;holder&gt;&lt;open type="Optional&lt;Long&gt;"&gt;x&lt;/open&gt;
 *     &lt;/holder&gt;     unreadable text; also &lt;open type="Optional&lt;Long&gt;"&gt;&lt;/open&gt;
 * &lt;holder&gt;&lt;open type="List&lt;Integer&gt;"&gt;[1, 2]&lt;/open&gt;&lt;/holder&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of L15 on the backends</caption>
 * <tr>
 * <th scope="col">Document</th>
 * <th scope="col">Abacus StAX/SAX, XML StAX</th>
 * <th scope="col">Abacus DOM</th>
 * <th scope="col">XML DOM</th>
 * </tr>
 * <tr><td>untyped root, type="Optional&lt;Long&gt;", text 5</td><td>Optional[5]</td><td>"5"</td><td>"5"</td></tr>
 * <tr>
 * <td>untyped root, type="Optional&lt;Long&gt;", text x</td>
 * <td>NumberFormatException: x is not a valid Long.</td>
 * <td>"x"</td>
 * <td>"x"</td>
 * </tr>
 * <tr><td>untyped root, type="Optional&lt;Long&gt;", no content</td><td>Optional.empty</td><td>""</td><td>""</td></tr>
 * <tr><td>untyped root, type="List&lt;Integer&gt;", [1, 2]</td><td>ArrayList[1, 2]</td><td>ArrayList[1, 2]</td><td>"[1, 2]"</td></tr>
 * <tr>
 * <td>typed root, either attribute</td>
 * <td>Optional[5]; ArrayList[1, 2]</td>
 * <td>Optional[5]; ArrayList[1, 2]</td>
 * <td>Optional[5]; ArrayList[1, 2]</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Found by the recheck of 2026-10-07 (the remaining disagreements of its property-attribute matrix between the Abacus readers); not
 *   pinned by a test. Whether an Optional wrapper follows the scalar rule (text) or the container rule (adopted) without type information is a
 *   ruling for the owner.</li>
 * </ul>
 *
 * <h4>L16  The standard StAX reader reads a bean property element without content with its own fallback</h4>
 * <p><b>Status:</b> remaining difference</p>
 * <p><b>Problem.</b> A bean property element without content is the empty text, read with the property's text rule, on the three Abacus
 * readers (I55) and the standard DOM reader. The standard StAX reader uses the selected type's {@code defaultValue()}, except that String,
 * open Object and enum properties are read through {@code valueOf("")}: an Optional&lt;Object&gt; property under int metadata or without metadata is Optional.empty where
 * the other four read Optional[0] and Optional[""], and a property with a number format is null where the other four fail with the format's
 * ParsingException (as all five do for blank text). Map values follow I20 on all five.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * public static class Rec &#123; &#64;JsonXmlField(numberFormat = "#,##0") Integer amount; Optional&lt;Object&gt; opt; /* getters and setters *&#47; &#125;
 * &lt;rec type="...Rec"&gt;&lt;opt type="int"&gt;&lt;/opt&gt;&lt;/rec&gt;          read as Rec; also &lt;opt&gt;&lt;/opt&gt;
 * &lt;rec type="...Rec"&gt;&lt;amount&gt;&lt;/amount&gt;&lt;/rec&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of L16 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">XML StAX</th><th scope="col">XML DOM, Abacus StAX/DOM/SAX</th></tr>
 * <tr><td>&lt;opt type="int"&gt;&lt;/opt&gt;</td><td>Optional.empty</td><td>Optional[0]</td></tr>
 * <tr><td>&lt;opt&gt;&lt;/opt&gt;</td><td>Optional.empty</td><td>Optional[""]</td></tr>
 * <tr>
 * <td>&lt;amount&gt;&lt;/amount&gt;, numberFormat #,##0</td>
 * <td>null</td>
 * <td>ParsingException: Failed to parse complete number value ...</td>
 * </tr>
 * <tr>
 * <td>control: &lt;opt type="Long"&gt;&lt;/opt&gt;, &lt;text&gt;&lt;/text&gt;, &lt;count&gt;&lt;/count&gt;</td>
 * <td>Optional.empty, "", 0</td>
 * <td>the same</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Where the standard readers adopt a scalar attribute that the Abacus readers ignore (L11), the empty value of that type follows:
 *   &lt;open type="int"&gt;&lt;/open&gt; is 0 (Integer) on both standard readers, and without type information &lt;open
 *   type="Long"&gt;&lt;/open&gt; is null on the standard StAX reader.</li>
 * </ul>
 * <h3>Part 2: Fixed issues (history, r47 to r57, r62 and the recheck fixes of 2026-10-07)</h3>
 *
 * <h4>I01  Unicode padding around compact JSON text of reference arrays and collections</h4>
 * <p><b>Fixed in:</b> r47</p>
 * <p><b>Problem.</b> Compact JSON text padded with Unicode whitespace (an em space, U+2003, written as &amp;#x2003;) was rejected for
 * reference arrays and collections on every backend, at the root and as a map value, although String[] and primitive-array text already
 * tolerated it.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create();
 * &lt;array&gt;&amp;#x2003;[1, 2]&amp;#x2003;&lt;/array&gt;                      read as Long[]
 * &lt;list&gt;&amp;#x2003;[1, 2]&amp;#x2003;&lt;/list&gt;                        read as List&lt;Long&gt;
 * &lt;map&gt;&lt;k&gt;&amp;#x2003;[1, 2]&amp;#x2003;&lt;/k&gt;
 *     &lt;/map&gt;                   read as Map&lt;String,List&lt;Long&gt;&gt; and Map&lt;String,Long[]&gt;
 * (Abacus form of the map: &lt;map&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value&gt;&amp;#x2003;[1, 2]&amp;#x2003;&lt;/value&gt;
 *     &lt;/entry&gt;&lt;/map&gt;)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I01 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (all five)</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>root &lt;array&gt; as Long[]</td>
 * <td>ParsingException: Unexpected content before the root JSON value: ?</td>
 * <td>Long[]&#123;1, 2&#125;</td>
 * </tr>
 * <tr>
 * <td>root &lt;list&gt; as List&lt;Long&gt;</td>
 * <td>ParsingException: Unexpected content before the root JSON value: ?</td>
 * <td>ArrayList[1L, 2L]</td>
 * </tr>
 * <tr>
 * <td>map value as Map&lt;String,List&lt;Long&gt;&gt;</td>
 * <td>ParsingException: Unexpected content before the root JSON value: ?</td>
 * <td>ArrayList[1L, 2L]</td>
 * </tr>
 * <tr>
 * <td>map value as Map&lt;String,Long[]&gt;</td>
 * <td>ParsingException: Unexpected content before the root JSON value: ?</td>
 * <td>Long[]&#123;1, 2&#125;</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The padding is stripped with String.strip (Unicode-aware) before the JSON reader sees the text; whitespace inside quoted string
 *   elements is preserved.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.referenceContainerTextAcceptsUnicodePaddingOnEveryBackend</p>
 *
 * <h4>I02  List-valued map metadata (Spring MultiValueMap) on a map whose declared or configured values are scalars</h4>
 * <p><b>Fixed in:</b> r47</p>
 * <p><b>Problem.</b> A type attribute naming a list-valued implementation on a map declared Map&lt;String,Integer&gt;, or read with a global
 * scalar value type, made the readers construct the implementation and fail inside it when the scalar value was stored; the compact form
 * returned an empty map silently.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map type="org.springframework.util.LinkedMultiValueMap"&gt;&lt;a&gt;1&lt;/a&gt;
 *     &lt;/map&gt;                 read as Map&lt;String,Integer&gt;
 * &lt;map type="org.springframework.util.LinkedMultiValueMap"&gt;&#123;"a":1&#125;&lt;/map&gt;              read as Map&lt;String,Integer&gt;
 * the first document read as Map&lt;String,Object&gt; with XmlDeserConfig.create().setMapValueType(String.class)
 * &lt;map&gt;&lt;k type="org.springframework.util.LinkedMultiValueMap"&gt;&lt;map&gt;&lt;a&gt;1&lt;/a&gt;&lt;/map&gt;&lt;/k&gt;
 *     &lt;/map&gt;   read as Map&lt;String,Map&lt;String,Integer&gt;&gt;
 * (Abacus form of an entry: &lt;entry&gt;&lt;key&gt;a&lt;/key&gt;&lt;value&gt;1&lt;/value&gt;&lt;/entry&gt;)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I02 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix</th></tr>
 * <tr>
 * <td>root, child elements, Map&lt;String,Integer&gt;</td>
 * <td>all five: ClassCastException (Integer cannot be cast to List)</td>
 * <td>all five: LinkedHashMap&#123;a=1&#125;</td>
 * </tr>
 * <tr>
 * <td>root, compact text, Map&lt;String,Integer&gt;</td>
 * <td>all five: LinkedMultiValueMap&#123;&#125; (empty)</td>
 * <td>XML StAX/DOM, Abacus StAX/DOM: HashMap&#123;a=1&#125;; Abacus SAX: LinkedHashMap&#123;a=1&#125;</td>
 * </tr>
 * <tr>
 * <td>root, child elements, Map&lt;String,Object&gt; + setMapValueType(String)</td>
 * <td>all five: ClassCastException (String cannot be cast to List)</td>
 * <td>all five: LinkedHashMap&#123;a="1"&#125;</td>
 * </tr>
 * <tr>
 * <td>map value with the metadata, Map&lt;String,Map&lt;String,Integer&gt;&gt;</td>
 * <td>all five: ClassCastException (Integer cannot be cast to List)</td>
 * <td>all five: LinkedHashMap&#123;a=1&#125;</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The declaration wins over the implementation: a list-valued map cannot hold a declared scalar, set or concrete-list value, so the
 *   readers keep ordinary map construction. Compatible List&lt;V&gt; and open Object values keep the specialized implementation.</li>
 *   <li>For the compact root form the map class differs slightly (HashMap from the JSON reader on four backends, LinkedHashMap on Abacus SAX,
 *   which fills the map it had created); the content is the same.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.specializedMapMetadataKeepsDeclaredScalarValues</p>
 *
 * <h4>I03  A named String override inside a list-valued map, in compact text and in child elements, at any nesting level</h4>
 * <p><b>Fixed in:</b> r47 (r48 for the fallback class of child elements)</p>
 * <p><b>Problem.</b> setValueType("a", String.class) must apply to the key "a" wherever it appears. Under LinkedMultiValueMap metadata the
 * compact form ignored the override and kept list values, and the child-element form failed with a ClassCastException on the standard readers;
 * the same happened for a list of such maps and for an array of them.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setValueType("a", String.class);
 * &lt;map&gt;&lt;k type="LinkedMultiValueMap&lt;String,Integer&gt;"&gt;&#123;"a":"1"&#125;&lt;/k&gt;
 *     &lt;/map&gt;                        read as Map&lt;String,Map&lt;String,Object&gt;&gt;
 * &lt;map&gt;&lt;k type="LinkedMultiValueMap&lt;String,Integer&gt;"&gt;&lt;map&gt;&lt;a&gt;1&lt;/a&gt;&lt;/map&gt;&lt;/k&gt;
 *     &lt;/map&gt;              same declaration, child elements
 * &lt;map&gt;&lt;k type="ArrayList&lt;LinkedMultiValueMap&lt;String,Integer&gt;&gt;"&gt;[&#123;"a":"1"&#125;]&lt;/k&gt;
 *     &lt;/map&gt;          read as Map&lt;String,List&lt;Map&lt;String,Object&gt;&gt;&gt;
 * &lt;map&gt;&lt;k type="LinkedMultiValueMap[]"&gt;[&#123;"a":"1"&#125;]&lt;/k&gt;
 *     &lt;/map&gt;                                  read as Map&lt;String,Map&lt;String,Object&gt;[]&gt;
 * (the class name in the attribute is org.springframework.util.LinkedMultiValueMap, written in full and XML-escaped)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I03 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>map value, compact</td>
 * <td>all five: LinkedMultiValueMap&#123;a=[1]&#125; (override ignored)</td>
 * <td>LinkedHashMap&#123;a="1"&#125;</td>
 * </tr>
 * <tr>
 * <td>map value, child elements</td>
 * <td>all five: ClassCastException (String cannot be cast to List)</td>
 * <td>LinkedHashMap&#123;a="1"&#125;</td>
 * </tr>
 * <tr>
 * <td>list of maps, compact</td>
 * <td>all five: ArrayList[LinkedMultiValueMap&#123;a=[1]&#125;]</td>
 * <td>ArrayList[LinkedHashMap&#123;a="1"&#125;]</td>
 * </tr>
 * <tr>
 * <td>list of maps, child elements</td>
 * <td>XML StAX/DOM: ClassCastException; Abacus: ArrayList[HashMap&#123;a="1"&#125;]</td>
 * <td>ArrayList[LinkedHashMap&#123;a="1"&#125;]</td>
 * </tr>
 * <tr>
 * <td>array of maps, compact</td>
 * <td>all five: LinkedMultiValueMap[]&#123;LinkedMultiValueMap&#123;a=[1]&#125;&#125; (the array class was narrowed too)</td>
 * <td>Map[]&#123;LinkedHashMap&#123;a="1"&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>array of maps, child elements</td>
 * <td>XML StAX/DOM, Abacus StAX/SAX: ClassCastException; Abacus DOM: LinkedMultiValueMap[]&#123;LinkedMultiValueMap&#123;a=[1]&#125;&#125;
 * </td>
 * <td>Map[]&#123;LinkedHashMap&#123;a="1"&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>control: no override, compact</td>
 * <td>LinkedMultiValueMap&#123;a=[1]&#125;</td>
 * <td>LinkedMultiValueMap&#123;a=[1]&#125; (unchanged)</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>A per-key override of a present key that is not a List moves the map to ordinary construction and preserves the entries read before
 *   it; configuration for an absent key changes nothing; a compatible List&lt;V&gt; declaration keeps both the implementation and its element
 *   type (specializedMapsKeepTheirImplementationUnlessAPresentKeyRequiresOtherwise).</li>
 *   <li>r47 fixed the results; r48 made the ordinary fallback the same LinkedHashMap on every backend (the Abacus readers produced a
 *   HashMap).</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.namedStringOverridesReachSpecializedMapsInEveryRepresentation,
 * CodexXmlParserTest.specializedMapsKeepTheirImplementationUnlessAPresentKeyRequiresOtherwise</p>
 *
 * <h4>I04  An explicit Object override is applied literally: open scalar text, never a list</h4>
 * <p><b>Fixed in:</b> r47</p>
 * <p><b>Problem.</b> setValueType("a", Object.class) names an unconstrained slot. Under LinkedMultiValueMap metadata the readers kept the list
 * form for compact text and failed on child elements.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setValueType("a", Object.class);
 * &lt;map&gt;&lt;k type="org.springframework.util.LinkedMultiValueMap"&gt;&#123;"a":"1"&#125;&lt;/k&gt;
 *     &lt;/map&gt;              read as Map&lt;String,Map&lt;String,Object&gt;&gt;
 * &lt;map&gt;&lt;k type="org.springframework.util.LinkedMultiValueMap"&gt;&lt;map&gt;&lt;a&gt;1&lt;/a&gt;&lt;/map&gt;&lt;/k&gt;
 *     &lt;/map&gt;    same, child elements
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I04 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr><td>compact</td><td>all five: LinkedMultiValueMap&#123;a=[1]&#125;</td><td>LinkedHashMap&#123;a="1"&#125;</td></tr>
 * <tr>
 * <td>child elements</td>
 * <td>XML StAX/DOM, Abacus StAX/SAX: ClassCastException (String cannot be cast to List); Abacus DOM: LinkedMultiValueMap&#123;a=[1]&#125;</td>
 * <td>LinkedHashMap&#123;a="1"&#125;</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>An Object override reads the value as open scalar text ("1" stays a String) and cannot live in a list-valued map either; a value that
 *   carries its own map metadata adopts that metadata instead (I23).</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.namedObjectOverridesReadTheSameInCompactAndStructuredForms</p>
 *
 * <h4>I05  Nested open array components honor element configuration in compact text</h4>
 * <p><b>Fixed in:</b> r47</p>
 * <p><b>Problem.</b> With setElementType(String.class), metadata such as Long[][], ArrayList&lt;Long[]&gt; or
 * LinkedHashMap&lt;String,Long[]&gt; on a slot declared with an open nested component (Object[][], List&lt;Object[]&gt;,
 * Map&lt;String,Object[]&gt;) narrowed the declared Object[] to Long[] and ignored the configured String.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setElementType(String.class);
 * &lt;map&gt;&lt;k type="Long[][]"&gt;[[1,2]]&lt;/k&gt;&lt;/map&gt;                            read as Map&lt;String,Object[][]&gt;
 * &lt;map&gt;&lt;k type="ArrayList&lt;Long[]&gt;"&gt;[[1,2]]&lt;/k&gt;
 *     &lt;/map&gt;                   read as Map&lt;String,List&lt;Object[]&gt;&gt;
 * &lt;map&gt;&lt;k type="LinkedHashMap&lt;String,Long[]&gt;"&gt;&#123;"x":[1,2]&#125;&lt;/k&gt;
 *     &lt;/map&gt;     read as Map&lt;String,Map&lt;String,Object[]&gt;&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I05 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (all five)</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>Object[][] under Long[][]</td>
 * <td>Long[][]&#123;Long[]&#123;1L, 2L&#125;&#125;</td>
 * <td>Object[][]&#123;Object[]&#123;"1", "2"&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>List&lt;Object[]&gt; under ArrayList&lt;Long[]&gt;</td>
 * <td>ArrayList[Long[]&#123;1L, 2L&#125;]</td>
 * <td>ArrayList[Object[]&#123;"1", "2"&#125;]</td>
 * </tr>
 * <tr>
 * <td>Map&lt;String,Object[]&gt; under LinkedHashMap&lt;String,Long[]&gt;</td>
 * <td>LinkedHashMap&#123;x=Long[]&#123;1L, 2L&#125;&#125;</td>
 * <td>LinkedHashMap&#123;x=Object[]&#123;"1", "2"&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>control: Long[][] metadata, no configuration</td>
 * <td>Long[][]&#123;Long[]&#123;1L, 2L&#125;&#125;</td>
 * <td>Long[][]&#123;Long[]&#123;1L, 2L&#125;&#125; (unchanged)</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>With element configuration present, metadata may supply the container implementation but may not narrow a declared open array; the
 *   configured type reaches the innermost elements. Without configuration the metadata is the only component information and decides both the
 *   runtime class and the elements (nestedArrayMetadataDecidesWithoutElementConfiguration).</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.nestedOpenArrayComponentsHonorElementConfigurationInCompactText,
 * CodexXmlParserTest.nestedArrayMetadataDecidesWithoutElementConfiguration</p>
 *
 * <h4>I06  A map written as compact JSON text inside a list element</h4>
 * <p><b>Fixed in:</b> r47</p>
 * <p><b>Problem.</b> The standard StAX reader returned an empty map for a list element whose content is the compact JSON form of a map.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;list&gt;&lt;e&gt;&#123;"a":[1,2]&#125;&lt;/e&gt;&lt;/list&gt;      read as List&lt;Map&lt;String,List&lt;Integer&gt;&gt;&gt;
 * &lt;list&gt;&lt;e&gt;&#123;"a":1&#125;&lt;/e&gt;&lt;/list&gt;          read as List&lt;Object&gt; (control)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I06 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix</th></tr>
 * <tr>
 * <td>&lt;e&gt;&#123;"a":[1,2]&#125;&lt;/e&gt; as List&lt;Map&lt;String,List&lt;Integer&gt;&gt;&gt;</td>
 * <td>XML StAX: ArrayList[HashMap&#123;&#125;]; the other four: ArrayList[HashMap&#123;a=[1, 2]&#125;]</td>
 * <td>all five: ArrayList[HashMap&#123;a=[1, 2]&#125;]</td>
 * </tr>
 * <tr>
 * <td>control: &lt;e&gt;&#123;"a":1&#125;&lt;/e&gt; as List&lt;Object&gt;</td>
 * <td>all five: ArrayList["&#123;\"a\":1&#125;"]</td>
 * <td>unchanged: the text of an open element is a String</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The text is taken as the map only when it is the whole content of the element, the map is still empty and the text starts with
 *   '&#123;'; stray text next to child elements is ignored as before.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.compactMapTextInsideListElementsIsReadOnEveryBackend</p>
 *
 * <h4>I07  Primitive-list text (IntList, CharList, ...) reads quoted numbers and null defaults through the array codecs</h4>
 * <p><b>Fixed in:</b> r47</p>
 * <p><b>Problem.</b> Compact JSON text for a primitive list rejected quoted numbers and the literal null, which the primitive-array codecs
 * already accept (quoted content decoded once, a bare null as the element default).</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map&gt;&lt;k&gt;["1",null,2]&lt;/k&gt;&lt;/map&gt;         read as Map&lt;String,IntList&gt;
 * &lt;map&gt;&lt;k&gt;["1",null,"65"]&lt;/k&gt;&lt;/map&gt;      read as Map&lt;String,CharList&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I07 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (all five)</th><th scope="col">After fix (all five)</th></tr>
 * <tr><td>IntList</td><td>NumberFormatException: "1" is not a valid Integer.</td><td>IntList[1, 0, 2]</td></tr>
 * <tr><td>CharList</td><td>NumberFormatException: For input string: "null"</td><td>CharList['1', '\0', 'A']</td></tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The rules are the primitive-array codec rules: a quoted token is unescaped once, a bare null is the element default, and for char a
 *   multi-character region is a code point ("65" -&gt; 'A'). JsonParserImpl's own primitive-list path follows the codecs as well.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.primitiveListTextReadsQuotedNumbersAndNullDefaults</p>
 *
 * <h4>I08  A primitive array adopted from metadata ignores element configuration</h4>
 * <p><b>Fixed in:</b> r48</p>
 * <p><b>Problem.</b> An open Object slot that adopts char[] metadata is as reified as a declared char[]. The StAX readers handed a configured
 * Integer to the char[] codec: the digit 1 became the character with code 1 from text, and child elements failed.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setElementType(Integer.class);
 * &lt;map&gt;&lt;k type="char[]"&gt;[1,1]&lt;/k&gt;&lt;/map&gt;                   read as Map&lt;String,Object&gt;
 * &lt;map&gt;&lt;k type="char[]"&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;1&lt;/e&gt;&lt;/k&gt;
 *     &lt;/map&gt;       same, child elements (Abacus: &lt;value type="char[]"&gt;&lt;array&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;1&lt;/e&gt;
 *     &lt;/array&gt;&lt;/value&gt;)
 * &lt;map&gt;&lt;k type="int[]"&gt;[1,2]&lt;/k&gt;&lt;/map&gt; and &lt;map&gt;&lt;k type="boolean[]"&gt;[1,0]&lt;/k&gt;
 *     &lt;/map&gt; with setElementType(String.class)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I08 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>char[] from text, Integer configured</td>
 * <td>XML StAX, Abacus StAX: char[]&#123;code 1, code 1&#125;; XML DOM, Abacus DOM/SAX: char[]&#123;'1', '1'&#125;</td>
 * <td>char[]&#123;'1', '1'&#125;</td>
 * </tr>
 * <tr>
 * <td>char[] from child elements, Integer configured</td>
 * <td>XML StAX: ClassCastException (Integer cannot be cast to Character); the other four: char[]&#123;'1', '1'&#125;</td>
 * <td>char[]&#123;'1', '1'&#125;</td>
 * </tr>
 * <tr><td>int[] from text, String configured</td><td>int[]&#123;1, 2&#125;</td><td>int[]&#123;1, 2&#125; (unchanged)</td></tr>
 * <tr>
 * <td>boolean[] from text, String configured</td>
 * <td>boolean[]&#123;true, false&#125;</td>
 * <td>boolean[]&#123;true, false&#125; (unchanged)</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>A primitive component, declared or adopted from metadata, always keeps its codec: single digits are characters and only 1 means
 *   true.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.adoptedPrimitiveArraysIgnoreElementConfiguration</p>
 *
 * <h4>I09  A scalar per-key override on a declared list value names its element type</h4>
 * <p><b>Fixed in:</b> r48</p>
 * <p><b>Problem.</b> setValueType("k", String.class) on a key whose declared value is a List&lt;Integer&gt; names the element type (a list of
 * Strings), as it already did for child elements; the compact text form read the whole JSON array as one String. Abacus SAX ignored the
 * override for a numeric key of a list-valued map.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setValueType("k", String.class);
 * &lt;map&gt;&lt;k&gt;[1,2]&lt;/k&gt;&lt;/map&gt;                        read as Map&lt;String,List&lt;Integer&gt;&gt;
 * &lt;map&gt;&lt;k&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;/k&gt;&lt;/map&gt;             same, child elements (Abacus: &lt;value&gt;
 *     &lt;list&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;/list&gt;&lt;/value&gt;)
 * &lt;map&gt;&lt;k type="LinkedMultiValueMap&lt;Integer,Integer&gt;"&gt;&#123;"1":[1,2]&#125;&lt;/k&gt;
 *     &lt;/map&gt;   read as Map&lt;String,Map&lt;Integer,List&lt;Integer&gt;&gt;&gt; with setValueType("1", String.class)
 * &lt;map&gt;&lt;k&gt;[1,2]&lt;/k&gt;&lt;/map&gt;                        read as Map&lt;String,Object&gt; (control)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I09 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (all five)</th><th scope="col">After fix (all five)</th></tr>
 * <tr><td>compact text, declared List&lt;Integer&gt;</td><td>"[1,2]" (one String)</td><td>ArrayList["1", "2"]</td></tr>
 * <tr><td>child elements, declared List&lt;Integer&gt;</td><td>ArrayList["1", "2"]</td><td>ArrayList["1", "2"] (unchanged)</td></tr>
 * <tr>
 * <td>numeric key of a list-valued map</td>
 * <td>r46: LinkedMultiValueMap&#123;1=[1, 2]&#125; (override ignored); r47: LinkedHashMap&#123;1="[1, 2]"&#125;</td>
 * <td>LinkedMultiValueMap&#123;1=["1", "2"]&#125; (implementation kept, String elements)</td>
 * </tr>
 * <tr>
 * <td>control: declared Object</td>
 * <td>"[1,2]"</td>
 * <td>"[1,2]" (unchanged: a declared scalar or open value keeps the literal text)</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The rule needs a declared list or array value; a declared scalar or open value keeps the literal reading of its text (see I26), and a
 *   container override still names the whole value.</li>
 *   <li>A list-valued map gets its List values from the implementation, so there a scalar override names the whole value: scalar text moves
 *   the map to ordinary construction, while a JSON array or child elements read as a list of the configured type and keep the
 *   implementation.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.scalarValueOverridesNameTheElementTypeOfDeclaredListValues,
 * CodexXmlParserTest.scalarValueOverridesKeepTheirLiteralMeaningElsewhere</p>
 *
 * <h4>I10  A nested &lt;array&gt; item of a list value with the array metadata on the list, under String element configuration</h4>
 * <p><b>Fixed in:</b> r47 (StAX), r48 (DOM)</p>
 * <p><b>Problem.</b> Standard XML, non-canonical shapes: the list carries ArrayList&lt;Long[]&gt; metadata and the &lt;array&gt; item carries
 * none. With element configuration the item must stay the declared open Object[] with String elements; the standard readers narrowed it to
 * Long[] (the DOM reader until r48). The serializer's own shape puts the type on the item and was already right.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setElementType(String.class);
 * Map&lt;String, List&lt;Object[]&gt;&gt; m = parser.deserialize(xml, config, Type.of("Map&lt;String,List&lt;Object[]&gt;&gt;"));
 * &lt;map&gt;&lt;k type="ArrayList&lt;Object&gt;"&gt;&lt;array type="Object[]"&gt;[1, 2]&lt;/array&gt;&lt;/k&gt;
 *     &lt;/map&gt;   &lt;!-- canonical, written by the serializer --&gt;
 * &lt;map&gt;&lt;k type="ArrayList&lt;Long[]&gt;"&gt;&lt;array&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;/array&gt;&lt;/k&gt;
 *     &lt;/map&gt;         &lt;!-- non-canonical, &lt;e&gt; children --&gt;
 * &lt;map&gt;&lt;k type="ArrayList&lt;Long[]&gt;"&gt;&lt;array&gt;[1,2]&lt;/array&gt;&lt;/k&gt;
 *     &lt;/map&gt;                      &lt;!-- non-canonical, compact text --&gt;
 * &lt;map&gt;&lt;k type="ArrayList&lt;Long[]&gt;"&gt;[[1,2]]&lt;/k&gt;
 *     &lt;/map&gt;                                   &lt;!-- whole list as compact text --&gt;
 * (Abacus form of an item: &lt;value type="..."&gt;&lt;list&gt;&lt;e&gt;&lt;array&gt;...&lt;/array&gt;&lt;/e&gt;&lt;/list&gt;&lt;/value&gt;)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I10 on the backends</caption>
 * <tr>
 * <th scope="col">Document</th>
 * <th scope="col">XML StAX before</th>
 * <th scope="col">XML DOM before</th>
 * <th scope="col">Abacus StAX, DOM, SAX before</th>
 * <th scope="col">After fix, all five</th>
 * </tr>
 * <tr>
 * <td>canonical shape</td>
 * <td>Object[]&#123;"1", "2"&#125;</td>
 * <td>Object[]&#123;"1", "2"&#125;</td>
 * <td>Object[]&#123;"1", "2"&#125; (their own shape)</td>
 * <td>Object[]&#123;"1", "2"&#125; (unchanged)</td>
 * </tr>
 * <tr>
 * <td>&lt;array&gt; child with &lt;e&gt; children</td>
 * <td>Long[]&#123;1L, 2L&#125;</td>
 * <td>Long[]&#123;1L, 2L&#125;</td>
 * <td>Object[]&#123;"1", "2"&#125;</td>
 * <td>Object[]&#123;"1", "2"&#125;</td>
 * </tr>
 * <tr>
 * <td>&lt;array&gt; child with compact text</td>
 * <td>Long[]&#123;1L, 2L&#125;</td>
 * <td>Long[]&#123;1L, 2L&#125;</td>
 * <td>Object[]&#123;"1", "2"&#125;</td>
 * <td>Object[]&#123;"1", "2"&#125;</td>
 * </tr>
 * <tr>
 * <td>whole list as compact text [[1,2]]</td>
 * <td>Long[]&#123;1L, 2L&#125;</td>
 * <td>Long[]&#123;1L, 2L&#125;</td>
 * <td>Long[]&#123;1L, 2L&#125;</td>
 * <td>Object[]&#123;"1", "2"&#125;</td>
 * </tr>
 * <tr>
 * <td>control: &lt;e&gt; children, no configuration</td>
 * <td>Long[]&#123;1L, 2L&#125;</td>
 * <td>Long[]&#123;1L, 2L&#125;</td>
 * <td>Object[]&#123;"1", "2"&#125; (metadata dropped)</td>
 * <td>Long[]&#123;1L, 2L&#125;</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The rule is the one in force everywhere else: with element configuration present, metadata may supply the list implementation but may
 *   not narrow the declared open Object[] component, and the configured String types its elements. Without configuration the Long[] metadata
 *   decides the item class on every backend (the Abacus readers used to drop it, see I16).</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.nestedArrayItemsOfStandardListValuesHonorElementConfiguration</p>
 *
 * <h4>I11  A standard-XML map value needs its &lt;map&gt; wrapper</h4>
 * <p><b>Fixed in:</b> r48</p>
 * <p><b>Problem.</b> The standard serializer writes a nested map value inside a &lt;map&gt; element. A document without that wrapper was
 * misread: the readers took the first child as the wrapper and lost the keys, and the StAX reader returned an empty map for two children. Such
 * a value is now rejected. (Abacus XML always wraps a nested map in &lt;map&gt; inside &lt;value&gt;, so the Abacus readers are not
 * concerned.)</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map&gt;&lt;k&gt;&lt;x&gt;1&lt;/x&gt;&lt;/k&gt;&lt;/map&gt;                 read as Map&lt;String,Map&lt;String,Object&gt;&gt;
 * &lt;map&gt;&lt;k&gt;&lt;x&gt;1&lt;/x&gt;&lt;y&gt;2&lt;/y&gt;&lt;/k&gt;&lt;/map&gt;
 * &lt;map&gt;&lt;k&gt;&lt;map&gt;&lt;x&gt;1&lt;/x&gt;&lt;/map&gt;&lt;/k&gt;&lt;/map&gt;      the serializer's shape (control)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I11 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">XML StAX before</th><th scope="col">XML DOM before</th><th scope="col">After fix</th></tr>
 * <tr>
 * <td>one child without the wrapper</td>
 * <td>HashMap&#123;&#125; (keys lost)</td>
 * <td>HashMap&#123;&#125; (keys lost)</td>
 * <td>both: ParsingException: The value of k must be a &lt;map&gt; element, found &lt;x&gt;</td>
 * </tr>
 * <tr>
 * <td>two children without the wrapper</td>
 * <td>HashMap&#123;&#125;</td>
 * <td>ParsingException: Only one child element is supported</td>
 * <td>StAX: ParsingException: The value of k must be a &lt;map&gt; element, found &lt;x&gt;; DOM: ParsingException: Only one child element is
 *     supported</td>
 * </tr>
 * <tr>
 * <td>control: with the wrapper</td>
 * <td>HashMap&#123;x="1"&#125;</td>
 * <td>HashMap&#123;x="1"&#125;</td>
 * <td>HashMap&#123;x="1"&#125; (unchanged)</td>
 * </tr>
 * </table>
 * <p><b>Tests:</b> CodexXmlParserTest.standardMapValuesRequireTheMapWrapper</p>
 *
 * <h4>I12  List and array metadata naming a specialized map item applies to child elements</h4>
 * <p><b>Fixed in:</b> r48</p>
 * <p><b>Problem.</b> ArrayList&lt;LinkedMultiValueMap&lt;String,Integer&gt;&gt; or LinkedMultiValueMap[] metadata typed the items of compact
 * text but not of child elements on the Abacus readers, which constructed the declared Map and read "1" as a scalar; the array form also
 * narrowed the array class.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map&gt;&lt;k type="ArrayList&lt;LinkedMultiValueMap&lt;String,Integer&gt;&gt;"&gt;&lt;map&gt;&lt;a&gt;1&lt;/a&gt;&lt;/map&gt;&lt;/k&gt;
 *     &lt;/map&gt;   read as Map&lt;String,List&lt;Map&lt;String,Object&gt;&gt;&gt;
 * &lt;map&gt;&lt;k type="LinkedMultiValueMap[]"&gt;&lt;map&gt;&lt;a&gt;1&lt;/a&gt;&lt;/map&gt;&lt;/k&gt;
 *     &lt;/map&gt;                           read as Map&lt;String,Map&lt;String,Object&gt;[]&gt;
 * (Abacus form: &lt;value type="..."&gt;&lt;list&gt;&lt;e&gt;&lt;map&gt;&lt;entry&gt;&lt;key&gt;a&lt;/key&gt;&lt;value&gt;1&lt;/value&gt;
 *     &lt;/entry&gt;&lt;/map&gt;&lt;/e&gt;&lt;/list&gt;&lt;/value&gt;, &lt;array&gt; for the array)
 * the list document with XmlDeserConfig.create().setValueType("a", String.class)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I12 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>list, child elements</td>
 * <td>XML StAX/DOM: ArrayList[LinkedMultiValueMap&#123;a=[1]&#125;]; Abacus: ArrayList[HashMap&#123;a="1"&#125;]</td>
 * <td>ArrayList[LinkedMultiValueMap&#123;a=[1]&#125;]</td>
 * </tr>
 * <tr>
 * <td>array, child elements</td>
 * <td>r46, all five: LinkedMultiValueMap[]&#123;LinkedMultiValueMap&#123;a=[1]&#125;&#125; (array class narrowed); r47: XML StAX/DOM
 *     Map[]&#123;LinkedMultiValueMap&#123;a=[1]&#125;&#125;, Abacus Map[]&#123;HashMap&#123;a="1"&#125;&#125;</td>
 * <td>Map[]&#123;LinkedMultiValueMap&#123;a=[1]&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>list, child elements, override a=String</td>
 * <td>XML StAX/DOM: ClassCastException; Abacus: ArrayList[HashMap&#123;a="1"&#125;]</td>
 * <td>ArrayList[LinkedHashMap&#123;a="1"&#125;]</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The Abacus readers now construct list and array items from the metadata-refined element schema; a declared Map[] stays a Map[] and
 *   only its items take the metadata.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.specializedMapItemsSurviveChildElements</p>
 *
 * <h4>I13  Nested open array items keep their declared open form under element configuration (child elements)</h4>
 * <p><b>Fixed in:</b> r47 (matrix), r48 (list on DOM)</p>
 * <p><b>Problem.</b> Child-element form of I05: a nested &lt;array&gt; item of a List&lt;Object[]&gt; or Object[][] slot under
 * ArrayList&lt;Long[]&gt; or Long[][] metadata must keep its declared open Object[] form when an element type is configured, and take the
 * metadata class without configuration.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setElementType(String.class);
 * &lt;map&gt;&lt;k type="ArrayList&lt;Long[]&gt;"&gt;&lt;array&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;/array&gt;&lt;/k&gt;
 *     &lt;/map&gt;           read as Map&lt;String,List&lt;Object[]&gt;&gt;
 * &lt;map&gt;&lt;k type="Long[][]"&gt;&lt;array&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;/array&gt;&lt;/k&gt;
 *     &lt;/map&gt;                   read as Map&lt;String,Object[][]&gt;
 * (Abacus form: &lt;value type="..."&gt;&lt;list&gt;&lt;e&gt;&lt;array&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;/array&gt;&lt;/e&gt;
 *     &lt;/list&gt;&lt;/value&gt;, outer &lt;array&gt; for the matrix)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I13 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>List&lt;Object[]&gt; under ArrayList&lt;Long[]&gt;, String configured</td>
 * <td>XML StAX/DOM: ArrayList[Long[]&#123;1L, 2L&#125;]; Abacus: ArrayList[Object[]&#123;"1", "2"&#125;]</td>
 * <td>ArrayList[Object[]&#123;"1", "2"&#125;]</td>
 * </tr>
 * <tr>
 * <td>Object[][] under Long[][], String configured</td>
 * <td>all five: Long[][]&#123;Long[]&#123;1L, 2L&#125;&#125;</td>
 * <td>Object[][]&#123;Object[]&#123;"1", "2"&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>control: first document, no configuration</td>
 * <td>XML StAX/DOM: ArrayList[Long[]&#123;1L, 2L&#125;]; Abacus: ArrayList[Object[]&#123;"1", "2"&#125;] (metadata dropped)</td>
 * <td>ArrayList[Long[]&#123;1L, 2L&#125;]</td>
 * </tr>
 * </table>
 * <p><b>Tests:</b> CodexXmlParserTest.nestedOpenArrayComponentsHonorElementConfigurationInChildElements</p>
 *
 * <h4>I14  Untyped children of an open Object slot are read as the writer named them (standard XML)</h4>
 * <p><b>Fixed in:</b> r49</p>
 * <p><b>Problem.</b> Standard XML without type information: an open Object slot holding element children is read the way the writer produced
 * them. The DOM reader wrapped a single nested map in a one-element list, the StAX reader kept only the first of several children, and both
 * read a nested &lt;list&gt; or &lt;array&gt; as a map of its &lt;e&gt; elements.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * Map&lt;String, Object&gt; m = parser.deserialize(xml, XmlDeserConfig.create(), Type.of("Map&lt;String,Object&gt;"));   // value of "k" shown
 * &lt;map&gt;&lt;k&gt;&lt;map&gt;&lt;x&gt;1&lt;/x&gt;&lt;/map&gt;&lt;/k&gt;&lt;/map&gt;
 * &lt;map&gt;&lt;k&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;/k&gt;&lt;/map&gt;
 * &lt;map&gt;&lt;k&gt;&lt;list&gt;&lt;e&gt;1&lt;/e&gt;&lt;/list&gt;&lt;list&gt;&lt;e&gt;2&lt;/e&gt;&lt;/list&gt;&lt;/k&gt;&lt;/map&gt;
 * &lt;map&gt;&lt;k&gt;&lt;array&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;/array&gt;&lt;/k&gt;&lt;/map&gt;
 * &lt;map&gt;&lt;k&gt;&lt;map&gt;&lt;x&gt;1&lt;/x&gt;&lt;/map&gt;&lt;map&gt;&lt;x&gt;2&lt;/x&gt;&lt;/map&gt;&lt;/k&gt;&lt;/map&gt;
 * &lt;map&gt;&lt;k&gt;&lt;list&gt;["1", "2"]&lt;/list&gt;&lt;/k&gt;&lt;/map&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I14 on the backends</caption>
 * <tr>
 * <th scope="col">Document</th>
 * <th scope="col">XML StAX before</th>
 * <th scope="col">XML DOM before</th>
 * <th scope="col">After fix (both)</th>
 * </tr>
 * <tr>
 * <td>single &lt;map&gt; child</td>
 * <td>HashMap&#123;x="1"&#125;</td>
 * <td>ArrayList[HashMap&#123;x="1"&#125;]</td>
 * <td>HashMap&#123;x="1"&#125;</td>
 * </tr>
 * <tr><td>two &lt;e&gt; children</td><td>HashMap&#123;&#125;</td><td>ArrayList["1", "2"]</td><td>ArrayList["1", "2"]</td></tr>
 * <tr>
 * <td>two &lt;list&gt; children</td>
 * <td>HashMap&#123;e="1"&#125;</td>
 * <td>ArrayList[HashMap&#123;e="1"&#125;, HashMap&#123;e="2"&#125;]</td>
 * <td>ArrayList[ArrayList["1"], ArrayList["2"]]</td>
 * </tr>
 * <tr>
 * <td>one &lt;array&gt; child</td>
 * <td>HashMap&#123;e="2"&#125;</td>
 * <td>ArrayList[HashMap&#123;e="2"&#125;]</td>
 * <td>ArrayList[Object[]&#123;"1", "2"&#125;]</td>
 * </tr>
 * <tr>
 * <td>two &lt;map&gt; children</td>
 * <td>HashMap&#123;x="1"&#125;</td>
 * <td>ArrayList[HashMap&#123;x="1"&#125;, HashMap&#123;x="2"&#125;]</td>
 * <td>ArrayList[HashMap&#123;x="1"&#125;, HashMap&#123;x="2"&#125;]</td>
 * </tr>
 * <tr>
 * <td>one &lt;list&gt; child with compact text</td>
 * <td>HashMap&#123;&#125;</td>
 * <td>ArrayList[HashMap&#123;&#125;]</td>
 * <td>ArrayList[ArrayList["1", "2"]]</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The rule: a single child is the value (a &lt;map&gt; is the map, an &lt;e&gt; its text, a null marker null, another name a fallback
 *   map); several children, or a container child (&lt;list&gt;, &lt;set&gt;, &lt;array&gt;, &lt;collection&gt;) even when alone, form a list
 *   whose items are read by name: &lt;map&gt; a map, &lt;list&gt; a list, &lt;set&gt; a set, &lt;array&gt; an Object[], &lt;e&gt; a String.
 *   The writer emits &lt;list&gt;/&lt;array&gt; only for items, so a single &lt;list&gt; child is a list holding one list.</li>
 *   <li>Compact JSON text keeps JSON typing (1 -&gt; Integer) everywhere; the text of an &lt;e&gt; element is a String.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.untypedChildrenOfOpenObjectSlotsAreReadByName</p>
 *
 * <h4>I15  The standard writer's untyped output for nested containers in open slots reads back</h4>
 * <p><b>Fixed in:</b> r49</p>
 * <p><b>Problem.</b> The round trip of the serializer's own untyped output for a map holding nested lists and arrays failed on the standard
 * readers.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * Map&lt;String,Object&gt; source: listOfLists=[["1","2"],["3"]], listOfArrays=[Object[]&#123;"1","2"&#125;], mixed=["1", &#123;x="1"&#125;]
 * String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(false));
 * &lt;map&gt;&lt;listOfLists&gt;&lt;list&gt;[&amp;quot;1&amp;quot;, &amp;quot;2&amp;quot;]&lt;/list&gt;
 *     &lt;list&gt;[&amp;quot;3&amp;quot;]&lt;/list&gt;&lt;/listOfLists&gt;&lt;listOfArrays&gt;
 *     &lt;array&gt;[&amp;quot;1&amp;quot;, &amp;quot;2&amp;quot;]&lt;/array&gt;&lt;/listOfArrays&gt;&lt;mixed&gt;&lt;e&gt;1&lt;/e&gt;
 *     &lt;map&gt;&lt;x&gt;1&lt;/x&gt;&lt;/map&gt;&lt;/mixed&gt;&lt;/map&gt;
 * Map&lt;String,Object&gt; read = parser.deserialize(xml, XmlDeserConfig.create(), Type.of("Map&lt;String,Object&gt;"));
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I15 on the backends</caption>
 * <tr><th scope="col">Reader</th><th scope="col">Before fix</th><th scope="col">After fix</th></tr>
 * <tr>
 * <td>XML StAX</td>
 * <td>&#123;listOfArrays=&#123;&#125;, mixed=&#123;&#125;, listOfLists=&#123;&#125;&#125;</td>
 * <td>&#123;listOfArrays=[Object[]&#123;"1", "2"&#125;], mixed=["1", &#123;x="1"&#125;], listOfLists=[["1", "2"], ["3"]]&#125;</td>
 * </tr>
 * <tr>
 * <td>XML DOM</td>
 * <td>&#123;listOfArrays=[&#123;&#125;], mixed=["1", &#123;x="1"&#125;], listOfLists=[&#123;&#125;, &#123;&#125;]&#125;</td>
 * <td>the same as above</td>
 * </tr>
 * </table>
 * <p><b>Tests:</b> CodexXmlParserTest.untypedWriterOutputOfOpenSlotsRoundTrips</p>
 *
 * <h4>I16  Abacus XML: metadata on the &lt;list&gt; or &lt;array&gt; element alone types the items</h4>
 * <p><b>Fixed in:</b> r49 (array on r47/r48 partially)</p>
 * <p><b>Problem.</b> Metadata carried by the &lt;list&gt; or &lt;array&gt; element itself, without the same type on its &lt;value&gt; wrapper,
 * must type the items as wrapper metadata does. The Abacus StAX and DOM readers dropped it for a declared array and constructed the declared
 * Map items; the DOM reader even failed storing them, and the array class was narrowed. (The documents are Abacus XML; the standard readers
 * are not concerned.)</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value&gt;&lt;array type="LinkedMultiValueMap[]"&gt;&lt;e&gt;&lt;map&gt;&lt;entry&gt;
 *     &lt;key&gt;a&lt;/key&gt;&lt;value&gt;1&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;&lt;/e&gt;&lt;/array&gt;&lt;/value&gt;&lt;/entry&gt;
 *     &lt;/map&gt;
 *         read as Map&lt;String,Map&lt;String,Object&gt;[]&gt;
 * &lt;map&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value&gt;&lt;list type="ArrayList&lt;LinkedMultiValueMap&lt;String,Integer&gt;&gt;"&gt;
 *     &lt;e&gt;&lt;map&gt;...&lt;/map&gt;&lt;/e&gt;&lt;/list&gt;&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;
 *         read as Map&lt;String,List&lt;Map&lt;String,Object&gt;&gt;&gt;
 * the array document with XmlDeserConfig.create().setValueType("a", String.class)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I16 on the backends</caption>
 * <tr>
 * <th scope="col">Document</th>
 * <th scope="col">Abacus StAX before</th>
 * <th scope="col">Abacus DOM before</th>
 * <th scope="col">Abacus SAX before</th>
 * <th scope="col">After fix (all three)</th>
 * </tr>
 * <tr>
 * <td>&lt;array type="LinkedMultiValueMap[]"&gt;</td>
 * <td>LinkedMultiValueMap[]&#123;LinkedMultiValueMap&#123;a=[1]&#125;&#125; (array class narrowed)</td>
 * <td>ParsingException: Element at index 0 is a java.util.HashMap, which cannot be stored in LinkedMultiValueMap&lt;String, Object&gt;[]</td>
 * <td>LinkedMultiValueMap[]&#123;LinkedMultiValueMap&#123;a=[1]&#125;&#125;</td>
 * <td>Map[]&#123;LinkedMultiValueMap&#123;a=[1]&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>&lt;list type="ArrayList&lt;LinkedMultiValueMap&lt;String,Integer&gt;&gt;"&gt;</td>
 * <td>ArrayList[HashMap&#123;a="1"&#125;]</td>
 * <td>ArrayList[HashMap&#123;a="1"&#125;]</td>
 * <td>ArrayList[HashMap&#123;a="1"&#125;]</td>
 * <td>ArrayList[LinkedMultiValueMap&#123;a=[1]&#125;]</td>
 * </tr>
 * <tr>
 * <td>the array document + override a=String</td>
 * <td>ClassCastException (String cannot be cast to List)</td>
 * <td>ParsingException: Element at index 0 is a java.util.HashMap, which cannot be stored in ...</td>
 * <td>ClassCastException</td>
 * <td>Map[]&#123;LinkedHashMap&#123;a="1"&#125;&#125;</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>r47 removed the failures and the array narrowing, r48 fixed the SAX reader, r49 the StAX and DOM readers.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.metadataOnTheContainerElementAloneTypesTheItems</p>
 *
 * <h4>I17  With type information, typed items of an open list keep their types (standard XML)</h4>
 * <p><b>Fixed in:</b> r50</p>
 * <p><b>Problem.</b> The standard readers read every structured item of an open list (List&lt;Object&gt;, Object[], an open map value) as a
 * map: a bean item became a map of its properties and a nested list item an empty map, although the items carry their type attributes.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * Map&lt;String,Object&gt; source: beans=[Person(n,3), Person(n,3)], lists=[["1"], ["2"]], mixed=["1", Person(n,3), &#123;x="1"&#125;]
 * String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(true));   // standard XML:
 * &lt;map type="LinkedHashMap&amp;lt;Object, Object&amp;gt;"&gt;&lt;beans type="...List12&amp;lt;Object&amp;gt;"&gt;
 *     &lt;person type="...Person"&gt;&lt;name type="String"&gt;n&lt;/name&gt;&lt;age type="int"&gt;3&lt;/age&gt;
 *     &lt;/person&gt;...&lt;/beans&gt;&lt;lists type="..."&gt;&lt;list type="..."&gt;[&amp;quot;1&amp;quot;]&lt;/list&gt;
 *     &lt;list type="..."&gt;[&amp;quot;2&amp;quot;]&lt;/list&gt;&lt;/lists&gt;&lt;mixed type="..."&gt;&lt;e type="String"&gt;1&lt;/e&gt;
 *     &lt;person type="...Person"&gt;...&lt;/person&gt;&lt;map type="..."&gt;&lt;x type="String"&gt;1&lt;/x&gt;&lt;/map&gt;&lt;/mixed&gt;
 *     &lt;/map&gt;
 * Map&lt;String,Object&gt; read = parser.deserialize(xml, XmlDeserConfig.create(), Type.of("Map&lt;String,Object&gt;"));
 * Also: [Person(n,3), &#123;x="1"&#125;, ["1"], "s"] serialized with type info and read as List&lt;Object&gt;, and the same items as Object[].
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I17 on the backends</caption>
 * <tr>
 * <th scope="col">Document</th>
 * <th scope="col">XML StAX before</th>
 * <th scope="col">XML DOM before</th>
 * <th scope="col">After fix (all five)</th>
 * </tr>
 * <tr>
 * <td>map with beans/lists/mixed</td>
 * <td>beans=[map of properties x2], lists=[&#123;&#125;, &#123;&#125;], mixed=["1", map of properties, &#123;x="1"&#125;]</td>
 * <td>the same as StAX</td>
 * <td>beans=[Person(n,3), Person(n,3)], lists=[["1"], ["2"]], mixed=["1", Person(n,3), &#123;x="1"&#125;]</td>
 * </tr>
 * <tr>
 * <td>root List&lt;Object&gt;</td>
 * <td>[map of properties, &#123;x="1"&#125;, &#123;&#125;, "s"]</td>
 * <td>[Person(n,3), &#123;x="1"&#125;, ["1"], "s"] (already right)</td>
 * <td>[Person(n,3), &#123;x="1"&#125;, ["1"], "s"]</td>
 * </tr>
 * <tr>
 * <td>root Object[]</td>
 * <td>Object[]&#123;map of properties, &#123;x="1"&#125;, &#123;&#125;, "s"&#125;</td>
 * <td>Object[]&#123;Person(n,3), &#123;x="1"&#125;, ["1"], "s"&#125; (already right)</td>
 * <td>Object[]&#123;Person(n,3), &#123;x="1"&#125;, ["1"], "s"&#125;</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>A structured item of an open list is typed by its own type attribute when that names a bean, a map or a container, otherwise by its
 *   element name (I14). An uninstantiable attribute class such as java.util.ImmutableCollections.List12 is not taken as the schema; the item
 *   is read as a mutable list. The Abacus readers were already right (every item sits in an &lt;e type="..."&gt; wrapper).</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.typedItemsOfOpenListsKeepTheirTypes</p>
 *
 * <h4>I18  Without type information, a bean in an open Object slot is read as a map of its properties (Abacus XML)</h4>
 * <p><b>Fixed in:</b> r50 (standard DOM in r49)</p>
 * <p><b>Problem.</b> The Abacus readers failed with "No bean class found" on their own writer's untyped output for a bean inside an open slot;
 * the standard DOM reader wrapped the map in a one-element list.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * Map&lt;String,Object&gt; source = &#123;k=Person(n,3)&#125;;
 * String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(false));
 *    standard: &lt;map&gt;&lt;k&gt;&lt;person&gt;&lt;name&gt;n&lt;/name&gt;&lt;age&gt;3&lt;/age&gt;&lt;/person&gt;&lt;/k&gt;&lt;/map&gt;
 *    Abacus:   &lt;map&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value&gt;&lt;person&gt;&lt;name&gt;n&lt;/name&gt;&lt;age&gt;3&lt;/age&gt;
 *     &lt;/person&gt;&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;
 * Map&lt;String,Object&gt; read = parser.deserialize(xml, XmlDeserConfig.create(), Type.of("Map&lt;String,Object&gt;"));
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I18 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>&#123;k=Person&#125; untyped</td>
 * <td>Abacus StAX/DOM/SAX: ParsingException: No bean class found for node name: person in package of class: java.util.Map; XML StAX:
 *     &#123;k=&#123;name="n", age="3"&#125;&#125;; XML DOM: &#123;k=[&#123;name="n", age="3"&#125;]&#125;</td>
 * <td>&#123;k=&#123;name="n", age="3"&#125;&#125;</td>
 * </tr>
 * <tr><td>control: Person(n,3) serialized untyped, read as Person</td><td>all five: Person(n,3)</td><td>Person(n,3) (unchanged)</td></tr>
 * <tr>
 * <td>control: &lt;map&gt;&lt;k&gt;&lt;a&gt;1&lt;/a&gt;&lt;/k&gt;&lt;/map&gt; as Map&lt;String,Object&gt; (text-only unknown element)</td>
 * <td>Abacus: ParsingException: No bean class found for node name: a ...; standard: HashMap&#123;&#125; (DOM: [&#123;&#125;] until r49)</td>
 * <td>Abacus: unchanged (the element is not bean-shaped); standard: HashMap&#123;&#125; fallback map</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>A declared bean slot still constructs the bean, and a bean root under another element name still reads the requested bean. A text-only
 *   unknown element is not bean-shaped and keeps the "No bean class found" rejection on the Abacus readers.</li>
 *   <li>Property values are read as text (age="3"), as the writer wrote them without type information.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.untypedBeansInOpenSlotsReadAsMaps</p>
 *
 * <h4>I19  With type information, the typed wrapper child of an Object property is the value</h4>
 * <p><b>Fixed in:</b> r51 (map child in r49)</p>
 * <p><b>Problem.</b> The standard writer keeps a bean property's declared type on the property element and records the value's runtime class
 * on a wrapper child (&lt;values type="Object"&gt;&lt;person type="..."&gt;). The standard readers read a bean child as a map of its
 * properties and a list or array child as an empty map, later as a list inside a list.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * ObjectBean bean = new ObjectBean(); bean.values = &lt;value below&gt;;
 * String xml = parser.serialize(bean, XmlSerConfig.create().setWriteTypeInfo(true));   // standard XML:
 * &lt;objectBean type="...ObjectBean"&gt;&lt;values type="Object"&gt;&lt;person type="...Person"&gt;&lt;name type="String"&gt;n&lt;/name&gt;
 *     &lt;age type="int"&gt;3&lt;/age&gt;&lt;/person&gt;&lt;/values&gt;&lt;/objectBean&gt;
 * &lt;objectBean type="...ObjectBean"&gt;&lt;values type="Object"&gt;
 *     &lt;list type="ArrayList&amp;lt;Object&amp;gt;"&gt;[&amp;quot;1&amp;quot;, 2]&lt;/list&gt;&lt;/values&gt;&lt;/objectBean&gt;
 * &lt;objectBean type="...ObjectBean"&gt;&lt;values type="Object"&gt;&lt;map type="LinkedHashMap&amp;lt;Object, Object&amp;gt;"&gt;
 *     &lt;x type="String"&gt;1&lt;/x&gt;&lt;/map&gt;&lt;/values&gt;&lt;/objectBean&gt;
 * &lt;objectBean type="...ObjectBean"&gt;&lt;values type="Object"&gt;&lt;array type="Object[]"&gt;[&amp;quot;1&amp;quot;, 2]&lt;/array&gt;
 *     &lt;/values&gt;&lt;/objectBean&gt;
 * ObjectBean read = parser.deserialize(xml, XmlDeserConfig.create(), Type.of(ObjectBean.class));   // read.values shown
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I19 on the backends</caption>
 * <tr>
 * <th scope="col">values</th>
 * <th scope="col">XML StAX before</th>
 * <th scope="col">XML DOM before</th>
 * <th scope="col">After fix (all five)</th>
 * </tr>
 * <tr>
 * <td>Person(n,3)</td>
 * <td>HashMap&#123;name="n", age=3&#125;</td>
 * <td>r46: ArrayList[HashMap&#123;name="n", age=3&#125;]; r49: HashMap&#123;name="n", age=3&#125;</td>
 * <td>Person(n,3)</td>
 * </tr>
 * <tr>
 * <td>ArrayList["1", 2]</td>
 * <td>r46: HashMap&#123;&#125;; r49: ArrayList[ArrayList["1", 2]]</td>
 * <td>r46: ArrayList[HashMap&#123;&#125;]; r49: ArrayList[ArrayList["1", 2]]</td>
 * <td>ArrayList["1", 2]</td>
 * </tr>
 * <tr>
 * <td>LinkedHashMap&#123;x="1"&#125;</td>
 * <td>LinkedHashMap&#123;x="1"&#125; (already right)</td>
 * <td>r46: ArrayList[LinkedHashMap&#123;x="1"&#125;]; r49: LinkedHashMap&#123;x="1"&#125;</td>
 * <td>LinkedHashMap&#123;x="1"&#125;</td>
 * </tr>
 * <tr>
 * <td>Object[]&#123;"1", 2&#125;</td>
 * <td>r46: HashMap&#123;&#125;; r49: ArrayList[Object[]&#123;"1", 2&#125;]</td>
 * <td>r46: ArrayList[HashMap&#123;&#125;]; r49: ArrayList[Object[]&#123;"1", 2&#125;]</td>
 * <td>Object[]&#123;"1", 2&#125;</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>A single child WITH a type attribute is the value itself: a bean from the attribute, a container by its name with the attribute
 *   applied. The Abacus readers were already right.</li>
 *   <li>Writer limitation, not fixed: an Optional&lt;Object&gt; bean property is written as compact JSON text even with type information
 *   (&lt;jdk&gt;&#123;...&#125;&lt;/jdk&gt;), and the readers hand back a String. Map values wrap Optionals properly.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.typedWrapperChildOfAnObjectPropertyIsTheValue</p>
 *
 * <h4>I20  An empty element is the empty string; a null marker is null</h4>
 * <p><b>Fixed in:</b> r51</p>
 * <p><b>Problem.</b> Both writers emit &lt;k&gt;&lt;/k&gt; for an empty String and &lt;k isNull="true"/&gt; for null. The standard StAX reader
 * read the empty element as the type's default, null for a String, so the empty string did not survive a round trip on that reader.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map&gt;&lt;k&gt;&lt;/k&gt;&lt;/map&gt;                 read as Map&lt;String,String&gt; and as Map&lt;String,Object&gt;
 * &lt;map&gt;&lt;k isNull="true"/&gt;&lt;/map&gt;     read as Map&lt;String,String&gt;
 * (Abacus form: &lt;value&gt;&lt;/value&gt; and &lt;value isNull="true"/&gt;)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I20 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr><td>&lt;k&gt;&lt;/k&gt; as Map&lt;String,String&gt;</td><td>XML StAX: null; the other four: ""</td><td>""</td></tr>
 * <tr><td>&lt;k&gt;&lt;/k&gt; as Map&lt;String,Object&gt;</td><td>XML StAX: null; the other four: ""</td><td>""</td></tr>
 * <tr><td>&lt;k isNull="true"/&gt;</td><td>all five: null</td><td>null (unchanged)</td></tr>
 * </table>
 * <p><b>Tests:</b> CodexXmlParserTest.emptyElementsReadAsEmptyStrings</p>
 *
 * <h4>I21  Without type information, bean-shaped elements inside arrays and nested values are read as maps (Abacus XML)</h4>
 * <p><b>Fixed in:</b> r50 (nested value), r51 (arrays)</p>
 * <p><b>Problem.</b> Follow-up of I18 for a bean inside an untyped &lt;array&gt; and inside a map value nested in a typed list: the Abacus
 * readers failed with "No bean class found" in both places (the untyped array assumed String items; the SAX handler judged the slot by the
 * enclosing list's element type).</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &#123;items=[&#123;k=Person(n,3)&#125;]&#125; serialized without type info, read as Map&lt;String,List&lt;Map&lt;String,Object&gt;&gt;&gt;:
 *    &lt;map&gt;&lt;entry&gt;&lt;key&gt;items&lt;/key&gt;&lt;value&gt;&lt;list&gt;&lt;e&gt;&lt;map&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;
 *     &lt;value&gt;&lt;person&gt;&lt;name&gt;n&lt;/name&gt;&lt;age&gt;3&lt;/age&gt;&lt;/person&gt;&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;
 *     &lt;/e&gt;&lt;/list&gt;&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;
 * &#123;people=Person[]&#123;Person(n,3)&#125;, mixed=Object[]&#123;Person(n,3), "s"&#125;&#125; serialized without type info, read as Map&lt;String,Object&gt;:
 *    &lt;map&gt;&lt;entry&gt;&lt;key&gt;people&lt;/key&gt;&lt;value&gt;&lt;array&gt;&lt;e&gt;&lt;person&gt;...&lt;/person&gt;&lt;/e&gt;
 *     &lt;/array&gt;&lt;/value&gt;&lt;/entry&gt;&lt;entry&gt;&lt;key&gt;mixed&lt;/key&gt;&lt;value&gt;&lt;array&gt;&lt;e&gt;
 *     &lt;person&gt;...&lt;/person&gt;&lt;/e&gt;&lt;e&gt;s&lt;/e&gt;&lt;/array&gt;&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I21 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Abacus StAX/DOM/SAX before</th><th scope="col">After fix (Abacus StAX/DOM/SAX)</th></tr>
 * <tr>
 * <td>bean in a map value nested in a typed list</td>
 * <td>ParsingException: No bean class found for node name: person in package of class: java.util.Map</td>
 * <td>&#123;items=[&#123;k=&#123;name="n", age="3"&#125;&#125;]&#125;</td>
 * </tr>
 * <tr>
 * <td>beans inside untyped &lt;array&gt; values</td>
 * <td>ParsingException: No bean class found for node name: person in package of class: java.util.Map</td>
 * <td>&#123;people=LinkedHashMap[]&#123;&#123;name="n", age="3"&#125;&#125;, mixed=Object[]&#123;&#123;name="n", age="3"&#125;,
 *     "s"&#125;&#125;</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Untyped &lt;array&gt; items are read as Object, and the array class is inferred from the items (a LinkedHashMap[] when every item is a
 *   map, an Object[] for mixed items). The standard writer has no wrapper for a one-element array, so the array shapes are Abacus only.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.untypedBeansInsideArraysAndNestedValuesReadAsMaps</p>
 *
 * <h4>I22  Per-key value types reach the compact text of a root array or list</h4>
 * <p><b>Fixed in:</b> r52</p>
 * <p><b>Problem.</b> setValueType("a", String.class) applies to the key "a" inside maps at any nesting level, in a map value and in JSON, but
 * the root array and list readers handed the JSON reader a configuration holding the element type only.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setValueType("a", String.class);
 * &lt;array&gt;[&#123;"a":1&#125;]&lt;/array&gt;      read as Object[] and as Map&lt;String,Object&gt;[]
 * &lt;list&gt;[&#123;"a":1&#125;]&lt;/list&gt;        read as List&lt;Object&gt; and as List&lt;Map&lt;String,Object&gt;&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I22 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (all five)</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>&lt;array&gt; as Object[]</td>
 * <td>Object[]&#123;HashMap&#123;a=1&#125;&#125; (override ignored)</td>
 * <td>Object[]&#123;HashMap&#123;a="1"&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>&lt;array&gt; as Map&lt;String,Object&gt;[]</td>
 * <td>Map[]&#123;HashMap&#123;a=1&#125;&#125;</td>
 * <td>Map[]&#123;HashMap&#123;a="1"&#125;&#125;</td>
 * </tr>
 * <tr><td>&lt;list&gt; as List&lt;Object&gt;</td><td>ArrayList[HashMap&#123;a=1&#125;]</td><td>ArrayList[HashMap&#123;a="1"&#125;]</td></tr>
 * <tr>
 * <td>&lt;list&gt; as List&lt;Map&lt;String,Object&gt;&gt;</td>
 * <td>ArrayList[HashMap&#123;a=1&#125;]</td>
 * <td>ArrayList[HashMap&#123;a="1"&#125;]</td>
 * </tr>
 * </table>
 * <p><b>Tests:</b> CodexXmlParserTest.perKeyValueTypesReachCompactTextOfRootArraysAndLists</p>
 *
 * <h4>I23  An explicit Object override on a value that carries map metadata adopts the metadata</h4>
 * <p><b>Fixed in:</b> r52</p>
 * <p><b>Problem.</b> setValueType("k", Object.class) on a value whose element carries LinkedMultiValueMap&lt;String,Integer&gt; metadata must
 * adopt that metadata, as any open slot does; the standard DOM reader skipped the metadata for an overridden slot and read the text
 * literally.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setValueType("k", Object.class);
 * &lt;map&gt;&lt;k type="LinkedMultiValueMap&lt;String,Integer&gt;"&gt;&#123;"x":[1],"y":[2]&#125;&lt;/k&gt;
 *     &lt;/map&gt;        read as Map&lt;String,Object&gt;
 * &lt;map&gt;&lt;k type="LinkedMultiValueMap&lt;String,Integer&gt;"&gt;&lt;map&gt;&lt;x&gt;1&lt;/x&gt;&lt;/map&gt;&lt;/k&gt;
 *     &lt;/map&gt;        same, child elements
 * &lt;map&gt;&lt;k&gt;&#123;"x":[1]&#125;&lt;/k&gt;&lt;/map&gt;                                                        no metadata (control)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I23 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>compact text with metadata</td>
 * <td>XML DOM: "&#123;\"x\":[1],\"y\":[2]&#125;" (a String); the other four: LinkedMultiValueMap&#123;x=[1], y=[2]&#125;</td>
 * <td>LinkedMultiValueMap&#123;x=[1], y=[2]&#125;</td>
 * </tr>
 * <tr>
 * <td>child elements with metadata</td>
 * <td>XML DOM: r46 ArrayList[HashMap&#123;x="1"&#125;], r49 HashMap&#123;x="1"&#125;; the other four: LinkedMultiValueMap&#123;x=[1]&#125;
 * </td>
 * <td>LinkedMultiValueMap&#123;x=[1]&#125;</td>
 * </tr>
 * <tr>
 * <td>control: no metadata</td>
 * <td>all five: "&#123;\"x\":[1]&#125;"</td>
 * <td>"&#123;\"x\":[1]&#125;" (unchanged: the text is read literally)</td>
 * </tr>
 * </table>
 * <p><b>Tests:</b> CodexXmlParserTest.objectOverrideOnAMetadataSlotAdoptsTheMetadata</p>
 *
 * <h4>I24  Per-key value types reach the compact text of an array value with an open component</h4>
 * <p><b>Fixed in:</b> r52</p>
 * <p><b>Problem.</b> The shared array-text reader handed an open Object component to the array type's own codec, which never sees the
 * configuration, so a per-key override did not reach the maps inside an Object[] value (it already reached a List&lt;Object&gt; value since
 * r47).</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setValueType("a", String.class);
 * &lt;map&gt;&lt;k&gt;[&#123;"a":1&#125;]&lt;/k&gt;
 *     &lt;/map&gt;      read as Map&lt;String,Object[]&gt; and as Map&lt;String,List&lt;Object&gt;&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I24 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (all five)</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>Map&lt;String,Object[]&gt;</td>
 * <td>Object[]&#123;HashMap&#123;a=1&#125;&#125;</td>
 * <td>Object[]&#123;HashMap&#123;a="1"&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>Map&lt;String,List&lt;Object&gt;&gt;</td>
 * <td>r46: ArrayList[HashMap&#123;a=1&#125;]; r47: ArrayList[HashMap&#123;a="1"&#125;]</td>
 * <td>ArrayList[HashMap&#123;a="1"&#125;]</td>
 * </tr>
 * </table>
 * <p><b>Tests:</b> CodexXmlParserTest.perKeyValueTypesReachCompactTextOfArrayValues</p>
 *
 * <h4>I25  The global map value type fills the open values of maps inside compact text at any nesting level</h4>
 * <p><b>Fixed in:</b> r52</p>
 * <p><b>Problem.</b> setMapValueType(Long.class) fills the open values of every map inside child elements, but the compact JSON reader applied
 * it to the root map and to typed map slots only, as the ordinary JSON reader does, so a map inside an open element of compact text kept JSON
 * typing.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setMapValueType(Long.class);
 * &lt;listBean&gt;&lt;values&gt;[&#123;"x":1&#125;]&lt;/values&gt;&lt;/listBean&gt;          read as ListBean  (values: List&lt;Object&gt;)
 * &lt;arrayBean&gt;&lt;values&gt;[&#123;"x":1&#125;]&lt;/values&gt;&lt;/arrayBean&gt;        read as ArrayBean (values: Object[])
 * &lt;listBean&gt;&lt;values&gt;[&#123;"x":[&#123;"y":1&#125;]&#125;]&lt;/values&gt;&lt;/listBean&gt;  a value that cannot be a Long
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I25 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (all five)</th><th scope="col">After fix (all five)</th></tr>
 * <tr><td>ListBean</td><td>values=[&#123;x=1 (Integer)&#125;]</td><td>values=[&#123;x=1L&#125;]</td></tr>
 * <tr><td>ArrayBean</td><td>values=Object[]&#123;&#123;x=1 (Integer)&#125;&#125;</td><td>values=Object[]&#123;&#123;x=1L&#125;&#125;</td></tr>
 * <tr>
 * <td>a list value under the Long configuration</td>
 * <td>values=[&#123;x=[&#123;y=1&#125;]&#125;]</td>
 * <td>NumberFormatException: [&#123;"y": 1&#125;] is not a valid Long.</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The ordinary JSON reader applies a global map value type to the root map and to typed map slots and leaves a map inside an element
 *   slot alone; the XML readers apply it to every open map value, in compact text as in child elements. A value that cannot be converted
 *   fails, as the JSON reader fails for a root map with the same text and configuration.</li>
 *   <li>At the ROOT an explicit global map key or value type overrides the requested type's own arguments (tested contract, see I29); that is
 *   why these samples put the maps below the root.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.globalMapValueTypeReachesMapsInOpenSlotsOfCompactText</p>
 *
 * <h4>I26  A compact map entry keeps its declared scalar type when it meets a JSON array (as JSON does)</h4>
 * <p><b>Fixed in:</b> r53</p>
 * <p><b>Problem.</b> The compact reader turned every scalar value type that met a JSON array into a list: &#123;"a":[1,2]&#125; into
 * Map&lt;String,String&gt; gave a List&lt;String&gt;, and into Map&lt;String,Integer&gt; a List&lt;Integer&gt;, where the JSON reader gives
 * the literal text and rejects. The rule introduced in r48 for I09 had been applied to declared scalar and open values as well.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map&gt;&lt;k&gt;&#123;"a":[1,2]&#125;&lt;/k&gt;
 *     &lt;/map&gt;   read as Map&lt;String,Map&lt;String,String&gt;&gt;, Map&lt;String,Map&lt;String,Integer&gt;&gt;, and
 *                                   as Map&lt;String,Map&lt;String,Object&gt;&gt; with XmlDeserConfig.create().setValueType("a", String.class)
 * control: as Map&lt;String,Map&lt;String,List&lt;Integer&gt;&gt;&gt; with the same override (the intended rule of I09)
 * JSON for comparison: N.fromJson("&#123;\"a\":[1,2]&#125;", Type.of("Map&lt;String,String&gt;")) -&gt; &#123;a="[1, 2]"&#125;; Map&lt;String,Integer&gt; -&gt; NumberFormatException
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I26 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (all five)</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>Map&lt;String,String&gt;</td>
 * <td>r46: &#123;a="[1, 2]"&#125;; r48 to r52: &#123;a=["1", "2"]&#125;</td>
 * <td>&#123;a="[1, 2]"&#125; (the literal text, as JSON)</td>
 * </tr>
 * <tr>
 * <td>Map&lt;String,Integer&gt;</td>
 * <td>r46: NumberFormatException; r48 to r52: &#123;a=[1, 2]&#125;</td>
 * <td>NumberFormatException: [1, 2] is not a valid Integer. (as JSON)</td>
 * </tr>
 * <tr>
 * <td>Map&lt;String,Object&gt; + override a=String</td>
 * <td>r46: &#123;a=[1, 2]&#125;; r47: &#123;a="[1, 2]"&#125;; r48 to r52: &#123;a=["1", "2"]&#125;</td>
 * <td>&#123;a="[1, 2]"&#125;</td>
 * </tr>
 * <tr>
 * <td>control: Map&lt;String,List&lt;Integer&gt;&gt; + override a=String</td>
 * <td>r48 to r52: &#123;a=["1", "2"]&#125;</td>
 * <td>&#123;a=["1", "2"]&#125; (unchanged)</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Only a scalar override on a DECLARED list or array value names the element type; a declared scalar or open value keeps the configured
 *   type and reads its text literally.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.compactScalarValuesKeepTheirDeclaredTypes</p>
 *
 * <h4>I27  A root array keeps the requested array class under metadata; a per-key override falls back to an ordinary map</h4>
 * <p><b>Fixed in:</b> r53</p>
 * <p><b>Problem.</b> A root &lt;array type="LinkedMultiValueMap[]"&gt; read as Map&lt;String,Object&gt;[] constructed the metadata's narrower
 * array on three readers, which then threw an array store error once a per-key override turned the items into ordinary maps; Abacus DOM
 * ignored the metadata.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;array type="org.springframework.util.LinkedMultiValueMap[]"&gt;[&#123;"a":1&#125;]&lt;/array&gt;                read as Map&lt;String,Object&gt;[]
 * the same with XmlDeserConfig.create().setValueType("a", String.class)
 * &lt;array type="org.springframework.util.LinkedMultiValueMap[]"&gt;&lt;map&gt;&lt;a&gt;1&lt;/a&gt;&lt;/map&gt;
 *     &lt;/array&gt;       child elements + the override
 * (Abacus form of the item: &lt;e&gt;&lt;map&gt;&lt;entry&gt;&lt;key&gt;a&lt;/key&gt;&lt;value&gt;1&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;
 *     &lt;/e&gt;)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I27 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>compact, no override</td>
 * <td>XML StAX/DOM, Abacus StAX: LinkedMultiValueMap[]&#123;LinkedMultiValueMap&#123;a=[1]&#125;&#125; (array class narrowed); Abacus DOM:
 *     Map[]&#123;HashMap&#123;a=1&#125;&#125; (metadata ignored); Abacus SAX: Map[]&#123;LinkedMultiValueMap&#123;a=[1]&#125;&#125;</td>
 * <td>Map[]&#123;LinkedMultiValueMap&#123;a=[1]&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>compact + override a=String</td>
 * <td>XML StAX/DOM, Abacus StAX: ParsingException: Element at index 0 is a java.util.LinkedHashMap, which cannot be stored in
 *     LinkedMultiValueMap&lt;String, Object&gt;[]; Abacus DOM: Map[]&#123;HashMap&#123;a="1"&#125;&#125;; Abacus SAX:
 *     Map[]&#123;LinkedHashMap&#123;a="1"&#125;&#125;</td>
 * <td>Map[]&#123;LinkedHashMap&#123;a="1"&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>child elements + override a=String</td>
 * <td>the same pattern as the compact form</td>
 * <td>Map[]&#123;LinkedHashMap&#123;a="1"&#125;&#125;</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Metadata never narrows a declared reference array (Number[], Map&lt;String,Object&gt;[]): the array keeps its class and only its items
 *   take the metadata, at the root as in a property.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.rootArraysKeepTheRequestedClassUnderMetadata</p>
 *
 * <h4>I28  A present key whose override is a scalar type moves a list-valued map to ordinary construction even for a null value</h4>
 * <p><b>Fixed in:</b> r53</p>
 * <p><b>Problem.</b> The guard that moves a LinkedMultiValueMap to an ordinary map on the first non-List value looked at non-null values only,
 * so &#123;"a":null&#125; with a String override on "a" kept the list-valued implementation.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setValueType("a", String.class);
 * &lt;map&gt;&lt;k type="org.springframework.util.LinkedMultiValueMap"&gt;&#123;"a":null&#125;&lt;/k&gt;
 *     &lt;/map&gt;                      read as Map&lt;String,Object&gt;
 * &lt;map&gt;&lt;k type="org.springframework.util.LinkedMultiValueMap"&gt;&lt;map&gt;&lt;a isNull="true"/&gt;&lt;/map&gt;&lt;/k&gt;
 *     &lt;/map&gt;   same, child elements
 * the compact document without the override (control)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I28 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (all five)</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>compact &#123;"a":null&#125; + override</td>
 * <td>LinkedMultiValueMap&#123;a=null&#125;</td>
 * <td>LinkedHashMap&#123;a=null&#125;</td>
 * </tr>
 * <tr><td>child element with isNull + override</td><td>LinkedMultiValueMap&#123;a=null&#125;</td><td>LinkedHashMap&#123;a=null&#125;</td></tr>
 * <tr>
 * <td>control: no override</td>
 * <td>LinkedMultiValueMap&#123;a=null&#125;</td>
 * <td>LinkedMultiValueMap&#123;a=null&#125; (unchanged)</td>
 * </tr>
 * </table>
 * <p><b>Tests:</b> CodexXmlParserTest.presentNullWithScalarOverrideMovesToOrdinaryMap</p>
 *
 * <h4>I29  In a declared map slot: declared argument &gt; global map key/value type &gt; metadata argument</h4>
 * <p><b>Fixed in:</b> r53</p>
 * <p><b>Problem.</b> A slot declared Map&lt;String,Object&gt; (here pinned with setValueType("k", Map&lt;String,Object&gt;)) with
 * LinkedMultiValueMap&lt;String,Integer&gt; metadata and a global setMapValueType(String): the metadata's list shape was decided before the
 * global type was consulted (all five for compact text, the standard readers for child elements), while the Abacus readers already let the
 * global type win for child elements.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setMapValueType(String.class).setValueType("k", Type.of("Map&lt;String,Object&gt;"));
 * &lt;map&gt;&lt;k type="LinkedMultiValueMap&lt;String,Integer&gt;"&gt;&#123;"a":"1"&#125;&lt;/k&gt;
 *     &lt;/map&gt;            read as Map&lt;String,Map&lt;String,Object&gt;&gt;
 * &lt;map&gt;&lt;k type="LinkedMultiValueMap&lt;String,Integer&gt;"&gt;&lt;map&gt;&lt;a&gt;1&lt;/a&gt;&lt;/map&gt;&lt;/k&gt;
 *     &lt;/map&gt;  same, child elements
 * &lt;map&gt;&lt;k type="LinkedHashMap&lt;String,Integer&gt;"&gt;&lt;map&gt;&lt;a&gt;1&lt;/a&gt;&lt;/map&gt;&lt;/k&gt;
 *     &lt;/map&gt;        ordinary-map metadata
 * the first document with setMapValueType(Type.of("List&lt;String&gt;")) instead of String
 * the first document with the pin only (control)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I29 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>list-valued metadata, compact, global String</td>
 * <td>all five: LinkedMultiValueMap&#123;a=[1]&#125;</td>
 * <td>HashMap&#123;a="1"&#125; (ordinary construction)</td>
 * </tr>
 * <tr>
 * <td>list-valued metadata, child elements, global String</td>
 * <td>XML StAX/DOM: LinkedMultiValueMap&#123;a=[1]&#125;; Abacus: LinkedHashMap&#123;a="1"&#125; (r46: ClassCastException)</td>
 * <td>LinkedHashMap&#123;a="1"&#125;</td>
 * </tr>
 * <tr>
 * <td>ordinary metadata LinkedHashMap&lt;String,Integer&gt;, child elements, global String</td>
 * <td>XML StAX/DOM: LinkedHashMap&#123;a=1 (Integer)&#125;; Abacus: LinkedHashMap&#123;a="1"&#125;</td>
 * <td>LinkedHashMap&#123;a="1"&#125;</td>
 * </tr>
 * <tr>
 * <td>list-valued metadata, compact, global List&lt;String&gt;</td>
 * <td>all five: LinkedMultiValueMap&#123;a=[1 (Integer)]&#125;</td>
 * <td>LinkedMultiValueMap&#123;a=["1"]&#125;</td>
 * </tr>
 * <tr>
 * <td>control: the pin only</td>
 * <td>LinkedMultiValueMap&#123;a=[1]&#125;</td>
 * <td>LinkedMultiValueMap&#123;a=[1]&#125; (unchanged: without a global type the metadata applies)</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The precedence is now the same on every backend and for both forms: a declared key or value type wins over the global map key and
 *   value types, which win over the arguments the slot's metadata supplies. The metadata's implementation still constructs the map where it
 *   can hold the configured values; a list-valued implementation that cannot falls back to ordinary construction.</li>
 *   <li>The root is the exception: at the root an explicit global map key or value type overrides the requested type's own arguments (tested
 *   contract of XmlParserImplTest/AbacusXmlParserImplTest testRootGenericTypesPreserveExplicitConfigAndNestedValues), which is why the sample
 *   pins the slot with a per-key type.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.globalScalarValueTypeBeatsSpecializedMetadata</p>
 *
 * <h4>I30  A root map written as compact JSON text is read by every backend, with the root's key and value types</h4>
 * <p><b>Fixed in:</b> r53</p>
 * <p><b>Problem.</b> &lt;map&gt;&#123;...&#125;&lt;/map&gt; at the root was read only by the standard StAX reader, and that reader used the
 * requested type's arguments instead of the explicit global key and value types the child-element form honors; the other four backends
 * returned an empty map.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map&gt;&#123;"a":1&#125;&lt;/map&gt;                        read as Map&lt;String,Integer&gt;; also with &amp;#x2003; padding and as &lt;map&gt;
 *     &lt;![CDATA[&#123;"a":1&#125;]]&gt;&lt;/map&gt;
 * &lt;map&gt;&#123;"1":1&#125;&lt;/map&gt;                        read as Map&lt;Integer,Integer&gt; with setMapKeyType(Long.class).setMapValueType(String.class)
 * &lt;map&gt;&lt;e name="1"&gt;1&lt;/e&gt;
 *     &lt;/map&gt;             the child-element form with the same configuration (control; Abacus: &lt;entry&gt;&lt;key&gt;1&lt;/key&gt;
 *     &lt;value&gt;1&lt;/value&gt;&lt;/entry&gt;)
 * &lt;map&gt; &lt;/map&gt;                              blank (control)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I30 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>&#123;"a":1&#125;, padded, CDATA</td>
 * <td>XML StAX: &#123;a=1&#125;; the other four: &#123;&#125; (empty)</td>
 * <td>&#123;a=1&#125;</td>
 * </tr>
 * <tr>
 * <td>&#123;"1":1&#125; + global key Long, value String</td>
 * <td>XML StAX: &#123;1 (Integer)=1 (Integer)&#125; (requested arguments); the other four: &#123;&#125;</td>
 * <td>&#123;1L="1"&#125;</td>
 * </tr>
 * <tr>
 * <td>control: child element + the same configuration</td>
 * <td>all five: &#123;1L="1"&#125;</td>
 * <td>&#123;1L="1"&#125; (unchanged)</td>
 * </tr>
 * <tr><td>control: blank</td><td>all five: &#123;&#125;</td><td>&#123;&#125; (unchanged)</td></tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The compact form now follows the root contract exactly as the child-element form: an explicit global map key or value type takes
 *   precedence over the requested type's own arguments at the root; below the root the declared arguments keep their precedence.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.compactRootMapsReadWithTheRootTypesOnEveryBackend</p>
 *
 * <h4>I31  The configured component types the innermost open elements of nested containers at parse time</h4>
 * <p><b>Fixed in:</b> r53</p>
 * <p><b>Problem.</b> A configured element type reached the innermost open elements of nested compact text only after the JSON reader had typed
 * them: a List&lt;Long&gt; component left a List&lt;Integer&gt; two levels down, and a String component received the parsed numbers (1.2300
 * became "1.23", 1e3 became "1000.0") instead of the token text that a one-level slot keeps.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map&gt;&lt;k&gt;[[[1,2]]]&lt;/k&gt;
 *     &lt;/map&gt;                read as Map&lt;String,Object[][]&gt; and Map&lt;String,List&lt;Object[]&gt;&gt; with setElementType(Type.of("List&lt;Long&gt;"))
 * &lt;map&gt;&lt;k&gt;[[1.2300,1e3]]&lt;/k&gt;
 *     &lt;/map&gt;           read as Map&lt;String,Object[][]&gt; with setElementType(String.class), with and without type="Long[][]"
 * &lt;map&gt;&lt;k&gt;[1.2300,1e3]&lt;/k&gt;
 *     &lt;/map&gt;             read as Map&lt;String,Object[]&gt; with setElementType(String.class) (one level, control)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I31 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (all five)</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>[[[1,2]]] as Object[][] + List&lt;Long&gt;</td>
 * <td>Object[][]&#123;Object[]&#123;ArrayList[1, 2] (Integer)&#125;&#125;</td>
 * <td>Object[][]&#123;Object[]&#123;ArrayList[1L, 2L]&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>[[[1,2]]] as List&lt;Object[]&gt; + List&lt;Long&gt;</td>
 * <td>ArrayList[Object[]&#123;ArrayList[1, 2] (Integer)&#125;]</td>
 * <td>ArrayList[Object[]&#123;ArrayList[1L, 2L]&#125;]</td>
 * </tr>
 * <tr>
 * <td>[[1.2300,1e3]] as Object[][] + String</td>
 * <td>r46: Object[][]&#123;Object[]&#123;1.23, 1000.0 (Double)&#125;&#125;; r47 to r52: Object[][]&#123;Object[]&#123;"1.23",
 *     "1000.0"&#125;&#125;</td>
 * <td>Object[][]&#123;Object[]&#123;"1.2300", "1e3"&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>the same under Long[][] metadata</td>
 * <td>r46: NumberFormatException: Invalid numeric String: "1e3"; r47 to r52: Object[][]&#123;Object[]&#123;"1.23", "1000.0"&#125;&#125;</td>
 * <td>Object[][]&#123;Object[]&#123;"1.2300", "1e3"&#125;&#125;</td>
 * </tr>
 * <tr><td>control: one level</td><td>Object[]&#123;"1.2300", "1e3"&#125;</td><td>Object[]&#123;"1.2300", "1e3"&#125; (unchanged)</td></tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>JSON itself types the root container only: N.fromJson("[[[1,2]]]",
 *   JsonDeserConfig.create().setElementType(Type.of("List&lt;Long&gt;")), Object[][].class) keeps the inner List&lt;Integer&gt;. In XML
 *   compact text the configured component reaches every nesting level, as it does for nested child elements.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.configuredComponentsReachNestedOpenElementsAtParseTime</p>
 *
 * <h4>I32  A blank root array element is an empty array of the requested class</h4>
 * <p><b>Fixed in:</b> r53</p>
 * <p><b>Problem.</b> The standard DOM reader returned null for a blank root &lt;array&gt;&lt;/array&gt; read without element configuration
 * (and would have done so with configuration after the I27 change); the other four backends return an empty array.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;array&gt;&lt;/array&gt;        read as Object[] and as Long[]
 * &lt;array&gt; &lt;/array&gt;       read as Object[] with setElementType(Long.class)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I32 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>&lt;array&gt;&lt;/array&gt; as Object[]</td>
 * <td>XML DOM: null; the other four: Object[]&#123;&#125;</td>
 * <td>Object[]&#123;&#125;</td>
 * </tr>
 * <tr>
 * <td>&lt;array&gt; &lt;/array&gt; as Object[] + Long configured</td>
 * <td>all five: Object[]&#123;&#125;</td>
 * <td>Object[]&#123;&#125; (unchanged; the configuration never makes it a Long[])</td>
 * </tr>
 * <tr><td>&lt;array&gt;&lt;/array&gt; as Long[]</td><td>all five: Long[]&#123;&#125;</td><td>Long[]&#123;&#125; (unchanged)</td></tr>
 * </table>
 * <p><b>Tests:</b> CodexXmlParserTest.blankRootArraysAreEmptyArraysOnEveryBackend</p>
 *
 * <h4>I33  A configured container component types the slot's open elements once, never its own elements</h4>
 * <p><b>Fixed in:</b> r56</p>
 * <p><b>Problem.</b> A configured container component ({@code List<Object>}, {@code Object[]}) was applied again inside its own instances: the
 * r53 compact reader re-applied it at every nested level, and the child-element readers (long before r53) typed the elements of a configured
 * list item a second time. {@code [[1,2],[3]]} read as {@code Object[]} with a {@code List<Object>} component came back as {@code
 * [[[1],[2]],[[3]]]} on the standard readers, in bean properties and map values on all five, and a configured {@code Object[]} component left
 * an {@code ArrayList} at the innermost level of an {@code Object[][]}.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig lists = XmlDeserConfig.create().setElementType(Type.of("List&lt;Object&gt;"));
 * XmlDeserConfig arrays = XmlDeserConfig.create().setElementType(Type.of("Object[]"));
 * &lt;array&gt;[[1,2],[3]]&lt;/array&gt;                 read as Object[], Set&lt;Object&gt;, List&lt;List&lt;Object&gt;&gt; (as &lt;list&gt;) with lists; as Object[] with arrays
 * &lt;arrayBean&gt;&lt;values&gt;[[1,2],[3]]&lt;/values&gt;&lt;/arrayBean&gt;   and &lt;listBean&gt;&lt;values&gt;...&lt;/values&gt;
 *     &lt;/listBean&gt;, with lists
 * &lt;map&gt;&lt;k&gt;[[1,2],[3]]&lt;/k&gt;&lt;/map&gt;              read as Map&lt;String,Object[]&gt; with lists
 * &lt;map&gt;&lt;k&gt;[[[1,2]]]&lt;/k&gt;&lt;/map&gt;                read as Map&lt;String,Object[][]&gt; with lists, and with arrays
 * &lt;map&gt;&lt;k&gt;&lt;list&gt;[1,2]&lt;/list&gt;&lt;list&gt;[3]&lt;/list&gt;&lt;/k&gt;
 *     &lt;/map&gt;   child elements, read as Map&lt;String,Object[]&gt; with lists (standard XML)
 * &lt;map&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value&gt;&lt;array&gt;&lt;e&gt;&lt;list&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;
 *     &lt;/list&gt;&lt;/e&gt;&lt;/array&gt;&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;   the Abacus form
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I33 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>root Object[] with List&lt;Object&gt;</td>
 * <td>XML StAX/DOM: Object[]&#123;[[1], [2]], [[3]]&#125;; Abacus: Object[]&#123;[1, 2], [3]&#125; (its root array text never applied the
 *     component, see I36)</td>
 * <td>Object[]&#123;[1, 2], [3]&#125;</td>
 * </tr>
 * <tr>
 * <td>root Set&lt;Object&gt;, List&lt;List&lt;Object&gt;&gt; with List&lt;Object&gt;</td>
 * <td>all five: [[1, 2], [3]] (unchanged)</td>
 * <td>[[1, 2], [3]]</td>
 * </tr>
 * <tr>
 * <td>root Object[] with Object[]</td>
 * <td>XML StAX/DOM: Object[]&#123;Object[]&#123;Object[]&#123;1&#125;, Object[]&#123;2&#125;&#125;,
 *     Object[]&#123;Object[]&#123;3&#125;&#125;&#125;; Abacus: Object[]&#123;Object[]&#123;1, 2&#125;, Object[]&#123;3&#125;&#125;</td>
 * <td>Object[]&#123;Object[]&#123;1, 2&#125;, Object[]&#123;3&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>ArrayBean.values / ListBean.values with List&lt;Object&gt;</td>
 * <td>all five: [[[1], [2]], [[3]]] inside the array or list</td>
 * <td>Object[]&#123;[1, 2], [3]&#125; / [[1, 2], [3]]</td>
 * </tr>
 * <tr>
 * <td>Map&lt;String,Object[]&gt; value with List&lt;Object&gt;</td>
 * <td>all five: Object[]&#123;[[1], [2]], [[3]]&#125;</td>
 * <td>Object[]&#123;[1, 2], [3]&#125;</td>
 * </tr>
 * <tr>
 * <td>Map&lt;String,Object[][]&gt; value [[[1,2]]] with List&lt;Object&gt;</td>
 * <td>all five: Object[][]&#123;Object[]&#123;[[1], [2]]&#125;&#125;</td>
 * <td>Object[][]&#123;Object[]&#123;[1, 2]&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>Map&lt;String,Object[][]&gt; value [[[1,2]]] with Object[]</td>
 * <td>all five: Object[][]&#123;Object[]&#123;ArrayList[1, 2]&#125;&#125;</td>
 * <td>Object[][]&#123;Object[]&#123;Object[]&#123;1, 2&#125;&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>child elements, standard (&lt;list&gt; items with compact text)</td>
 * <td>XML StAX/DOM: Object[]&#123;[[1], [2]], [[3]]&#125; (since r46)</td>
 * <td>Object[]&#123;[1, 2], [3]&#125;</td>
 * </tr>
 * <tr>
 * <td>child elements, Abacus (&lt;e&gt;&lt;list&gt;&lt;e&gt;1&lt;/e&gt;..)</td>
 * <td>Abacus: Object[]&#123;[[[1]], [[2]]]&#125; (since r46)</td>
 * <td>Object[]&#123;["1", "2"]&#125; (element text is String)</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The compact reader now counts the nesting depth of bracketed values and applies the component only at the depth of the slot's own open
 *   level (arrays at any depth, a collection's direct element, a map's array or collection value; a collection nested in a collection keeps
 *   its own element). The child-element readers read an item that is a configured container component with a copy of the configuration without
 *   the element type, so nothing inside it is typed again; a configured BEAN component keeps typing the open collections nested in the bean
 *   (AbacusXmlParserImplTest testDeserialize_configuredBeanElementTypeStillTypesNestedElements).</li>
 *   <li>A root that is itself the component ({@code <list>[1,2]</list>} read as List&lt;Object&gt; with a List&lt;Object&gt; component) still
 *   types its elements literally: [[1], [2]], as the JSON reader does for the same text and configuration.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.configuredContainerComponentsApplyOnceAtTheirOpenLevel</p>
 *
 * <h4>I34  A scalar per-key override on a declared array or Set value keeps the declared container shape in compact text</h4>
 * <p><b>Fixed in:</b> r56</p>
 * <p><b>Problem.</b> The compact reader named a List for every scalar override of a declared container value: an {@code Integer[]} value with
 * a String override became an {@code ArrayList} stored in the array-typed slot, and a {@code Set<Integer>} value an {@code ArrayList} with
 * duplicates, while child elements produced a {@code String[]} (on three readers, see I35) and a Set.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig override = XmlDeserConfig.create().setValueType("a", String.class);
 * &lt;map&gt;&lt;k&gt;&#123;"a":[1,2]&#125;&lt;/k&gt;
 *     &lt;/map&gt;        read as Map&lt;String,Map&lt;String,Integer[]&gt;&gt;, Map&lt;String,Map&lt;String,List&lt;Integer&gt;&gt;&gt;
 * &lt;map&gt;&lt;k&gt;&#123;"a":[1,1,2]&#125;&lt;/k&gt;&lt;/map&gt;      read as Map&lt;String,Map&lt;String,Set&lt;Integer&gt;&gt;&gt;
 * &lt;map&gt;&lt;a&gt;[1,2]&lt;/a&gt;&lt;/map&gt;              read as Map&lt;String,Integer[]&gt; (root map, compact value)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I34 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (all five)</th><th scope="col">After fix (all five)</th></tr>
 * <tr><td>Integer[] value, compact</td><td>ArrayList["1", "2"] in an Integer[] slot</td><td>String[]&#123;"1", "2"&#125;</td></tr>
 * <tr><td>Set&lt;Integer&gt; value, compact</td><td>ArrayList["1", "1", "2"]</td><td>Set&#123;"1", "2"&#125;</td></tr>
 * <tr><td>List&lt;Integer&gt; value, compact (control)</td><td>ArrayList["1", "2"]</td><td>ArrayList["1", "2"] (unchanged)</td></tr>
 * <tr>
 * <td>root map, Integer[] value, compact</td>
 * <td>XML StAX/DOM, Abacus StAX: String[]&#123;"1", "2"&#125;; Abacus DOM/SAX: Integer[]&#123;1, 2&#125;</td>
 * <td>String[]&#123;"1", "2"&#125;</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The compact hook mirrors the child-element rule (elementOverride): the override names the element type of the declared list, set or
 *   array, which keeps its own class. A list-valued map (MultiValueMap) still reads a JSON array under a scalar override as a List of that
 *   type and keeps the implementation (I09).</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.scalarValueOverridesKeepTheDeclaredContainerShapeInCompactText</p>
 *
 * <h4>I35  A per-key array type names a map entry's array on the Abacus DOM and SAX readers</h4>
 * <p><b>Fixed in:</b> r56</p>
 * <p><b>Problem.</b> The Abacus DOM and SAX readers passed the map's declared value type as the entry's declaration when they kept the
 * declared array class, so a key configured as {@code String[]} or {@code Long[]} (or a scalar override that names it) still produced the
 * map's {@code Integer[]}, for child elements and for compact text. Abacus StAX and the standard readers honored the override.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map&gt;&lt;a&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;/a&gt;
 *     &lt;/map&gt;   read as Map&lt;String,Integer[]&gt; with setValueType("a", Type.of("String[]")), Type.of("Long[]"), String.class
 * &lt;map&gt;&lt;a&gt;[1,2]&lt;/a&gt;&lt;/map&gt;              compact, with setValueType("a", Type.of("String[]"))
 * (Abacus form: &lt;map&gt;&lt;entry&gt;&lt;key&gt;a&lt;/key&gt;&lt;value&gt;&lt;array&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;
 *     &lt;/array&gt;&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I35 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>child elements, String[] override</td>
 * <td>Abacus DOM/SAX: Integer[]&#123;1, 2&#125;; the other three: String[]&#123;"1", "2"&#125;</td>
 * <td>String[]&#123;"1", "2"&#125;</td>
 * </tr>
 * <tr>
 * <td>child elements, Long[] override</td>
 * <td>Abacus DOM/SAX: Integer[]&#123;1, 2&#125;; the other three: Long[]&#123;1L, 2L&#125;</td>
 * <td>Long[]&#123;1L, 2L&#125;</td>
 * </tr>
 * <tr>
 * <td>child elements, String override</td>
 * <td>Abacus DOM/SAX: Integer[]&#123;1, 2&#125;; the other three: String[]&#123;"1", "2"&#125;</td>
 * <td>String[]&#123;"1", "2"&#125;</td>
 * </tr>
 * <tr>
 * <td>compact text, String[] override</td>
 * <td>Abacus DOM/SAX: Integer[]&#123;1, 2&#125;; the other three: String[]&#123;"1", "2"&#125;</td>
 * <td>String[]&#123;"1", "2"&#125;</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The configured type is the entry's declaration: openArrayDestination keeps the configured array class, not the map's.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.namedArrayOverridesApplyOnEveryBackend</p>
 *
 * <h4>I36  A root array or list whose requested element is a container keeps it, and the configured component reaches its open elements</h4>
 * <p><b>Fixed in:</b> r56</p>
 * <p><b>Problem.</b> The Abacus root array text set the element type to the declared {@code Object[]} and never applied the configured
 * component ({@code [[1,2],[3]]} read as {@code Object[][]} with a Long component gave Integers on the three Abacus readers), and every
 * backend replaced the requested {@code Object[]} element of a root {@code List<Object[]>} with the configured Long by the root contract, so
 * the nested arrays failed to convert.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig longs = XmlDeserConfig.create().setElementType(Long.class);
 * &lt;array&gt;[[1,2],[3]]&lt;/array&gt;     read as Object[][]
 * &lt;list&gt;[[1,2],[3]]&lt;/list&gt;       read as List&lt;Object[]&gt;, and &lt;list&gt;[[1,2]]&lt;/list&gt; as List&lt;List&lt;Integer&gt;&gt; (control)
 * &lt;array&gt;&lt;array&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;/array&gt;
 *     &lt;/array&gt;   child elements, read as Object[][] (Abacus: &lt;array&gt;&lt;e&gt;&lt;array&gt;&lt;e&gt;1&lt;/e&gt;...&lt;/array&gt;
 *     &lt;/e&gt;&lt;/array&gt;)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I36 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>root Object[][] compact</td>
 * <td>XML StAX/DOM: Object[][]&#123;Object[]&#123;1L, 2L&#125;, Object[]&#123;3L&#125;&#125;; Abacus: Object[][]&#123;Object[]&#123;1,
 *     2&#125;, Object[]&#123;3&#125;&#125; (Integer)</td>
 * <td>Object[][]&#123;Object[]&#123;1L, 2L&#125;, Object[]&#123;3L&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>root List&lt;Object[]&gt; compact</td>
 * <td>all five: NumberFormatException: [1, 2] is not a valid Long.</td>
 * <td>[Object[]&#123;1L, 2L&#125;, Object[]&#123;3L&#125;]</td>
 * </tr>
 * <tr>
 * <td>root Object[][] child elements</td>
 * <td>all five: Object[][]&#123;Object[]&#123;1L, 2L&#125;&#125; (unchanged)</td>
 * <td>Object[][]&#123;Object[]&#123;1L, 2L&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>control: List&lt;List&lt;Integer&gt;&gt; + Long</td>
 * <td>all five: NumberFormatException</td>
 * <td>[[1, 2]] (a requested element that is not open keeps its arguments)</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The Abacus root array text now goes through the shared array reader with the slot's component override, as the standard readers do,
 *   and all root collection text goes through a shared collection reader that types the open components nested in the element.</li>
 *   <li>A bean property declared {@code Object[]} with a Long component and the text {@code [[1,2],[3]]} still fails: its elements are the
 *   nested arrays, which the configuration says are Longs, as the JSON reader fails for the same text and configuration.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.configuredComponentsReachRootNestedOpenArraysOnEveryBackend</p>
 *
 * <h4>I37  At the root, a global map key or value type never replaces a requested container argument</h4>
 * <p><b>Fixed in:</b> r56</p>
 * <p><b>Problem.</b> By the root contract an explicit global map value type replaced any requested value argument, including a container:
 * {@code Map<String,Object[]>} with {@code setMapValueType(String.class)} read the whole array text as one String, {@code
 * setMapValueType(Long.class)} failed to convert it, and a requested {@code List<Integer>} value failed the same way. A requested container
 * argument now keeps its shape and the global type applies inside it, as it does below the root; a requested scalar, bean or open argument
 * still yields to the global type.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map&gt;&lt;k&gt;[&#123;"a":[1,2]&#125;]&lt;/k&gt;&lt;/map&gt;   read as Map&lt;String,Object[]&gt; with setMapValueType(String.class)
 * &lt;map&gt;&lt;k&gt;[&#123;"a":1&#125;]&lt;/k&gt;&lt;/map&gt;       read as Map&lt;String,Object[]&gt; with setMapValueType(Long.class)
 * &lt;map&gt;&lt;k&gt;[1,2]&lt;/k&gt;&lt;/map&gt;           read as Map&lt;String,List&lt;Integer&gt;&gt; with setMapValueType(Long.class)
 * &lt;map&gt;&lt;k&gt;1&lt;/k&gt;
 *     &lt;/map&gt;               read as Map&lt;String,Integer&gt; with setMapValueType(Long.class) (the tested root contract, unchanged)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I37 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (all five)</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>Map&lt;String,Object[]&gt; + global String</td>
 * <td>&#123;k="[&#123;\"a\":[1,2]&#125;]"&#125; (one String; the array is never built)</td>
 * <td>&#123;k=Object[]&#123;&#123;a="[1, 2]"&#125;&#125;&#125; (the global String reaches the open values of the map inside)</td>
 * </tr>
 * <tr>
 * <td>Map&lt;String,Object[]&gt; + global Long</td>
 * <td>NumberFormatException: [&#123;"a":1&#125;] is not a valid Long.</td>
 * <td>&#123;k=Object[]&#123;&#123;a=1L&#125;&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>Map&lt;String,List&lt;Integer&gt;&gt; + global Long</td>
 * <td>NumberFormatException: [1,2] is not a valid Long.</td>
 * <td>&#123;k=[1, 2]&#125; (the requested List&lt;Integer&gt; keeps its element)</td>
 * </tr>
 * <tr><td>Map&lt;String,Integer&gt; + global Long</td><td>&#123;k=1L&#125;</td><td>&#123;k=1L&#125; (unchanged)</td></tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The same carve-out applies to a root list's requested container element (I36). The tested contract for scalar arguments
 *   (XmlParserImplTest / AbacusXmlParserImplTest testRootGenericTypesPreserveExplicitConfigAndNestedValues) is unchanged.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.globalMapTypesKeepARequestedContainerArgumentAtTheRoot</p>
 *
 * <h4>I38  A present null under any non-List override moves a list-valued map to ordinary construction</h4>
 * <p><b>Fixed in:</b> r56</p>
 * <p><b>Problem.</b> The guard that moves a list-valued map (Spring MultiValueMap) to an ordinary map reacted to a scalar override only when
 * the key's value was null: an explicit Object, an array or a Set override kept the implementation, contrary to the documented rule that any
 * override of a present key that cannot hold a List does.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map&gt;&lt;k type="LinkedMultiValueMap"&gt;&#123;"a":null&#125;&lt;/k&gt;
 *     &lt;/map&gt;   read as Map&lt;String,Object&gt; with setValueType("a", Object.class), Type.of("String[]"), Type.of("Set&lt;String&gt;"), Type.of("List&lt;String&gt;")
 * &lt;map&gt;&lt;k type="LinkedMultiValueMap"&gt;&lt;map&gt;&lt;a isNull="true"/&gt;&lt;/map&gt;&lt;/k&gt;
 *     &lt;/map&gt;   child elements, with the Object override
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I38 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (all five)</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>Object, String[] or Set&lt;String&gt; override</td>
 * <td>LinkedMultiValueMap&#123;a=null&#125;</td>
 * <td>LinkedHashMap&#123;a=null&#125;</td>
 * </tr>
 * <tr>
 * <td>List&lt;String&gt; override</td>
 * <td>LinkedMultiValueMap&#123;a=null&#125;</td>
 * <td>LinkedMultiValueMap&#123;a=null&#125; (unchanged: a List override keeps the implementation)</td>
 * </tr>
 * <tr>
 * <td>child element with isNull, Object override</td>
 * <td>LinkedMultiValueMap&#123;a=null&#125;</td>
 * <td>LinkedHashMap&#123;a=null&#125;</td>
 * </tr>
 * </table>
 * <p><b>Tests:</b> CodexXmlParserTest.presentNullWithAnyNonListOverrideMovesToOrdinaryMap</p>
 *
 * <h4>I39  A global scalar map value type on a root map whose value holds a structured child</h4>
 * <p><b>Fixed in:</b> r56, by I37</p>
 * <p><b>Problem.</b> Formerly limitation L06: with {@code setMapValueType(String.class)} the root contract made the value slot of a root
 * {@code Map<String,Map<String,Object>>} String-typed, so a structured child was read by the untyped rules of each writer: a one-element list
 * on the standard readers, a map on the Abacus readers, and the compact form was a String. The requested {@code Map<String,Object>} value is a
 * container and is now kept (I37), with the global String filling its open values.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setMapValueType(String.class);
 * &lt;map&gt;&lt;k type="LinkedMultiValueMap&lt;String,Integer&gt;"&gt;&lt;map&gt;&lt;a&gt;1&lt;/a&gt;&lt;/map&gt;&lt;/k&gt;
 *     &lt;/map&gt;   read as Map&lt;String,Map&lt;String,Object&gt;&gt;
 * &lt;map&gt;&lt;k&gt;&lt;map&gt;&lt;a&gt;1&lt;/a&gt;&lt;/map&gt;&lt;/k&gt;
 *     &lt;/map&gt;                                              same without metadata
 * &lt;map&gt;&lt;k type="LinkedMultiValueMap&lt;String,Integer&gt;"&gt;&#123;"a":"1"&#125;&lt;/k&gt;&lt;/map&gt;             compact text
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I39 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>child elements, with metadata</td>
 * <td>XML StAX/DOM: ArrayList[HashMap&#123;a="1"&#125;]; Abacus: LinkedHashMap&#123;a="1"&#125;</td>
 * <td>LinkedHashMap&#123;a="1"&#125;</td>
 * </tr>
 * <tr>
 * <td>child elements, no metadata</td>
 * <td>XML StAX/DOM: ArrayList[HashMap&#123;a="1"&#125;]; Abacus: LinkedHashMap&#123;a="1"&#125;</td>
 * <td>LinkedHashMap&#123;a="1"&#125;</td>
 * </tr>
 * <tr><td>compact text</td><td>all five: "&#123;\"a\":\"1\"&#125;" (a String)</td><td>HashMap&#123;a="1"&#125;</td></tr>
 * </table>
 * <p><b>Tests:</b> CodexXmlParserTest.globalMapTypesKeepARequestedContainerArgumentAtTheRoot</p>
 *
 * <h4>I40  A declared Number[] value whose metadata names Long[] holds Long elements on every backend</h4>
 * <p><b>Fixed in:</b> r56, found by the differential</p>
 * <p><b>Problem.</b> Not reported, found by the probe differential of the third round: a map value declared {@code Number[]} with {@code
 * type="Long[]"} metadata kept JSON-typed Integers in compact text on every backend, and its child-element form failed on three readers
 * because {@code Number} has no text conversion. The metadata-refined component now types the items, as the shared array reader documents, and
 * the declared {@code Number[]} is still the array that is constructed.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map&gt;&lt;k type="Long[]"&gt;[1,2]&lt;/k&gt;&lt;/map&gt;                   read as Map&lt;String,Number[]&gt;
 * &lt;map&gt;&lt;k type="Long[]"&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;/k&gt;
 *     &lt;/map&gt;       same, child elements (Abacus: &lt;value type="Long[]"&gt;&lt;array&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;
 *     &lt;/array&gt;&lt;/value&gt;)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I40 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix</th><th scope="col">After fix (all five)</th></tr>
 * <tr><td>compact text</td><td>all five: Number[]&#123;1 (Integer), 2 (Integer)&#125;</td><td>Number[]&#123;1L, 2L&#125;</td></tr>
 * <tr>
 * <td>child elements</td>
 * <td>XML StAX/DOM, Abacus SAX: UnsupportedOperationException: No public static method or constructor found in java.lang.Number to create a
 *     value from String; Abacus StAX/DOM: Number[]&#123;1L, 2L&#125;</td>
 * <td>Number[]&#123;1L, 2L&#125;</td>
 * </tr>
 * </table>
 * <p><b>Tests:</b> CodexXmlParserTest.metadataRefinesTheElementsOfAnAbstractReferenceArrayValue</p>
 *
 * <h4>I41  A declared container element keeps the element configuration inside it, on every backend</h4>
 * <p><b>Fixed in:</b> r57</p>
 * <p><b>Problem.</b> A structured item that is the declared container element of its slot ({@code Object[]} in {@code Object[][]} or {@code
 * List<Object[]>}) was treated as a configured item whenever it equalled the configured component: the round-8 rule that a configured
 * component is applied once (I33) then cleared the configuration one level too early, and a configured {@code Object[]} component left an
 * {@code ArrayList} at the innermost level of the item's compact text, on every backend. (Reported as a regression: at r53 the Abacus readers
 * read these documents correctly and the standard readers over-nested them.) Each reader now decides per array or collection whether its items
 * are configured components, and only when the slot's declared element is open; a declared container element is read as that element with the
 * configuration applied inside it, for compact item text and child elements alike.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig arrays = XmlDeserConfig.create().setElementType(Type.of("Object[]"));
 * &lt;array&gt;&lt;array&gt;[[1,2]]&lt;/array&gt;&lt;/array&gt;              read as Object[][] (Abacus: &lt;array&gt;&lt;e&gt;
 *     &lt;array&gt;[[1,2]]&lt;/array&gt;&lt;/e&gt;&lt;/array&gt;)
 * &lt;map&gt;&lt;k&gt;&lt;array&gt;[[1,2]]&lt;/array&gt;&lt;/k&gt;
 *     &lt;/map&gt;           read as Map&lt;String,Object[][]&gt; (Abacus: &lt;value&gt;&lt;array&gt;&lt;e&gt;&lt;array&gt;..&lt;/array&gt;
 *     &lt;/e&gt;&lt;/array&gt;&lt;/value&gt;)
 * &lt;list&gt;&lt;array&gt;[[1,2]]&lt;/array&gt;&lt;/list&gt;                read as List&lt;Object[]&gt; (Abacus: &lt;list&gt;&lt;e&gt;
 *     &lt;array&gt;[[1,2]]&lt;/array&gt;&lt;/e&gt;&lt;/list&gt;)
 * &lt;map&gt;&lt;k&gt;&lt;array&gt;[[1,2]]&lt;/array&gt;&lt;/k&gt;
 *     &lt;/map&gt;           read as Map&lt;String,List&lt;Object[]&gt;&gt; (Abacus: &lt;value&gt;&lt;list&gt;&lt;e&gt;
 *     &lt;array&gt;..&lt;/array&gt;&lt;/e&gt;&lt;/list&gt;&lt;/value&gt;)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I41 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (r56)</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>root Object[][]</td>
 * <td>all five: Object[][]&#123;Object[]&#123;ArrayList[1, 2]&#125;&#125; (r53: Abacus Object[][]&#123;Object[]&#123;Object[]&#123;1,
 *     2&#125;&#125;&#125;, XML StAX/DOM Object[][]&#123;Object[]&#123;Object[]&#123;Object[]&#123;1&#125;,
 *     Object[]&#123;2&#125;&#125;&#125;&#125;)</td>
 * <td>Object[][]&#123;Object[]&#123;Object[]&#123;1, 2&#125;&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>map value Object[][]</td>
 * <td>all five: Object[][]&#123;Object[]&#123;ArrayList[1, 2]&#125;&#125;</td>
 * <td>Object[][]&#123;Object[]&#123;Object[]&#123;1, 2&#125;&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>root List&lt;Object[]&gt;</td>
 * <td>all five: ArrayList[Object[]&#123;ArrayList[1, 2]&#125;]</td>
 * <td>ArrayList[Object[]&#123;Object[]&#123;1, 2&#125;&#125;]</td>
 * </tr>
 * <tr>
 * <td>map value List&lt;Object[]&gt;</td>
 * <td>all five: ArrayList[Object[]&#123;ArrayList[1, 2]&#125;]</td>
 * <td>ArrayList[Object[]&#123;Object[]&#123;1, 2&#125;&#125;]</td>
 * </tr>
 * <tr>
 * <td>control: root Object[] holding &lt;array&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;/array&gt; (the item is the configured
 *     component)</td>
 * <td>all five: Object[]&#123;Object[]&#123;"1", "2"&#125;&#125;</td>
 * <td>unchanged: Object[]&#123;Object[]&#123;"1", "2"&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>control: compact root &lt;array&gt;[[[1,2]]]&lt;/array&gt; as Object[][]</td>
 * <td>all five: Object[][]&#123;Object[]&#123;Object[]&#123;1, 2&#125;&#125;&#125;</td>
 * <td>unchanged</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The StAX and DOM readers pass the slot's declared schema to itemConfig; the SAX reader decides at each array or collection start
 *   (configuredItemsQueue). The Abacus DOM collection case reuses its item variable per child, so it captures the declared slot before the
 *   loop (the map value List&lt;Object[]&gt; row needed that).</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.declaredContainerItemsKeepTheConfiguredComponentInside</p>
 *
 * <h4>I42  The standard readers read the structured items of a nested array slot with the slot's declared element</h4>
 * <p><b>Fixed in:</b> r57</p>
 * <p><b>Problem.</b> In a map value or bean property declared {@code Object[][]} or {@code List<Object[]>}, the standard StAX and DOM readers
 * took the configured component ({@code List<Object>}) as the type of the structured {@code <array>} items instead of the slot's declared
 * element, and then failed to store the ArrayList in the {@code Object[][]}, or lost the declared {@code Object[]} item of the list; the
 * Abacus readers (arrayItemType) already read the declared element with the configuration applied inside each item. The standard readers now
 * derive the item type the same way (containerItemType): the declared element of an array slot, or of a collection whose declared element is a
 * container, and the configured component otherwise.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig lists = XmlDeserConfig.create().setElementType(Type.of("List&lt;Object&gt;"));
 * &lt;map&gt;&lt;k&gt;&lt;array&gt;[[1,2]]&lt;/array&gt;&lt;/k&gt;
 *     &lt;/map&gt;      read as Map&lt;String,Object[][]&gt; (Abacus: &lt;value&gt;&lt;array&gt;&lt;e&gt;&lt;array&gt;[[1,2]]&lt;/array&gt;
 *     &lt;/e&gt;&lt;/array&gt;&lt;/value&gt;)
 * &lt;map&gt;&lt;k&gt;&lt;array&gt;[[1,2]]&lt;/array&gt;&lt;/k&gt;
 *     &lt;/map&gt;      read as Map&lt;String,List&lt;Object[]&gt;&gt; (Abacus: &lt;value&gt;&lt;list&gt;&lt;e&gt;
 *     &lt;array&gt;..&lt;/array&gt;&lt;/e&gt;&lt;/list&gt;&lt;/value&gt;)
 * &lt;map&gt;&lt;k&gt;&lt;array&gt;[1,2]&lt;/array&gt;&lt;/k&gt;
 *     &lt;/map&gt;        read as Map&lt;String,Object[][]&gt; with setElementType(Long.class), and with &lt;e&gt;1&lt;/e&gt;
 *     &lt;e&gt;2&lt;/e&gt; items
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I42 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (r56)</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>Map&lt;String,Object[][]&gt; + List&lt;Object&gt;</td>
 * <td>XML StAX/DOM: ParsingException: Element at index 0 is a java.util.ArrayList, which cannot be stored in Object[][] (expected Object[]);
 *     Abacus StAX/DOM/SAX: Object[][]&#123;Object[]&#123;ArrayList[1, 2]&#125;&#125;</td>
 * <td>Object[][]&#123;Object[]&#123;ArrayList[1, 2]&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>Map&lt;String,List&lt;Object[]&gt;&gt; + List&lt;Object&gt;</td>
 * <td>XML StAX/DOM: ArrayList[ArrayList[ArrayList[1, 2]]] (the declared Object[] item lost); Abacus: ArrayList[Object[]&#123;ArrayList[1,
 *     2]&#125;]</td>
 * <td>ArrayList[Object[]&#123;ArrayList[1, 2]&#125;]</td>
 * </tr>
 * <tr>
 * <td>Map&lt;String,Object[][]&gt; + Long, [1,2] item</td>
 * <td>all five: Object[][]&#123;Object[]&#123;1L, 2L&#125;&#125;</td>
 * <td>unchanged</td>
 * </tr>
 * <tr>
 * <td>Map&lt;String,Object[][]&gt; + Long, &lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt; item</td>
 * <td>all five: Object[][]&#123;Object[]&#123;1L, 2L&#125;&#125;</td>
 * <td>unchanged</td>
 * </tr>
 * </table>
 * <p><b>Tests:</b> CodexXmlParserTest.standardReadersKeepTheDeclaredItemOfANestedArraySlot</p>
 *
 * <h4>I43  The global map value type reaches the maps nested in compact text</h4>
 * <p><b>Fixed in:</b> r57</p>
 * <p><b>Problem.</b> The XmlDeserConfig contract says the global map key and value types fill a map's open arguments at any nesting level,
 * inside compact text as inside child elements, and the child-element readers do; the compact reader lost them when it entered a nested map:
 * JsonParserImpl.readBracedValueBody replaced the configuration's key and value types with the nested map's own arguments ({@code Object}),
 * and the compact root map overwrote the configured types with the schema's arguments before reading. {@code {"k":{"a":1}}} read as {@code
 * Map<String,Map<String,Object>>} with setMapValueType(Long) gave an Integer, with and without a per-key type for k, on every backend, while
 * the child-element form gave a Long. The compact reader now keeps the global type for an open nested argument
 * (CompactJsonParser.nestedMapType, a hook whose JSON default is unchanged) and reads the root map with its arguments as a typed schema.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig longs = XmlDeserConfig.create().setMapValueType(Long.class);
 * &lt;map&gt;&#123;"k":&#123;"a":1&#125;&#125;&lt;/map&gt;                     read as Map&lt;String,Map&lt;String,Object&gt;&gt;
 * &lt;map&gt;&#123;"k":&#123;"a":1&#125;&#125;&lt;/map&gt;                     same, with setValueType("k", Type.of("Map&lt;String,Object&gt;"))
 * &lt;map&gt;&lt;k&gt;&lt;map&gt;&lt;a&gt;1&lt;/a&gt;&lt;/map&gt;&lt;/k&gt;&lt;/map&gt;          child elements (Abacus: &lt;value&gt;
 *     &lt;map&gt;&lt;entry&gt;&lt;key&gt;a&lt;/key&gt;&lt;value&gt;1&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;&lt;/value&gt;)
 * &lt;map&gt;&lt;k&gt;&#123;"a":1&#125;&lt;/k&gt;&lt;/map&gt;                     compact value
 * &lt;map&gt;&#123;"k":&#123;"a":&#123;"b":1&#125;&#125;&#125;&lt;/map&gt;               two levels: the inner value is a map
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I43 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (r56)</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>compact root</td>
 * <td>all five: HashMap&#123;k=HashMap&#123;a=1 (Integer)&#125;&#125;</td>
 * <td>HashMap&#123;k=HashMap&#123;a=1L&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>compact root + per-key Map&lt;String,Object&gt; for k</td>
 * <td>all five: HashMap&#123;k=HashMap&#123;a=1 (Integer)&#125;&#125;</td>
 * <td>HashMap&#123;k=HashMap&#123;a=1L&#125;&#125;</td>
 * </tr>
 * <tr><td>child elements</td><td>all five: HashMap&#123;k=HashMap&#123;a=1L&#125;&#125;</td><td>unchanged</td></tr>
 * <tr><td>compact value</td><td>all five: HashMap&#123;k=HashMap&#123;a=1L&#125;&#125;</td><td>unchanged</td></tr>
 * <tr>
 * <td>two levels, the inner value a map</td>
 * <td>all five: HashMap&#123;k=HashMap&#123;a=HashMap&#123;b=1 (Integer)&#125;&#125;&#125; (the configuration silently lost)</td>
 * <td>NumberFormatException: &#123;"b": 1&#125; is not a valid Long (the JSON rule for a map under a scalar value type, L10)</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Map&lt;String,Map&lt;String,Long&gt;&gt; with setMapValueType(Integer) gives Longs on r56 and now: a declared argument of a nested map
 *   keeps its precedence over the global configuration, and a requested container argument is kept at the root (I37). The JSON reader itself
 *   is unchanged: &#123;"k":&#123;"a":1&#125;&#125; as Map&lt;String,Map&lt;String,Object&gt;&gt; with a Long map value type still gives an
 *   Integer there.</li>
 * </ul>
 * <p><b>Tests:</b> CodexXmlParserTest.globalMapValueTypeReachesNestedMapsOfCompactText</p>
 *
 * <h4>I44  With type information, a scalar type attribute on an Object bean property is adopted by the Abacus StAX and SAX readers too</h4>
 * <p><b>Fixed in:</b> r62</p>
 * <p><b>Problem.</b> A bean property declared Object whose element carries an approved scalar type attribute, under a root with type
 * information, was read with that type by the standard readers and the Abacus DOM reader (Long 5) but as the declared Object's text ("5") by
 * the Abacus StAX and SAX readers, whose text paths converted with the declared type. Owner ruling (2026-10-07): the StAX and SAX readers
 * adopt it the way the DOM reader does. Both text paths now share one rule with the DOM property loop: the approved attribute types the text
 * when type information is honored, no value type is configured for the property (I50), the property has no format and its declared type is
 * the Object handler (Object, or an interface the Object handler reads); a primitive name (int) does not fit an Object slot and keeps the
 * text, as before. An unreadable text now fails on all five where it failed on three (NumberFormatException for a Long,
 * IllegalArgumentException for a Boolean).</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * public static class Holder &#123; public Object open; /* getter and setter *&#47; &#125;   // the root element carries the class: &lt;holder type="...Holder"&gt;
 * &lt;holder type="...Holder"&gt;&lt;open type="Long"&gt;5&lt;/open&gt;
 *     &lt;/holder&gt;    read as Holder; also type="Integer", "Boolean", "int", "Object", no attribute
 * &lt;holder type="...Holder"&gt;&lt;open type="Long"&gt;x&lt;/open&gt;&lt;/holder&gt;    unreadable text
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I44 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (r61)</th><th scope="col">After fix (all five)</th></tr>
 * <tr><td>type="Long", text 5</td><td>XML StAX/DOM, Abacus DOM: 5L; Abacus StAX/SAX: "5"</td><td>5L</td></tr>
 * <tr><td>type="Integer", text 5</td><td>XML StAX/DOM, Abacus DOM: 5 (Integer); Abacus StAX/SAX: "5"</td><td>5 (Integer)</td></tr>
 * <tr>
 * <td>type="Boolean", text 5</td>
 * <td>XML StAX/DOM, Abacus DOM: IllegalArgumentException: Cannot convert "5" to boolean: only 1, y, Y and true (any case) are true, and only
 *     0, -1, n, N and false (any case) are false; Abacus StAX/SAX: "5"</td>
 * <td>IllegalArgumentException: Cannot convert "5" to boolean: only 1, y, Y and true (any case) are true, and only 0, -1, n, N and false (any
 *     case) are false</td>
 * </tr>
 * <tr>
 * <td>type="Long", text x</td>
 * <td>XML StAX/DOM, Abacus DOM: NumberFormatException: x is not a valid Long.; Abacus StAX/SAX: "x"</td>
 * <td>NumberFormatException: x is not a valid Long.</td>
 * </tr>
 * <tr><td>type="int", text 5</td><td>XML StAX/DOM: 5 (Integer); Abacus StAX/DOM/SAX: "5"</td><td>unchanged (L11)</td></tr>
 * <tr><td>type="Object" / no attribute</td><td>all five: "5"</td><td>unchanged</td></tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Without type information the Abacus readers keep the text on all three (pinned); the standard StAX reader adopts (L11).</li>
 *   <li>A scalar attribute naming an abstract class whose codec cannot read text (Number) is a ParsingException on all five (I48).</li>
 * </ul>
 * <p><b>Tests:</b> AbacusXmlParserImplTest.testReadObjectProperty_scalarTypeAttributeAdoptedOnAllBackends,
 * AbacusXmlParserImplTest.testReadObjectProperty_rejectedAttributeAndConcreteScalarsPinned,
 * AbacusXmlParserImplTest.testReadObjectProperty_unadoptedScalarAttributeShapesPinned</p>
 *
 * <h4>I45  A nested map key whose type attribute names a bean class is read as that bean on the Abacus DOM reader too</h4>
 * <p><b>Fixed in:</b> r62</p>
 * <p><b>Problem.</b> In an open Map&lt;Object,Object&gt;, a &lt;key&gt; element carrying an approved bean class and the bean's child element
 * was read as the bean by the Abacus StAX and SAX readers but as a bean-shaped map of its properties by the Abacus DOM reader, which handed
 * its nested reader the declared Object key type instead of the type the attribute selected (the value site already passed the selected type).
 * The key site now passes the adopted type for a concrete attribute class, as the value site does; keys without an attribute, with an Object
 * attribute, with container metadata and text keys are unchanged. Standard XML has no such shape (map keys are element names).</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map type="HashMap&lt;Object, Object&gt;"&gt;&lt;entry&gt;&lt;key type="...Child"&gt;&lt;child&gt;&lt;name&gt;k&lt;/name&gt;
 *     &lt;count&gt;1&lt;/count&gt;&lt;/child&gt;&lt;/key&gt;&lt;value&gt;v&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;
 *     read as Map&lt;Object,Object&gt;; also with an untyped root &lt;map&gt;
 * &lt;map type="..."&gt;&lt;entry&gt;&lt;key&gt;&lt;child&gt;...&lt;/child&gt;&lt;/key&gt;&lt;value&gt;v&lt;/value&gt;&lt;/entry&gt;
 *     &lt;/map&gt;           no attribute (control)
 * &lt;map type="..."&gt;&lt;entry&gt;&lt;key type="List&lt;Integer&gt;"&gt;&lt;list&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;/list&gt;
 *     &lt;/key&gt;&lt;value&gt;v&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;   container metadata (control)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I45 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (r61)</th><th scope="col">After fix (Abacus StAX/DOM/SAX)</th></tr>
 * <tr>
 * <td>bean class on the key, typed root</td>
 * <td>Abacus StAX/SAX: HashMap&#123;Child(k,1)="v"&#125;; Abacus DOM: HashMap&#123;LinkedHashMap&#123;name="k", count="1"&#125;="v"&#125;</td>
 * <td>HashMap&#123;Child(k,1)="v"&#125;</td>
 * </tr>
 * <tr><td>bean class on the key, untyped root</td><td>the same difference</td><td>HashMap&#123;Child(k,1)="v"&#125;</td></tr>
 * <tr>
 * <td>control: no attribute</td>
 * <td>all three: HashMap&#123;LinkedHashMap&#123;name="k", count="1"&#125;="v"&#125;</td>
 * <td>unchanged</td>
 * </tr>
 * <tr><td>control: the same bean class on a value</td><td>all three: HashMap&#123;"k"=Child(k,1)&#125;</td><td>unchanged</td></tr>
 * <tr><td>control: type="List&lt;Integer&gt;" key</td><td>all three: HashMap&#123;ArrayList[1, 2]="v"&#125;</td><td>unchanged</td></tr>
 * </table>
 * <p><b>Tests:</b> AbacusXmlParserImplTest.testReadMapEntry_nestedKeyWithBeanAttributeAdoptedOnAllBackends</p>
 *
 * <h4>I46  An explicit Object override keeps scalar text on the Abacus SAX reader, and a nested bean under it is read on the Abacus DOM reader</h4>
 * <p><b>Fixed in:</b> r62</p>
 * <p><b>Problem.</b> With setValueType("k", Object.class) (an open scalar override, I04) the Abacus SAX reader adopted a scalar type attribute
 * on the value (&lt;value type="Integer"&gt;2&lt;/value&gt; gave Integer 2) while the Abacus StAX and DOM readers kept the text; its
 * end-of-element refinement applied the attribute to the Object slot. And the Abacus DOM reader read a nested value carrying an approved bean
 * class as a map of its properties under the override while StAX and SAX read the bean. Now all three Abacus readers keep the text for a
 * scalar attribute and read the bean for a bean class; container metadata was already adopted on all three. The standard readers adopt the
 * scalar attribute under the override (L14).</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setValueType("k", Type.of(Object.class));
 * &lt;map type="HashMap&lt;String, Object&gt;"&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value type="Integer"&gt;2&lt;/value&gt;
 *     &lt;/entry&gt;&lt;/map&gt;              read as Map&lt;String,Object&gt;
 * &lt;map type="..."&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value type="...Child"&gt;&lt;child&gt;&lt;name&gt;k&lt;/name&gt;
 *     &lt;count&gt;1&lt;/count&gt;&lt;/child&gt;&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;
 * &lt;map type="..."&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value type="List&lt;Integer&gt;"&gt;[1, 2]&lt;/value&gt;&lt;/entry&gt;
 *     &lt;/map&gt;                        (control)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I46 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (r61)</th><th scope="col">After fix (Abacus StAX/DOM/SAX)</th></tr>
 * <tr><td>override, type="Integer", text 2</td><td>Abacus StAX/DOM: "2"; Abacus SAX: 2 (Integer)</td><td>"2"</td></tr>
 * <tr>
 * <td>override, bean class on a nested child</td>
 * <td>Abacus StAX/SAX: Child(k,1); Abacus DOM: LinkedHashMap&#123;name="k", count="1"&#125;</td>
 * <td>Child(k,1)</td>
 * </tr>
 * <tr><td>control: override, no attribute</td><td>all three: "2"</td><td>unchanged</td></tr>
 * <tr><td>control: override, type="List&lt;Integer&gt;" compact</td><td>all three: ArrayList[1, 2]</td><td>unchanged</td></tr>
 * <tr><td>control: no override, type="Integer"</td><td>all three: 2 (Integer)</td><td>unchanged</td></tr>
 * <tr><td>control: setValueType("k", Long.class)</td><td>all three: 2L</td><td>unchanged</td></tr>
 * </table>
 * <p><b>Tests:</b> AbacusXmlParserImplTest.testReadMapEntry_explicitObjectOverrideOnAllBackends</p>
 *
 * <h4>I47  A bean-shaped element nested in a bean-shaped element is read on the Abacus SAX reader</h4>
 * <p><b>Fixed in:</b> r62</p>
 * <p><b>Problem.</b> A bean-shaped element in an open slot is read as a map of its properties (I18, I21). When such an element held another
 * one (a property whose single child is itself bean-shaped), the Abacus SAX handler restored the outer element as if it were a map node when
 * the inner one closed: inside a map value it popped the enclosing map's key and value types, inside a list item or an Object property it
 * failed with IndexOutOfBoundsException: Index -1 out of bounds for length 0. The handler now restores the outer element as the sink it is.
 * The StAX and DOM readers were unaffected. The standard readers read a bean-shaped &lt;e&gt; item as the element's text (L08) and a
 * bean-shaped map value or Object property as nested maps, as before.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;list&gt;&lt;e&gt;&lt;thing&gt;&lt;a&gt;1&lt;/a&gt;&lt;d&gt;&lt;inner&gt;&lt;x&gt;4&lt;/x&gt;&lt;y&gt;5&lt;/y&gt;&lt;/inner&gt;&lt;/d&gt;
 *     &lt;b&gt;2&lt;/b&gt;&lt;/thing&gt;&lt;/e&gt;&lt;/list&gt;      read as List&lt;Object&gt;
 * &lt;holder type="...Holder"&gt;&lt;open&gt;&lt;thing&gt;...the same...&lt;/thing&gt;&lt;/open&gt;
 *     &lt;/holder&gt;                       read as Holder
 * &lt;map&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value&gt;&lt;thing&gt;...the same...&lt;/thing&gt;&lt;/value&gt;&lt;/entry&gt;
 *     &lt;/map&gt;                read as Map&lt;String,Object&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I47 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (r61)</th><th scope="col">After fix</th></tr>
 * <tr>
 * <td>list item</td>
 * <td>Abacus StAX/DOM: ArrayList[LinkedHashMap&#123;a="1", d=LinkedHashMap&#123;x="4", y="5"&#125;, b="2"&#125;]; Abacus SAX:
 *     IndexOutOfBoundsException: Index -1 out of bounds for length 0; XML StAX/DOM: ArrayList["1452"]</td>
 * <td>Abacus StAX/DOM/SAX: ArrayList[LinkedHashMap&#123;a="1", d=LinkedHashMap&#123;x="4", y="5"&#125;, b="2"&#125;]; XML StAX/DOM unchanged
 * </td>
 * </tr>
 * <tr>
 * <td>Object property</td>
 * <td>Abacus StAX/DOM and XML StAX/DOM: the nested maps; Abacus SAX: the same IndexOutOfBoundsException</td>
 * <td>the nested maps on all five</td>
 * </tr>
 * <tr>
 * <td>map value</td>
 * <td>all five: the nested maps (the SAX handler used the enclosing map's types for the inner element)</td>
 * <td>unchanged</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Properties without type attributes only: with attributes the Abacus readers still differ (L13).</li>
 * </ul>
 * <p><b>Tests:</b> AbacusXmlParserImplTest.testReadBeanShapedElement_nestedBeanShapedElementOnAllBackends</p>
 *
 * <h4>I48  An adopted type attribute naming a class whose values cannot be read from text is a ParsingException</h4>
 * <p><b>Fixed in:</b> r62</p>
 * <p><b>Problem.</b> When the type attribute of a bean property, map value, map key or list item names an allowed class whose codec cannot
 * construct a value from text at all (java.lang.Number: no String factory or constructor), every reader leaked the codec's
 * UnsupportedOperationException (the Abacus StAX and SAX readers did not adopt it on a bean property before I44 and returned the text). Owner
 * ruling (2026-10-07): it is a document error, reported as a ParsingException naming the attribute, on all five backends and at every site,
 * with the UnsupportedOperationException as the cause; only when the attribute selected the converting type. Value errors of working codecs
 * keep the codec's own exception as for declared types and in JSON: NumberFormatException for type="Long" with text x (I44), the
 * IllegalArgumentException of an ambiguous or unparsable date text; an abstract class with a working codec (Calendar) and an interface the
 * Object handler reads (CharSequence) still convert.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * public static class Holder &#123; public Object open; /* getter and setter *&#47; &#125;   // the root element carries the class: &lt;holder type="...Holder"&gt;
 * &lt;holder type="...Holder"&gt;&lt;open type="Number"&gt;5&lt;/open&gt;&lt;/holder&gt;                    read as Holder
 * &lt;map type="HashMap&lt;String, Object&gt;"&gt;&lt;k type="Number"&gt;5&lt;/k&gt;
 *     &lt;/map&gt;                 read as Map&lt;String,Object&gt; (Abacus: &lt;value type="Number"&gt;5&lt;/value&gt;)
 * &lt;list type="ArrayList&lt;Object&gt;"&gt;&lt;e type="Number"&gt;5&lt;/e&gt;&lt;/list&gt;                   read as List&lt;Object&gt;
 * &lt;holder type="...Holder"&gt;&lt;open type="Timestamp"&gt;5&lt;/open&gt;
 *     &lt;/holder&gt;                 a working codec's value error (control)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I48 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (r61)</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>bean property, type="Number"</td>
 * <td>XML StAX/DOM, Abacus DOM: UnsupportedOperationException: No public static method or constructor found in java.lang.Number to create a
 *     value from String; Abacus StAX/SAX: "5"</td>
 * <td>ParsingException: XML type attribute 'Number' names java.lang.Number, whose values cannot be read from element text</td>
 * </tr>
 * <tr>
 * <td>map value, type="Number"</td>
 * <td>all five: UnsupportedOperationException: No public static method or constructor found in java.lang.Number to create a value from String
 * </td>
 * <td>ParsingException: XML type attribute 'Number' names java.lang.Number, whose values cannot be read from element text</td>
 * </tr>
 * <tr>
 * <td>list item, type="Number"</td>
 * <td>all five: UnsupportedOperationException: No public static method or constructor found in java.lang.Number to create a value from String
 * </td>
 * <td>ParsingException: XML type attribute 'Number' names java.lang.Number, whose values cannot be read from element text</td>
 * </tr>
 * <tr>
 * <td>control: type="Timestamp", text 5</td>
 * <td>XML StAX/DOM, Abacus DOM: IllegalArgumentException: Ambiguous numeric date/time text; supply an explicit format or use parseEpochMillis:
 *     "5"; Abacus StAX/SAX: "5"</td>
 * <td>IllegalArgumentException: Ambiguous numeric date/time text; supply an explicit format or use parseEpochMillis: "5" (the codec's own
 *     exception)</td>
 * </tr>
 * <tr>
 * <td>control: type="Calendar", 2024-01-02T03:04:05Z</td>
 * <td>XML StAX/DOM, Abacus DOM: a GregorianCalendar; Abacus StAX/SAX: the text</td>
 * <td>a GregorianCalendar</td>
 * </tr>
 * <tr><td>control: type="CharSequence", text 5</td><td>all five: "5"</td><td>unchanged</td></tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Implemented in AbstractXmlParser.valueOfAdoptedText, used by the DOM property reader of both parsers, the Abacus StAX/SAX value, key,
 *   item and property text paths and the standard StAX property, value and item text paths.</li>
 *   <li>An attribute naming the declared class of a Number property (the writer's own typed output) still leaked the
 *   UnsupportedOperationException on the Abacus StAX and SAX readers; fixed by I49.</li>
 * </ul>
 * <p><b>Tests:</b> AbacusXmlParserImplTest.testAdoptedTypeAttribute_unreadableCodecIsParsingExceptionOnAllBackends,
 * XmlParserImplTest.testAdoptedTypeAttribute_unreadableCodecIsParsingExceptionOnBothBackends</p>
 *
 * <h4>I49  A Number bean property whose type attribute names its own declared class is a ParsingException on the Abacus StAX and SAX readers too</h4>
 * <p><b>Fixed in:</b> r63</p>
 * <p><b>Problem.</b> The writer marks the value of a property declared Number with its own class (&lt;n type="Number"&gt;5&lt;/n&gt;), and
 * java.lang.Number has no String factory or constructor. I48 made an attribute naming such a class a ParsingException naming the attribute; on
 * a property declared Number the attribute names the declared class itself, and the Abacus StAX and SAX property text paths read the text with
 * the declared codec and leaked its UnsupportedOperationException, while the Abacus DOM reader and both standard readers reported the
 * ParsingException. The two text paths now read the declared type's text through the same check as an adopted type (valueOfAdoptedText), so
 * all five report the ParsingException, with the UnsupportedOperationException as the cause, with and without type information. Without an
 * attribute, or with an attribute naming another class, the declared codec's own exception stays, as for any declared type.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * public static class NumHolder &#123; public Number n; /* getter and setter *&#47; &#125;   // written with type information: &lt;n type="Number"&gt;5&lt;/n&gt;
 * &lt;numHolder type="...NumHolder"&gt;&lt;n type="Number"&gt;5&lt;/n&gt;&lt;/numHolder&gt;    read as NumHolder (the writer's own output)
 * &lt;numHolder&gt;&lt;n type="Number"&gt;5&lt;/n&gt;&lt;/numHolder&gt;                         untyped root
 * &lt;numHolder type="...NumHolder"&gt;&lt;n&gt;5&lt;/n&gt;&lt;/numHolder&gt;                    no attribute (control)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I49 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (r62)</th><th scope="col">After fix (all five)</th></tr>
 * <tr>
 * <td>type="Number", typed root</td>
 * <td>XML StAX/DOM, Abacus DOM: ParsingException: XML type attribute 'Number' names java.lang.Number, whose values cannot be read from element
 *     text; Abacus StAX/SAX: UnsupportedOperationException: No public static method or constructor found in java.lang.Number to create a value
 *     from String</td>
 * <td>ParsingException: XML type attribute 'Number' names java.lang.Number, whose values cannot be read from element text</td>
 * </tr>
 * <tr>
 * <td>type="Number", untyped root</td>
 * <td>the same difference</td>
 * <td>ParsingException: XML type attribute 'Number' names java.lang.Number, whose values cannot be read from element text</td>
 * </tr>
 * <tr>
 * <td>type="Number", setValueType("n", Long.class)</td>
 * <td>XML StAX/DOM, Abacus DOM: 5L; Abacus StAX/SAX: UnsupportedOperationException: No public static method or constructor found in
 *     java.lang.Number to create a value from String</td>
 * <td>5L (I50)</td>
 * </tr>
 * <tr>
 * <td>control: no attribute, or type="Integer"</td>
 * <td>all five: UnsupportedOperationException: No public static method or constructor found in java.lang.Number to create a value from String
 * </td>
 * <td>unchanged (the declared codec's own exception)</td>
 * </tr>
 * <tr><td>control: type="Number", no content or isNull="true"</td><td>all five: null</td><td>unchanged</td></tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>At r61 the three Abacus readers agreed on the UnsupportedOperationException; I48 (r62) moved the Abacus DOM reader to the
 *   ParsingException, which left this difference.</li>
 *   <li>An attribute naming another scalar (Integer) does not type a property declared Number: only an Object property adopts a scalar
 *   attribute (I44).</li>
 * </ul>
 * <p><b>Tests:</b> AbacusXmlParserImplTest.testDeclaredNumberPropertyWithItsOwnTypeAttribute_sameOnAllBackends</p>
 *
 * <h4>I50  A value type configured for a bean property types its text on the Abacus StAX and SAX readers too (formerly L12)</h4>
 * <p><b>Fixed in:</b> r63</p>
 * <p><b>Problem.</b> XmlDeserConfig.setValueType("open", Long.class) on a bean property declared Object was honored by the standard readers
 * and the Abacus DOM reader (5L) but not by the Abacus StAX and SAX readers, whose property text paths took a configured type only when it was
 * a container: they read the declared Object's text ("5"), with and without type information and whatever scalar or Optional attribute the
 * element carried (L12, reported as R2A-03 in the ledger of the 2026-10-07 review). Both text paths now read the text of a configured type the
 * way the DOM reader does (readValueText with the configured type): the configuration wins over a scalar or Optional attribute, an unreadable
 * text fails with the configured codec's exception and an element without content is the configured type's empty value. An Object
 * configuration is an explicit open override (I04): it keeps scalar and Optional metadata as text and adopts container metadata, as for map
 * values (I46); under it the Abacus SAX reader adopted an Optional attribute and the Abacus StAX reader kept container metadata as text. A
 * formatted property still reads through its format.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * public static class Holder &#123; public Object open; /* getter and setter *&#47; &#125;   // the root element carries the class: &lt;holder type="...Holder"&gt;
 * XmlDeserConfig config = XmlDeserConfig.create().setValueType("open", Type.of(Long.class));
 * &lt;holder type="...Holder"&gt;&lt;open&gt;5&lt;/open&gt;
 *     &lt;/holder&gt;     read as Holder; also an untyped root &lt;holder&gt;, type="Integer", type="Optional&lt;Long&gt;"
 * &lt;holder type="...Holder"&gt;&lt;open&gt;x&lt;/open&gt;&lt;/holder&gt;     unreadable text; also &lt;open&gt;&lt;/open&gt;
 * setValueType("open", Type.of(Object.class)) with &lt;open type="Optional&lt;Long&gt;"&gt;5&lt;/open&gt; and &lt;open type="List&lt;Integer&gt;"&gt;[1, 2]&lt;/open&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I50 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (r62)</th><th scope="col">After fix</th></tr>
 * <tr>
 * <td>Long configured, no attribute, typed or untyped root</td>
 * <td>XML StAX/DOM, Abacus DOM: 5L; Abacus StAX/SAX: "5"</td>
 * <td>5L (all five)</td>
 * </tr>
 * <tr><td>Long configured, type="Integer" or type="Optional&lt;Long&gt;"</td><td>the same difference</td><td>5L (all five)</td></tr>
 * <tr>
 * <td>Long configured, text x</td>
 * <td>XML StAX/DOM, Abacus DOM: NumberFormatException: x is not a valid Long.; Abacus StAX/SAX: "x"</td>
 * <td>NumberFormatException: x is not a valid Long. (all five)</td>
 * </tr>
 * <tr><td>Long configured, no content</td><td>Abacus SAX: ""; the other four: null</td><td>null (all five)</td></tr>
 * <tr>
 * <td>Object configured, type="Optional&lt;Long&gt;", typed root</td>
 * <td>Abacus StAX/DOM: "5"; Abacus SAX, XML StAX/DOM: Optional[5]</td>
 * <td>Abacus StAX/DOM/SAX: "5"; XML StAX/DOM: Optional[5] (L14)</td>
 * </tr>
 * <tr>
 * <td>Object configured, type="List&lt;Integer&gt;", [1, 2], typed root</td>
 * <td>Abacus StAX: "[1, 2]"; the other four: ArrayList[1, 2]</td>
 * <td>ArrayList[1, 2] (all five)</td>
 * </tr>
 * <tr>
 * <td>control: Object configured, type="Integer", typed root</td>
 * <td>Abacus StAX/DOM/SAX: "5"; XML StAX/DOM: 5 (Integer)</td>
 * <td>unchanged (L14)</td>
 * </tr>
 * <tr>
 * <td>control: Long configured on a property with dateFormat yyyy-MM-dd, text 2024-01-02</td>
 * <td>all five: the date, read through the format</td>
 * <td>unchanged</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Formerly L12 in part 1, moved here when it was fixed.</li>
 *   <li>Without type information the standard DOM reader keeps container metadata as text under an Object configuration as well ("[1, 2]"), as
 *   it does without the configuration (L15).</li>
 * </ul>
 * <p><b>Tests:</b> AbacusXmlParserImplTest.testConfiguredValueTypeOfBeanProperty_sameOnAllBackends</p>
 *
 * <h4>I51  An explicit Object override keeps the text of a value whose type attribute names a bean class on the Abacus SAX reader</h4>
 * <p><b>Fixed in:</b> r63</p>
 * <p><b>Problem.</b> Under an explicit Object override (setValueType("k", Object.class): open scalar text, I04) the Abacus StAX and DOM
 * readers keep the text of a map value whose type attribute names an approved bean class, as they keep it under a scalar attribute (I46). The
 * Abacus SAX handler adopted the bean class for the whole value and converted the text with it: "Can't parse: 5" for the text 5, a bean read
 * from JSON object text, a parse error for [1, 2], null for empty or blank text. The handler now keeps the bean class for a nested element
 * only and reads the text with the open Object type, so all three Abacus readers keep the text; a nested element carrying the bean is still
 * read as the bean (I46).</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setValueType("k", Type.of(Object.class));
 * &lt;map type="HashMap&lt;String, Object&gt;"&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value type="...Child"&gt;5&lt;/value&gt;
 *     &lt;/entry&gt;&lt;/map&gt;   read as Map&lt;String,Object&gt;; also &lt;map&gt;
 * &lt;map type="..."&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value type="...Child"&gt;&#123;"name": "x"&#125;&lt;/value&gt;&lt;/entry&gt;
 *     &lt;/map&gt;   also [1, 2], empty and blank text
 * &lt;map type="..."&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value type="...Child"&gt;&lt;child&gt;&lt;name&gt;k&lt;/name&gt;
 *     &lt;count&gt;1&lt;/count&gt;&lt;/child&gt;&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;   (control)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I51 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (r62)</th><th scope="col">After fix (Abacus StAX/DOM/SAX)</th></tr>
 * <tr>
 * <td>override, type="...Child", text 5</td>
 * <td>Abacus StAX/DOM: HashMap&#123;k="5"&#125;; Abacus SAX: ParsingException: Can't parse: 5</td>
 * <td>HashMap&#123;k="5"&#125;</td>
 * </tr>
 * <tr>
 * <td>override, text &#123;"name": "x"&#125;</td>
 * <td>Abacus StAX/DOM: the text; Abacus SAX: HashMap&#123;k=Child(x,0)&#125;</td>
 * <td>the text (a String)</td>
 * </tr>
 * <tr>
 * <td>override, text [1, 2]</td>
 * <td>Abacus StAX/DOM: the text; Abacus SAX: ParsingException: Error on parsing at '[' ...</td>
 * <td>the text</td>
 * </tr>
 * <tr><td>override, empty or blank text</td><td>Abacus StAX/DOM: "" or " "; Abacus SAX: null</td><td>"" or " "</td></tr>
 * <tr>
 * <td>control: override, the bean class on a nested &lt;child&gt;</td>
 * <td>all three: HashMap&#123;k=Child(k,1)&#125;</td>
 * <td>unchanged</td>
 * </tr>
 * <tr><td>control: no override, text 5</td><td>all three: ParsingException: Can't parse: 5</td><td>unchanged</td></tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>The standard readers convert the text with the bean class under the override (ParsingException: Can't parse: 5 for the text 5,
 *   Child(x,0) for the JSON text, null for empty text), as they adopt a scalar attribute under it: the same difference as L14.</li>
 * </ul>
 * <p><b>Tests:</b> AbacusXmlParserImplTest.testExplicitObjectOverrideWithBeanAttributeKeepsText_sameOnAllBackends</p>
 *
 * <h4>I52  Container metadata on an explicit-Object-override value or a nested map key types the nested element on every Abacus reader</h4>
 * <p><b>Fixed in:</b> r63</p>
 * <p><b>Problem.</b> A nested element under a map value whose explicit Object override (I04) carries container metadata (List&lt;Integer&gt;,
 * ImmutableList&lt;Integer&gt;, Object[], Map&lt;String, Integer&gt;, ImmutableMap&lt;String, Integer&gt;) is read as that container by the
 * Abacus DOM and SAX readers, which adopt the metadata (I23): a bean-shaped child that is no item of the container is rejected, as without the
 * override. The Abacus StAX reader read the nested element through the open slot of the override and returned the bean-shaped map. And an open
 * map key whose metadata names an uninstantiable container (ImmutableList, ImmutableMap: its open mutable schema is adopted) was read through
 * the declared Object key type by the Abacus DOM reader, which returned the bean-shaped map as the key; a key with an instantiable container
 * was already rejected on all three. The StAX value site now adopts container metadata under the override and the DOM key site reads with the
 * adopted container schema, as its value site does; a nested list or map is read with the container's item types on all three, as before.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * XmlDeserConfig config = XmlDeserConfig.create().setValueType("k", Type.of(Object.class));
 * &lt;map type="HashMap&lt;Object, Object&gt;"&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value type="List&lt;Integer&gt;"&gt;&lt;child&gt;
 *     &lt;name&gt;k&lt;/name&gt;&lt;count&gt;1&lt;/count&gt;&lt;/child&gt;&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;
 *     read as Map&lt;String,Object&gt;; also ImmutableList&lt;Integer&gt;, Object[], Map&lt;String, Integer&gt;, ImmutableMap&lt;String, Integer&gt;, an untyped root &lt;map&gt;
 * &lt;map type="..."&gt;&lt;entry&gt;&lt;key type="ImmutableList&lt;Integer&gt;"&gt;&lt;child&gt;...&lt;/child&gt;&lt;/key&gt;
 *     &lt;value&gt;v&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;   read as Map&lt;Object,Object&gt;
 * &lt;map type="..."&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value type="List&lt;Integer&gt;"&gt;&lt;list&gt;&lt;e&gt;1&lt;/e&gt;
 *     &lt;e&gt;2&lt;/e&gt;&lt;/list&gt;&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;   (control)
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I52 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (r62)</th><th scope="col">After fix (Abacus StAX/DOM/SAX)</th></tr>
 * <tr>
 * <td>override, container metadata, nested &lt;child&gt;</td>
 * <td>Abacus DOM/SAX: ParsingException: No bean class found for node name: child in package of class: java.util.Map; Abacus StAX:
 *     HashMap&#123;k=LinkedHashMap&#123;name="k", count="1"&#125;&#125;</td>
 * <td>ParsingException: No bean class found for node name: child in package of class: java.util.Map</td>
 * </tr>
 * <tr>
 * <td>key, type="ImmutableList&lt;Integer&gt;" or "ImmutableMap&lt;String, Integer&gt;", nested &lt;child&gt;</td>
 * <td>Abacus StAX/SAX: the same ParsingException; Abacus DOM: HashMap&#123;LinkedHashMap&#123;name="k", count="1"&#125;="v"&#125;</td>
 * <td>the same ParsingException</td>
 * </tr>
 * <tr>
 * <td>control: no override, container metadata, nested &lt;child&gt;</td>
 * <td>all three: the same ParsingException</td>
 * <td>unchanged</td>
 * </tr>
 * <tr>
 * <td>control: key, type="List&lt;Integer&gt;", "Map&lt;String, Integer&gt;" or "Object[]", nested &lt;child&gt;</td>
 * <td>all three: the same ParsingException</td>
 * <td>unchanged</td>
 * </tr>
 * <tr>
 * <td>control: override, type="List&lt;Integer&gt;", nested &lt;list&gt;</td>
 * <td>all three: HashMap&#123;k=ArrayList[1, 2]&#125;</td>
 * <td>unchanged</td>
 * </tr>
 * <tr>
 * <td>control: override, type="Map&lt;String, Integer&gt;", nested &lt;map&gt;</td>
 * <td>all three: HashMap&#123;k=HashMap&#123;a=1&#125;&#125;</td>
 * <td>unchanged</td>
 * </tr>
 * <tr>
 * <td>control: key, type="ImmutableList&lt;Integer&gt;", nested &lt;list&gt;</td>
 * <td>all three: HashMap&#123;ArrayList[1, 2]="v"&#125;</td>
 * <td>unchanged</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Standard XML has no nested key and reads the child elements of a value with its own rules (unchanged).</li>
 * </ul>
 * <p><b>Tests:</b> AbacusXmlParserImplTest.testContainerMetadataOnNestedKeyOrOverriddenValue_sameOnAllBackends</p>
 *
 * <h4>I53  A nested element in a String map key or value slot is read with its container metadata on the Abacus StAX and DOM readers too</h4>
 * <p><b>Fixed in:</b> r63</p>
 * <p><b>Problem.</b> A String key or value slot (the keys of a Map&lt;String, Object&gt;, the values of a Map&lt;Object, String&gt;) cannot
 * hold a nested element; the Abacus readers read one as an open slot does. Its container metadata typed the nested element on the Abacus SAX
 * reader only: the Abacus StAX reader read a nested list or map with String items, and the Abacus DOM reader typed a list but read a map with
 * String values. Both now read it with the metadata, as the SAX reader and an open slot do (an uninstantiable container gives its mutable
 * fallback class). Bean metadata does not open the slot on any Abacus reader (the nested element is rejected), text in the slot stays the
 * declared String, and a nested element without metadata keeps String items, all unchanged.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * &lt;map type="HashMap&lt;Object, Object&gt;"&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value type="List&lt;Integer&gt;"&gt;&lt;list&gt;
 *     &lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;/list&gt;&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;
 *     read as Map&lt;Object,String&gt;; also ImmutableList&lt;Integer&gt;, and an untyped root &lt;map&gt;
 * &lt;map type="..."&gt;&lt;entry&gt;&lt;key&gt;k&lt;/key&gt;&lt;value type="Map&lt;String, Integer&gt;"&gt;&lt;map&gt;&lt;entry&gt;
 *     &lt;key&gt;a&lt;/key&gt;&lt;value&gt;1&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;
 * &lt;map type="..."&gt;&lt;entry&gt;&lt;key type="List&lt;Integer&gt;"&gt;&lt;list&gt;&lt;e&gt;1&lt;/e&gt;&lt;e&gt;2&lt;/e&gt;&lt;/list&gt;
 *     &lt;/key&gt;&lt;value&gt;v&lt;/value&gt;&lt;/entry&gt;&lt;/map&gt;   read as Map&lt;String,Object&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I53 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (r62)</th><th scope="col">After fix (Abacus StAX/DOM/SAX)</th></tr>
 * <tr>
 * <td>String value, type="List&lt;Integer&gt;", nested &lt;list&gt;</td>
 * <td>Abacus StAX: HashMap&#123;k=ArrayList["1", "2"]&#125;; Abacus DOM/SAX: HashMap&#123;k=ArrayList[1, 2]&#125;</td>
 * <td>HashMap&#123;k=ArrayList[1, 2]&#125;</td>
 * </tr>
 * <tr>
 * <td>String key, type="List&lt;Integer&gt;", nested &lt;list&gt;</td>
 * <td>Abacus StAX: HashMap&#123;ArrayList["1", "2"]="v"&#125;; Abacus DOM/SAX: HashMap&#123;ArrayList[1, 2]="v"&#125;</td>
 * <td>HashMap&#123;ArrayList[1, 2]="v"&#125;</td>
 * </tr>
 * <tr>
 * <td>String value, type="Map&lt;String, Integer&gt;", nested &lt;map&gt;</td>
 * <td>Abacus StAX/DOM: HashMap&#123;k=LinkedHashMap&#123;a="1"&#125;&#125;; Abacus SAX: HashMap&#123;k=HashMap&#123;a=1&#125;&#125;</td>
 * <td>HashMap&#123;k=HashMap&#123;a=1&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>String key, type="Map&lt;String, Integer&gt;", nested &lt;map&gt;</td>
 * <td>the same difference</td>
 * <td>HashMap&#123;HashMap&#123;a=1&#125;="v"&#125;</td>
 * </tr>
 * <tr>
 * <td>type="ImmutableMap&lt;String, Integer&gt;", nested &lt;map&gt;</td>
 * <td>Abacus StAX/DOM: LinkedHashMap&#123;a="1"&#125;; Abacus SAX: LinkedHashMap&#123;a=1&#125;</td>
 * <td>LinkedHashMap&#123;a=1&#125;</td>
 * </tr>
 * <tr><td>control: no attribute, nested &lt;list&gt;</td><td>all three: HashMap&#123;k=ArrayList["1", "2"]&#125;</td><td>unchanged</td></tr>
 * <tr><td>control: type="List&lt;Integer&gt;", text [1, 2]</td><td>all three: HashMap&#123;k="[1, 2]"&#125;</td><td>unchanged</td></tr>
 * <tr>
 * <td>control: type="...Child", nested &lt;child&gt;</td>
 * <td>all three: ParsingException: No bean class found for node name: child in package of class: java.util.Map</td>
 * <td>unchanged</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Standard XML writes and reads its own nested forms (unchanged). I54 follows from this fix.</li>
 * </ul>
 * <p><b>Tests:</b> AbacusXmlParserImplTest.testContainerMetadataOnNestedElementInStringSlot_sameOnAllBackends</p>
 *
 * <h4>I54  A nested Object[] item of an Object[] property keeps its class under String element configuration on the Abacus StAX and DOM readers</h4>
 * <p><b>Fixed in:</b> r63</p>
 * <p><b>Problem.</b> An Object[] property holding a nested Object[] item, written with type information (&lt;e type="Object[]"&gt;&lt;array
 * type="Object[]"&gt;...), was read back with setElementType(String.class) as a String[] item by the Abacus StAX and DOM readers: the
 * configured String types the open item slot, and the nested array in that String slot was read with the slot's String type instead of its own
 * metadata (I53). Under the attribute name style (tagByPropertyName false) the Abacus StAX reader failed instead. The Abacus SAX reader and
 * the standard readers kept the Object[] item. With I53 the item's own metadata types the nested array on all three Abacus readers; the
 * configured String still types its open elements.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * public static class ArrHolder &#123; public Object[] arr; /* getter and setter *&#47; &#125;   // arr = Object[]&#123;"x", Object[]&#123;"deep"&#125;&#125;
 * &lt;arrHolder type="...ArrHolder"&gt;&lt;arr type="Object[]"&gt;&lt;array type="Object[]"&gt;&lt;e type="String"&gt;x&lt;/e&gt;
 *     &lt;e type="Object[]"&gt;&lt;array type="Object[]"&gt;&lt;e type="String"&gt;deep&lt;/e&gt;&lt;/array&gt;&lt;/e&gt;&lt;/array&gt;
 *     &lt;/arr&gt;&lt;/arrHolder&gt;
 *     written with type information, read as ArrHolder with setElementType(String.class); also the attribute name style:
 * &lt;bean name="arrHolder" type="...ArrHolder"&gt;&lt;property name="arr" type="Object[]"&gt;...the same...&lt;/property&gt;&lt;/bean&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I54 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (r62)</th><th scope="col">After fix (Abacus StAX/DOM/SAX)</th></tr>
 * <tr>
 * <td>typed output, String configured</td>
 * <td>Abacus StAX/DOM: Object[]&#123;"x", String[]&#123;"deep"&#125;&#125;; Abacus SAX: Object[]&#123;"x", Object[]&#123;"deep"&#125;&#125;
 * </td>
 * <td>Object[]&#123;"x", Object[]&#123;"deep"&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>attribute name style, String configured</td>
 * <td>Abacus StAX: ParsingException: Unsupported class type: class java.lang.String. Only object array, collection, map and bean types are
 *     supported; Abacus DOM: Object[]&#123;"x", String[]&#123;"deep"&#125;&#125;; Abacus SAX: Object[]&#123;"x",
 *     Object[]&#123;"deep"&#125;&#125;</td>
 * <td>Object[]&#123;"x", Object[]&#123;"deep"&#125;&#125;</td>
 * </tr>
 * <tr>
 * <td>control: typed output, no configuration</td>
 * <td>all three: Object[]&#123;"x", Object[]&#123;"deep"&#125;&#125;</td>
 * <td>unchanged</td>
 * </tr>
 * <tr>
 * <td>control: untyped output, with or without configuration</td>
 * <td>all three: Object[]&#123;"x", String[]&#123;"deep"&#125;&#125;</td>
 * <td>unchanged</td>
 * </tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Without type information the Abacus readers read an untyped nested &lt;array&gt; as a String[] on all three (unchanged).</li>
 * </ul>
 * <p><b>Tests:</b> AbacusXmlParserImplTest.testNestedArrayItemUnderStringElementConfiguration_sameOnAllBackends</p>
 *
 * <h4>I55  A bean property element without content is read as empty text on the Abacus StAX reader</h4>
 * <p><b>Fixed in:</b> r63</p>
 * <p><b>Problem.</b> The Abacus StAX reader read a bean property element without content (also one holding only a comment) with its own
 * fallback, the selected type's value of the empty string, instead of the property text rule the Abacus DOM and SAX readers apply: it read an
 * attribute the text rule ignores (a primitive name, a scalar attribute without type information: 0 or null where the text rule keeps ""),
 * skipped the Optional payload refinement (Optional[""] for an Optional&lt;Object&gt; property under scalar metadata) and ignored the
 * property's number format (null). It now reads the empty text through the same routine as text. An element with blank text was already read
 * as text.</p>
 * <p><b>Sample input</b></p>
 * <pre>
 * public static class Rec &#123; &#64;JsonXmlField(numberFormat = "#,##0") Integer amount; Optional&lt;Object&gt; opt; Object open; /* getters and setters *&#47; &#125;
 * &lt;rec type="...Rec"&gt;&lt;open type="int"&gt;&lt;/open&gt;&lt;/rec&gt;     read as Rec; also &lt;open type="Long"&gt;
 *     &lt;/open&gt; under an untyped root &lt;rec&gt;
 * &lt;rec type="...Rec"&gt;&lt;opt type="Long"&gt;&lt;/opt&gt;&lt;/rec&gt;      also type="int"
 * &lt;rec type="...Rec"&gt;&lt;amount&gt;&lt;/amount&gt;&lt;/rec&gt;             also &lt;amount&gt;&lt;!-- c --&gt;&lt;/amount&gt;
 * </pre>
 * <p><b>Output</b></p>
 * <table class="striped">
 * <caption>Output of I55 on the backends</caption>
 * <tr><th scope="col">Document</th><th scope="col">Before fix (r62)</th><th scope="col">After fix</th></tr>
 * <tr>
 * <td>typed root, &lt;open type="int"&gt;&lt;/open&gt;</td>
 * <td>Abacus StAX, XML StAX/DOM: 0 (Integer); Abacus DOM/SAX: ""</td>
 * <td>Abacus StAX/DOM/SAX: ""; XML StAX/DOM unchanged (L11)</td>
 * </tr>
 * <tr>
 * <td>untyped root, &lt;open type="Long"&gt;&lt;/open&gt;</td>
 * <td>Abacus StAX, XML StAX: null; the other three: ""</td>
 * <td>Abacus StAX/DOM/SAX: ""; XML StAX unchanged (L11)</td>
 * </tr>
 * <tr>
 * <td>untyped root, &lt;open type="int"&gt;&lt;/open&gt;</td>
 * <td>Abacus StAX, XML StAX: 0 (Integer); the other three: ""</td>
 * <td>Abacus StAX/DOM/SAX: ""; XML StAX unchanged (L11)</td>
 * </tr>
 * <tr>
 * <td>&lt;opt type="Long"&gt;&lt;/opt&gt;</td>
 * <td>Abacus StAX: Optional[""]; the other four: Optional.empty</td>
 * <td>Optional.empty (all five)</td>
 * </tr>
 * <tr>
 * <td>&lt;opt type="int"&gt;&lt;/opt&gt;</td>
 * <td>Abacus StAX: Optional[""]; Abacus DOM/SAX, XML DOM: Optional[0]; XML StAX: Optional.empty</td>
 * <td>Abacus StAX/DOM/SAX: Optional[0]; XML StAX unchanged (L16)</td>
 * </tr>
 * <tr>
 * <td>&lt;amount&gt;&lt;/amount&gt;, numberFormat #,##0</td>
 * <td>Abacus StAX, XML StAX: null; Abacus DOM/SAX, XML DOM: ParsingException: Failed to parse complete number value ...</td>
 * <td>Abacus StAX/DOM/SAX: the ParsingException; XML StAX unchanged (L16)</td>
 * </tr>
 * <tr><td>control: typed root, &lt;open type="Long"&gt;&lt;/open&gt;</td><td>all five: null</td><td>unchanged</td></tr>
 * <tr>
 * <td>control: &lt;open&gt;&lt;/open&gt;, &lt;text&gt;&lt;/text&gt;, &lt;count&gt;&lt;/count&gt;, &lt;list&gt;&lt;/list&gt;,
 *     &lt;when&gt;&lt;/when&gt; (dateFormat)</td>
 * <td>all five: "", "", 0, null, null</td>
 * <td>unchanged</td>
 * </tr>
 * <tr><td>control: &lt;amount&gt; &lt;/amount&gt; (blank text)</td><td>all five: the ParsingException</td><td>unchanged</td></tr>
 * </table>
 * <p><b>Worth knowing</b></p>
 * <ul>
 *   <li>Behavior change: an element without content of a property with a number format now fails on the Abacus StAX reader with the format's
 *   ParsingException, as on the Abacus DOM and SAX readers and the standard DOM reader and as blank text already did on all five (it was
 *   null). A property with a date format still reads it as null on all five.</li>
 *   <li>The text rule is the one of I44 and I50: an attribute types the text of an Object property only with type information and not for a
 *   primitive name, and a configured value type wins.</li>
 * </ul>
 * <p><b>Tests:</b> AbacusXmlParserImplTest.testPropertyElementWithoutContent_sameOnAllBackends</p>
 *
 * @see ParserFactory
 * @see Parser
 * @see JsonParser
 * @see XmlParser
 */
package com.landawn.abacus.parser;
