package com.landawn.abacus.parser.entity;

import com.landawn.abacus.TestBase;

import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
public class ImmutableEntity5 extends TestBase {
    private int id;
    private String firstName;
    private String lastName;
}
