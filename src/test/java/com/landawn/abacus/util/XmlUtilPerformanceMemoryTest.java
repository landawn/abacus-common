package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for XmlUtil. */
@Tag("slow-test")
public class XmlUtilPerformanceMemoryTest extends TestBase {

    @ParameterizedTest
    @ValueSource(strings = { "value", "\u4E2D\uD83D\uDE03" })
    public void testGetTextContent_normalizationDoesNotCopyInputToCharArray(final String expected) throws Exception {
        final java.lang.management.ThreadMXBean managementBean = java.lang.management.ManagementFactory.getThreadMXBean();
        org.junit.jupiter.api.Assumptions.assumeTrue(managementBean instanceof com.sun.management.ThreadMXBean,
                "This JVM does not expose per-thread allocation accounting");
        final com.sun.management.ThreadMXBean allocationBean = (com.sun.management.ThreadMXBean) managementBean;
        org.junit.jupiter.api.Assumptions.assumeTrue(allocationBean.isThreadAllocatedMemorySupported() && allocationBean.isThreadAllocatedMemoryEnabled(),
                "Per-thread allocation accounting must be supported and enabled");
        org.junit.jupiter.api.Assumptions.assumeTrue(allocationBean.getCurrentThreadAllocatedBytes() >= 0,
                "Allocation accounting is unavailable for the current thread");

        final int smallPrefixLength = 64;
        final int largePrefixLength = 262_144;
        final org.w3c.dom.Document document = XmlUtil.createDOMParser().newDocument();
        final org.w3c.dom.Text smallText = document.createTextNode("\n".repeat(smallPrefixLength) + expected);
        final org.w3c.dom.Text largeText = document.createTextNode("\n".repeat(largePrefixLength) + expected);

        // Warm the exact DOM/result paths, the builder pool, and the allocation counter before comparing equal outputs.
        normalizationAllocatedBytes(allocationBean, smallText, expected, 2_000);
        normalizationAllocatedBytes(allocationBean, largeText, expected, 16);

        final int calls = 8;
        long smallAllocation = Long.MAX_VALUE;
        long largeAllocation = Long.MAX_VALUE;
        for (int sample = 0; sample < 5; sample++) {
            smallAllocation = Math.min(smallAllocation, normalizationAllocatedBytes(allocationBean, smallText, expected, calls));
            largeAllocation = Math.min(largeAllocation, normalizationAllocatedBytes(allocationBean, largeText, expected, calls));
        }

        // Both outputs are tiny and identical. One copied char[] adds two bytes per extra input code unit per call;
        // allow half that growth to absorb fixed VM/pool overhead without depending on object headers or exact counts.
        final long copiedInputGrowth = (long) (largePrefixLength - smallPrefixLength) * Character.BYTES * calls;
        assertTrue(largeAllocation - smallAllocation < copiedInputGrowth / 2,
                "Allocation grew by " + (largeAllocation - smallAllocation) + " bytes; a redundant input char[] would add " + copiedInputGrowth + " bytes");
    }

    private static long normalizationAllocatedBytes(final com.sun.management.ThreadMXBean allocationBean, final org.w3c.dom.Node text, final String expected,
            final int calls) {
        final String[] results = new String[calls];
        final long before = allocationBean.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < calls; i++) {
            results[i] = XmlUtil.getTextContent(text, true);
        }
        final long after = allocationBean.getCurrentThreadAllocatedBytes();

        org.junit.jupiter.api.Assumptions.assumeTrue(before >= 0 && after >= before, "Per-thread allocation accounting became unavailable");
        for (final String result : results) {
            assertEquals(expected, result);
        }
        return after - before;
    }

    // ---- perf review 2026-10-08 G075 end ----
    // ---- perf review 2026-10-08 G076 begin ----
    @Nested
    class G076 {

        private org.w3c.dom.Element parse(final String xml) throws Exception {
            return XmlUtil.createDOMParser().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml))).getDocumentElement();
        }

        /**
         * Independent recursive model of readElement: preorder, root attributes unqualified, nested attributes and text keyed
         * by the dotted element path, text only for elements without element children, last value wins.
         */
        private Map<String, String> reference(final org.w3c.dom.Element root) {
            final Map<String, String> output = new LinkedHashMap<>();
            visit(root, null, output);
            return output;
        }

        private void visit(final org.w3c.dom.Element element, final String parentPath, final Map<String, String> output) {
            final String path = parentPath == null ? element.getNodeName() : parentPath + "." + element.getNodeName();
            final org.w3c.dom.NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                final org.w3c.dom.Node attribute = attributes.item(i);
                output.put(parentPath == null ? attribute.getNodeName() : path + "." + attribute.getNodeName(), attribute.getNodeValue());
            }
            final org.w3c.dom.NodeList children = element.getChildNodes();
            final List<org.w3c.dom.Element> childElements = new ArrayList<>();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof org.w3c.dom.Element child) {
                    childElements.add(child);
                }
            }
            if (childElements.isEmpty()) {
                output.put(path, Strings.strip(element.getTextContent()));
            }
            for (final org.w3c.dom.Element child : childElements) {
                visit(child, path, output);
            }
        }

        private void assertSameEntries(final Map<String, String> expected, final Map<String, String> actual) {
            assertEquals(new ArrayList<>(expected.entrySet()), new ArrayList<>(actual.entrySet()));
            assertEquals(LinkedHashMap.class, actual.getClass());
        }

        private String prettyItems(final int count) {
            final StringBuilder sb = new StringBuilder();
            sb.append("<root a=\"1\">\n");
            for (int i = 0; i < count; i++) {
                sb.append("  <item id=\"").append(i).append("\">\n");
                sb.append("    <name> n").append(i).append(" </name>\n");
                if (i % 3 == 0) {
                    sb.append("    <!-- note ").append(i).append(" -->\n");
                }
                sb.append("    <value>v").append(i).append("<![CDATA[&x]]></value>\n");
                if (i % 5 == 0) {
                    sb.append("    <extra k=\"").append(i).append("\"/>\n");
                }
                sb.append("  </item>\n");
                if (i % 7 == 0) {
                    sb.append("  <?pi data?>\n");
                }
            }
            sb.append("  <tail>end</tail>\n</root>\n");
            return sb.toString();
        }

        private void randomElement(final Random random, final StringBuilder sb, final int depth, final boolean pretty) {
            final String[] names = { "a", "b", "item", "p:n" };
            final String name = names[random.nextInt(names.length)];
            sb.append('<').append(name);
            if (depth == 0) {
                // the root declares the namespace of the "p:" element and attribute names
                sb.append(" xmlns:p=\"urn:p\"");
            }
            final int attributeCount = random.nextInt(3);
            for (int i = 0; i < attributeCount; i++) {
                sb.append(' ').append(i == 0 ? "id" : (random.nextBoolean() ? "x" : "p:y")).append("=\"").append(random.nextInt(5)).append("&amp;\"");
            }
            final int childCount = depth >= 4 ? 0 : random.nextInt(5);
            if (childCount == 0 && random.nextInt(4) == 0) {
                sb.append("/>");
                return;
            }
            sb.append('>');
            for (int i = 0; i < childCount; i++) {
                if (pretty) {
                    sb.append("\n").append("  ".repeat(depth + 1));
                }
                switch (random.nextInt(8)) {
                    case 0:
                        sb.append(" t").append(random.nextInt(9)).append(" &lt; ");
                        break;
                    case 1:
                        sb.append("<!-- c -->");
                        break;
                    case 2:
                        sb.append("<![CDATA[ cd ]]>");
                        break;
                    case 3:
                        sb.append("<?pi x?>");
                        break;
                    default:
                        randomElement(random, sb, depth + 1, pretty);
                }
            }
            if (pretty && childCount > 0) {
                sb.append("\n").append("  ".repeat(depth));
            }
            sb.append("</").append(name).append('>');
        }

        // G076-01: readElement walks children by sibling links; pins its output on pretty-printed documents (whitespace text
        // nodes, comments, CDATA, processing instructions, empty and attribute-only elements, repeated paths).
        @Test
        public void testReadElement_prettyPrintedDocumentMatchesReference() throws Exception {
            final org.w3c.dom.Element root = parse(prettyItems(1500));
            final Map<String, String> expected = reference(root);
            assertSameEntries(expected, XmlUtil.readElement(root));
            // A second call on the same, already traversed DOM must give the same result.
            assertSameEntries(expected, XmlUtil.readElement(root));
            assertEquals("1", expected.get("a"));
            assertEquals("1499", expected.get("root.item.id"));
            assertEquals("n1499", expected.get("root.item.name"));
            assertEquals("v1499&x", expected.get("root.item.value"));
            assertEquals("1495", expected.get("root.item.extra.k"));
            assertEquals("", expected.get("root.item.extra"));
            assertEquals("end", expected.get("root.tail"));
            assertEquals(List.of("a", "root.item.id", "root.item.name", "root.item.value", "root.item.extra.k", "root.item.extra", "root.tail"),
                    new ArrayList<>(expected.keySet()));
        }

        // G076-01: random parsed documents (pretty and compact, namespaces, mixed content) give the reference result, also on
        // a second traversal of the same DOM.
        @Test
        public void testReadElement_randomDocumentsMatchReference() throws Exception {
            final Random random = new Random(20261008L);
            for (int round = 0; round < 400; round++) {
                final boolean pretty = random.nextBoolean();
                final StringBuilder sb = new StringBuilder();
                randomElement(random, sb, 0, pretty);
                final org.w3c.dom.Element root = parse(sb.toString());
                final Map<String, String> expected = reference(root);
                assertSameEntries(expected, XmlUtil.readElement(root));
                assertSameEntries(expected, XmlUtil.readElement(root));
            }
        }

        // G076-01: leaf, attribute-only, text-only, whitespace-only and non-element-only roots, element children followed by
        // trailing non-element nodes, and a programmatically built DOM with an entity reference child.
        @Test
        public void testReadElement_edgeShapesMatchReference() throws Exception {
            final String[] documents = { "<r/>", "<r id=\"1\" x=\"2\"/>", "<r>  text  </r>", "<r>   </r>", "<r><!-- c --></r>", "<r><?pi x?>t<!--c-->u</r>",
                    "<r><a/>tail<!-- c --><?pi?></r>", "<r>\n  <a>1</a>\n</r>", "<r>\n  <a>1</a>\n  <a>2</a>\n  <b><a>3</a></b>\n</r>",
                    "<r><a x=\"1\"><b>t</b></a><a y=\"2\"/></r>" };
            for (final String document : documents) {
                final org.w3c.dom.Element root = parse(document);
                final Map<String, String> expected = reference(root);
                assertSameEntries(expected, XmlUtil.readElement(root));
            }
            assertEquals(Map.of("r", ""), XmlUtil.readElement(parse("<r/>")));
            assertEquals(List.of("id", "x", "r"), new ArrayList<>(XmlUtil.readElement(parse("<r id=\"1\" x=\"2\"/>")).keySet()));
            assertEquals("tu", XmlUtil.readElement(parse("<r><?pi x?>t<!--c-->u</r>")).get("r"));
            assertEquals(Map.of("r.a", "2", "r.b.a", "3"), XmlUtil.readElement(parse("<r>\n  <a>1</a>\n  <a>2</a>\n  <b><a>3</a></b>\n</r>")));

            final org.w3c.dom.Document document = XmlUtil.createDOMParser().newDocument();
            final org.w3c.dom.Element root = document.createElement("root");
            document.appendChild(root);
            final org.w3c.dom.Element first = document.createElement("first");
            first.appendChild(document.createTextNode(" one "));
            root.appendChild(first);
            root.appendChild(document.createEntityReference("ent"));
            root.appendChild(document.createTextNode("  "));
            final org.w3c.dom.Element second = document.createElement("second");
            second.setAttribute("k", "v");
            second.appendChild(document.createEntityReference("ent"));
            root.appendChild(second);
            final Map<String, String> expected = reference(root);
            assertSameEntries(expected, XmlUtil.readElement(root));
            assertEquals("one", expected.get("root.first"));
            assertEquals("v", expected.get("root.second.k"));
            assertTrue(expected.containsKey("root.second"));
        }

        private boolean referenceIsTextElement(final org.w3c.dom.Node node) {
            final org.w3c.dom.NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    return false;
                }
            }
            return true;
        }

        private void collect(final org.w3c.dom.Node node, final List<org.w3c.dom.Node> output) {
            output.add(node);
            final org.w3c.dom.NamedNodeMap attributes = node.getAttributes();
            if (attributes != null) {
                for (int i = 0; i < attributes.getLength(); i++) {
                    output.add(attributes.item(i));
                }
            }
            for (org.w3c.dom.Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                collect(child, output);
            }
        }

        // G076-02: isTextElement walks sibling links; pins its result for every node type (document, element, text, CDATA,
        // comment, processing instruction, attribute, fragment, entity reference, document type) and on repeated calls.
        @Test
        public void testIsTextElement_allNodeTypesMatchReference() throws Exception {
            assertFalse(XmlUtil.isTextElement(null));
            final Random random = new Random(8_20261008L);
            int checked = 0;
            for (int round = 0; round < 200; round++) {
                final StringBuilder sb = new StringBuilder();
                randomElement(random, sb, 0, random.nextBoolean());
                final org.w3c.dom.Element root = parse(sb.toString());
                final List<org.w3c.dom.Node> nodes = new ArrayList<>();
                collect(root.getOwnerDocument(), nodes);
                for (int pass = 0; pass < 2; pass++) {
                    for (final org.w3c.dom.Node node : nodes) {
                        assertEquals(referenceIsTextElement(node), XmlUtil.isTextElement(node), () -> node.getNodeType() + " " + node.getNodeName());
                        checked++;
                    }
                }
            }
            assertTrue(checked > 1000);

            final org.w3c.dom.Document document = XmlUtil.createDOMParser().newDocument();
            final org.w3c.dom.Element root = document.createElement("root");
            document.appendChild(root);
            final org.w3c.dom.Attr plainAttribute = document.createAttribute("plain");
            plainAttribute.setValue("v");
            root.setAttributeNode(plainAttribute);
            final org.w3c.dom.Attr nodeAttribute = document.createAttribute("nodes");
            nodeAttribute.appendChild(document.createTextNode("a"));
            nodeAttribute.appendChild(document.createEntityReference("ent"));
            root.setAttributeNode(nodeAttribute);
            final org.w3c.dom.DocumentFragment emptyFragment = document.createDocumentFragment();
            final org.w3c.dom.DocumentFragment textFragment = document.createDocumentFragment();
            textFragment.appendChild(document.createTextNode("t"));
            final org.w3c.dom.DocumentFragment elementFragment = document.createDocumentFragment();
            elementFragment.appendChild(document.createTextNode(" "));
            elementFragment.appendChild(document.createElement("e"));
            final org.w3c.dom.DocumentType documentType = document.getImplementation().createDocumentType("root", null, null);
            final org.w3c.dom.Node[] specials = { document, root, plainAttribute, nodeAttribute, emptyFragment, textFragment, elementFragment,
                    document.createEntityReference("ent"), documentType, document.createComment("c"), document.createProcessingInstruction("t", "d"),
                    document.createCDATASection("x"), document.createTextNode("") };
            final boolean[] expected = { false, true, true, true, true, true, false, true, true, true, true, true, true };
            for (int i = 0; i < specials.length; i++) {
                assertEquals(expected[i], referenceIsTextElement(specials[i]), String.valueOf(i));
                assertEquals(expected[i], XmlUtil.isTextElement(specials[i]), String.valueOf(i));
                assertEquals(expected[i], XmlUtil.isTextElement(specials[i]), String.valueOf(i));
            }
        }
    }
}
