package com.landawn.abacus.parser.entity;

@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class User extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
    public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse(
            "{\"type\":\"record\",\"name\":\"User\",\"namespace\":\"example.avro\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"favorite_number\",\"type\":[\"int\",\"null\"]},{\"name\":\"favorite_color\",\"type\":[\"string\",\"null\"]}]}");

    public static org.apache.avro.Schema getClassSchema() {
        return SCHEMA$;
    }

    @Deprecated
    public java.lang.CharSequence name;
    @Deprecated
    public java.lang.Integer favorite_number;
    @Deprecated
    public java.lang.CharSequence favorite_color;

    public User() {
    }

    public User(java.lang.CharSequence name, java.lang.Integer favorite_number, java.lang.CharSequence favorite_color) {
        this.name = name;
        this.favorite_number = favorite_number;
        this.favorite_color = favorite_color;
    }

    @Override
    public org.apache.avro.Schema getSchema() {
        return SCHEMA$;
    }

    @Override
    public java.lang.Object get(int field$) {
        switch (field$) {
            case 0:
                return name;
            case 1:
                return favorite_number;
            case 2:
                return favorite_color;
            default:
                throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public void put(int field$, java.lang.Object value$) {
        switch (field$) {
            case 0:
                name = (java.lang.CharSequence) value$;
                break;
            case 1:
                favorite_number = (java.lang.Integer) value$;
                break;
            case 2:
                favorite_color = (java.lang.CharSequence) value$;
                break;
            default:
                throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    public java.lang.CharSequence getName() {
        return name;
    }

    public void setName(java.lang.CharSequence value) {
        this.name = value;
    }

    public java.lang.Integer getFavoriteNumber() {
        return favorite_number;
    }

    public void setFavoriteNumber(java.lang.Integer value) {
        this.favorite_number = value;
    }

    public java.lang.CharSequence getFavoriteColor() {
        return favorite_color;
    }

    public void setFavoriteColor(java.lang.CharSequence value) {
        this.favorite_color = value;
    }

    public static com.landawn.abacus.parser.entity.User.Builder newBuilder() {
        return new com.landawn.abacus.parser.entity.User.Builder();
    }

    public static com.landawn.abacus.parser.entity.User.Builder newBuilder(com.landawn.abacus.parser.entity.User.Builder other) {
        return new com.landawn.abacus.parser.entity.User.Builder(other);
    }

    public static com.landawn.abacus.parser.entity.User.Builder newBuilder(com.landawn.abacus.parser.entity.User other) {
        return new com.landawn.abacus.parser.entity.User.Builder(other);
    }

    public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<User> implements org.apache.avro.data.RecordBuilder<User> {

        private java.lang.CharSequence name;
        private java.lang.Integer favorite_number;
        private java.lang.CharSequence favorite_color;

        private Builder() {
            super(com.landawn.abacus.parser.entity.User.SCHEMA$);
        }

        private Builder(com.landawn.abacus.parser.entity.User.Builder other) {
            super(other);
            if (isValidValue(fields()[0], other.name)) {
                this.name = data().deepCopy(fields()[0].schema(), other.name);
                fieldSetFlags()[0] = true;
            }
            if (isValidValue(fields()[1], other.favorite_number)) {
                this.favorite_number = data().deepCopy(fields()[1].schema(), other.favorite_number);
                fieldSetFlags()[1] = true;
            }
            if (isValidValue(fields()[2], other.favorite_color)) {
                this.favorite_color = data().deepCopy(fields()[2].schema(), other.favorite_color);
                fieldSetFlags()[2] = true;
            }
        }

        private Builder(com.landawn.abacus.parser.entity.User other) {
            super(com.landawn.abacus.parser.entity.User.SCHEMA$);
            if (isValidValue(fields()[0], other.name)) {
                this.name = data().deepCopy(fields()[0].schema(), other.name);
                fieldSetFlags()[0] = true;
            }
            if (isValidValue(fields()[1], other.favorite_number)) {
                this.favorite_number = data().deepCopy(fields()[1].schema(), other.favorite_number);
                fieldSetFlags()[1] = true;
            }
            if (isValidValue(fields()[2], other.favorite_color)) {
                this.favorite_color = data().deepCopy(fields()[2].schema(), other.favorite_color);
                fieldSetFlags()[2] = true;
            }
        }

        public java.lang.CharSequence getName() {
            return name;
        }

        public com.landawn.abacus.parser.entity.User.Builder setName(java.lang.CharSequence value) {
            validate(fields()[0], value);
            this.name = value;
            fieldSetFlags()[0] = true;
            return this;
        }

        public boolean hasName() {
            return fieldSetFlags()[0];
        }

        public com.landawn.abacus.parser.entity.User.Builder clearName() {
            name = null;
            fieldSetFlags()[0] = false;
            return this;
        }

        public java.lang.Integer getFavoriteNumber() {
            return favorite_number;
        }

        public com.landawn.abacus.parser.entity.User.Builder setFavoriteNumber(java.lang.Integer value) {
            validate(fields()[1], value);
            this.favorite_number = value;
            fieldSetFlags()[1] = true;
            return this;
        }

        public boolean hasFavoriteNumber() {
            return fieldSetFlags()[1];
        }

        public com.landawn.abacus.parser.entity.User.Builder clearFavoriteNumber() {
            favorite_number = null;
            fieldSetFlags()[1] = false;
            return this;
        }

        public java.lang.CharSequence getFavoriteColor() {
            return favorite_color;
        }

        public com.landawn.abacus.parser.entity.User.Builder setFavoriteColor(java.lang.CharSequence value) {
            validate(fields()[2], value);
            this.favorite_color = value;
            fieldSetFlags()[2] = true;
            return this;
        }

        public boolean hasFavoriteColor() {
            return fieldSetFlags()[2];
        }

        public com.landawn.abacus.parser.entity.User.Builder clearFavoriteColor() {
            favorite_color = null;
            fieldSetFlags()[2] = false;
            return this;
        }

        @Override
        public User build() {
            try {
                User record = new User();
                record.name = fieldSetFlags()[0] ? this.name : (java.lang.CharSequence) defaultValue(fields()[0]);
                record.favorite_number = fieldSetFlags()[1] ? this.favorite_number : (java.lang.Integer) defaultValue(fields()[1]);
                record.favorite_color = fieldSetFlags()[2] ? this.favorite_color : (java.lang.CharSequence) defaultValue(fields()[2]);
                return record;
            } catch (Exception e) {
                throw new org.apache.avro.AvroRuntimeException(e);
            }
        }
    }
}
