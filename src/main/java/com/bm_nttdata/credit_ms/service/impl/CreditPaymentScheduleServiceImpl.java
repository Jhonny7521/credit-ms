package com.bm_nttdata.credit_ms.service.impl;

import com.bm_nttdata.credit_ms.dto.PaymentDetailsDto;
import com.bm_nttdata.credit_ms.entity.Credit;
import com.bm_nttdata.credit_ms.entity.CreditPaymentSchedule;
import com.bm_nttdata.credit_ms.enums.InstallmentStatusEnum;
import com.bm_nttdata.credit_ms.exception.BusinessRuleException;
import com.bm_nttdata.credit_ms.exception.CreditNotFoundException;
import com.bm_nttdata.credit_ms.exception.ServiceException;
import com.bm_nttdata.credit_ms.repository.CreditPaymentScheduleRepository;
import com.bm_nttdata.credit_ms.service.CreditPaymentScheduleService;
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
 * Implementación del servicio de cronograma de pagos de créditos.
 * Gestiona la creación y mantenimiento de cronogramas de pago, cálculo de cuotas
 * y procesamiento de pagos mensuales.
 */
@Slf4j
@Transactional
@Service
public class CreditPaymentScheduleServiceImpl implements CreditPaymentScheduleService {

    @Autowired
    private CreditPaymentScheduleRepository paymentScheduleRepository;

    /**
     * Crea un cronograma de pagos para un crédito.
     * Genera las cuotas mensuales para todo el período del crédito.
     *
     * @param credit Crédito para el cual se creará el cronograma
     * @throws ServiceException si ocurre un error durante la generación o guardado del cronograma
     */
    @Override
    public void createPaymentSchedule(Credit credit) {

        log.info("Creating monthly payment list for customer: {} - credit: {}",
                credit.getCustomerId(), credit.getId());
        
        List<CreditPaymentSchedule> listMonthlyInstallments;

        try {
            listMonthlyInstallments = IntStream.range(0, credit.getTerm())
                    .mapToObj(i -> CreditPaymentSchedule.builder()
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

        } catch (Exception e) {
            log.error("Unexpected error while generating monthly payment list: {}", e.getMessage());
            throw new ServiceException("Unexpected error while generating monthly payment list");
        }

        try {
            paymentScheduleRepository.saveAll(listMonthlyInstallments);
        } catch (Exception e) {
            log.error("Unexpected error while saving monthly payment list: {}", e.getMessage());
            throw new ServiceException("Unexpected error while saving monthly payment list");
        }
        log.info(" *** Successful creation *** ");
    }

    /**
     * Obtiene las cuotas mensuales pendientes de pago.
     *
     * @param creditId ID del crédito
     * @param paymentDay Día de pago establecido
     * @return Lista de cuotas pendientes
     * @throws CreditNotFoundException si no existen deudas pendientes
     * @throws ServiceException si ocurre un error durante la búsqueda
     */
    public List<CreditPaymentSchedule> getMonthInstallments(String creditId, int paymentDay) {

        try {
            LocalDate currentDate = LocalDate.now();
            LocalDate dueDate =
                    LocalDate.of(currentDate.getYear(), currentDate.getMonth(), paymentDay);

            // Si la fecha de pago ya pasó este mes, obtener la del próximo mes
            if (currentDate.isAfter(dueDate)) {
                dueDate = dueDate.plusMonths(1);
            }

            List<CreditPaymentSchedule> paymentScheduleList =
                    paymentScheduleRepository.findByCreditIdAndDueDateLessThanAndStatusNot(
                            creditId, dueDate, InstallmentStatusEnum.PAID);

            if (paymentScheduleList.isEmpty()) {
                throw new CreditNotFoundException("No debt exists");
            }

            return paymentScheduleList;

        } catch (Exception e) {
            log.error("Error getting current month due installments: {}", e.getMessage());
            throw new ServiceException(
                    "Error retrieving current month installments" + e.getMessage());
        }
    }

    /**
     * Calcula el pago mensual considerando intereses por mora si aplica.
     * Si una cuota está vencida, calcula los intereses moratorios basados en los días de retraso.
     *
     * @param creditId ID del crédito
     * @param installmentNumber Número de cuota
     * @return Dto con el detalle a pagar en el mes
     * @throws ServiceException si ocurre un error durante el cálculo
     */
    @Override
    public PaymentDetailsDto calculateMonthlyPayment(String creditId, int installmentNumber) {

        log.info("Calculating the monthly payment.: {}", creditId);

        try {

            List<CreditPaymentSchedule> creditInstallmentList =
                    getMonthInstallments(creditId, installmentNumber);

            BigDecimal totalInstallment = BigDecimal.ZERO;
            BigDecimal totalInterest = BigDecimal.ZERO;
            PaymentDetailsDto paymentDetails = new PaymentDetailsDto();

            for (CreditPaymentSchedule creditInstallment : creditInstallmentList) {

                BigDecimal installmentAmount = creditInstallment.getInstallmentAmount();
                BigDecimal interestAmount;

                if (isOverdue(creditInstallment)) {
                    try {
                        // Obtener días de retraso
                        long daysOverdue = ChronoUnit.DAYS
                                .between(creditInstallment.getDueDate(), LocalDate.now());

                        // Calcular interés diario (interes anual / 365)
                        BigDecimal dailyInterestRate =
                                BigDecimal.valueOf(Constants.LATE_PAYMENT_INTEREST / 365.0);

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

                        totalInterest = totalInterest.add(interestAmount);
                    } catch (Exception e) {
                        log.error("Error updating overdue installment {}: {}",
                                creditInstallment.getId(), e.getMessage());
                        throw new ServiceException(
                                "Error updating overdue installment" + e.getMessage());
                    }
                }

                totalInstallment = totalInstallment.add(installmentAmount);
            }

            paymentDetails.setPaymentAmount(totalInstallment);
            paymentDetails.setPaymentFee(totalInterest);
            paymentDetails.setTotalPayment(totalInstallment.add(totalInterest));

            return paymentDetails;
        } catch (Exception e) {
            log.error("Error calculating monthly payment: {}", e.getMessage());
            throw new ServiceException("Error calculating monthly payment" + e.getMessage());
        }
    }

    /**
     * Procesa el pago de una cuota mensual.
     * Verifica que el monto del pago coincida con la deuda total y actualiza el estado
     * de las cuotas.
     *
     * @param paymentAmount Monto del pago
     * @param creditId ID del crédito
     * @param paymentDay Día de pago
     * @return Dto con el detalle de lo pagado
     * @throws BusinessRuleException si el monto del pago no coincide con la deuda
     * @throws ServiceException si ocurre un error durante el procesamiento
     */
    @Override
    public PaymentDetailsDto payMonthlyInstallment(
            BigDecimal paymentAmount, String creditId, int paymentDay) {

        log.info("Paying monthly installment - credit: {}", creditId);

        try {
            List<CreditPaymentSchedule> creditInstallmentList =
                    getMonthInstallments(creditId, paymentDay);

            BigDecimal amountPaid = BigDecimal.ZERO;
            PaymentDetailsDto paymentDetails = calculateMonthlyPayment(creditId, paymentDay);

            if (paymentAmount.compareTo(paymentDetails.getTotalPayment()) != 0) {
                throw new BusinessRuleException(
                        "Payment amount is different than monthly debt amount");
            }

            for (CreditPaymentSchedule creditInstallment : creditInstallmentList) {

                creditInstallment.setStatus(InstallmentStatusEnum.PAID);
                creditInstallment.setUpdatedAt(LocalDateTime.now());
                amountPaid = amountPaid.add(creditInstallment.getInstallmentAmount());

                paymentScheduleRepository.save(creditInstallment);
            }

            log.info(" *** Successful payment *** ");

            return paymentDetails;
        } catch (BusinessRuleException | CreditNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing monthly payment: {}", e.getMessage());
            throw new ServiceException("Error processing monthly payment" + e.getMessage());
        }
    }

    /**
     * Verifica si existen cuotas vencidas para un crédito.
     *
     * @param creditId identificador de crédito
     * @param status estatus de la cuota
     * @return resultado si el credito cuenta con deudas vencidas
     */
    @Override
    public boolean getCustomerCreditDebts(String creditId, InstallmentStatusEnum status) {

        try {
            Long numberOverdueInstallments =
                    paymentScheduleRepository.countByCreditIdAndStatus(creditId, status);

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
     * @param creditInstallment Cuota a verificar
     * @return true si la fecha actual es posterior a la fecha de vencimiento
     */
    private boolean isOverdue(CreditPaymentSchedule creditInstallment) {
        return LocalDate.now().isAfter(creditInstallment.getDueDate());
    }
}
