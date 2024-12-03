package com.bm_nttdata.credit_ms.repository;

import com.bm_nttdata.credit_ms.entity.CreditCardInstallment;
import com.bm_nttdata.credit_ms.enums.InstallmentStatusEnum;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la gestión de cuotas de tarjetas de crédito en MongoDB.
 * Proporciona operaciones de acceso a datos para la entidad CreditCardInstallment.
 */
@Repository
public interface CreditCardInstallmentRepository
        extends MongoRepository<CreditCardInstallment, String> {

    /**
     * Busca las cuotas de una tarjeta de crédito por su ID y estado.
     *
     * @param creditCardId ID de la tarjeta de crédito
     * @param statusEnum Estado de las cuotas a buscar
     * @return Lista de cuotas que coinciden con los criterios de búsqueda
     */
    List<CreditCardInstallment> findByCreditCardIdAndStatus(
            String creditCardId, InstallmentStatusEnum statusEnum);

    /**
     * Busca las cuotas de una tarjeta de crédito que tienen fecha de vencimiento anterior
     * a la especificada y que no están en el estado indicado.
     *
     * @param creditCardId ID de la tarjeta de crédito
     * @param dueDate Fecha de vencimiento límite
     * @param statusEnum Estado que se excluirá de la búsqueda
     * @return Lista de cuotas que coinciden con los criterios de búsqueda
     */
    List<CreditCardInstallment> findByCreditCardIdAndDueDateLessThanAndStatusNot(
            String creditCardId, LocalDate dueDate, InstallmentStatusEnum statusEnum);
}
