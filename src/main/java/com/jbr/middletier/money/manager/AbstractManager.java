package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.dto.ComparableNamedDTO;
import org.modelmapper.ModelMapper;
import org.springframework.data.repository.CrudRepository;

import java.util.*;

public abstract class AbstractManager<O,E extends ComparableNamedDTO,I,R extends CrudRepository<O, I>,ADD_EXCEPTION extends Throwable,UPDATE_DELETE_EXCEPTION extends Throwable> {
    private final Class<E> externalClass;
    private final Class<O> internalClass;
    private final ModelMapper modelMapper;
    private final R repository;
    private final Map<I,O> cache;

    protected AbstractManager(Class<E> externalClass, Class<O> internalClass, ModelMapper modelMapper, R repository) {
        this.externalClass = externalClass;
        this.internalClass = internalClass;
        this.modelMapper = modelMapper;
        this.repository = repository;
        this.cache = new HashMap<>();
    }

    abstract I getInstanceId(O instance);
    abstract ADD_EXCEPTION getAddException(I id);
    abstract UPDATE_DELETE_EXCEPTION getUpdateDeleteException(I id);
    abstract void updateInstance(O instance, O from);
    abstract void validateUpdateOrDelete(O instance, boolean update) throws UPDATE_DELETE_EXCEPTION;

    private void loadCache() {
        // Load the instances.
        if(this.cache.isEmpty()) {
            for (O next : repository.findAll()) {
                this.cache.put(getInstanceId(next), next);
            }
        }
    }

    public List<E> getAll() {
        this.loadCache();

        List<E> result = new ArrayList<>();

        // Return all the instances
        for(O next : this.cache.values()) {
            result.add(modelMapper.map(next,this.externalClass));
        }

        // Sort the list.
        result.sort(ComparableNamedDTO::compareTo);

        return result;
    }

    public List<O> getAllExternal() {
        this.loadCache();

        return new ArrayList<>(this.cache.values());
    }

    public Optional<O> getIfValid(I id) {
        this.loadCache();

        // Return the instance with id specified
        if(this.cache.containsKey(id)) {
            return Optional.of(this.cache.get(id));
        }

        return Optional.empty();
    }

    public O get(I id) throws UPDATE_DELETE_EXCEPTION {
        Optional<O> instance = getIfValid(id);

        // Return the instance with id, otherwise throw an exception
        if(instance.isPresent()) {
            return instance.get();
        }

        throw getUpdateDeleteException(id);
    }

    public List<E> create(E instance) throws ADD_EXCEPTION {
        this.loadCache();

        // Translate the external instance to the internal.
        O internalInstance = this.modelMapper.map(instance,internalClass);
        I instanceId = getInstanceId(internalInstance);

        // Create a new instance - ensure that it's not already in the list.
        if(this.cache.containsKey(instanceId)) {
            throw getAddException(instanceId);
        }

        // Save the new value.
        repository.save(internalInstance);
        this.cache.put(instanceId,internalInstance);

        return getAll();
    }

    private List<E> updateOrDelete(E instance, boolean update) throws UPDATE_DELETE_EXCEPTION {
        this.loadCache();

        // Translate the external instance to the internal.
        O internalInstance = this.modelMapper.map(instance,internalClass);
        I instanceId = getInstanceId(internalInstance);

        // Instance must exist.
        if(!this.cache.containsKey(instanceId)) {
            throw getUpdateDeleteException(instanceId);
        }

        // Get the actual internal instance
        internalInstance = this.cache.get(instanceId);

        // Validate that this is OK
        validateUpdateOrDelete(internalInstance,update);

        // Perform the action
        if(update) {
            // Update
            updateInstance(internalInstance,internalInstance);
            repository.save(internalInstance);
        } else {
            // Delete
            repository.delete(internalInstance);
            this.cache.remove(instanceId);
        }

        return getAll();
    }

    public List<E> update(E instance) throws UPDATE_DELETE_EXCEPTION {
        return updateOrDelete(instance,true);
    }

    public List<E> delete(E instance) throws UPDATE_DELETE_EXCEPTION {
        return updateOrDelete(instance,false);
    }
}
