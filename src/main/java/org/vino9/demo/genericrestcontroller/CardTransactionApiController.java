package org.vino9.demo.genericrestcontroller;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vino9.demo.genericrestcontroller.data.CardTransaction;
import org.vino9.demo.genericrestcontroller.data.EntityNotExistException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/transactions")
@Slf4j
public class CardTransactionApiController extends BaseRestApiController<CardTransaction, Long> {

    private PagingAndSortingRepository<CardTransaction, Long> repository;

    @Getter private int defaultPageSize = 2;
    @Getter private boolean paginationSupported = true;

    @Autowired
    public CardTransactionApiController(PagingAndSortingRepository repository) {
        this.repository = repository;
    }

    @Override
    public CardTransaction findEntityById(Long id) throws EntityNotExistException {
        Optional<CardTransaction> found = repository.findById(id);
        if (found.isPresent()) {
            return found.get();
        }

        String message = String.format("Entity of type %s with Long = %d not found", getClassForT().getName(), id);
        log.debug("{}", message);
        throw new EntityNotExistException(message);
    }

    @Override
    public CardTransaction saveEntity(CardTransaction entity) {
        return repository.save(entity);
    }

    @Override
    public Page<CardTransaction> queryForEntitiesByPage(Map<String, String> params, Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public List<CardTransaction> queryForEntities(Map<String, String> params) {
        return null;
    }
}
