package com.bm_nttdata.credit_ms.util;

import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * Generador de números de tarjeta de crédito.
 * Proporciona funcionalidad para crear números de tarjeta únicos
 * en formato XXXX-XXXX-XXXX-XXXX donde X son dígitos aleatorios.
 */
@Component
public class CardNumberGenerator {

    private static final Random random = new Random();

    /**
     * Genera un nuevo número de tarjeta de crédito.
     * El formato generado es XXXX-XXXX-XXXX-XXXX donde cada X es un dígito aleatorio.
     *
     * @return String con el número de tarjeta de crédito generado en formato XXXX-XXXX-XXXX-XXXX
     */
    public String generateCardNumber() {
        return String.format("%04d-%04d-%04d-%04d",
                random.nextInt(10000),
                random.nextInt(10000),
                random.nextInt(10000),
                random.nextInt(10000)
                );
    }
}
