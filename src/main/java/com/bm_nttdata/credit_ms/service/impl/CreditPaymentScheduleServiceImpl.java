package com.bm_nttdata.credit_ms.service.impl;

import com.bm_nttdata.credit_ms.entity.Credit;
import com.bm_nttdata.credit_ms.entity.CreditPaymentSchedule;
import com.bm_nttdata.credit_ms.enums.InstallmentStatusEnum;
import com.bm_nttdata.credit_ms.exception.BusinessRuleException;
import com.bm_nttdata.credit_ms.exception.CreditNotFoundException;
import com.bm_nttdata.credit_ms.exception.ServiceException;
import com.bm_nttdata.credit_ms.repository.CreditPaymentScheduleRepository;
import com.bm_nttdata.credit_ms.service.ICreditPaymentScheduleService;
import com.bm_nttdata.credit_ms.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    public List<CreditPaymentSchedule> getMonthInstallments(String creditCardId, int paymentDay) {

        try{
            LocalDate currentDate = LocalDate.now();
            LocalDate dueDate = LocalDate.of( currentDate.getYear(), currentDate.getMonth(), paymentDay );

            // Si la fecha de pago ya pasó este mes, obtener la del próximo mes
            if (currentDate.isAfter(dueDate)) {
                dueDate = dueDate.plusMonths(1);
            }

            List<CreditPaymentSchedule> paymentScheduleList = paymentScheduleRepository
                    .findByCreditCardIdAndDueDateLessThanAndStatusNot( creditCardId, dueDate, InstallmentStatusEnum.PAID );

            if (paymentScheduleList.isEmpty()) throw new CreditNotFoundException("No debt exists");

            return paymentScheduleList;

        } catch (Exception e){
            log.error("Error getting current month due installments: {}", e.getMessage());
            throw new ServiceException("Error retrieving current month installments" + e.getMessage());
        }
    }

    @Override
    public BigDecimal calculateMonthlyPayment(String creditId, int installmentNumber){

        log.info("Calculating the monthly payment.: {}", creditId);

        try {

            List<CreditPaymentSchedule> creditInstallmentList = getMonthInstallments(creditId, installmentNumber);

            BigDecimal totalAmount = BigDecimal.ZERO;

            for (CreditPaymentSchedule creditInstallment : creditInstallmentList) {

                BigDecimal installmentAmount = creditInstallment.getInstallmentAmount();
                BigDecimal interestAmount = BigDecimal.ZERO;

                if (isOverdue(creditInstallment)) {
                    try {
                        // Obtener días de retraso
                        long daysOverdue = ChronoUnit.DAYS.between(creditInstallment.getDueDate(), LocalDate.now());

                        // Calcular interés diario (interes anual / 365)
                        BigDecimal dailyInterestRate = BigDecimal.valueOf(Constants.LATE_PAYMENT_INTEREST / 365.0);

                        // Calcular monto de interés
                        interestAmount = installmentAmount
                                .multiply(dailyInterestRate)
                                .multiply(BigDecimal.valueOf(daysOverdue));

                        // Actualizar estado de cuota mensual a VENCIDO (OVERDUE), si aún no lo esta
                        if (creditInstallment.getStatus() != InstallmentStatusEnum.OVERDUE) {
                            creditInstallment.setStatus(InstallmentStatusEnum.OVERDUE);
                        }
                        creditInstallment.setInterest(interestAmount);
                        creditInstallment.setDaysOverdue(daysOverdue);
                        creditInstallment.setUpdatedAt(LocalDateTime.now());

                        paymentScheduleRepository.save(creditInstallment);

                    } catch (Exception e) {
                        log.error("Error updating overdue installment {}: {}", creditInstallment.getId(), e.getMessage());
                        throw new ServiceException("Error updating overdue installment" + e.getMessage());
                    }
                }

                totalAmount = totalAmount.add(installmentAmount).add(interestAmount);
            }

            return totalAmount;
        } catch (Exception e){
            log.error("Error calculating monthly payment: {}", e.getMessage());
            throw new ServiceException("Error calculating monthly payment" + e.getMessage());
        }
    }

    @Override
    public BigDecimal payMonthlyInstallment(BigDecimal paymentAmount, String creditId, int paymentDay) {

        log.info("Paying monthly installment - credit: {}", creditId);

        try{
            List<CreditPaymentSchedule> creditInstallmentList = getMonthInstallments(creditId, paymentDay);

            BigDecimal amountPaid = BigDecimal.ZERO;
            BigDecimal totalAmount = calculateMonthlyPayment(creditId, paymentDay);

            if (paymentAmount.compareTo(totalAmount) != 0){
                throw new BusinessRuleException("Payment amount is different than monthly debt amount");
            }

            for (CreditPaymentSchedule creditInstallment : creditInstallmentList){

                creditInstallment.setStatus(InstallmentStatusEnum.PAID);
                creditInstallment.setUpdatedAt(LocalDateTime.now());
                amountPaid = amountPaid.add(creditInstallment.getInstallmentAmount());

                paymentScheduleRepository.save(creditInstallment);
            }

            log.info(" *** Successful payment *** ");

            return amountPaid;
        } catch (BusinessRuleException | CreditNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing monthly payment: {}", e.getMessage());
            throw new ServiceException("Error processing monthly payment" + e.getMessage());
        }
    }

    private boolean isOverdue(CreditPaymentSchedule creditInstallment) {
        return LocalDate.now().isAfter(creditInstallment.getDueDate());
    }
}
