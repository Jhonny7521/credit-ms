package com.bm_nttdata.credit_ms.repository;

import com.bm_nttdata.credit_ms.entity.CreditCardInstallment;
import com.bm_nttdata.credit_ms.enums.InstallmentStatusEnum;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CreditCardInstallmentRepository extends MongoRepository<CreditCardInstallment, String> {

    List<CreditCardInstallment> findByCreditCardIdAndStatus(String creditCardId, InstallmentStatusEnum statusEnum);

    List<CreditCardInstallment> findByCreditCardIdAndDueDateLessThanAndStatusNot (String creditCardId, LocalDate dueDate, InstallmentStatusEnum statusEnum);
}
