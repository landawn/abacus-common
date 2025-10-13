package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class BSONObjectIdType100Test extends TestBase {

    private BSONObjectIdType type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = (BSONObjectIdType) createType("BSONObjectId");
        writer = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        Class<ObjectId> result = type.clazz();
        assertEquals(ObjectId.class, result);
    }

    @Test
    public void testStringOf_ValidObjectId() {
        ObjectId objectId = new ObjectId();
        String result = type.stringOf(objectId);
        Assertions.assertNotNull(result);
        assertEquals(objectId.toHexString(), result);
        assertEquals(24, result.length());
    }

    @Test
    public void testStringOf_Null() {
        String result = type.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_ValidHexString() {
        String hexString = "507f1f77bcf86cd799439011";
        ObjectId result = type.valueOf(hexString);
        Assertions.assertNotNull(result);
        assertEquals(hexString, result.toHexString());
    }

    @Test
    public void testValueOf_Null() {
        ObjectId result = type.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_EmptyString() {
        ObjectId result = type.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_InvalidHexString() {
        String invalidHex = "invalid";
        Assertions.assertThrows(IllegalArgumentException.class, () -> type.valueOf(invalidHex));
    }

    @Test
    public void testValueOf_WrongLengthHexString() {
        String wrongLengthHex = "507f1f77bcf86cd79943901";
        Assertions.assertThrows(IllegalArgumentException.class, () -> type.valueOf(wrongLengthHex));
    }

    @Test
    public void testRoundTrip() {
        ObjectId original = new ObjectId();
        String hexString = type.stringOf(original);
        ObjectId restored = type.valueOf(hexString);

        Assertions.assertNotNull(restored);
        assertEquals(original, restored);
        assertEquals(original.toHexString(), restored.toHexString());
    }

    @Test
    public void testMultipleObjectIds() {
        ObjectId id1 = new ObjectId();
        ObjectId id2 = new ObjectId();

        String hex1 = type.stringOf(id1);
        String hex2 = type.stringOf(id2);

        Assertions.assertNotEquals(hex1, hex2);

        ObjectId restored1 = type.valueOf(hex1);
        ObjectId restored2 = type.valueOf(hex2);

        assertEquals(id1, restored1);
        assertEquals(id2, restored2);
        Assertions.assertNotEquals(restored1, restored2);
    }
}
