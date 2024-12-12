package com.bm_nttdata.credit_ms.service.impl;

import com.bm_nttdata.credit_ms.dto.PaymentDetailsDto;
import com.bm_nttdata.credit_ms.entity.CreditCardInstallment;
import com.bm_nttdata.credit_ms.enums.InstallmentStatusEnum;
import com.bm_nttdata.credit_ms.exception.BusinessRuleException;
import com.bm_nttdata.credit_ms.exception.CreditNotFoundException;
import com.bm_nttdata.credit_ms.exception.ServiceException;
import com.bm_nttdata.credit_ms.repository.CreditCardInstallmentRepository;
import com.bm_nttdata.credit_ms.service.CreditCardInstallmentService;
import com.bm_nttdata.credit_ms.util.Constants;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de gestión de cuotas de tarjetas de crédito.
 * Maneja la lógica de negocio para el procesamiento de cuotas, cálculo de pagos
 * y gestión de estados de las cuotas.
 */
@Slf4j
@Transactional
@Service
public class CreditCardInstallmentServiceImpl implements CreditCardInstallmentService {

    @Autowired
    private CreditCardInstallmentRepository cardInstallmentRepository;


    /**
     * Obtiene las cuotas de una tarjeta de crédito según su estado.
     *
     * @param creditCardId ID de la tarjeta de crédito
     * @param status Estado de las cuotas a buscar
     * @return Lista de cuotas que coinciden con los criterios
     * @throws BusinessRuleException si el estado proporcionado no es válido
     * @throws ServiceException si ocurre un error durante la búsqueda
     */
    public List<CreditCardInstallment> getInstallmentsByCreditCardIdAndStatus(
            String creditCardId, String status) {

        try {
            InstallmentStatusEnum statusEnum = InstallmentStatusEnum.valueOf(status.toUpperCase());

            return cardInstallmentRepository.findByCreditCardIdAndStatus(creditCardId, statusEnum);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", status);
            throw new BusinessRuleException("Invalid installment status: " + status);
        } catch (Exception e) {
            log.error("Invalid status value: {}", status);
            throw new ServiceException(
                    "Error retrieving credit card installments" + e.getMessage());
        }
    }

    /**
     * Obtiene las cuotas vencidas del mes actual para una tarjeta de crédito.
     *
     * @param creditCardId ID de la tarjeta de crédito
     * @param paymentDay Día de pago establecido
     * @return Lista de cuotas vencidas
     * @throws CreditNotFoundException si no existen deudas
     * @throws ServiceException si ocurre un error durante la búsqueda
     */
    public List<CreditCardInstallment> getCurrentMonthDueInstallments(
            String creditCardId, int paymentDay) {

        try {
            LocalDate currentDate = LocalDate.now();
            LocalDate dueDate =
                    LocalDate.of(currentDate.getYear(), currentDate.getMonth(), paymentDay);

            // Si la fecha de pago ya pasó este mes, obtener la del próximo mes
            if (currentDate.isAfter(dueDate)) {
                dueDate = dueDate.plusMonths(1);
            }
            List<CreditCardInstallment> creditCardInstallmentList =
                    cardInstallmentRepository.findByCreditCardIdAndDueDateLessThanAndStatusNot(
                            creditCardId, dueDate, InstallmentStatusEnum.PAID);

            if (creditCardInstallmentList.isEmpty()) {
                throw new CreditNotFoundException("No debt exists");
            }

            return creditCardInstallmentList;

        } catch (Exception e) {
            log.error("Error getting current month due installments: {}", e.getMessage());
            throw new ServiceException(
                    "Error retrieving current month installments" + e.getMessage());
        }
    }

    /**
     * Calcula el pago del mes actual para una tarjeta de crédito específica.
     *
     * @param creditCardId ID de la tarjeta de crédito
     * @param paymentDay Día de pago establecido
     * @return Dto con el detalle a pagar en el mes actual
     * @throws ServiceException si ocurre un error durante el cálculo
     */
    @Override
    public PaymentDetailsDto calculateCurrentMonthPayment(String creditCardId, int paymentDay) {

        log.info("Calculating current month's credit card payment: {}", creditCardId);

        try {
            List<CreditCardInstallment> creditCardInstallmentList =
                    getCurrentMonthDueInstallments(creditCardId, paymentDay);

            BigDecimal totalInstallment = BigDecimal.ZERO;
            BigDecimal totalInterest = BigDecimal.ZERO;
            PaymentDetailsDto paymentDetails = new PaymentDetailsDto();

            for (CreditCardInstallment installment : creditCardInstallmentList) {
                BigDecimal baseAmount = installment.getTotalAmount();
                BigDecimal interestAmount = BigDecimal.ZERO;

                // Calcular monto con intereses si aplica
                if (isOverdue(installment)) {
                    try {
                        // Obtener días de retraso
                        long daysOverdue =
                                ChronoUnit.DAYS.between(installment.getDueDate(), LocalDate.now());

                        // Calcular interés diario (interes anual / 365)
                        BigDecimal dailyInterestRate =
                                BigDecimal.valueOf(Constants.LATE_PAYMENT_INTEREST / 365.0);

                        // Calcular monto de interés
                        interestAmount = baseAmount
                                .multiply(dailyInterestRate)
                                .multiply(BigDecimal.valueOf(daysOverdue));

                        // Actualizar estado de cuota mensual a VENCIDO, si aún no lo esta
                        if (installment.getStatus() != InstallmentStatusEnum.OVERDUE) {
                            installment.setStatus(InstallmentStatusEnum.OVERDUE);
                        }
                        installment.setTotalInterest(interestAmount);
                        installment.setDaysOverdue(daysOverdue);
                        installment.setUpdatedAt(LocalDateTime.now());

                        cardInstallmentRepository.save(installment);

                        totalInterest = totalInterest.add(interestAmount);
                    } catch (Exception e) {
                        log.error(
                                "Error updating overdue installment {}: {}",
                                installment.getId(), e.getMessage());
                        throw new ServiceException(
                                "Error updating overdue installment" + e.getMessage());
                    }
                }

                totalInstallment = totalInstallment.add(baseAmount);
            }

            paymentDetails.setPaymentAmount(totalInstallment);
            paymentDetails.setPaymentFee(totalInterest);
            paymentDetails.setTotalPayment(totalInstallment.add(totalInterest));

            return paymentDetails;

        } catch (Exception e) {
            log.error("Error calculating current month payment: {}", e.getMessage());
            throw new ServiceException("Error calculating current month payment" + e.getMessage());
        }
    }

    /**
     * Crea los cargos para un plan de cuotas de tarjeta de crédito.
     *
     * @param installmentAmount Monto de cada cuota
     * @param totalInstallments Número total de cuotas
     * @param creditCardId ID de la tarjeta de crédito
     * @param paymentDate Día de pago mensual
     * @throws ServiceException si ocurre un error durante la creación de cargos
     */
    @Override
    public void createCharges(
            BigDecimal installmentAmount, int totalInstallments,
            String creditCardId, int paymentDate) {

        log.info("Creating a list of credit card charges: {}", creditCardId);

        List<CreditCardInstallment> creditCardInstallmentList;

        try {
            creditCardInstallmentList = IntStream.range(0, totalInstallments)
                    .mapToObj(i -> CreditCardInstallment.builder()
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
        } catch (Exception e) {
            log.error("Unexpected error while Creating a list of installments: {}", e.getMessage());
            throw new ServiceException(
                    "Unexpected error while Creating a list of installments" + e.getMessage());
        }

        try {
            cardInstallmentRepository.saveAll(creditCardInstallmentList);
        } catch (Exception e) {
            log.error(
                    "Unexpected error while saving the list of installments: {}", e.getMessage());
            throw new ServiceException(
                    "Unexpected error while saving the list of installments" + e.getMessage());
        }
        log.info(" *** Successful creation *** ");
    }

    /**
     * Procesa el pago mensual de una tarjeta de crédito.
     *
     * @param installmentAmount Monto de la cuota a pagar
     * @param creditCardId ID de la tarjeta de crédito
     * @param paymentDay Día de pago
     * @return Dto con el detalle de lo pagado
     * @throws BusinessRuleException si el monto de pago es diferente al monto de deuda
     * @throws ServiceException si ocurre un error durante el pago
     */
    @Override
    public PaymentDetailsDto payBillMonth(
            BigDecimal installmentAmount, String creditCardId, int paymentDay) {

        log.info("Paying monthly credit card bill: {}", creditCardId);

        try {
            List<CreditCardInstallment> cardInstallmentList =
                    getCurrentMonthDueInstallments(creditCardId, paymentDay);

            BigDecimal amountPaid = BigDecimal.ZERO;
            PaymentDetailsDto paymentDetails =
                    calculateCurrentMonthPayment(creditCardId, paymentDay);

            if (installmentAmount.compareTo(paymentDetails.getTotalPayment()) != 0) {
                throw new BusinessRuleException(
                        "Payment amount is different than monthly debt amount");
            }

            for (CreditCardInstallment cardInstallment : cardInstallmentList) {

                cardInstallment.setStatus(InstallmentStatusEnum.PAID);
                cardInstallment.setUpdatedAt(LocalDateTime.now());
                amountPaid = amountPaid.add(cardInstallment.getTotalAmount());

                cardInstallmentRepository.save(cardInstallment);
            }
            log.info(" *** Successful payment *** ");

            return paymentDetails;
        } catch (BusinessRuleException | CreditNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing monthly bill payment: {}", e.getMessage());
            throw new ServiceException("Error processing monthly bill payment" + e.getMessage());
        }
    }

    /**
     * Verifica si existen cuotas vencidas para una tarjeta de crédito.
     *
     * @param creditId identificador de tarjeta de crédito
     * @param status estatus de la cuota
     * @return resultado si la tarjeta de credito cuenta con deudas vencidas
     */
    @Override
    public boolean getCustomerCreditCardDebts(String creditId, InstallmentStatusEnum status) {
        try {
            Long numberOverdueInstallments =
                    cardInstallmentRepository.countByCreditCardIdAndStatus(creditId, status);

            if (numberOverdueInstallments > 0) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error getting number of overdue installments : {}", e.getMessage());
            throw new ServiceException(
                    "Error getting number of overdue installments" + e.getMessage());
        }
    }

    /**
     * Verifica si una cuota está vencida.
     *
     * @param creditCardInstallment Cuota a verificar
     * @return true si la cuota está vencida, false en caso contrario
     */
    private boolean isOverdue(CreditCardInstallment creditCardInstallment) {
        return LocalDate.now().isAfter(creditCardInstallment.getDueDate());
    }
}
