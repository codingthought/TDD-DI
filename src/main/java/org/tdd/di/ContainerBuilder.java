package org.tdd.di;

import org.tdd.di.exception.CycleDependencyNotAllowed;
import org.tdd.di.exception.DependencyNotFoundException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class ContainerBuilder {

    private final Map<Class<?>, ComponentProvider<?>> componentProviders = new HashMap<>();

    public <Type> ContainerBuilder bind(Class<Type> type, Type instance) {
        componentProviders.put(type, (container) -> instance);
        return this;
    }

    public <Type> ContainerBuilder bind(Class<Type> type, Class<? extends Type> implType) {
        componentProviders.put(type, new InjectComponentProvider<>(implType));
        return this;
    }

    public Container build() {
        componentProviders.keySet().forEach(component -> checkCycleDependency(component, new Stack<>()));
        return new Container(componentProviders);
    }

    private void checkCycleDependency(Class<?> component, Stack<Class<?>> stack) {
        for (Type dependency : componentProviders.get(component).getTypeDependencies()) {
            if (dependency instanceof Class<?>) {
                checkDependency(component, stack, (Class<?>) dependency);
            } else {
                Class<?> type = (Class<?>)((ParameterizedType)dependency).getActualTypeArguments()[0];
                if (!componentProviders.containsKey(type)) {
                    throw new DependencyNotFoundException(component, type);
                }
            }
        }
    }

    private void checkDependency(Class<?> component, Stack<Class<?>> stack, Class<?> dependency) {
        if (!componentProviders.containsKey(dependency)) {
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
