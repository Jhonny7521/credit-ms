package com.bm_nttdata.credit_ms.repository;

import com.bm_nttdata.credit_ms.entity.Credit;
import com.bm_nttdata.credit_ms.enums.CreditStatusEnum;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la gestión de créditos en MongoDB.
 * Proporciona operaciones de acceso a datos para la entidad Credit.
 */
@Repository
public interface CreditRepository extends MongoRepository<Credit, String> {

    /**
     * Busca todos los créditos asociados a un cliente.
     *
     * @param id ID del cliente
     * @return Lista de créditos del cliente
     */
    List<Credit> findByCustomerId(String id);

    /**
     * Busca todos los créditos segun su estatus.
     *
     * @param status estatus del credito
     * @return Lista de créditos que cumplen con los criterios
     */
    List<Credit> findByStatus(CreditStatusEnum status);

    /**
     * Cuenta el número de créditos de un cliente que superan un monto específico (0).
     *
     * @param id ID del cliente
     * @param amount Monto mínimo de los créditos a contar
     * @return Número de créditos que cumplen con los criterios
     */
    long countByCustomerIdAndAmountGreaterThan(String id, BigDecimal amount);
}
