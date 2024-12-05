package com.bm_nttdata.credit_ms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients
@EnableScheduling
@SpringBootApplication
public class CreditMsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CreditMsApplication.class, args);
	}

}
