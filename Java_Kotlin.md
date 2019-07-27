Motivated by [What Java 8 Stream.collect equivalents are available in the standard Kotlin library?][1]

* **Accumulate names in a List**

```java
// Java:  
List<String> list = people.stream().map(Person::getName).collect(Collectors.toList());

// Kotlin:
val list = people.map { it.name }  // toList() not needed

// Java by Abacus-Util
List<String> list = Seq.of(people).map(Person::getName);
// Or:
List<String> list = Stream.of(people).map(Person::getName).toList();
```

***

* **Collect example #5 - find people of legal age, output formatted string**

```java
// Java:
String phrase = persons
        .stream()
        .filter(p -> p.age >= 18)
        .map(p -> p.name)
        .collect(Collectors.joining(" and ", "In Germany ", " are of legal age."));

// Kotlin:
val phrase = persons
        .filter { it.age >= 18 }
        .map { it.name }
        .joinToString(" and ", "In Germany ", " are of legal age.")

// Java by Abacus-Util
String phrase = Stream.of(persons)
        .filter(p -> p.age >= 18)
        .map(p -> p.name)
        .join(" and ", "In Germany ", " are of legal age.");
```

***

* **Counting items in a list after filter is applied**

```java
// Java:
long count = items.stream().filter( item -> item.startsWith("t")).count();
item -> item.startsWith("t")

// Kotlin:
val count = items.filter { it.startsWith('t') }.size
// but better to not filter, but count with a predicate
val count = items.count { it.startsWith('t') }

// Java by Abacus-Util
int count = Seq.of(items).count(item -> item.startsWith("t"));
// Or:
int count = Seq.of(items).count(Fn.startsWith("t"));
```

* **Collect example #6 - group people by age, print age and names together**
```java
// Java:
Map<Integer, String> map = persons
        .stream()
        .collect(Collectors.toMap(
                p -> p.age,
                p -> p.name,
                (name1, name2) -> name1 + ";" + name2));

System.out.println(map);
// {18=Max, 23=Peter;Pamela, 12=David}  

// Kotlin:
val map1 = persons.map { it.age to it.name }.toMap()
// output: {18=Max, 23=Pamela, 12=David} 
// Result: duplicates overridden, no exception similar to Java 8

val map2 = persons.toMap({ it.age }, { it.name })
// output: {18=Max, 23=Pamela, 12=David} 
// Result: same as above, more verbose, duplicates overridden

val map3 = persons.toMapBy { it.age }
// output: {18=Person(name=Max, age=18), 23=Person(name=Pamela, age=23), 12=Person(name=David, age=12)}
// Result: duplicates overridden again

val map4 = persons.groupBy { it.age }
// output: {18=[Person(name=Max, age=18)], 23=[Person(name=Peter, age=23), Person(name=Pamela, age=23)], 12=[Person(name=David, age=12)]}
// Result: closer, but now have a Map<Int, List<Person>> instead of Map<Int, String>

val map5 = persons.groupBy { it.age }.mapValues { it.value.map { it.name } }
// output: {18=[Max], 23=[Peter, Pamela], 12=[David]}

// Result: closer, but now have a Map<Int, List<String>> instead of Map<Int, String>
// And now for the correct answer:
// Kotlin:
val map6 = persons.groupBy { it.age }.mapValues { it.value.joinToString(";") { it.name } }
// output: {18=Max, 23=Peter;Pamela, 12=David}

// Java by Abacus-Util
Map<Integer, String> map = Stream.of(persons).toMap(p -> p.age, p -> p.name, Fn.replacingMerger());
// {18=Max, 23=Pamela, 12=David} 

Map<Integer, String> map = Stream.of(persons).toMap(p -> p.age, p -> p.name, (a, b) -> a + ";" + b);
// {18=Max, 23=Peter;Pamela, 12=David}

Map<Integer, List<Person>> map = Stream.of(persons).groupTo(p -> p.age);
// {18=[Person(name=Max, age=18)], 23=[Person(name=Peter, age=23), Person(name=Pamela, age=23)], 12=[Person(name=David, age=12)]}

Map<Integer, List<String>> map = Stream.of(persons).groupTo(p -> p.age, p -> p.name);
// {18=[Max], 23=[Peter, Pamela], 12=[David]}
```


* **Compute sum of salaries by department**
```java
// Java:
Map<Department, Integer> totalByDept
     = employees.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment,
                     Collectors.summingInt(Employee::getSalary)));
// Kotlin:
val totalByDept = employees.groupBy { it.dept }.mapValues { it.value.sumBy { it.salary }}

// Java by Abacus-Util
Map<Department, Integer> totalByDept = Stream.of(employees).toMap(Employee::getDepartment, Fn.summingInt(Employee::getSalary));

```

* **Convert elements to strings and concatenate them, separated by commas**
```java
// Java:
String joined = things.stream()
                       .map(Object::toString)
                       .collect(Collectors.joining(", "));
// Kotlin:
val joined = things.joinToString() // ", " is used as separator, by default

// Java by Abacus-Util
String joined = Stream.of(things).join(", ");
// Or:
String joined = Joiner.defauLt().join(things);
```

* **Different Kinds of Streams #1 - eager using first item if it exists**
```java
// Java:
Arrays.asList("a1", "a2", "a3")
    .stream()
    .findFirst()
    .ifPresent(System.out::println);
    
// Kotlin:
listOf("a1", "a2", "a3").firstOrNull()?.apply(::println)

// Java by Abacus-Util
Stream.of("a1", "a2", "a3").first().ifPresent(Fn.println());
```

* **Group names of members in roster by gender**
```java
// Java:
Map<Person.Sex, List<String>> namesByGender =
      roster.stream().collect(
        Collectors.groupingBy(
            Person::getGender,                      
            Collectors.mapping(
                Person::getName,
                Collectors.toList())));
// Kotlin:
val namesByGender = roster.groupBy { it.gender }.mapValues { it.value.map { it.name } } 

// Java by Abacus-Util
Map<Person.Sex, List<String>> namesByGender = Stream.of(roster).groupTo(Person::getGender, Person::getName);
```

[1]: https://stackoverflow.com/questions/34642254/what-java-8-stream-collect-equivalents-are-available-in-the-standard-kotlin-libr
