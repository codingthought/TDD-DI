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

    static class Ref {
        private ParameterizedType containerType;
        private final Class<?> componentType;

        public static Ref of(Type type) {
            if (type instanceof ParameterizedType parameterizedType) {
                return new Ref(parameterizedType);
            } else {
                return new Ref((Class<?>) type);
            }
        }

        private Ref(Class<?> componentType) {
            this.componentType = componentType;
        }

        private Ref(ParameterizedType containerType) {
            this.componentType = (Class<?>) containerType.getActualTypeArguments()[0];
            this.containerType = containerType;
        }

        public ParameterizedType getContainerType() {
            return containerType;
        }

        public Class<?> getComponentType() {
            return componentType;
        }

        public boolean isContainer() {
            return containerType != null;
        }
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> stack) {
        for (Type dependency : componentProviders.get(component).getDependencies()) {
            Ref ref = Ref.of(dependency);
            checkExist(component, ref.getComponentType());
            if (!ref.isContainer()) {
                checkCycleDependencies(stack, ref.getComponentType());
            }
        }
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
