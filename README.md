# abacus-common
[![Maven Central](https://img.shields.io/maven-central/v/com.landawn/abacus-common.svg)](https://maven-badges.herokuapp.com/maven-central/com.landawn/abacus-common/)
[![Javadocs](https://img.shields.io/badge/javadoc-6.26.6-brightgreen.svg)](https://www.javadoc.io/doc/com.landawn/abacus-common/6.26.6/index.html)


**abacus-common** is a general-purpose Java programming library that provides a rich set of utilities, data structures, and functional APIs.

##### Key Features

1. *Frequently used utilities* — e.g., `Strings`, `Numbers`, `Maps`, `Joiner`, `Splitter`.
2. *Additional data structures* — `Dataset`, `Sheet`, `Multiset`, `Multimap`, `Pair`, `Tuple`, `Range`, `Fraction`.
3. *Comprehensive functional programming APIs* — including `Stream`, `EntryStream`, `Collectors`.

##### How it differs from other libraries

1. *Comprehensive and consistent*:
   abacus-common offers unified APIs across data types — for example, `N.isEmpty(String/byte[]/Collection/Map/...)` — instead of relying on separate utilities like `StringUtils/ArrayUtils/Collections/MapUtils.isEmpty(...)` from different classes/libraries.

2. *Simple and concise*:
   All APIs are well organized and follow consistent design principles:
	* Use concise, consistent method names and parameter orders.
	* Ensure predictable and uniform behavior across components.
	* Empty `strings/collections/maps` or `Optional` are preferred over `null` as return values whenever appropriate.
	* More ... refer to: [How To Design A Good API and Why it Matters](https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/32713.pdf), [video](https://www.youtube.com/watch?v=aAb7hSCtvGw)

3. *Powerful and extensive*:
   The library provides thousands of utility methods — covering everything from daily-use helpers to advanced support for I/O, concurrency, JSON/XML serialization, and functional programming(e.g.,`Stream.of(persons).map(Person::getEmail())...filter(Strings::isNotEmpty).groupBy(Person::getLastName).persist(file)...`).


##### Why abacus-common?

abacus-common is a *mega-library* unlike most others. It contains thousands of public methods across tens of classes, covering nearly all common programming use cases. You no longer need to search multiple libraries or maintain your own `StringUtils` or `CollectionUtils` — everything is conveniently available in one place with clean, consistent, and easy-to-use APIs. Well-designed APIs and data structures steady your mind ♪ 𝆑 ♪ and set your keyboard alight ♪♫♪♫. It may very well change the way you write Java. The ultimate goal is to make code that uses this library easy to read and maintain by simplifying the implementation and enhancing readability through concise APIs and data structures.

## Features:

* Frequently Used APIs: [CommonUtil](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/CommonUtil_view.html), 
[N](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/N_view.html), 
[Strings](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Strings_view.html), 
[Numbers](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Numbers_view.html), 
[Array](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Array_view.html), 
[Iterables](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Iterables_view.html), 
[Iterators](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Iterators_view.html), 
[Maps](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Maps_view.html), 
[Beans](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Beans_view.html), 
[Dates](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Dates_view.html), 
[IOUtil](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/IOUtil_view.html), 
[Files](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Files_view.html), 
[Multiset](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Multiset_view.html), 
[Multimap](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Multimap_view.html), 
[ListMultimap](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/ListMultimap_view.html), 
[SetMultimap](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/SetMultimap_view.html), 
[Dataset](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Dataset_view.html), 
[Sheet](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Sheet_view.html), 
[BiMap](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/BiMap_view.html), 
[ImmutableList](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/ImmutableList_view.html), 
[ImmutableSet](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/ImmutableSet_view.html), 
[ImmutableMap](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/ImmutableMap_view.html), 
[Result](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Result_view.html), 
[Holder](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Holder_view.html), 
[Pair](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Pair_view.html), 
[Triple](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Triple_view.html), 
[Tuple](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Tuple_view.html), 
[Range](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Range_view.html), 
[Duration](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Duration_view.html), 
[Stopwatch](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/Stopwatch.html), 
[RateLimiter](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/RateLimiter.html), 
[Fraction](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Fraction_view.html), 
[Splitter](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Splitter_view.html), 
[Joiner](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Joiner_view.html), 
[Builder](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Builder_view.html), 
[Difference](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Difference_view.html), 
[Comparators](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Comparators_view.html), 
[Index](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Index_view.html), 
[Median](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/Median.html), 
[Wrapper](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/Wrapper.html), 
[Indexed](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Indexed_view.html), 
[Keyed](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Keyed_view.html), 
[Timed](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/Timed.html), 
[IndexedKeyed](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/IndexedKeyed.html), 
[TypeReference](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/TypeReference.html), 
[Clazz](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Clazz_view.html), 
[If](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/If_view.html), 
[Try](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Try_view.html), 
[Synchronized](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Synchronized_view.html), 
[Retry](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Retry_view.html), 
[Hashing](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Hashing_view.html), 
[Hex](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Hex_view.html), 
[DigestUtil](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/DigestUtil_view.html), 
[RegExUtil](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/RegExUtil_view.html), 
[URLEncodedUtil](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/URLEncodedUtil_view.html), 
[CSVUtil](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/CSVUtil.html), 
[ExcelUtil](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/poi/ExcelUtil.html), 
[AsyncExecutor](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/AsyncExecutor_view.html), 
[ContinuableFuture](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/ContinuableFuture_view.html), 
[Futures](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Futures_view.html),
[Profiler](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/Profiler.html)...

* Streams, both sequential and parallel, are supported with more functions:  
[BaseStream](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/BaseStream_view.html), 
[Stream](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Stream_view.html), 
[CharStream](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/CharStream_view.html), 
[ByteStream](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/ByteStream_view.html), 
[ShortStream](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/ShortStream_view.html), 
[IntStream](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/IntStream_view.html), 
[LongStream](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/LongStream_view.html), 
[FloatStream](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/FloatStream_view.html), 
[DoubleStream](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/DoubleStream_view.html), 
[EntryStream](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/EntryStream_view.html), 
[Fn](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Fn_view.html), 
[MoreCollectors](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Collectors_view.html), 
[Seq](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Seq_view.html), 
[Fnn](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/Fnn.html) and 
[Throwables](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Throwables_view.html).

* Optional: 
[OptionalBoolean](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/OptionalBoolean_view.html), 
[OptionalChar](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/OptionalChar_view.html), 
[OptionalByte](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/OptionalByte_view.html), 
[OptionalShort](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/OptionalShort_view.html), 
[OptionalInt](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/OptionalInt_view.html), 
[OptionalLong](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/OptionalLong_view.html), 
[OptionalFloat](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/OptionalFloat_view.html), 
[OptionalDouble](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/OptionalDouble_view.html), 
[Optional](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Optional_view.html) and 
[Nullable](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/Nullable_view.html)

* Primitive Mutable: 
[MutableBoolean](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/MutableBoolean_view.html), 
[MutableChar](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/MutableChar_view.html), 
[MutableByte](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/MutableByte_view.html), 
[MutableShort](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/MutableShort_view.html), 
[MutableInt](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/MutableInt_view.html), 
[MutableLong](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/MutableLong_view.html), 
[MutableFloat](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/MutableFloat_view.html) and 
[MutableDouble](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/MutableDouble_view.html).

* Primitive/Immutable Iterator: 
[ObjIterator](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/ObjIterator_view.html),
[BooleanIterator](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/BooleanIterator_view.html), 
[CharIterator](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/CharIterator_view.html), 
[ByteIterator](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/ByteIterator_view.html), 
[ShortIterator](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/ShortIterator_view.html), 
[IntIterator](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/IntIterator_view.html), 
[LongIterator](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/LongIterator_view.html), 
[FloatIterator](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/FloatIterator_view.html), 
[DoubleIterator](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/DoubleIterator_view.html),
[BiIterator](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/BiIterator_view.html),
[TriIterator](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/TriIterator_view.html) and 
[LineIterator](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/LineIterator_view.html). 

* Primitive List: 
[PrimitiveList](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/PrimitiveList_view.html), 
[BooleanList](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/BooleanList_view.html), 
[CharList](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/CharList_view.html), 
[ByteList](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/ByteList_view.html), 
[ShortList](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/ShortList_view.html), 
[IntList](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/IntList_view.html), 
[LongList](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/LongList_view.html), 
[FloatList](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/FloatList_view.html) and
[DoubleList](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/DoubleList_view.html).

* Http:
[HttpClient](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/HttpClient_view.html), 
[HttpRequest](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/HttpRequest_view.html), 
[OkHttpRequest](https://htmlpreview.github.io/?https://github.com/landawn/abacus-common/master/docs/OkHttpRequest_view.html), 
and [More](https://www.javadoc.io/static/com.landawn/abacus-common/6.26.6/com/landawn/abacus/http/package-summary.html) ...

* JSON/XML Data Binding: 
[Parser](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/parser/Parser.html), 
[JSONParser](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/parser/JSONParser.html), 
[XMLParser](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/parser/XMLParser.html), 
[KryoParser](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/parser/KryoParser.html), 
[ParserFactory](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/parser/ParserFactory.html),
[JsonMappers](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/JsonMappers.html), 
[XmlMappers](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/XmlMappers.html), 
[FastJson](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/FastJson.html), 
[JSONUtil](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/JSONUtil.html)
and [More](https://www.javadoc.io/static/com.landawn/abacus-common/6.26.6/com/landawn/abacus/parser/package-summary.html) ...

* Pool: 
[Pool](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/pool/Pool.html), 
[ObjectPool](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/pool/ObjectPool.html), 
[KeyedObjectPool](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/pool/KeyedObjectPool.html), 
[PoolFactory](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/pool/PoolFactory.html)
and [More](https://www.javadoc.io/static/com.landawn/abacus-common/6.26.6/com/landawn/abacus/pool/package-summary.html) ...


* More:
[CodeGenerationUtil](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/CodeGenerationUtil.html), 
[ClassUtil](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/ClassUtil.html),
[Charsets](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/Charsets.html),
[Ascii](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/Ascii.html),
[CalendarField](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/CalendarField.html),
[NamingPolicy](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/NamingPolicy.html), 
[Properties](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/Properties.html),
[PropertiesUtil](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/PropertiesUtil.html),
[Configuration](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/Configuration.html),
[XmlUtil](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/XmlUtil.html),
[EscapeUtil](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/EscapeUtil.html),
[FilenameUtil](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/FilenameUtil.html),
[AddrUtil](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/AddrUtil.html),
[WSSecurityUtil](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/WSSecurityUtil.html),
[EmailUtil](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/EmailUtil.html),
[IEEE754rUtil](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/util/IEEE754rUtil.html),
[WebUtil](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/http/WebUtil.html),
[Files](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/guava/Files.html),
[Traverser](https://static.javadoc.io/com.landawn/abacus-common/6.26.6/com/landawn/abacus/guava/Traverser.html)(copied from Apache commons, Google Guava...under Apache License v2) ...


## Download/Installation & [Changes](https://github.com/landawn/abacus-common/blob/master/CHANGES.md):

* [Maven](https://central.sonatype.com/artifact/com.landawn/abacus-common)

```xml
<dependency>
	<groupId>com.landawn</groupId>
	<artifactId>abacus-common</artifactId>
	<version>6.26.6</version> 
<dependency>
```

* Gradle:

```gradle
// JDK 17 or above:
compile 'com.landawn:abacus-common:6.26.6'
```


### Design and Implementation Considerations:

* In general, empty values—such as an empty `String`, `Collection`, or `Map`—are returned instead of `null`. However, certain methods (e.g., `Strings.firstNonEmpty()` or `Strings.emptyToNull()`) may legitimately return `null`. Methods that may return `null` are typically annotated with `@MayReturnNull` or explicitly documented in the Javadoc.


* Most methods are designed to support broad and general use cases. `null` parameters are generally permitted as long as they do not violate the method’s intended contract. For example: `Numbers.createNumber(...)` or `N.filter(...)`. It is the user’s responsibility to handle `null` values appropriately if they are considered invalid in a given context.

* Immutable vs. Mutable: Immutable objects are not preferred over mutable ones in this library. Mutable variables and parameters are generally used as the base cases. <u>*a),*</u> when a variable is passed as a method parameter, it should typically not be modified within the called method, unless it is explicitly intended to serve as an output parameter or modifying the input parameter is (part of) the purpose of the method call. <u>*b),*</u> a value returned from a method may be freely modified by the caller, as the caller is considered the owner/holder of the returned value. If the returned value is not meant to be modified by the caller, an immutable value should be returned instead. <u>*c),*</u> variables must not be modified concurrently by multiple threads without proper synchronization, regardless of whether they are mutable or immutable. In other words, if no modifications occur, it makes no difference whether the variables are mutable or immutable.

* Given the large number of methods across this library, maintaining strict consistency in handling exceptions such as `IllegalArgumentException`, `IndexOutOfBoundsException`,  `NullPointerException`, and similar is inherently challenging. Therefore, these exceptions should not be treated differently, and the following approach **must be avoided**:

```java
		try {
		    call some methods which may throw: IllegalArgumentException/IndexOutOfBoundsException/NullPointerException in this library.
		} catch (IllegalArgumentException e) {
		    // do something...
		} catch (IndexOutOfBoundsException e) {
		    // do something else...
		} catch (NullPointerException e) {
		    // otherwise...
		}
```


### Comparison with *Apache Commons Lang*, *Google Guava* and other libraries:

First of all, some code in *abacus-common* was originally derived from *Apache Commons Lang*, *Google Guava*, and other libraries under *the Apache License v2*.
In addition, *abacus-common* includes wrapper classes built on top of third-party APIs. Whether the code is adapted from existing libraries or implemented as wrappers(e.g., `OkHttpRequest/Hashing`), the primary goal is to provide a unified API and consistent programming experience through a cohesive and integrated design.  (*abacus-common could serve as a replacement for Apache Commons Lang or Google Guava in certain scenarios; however, this is not a recommendation. Developers are encouraged to choose the library that best fits their preferences and project requirements.*)

Secondly, although *abacus-common* includes thousands of public methods across dozens of classes and additional data structures, its APIs mainly focus on solving common everyday programming problems. The design centers around the most frequently used Java data structures, such as `String/Number/Collection/Map/Bean/Stream...`. APIs for HTTP web requests and `JSON/XML` serialization are also included because these are widely used in daily programming—much like collections.
Beyond that, the library provides a few small miscellaneous utility classes.

There are **no plans** to extend *abacus-common* into unrelated domains such as *compression*, *artificial intelligence*, or other specialized areas. The focus remains on providing a clean, consistent, and integrated utility foundation for general-purpose Java programming. The APIs are designed to remain stable and long-lasting, with no major changes anticipated in future releases, even though there have been numerous iterations and modifications in the past.


### Functional Programming:
To fully leverage abacus-common, familiarity with Java 8+ lambdas and the Stream API is recommended. Helpful resources include:

[What's New in Java 8](https://leanpub.com/whatsnewinjava8/read)

[An introduction to the java.util.stream library](https://www.ibm.com/developerworks/library/j-java-streams-1-brian-goetz/index.html)

[When to use parallel streams](http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html)

[Top Java 8 stream questions on stackoverflow](./Top_java_8_stream_questions_so.md)

[Kotlin vs Java 8 on Collection](./Java_Kotlin.md)



## Also See: [abacus-jdbc](https://github.com/landawn/abacus-jdbc), [abacus-query](https://github.com/landawn/abacus-query), [abacus-extra](https://github.com/landawn/abacus-extra), [abacus-cache](https://github.com/landawn/abacus-cache) and [abacus-android](https://github.com/landawn/abacus-android)


## Recommended Java programming libraries/frameworks:
[lombok](https://github.com/rzwitserloot/lombok), 
[Guava](https://github.com/google/guava), 
[protobuf](https://github.com/protocolbuffers/protobuf), 
[Kyro](https://github.com/EsotericSoftware/kryo), 
[snappy-java](https://github.com/xerial/snappy-java), 
[lz4-java](https://github.com/lz4/lz4-java), 
[Caffeine](https://github.com/ben-manes/caffeine), 
[Ehcache](http://www.ehcache.org/), 
[Chronicle-Map](https://github.com/OpenHFT/Chronicle-Map), 
[echarts](https://github.com/apache/incubator-echarts), 
[Chartjs](https://github.com/chartjs/Chart.js), 
[Highcharts](https://www.highcharts.com/blog/products/highcharts/), 
[Apache POI](https://github.com/apache/poi), 
[easyexcel](https://github.com/alibaba/easyexcel),
[opencsv](http://opencsv.sourceforge.net/), 
[mapstruct](https://github.com/mapstruct/mapstruct), 
[fastutil](https://github.com/vigna/fastutil), 
[hppc](https://github.com/carrotsearch/hppc), 
[re2j](https://github.com/google/re2j) ... [awesome-java](https://github.com/akullpp/awesome-java)

## Recommended Java programming tools:
[Spotbugs](https://github.com/spotbugs/spotbugs), [JaCoCo](https://www.eclemma.org/jacoco/)...


## 

If Proposals is slow with Big class: N, Strings, Stream, Open the class file and explore the methods in outline once. Then test again.
![image](https://github.com/landawn/abacus-common/assets/16568599/248990bb-f2c8-43e0-956b-edaa1477d5cd)

                               ...beyond imagination...







