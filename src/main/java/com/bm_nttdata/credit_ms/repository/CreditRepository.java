package com.bm_nttdata.credit_ms.repository;

import com.bm_nttdata.credit_ms.entity.Credit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditRepository extends MongoRepository<Credit, String> {
    List<Credit> findByCustomerId(String id);

    long countByCustomerId(String id);
    long countByCustomerIdAndAmountGreaterThan(String id, double amount);
}
