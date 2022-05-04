package org.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tdd.di.exception.FinalFieldInjectException;
import org.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InjectTest {
    @Mock
    private Dependency dependency;
    @Mock
    private Provider<Dependency> dependencyProvider;
    @Mock(lenient = true)
    private Container container;
    private ParameterizedType providerType;

    @BeforeEach
    public void setup() throws NoSuchFieldException {
        when(container.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
        providerType = (ParameterizedType) getClass().getDeclaredField("dependencyProvider").getGenericType();
        when(container.get(eq(providerType))).thenReturn(Optional.of(dependencyProvider));
    }

    @Nested
    class InjectWithConstructorTest {

        @Nested
        class HappyPathTest {
            @Test
            void should_call_default_constructor_if_no_inject_constructor() {
                Component component = new InjectComponentProvider<>(ComponentNoDependency.class).getFrom(container);
                assertNotNull(component);
            }

            @Test
            void should_inject_dependency_via_inject_constructor() {
                ComponentDependentDependency component = new InjectComponentProvider<>(ComponentDependentDependency.class).getFrom(container);

                assertNotNull(component);
                assertSame(dependency, component.getDependency());
            }

            @Test
            void should_include_constructor_dependency_when_get_dependencies() {
                InjectComponentProvider<ComponentDependentDependency> provider = new InjectComponentProvider<>(ComponentDependentDependency.class);

                assertArrayEquals(new Class[]{Dependency.class}, provider.getDependencies().toArray());
            }

            @Test
            void should_include_provider_type_dependency_when_get_type_dependencies() {
                InjectComponentProvider<ConstructorInjectProvider> provider = new InjectComponentProvider<>(ConstructorInjectProvider.class);

                assertArrayEquals(new Type[]{providerType}, provider.getTypeDependencies().toArray());
            }

            @Test
            void should_support_inject_provider_dependency_via_inject_constructor() {
                ConstructorInjectProvider instance = new InjectComponentProvider<>(ConstructorInjectProvider.class).getFrom(container);

                assertSame(dependencyProvider, instance.dependency);
            }

            static class ConstructorInjectProvider {
                Provider<Dependency> dependency;
                @Inject
                public ConstructorInjectProvider(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }
        }

        @Nested
        class SadPathTest {
            @Test
            void should_throw_Exception_when_bind_if_multi_inject_constructor_provided() {
                assertThrows(IllegalComponentException.class, () ->
                        new InjectComponentProvider<>(ComponentWithMultiInjectConstructor.class));
            }

            @Test
            void should_throw_Exception_when_bind_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () ->
                        new InjectComponentProvider<>(ComponentWithNoInjectNorDefaultConstructor.class));
            }
        }
    }

    @Nested
    class InjectWithFieldTest {
        @Nested
        class HappyPathTest {
            @Test
            void should_return_component_when_get_if_dependency_has_bind() {
                ComponentInjectDependencyWithField component = new InjectComponentProvider<>(ComponentInjectDependencyWithField.class).getFrom(container);
                assertNotNull(component);
                assertSame(dependency, component.dependency);
            }

            static class ComponentInjectDependencyWithField implements Component {
                @Inject
                Dependency dependency;
            }

            static class SubComponent extends ComponentInjectDependencyWithField {
            }

            @Test
            void should_inject_supper_class_fields_when_get_subclass_component() {
                SubComponent component = new InjectComponentProvider<>(SubComponent.class).getFrom(container);
                assertNotNull(component);
                assertSame(dependency, component.dependency);
            }

            @Test
            void should_return_filed_dependency_when_get_dependencies() {
                InjectComponentProvider<ComponentInjectDependencyWithField> provider = new InjectComponentProvider<>(ComponentInjectDependencyWithField.class);

                assertArrayEquals(new Class[]{Dependency.class}, provider.getDependencies().toArray());
            }

            @Test
            void should_include_provider_type_dependency_when_get_type_dependencies() {
                InjectComponentProvider<FieldInjectProvider> provider = new InjectComponentProvider<>(FieldInjectProvider.class);

                assertArrayEquals(new Type[]{providerType}, provider.getTypeDependencies().toArray());
            }

            @Test
            void should_support_inject_provider_dependency_via_inject_field() {
                FieldInjectProvider instance = new InjectComponentProvider<>(FieldInjectProvider.class).getFrom(container);

                assertSame(dependencyProvider, instance.dependency);
            }

            static class FieldInjectProvider {
                @Inject
                Provider<Dependency> dependency;
            }
        }

        @Nested
        class SadPathTest {
            @Test
            void should_throw_exception_when_inject_field_is_final() {
                FinalFieldInjectException exception = assertThrows(FinalFieldInjectException.class,
                        () -> new InjectComponentProvider<>(ComponentWithFinalField.class).getFrom(container));
                assertEquals(ComponentWithFinalField.class, exception.getComponent());
                assertEquals("dependency", exception.getFieldName());
            }

            static class ComponentWithFinalField implements Component {
                @Inject
                private final Dependency dependency;

                ComponentWithFinalField() {
                    dependency = null;
                }
            }
        }
    }

    @Nested
    class InjectWithMethodTest {
        @Nested
        class HappyPathTest {
            @Test
            void should_return_component_when_get_if_dependency_has_bind() {
                ComponentInjectDependencyWithMethod component = new InjectComponentProvider<>(ComponentInjectDependencyWithMethod.class).getFrom(container);
                assertNotNull(component);
                assertSame(dependency, component.dependency);
                // 通过 Inject 标注的无参数方法，会被调用。
                assertTrue(component.injected);
            }

            static class ComponentInjectDependencyWithMethod implements Component {
                boolean injected = false;

                @Inject
                public void setDependency(Dependency dependency) {
                    this.dependency = dependency;
                }

                Dependency dependency;

                @Inject
                public void inject() {
                    this.injected = true;
                }
            }

            static class SubComponent extends ComponentInjectDependencyWithMethod {
                boolean subInjected = false;

                @Override
                @Inject
                public void inject() {
                    subInjected = true;
                }
            }

            @Test
            void should_call_sub_class_inject_method_only_if_override_supper_method() {
                SubComponent component = new InjectComponentProvider<>(SubComponent.class).getFrom(container);
                assertNotNull(component);
                assertSame(dependency, component.dependency);
                assertFalse(component.injected);
                assertTrue(component.subInjected);
            }

            @Test
            void should_call_supper_inject_method_before_sub_inject_method() {
                SubComponentWithAnotherInjectMethod component = new InjectComponentProvider<>(SubComponentWithAnotherInjectMethod.class).getFrom(container);

                assertNotNull(component);
                assertEquals(1, component.superInjectCall);
                assertEquals(2, component.subInjectCall);
            }

            static class SuperComponentWithInjectMethod {

                int superInjectCall = 0;
                @Inject
                public void supperInject() {
                    superInjectCall++;
                }

            }
            static class SubComponentWithAnotherInjectMethod extends SuperComponentWithInjectMethod {

                int subInjectCall = 0;
                @Inject
                public void subInject() {
                    subInjectCall = super.superInjectCall + 1;
                }

            }
            static class SubComponentOverrideMethod extends SuperComponentWithInjectMethod {

                int subInjectCall = 0;
                @Override
                public void supperInject() {
                    subInjectCall++;
                }

            }
            @Test
            void should_not_call_inject_method_when_sub_override_and_no_inject() {

                SubComponentOverrideMethod component = new InjectComponentProvider<>(SubComponentOverrideMethod.class).getFrom(container);

                assertNotNull(component);
                assertEquals(0, component.superInjectCall);
                assertEquals(0, component.subInjectCall);
            }

            @Test
            void should_return_method_inject_dependencies_when_get_dependencies() {
                InjectComponentProvider<ComponentInjectDependencyWithMethod> provider = new InjectComponentProvider<>(ComponentInjectDependencyWithMethod.class);

                assertArrayEquals(new Class[]{Dependency.class}, provider.getDependencies().toArray());
            }

            @Test
            void should_include_provider_type_dependency_when_get_type_dependencies() {
                InjectComponentProvider<MethodInjectProvider> provider = new InjectComponentProvider<>(MethodInjectProvider.class);

                assertArrayEquals(new Type[]{providerType}, provider.getTypeDependencies().toArray());
            }

            @Test
            void should_support_inject_provider_dependency_via_inject_method() {
                MethodInjectProvider instance = new InjectComponentProvider<>(MethodInjectProvider.class).getFrom(container);

                assertSame(dependencyProvider, instance.dependency);
            }

            static class MethodInjectProvider {
                Provider<Dependency> dependency;

                @Inject
                public void setDependency(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }
        }

        @Nested
        class SadPathTest {
            @Test
            void should_throw_exception_when_build_inject_method_defined_class_param() {
                assertThrows(IllegalComponentException.class, () -> new InjectComponentProvider<>(ComponentWithInjectMethodWithTypeParam.class));
            }

            static class ComponentWithInjectMethodWithTypeParam {
                @Inject
                public <T> void inject(T type) {
                }
            }
        }
    }
}
