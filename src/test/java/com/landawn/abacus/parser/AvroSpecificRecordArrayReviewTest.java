package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.avro.Schema;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.entity.User;
import com.landawn.abacus.type.Type;

@Tag("unit")
class AvroSpecificRecordArrayReviewTest extends TestBase {
    @Test
    void nullableSpecificRecordArraysRoundTripWithDeclaredElementType() {
        AvroParser parser = new AvroParser();
        User user = new User("name", 7, "blue");
        Type<LinkedList<User>> target = Type.of("LinkedList<" + User.class.getName() + ">");

        for (boolean nullFirst : new boolean[] { true, false }) {
            Schema nullSchema = Schema.create(Schema.Type.NULL);
            Schema elementSchema = nullFirst ? Schema.createUnion(nullSchema, user.getSchema()) : Schema.createUnion(user.getSchema(), nullSchema);
            Schema schema = Schema.createArray(elementSchema);
            String encoded = parser.serialize(Arrays.asList(null, user, null), AvroSerConfig.create().setSchema(schema));

            LinkedList<User> result = parser.deserialize(encoded, null, target);
            assertEquals(3, result.size());
            assertNull(result.getFirst());
            assertEquals(user, result.get(1));
            assertNull(result.getLast());
        }
    }

    @Test
    void nullableSpecificRecordArraysHonorConfiguredElementType() {
        AvroParser parser = new AvroParser();
        Schema schema = Schema.createArray(Schema.createUnion(Schema.create(Schema.Type.NULL), User.getClassSchema()));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        User user = new User("configured", null, null);
        parser.serialize(Arrays.asList(user, null), AvroSerConfig.create().setSchema(schema), output);

        List<?> result = parser.deserialize(new ByteArrayInputStream(output.toByteArray()), AvroDeserConfig.create().setElementType(User.class), List.class);
        assertEquals(user, assertInstanceOf(User.class, result.getFirst()));
        assertNull(result.getLast());
    }
}
