package org.tdd.di;

import jakarta.inject.Inject;
import org.tdd.di.exception.CycleDependencyNotAllowed;
import org.tdd.di.exception.DependencyNotFoundException;
import org.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Supplier;

public class Container {

    private final Map<Class<?>, Supplier<?>> MAP = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        MAP.put(type, () -> instance);
    }

    public <Type> void bind(Class<Type> type, Class<? extends Type> implType) {
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
        MAP.put(type, new InjectConstruction<>(constructor));
    }

    class InjectConstruction<Type> implements Supplier<Type> {
        private final Constructor<Type> constructor;
        private boolean constructing = false;

        public InjectConstruction(Constructor<Type> constructor) {
            this.constructor = constructor;
        }

        @Override
        public Type get() {
            if (constructing) {
                throw new CycleDependencyNotAllowed();
            }
            try {
                constructing = true;
                return constructor.newInstance(Arrays.stream(constructor.getParameterTypes())
                        .map(p -> Container.this.get(p).orElseThrow(() ->
                                new DependencyNotFoundException(constructor.getDeclaringClass(), p)))
                        .toArray());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                new RuntimeException(e);
            } finally {
                constructing = false;
            }
            return null;
        }
    }

    public <Type> Optional<Type> get(Class<Type> type) {
        return Optional.ofNullable(MAP.get(type)).map(p -> (Type) p.get());
    }
}
