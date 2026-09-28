package com.landawn.abacus.testfixture.beansreview;

/**
 * A public builder-based bean whose nested builder class is package-private (ledger C-403, 2026-09-24): the builder's
 * public {@code build()} and setters are unreachable from another package without {@code setAccessible}.
 */
public class PubPerson {
    private final String name;
    private final int age;

    PubPerson(final String name, final int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private String name;
        private int age;

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder age(final int age) {
            this.age = age;
            return this;
        }

        public PubPerson build() {
            return new PubPerson(name, age);
        }
    }
}
