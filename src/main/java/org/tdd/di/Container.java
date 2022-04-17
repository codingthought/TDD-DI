package org.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Container {

    private static final Map<Class<?>, Object> INSTANCE_MAP = new HashMap<>();
    private static final Map<Class<?>, Class<?>> TYPE_BIND_MAP = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        INSTANCE_MAP.put(type, instance);
    }

    public <Type> void bind(Class<Type> type, Class<? extends Type> implType) {
        TYPE_BIND_MAP.put(type, implType);
    }

    public <Type> Type get(Class<Type> type) {
        Type instance = (Type) INSTANCE_MAP.get(type);
        if (instance == null) {
            Class<?> implType = TYPE_BIND_MAP.get(type);

            Constructor<Type>[] constructors = (Constructor<Type>[]) implType.getDeclaredConstructors();
            instance = Arrays.stream(constructors).filter(c -> Objects.nonNull(c.getAnnotation(Inject.class))).findFirst().map(c -> {
                try {
                    return c.newInstance(Arrays.stream(c.getParameterTypes()).map(p -> get(p)).toArray());
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    new RuntimeException(e);
                }
                return null;
            }).orElseGet(() -> {
                try {
                    return constructors[0].newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    new RuntimeException(e);
                }
                return null;
            });
        }
        return instance;
    }
}
