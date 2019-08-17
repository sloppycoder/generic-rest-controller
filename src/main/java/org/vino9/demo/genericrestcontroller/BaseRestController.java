package org.vino9.demo.genericrestcontroller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;
import java.util.Optional;

@Slf4j
abstract public class BaseRestController<T, ID> {

    private PagingAndSortingRepository<T, ID> repository;
    private IdConverter<ID> idIdConverter;

    public BaseRestController(PagingAndSortingRepository<T, ID> repository, IdConverter<ID> converter) {
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
    public ResponseEntity list(HttpServletRequest request) {
        String idParam = request.getParameter("id");

        // if id parameter exists then ignore all other parameters
        if (idParam != null && !idParam.isEmpty()) {
            return getById(idIdConverter.convert(idParam));
        }

        int page = 0;
        int perPage = 0;

        String pageParam = request.getParameter("page");
        String perPageParam = request.getParameter("per_page");

        if (pageParam != null && !pageParam.isEmpty()) {
            page = Integer.valueOf(pageParam);
        }

        if (perPageParam != null && !perPageParam.isEmpty()) {
            perPage = Integer.valueOf(perPageParam);
        }

        if (page == 0 || perPage == 0) {
            log.info("Invalid pagination parameter, page = {}, per_page = {}", page, perPage);
            return new ResponseEntity("invalid pagination parameters", HttpStatus.BAD_REQUEST);
        }


        Pageable pageable = PageRequest.of(perPage * (page-1), perPage * page );
        Page<T> resultPage = repository.findAll(pageable);

        Map<String, Object> meta  = Map.of(
                "page", page,
                "per_page", perPage,
                "curr", request.getRequestURI() + "?page=" + page + "&per_page=" + perPage
        );

        Map<String, Object> result = Map.of(
                "_meta_", meta,
                "data", resultPage.getContent()
        );

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

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<T> options() {
        return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<T> head() {
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
