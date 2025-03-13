package com.trainticket.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.trainticket.entity.Payment;
import org.springframework.stereotype.Repository;

/**
 * @author fdse
 */
@Repository
public interface PaymentRepository extends CrudRepository<Payment, String> {
    Optional<Payment> findById(String id);

    Payment findByOrderId(String orderId);

    @Override
    List<Payment> findAll();

    List<Payment> findByUserId(String userId);
}
