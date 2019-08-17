package org.vino9.demo.genericrestcontroller.data;

import org.springframework.data.repository.CrudRepository;

public interface CardTransactionRepository extends CrudRepository<CardTransaction, Long> {
}
