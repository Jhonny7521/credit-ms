package com.bm_nttdata.credit_ms.exception;

/**
 * Excepción que se lanza cuando una solicitud API contiene datos inválidos o está mal formada.
 * Esta excepción es utilizada para manejar errores de validación en las solicitudes entrantes.
 */
public class ApiInvalidRequestException extends RuntimeException {

    /**
     * Construye una nueva excepción de solicitud inválida con el mensaje especificado.
     *
     * @param message Mensaje que describe el error en la solicitud
     */
    public ApiInvalidRequestException(String message) {
        super(message);
    }
}
