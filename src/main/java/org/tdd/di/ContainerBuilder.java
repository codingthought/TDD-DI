package org.tdd.di;

import jakarta.inject.Inject;
import org.tdd.di.exception.CycleDependencyNotAllowed;
import org.tdd.di.exception.DependencyNotFoundException;
import org.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.Constructor;
import java.util.*;

public class ContainerBuilder {

    private final Map<Class<?>, ComponentProvider<?>> componentProviders = new HashMap<>();

    public <Type> ContainerBuilder bind(Class<Type> type, Type instance) {
        componentProviders.put(type, (container) -> instance);
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
        return this;
    }

    public Container build() {
        componentProviders.keySet().forEach(component -> checkCycleDependency(component, new Stack<>()));
        return new Container(componentProviders);
    }

    private void checkCycleDependency(Class<?> component, Stack<Class<?>> stack) {
        for (Class<?> dependency : getDependencies(component)) {
            if (!componentProviders.containsKey(dependency) && componentProviders.keySet().stream()
                    .noneMatch(t -> Arrays.asList(t.getInterfaces()).contains(dependency))) {
                throw new DependencyNotFoundException(component, dependency);
            }
            if (stack.contains(dependency)) {
                throw new CycleDependencyNotAllowed(stack);
            }
            stack.push(dependency);
            checkCycleDependency(dependency, stack);
            stack.pop();
        }
    }

    private List<Class<?>> getDependencies(Class<?> component) {
        return Optional.ofNullable(componentProviders.get(component)).map(ComponentProvider::getDependencies).orElseGet(List::of);
    }
}
