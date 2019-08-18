package org.vino9.demo.genericrestcontroller;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
import org.vino9.demo.genericrestcontroller.data.AccessDeniedException;
import org.vino9.demo.genericrestcontroller.data.EntityNotExistException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.*;

import static org.vino9.demo.genericrestcontroller.RestApiConstants.PAGINATION_DATA;
import static org.vino9.demo.genericrestcontroller.RestApiConstants.PAGINATION_META;
import static org.vino9.demo.genericrestcontroller.RestApiUtils.*;

@Slf4j
abstract public class BaseRestApiController<T, ID> {

    @Autowired
    private Jackson2ObjectMapperBuilder builder;

    private PagingAndSortingRepository<T, ID> repository;

    public BaseRestApiController(PagingAndSortingRepository<T, ID> repository) {
        this.repository = repository;
    }

    @GetMapping("{id}")
    public T getById(@PathVariable ID id) throws EntityNotExistException {
        return  findEntityById(id);
    }

    @GetMapping("")
    public HashMap<String, Object> list(@RequestParam(value = "id", required = false) ID id,
                                        @RequestParam Map<String, String> params,
                                        HttpServletRequest request) {

        HashMap<String, Object> result = new HashMap<>();

        // if id parameter exists then ignore all other parameters
        if (id != null) {
            ArrayList<T> entityList = new ArrayList<>();

            // we don't use getById to avoid unnecessary exception logs
            Optional<T> found = repository.findById(id);
            if (found.isPresent()) {
                entityList.add(found.get());
            }
            result.put(PAGINATION_DATA, entityList);
            return result;
        }

        Pageable pageable = getPageableFromParams(params, 2);
        // be careful, below call result in a select count query
        Page<T> resultPage = repository.findAll(pageable);

        result.put(PAGINATION_META, paginationMeta(resultPage, request.getRequestURI()));
        result.put(PAGINATION_DATA, resultPage.getContent());

        return result;
    }

    @PostMapping
    public ResponseEntity<T> post(@Valid @RequestBody T newEntity) {
        T savedEntity = repository.save(newEntity);
        return new ResponseEntity<>(savedEntity, HttpStatus.CREATED);
    }

    @PatchMapping("{id}")
    public T patch(@PathVariable("id") ID id,
                   @RequestBody String body) throws EntityNotExistException {

        T currentEntity = findEntityById(id);

        // we need to know exactly which fields the request body contain in order to know which fields to update
        // we'll deserialized the body twice, first time into a T object,
        //  2nd as a map then use the keys to determine which fields exists in the request body
        BeanWrapper wrapperSource = new BeanWrapperImpl(getEntityForPatch(body));
        BeanWrapper wrapperTarget = new BeanWrapperImpl(currentEntity);

        String idString = String.format("Entity {} with id = {}", currentEntity.getClass().getName(), id.toString());
        for (String key : getKeySetFromMap(body)) {
            if (! "id".equalsIgnoreCase(key)) {
                log.debug("Copying beaning property {} to {} ", key, idString);
                wrapperTarget.setPropertyValue(key, wrapperSource.getPropertyValue(key));
            }
        }

        return repository.save(currentEntity);
    }

    @PutMapping("{id}")
    public void put(@PathVariable("id") ID id,
                    @Valid @RequestBody T updatedEntity) throws EntityNotExistException {
        T currentEntity = findEntityById(id);
        BeanUtils.copyProperties(updatedEntity, currentEntity, new String[]{ "id" });
        repository.save(updatedEntity);
    }

    @ExceptionHandler(EntityNotExistException.class)
    public ResponseEntity<String> exceptionHandler(EntityNotExistException e) {
        log.info("{}", e);
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> exceptionHandler(AccessDeniedException e) {
        log.info("{}", e);
        return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
    }

    // Catch All exception handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> exceptionHandler(Exception e) {
        String message = String.format("%s - %s ", e.getClass().getName(), e.getMessage());
        log.info(message, e);
        return new ResponseEntity<String>(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private T findEntityById(ID id) throws EntityNotExistException {
        Optional<T> found = repository.findById(id);
        if (found.isPresent()) {
            return found.get();
        }

        String message = String.format("Entity of type %s with id = %s not found", getClassForT().getName(), id.toString());
        log.debug("{}", message);
        throw new EntityNotExistException(message);
    }

    // deserialize a json payload into an instance of T, used in handler for PATCH method
    private T getEntityForPatch(String requestBody) {
        // black magic to deal with Java type erasure
        // https://stackoverflow.com/questions/3403909/get-generic-type-of-class-at-runtime/19775924
        try {
            return builder.build().readValue(requestBody, getClassForT());
        } catch (IOException e) {
            // we're using a string, this is not possible
        }
        return null;
    }

    // deserialize a json payload into a Map, then return the keys as a set
    private Set<String> getKeySetFromMap(String mapJson) {
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
        try {
            HashMap<String, Object> patch = builder.build().readValue(mapJson, typeRef);
            return patch.keySet();
        } catch (IOException e) {
            // we're using a string, this is not possible
        }
        return new HashSet<String>();
    }

    private Class<T> getClassForT() {
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    } 
}
