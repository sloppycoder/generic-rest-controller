package org.vino9.demo.genericrestcontroller;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import static org.vino9.demo.genericrestcontroller.RestApiUtils.*;

@Slf4j
abstract public class BaseRestApiController<T, ID> {

    @Autowired
    private Jackson2ObjectMapperBuilder builder;

    @GetMapping("{id}")
    public T getById(@PathVariable ID id) throws EntityNotExistException {
        return  findEntityById(id);
    }

    @GetMapping("")
    public HashMap<String, Object> list(@RequestParam(value = "id", required = false) ID id,
                                        @RequestParam Map<String, String> params,
                                        HttpServletRequest request) {

        HashMap<String, Object> result = new HashMap<>();

        if (isPaginationSupported()) {
            Pageable pageable = getPageableFromParams(params, getDefaultPageSize());
            // be careful, below call result in a select count query
            Page<T> resultPage = queryForEntitiesByPage(params, pageable);

            result.put(PAGINATION_META, paginationMeta(resultPage, request.getRequestURI()));
            result.put(PAGINATION_DATA, resultPage.getContent());
        } else {
            ArrayList<T> entityList = new ArrayList<>();
            queryForEntities(params).forEach( item -> entityList.add(item) );
            result.put(PAGINATION_DATA, entityList);
        }

        return result;
    }

    @PostMapping
    public ResponseEntity<T> post(@Valid @RequestBody T newEntity) {
        T savedEntity = saveEntity(newEntity);
        return new ResponseEntity<>(savedEntity, HttpStatus.CREATED);
    }

    @PatchMapping("{id}")
    public T patch(@PathVariable("id") ID id,
                   @RequestBody String body) throws EntityNotExistException {

        T currentEntity = findEntityById(id);

        // we need to know exactly which fields the request body contain in order to know which fields to update
        // we'll deserialized the body twice, first time into a T object,
        //  2nd as a map then use the keys to determine which fields exists in the request body
        patchEntity(getEntityForPatch(body), currentEntity, getKeySetFromMap(body));

        return saveEntity(currentEntity);
    }

    @PutMapping("{id}")
    public void put(@PathVariable("id") ID id,
                    @Valid @RequestBody T updatedEntity) throws EntityNotExistException {
        T currentEntity = findEntityById(id);
        BeanUtils.copyProperties(updatedEntity, currentEntity);
        saveEntity(updatedEntity);
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

    // methods related to handle the data entities are delegated to the child class
    abstract public T findEntityById(ID id) throws EntityNotExistException;
    abstract public T saveEntity(T entity);
    abstract public Iterable<T> queryForEntities(Map<String, String> params);
    abstract public Page<T> queryForEntitiesByPage(Map<String, String> params, Pageable pageable);
    abstract public boolean isPaginationSupported();
    abstract public int getDefaultPageSize();

    // private methods

    // use properties from source entity to update target entity
    protected T patchEntity(T source, T target, Set<String> keys) {
        BeanWrapper wrapperSource = new BeanWrapperImpl(source);
        BeanWrapper wrapperTarget = new BeanWrapperImpl(target);

        keys.stream().forEach( key -> {
            log.debug("Copying beaning property {} to {} ", key, target.toString());
            wrapperTarget.setPropertyValue(key, wrapperSource.getPropertyValue(key));
        });

        return target;
    }


    // deserialize a json payload into an instance of T, used in handler for PATCH method
    protected T getEntityForPatch(String requestBody) {
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
    protected Set<String> getKeySetFromMap(String mapJson) {
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
        try {
            HashMap<String, Object> patch = builder.build().readValue(mapJson, typeRef);
            return patch.keySet();
        } catch (IOException e) {
            // we're using a string, this is not possible
        }
        return new HashSet<String>();
    }

    protected Class<T> getClassForT() {
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    } 
}
