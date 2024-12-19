package com.bm_nttdata.credit_ms.exception;

/**
 * Excepción personalizada para manejar errores relacionados con los servicios de la aplicación.
 * Esta excepción se lanza cuando ocurren errores durante la ejecución de operaciones en la
 * capa de servicio.
 */
public class ServiceException extends RuntimeException {

    /**
     * Construye una nueva excepción de servicio con el mensaje de error especificado.
     *
     * @param message Mensaje que describe la razón del error
     */
    public ServiceException(String message) {
        super(message);
    }
}
