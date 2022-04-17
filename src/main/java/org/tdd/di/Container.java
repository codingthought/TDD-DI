package org.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Supplier;

public class Container {

    private static final Map<Class<?>, Class<?>> TYPE_BIND_MAP = new HashMap<>();
    private static final Map<Class<?>, Supplier<?>> MAP = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        MAP.put(type, () -> instance);
    }

    public <Type> void bind(Class<Type> type, Class<? extends Type> implType) {
        TYPE_BIND_MAP.put(type, implType);
    }

    public <Type> Type get(Class<Type> type) {
        return Optional.ofNullable(MAP.get(type)).map(p -> (Type) p.get()).orElseGet(() -> {
            Constructor<Type>[] constructors = (Constructor<Type>[]) TYPE_BIND_MAP.get(type).getDeclaredConstructors();
            return Arrays.stream(constructors).filter(c -> Objects.nonNull(c.getAnnotation(Inject.class)))
                    .findFirst().map(this::newInstanceWith).orElseGet(() -> newInstanceWith(constructors[0]));
        });
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
