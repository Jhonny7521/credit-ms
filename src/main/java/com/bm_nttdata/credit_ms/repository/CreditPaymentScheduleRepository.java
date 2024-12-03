package com.bm_nttdata.credit_ms.repository;

import com.bm_nttdata.credit_ms.entity.CreditPaymentSchedule;
import com.bm_nttdata.credit_ms.enums.InstallmentStatusEnum;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repositorio para la gestión de cronogramas de pago de créditos en MongoDB.
 * Proporciona operaciones de acceso a datos para la entidad CreditPaymentSchedule.
 */
public interface CreditPaymentScheduleRepository
        extends MongoRepository<CreditPaymentSchedule, String> {

    /**
     * Busca los cronogramas de pago de un crédito que tienen fecha de vencimiento anterior
     * a la especificada y que no están en el estado enviado.
     *
     * @param creditId ID del crédito
     * @param dueDate Fecha de vencimiento límite
     * @param status Estado de pago que se excluirá de la búsqueda
     * @return Lista de cronogramas de pago que coinciden con los criterios de búsqueda
     */
    List<CreditPaymentSchedule> findByCreditIdAndDueDateLessThanAndStatusNot(
            String creditId, LocalDate dueDate, InstallmentStatusEnum status);
}
