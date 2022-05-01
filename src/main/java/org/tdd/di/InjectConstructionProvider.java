package org.tdd.di;

import org.tdd.di.exception.CycleDependencyNotAllowed;
import org.tdd.di.exception.DependencyNotFoundException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

class InjectConstructionProvider<Type> implements ComponentProvider<Type> {
    private final Constructor<Type> constructor;
    private boolean constructing = false;

    public InjectConstructionProvider(Constructor<Type> constructor) {
        this.constructor = constructor;
    }

    @Override
    public Type getFrom(Container container) {
        if (constructing) {
            throw new CycleDependencyNotAllowed(constructor.getDeclaringClass());
        }
        try {
            constructing = true;
            return constructor.newInstance(Arrays.stream(constructor.getParameterTypes())
                    .map(p -> container.get(p).orElseThrow(() ->
                            new DependencyNotFoundException(constructor.getDeclaringClass(), p)))
                    .toArray());
        } catch (CycleDependencyNotAllowed e) {
            throw new CycleDependencyNotAllowed(e, constructor.getDeclaringClass());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            new RuntimeException(e);
        } finally {
            constructing = false;
        }
        return null;
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Arrays.stream(constructor.getParameterTypes()).toList();
    }
}
