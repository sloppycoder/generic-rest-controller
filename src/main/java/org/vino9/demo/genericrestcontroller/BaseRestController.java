package org.vino9.demo.genericrestcontroller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.vino9.demo.genericrestcontroller.RestApiUtils.getPageableFromParams;
import static org.vino9.demo.genericrestcontroller.RestApiUtils.paginationResult;

@Slf4j
abstract public class BaseRestController<T, ID> {

    @Autowired
    private Jackson2ObjectMapperBuilder builder;

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
    public ResponseEntity list(@RequestParam(value = "id", required = false) ID id,
                               @RequestParam Map<String, String> params,
                               HttpServletRequest request) {

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

    @PatchMapping("{id}")
    public ResponseEntity<T> patch(@PathVariable("id") ID id,
                                   @RequestBody String body) {

        Optional<T> foundEntity = repository.findById(id);
        if (! foundEntity.isPresent()) {
            log.debug("entity with id = {} does not exist, cannot patch", id);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        T currentEntity = foundEntity.get();

        ObjectMapper mapper = builder.build();

        // we need to know exactly which fields the request body contain in order to know which fields to update
        // we'll deserialized the body twice, first time into a T object, 2nd as a map
        // then use the map keys to find out which fields exists in the request body
        try {

            // black magic to deal with Java type erasure
            // https://stackoverflow.com/questions/3403909/get-generic-type-of-class-at-runtime/19775924
            Class<T> clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            T entityPatch = mapper.readValue(body, clazz);
            String className = clazz.getName();

            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
            HashMap<String, Object> patch = mapper.readValue(body, typeRef);

            BeanWrapper wrapperSource = new BeanWrapperImpl(entityPatch);
            BeanWrapper wrapperTarget = new BeanWrapperImpl(currentEntity);
            Object entityId = wrapperSource.getPropertyValue("id");

            for (String key : patch.keySet()) {
                if (! "id".equalsIgnoreCase(key)) {
                    log.debug("copying beaning property to entity {} with id of {} ", className, entityId, key);
                    wrapperTarget.setPropertyValue(key, wrapperSource.getPropertyValue(key));
                }
            }
        } catch (IOException e) {
            // since we're reading from a string, this is impossible...
        }

        T updatedEntity = repository.save(currentEntity);

        return new ResponseEntity<T>(updatedEntity, HttpStatus.OK);
    }

    @PutMapping("{id}")
    public ResponseEntity<T> put(@PathVariable("id") ID id, @Valid @RequestBody T updatedEntity) {
        // what to do if request body contains an id that is different from path variable??
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
