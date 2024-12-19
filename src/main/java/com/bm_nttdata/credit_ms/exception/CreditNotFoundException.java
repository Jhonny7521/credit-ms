package com.bm_nttdata.credit_ms.exception;

/**
 * Excepción que se lanza cuando no se encuentra un credito en específico.
 * Esta excepción es utilizada para manejar casos donde se intenta acceder a una
 * credito que no existe.
 */
public class CreditNotFoundException extends RuntimeException {

    /**
     * Construye una nueva excepción de credito no encontrado con el mensaje especificado.
     *
     * @param message Mensaje con la razón por la que no se encontró la cuenta
     */
    public CreditNotFoundException(String message) {
        super(message);
    }
}
