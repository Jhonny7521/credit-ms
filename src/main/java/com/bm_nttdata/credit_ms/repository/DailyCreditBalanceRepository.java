package com.bm_nttdata.credit_ms.repository;

import com.bm_nttdata.credit_ms.entity.DailyCreditBalance;
import java.util.Date;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repositorio para la gestión de saldos diarios de cuentas de créditos en MongoDB.
 * Proporciona operaciones de acceso a datos para la entidad DailyBalance.
 */
public interface DailyCreditBalanceRepository extends MongoRepository<DailyCreditBalance, String> {

    /**
     * Busca todos los saldos diarios de un crédito en un período específico.
     *
     * @param accountId Identificador único del crédito
     * @param startDate fecha inicial del período de consulta
     * @param endDate fecha final del período de consulta
     * @return Lista de saldos diarios del crédito
     */
    List<DailyCreditBalance> findByCreditProductIdAndDateBetween(
            String accountId, Date startDate, Date endDate);
}
