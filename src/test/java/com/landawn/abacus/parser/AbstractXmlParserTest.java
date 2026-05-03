package com.landawn.abacus.parser;

public abstract class AbstractXmlParserTest extends AbstractParserTest {
    // TODO: AbstractXmlParser's Node deserialize delegate methods are exercised by concrete XmlParser tests; isolated coverage would require
    // a full fake XmlParser implementation because the class is package-private and has many inherited abstract parser methods.
    // TODO: AbstractXmlParser's File/InputStream/Reader/Node nodeTypes overloads are default unsupported hooks; concrete support is covered in
    // XmlParserImplTest and AbacusXmlParserImplTest, while JaxbParser intentionally does not support these overloads.
}
