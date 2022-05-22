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

    public <T> ContainerBuilder bind(Class<T> type, T instance) {
        componentProviders.put(type, (container) -> instance);
        return this;
    }

    public <T> ContainerBuilder bind(Class<T> type, Class<? extends T> implType) {
        componentProviders.put(type, new InjectComponentProvider<>(implType));
        return this;
    }

    public Container build() {
        componentProviders.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Container(componentProviders);
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> stack) {
        for (Type dependency : componentProviders.get(component).getTypeDependencies()) {
            Class<?> clazz = getComponentType(dependency);
            checkExist(component, clazz);
            if (isContainer(dependency)) {
                checkCycleDependencies(stack, clazz);
            }
        }
    }

    private boolean isContainer(Type dependency) {
        return dependency instanceof Class<?>;
    }

    private Class<?> getComponentType(Type dependency) {
        return isContainer(dependency) ? (Class<?>) dependency :
                (Class<?>) ((ParameterizedType) dependency).getActualTypeArguments()[0];
    }

    private void checkExist(Class<?> component, Class<?> dependency) {
        if (!componentProviders.containsKey(dependency)) {
            throw new DependencyNotFoundException(component, dependency);
        }
    }

    private void checkCycleDependencies(Stack<Class<?>> stack, Class<?> dependency) {
        if (stack.contains(dependency)) {
            throw new CycleDependencyNotAllowed(stack);
        }
        stack.push(dependency);
        checkDependencies(dependency, stack);
        stack.pop();
    }
}
