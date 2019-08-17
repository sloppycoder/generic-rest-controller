package org.vino9.demo.genericrestcontroller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletRequest;
import javax.validation.Valid;
import java.util.Optional;

@Slf4j
abstract public class BaseRestController<T, ID> {

    private CrudRepository<T, ID> repository;
    private IdConverter<ID> idIdConverter;

    public BaseRestController(CrudRepository<T, ID> repository, IdConverter<ID> converter) {
        this.repository = repository;
        this.idIdConverter = converter;
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
    public ResponseEntity<T> list(ServletRequest request) {
        String idParam = request.getParameter("id");

        // if id parameter exists then ignore all other parameters
        if (idParam != null && !idParam.isEmpty()) {
            return getById(idIdConverter.convert(idParam));
        }

        // do pagination stuff here
        repository.findAll();
        return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
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

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<T> options() {
        return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<T> head() {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // Exception handlers
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> exceptionHandler(Exception e) {
        log.info("Exception", e);
        return new ResponseEntity<String>(
                String.format("%s - %s ", e.getClass().getName(), e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
