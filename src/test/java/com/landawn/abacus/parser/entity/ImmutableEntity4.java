package com.landawn.abacus.parser.entity;

import com.landawn.abacus.TestBase;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class ImmutableEntity4 extends TestBase {
    private int id;
    private String firstName;
    private String lastName;
}
