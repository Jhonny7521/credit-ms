package com.bm_nttdata.credit_ms.service.impl;

import com.bm_nttdata.credit_ms.entity.CreditCardInstallment;
import com.bm_nttdata.credit_ms.enums.InstallmentStatusEnum;
import com.bm_nttdata.credit_ms.exception.BusinessRuleException;
import com.bm_nttdata.credit_ms.exception.CreditNotFoundException;
import com.bm_nttdata.credit_ms.exception.ServiceException;
import com.bm_nttdata.credit_ms.repository.CreditCardInstallmentRepository;
import com.bm_nttdata.credit_ms.service.ICreditCardInstallmentService;
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
public class CreditCardInstallmentServiceImpl implements ICreditCardInstallmentService {

    @Autowired
    private CreditCardInstallmentRepository cardInstallmentRepository;


    public List<CreditCardInstallment> getInstallmentsByCreditCardIdAndStatus(String creditCardId, String status) {

        try{
            InstallmentStatusEnum statusEnum = InstallmentStatusEnum.valueOf(status.toUpperCase());

            return cardInstallmentRepository.findByCreditCardIdAndStatus(creditCardId, statusEnum);
        } catch (IllegalArgumentException e){
            log.error("Invalid status value: {}", status);
            throw new BusinessRuleException("Invalid installment status: " + status);
        }catch (Exception e){
            log.error("Invalid status value: {}", status);
            throw new ServiceException("Error retrieving credit card installments" + e.getMessage());
        }
    }

    public List<CreditCardInstallment> getCurrentMonthDueInstallments(String creditCardId, int paymentDay) {

        try{
            LocalDate currentDate = LocalDate.now();
            LocalDate dueDate = LocalDate.of( currentDate.getYear(), currentDate.getMonth(), paymentDay );

            // Si la fecha de pago ya pasó este mes, obtener la del próximo mes
            if (currentDate.isAfter(dueDate)) {
                dueDate = dueDate.plusMonths(1);
            }
            List<CreditCardInstallment> creditCardInstallmentList = cardInstallmentRepository
                    .findByCreditCardIdAndDueDateLessThanAndStatusNot( creditCardId, dueDate, InstallmentStatusEnum.PAID );

            if (creditCardInstallmentList.isEmpty()) throw new CreditNotFoundException("No debt exists");

            return creditCardInstallmentList;

        } catch (Exception e){
            log.error("Error getting current month due installments: {}", e.getMessage());
            throw new ServiceException("Error retrieving current month installments" + e.getMessage());
        }
    }

    @Override
    public BigDecimal calculateCurrentMonthPayment(String creditCardId, int paymentDay){

        log.info("Calculating current month's credit card payment: {}", creditCardId);

        try {
            List<CreditCardInstallment> creditCardInstallmentList = getCurrentMonthDueInstallments(creditCardId, paymentDay);

            BigDecimal totalAmount = BigDecimal.ZERO;

            for (CreditCardInstallment installment : creditCardInstallmentList) {
                BigDecimal baseAmount = installment.getTotalAmount();
                BigDecimal interestAmount = BigDecimal.ZERO;

                // Calcular monto con intereses si aplica
                if (isOverdue(installment)) {
                    try{
                        // Obtener días de retraso
                        long daysOverdue = ChronoUnit.DAYS.between(installment.getDueDate(), LocalDate.now());

                        // Calcular interés diario (interes anual / 365)
                        BigDecimal dailyInterestRate = BigDecimal.valueOf(Constants.LATE_PAYMENT_INTEREST / 365.0);

                        // Calcular monto de interés
                        interestAmount = baseAmount
                                .multiply(dailyInterestRate)
                                .multiply(BigDecimal.valueOf(daysOverdue));

                        // Actualizar estado de cuota mensual a VENCIDO (OVERDUE), si aún no lo esta
                        if (installment.getStatus() != InstallmentStatusEnum.OVERDUE) {
                            installment.setStatus(InstallmentStatusEnum.OVERDUE);
                        }
                        installment.setTotalInterest(interestAmount);
                        installment.setDaysOverdue(daysOverdue);
                        installment.setUpdatedAt(LocalDateTime.now());

                        cardInstallmentRepository.save(installment);
                    }catch (Exception e){
                        log.error("Error updating overdue installment {}: {}", installment.getId(), e.getMessage());
                        throw new ServiceException("Error updating overdue installment" + e.getMessage());
                    }
                }

                totalAmount = totalAmount.add(baseAmount).add(interestAmount);
            }

            return totalAmount;

        } catch (Exception e){
            log.error("Error calculating current month payment: {}", e.getMessage());
            throw new ServiceException("Error calculating current month payment" + e.getMessage());
        }
    }

    @Override
    public void createCharges(BigDecimal installmentAmount, int totalInstallments, String creditCardId, int paymentDate) {

        log.info("Creating a list of credit card charges: {}", creditCardId);

        List<CreditCardInstallment> creditCardInstallmentList;

        try {
            creditCardInstallmentList = IntStream.range(0, totalInstallments)
                    .mapToObj( i -> CreditCardInstallment.builder()
                            .creditCardId(creditCardId)
                            .installmentNumber(i)
                            .totalInstallments(totalInstallments)
                            .totalAmount(installmentAmount)
                            .dueDate(LocalDate.now().plusMonths(i + 1).withDayOfMonth(paymentDate))
                            .status(InstallmentStatusEnum.PENDING)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build()
                    )
                    .collect(Collectors.toList());
        } catch (Exception e){
            log.error("Unexpected error while Creating a list of credit card charges: {}", e.getMessage());
            throw new ServiceException("Unexpected error while Creating a list of credit card charges" + e.getMessage());
        }

        try {
            cardInstallmentRepository.saveAll(creditCardInstallmentList);
        }catch (Exception e){
            log.error("Unexpected error while saving the list of credit card charges: {}", e.getMessage());
            throw new ServiceException("Unexpected error while saving the list of credit card charges" + e.getMessage());
        }
        log.info(" *** Successful creation *** ");
    }

    @Override
    public BigDecimal payBillMonth(BigDecimal installmentAmount, String creditCardId, int paymentDay) {

        log.info("Paying monthly credit card bill: {}", creditCardId);

        try{
            List<CreditCardInstallment> cardInstallmentList = getCurrentMonthDueInstallments(creditCardId, paymentDay);

            BigDecimal amountPaid = BigDecimal.ZERO;
            BigDecimal totalAmount = calculateCurrentMonthPayment(creditCardId, paymentDay);

            if (installmentAmount.compareTo(totalAmount) != 0){
                throw new BusinessRuleException("Payment amount is different than monthly debt amount");
            }

            for(CreditCardInstallment cardInstallment : cardInstallmentList){

                cardInstallment.setStatus(InstallmentStatusEnum.PAID);
                cardInstallment.setUpdatedAt(LocalDateTime.now());
                amountPaid = amountPaid.add(cardInstallment.getTotalAmount());

                cardInstallmentRepository.save(cardInstallment);
            }
            log.info(" *** Successful payment *** ");

            return amountPaid;
        } catch (BusinessRuleException | CreditNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing monthly bill payment: {}", e.getMessage());
            throw new ServiceException("Error processing monthly bill payment" + e.getMessage());
        }
    }

    private boolean isOverdue(CreditCardInstallment creditCardInstallment) {
        return LocalDate.now().isAfter(creditCardInstallment.getDueDate());
    }
}
