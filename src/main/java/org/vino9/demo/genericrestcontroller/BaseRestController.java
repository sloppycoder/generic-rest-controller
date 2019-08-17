package org.vino9.demo.genericrestcontroller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;
import java.util.Optional;

import static org.vino9.demo.genericrestcontroller.RestApiUtils.getPageableFromParams;
import static org.vino9.demo.genericrestcontroller.RestApiUtils.paginationResult;

@Slf4j
abstract public class BaseRestController<T, ID> {

    private PagingAndSortingRepository<T, ID> repository;

    public BaseRestController(PagingAndSortingRepository<T, ID> repository) {
        this.repository = repository;
    }

    @GetMapping("{id}")
    public ResponseEntity<T> getById(@PathVariable ID id) {
        Optional<T> entity = repository.findById(id);
        if (entity.isPresent()) {
            log.debug("returning entity with id = {}", id);
            return new ResponseEntity<>(entity.get(), HttpStatus.OK);
        } else {
            //TODO: add error object with L10N error message
            log.debug("entity with id = {} not found", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("")
    public ResponseEntity list(@RequestParam(value = "id", required = false) ID id, @RequestParam Map<String, String> params, HttpServletRequest request) {

        // if id parameter exists then ignore all other parameters
        if (id != null) {
            return getById(id);
        }

        Pageable pageable = getPageableFromParams(params, 2);
        // be careful, below call result in a select count query
        Page<T> resultPage = repository.findAll(pageable);

        Map<String, Object> result = paginationResult(resultPage, request.getRequestURI());
        return new ResponseEntity(result, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<T> post(@Valid @RequestBody T newEntity) {
        T savedEntity = repository.save(newEntity);
        return new ResponseEntity<>(savedEntity, HttpStatus.CREATED);
    }


    @PatchMapping
    public ResponseEntity<T> patch(@RequestBody T updates) {
        // How do I know which attributes in T is present in request body??
        return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @PutMapping
    public ResponseEntity<T> put(@Valid @RequestBody T updatedEntity) {
        T savedEntity = repository.save(updatedEntity);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // Catch All exception handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> exceptionHandler(Exception e) {
        log.info("Exception", e);
        return new ResponseEntity<String>(
                String.format("%s - %s ", e.getClass().getName(), e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
