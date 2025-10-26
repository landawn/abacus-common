
**Abacus-common** is a general-purpose Java programming library that provides a rich set of utilities, data structures, and functional APIs.

##### Key Features

1. *Frequently used utilities* — e.g., `Strings`, `Numbers`, `Maps`, `Joiner`, `Splitter`.
2. *Additional data structures* — `Dataset`, `Sheet`, `Multiset`, `Multimap`, `Pair`, `Tuple`, `Range`, `Fraction`.
3. *Comprehensive functional programming APIs* — including `Stream`, `EntryStream`, `Collectors`.

##### How it differs from other libraries

1. *Comprehensive and consistent*:
   abacus-common offers unified APIs across data types — for example, `N.isEmpty(String/byte[]/Collection/...)` — instead of relying on separate utilities like `StringUtils/Collections/MapUtils.isEmpty(...)` from different classes/libraries.

2. *Simple and concise*:
   All APIs are well organized and follow consistent design principles:
	* Always return an empty string or collection instead of `null`.
	* Use concise, consistent method names and parameter orders.
	* Ensure predictable and uniform behavior across components.
	* More...

3. *Powerful and extensive*:
   The library provides thousands of utility methods — covering everything from daily-use helpers to advanced support for I/O, concurrency, JSON/XML serialization, and functional programming.

##### Why Abacus-common?

Abacus-common is a *mega-library* unlike most others. It contains thousands of public methods across hundreds of classes, covering nearly all common programming use cases. You no longer need to search multiple libraries or maintain your own `StringUtils` or `CollectionUtils` — everything is conveniently available in one place with clean, consistent, and easy-to-use APIs.
