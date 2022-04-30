package org.tdd.di;

import jakarta.inject.Inject;
import org.tdd.di.exception.CycleDependencyNotAllowed;
import org.tdd.di.exception.DependencyNotFoundException;
import org.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Container {

    private final Map<Class<?>, Supplier<?>> MAP = new HashMap<>();

    private final Set<Dependency> typeDependencies = new HashSet<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        MAP.put(type, () -> instance);
    }

    public <Type, Impl extends Type> void bind(Class<Type> type, Class<? extends Type> implType) {
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
        Constructor<Impl> finalConstructor = (Constructor<Impl>) constructor;
        MAP.put(type, () -> newInstanceWith(finalConstructor));
    }

    private void checkCycleDependency() {
        Set<? extends Class<?>> components = typeDependencies.stream().map(Dependency::getComponentType).collect(Collectors.toSet());
        Set<? extends Class<?>> dependencies = typeDependencies.stream().map(Dependency::getDependencyType).collect(Collectors.toSet());
        System.out.printf("components: %s, dependencies: %s%n", components, dependencies);

        components.forEach(component -> hasDependency(component, 1));
    }

    private void hasDependency(Class<?> component, int length) {
        System.out.printf("component: %s, length: %d%n", component, length);
        if (length > typeDependencies.size() + 1) {
            throw new CycleDependencyNotAllowed();
        }
        typeDependencies.forEach(td -> {
            if (td.getComponentType().equals(component)) {
                hasDependency(td.getDependencyType(), length + 1);
            }
        });
    }

    private <Type> Type newInstanceWith(Constructor<Type> c) {
        checkCycleDependency();
        try {
            return c.newInstance(Arrays.stream(c.getParameterTypes())
                    .map(p -> {
                        typeDependencies.add(Dependency.of(c.getDeclaringClass().getInterfaces()[0], p));
                        return get(p).orElseThrow(DependencyNotFoundException::new);
                    }).toArray());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            new RuntimeException(e);
        }
        return null;
    }

    public <Type> Optional<Type> get(Class<Type> type) {
        return Optional.ofNullable(MAP.get(type)).map(p -> (Type) p.get());
    }
}

record Dependency(Class<?> componentType, Class<?> dependencyType) {
    public Class<?> getComponentType() {
        return componentType;
    }

    public Class<?> getDependencyType() {
        return dependencyType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return componentType.equals(that.componentType) && dependencyType.equals(that.dependencyType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentType, dependencyType);
    }

    public static Dependency of(Class<?> componentType, Class<?> dependencyType) {
        return new Dependency(componentType, dependencyType);
    }
}
