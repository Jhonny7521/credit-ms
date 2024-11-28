package com.bm_nttdata.credit_ms.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum InstallmentStatusEnum {
    PENDING("PENDING"),
    PAID("PAID"),
    OVERDUE("OVERDUE");

    public final String value;

    InstallmentStatusEnum(String value){
        this.value = value;
    }

    @JsonValue
    public String getValue(){ return value; }

    @Override
    public String toString(){ return String.valueOf(value); }

    @JsonCreator
    public static InstallmentStatusEnum fromValue(String value){
        for (InstallmentStatusEnum b : InstallmentStatusEnum.values()){
            if (b.value.equals(value)){
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

}
