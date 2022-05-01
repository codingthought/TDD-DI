package org.tdd.di;

import jakarta.inject.Inject;
import org.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.Constructor;
import java.util.*;

public class ContainerBuilder {

    private final Map<Class<?>, ComponentProvider<?>> MAP = new HashMap<>();

    public <Type> ContainerBuilder bind(Class<Type> type, Type instance) {
        MAP.put(type, (container) -> instance);
        return this;
    }

    public <Type> ContainerBuilder bind(Class<Type> type, Class<? extends Type> implType) {
        Constructor<?> constructor;
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
        } else {
            constructor = declaredConstructors[0];
        }
        MAP.put(type, new InjectConstructionProvider<>(constructor));
        return this;
    }

    public Container build() {
        return new Container(MAP);
    }
}
