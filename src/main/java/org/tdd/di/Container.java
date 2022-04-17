package org.tdd.di;

import jakarta.inject.Inject;
import org.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Supplier;

public class Container {

    private static final Map<Class<?>, Supplier<?>> MAP = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        MAP.put(type, () -> instance);
    }

    public <Type, Impl extends Type> void bind(Class<Type> type, Class<? extends Type> implType) {
        Constructor<?> constructor = null;
        Constructor<?>[] declaredConstructors = implType.getDeclaredConstructors();
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
            constructor = filteredConstructors.get(0);
        }
        if (filteredConstructors.size() == 0) {
            constructor = declaredConstructors[0];
        }
        Constructor<Impl> finalConstructor = (Constructor<Impl>) constructor;
        MAP.put(type, () -> newInstanceWith(finalConstructor));
    }

    public <Type> Type get(Class<Type> type) {
        return Optional.ofNullable(MAP.get(type)).map(p -> (Type) p.get()).orElse(null);
    }

    private <Type> Type newInstanceWith(Constructor<Type> c) {
        try {
            return c.newInstance(Arrays.stream(c.getParameterTypes()).map(this::get).toArray());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            new RuntimeException(e);
        }
        return null;
    }
}
