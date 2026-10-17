package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.landawn.abacus.TestBase;

/** Every input/output overload must surface failures and leave its mapper reusable. */
public class MapperFailureContractTest extends TestBase {
    @TempDir
    Path directory;

    public static class ValueBean {
        public int value;
    }

    public static class BrokenBean {
        public int getValue() {
            throw new IllegalStateException("getter failed deliberately");
        }
    }

    private static class RetainingDataOutput extends DataOutputStream {
        private final ByteArrayOutputStream buffer;

        RetainingDataOutput() {
            this(new ByteArrayOutputStream());
        }

        private RetainingDataOutput(ByteArrayOutputStream buffer) {
            super(buffer);
            this.buffer = buffer;
        }
    }

    static java.util.stream.Stream<Method> readers() {
        return methods().filter(m -> m.getName().equals("fromJson") || m.getName().equals("fromXml"));
    }

    static java.util.stream.Stream<Method> writers() {
        return methods().filter(m -> m.getName().equals("toJson") || m.getName().equals("toXml"));
    }

    private static java.util.stream.Stream<Method> methods() {
        return java.util.stream.Stream.of(JsonMappers.class, JsonMappers.One.class, XmlMappers.class, XmlMappers.One.class)
                .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                .sorted(Comparator.comparing(Method::toGenericString));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("readers")
    void malformedInputIsRejectedAndTheNextReadSucceeds(Method method) throws Throwable {
        boolean xml = method.getName().endsWith("Xml");
        Object receiver = receiver(method);
        if (xml && Arrays.asList(method.getParameterTypes()).contains(DataInput.class)) {
            // The XML backend explicitly does not implement its DataInput overloads.
            assertThrows(UnsupportedOperationException.class, () -> invoke(method, receiver, readArguments(method, false)));
            return;
        }
        RuntimeException failure = assertThrows(RuntimeException.class, () -> invoke(method, receiver, readArguments(method, true)));
        assertTrue(hasCause(failure, IOException.class), failure.toString());
        ValueBean value = (ValueBean) invoke(method, receiver, readArguments(method, false));
        assertEquals(7, value.value);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("writers")
    void getterFailuresRemainVisibleAndTheNextWriteSucceeds(Method method) throws Throwable {
        Object receiver = receiver(method);
        RuntimeException failure = assertThrows(RuntimeException.class, () -> invoke(method, receiver, writeArguments(method, new BrokenBean())));
        assertTrue(hasMessage(failure, "getter failed deliberately"), failure.toString());
        ValueBean bean = new ValueBean();
        bean.value = 7;
        Object[] arguments = writeArguments(method, bean);
        Object returned = invoke(method, receiver, arguments);
        String serialized = outputText(returned, arguments);
        assertEquals(7, method.getName().endsWith("Xml") ? XmlMappers.fromXml(serialized, ValueBean.class).value
                : JsonMappers.fromJson(serialized, ValueBean.class).value);
    }

    private Object[] readArguments(Method method, boolean invalid) throws Exception {
        boolean xml = method.getName().endsWith("Xml");
        String text = invalid ? (xml ? "<value>" : "{\"value\":") : (xml ? "<ValueBean><value>7</value></ValueBean>" : "{\"value\":7}");
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        File input = directory.resolve("input.txt").toFile();
        java.nio.file.Files.writeString(input.toPath(), text);
        Class<?>[] types = method.getParameterTypes();
        Object[] args = new Object[types.length];
        int integerIndex = 0;
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            if (type == byte[].class)
                args[i] = bytes;
            else if (type == int.class)
                args[i] = integerIndex++ == 0 ? 0 : bytes.length;
            else if (type == String.class)
                args[i] = text;
            else if (type == File.class)
                args[i] = input;
            else if (type == URL.class)
                args[i] = input.toURI().toURL();
            else if (type == InputStream.class)
                args[i] = new ByteArrayInputStream(bytes);
            else if (type == Reader.class)
                args[i] = new StringReader(text);
            else if (type == DataInput.class)
                args[i] = new DataInputStream(new ByteArrayInputStream(bytes));
            else if (type == Class.class)
                args[i] = ValueBean.class;
            else if (type == TypeReference.class)
                args[i] = new TypeReference<ValueBean>() {
                };
            else if (type == DeserializationConfig.class)
                args[i] = xml ? XmlMappers.createDeserializationConfig() : JsonMappers.createDeserializationConfig();
            else if (type == DeserializationFeature.class)
                args[i] = DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES;
            else if (type == DeserializationFeature[].class)
                args[i] = new DeserializationFeature[0];
            else
                throw new AssertionError("Unsupported reader parameter: " + type);
        }
        return args;
    }

    private Object[] writeArguments(Method method, Object value) {
        boolean xml = method.getName().endsWith("Xml");
        Class<?>[] types = method.getParameterTypes();
        Object[] args = new Object[types.length];
        args[0] = value;
        for (int i = 1; i < types.length; i++) {
            Class<?> type = types[i];
            if (type == boolean.class)
                args[i] = true;
            else if (type == File.class)
                args[i] = directory.resolve("output.txt").toFile();
            else if (type == OutputStream.class)
                args[i] = new ByteArrayOutputStream();
            else if (type == Writer.class)
                args[i] = new StringWriter();
            else if (type == DataOutput.class)
                args[i] = new RetainingDataOutput();
            else if (type == SerializationConfig.class)
                args[i] = xml ? XmlMappers.createSerializationConfig() : JsonMappers.createSerializationConfig();
            else if (type == SerializationFeature.class)
                args[i] = SerializationFeature.INDENT_OUTPUT;
            else if (type == SerializationFeature[].class)
                args[i] = new SerializationFeature[0];
            else
                throw new AssertionError("Unsupported writer parameter: " + type);
        }
        return args;
    }

    private static Object receiver(Method method) {
        if (method.getDeclaringClass() == JsonMappers.One.class)
            return JsonMappers.wrap(new JsonMapper());
        if (method.getDeclaringClass() == XmlMappers.One.class)
            return XmlMappers.wrap(new XmlMapper());
        return null;
    }

    private static String outputText(Object returned, Object[] args) throws IOException {
        if (returned instanceof String text)
            return text;
        for (Object arg : args) {
            if (arg instanceof File file)
                return java.nio.file.Files.readString(file.toPath());
            if (arg instanceof ByteArrayOutputStream out)
                return out.toString(StandardCharsets.UTF_8);
            if (arg instanceof StringWriter writer)
                return writer.toString();
            if (arg instanceof RetainingDataOutput output)
                return output.buffer.toString(StandardCharsets.UTF_8);
        }
        throw new AssertionError("No readable serialization output");
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable t = failure; t != null; t = t.getCause())
            if (type.isInstance(t))
                return true;
        return false;
    }

    private static boolean hasMessage(Throwable failure, String message) {
        for (Throwable t = failure; t != null; t = t.getCause())
            if (t.getMessage() != null && t.getMessage().contains(message))
                return true;
        return false;
    }

    private static Object invoke(Method method, Object receiver, Object[] args) throws Throwable {
        try {
            return method.invoke(receiver, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
