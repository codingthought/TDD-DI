package org.tdd.di;

import jakarta.inject.Inject;
import org.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

class InjectConstructionProvider<Type> implements ComponentProvider<Type> {
    private final Constructor<Type> constructor;

    InjectConstructionProvider(Class<? extends Type> component) {
        Constructor<?>[] declaredConstructors = component.getDeclaredConstructors();
        List<Constructor<?>> filteredConstructors = Arrays.stream(declaredConstructors)
                .filter(c -> Objects.nonNull(c.getAnnotation(Inject.class))).toList();
        if (filteredConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        if (filteredConstructors.size() == 0 &&
                Arrays.stream(declaredConstructors).allMatch(d -> d.getParameterTypes().length > 0)) {
            throw new IllegalComponentException();
        }
        if (filteredConstructors.size() == 1) {
            constructor = (Constructor<Type>) filteredConstructors.get(0);
        } else {
            constructor = (Constructor<Type>) declaredConstructors[0];
        }
    }

    @Override
    public Type getFrom(Container container) {
        try {
            return constructor.newInstance(Arrays.stream(constructor.getParameterTypes()).map(p -> container.get(p).get()).toArray());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Arrays.stream(constructor.getParameterTypes()).toList();
    }
}
