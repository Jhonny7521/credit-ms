package com.bm_nttdata.credit_ms.repository;

import com.bm_nttdata.credit_ms.entity.CreditPaymentSchedule;
import com.bm_nttdata.credit_ms.enums.InstallmentStatusEnum;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface CreditPaymentScheduleRepository extends MongoRepository<CreditPaymentSchedule, String> {

    List<CreditPaymentSchedule> findByCreditIdAndDueDateLessThanAndStatusNot(String creditId, LocalDate dueDate, InstallmentStatusEnum paid);
}
