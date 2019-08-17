package org.vino9.demo.genericrestcontroller.data;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface CardTransactionRepository extends PagingAndSortingRepository<CardTransaction, Long> {
}
