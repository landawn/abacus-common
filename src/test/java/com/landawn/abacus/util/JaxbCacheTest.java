package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.annotation.XmlRootElement;

@Timeout(30)
public class JaxbCacheTest extends TestBase {
    @TempDir
    Path temp;

    @Test
    void identicalPathsUseTheirOwnLoaderEvenWhenLoadersCompareEqual() throws Exception {
        URL fixture = compileFixture();
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try (EqualLoader first = new EqualLoader(fixture);
             EqualLoader second = new EqualLoader(fixture)) {
            assertEquals(first, second);
            for (EqualLoader loader : List.of(first, second)) {
                Thread.currentThread().setContextClassLoader(loader);
                Class<?> beanType = Class.forName("reviewjaxb.Person", true, loader);
                Object bean = beanType.getConstructor().newInstance();
                beanType.getField("value").set(bean, "\u4e2d\ud83d\ude00 & < >");
                StringWriter writer = new StringWriter();
                XmlUtil.createMarshaller("reviewjaxb").marshal(bean, writer);
                Object decoded = XmlUtil.createUnmarshaller("reviewjaxb").unmarshal(new StringReader(writer.toString()));
                assertSame(beanType, decoded.getClass());
                assertEquals("\u4e2d\ud83d\ude00 & < >", beanType.getField("value").get(decoded));
                String xml = XmlUtil.marshal(bean);
                Object typed = XmlUtil.unmarshal(beanType, xml);
                assertSame(beanType, typed.getClass());
                assertEquals(beanType.getField("value").get(bean), beanType.getField("value").get(typed));
            }
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Test
    void liveContextsAreReusedAndClassBindingsAreScopedByAmbientLoader() throws Exception {
        URL fixture = compileFixture();
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try (EqualLoader first = new EqualLoader(fixture);
             EqualLoader second = new EqualLoader(fixture)) {
            Thread.currentThread().setContextClassLoader(first);
            JAXBContext path = context("reviewjaxb");
            JAXBContext type = context(StableBean.class);
            assertSame(path, context("reviewjaxb"));
            assertSame(type, context(StableBean.class));
            Thread.currentThread().setContextClassLoader(second);
            assertNotSame(path, context("reviewjaxb"));
            assertNotSame(type, context(StableBean.class));
            Thread.currentThread().setContextClassLoader(null);
            JAXBContext nullLoader = context(StableBean.class);
            assertSame(nullLoader, context(StableBean.class));
            assertNotSame(type, nullLoader);
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.createMarshaller((Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.createUnmarshaller((String) null));
        for (int i = 0; i < 2; i++) {
            assertThrows(RuntimeException.class, () -> XmlUtil.createMarshaller("missing.review.jaxb"));
        }
    }

    @Test
    void concurrentColdRequestsPublishOneLiveContext() throws Exception {
        URL fixture = compileFixture();
        try (EqualLoader loader = new EqualLoader(fixture)) {
            ExecutorService executor = Executors.newFixedThreadPool(8);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<JAXBContext>> futures = new ArrayList<>();
            try {
                for (int i = 0; i < 8; i++) {
                    futures.add(executor.submit(() -> {
                        ClassLoader original = Thread.currentThread().getContextClassLoader();
                        try {
                            Thread.currentThread().setContextClassLoader(loader);
                            assertTrue(start.await(10, TimeUnit.SECONDS));
                            return context("reviewjaxb");
                        } finally {
                            Thread.currentThread().setContextClassLoader(original);
                        }
                    }));
                }
                start.countDown();
                Set<JAXBContext> contexts = Collections.newSetFromMap(new IdentityHashMap<>());
                for (Future<JAXBContext> future : futures) {
                    contexts.add(future.get(10, TimeUnit.SECONDS));
                }
                assertEquals(1, contexts.size());
            } finally {
                start.countDown();
                futures.forEach(future -> future.cancel(true));
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
            }
        }
    }

    private URL compileFixture() throws Exception {
        // Initialize unrelated XML factories under the normal application loader.
        Class.forName(XmlUtil.class.getName());
        Path source = temp.resolve("reviewjaxb/Person.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package reviewjaxb;
                @jakarta.xml.bind.annotation.XmlRootElement
                public class Person {
                    public String value;
                    public Person() {}
                }
                """, StandardCharsets.UTF_8);
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-classpath", classpath, "-d", temp.toString(), source.toString()));
        Files.writeString(temp.resolve("reviewjaxb/jaxb.index"), "Person\n", StandardCharsets.UTF_8);
        return temp.toUri().toURL();
    }

    // Identity observation holds a context strongly while asserting opportunistic cache reuse.
    private static JAXBContext context(Object binding) throws Exception {
        Method method = XmlUtil.class.getDeclaredMethod("jaxbContext", binding instanceof String ? String.class : Class.class);
        method.setAccessible(true);
        return (JAXBContext) method.invoke(null, binding);
    }

    @XmlRootElement
    public static class StableBean {
        public String value;
    }

    private static final class EqualLoader extends URLClassLoader {
        EqualLoader(URL fixture) {
            super(new URL[] { fixture }, JaxbCacheTest.class.getClassLoader());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof EqualLoader;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}
