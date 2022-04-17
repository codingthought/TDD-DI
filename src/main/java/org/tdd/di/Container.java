package org.tdd.di;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class Container {
    private static final Map<Class<?>, Object> INSTANCE_MAP = new HashMap<>();

    private static final Map<Class<?>, Class<?>> TYPE_BIND_MAP = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        INSTANCE_MAP.put(type, instance);
    }

    public <Type> Type get(Class<Type> type) {
        Object instance = INSTANCE_MAP.get(type);
        if (instance == null) {
            try {
                return (Type) TYPE_BIND_MAP.get(type).getDeclaredConstructors()[0].newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                new RuntimeException(e);
            }
        }
        return (Type) instance;
    }

    public <Type> void bind(Class<Type> type, Class<? extends Type>  subType) {
        TYPE_BIND_MAP.put(type, subType);
    }
}
