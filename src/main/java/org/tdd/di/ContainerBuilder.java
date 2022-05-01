package org.tdd.di;

import jakarta.inject.Inject;
import org.tdd.di.exception.DependencyNotFoundException;
import org.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

public class ContainerBuilder {

    private final Map<Class<?>, ComponentProvider<?>> componentProviders = new HashMap<>();

    private final Map<Class<?>, List<Class<?>>> componentDependencies = new HashMap<>();

    public <Type> ContainerBuilder bind(Class<Type> type, Type instance) {
        componentProviders.put(type, (container) -> instance);
        componentDependencies.put(type, List.of());
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
        componentProviders.put(type, new InjectConstructionProvider<>(constructor));
        componentDependencies.put(constructor.getDeclaringClass(), Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toList()));
        return this;
    }

    public Container build() {
        for (Class<?> component : componentDependencies.keySet()) {
            for (Class<?> dependency : componentDependencies.get(component)) {
                if (!componentDependencies.containsKey(dependency) && componentDependencies.keySet().stream()
                        .noneMatch(t -> Arrays.asList(t.getInterfaces()).contains(dependency))) {
                    throw new DependencyNotFoundException(component, dependency);
                }
            }
        }
        return new Container(componentProviders);
    }
}
