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

    public <Type> void bind(Class<Type> type, Class<? extends Type> implType) {
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
        MAP.put(type, () -> newInstance(implType));
    }

    public <Type> Type get(Class<Type> type) {
        return Optional.ofNullable(MAP.get(type)).map(p -> (Type) p.get()).orElse(null);
    }

    private <Type> Type newInstance(Class<? extends Type> implType) {
        Constructor<Type>[] constructors = (Constructor<Type>[]) implType.getDeclaredConstructors();
        return Arrays.stream(constructors).filter(c -> Objects.nonNull(c.getAnnotation(Inject.class)))
                .findFirst().map(this::newInstanceWith).orElseGet(() -> newInstanceWith(constructors[0]));
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
