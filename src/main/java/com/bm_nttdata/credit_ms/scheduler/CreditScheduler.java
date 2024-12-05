package com.bm_nttdata.credit_ms.scheduler;

import com.bm_nttdata.credit_ms.entity.Credit;
import com.bm_nttdata.credit_ms.entity.CreditCard;
import com.bm_nttdata.credit_ms.entity.DailyCreditBalance;
import com.bm_nttdata.credit_ms.enums.CardStatusEnum;
import com.bm_nttdata.credit_ms.enums.CreditStatusEnum;
import com.bm_nttdata.credit_ms.repository.CreditCardRepository;
import com.bm_nttdata.credit_ms.repository.CreditRepository;
import com.bm_nttdata.credit_ms.repository.DailyCreditBalanceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Programador de tareas para la gestión de créditos.
 * Esta clase maneja las operaciones programadas relacionadas con el registro de saldos diarios.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CreditScheduler {

    private final CreditRepository creditRepository;

    private final CreditCardRepository creditCardRepository;

    private final DailyCreditBalanceRepository dailyCreditBalanceRepository;

    /**
     * Registra los saldos diarios de todos las créditos ACTIVOS.
     * Se ejecuta automáticamente todos los días a medianoche.
     * Obtiene todos los créditos ACTIVOS y registra sus saldos actuales.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void recordDailyBalances() {

        log.info("starts daily balance recording process");

        try {
            // Credits
            LocalDate currentDay = LocalDate.now();
            List<Credit> credits =
                    creditRepository.findByStatus(CreditStatusEnum.ACTIVE);

            for (Credit credit : credits) {
                try {
                    recordCreditDailyBalance(credit.getId(), credit.getBalance(), currentDay);

                } catch (Exception e) {
                    log.error(
                            "Error processing the daily balance for the account {} - {}: {}",
                            credit.getId(), credit.getCreditType().getValue(), e.getMessage());
                }
            }

            // Credit-Cards
            List<CreditCard> creditCards =
                    creditCardRepository.findByStatus(CardStatusEnum.ACTIVE);

            for (CreditCard creditCard : creditCards) {
                try {
                    recordCreditDailyBalance(
                            creditCard.getId(), creditCard.getAvailableCredit(), currentDay);

                } catch (Exception e) {
                    log.error(
                            "Error processing the daily balance for the account {} - {}: {}",
                            creditCard.getId(),
                            creditCard.getCardType().getValue(),
                            e.getMessage());
                }
            }

            log.info("Daily balance recording process completed");

        } catch (Exception e) {
            log.error("Error when processing daily balances: {}", e.getMessage());
        }
    }

    /**
     * Registra el saldo diario de un crédito específico.
     *
     * @param creditProductId  El producto de crédito cuyo saldo se va a registrar
     * @param currentBalance  El saldo que se va a registrar
     * @param date La fecha del registro
     */
    private void recordCreditDailyBalance(
            String creditProductId, BigDecimal currentBalance, LocalDate date) {

        try {
            DailyCreditBalance dailyBalance = DailyCreditBalance.builder()
                    .creditProductId(creditProductId)
                    .date(date)
                    .balance(currentBalance)
                    .build();
            dailyCreditBalanceRepository.save(dailyBalance);
        } catch (Exception e) {
            log.error("Database error while saving the daily balance: {}", e.getMessage());
            throw new RuntimeException("Error saving daily balance", e);
        }
    }
}
