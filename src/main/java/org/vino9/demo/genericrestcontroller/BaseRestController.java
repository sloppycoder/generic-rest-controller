package org.vino9.demo.genericrestcontroller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.vino9.demo.genericrestcontroller.data.EntityNotExistException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.vino9.demo.genericrestcontroller.RestApiConstants.PAGINATION_DATA;
import static org.vino9.demo.genericrestcontroller.RestApiConstants.PAGINATION_META;
import static org.vino9.demo.genericrestcontroller.RestApiUtils.*;

@Slf4j
abstract public class BaseRestController<T, ID> {

    @Autowired
    private Jackson2ObjectMapperBuilder builder;

    private PagingAndSortingRepository<T, ID> repository;

    public BaseRestController(PagingAndSortingRepository<T, ID> repository) {
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
        // we'll deserialized the body twice, first time into a T object, 2nd as a map
        // then use the map keys to find out which fields exists in the request body
        try {
            ObjectMapper mapper = builder.build();

            // black magic to deal with Java type erasure
            // https://stackoverflow.com/questions/3403909/get-generic-type-of-class-at-runtime/19775924
            Class<T> clazz = getClassForT();
            T entityPatch = mapper.readValue(body, getClassForT());
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

    private Class<T> getClassForT() {
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    } 

}
