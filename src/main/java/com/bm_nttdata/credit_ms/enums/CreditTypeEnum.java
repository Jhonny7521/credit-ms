package com.bm_nttdata.credit_ms.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeración que representa los tipos de credito.
 * Define si un credito es de tipo Personal o Empresarial.
 */
public enum CreditTypeEnum {
    PERSONAL("PERSONAL"),
    BUSINESS("BUSINESS");

    public final String value;

    /**
     * Constructor del enum CreditTypeEnum.
     *
     * @param value Valor string que representa el tipo de estatus
     */
    CreditTypeEnum(String value) {
        this.value = value;
    }

    /**
     * Obtiene el valor string del tipo de estatus.
     *
     * @return El valor string asociado al tipo de estatus
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Retorna la representación en string del tipo de estatus.
     *
     * @return String que representa el tipo de estatus
     */
    @Override
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * Convierte un valor string a su correspondiente enum CreditTypeEnum.
     *
     * @param value Valor string a convertir
     * @return El enum CreditTypeEnum correspondiente al valor
     * @throws IllegalArgumentException si el valor no corresponde a ningún tipo de estatus válido
     */
    @JsonCreator
    public static CreditTypeEnum fromValue(String value) {
        for (CreditTypeEnum b : CreditTypeEnum.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

}
