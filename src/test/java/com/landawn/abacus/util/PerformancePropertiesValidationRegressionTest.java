package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Tag("unit")
class PerformancePropertiesValidationRegressionTest {
    @Test
    void nestedValidationTraversesEachSubtreeOnce() throws Exception {
        long shallow = count(30, 1), deep = count(60, 1);
        assertTrue(deep < shallow * 2.3, shallow + " -> " + deep);
        long flatWide = count(0, 300), deepWide = count(60, 300);
        assertTrue(deepWide < flatWide * 2, flatWide + " -> " + deepWide);
    }

    @Test
    void deepDuplicatesAreRejectedBeforeMutatingAnExistingTarget() throws Exception {
        Properties<String, Object> output = new Properties<>();
        output.put("keep", "original");
        String xml = "<root><keep>changed</keep><nested><x><item>1</item><item>2</item></x></nested></root>";
        Node root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml))).getDocumentElement();
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> loader().invoke(null, root, null, true, output, Properties.class));
        assertInstanceOf(RuntimeException.class, exception.getCause());
        assertEquals(1, output.size());
        assertEquals("original", output.get("keep"));
        assertThrows(RuntimeException.class, () -> PropertiesUtil.loadFromXml(new StringReader(xml)));
    }

    @Test
    void textPropertiesAndNormalizedDuplicatesKeepTheirRules() {
        assertTrue(PropertiesUtil.loadFromXml(new StringReader("<root><empty type=\"Properties\"></empty></root>")).get("empty") instanceof Properties);
        assertThrows(RuntimeException.class,
                () -> PropertiesUtil.loadFromXml(new StringReader("<root><nested><retry-count>1</retry-count><retryCount>2</retryCount></nested></root>")));
    }

    private static Method loader() throws Exception {
        Method method = PropertiesUtil.class.getDeclaredMethod("loadFromXml", Node.class, Method.class, boolean.class, Properties.class, Class.class);
        method.setAccessible(true);
        return method;
    }

    private static long count(int depth, int width) throws Exception {
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = document.createElement("root"), cursor = root;
        document.appendChild(root);
        for (int i = 0; i < depth; i++) {
            Element child = document.createElement("p"); cursor.appendChild(child); cursor = child;
        }
        for (int i = 0; i < width; i++) {
            Element child = document.createElement("v" + i); child.appendChild(document.createTextNode("ok")); cursor.appendChild(child);
        }
        CountedDom dom = new CountedDom();
        Properties<?, ?> result = (Properties<?, ?>) loader().invoke(null, dom.wrap(root), null, true, null, Properties.class);
        for (int i = 0; i < depth; i++) result = (Properties<?, ?>) result.get("p");
        assertEquals(width, result.size());
        assertEquals("ok", result.get("v0"));
        return dom.calls;
    }

    private static class CountedDom {
        final IdentityHashMap<Node, Node> proxies = new IdentityHashMap<>();
        long calls;
        Node wrap(Node node) {
            if (node == null) return null;
            return proxies.computeIfAbsent(node, original -> (Node) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { original instanceof Element ? Element.class : Node.class }, (proxy, method, args) -> {
                        if (method.getName().equals("getChildNodes")) calls++;
                        Object value;
                        try { value = method.invoke(original, args); } catch (InvocationTargetException e) { throw e.getCause(); }
                        if (value instanceof NodeList nodes && method.getReturnType() == NodeList.class) return new NodeList() {
                            public int getLength() { return nodes.getLength(); }
                            public Node item(int index) { return wrap(nodes.item(index)); }
                        };
                        return value instanceof Node n ? wrap(n) : value;
                    }));
        }
    }
}
