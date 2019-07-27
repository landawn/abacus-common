### [Java 8 `List<V>` into `Map<K, V>`](https://stackoverflow.com/questions/20363719/java-8-listv-into-mapk-v)

* By Java 8
```java
// if choices are unique by name.
Map<String, Choice> result = choices.stream()
                                    .collect(Collectors.toMap(Choice::getName, Function.identity()));
// owtherwise group by
Map<String, List<Choice>> result = choices.stream()
                                          .collect(Collectors.groupingBy(Choice::getName));
```
* By Abacus-Util
```java
Map<String, Choice> result = Stream.of(choices).toMap(Choice::getName, Fn.identity());

// group by
Map<String, List<Choice>> result = Stream.of(choices).groupTo(Choice::getName);
```
---
###[`Map<String,List<String>>` to `Pair<String,String>`](https://stackoverflow.com/questions/32189147/mapstring-liststring-to-pairstring-string)

* By Java 8
```java
List<Pair<String, String>> result =
    map.entrySet()
       .stream()
       .flatMap(
           entry -> entry.getValue()
                         .stream()
                         .map(string -> new Pair<>(entry.getKey(), string)))
       .collect(Collectors.toList());
```

* By Abacus-Util
```java
List<Pair<String, String>> result = Stream.of(map)
        .flatMap(e -> Stream.of(e.getValue()).map(v -> Pair.of(e.getKey(), v))).toList();
```

---
### [Retrieving a List from a java.util.stream.Stream in Java 8](https://stackoverflow.com/questions/14830313/retrieving-a-list-from-a-java-util-stream-stream-in-java-8)

* By Java 8
```java
targetLongList = sourceLongList.stream()
                               .filter(l -> l > 100)
                               .collect(Collectors.toList());
```

* By Abacus-Util
```java
targetLongList = Stream.of(sourceLongList).filter(l -> l > 100).toList();
```

---
### [Custom thread pool in Java 8 parallel stream](https://stackoverflow.com/questions/21163108/custom-thread-pool-in-java-8-parallel-stream)

* By Java 8
```java
???
```

* By Abacus-Util
```java
stream.parallel(threadNum);
```

---
### [Is there a concise way to iterate over a stream with indices in Java 8?](https://stackoverflow.com/questions/18552005/is-there-a-concise-way-to-iterate-over-a-stream-with-indices-in-java-8)

* By Java 8
```java
String[] names = {"Sam", "Pamela", "Dave", "Pascal", "Erik"};
IntStream.range(0, names.length)
         .filter(i -> names[i].length() <= i)
         .mapToObj(i -> names[i])
         .collect(Collectors.toList());
```

* By Abacus-Util
```java
String[] names = {"Sam", "Pamela", "Dave", "Pascal", "Erik"};
IntStream.range(0, names.length)
         .filter(i -> names[i].length() <= i)
         .mapToObj(i -> names[i])
         .toList();

// Or: indexed for any type of collection/iterator.
Stream.of(collection).indexed()...;
```

---
### [How can I throw CHECKED exceptions from inside Java 8 streams? (Not wrapping it into unchecked exceptions)](https://stackoverflow.com/questions/27644361/how-can-i-throw-checked-exceptions-from-inside-java-8-streams-not-wrapping-it)

* By Java 8
```java
Stream.of("java.lang.Object", "java.lang.Integer", "java.lang.String")
              .map(className -> try {
                            Class.forName(className))
                        } catch (ClassNotFoundException ) {
                            throw new RuntimeException(e);
                        }
              .collect(Collectors.toList());
```

* By Abacus-Util
```java
Stream.of("java.lang.Object", "java.lang.Integer", "java.lang.String")
              .map(Fn.ff(className -> Class.forName(className)))
              .toList();
```

---
### [Using Java 8's Optional with Stream::flatMap](https://stackoverflow.com/questions/22725537/using-java-8s-optional-with-streamflatmap)

* By Java 8
```java
Optional<Other> result =
    things.stream()
          .map(this::resolve)
          .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())
          .findFirst();
```

* By Abacus-Util
```java
Optional<Other> result =
    things.stream()
          .map(this::resolve)
          .flatMap(Optional::stream)
          .first();
```

---
### [Limit a stream by a predicate](https://stackoverflow.com/questions/20746429/limit-a-stream-by-a-predicate)

* By Java 8
```java
// ??? No easy way to do it until Java 9
IntStream
    .iterate(1, n -> n + 1)
    .takeWhile(n -> n < 10)
    .forEach(System.out::println);
```

* By Abacus-Util
```java
IntStream
    .iterate(1, n -> n + 1)
    .takeWhile(n -> n < 10)
    .forEach(Fn.println);
```

---
### [Java 8 NullPointerException in Collectors.toMap](https://stackoverflow.com/questions/24630963/java-8-nullpointerexception-in-collectors-tomap)

* By Java 8
```java
List<Answer> answerList = new ArrayList<>();
answerList.add(new Answer(1, true));
answerList.add(new Answer(2, true));
answerList.add(new Answer(3, null));

Map<Integer, Boolean> answerMap = answerList.stream()
          .collect(Collectors.toMap(Answer::getId, Answer::getAnswer)); // throw NullPointerException
```

* By Abacus-Util
```java
// Works well.
Map<Integer, Boolean> answerMap = answerList.stream().toMap(Answer::getId, Answer::getAnswer);
```

---
### [Adding two Java 8 streams, or an extra element to a stream](https://stackoverflow.com/questions/22740464/adding-two-java-8-streams-or-an-extra-element-to-a-stream)

* By Java 8
```java
Stream stream = Stream.concat(
                       Stream.concat(
                              stream1.filter(x -> x!=0), stream2)
                              .filter(x -> x!=1),
                                  Stream.of(element))
                                  .filter(x -> x!=2);
```

* By Abacus-Util
```java
stream1.append(stream2).append(element);
```

---
### [Java8: HashMap<X, Y> to HashMap<X, Z> using Stream / Map-Reduce / Collector](https://stackoverflow.com/questions/25903137/java8-hashmapx-y-to-hashmapx-z-using-stream-map-reduce-collector)

* By Java 8
```java
Map<String, String> x;
Map<String, Integer> y =
    x.entrySet().stream()
        .collect(Collectors.toMap(
            e -> e.getKey(),
            e -> Integer.parseInt(e.getValue())
        ));
```

* By Abacus-Util
```java
Map<String, Integer> y = Stream.of(x).toMap(e -> e.getKey(), e -> Integer.parseInt(e.getValue()));
```

---
### [Does Java SE 8 have Pairs or Tuples?](https://stackoverflow.com/questions/24328679/does-java-se-8-have-pairs-or-tuples)

* By Java 8
```java
???
```

* By Abacus-Util

Yes, with the most beautiful design: 
[Pair](https://static.javadoc.io/com.landawn/abacus-util-all/1.0.6/com/landawn/abacus/util/Pair.html), [Triple](https://static.javadoc.io/com.landawn/abacus-util-all/1.0.6/com/landawn/abacus/util/Triple.html), [Tuple](https://static.javadoc.io/com.landawn/abacus-util-all/1.0.6/com/landawn/abacus/util/Tuple.html)


---
### [Filter Java Stream to 1 and only 1 element](https://stackoverflow.com/questions/22694884/filter-java-stream-to-1-and-only-1-element)

* By Java 8
```java
List<User> resultUserList = users.stream()
        .filter(user -> user.getId() == 1)
        .limit(2)
        .collect(Collectors.toList());
if (resultUserList.size() != 1) {
    throw new IllegalStateException();
}
User resultUser = resultUserList.get(0);

// Or by singletonCollector()
public static <T> Collector<T, ?, T> singletonCollector() {
    return Collectors.collectingAndThen(
            Collectors.toList(),
            list -> {
                if (list.size() != 1) {
                    throw new IllegalStateException();
                }
                return list.get(0);
            }
    );
}
```

* By Abacus-Util
```java
users.stream().filter(user -> user.getId() == 1).onlyOne();
```

---
### [What Java 8 Stream.collect equivalents are available in the standard Kotlin library?](https://stackoverflow.com/questions/34642254/what-java-8-stream-collect-equivalents-are-available-in-the-standard-kotlin-libr)

* By Java 8
```java
// Answers are included in the above link.
```

* By Abacus-Util

[Kotlin vs Java 8 on Collection](./Java_Kotlin.md)


---
### [Zipping streams using JDK8 with lambda (java.util.stream.Streams.zip)](https://stackoverflow.com/questions/17640754/zipping-streams-using-jdk8-with-lambda-java-util-stream-streams-zip)

* By Java 8
```java
???
```

* By Abacus-Util
```java
String[] a = {"a", "b", "c"};
String[] b = {"1", "2", "3"};
Stream.zip(a, b, Pair::of)...;
```

---
### [Ignore duplicates when producing map using streams](https://stackoverflow.com/questions/32312876/ignore-duplicates-when-producing-map-using-streams)

* By Java 8
```java
Map<String, String> phoneBook = 
    people.stream()
          .collect(Collectors.toMap(
             Person::getName,
             Person::getAddress,
             (address1, address2) -> {
                 System.out.println("duplicate key found!");
                 return address1;
             }
          ));
```

* By Abacus-Util
```java
Map<String, String> phoneBook = people.stream().toMap(Person::getName, Person::getAddress, Fn.ignoringMerger()); // Fn.replacingMerger()
```

---
### [Is it possible to cast a Stream in Java 8?](https://stackoverflow.com/questions/22511750/is-it-possible-to-cast-a-stream-in-java-8)

* By Java 8
```java
Stream.of(objects).filter(c -> c instanceof Client)
    .map(c -> ((Client) c).getID()).forEach(System.out::println);
```

* By Abacus-Util
```java
Stream.of(objects).select(Client.class).forEach(Fn.println);
```

---
### [Java 8: How do I work with exception throwing methods in streams?](https://stackoverflow.com/questions/23548589/java-8-how-do-i-work-with-exception-throwing-methods-in-streams)

* By Java 8
```java
stream.forEach(a -> safeFoo(a));
  
private void safeFoo(final A a) {
    try {
        a.foo();
    } catch (Exception ex) {
        throw new RuntimeException(ex);
    }
}
```

* By Abacus-Util
```java
stream.forEach(a -> Try.run(() -> a.foo()));
```

---
### [Java 8 stream reverse order](https://stackoverflow.com/questions/24010109/java-8-stream-reverse-order/24011264#24011264)

* By Java 8
```java
???
```

* By Abacus-Util
```java
stream.reversed()...
IntStream.of(1, 5, 3).reverseSorted()...
```

---
### [Collect successive pairs from a stream](https://stackoverflow.com/questions/20470010/collect-successive-pairs-from-a-stream)

* By Java 8
```java
// For Array/List only.
IntStream.range(1, arrayList.size())
             .mapToObj(i -> new Pair(arrayList.get(i-1), arrayList.get(i)))
             .forEach(System.out::println);
// There is no general solution Stream created from iterator/collection..
```

* By Abacus-Util
```java
// For any stream created from iterator/Collection/Array/List...
stream().slidingMap(Pair::of).forEach(Fn.println());
```

---
### [Most efficient way to get the last element of a stream](https://stackoverflow.com/questions/27547519/most-efficient-way-to-get-the-last-element-of-a-stream)

* By Java 8
```java
T last = stream.reduce((a, b) -> b).orElse(null);
```

* By Abacus-Util
```java
T last = stream.last().orElse(null);
```

---
### [Java 8 Stream with batch processing](https://stackoverflow.com/questions/30641383/java-8-stream-with-batch-processing)

* By Java 8
```java
???
```

* By Abacus-Util
```java
stream.split(batchSize)...
```

---
### [Merging two Map<String, Integer> with Java 8 Stream API](https://stackoverflow.com/questions/23038673/merging-two-mapstring-integer-with-java-8-stream-api)

* By Java 8
```java
Map<String, Integer> m1 = ImmutableMap.of("a", 2, "b", 3);
Map<String, Integer> m2 = ImmutableMap.of("a", 3, "c", 4);

Map<String, Integer> mx = Stream.of(m1, m2)
    .map(Map::entrySet)          // converts each map into an entry set
    .flatMap(Collection::stream) // converts each set into an entry stream, then
                                 // "concatenates" it in place of the original set
    .collect(
        Collectors.toMap(        // collects into a map
            Map.Entry::getKey,   // where each entry is based
            Map.Entry::getValue, // on the entries in the stream
            Integer::max         // such that if a value already exist for
                                 // a given key, the max of the old
                                 // and new value is taken
        )
    );
```

* By Abacus-Util
```java
Stream.of(m1).append(m2.entrySet()).toMap(Fn.key(), Fn.value(), Integer::max);
```

---
### [Take every nth element from a Java 8 stream](https://stackoverflow.com/questions/31602425/take-every-nth-element-from-a-java-8-stream)

* By Java 8
```java
List<String> list = ...;
return IntStream.range(0, list.size())
    .filter(n -> n % 3 == 0)
    .mapToObj(list::get)
    .collect(Collectors.toList());
```

* By Abacus-Util
```java
Stream.of(list).step(3).toList();
```

---
### [Java 8, Streams to find the duplicate elements](https://stackoverflow.com/questions/27677256/java-8-streams-to-find-the-duplicate-elements)

* By Java 8
```java
Integer[] numbers = new Integer[] { 1, 2, 1, 3, 4, 4 };
Set<Integer> allItems = new HashSet<>();
Set<Integer> duplicates = Arrays.stream(numbers)
        .filter(n -> !allItems.add(n)) //Set.add() returns false if the item was already in the set.
        .collect(Collectors.toSet());
```

* By Abacus-Util
```java
Stream.of(numbers).groupByToEntry(Fn.identity(), Fn.counting()).filterByValue(occur -> occur > 1).keys().toList();
// Or:
Multiset.of(numbers).entryStream().filterByValue(occur -> occur > 1).keys().toList();
```

---
### [How to map to multiple elements with Java 8 streams?](https://stackoverflow.com/questions/23620360/how-to-map-to-multiple-elements-with-java-8-streams)

* By Java 8
```java
Collection<DataSet> convert(List<MultiDataPoint> multiDataPoints) {
    Map<String, DataSet> result = new HashMap<>();
    multiDataPoints.forEach(pt ->
        pt.keyToData.forEach((key, value) ->
            result.computeIfAbsent(
                key, k -> new DataSet(k, new ArrayList<>()))
            .dataPoints.add(new DataPoint(pt.timestamp, value))));
    return result.values();
}

// Or:
Collection<DataSet> convert(List<MultiDataPoint> multiDataPoints) {
    return multiDataPoints.stream()
        .flatMap(mdp -> mdp.keyToData.entrySet().stream().map(e ->
            new Object() {
                String key = e.getKey();
                DataPoint dataPoint = new DataPoint(mdp.timestamp, e.getValue());
            }))
        .collect(
            collectingAndThen(
                groupingBy(t -> t.key, mapping(t -> t.dataPoint, toList())),
                m -> m.entrySet().stream().map(e -> new DataSet(e.getKey(), e.getValue())).collect(toList())));
}
```

* By Abacus-Util
```java
Stream.of(multiDataPoints)
    .flatMap(mdp -> Stream.of(mdp.keyToData).map(e -> Pair.of(e.getKey(), new DataPoint(mdp.timestamp, e.getValue()))))
    .groupBy(Entry::getKey, Entry::getValue).map(e -> new DataSet(e.getKey(), e.getValue())).toList();
```

---
### [Simplest way to print an `IntStream` as a `String`](https://stackoverflow.com/questions/20266422/simplest-way-to-print-an-intstream-as-a-string)

* By Java 8
```java
String result = "Hello world."
  .codePoints()
//.parallel()  // uncomment this line for large strings
  .map(c -> c == ' ' ? ' ': '*')
  .collect(StringBuilder::new,
           StringBuilder::appendCodePoint, StringBuilder::append)
  .toString();
```

* By Abacus-Util
```java
CharStream.of(result).map(c -> c == ' ' ? ' ': '*').println();
```

---
### [java.util.stream with ResultSet](https://stackoverflow.com/questions/32209248/java-util-stream-with-resultset)

* By Java 8
```java
???
```

* By Abacus-Util
```java
Stream.of(resultSet)...
// Or:
Stream.of(entityClass, resultSet)...
//Or:
sqlExecutor.stream(sql, parameters);
```

---
### [How to use streams to find pairs of elements from two lists or array multiplication](https://stackoverflow.com/questions/42220047/how-to-use-streams-to-find-pairs-of-elements-from-two-lists-or-array-multiplicat)

* By Java 8
```java
int[] one = new int[]{1, 2, 3};
int[] two = new int[]{3, 4};
List<IntIntPair> list = new ArrayList<>();
IntStream.of(one).forEach(i ->
        IntStream.of(two).mapToObj(j -> PrimitiveTuples.pair(i, j)).forEach(list::add));
System.out.println(list);
```

* By Abacus-Util
```java
Stream.of(1, 2, 3).cartesianProduct(Arrays.asList(3, 4)).forEach(Fn.println());
```

