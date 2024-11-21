package com.bm_nttdata.credit_ms.client;

import com.bm_nttdata.credit_ms.DTO.CustomerDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-ms", url = "${customer-service.url}")
public interface CustomerClient {

    @GetMapping("/{id}")
    CustomerDTO getCustomerById(@PathVariable("id") String id);
}
