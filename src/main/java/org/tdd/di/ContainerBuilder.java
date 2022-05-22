package org.tdd.di;

import org.tdd.di.exception.CycleDependencyNotAllowed;
import org.tdd.di.exception.DependencyNotFoundException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

    static class Ref<T> {
        private Type container;
        private Class<T> component;

        public static <T> Ref<T> of(Type type) {
            return new Ref<>(type);
        }

        public static <T> Ref<T> of(Class<T> componentType) {
            return new Ref<>(componentType);
        }

        private Ref(Class<T> componentType) {
            init(componentType);
        }

        private Ref(Type containerType) {
            init(containerType);
        }

        protected Ref() {
            Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            init(type);
        }

        private void init(Type type) {
            if (type instanceof ParameterizedType parameterizedType) {
                this.container = parameterizedType.getRawType();
                this.component = (Class<T>) parameterizedType.getActualTypeArguments()[0];
            } else {
                this.component = (Class<T>) type;
            }
        }

        public Type getContainer() {
            return container;
        }

        public Class<T> getComponent() {
            return component;
        }

        public boolean isContainer() {
            return container != null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Ref<?> ref = (Ref<?>) o;
            return Objects.equals(container, ref.container) && component.equals(ref.component);
        }

        @Override
        public int hashCode() {
            return Objects.hash(container, component);
        }
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> stack) {
        for (Ref<?> dependency : componentProviders.get(component).getDependencies()) {
            checkExist(component, dependency.getComponent());
            if (!dependency.isContainer()) {
                checkCycleDependencies(stack, dependency.getComponent());
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
