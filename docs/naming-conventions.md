# Abacus — Public API Naming Conventions & Style

> **Audience:** contributors and maintainers of `abacus-core`.
> **Purpose:** capture the *house style* that the public API already follows so it
> stops drifting at the seams between class families. This is a description of the
> existing conventions first, and a set of rules for *new* code second.

## Prime directive: this is a published library

Every public type, method, and parameter name is part of a compatibility contract.
**Do not rename or remove public API.** When a name needs to change, add the new
name and mark the old one `@Deprecated` (see [Evolving the API](#evolving-the-api)).
The rules below govern what to name *new* API and how to converge old API safely —
never a bulk rename.

---

## 1. Two guiding principles

### 1.1 Family parallelism

When a concept exists for a *family* of types or arities, the names must be
**identical across the family except for the type token**. The primitive list
family (`IntList`, `LongList`, … `BooleanList`), the functional-interface family
(`IntFunction`, `ToLongFunction`, …), and the `Type<T>` family are the model here —
follow them.

- A new operation added to `IntList` must be added, with the same name and the same
  parameter order, to every sibling for which it is meaningful.
- A gap is acceptable **only** when the operation is semantically undefined for that
  type (e.g. no `min`/`max`/`range` on `BooleanList`; widening-only `toXxxList`
  conversions). Document the gap; don't leave it to be guessed.

### 1.2 One verb, one meaning

A verb prefix maps to exactly one semantic across the whole library. A reader who
has learned what `check*` or `to*` means in one class must be able to rely on it
everywhere. The dictionary below is the source of truth.

---

## 2. Verb / prefix dictionary

| Prefix | Meaning | Returns | Examples | Notes |
|---|---|---|---|---|
| `isXxx`, `hasXxx`, `canXxx` | side-effect-free boolean query | `boolean` | `isEmpty`, `isPrimitiveType`, `hasDuplicates` | The **only** prefixes that may return a bare boolean answer. |
| `checkXxx` | validate a precondition; **throw** on failure | `void` or the validated argument | `checkArgNotNull`, `checkArgument`, `checkFromToIndex` | **Never** return a plain boolean answer from a `check*` method. |
| `toXxx` | convert the receiver/argument to a new representation | the type named by `Xxx` | `toInt`, `toList`, `toArray`, `toCamelCase` | The suffix **encodes the result type**. `toInt` → `int`, `toInteger` → `Integer`. |
| `parseXxx` | parse text → primitive/value | primitive or value | `parseInt`, `parseBoolean` | Text-input sibling of `toXxx`; prefer `parseXxx` for the primitive parsers. |
| `valueOf` | parse/convert a single value → `T` | `T` | `Type#valueOf`, `Strings#valueOf` | JDK-aligned single-value factory. |
| `of` | factory **from explicit elements / a literal** | the constructed type | `List.of`-style, `Pair.of`, `Tuple.of` | Preferred modern factory for value/literal construction. |
| `from` | factory by **adapting or deserializing another representation** | the constructed type | `fromJson`, `fromXml`, `fromCollection`, `fromMap` | Use when the input *is* another form of the same data. |
| `newXxx` | construct a **fresh, empty/sized, mutable** instance | the new instance | `newArrayList`, `newLinkedHashMap`, `newBufferedReader` | "Give me a new one." Distinct from `of`/`as` (from elements). |
| `asXxx` | build-from-elements **or** lightweight view/wrapper | collection/view | `asList`, `asMap`, `asSingletonList`, `asReversed` | Document which: literal-builder vs. wrapping view. |
| `getXxx` / `setXxx` | property accessor / mutator | property type / `void` or `this` | `getCreatedTime`, `setLiveTime` | Standard bean accessors for stateful objects. |
| `withXxx` | copy-with-change (immutable) | a new instance | `withZone` | Use only for immutable copy semantics; do **not** mix with `setXxx` on the same type. |

### Reserved / restricted prefixes

- **`createXxx`** — reserved for two existing meanings only: **(a)** pool borrow /
  recycle (`Objectory`), and **(b)** legacy Apache-Commons-compatible factories
  (`Numbers#createInteger`, …). **Do not use `create*` for new API** — choose
  `of` / `from` / `new` / `valueOf` per the rules above.

---

## 3. Boolean naming

- Single-subject predicates use `is` / `has` / `can`: `isEmpty`, `hasNext`.
- Collective predicates may use the recognized `all*` / `any* `/ `none*` forms:
  `allEmpty`, `anyNull`, `noneMatch`. These are an accepted exception to the
  `is`/`has` rule.
- Equality/relation predicates keep their conventional names: `equals`,
  `deepEquals`, `contentEquals`, `disjoint`, `startsWith`.
- **Comparison predicates: prefer spelled-out names** — `lessThan`,
  `lessThanOrEqual`, `greaterThan`, `greaterThanOrEqual`. Avoid the abbreviated
  `lt`/`le`/`gt`/`ge` and the abbreviated range forms (`gtAndLt`, `geAndLe`) for new
  API; if a range predicate is needed, prefer a readable name such as
  `isBetween` / `isBetweenInclusive`.

---

## 4. Factory & conversion — quick decision guide

```
Need to construct/convert something? Pick the prefix by INPUT:

  explicit elements / a literal value ........... of(...)
  a single value to be parsed/converted ......... valueOf(...)   (or toXxx for primitives)
  text to a primitive ........................... parseXxx(...)
  another representation (json/xml/collection) .. from... / fromXxx(...)
  nothing — I want a fresh empty/sized instance . newXxx(...)
  a lightweight view over existing data ......... asXxx(...)

  (pool borrow or Apache-compat) ................ create...   [existing API only]
```

The result type is always encoded by the suffix for `toXxx` (`toInt` → `int`,
`toInteger` → `Integer`). Do not give two methods the same name but different
return types in sibling utility classes.

---

## 5. Fluent builders

A class may use **either** prefix-less fluent setters (`connectTimeout(...)`)
**or** `setXxx(...)` returning `this` — but **one style per class**, and ideally one
style per package. New fluent builders should prefer the prefix-less form
(`connectTimeout`, `header`, `readTimeout`) to match the modern `*Request` builders.

---

## 6. Class naming

- Types are `PascalCase` nouns.
- **Static-utility holders are pluralized:** `Strings`, `Numbers`, `Dates`, `Maps`,
  `Iterables`, `Iterators`, `Comparators`, `Suppliers`. New utility classes should
  follow this (`Foos`, not `FooUtil`/`FooUtils`, unless matching an existing sibling).
- Exception types end in `Exception`.
- **The terse facades (`N`, `u`, `cs`, `Fn`, `Fnn`) are intentional and frozen.**
  They optimize call-site brevity (`N.isEmpty(x)`) and namespace nested types
  (`u.Optional`). Do **not** add *new* one/two-letter public class names — new
  classes get descriptive names.

---

## 7. Parameter naming canon

Consistent parameter names keep the generated Javadoc and IDE hints uniform. Use the
established names:

| Concept | Name |
|---|---|
| `String` | `str` |
| array | `a` |
| `Collection` | `c` (or `coll`) |
| `Map` | `m` |
| `Iterator` / `Iterable` | `iter` |
| `Comparator` | `cmp` |
| element being searched for | `valueToFind` |
| index range (inclusive/exclusive) | `fromIndex`, `toIndex` |
| fallback value | `defaultValue` |
| function arguments | `predicate`, `mapper`, `action`, `supplier` (or `xxxSupplier`, e.g. `mapSupplier`) |

---

## 8. Anti-patterns (don't)

- **Don't disambiguate overloads by letter-case or letter-count.** `flatMap`
  vs. `flatmap` vs. `flattMap`, or `forEach` vs. `foreach`, are invisible in speech,
  trivially mistyped, and sort adjacently in autocomplete. Use distinct words
  instead (e.g. a Collection-returning variant → `flatMapToColl`; a
  `java.util.stream`-returning variant → `flatMapToJdkStream`).
- **Don't use a prefix against its meaning** — e.g. a `check*` method that returns a
  boolean instead of throwing.
- **Don't coin a second name for an existing concept.** Reuse the existing name; if a
  rename is truly warranted, alias + deprecate (§9).
- **Don't use the `2` digit for "to"** (`string2Array`). Use camelCase `To`
  (`stringToArray`) — the `type` package is the model (`collectionToArray`).
- **Don't add `create*` for new factories** (see §2).

---

## 9. Evolving the API

To change a name without breaking callers:

1. Add the new, convention-correct method.
2. Re-implement the old method to delegate to the new one.
3. Mark the old method `@Deprecated` with `@deprecated use {@link #newName} instead`.
4. Keep the deprecated method indefinitely (or until a documented major version).

Never hard-rename, change a return type, or delete a public method in a minor/patch
release.

---

## 10. Known intentional exceptions

These deviate from the rules above **on purpose**. They are documented here so they
are not "corrected" by a future cleanup:

- **Terse facade classes** `N`, `u`, `cs`, `Fn`, `Fnn` — see §6.
- **`Objectory.createXxx(...)`** — `create*` here means "borrow from the object pool",
  deliberately distinct from `IOUtil`/`CommonUtil` `newXxx(...)` which constructs a
  fresh instance.
- **Apache-Commons-compatible names** in `Numbers` (`createInteger`, `createNumber`,
  …) — kept for drop-in source compatibility with `org.apache.commons.lang3`.
- **`java.net`-mirrored names** in `http` (`doInput`, `doOutput`, `useCaches`) — kept
  to mirror `HttpURLConnection`.
- **`@Beta` stream variants** (`foreach`, `flattMap`, `onEach`, …) — experimental; the
  case/letter-count distinctions in §8 apply to these, and they are the first
  candidates to be renamed before they leave `@Beta`.

---

*See also the package summary in
`com.landawn.abacus.util` (`package-info.java`).*
