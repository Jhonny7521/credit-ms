package com.bm_nttdata.credit_ms.service.impl;

import com.bm_nttdata.credit_ms.entity.Credit;
import com.bm_nttdata.credit_ms.entity.CreditPaymentSchedule;
import com.bm_nttdata.credit_ms.enums.InstallmentStatusEnum;
import com.bm_nttdata.credit_ms.exception.ServiceException;
import com.bm_nttdata.credit_ms.repository.CreditPaymentScheduleRepository;
import com.bm_nttdata.credit_ms.service.ICreditPaymentScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Transactional
@Service
public class CreditPaymentScheduleServiceImpl implements ICreditPaymentScheduleService {

    @Autowired
    private CreditPaymentScheduleRepository paymentScheduleRepository;

    @Override
    public void createPaymentSchedule(Credit credit) {

        log.info("Creating monthly payment list for customer: {} - credit: {}", credit.getCustomerId(), credit.getId());
        
        List<CreditPaymentSchedule> listMonthlyInstallments ;

        try{
            listMonthlyInstallments = IntStream.range(0, credit.getTerm())
                    .mapToObj( i -> CreditPaymentSchedule.builder()
                            .creditId(credit.getId())
                            .creditAmount(credit.getAmount())
                            .installmentNumber(i + 1)
                            .installmentAmount(credit.getNextPaymentAmount())
                            .dueDate(credit.getCreatedAt().toLocalDate().plusMonths(i + 1))
                            .status(InstallmentStatusEnum.PENDING)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e){
            log.error("Unexpected error while generating monthly payment list: {}", e.getMessage());
            throw new ServiceException("Unexpected error while generating monthly payment list");
        }

        try {
            paymentScheduleRepository.saveAll(listMonthlyInstallments);
        }catch (Exception e){
            log.error("Unexpected error while saving monthly payment list: {}", e.getMessage());
            throw new ServiceException("Unexpected error while saving monthly payment list");
        }
        log.info(" *** Successful creation *** ");
    }

    @Override
    public BigDecimal calculateMonthlyPayment(BigDecimal creditAmount, BigDecimal interestRate, int months){

        log.info("Calculating monthly payment");

        try {
            BigDecimal monthlyRate = BigDecimal.valueOf(Math.pow(1 + (interestRate.doubleValue() / 100), 1.0 / 12) - 1); // r = ((1 + TEA)^(1/12)) - 1
            BigDecimal ratePowerMonths = monthlyRate.add(BigDecimal.ONE).pow(months); // (1 + r)^n
            BigDecimal monthlyPayment = creditAmount
                    .multiply(monthlyRate)
                    .multiply(ratePowerMonths)
                    .divide(ratePowerMonths.subtract(BigDecimal.ONE), 2 , RoundingMode.HALF_UP);

            return monthlyPayment;
        } catch (Exception e){
            log.error("Unexpected error while calculating the monthly payment: {}", e.getMessage());
            throw new ServiceException("Unexpected error while calculating the monthly payment");
        }
    }

    @Override
    public void updatePaymentSchedule(Credit credit) {

        log.info("Updating monthly payment for customer: {} - credit: {}", credit.getCustomerId(), credit.getId());
        CreditPaymentSchedule creditPayment = paymentScheduleRepository.findByCreditIdAndInstallmentNumber(credit.getId(), credit.getNextPaymentInstallment());

        creditPayment.setStatus(InstallmentStatusEnum.PAID);
        if (LocalDate.now().isAfter(creditPayment.getDueDate())){
            creditPayment.setDaysOverdue((int) ChronoUnit.DAYS.between(creditPayment.getDueDate(), LocalDate.now()));
            creditPayment.setStatus(InstallmentStatusEnum.OVERDUE);
        }
        creditPayment.setUpdatedAt(LocalDateTime.now());

        try {
            paymentScheduleRepository.save(creditPayment);
        }catch (Exception e){
            log.error("Unexpected error while updating monthly payment: {}", e.getMessage());
            throw new ServiceException("Unexpected error while updating monthly payment");
        }
        log.info(" *** Successful update *** ");
    }
}
