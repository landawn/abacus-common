package com.landawn.abacus.util.entity;

import com.landawn.abacus.annotation.Id;

import lombok.Data;

@Data
public class IdEntity {

    @Id
    private long id;

    @javax.persistence.Id
    private String guid;

}
