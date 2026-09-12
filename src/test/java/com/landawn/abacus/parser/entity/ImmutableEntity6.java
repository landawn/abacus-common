package com.landawn.abacus.parser.entity;

import com.landawn.abacus.TestBase;

import lombok.Value;

@Value
public class ImmutableEntity6 extends TestBase {
    private int id;
    private String firstName;
    private String lastName;
}
