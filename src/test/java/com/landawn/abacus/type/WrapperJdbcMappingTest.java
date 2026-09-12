package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;

public class WrapperJdbcMappingTest extends TestBase {
    @ParameterizedTest
    @ValueSource(strings = { "JdkOptional", "Optional", "Nullable", "Holder" })
    void collectionsUseTheirJsonJdbcRepresentation(String wrapper) throws Exception {
        Type<Object> type = TypeFactory.getType(wrapper + "<List<String>>");
        Type<List<String>> element = TypeFactory.getType("List<String>");
        for (List<String> value : List.of(List.<String> of(), List.of("\u6C49\u5B57", "a,b", "\uD83D\uDE00"))) {
            String json = element.stringOf(value);
            PreparedStatement stmt = mock(PreparedStatement.class);
            CallableStatement call = mock(CallableStatement.class);
            type.set(stmt, 1, wrap(wrapper, value));
            type.set(call, "value", wrap(wrapper, value));
            verify(stmt).setString(1, json);
            verify(call).setString("value", json);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString(1)).thenReturn(json);
            when(rs.getString("value")).thenReturn(json);
            assertEquals(json, type.stringOf(type.get(rs, 1)));
            assertEquals(json, type.stringOf(type.get(rs, "value")));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "JdkOptional", "Optional", "Nullable", "Holder" })
    void nullAndEmptyWrappersUseThePrimitiveNullMapping(String wrapper) throws Exception {
        Type<Object> type = TypeFactory.getType(wrapper + "<int>");
        for (Object value : new Object[] { null, wrap(wrapper, null), type.valueOf((String) null) }) {
            PreparedStatement stmt = mock(PreparedStatement.class);
            CallableStatement call = mock(CallableStatement.class);
            type.set(stmt, 1, value);
            type.set(call, "value", value);
            verify(stmt).setNull(1, Types.INTEGER);
            verify(call).setNull("value", Types.INTEGER);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "JdkOptional", "Optional", "Nullable", "Holder" })
    void integerValuesUseNativeBinding(String wrapper) throws Exception {
        Type<Object> type = TypeFactory.getType(wrapper + "<Integer>");
        for (int value : new int[] { Integer.MIN_VALUE, 0, Integer.MAX_VALUE }) {
            PreparedStatement stmt = mock(PreparedStatement.class);
            CallableStatement call = mock(CallableStatement.class);
            type.set(stmt, 1, wrap(wrapper, value));
            type.set(call, "value", wrap(wrapper, value));
            verify(stmt).setInt(1, value);
            verify(call).setInt("value", value);
        }
    }

    private static Object wrap(String wrapper, Object value) {
        return switch (wrapper) {
            case "JdkOptional" -> java.util.Optional.ofNullable(value);
            case "Optional" -> Optional.ofNullable(value);
            case "Nullable" -> Nullable.of(value);
            case "Holder" -> Holder.of(value);
            default -> throw new AssertionError(wrapper);
        };
    }
}
