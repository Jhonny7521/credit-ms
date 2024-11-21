package com.bm_nttdata.credit_ms.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CardStatusEnum {

    ACTIVE("ACTIVE"),
    BLOCKED("BLOCKED"),
    CANCELLED("CANCELLED");

    private final String value;

    CardStatusEnum(String value){ this.value = value; }

    @JsonValue
    public String getValue(){ return value; }

    @Override
    public String toString(){ return String.valueOf(value); }

    @JsonCreator
    public static CardStatusEnum fromValue(String value){
        for (CardStatusEnum b : CardStatusEnum.values()){
            if (b.value.equals(value)){
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
