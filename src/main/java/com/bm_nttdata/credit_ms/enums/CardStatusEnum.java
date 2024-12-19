package com.bm_nttdata.credit_ms.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeración que representa los tipos de estatus de una tarjeta de credito.
 * Define si una tarjeta de credito se encuentra Activa, Bloqueada o Cancelada.
 */
public enum CardStatusEnum {

    ACTIVE("ACTIVE"),
    BLOCKED("BLOCKED"),
    CANCELLED("CANCELLED");

    private final String value;

    /**
     * Constructor del enum CardStatusEnum.
     *
     * @param value Valor string que representa el tipo de estatus
     */
    CardStatusEnum(String value) {
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
     * Convierte un valor string a su correspondiente enum CardStatusEnum.
     *
     * @param value Valor string a convertir
     * @return El enum CardStatusEnum correspondiente al valor
     * @throws IllegalArgumentException si el valor no corresponde a ningún tipo de estatus válido
     */
    @JsonCreator
    public static CardStatusEnum fromValue(String value) {
        for (CardStatusEnum b : CardStatusEnum.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
