package com.landawn.abacus.util.entity;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;

import lombok.Data;

@Data
@Table("data_type")
public class DataType2 {
    private byte byteType;
    private char charType;
    private boolean booleanType;
    private short shortType;
    @Id
    private int intType;
    @Id
    private long longType;
    private float floatType;
    private double doubleType;
    private BigInteger bigIntegerType;
    private BigDecimal bigDecimalType;
    private String stringType;
}
