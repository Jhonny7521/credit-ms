package com.bm_nttdata.credit_ms.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CreditStatusEnum {
    ACTIVE("ACTIVE"),
    PAID("PAID"),
    DEFAULTED("DEFAULTED");

    public final String value;

    CreditStatusEnum(String value) { this.value = value; }

    @JsonValue
    public String getValue(){ return value; }

    @Override
    public String toString(){ return String.valueOf(value); }

    @JsonCreator
    public static CreditStatusEnum fromValue(String value){
        for (CreditStatusEnum b : CreditStatusEnum.values()){
            if (b.value.equals(value)){
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
