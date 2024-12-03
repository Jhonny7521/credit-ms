package com.bm_nttdata.credit_ms.exception;

/**
 * Excepción que se lanza cuando se viola una regla de negocio.
 * Esta excepción es utilizada para manejar casos donde una operación no cumple
 * con las reglas establecidas del negocio.
 */
public class BusinessRuleException extends RuntimeException {

    /**
     * Construye una nueva excepción de regla de negocio con el mensaje especificado.
     *
     * @param message Mensaje que describe la regla de negocio violada
     */
    public BusinessRuleException(String message) {
        super(message);
    }
}
