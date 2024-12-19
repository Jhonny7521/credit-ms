package com.bm_nttdata.credit_ms.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeración que representa los tipos de estatus de un credito.
 * Define si un credito se encuentra Activa, Pagado o Incumplida.
 */
public enum CreditStatusEnum {
    ACTIVE("ACTIVE"),
    PAID("PAID"),
    DEFAULTED("DEFAULTED");

    public final String value;

    /**
     * Constructor del enum CreditStatusEnum.
     *
     * @param value Valor string que representa el tipo de estatus
     */
    CreditStatusEnum(String value) {
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
     * Convierte un valor string a su correspondiente enum CreditStatusEnum.
     *
     * @param value Valor string a convertir
     * @return El enum CreditStatusEnum correspondiente al valor
     * @throws IllegalArgumentException si el valor no corresponde a ningún tipo de estatus válido
     */
    @JsonCreator
    public static CreditStatusEnum fromValue(String value) {
        for (CreditStatusEnum b : CreditStatusEnum.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
