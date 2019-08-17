package org.vino9.demo.genericrestcontroller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vino9.demo.genericrestcontroller.data.CardTransaction;
import org.vino9.demo.genericrestcontroller.data.CardTransactionRepository;

@RestController
@RequestMapping("/transactions")
public class TransactionController extends BaseRestController<CardTransaction, Long> {

    @Autowired
    public TransactionController(CardTransactionRepository repo) {
        super(repo);
    }
}
