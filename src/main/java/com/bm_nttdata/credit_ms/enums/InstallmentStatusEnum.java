package com.bm_nttdata.credit_ms.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeración que representa los tipos de estatus de una cuota de pago de un credito.
 * Define si una cuota se encuentra Pendiente, Pagada o Vencida.
 */
public enum InstallmentStatusEnum {
    PENDING("PENDING"),
    PAID("PAID"),
    OVERDUE("OVERDUE");

    public final String value;

    /**
     * Constructor del enum InstallmentStatusEnum.
     *
     * @param value Valor string que representa el tipo de estatus
     */
    InstallmentStatusEnum(String value) {
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
     * Convierte un valor string a su correspondiente enum InstallmentStatusEnum.
     *
     * @param value Valor string a convertir
     * @return El enum InstallmentStatusEnum correspondiente al valor
     * @throws IllegalArgumentException si el valor no corresponde a ningún tipo de estatus válido
     */
    @JsonCreator
    public static InstallmentStatusEnum fromValue(String value) {
        for (InstallmentStatusEnum b : InstallmentStatusEnum.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

}
