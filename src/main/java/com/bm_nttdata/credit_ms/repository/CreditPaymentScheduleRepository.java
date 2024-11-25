package com.bm_nttdata.credit_ms.repository;

import com.bm_nttdata.credit_ms.entity.CreditPaymentSchedule;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CreditPaymentScheduleRepository extends MongoRepository<CreditPaymentSchedule, String> {

    CreditPaymentSchedule findByCreditIdAndInstallmentNumber(String creditId, Integer InstallerNumber);
}
