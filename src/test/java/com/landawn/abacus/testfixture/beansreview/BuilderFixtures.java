package com.landawn.abacus.testfixture.beansreview;

/**
 * Builder-based bean fixtures deliberately declared in a package other than {@code com.landawn.abacus.util}, so that
 * {@code Beans} cannot reach their non-public members without {@code setAccessible} (ledger C-403, 2026-09-24).
 * Used by {@code com.landawn.abacus.util.BeansReview20260924Test}.
 */
public final class BuilderFixtures {

    private BuilderFixtures() {
    }

    /** Package-private bean with a package-private nested builder (the {@code Beans.getBuilderInfo} javadoc shape). */
    public static Class<?> pkgPersonClass() {
        return PkgPerson.class;
    }

    /** Public bean with a package-private nested builder. */
    public static Class<?> pubPersonClass() {
        return PubPerson.class;
    }

    /** Package-private bean with a public nested builder (the Lombok {@code @Builder} shape). */
    public static Class<?> lombokPersonClass() {
        return LombokPerson.class;
    }
}

class PkgPerson {
    private final String name;
    private final int age;

    PkgPerson(final String name, final int age) {
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

        public PkgPerson build() {
            return new PkgPerson(name, age);
        }
    }
}

class LombokPerson {
    private final String name;
    private final int age;

    LombokPerson(final String name, final int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public static LombokPersonBuilder builder() {
        return new LombokPersonBuilder();
    }

    public static class LombokPersonBuilder {
        private String name;
        private int age;

        public LombokPersonBuilder name(final String name) {
            this.name = name;
            return this;
        }

        public LombokPersonBuilder age(final int age) {
            this.age = age;
            return this;
        }

        public LombokPerson build() {
            return new LombokPerson(name, age);
        }
    }
}
