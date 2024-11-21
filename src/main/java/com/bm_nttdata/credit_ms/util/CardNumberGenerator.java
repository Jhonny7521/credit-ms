package com.bm_nttdata.credit_ms.util;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class CardNumberGenerator {

    private static final Random random = new Random();

    public String generateCardNumber(){
        return String.format("",
                random.nextInt(10000),
                random.nextInt(10000),
                random.nextInt(10000),
                random.nextInt(10000)
                );
    }
}
