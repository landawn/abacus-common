package com.landawn.abacus.parser.entity;

import java.sql.Timestamp;
import java.util.Map;

import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.annotation.Type;
import com.landawn.abacus.util.Color;
import com.landawn.abacus.util.EnumType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("data_type")
public class EntityA {
    @Id
    @JsonXmlField(name = "xxx")
    private String stringType;
    private int intType;
    @Type(enumerated = EnumType.ORDINAL)
    private Color longType;

    private Map<Timestamp, Float> timestampHashMapType;

}
