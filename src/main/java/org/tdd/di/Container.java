package org.tdd.di;

import java.util.HashMap;
import java.util.Map;

public class Container {
    private static final Map<Class<?>, Object> INSTANCE_MAP = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        INSTANCE_MAP.put(type, instance);
    }

    public <Type> Type get(Class<Type> type) {
        return (Type) INSTANCE_MAP.get(type);
    }

}
