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
        private ParameterizedType containerType;
        private Class<T> componentType;

        public static Ref<?> of(Type type) {
            return new Ref<>(type);
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
                this.containerType = parameterizedType;
                this.componentType = (Class<T>) parameterizedType.getActualTypeArguments()[0];
            } else {
                this.componentType = (Class<T>) type;
            }
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

        public Type getType() {
            return isContainer() ? containerType : componentType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Ref ref = (Ref) o;
            return Objects.equals(containerType, ref.containerType) && componentType.equals(ref.componentType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(containerType, componentType);
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
