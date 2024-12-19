package com.bm_nttdata.credit_ms.repository;

import com.bm_nttdata.credit_ms.entity.CreditCard;
import com.bm_nttdata.credit_ms.enums.CardStatusEnum;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la gestión de tarjetas de crédito en MongoDB.
 * Proporciona operaciones de acceso a datos para la entidad CreditCard.
 */
@Repository
public interface CreditCardRepository extends MongoRepository<CreditCard, String> {

    /**
     * Busca todas las tarjetas de crédito asociadas a un cliente.
     *
     * @param id ID del cliente
     * @return Lista de tarjetas de crédito del cliente
     */
    List<CreditCard> findByCustomerId(String id);

    /**
     * Busca todas las tarjetas de crédito segun su estatus.
     *
     * @param status estatus de la tarjeta de crédito
     * @return Lista de tarjetas de créditos que cumplen con los criterios
     */
    List<CreditCard> findByStatus(CardStatusEnum status);
}
