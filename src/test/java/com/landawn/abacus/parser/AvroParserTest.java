package com.landawn.abacus.parser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.parser.AvroDeserializationConfig.ADC;
import com.landawn.abacus.parser.AvroSerializationConfig.ASC;
import com.landawn.abacus.parser.entity.User;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;

public class AvroParserTest {
    static final AvroParser avroParser = ParserFactory.createAvroParser();

    static final Schema schema;

    static {
        try {
            schema = new Schema.Parser().parse(new File("./src/test/resources/user.avsc"));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSerialize_0() throws Exception {
        User user1 = new User();
        user1.setName("Alyssa");
        user1.setFavoriteNumber(256);

        String str = avroParser.serialize(user1);

        N.println(str);

        GenericRecord xBean2 = avroParser.deserialize(str, User.class);
        N.println(xBean2);

        Map<String, Object> m = avroParser.deserialize(str, ADC.create().setSchema(schema), Map.class);
        N.println(m);

        User user2 = new User("Ben", 7, "red");
        User user3 = User.newBuilder().setName("Charlie").setFavoriteColor("blue").setFavoriteNumber(null).build();
        str = avroParser.serialize(N.asList(user1, user2, user3));

        N.println(str);

        AvroDeserializationConfig ds = ADC.create().setSchema(schema).setElementType(User.class);
        List<User> users = avroParser.deserialize(str, ds, List.class);
        N.println(users);

        ds = ADC.create().setElementType(User.class);
        users = avroParser.deserialize(str, ds, List.class);
        N.println(users);

    }

    @Test
    public void testSerialize_0_1() throws Exception {
        User user1 = new User();
        user1.setName("Alyssa");
        user1.setFavoriteNumber(256);
        File file = new File("./src/test/resources/test.avsc");
        avroParser.serialize(user1, file);

        N.println(IOUtil.readAllToString(file));

        GenericRecord xBean2 = avroParser.deserialize(file, User.class);
        N.println(xBean2);

        User user2 = new User("Ben", 7, "red");
        User user3 = User.newBuilder().setName("Charlie").setFavoriteColor("blue").setFavoriteNumber(null).build();
        avroParser.serialize(N.asList(user1, user2, user3), file);

        N.println(IOUtil.readAllToString(file));

        AvroDeserializationConfig ds = ADC.create().setSchema(schema).setElementType(User.class);
        List<User> users = avroParser.deserialize(file, ds, List.class);
        N.println(users);

        ds = ADC.create().setElementType(User.class);
        users = avroParser.deserialize(file, ds, List.class);
        N.println(users);

    }

    @Test
    public void testSerialize_1() throws Exception {
        GenericRecord user1 = new GenericData.Record(schema);
        user1.put("name", "Alyssa");
        user1.put("favorite_number", 256);

        AvroSerializationConfig sc = ASC.of(schema);
        String str = avroParser.serialize(user1, sc);
        N.println(str);

        Map<String, Object> user2 = new HashMap<>();
        user2.put("name", "Ben");
        user2.put("favorite_number", 7);
        user2.put("favorite_color", "red");

        sc = ASC.of(schema);
        str = avroParser.serialize(user2, sc);
        N.println(str);

        AvroDeserializationConfig ds = ADC.create().setSchema(schema);
        GenericRecord record = avroParser.deserialize(str, ds, GenericRecord.class);
        N.println(record);

        Map<String, Object> m = avroParser.deserialize(str, ds, Map.class);
        N.println(m);
    }
}
