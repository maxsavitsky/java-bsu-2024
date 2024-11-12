package by.bsu.dependency.context;

import by.bsu.dependency.annotation.Bean;
import by.bsu.dependency.annotation.BeanScope;
import by.bsu.dependency.annotation.Inject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class AbstractApplicationContext implements ApplicationContext {

    protected enum ContextStatus {
        NOT_STARTED,
        STARTED
    }

    protected ContextStatus contextStatus = ContextStatus.NOT_STARTED;

    protected final Map<Class<?>, BeanConfiguration> beanConfigurationMap = new HashMap<>();

    protected void initializeBeanConfigurations(List<Class<?>> beanClasses) {
        for (var cl : beanClasses) {
            String name = cl.getName();
            BeanScope scope = BeanScope.SINGLETON;
            if (cl.isAnnotationPresent(Bean.class)) {
                Bean b = cl.getAnnotation(Bean.class);
                if (!b.name().isEmpty()) {
                    name = b.name();
                }
                scope = b.scope();
            }

            List<Field> dependencies = Arrays.stream(cl.getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(Inject.class))
                    .toList();

            BeanConfiguration beanConfig = new BeanConfiguration(name, cl, scope, dependencies);
            beanConfigurationMap.put(cl, beanConfig);
        }
    }

    @Override
    public void start() {
        beanConfigurationMap.values().stream()
                .filter(bc -> bc.getBeanScope() == BeanScope.SINGLETON)
                .forEach(bc -> {
                    Object obj = createBeanFromClass(bc.getBeanClass());
                    bc.setBeanObject(obj);
                });
        beanConfigurationMap.values().stream()
                .filter(bc -> bc.getBeanScope() == BeanScope.SINGLETON)
                .forEach(bc -> {
                    Object obj = bc.getBeanObject().orElseThrow(()->new IllegalStateException("Injecting on null object"));
                    try {
                        injectDependencies(bc, obj);
                    } catch (IllegalAccessException e) {
                        throw new ApplicationContextNotStartedException(e);
                    }
                });
        contextStatus = ContextStatus.STARTED;
    }

    protected <T> T createBeanFromClass(Class<T> clazz) {
        if (!beanConfigurationMap.containsKey(clazz)) {
            throw new NoSuchBeanDefinitionException("Bean with type " + clazz.getName() + " does not exist");
        }
        BeanConfiguration beanConfiguration = beanConfigurationMap.get(clazz);
        if (beanConfiguration.getBeanObject().isPresent()) {
            return (T) beanConfiguration.getBeanObject().get();
        }

        Object obj;
        try {
            obj = beanConfiguration.getBeanClass().getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return (T) obj;
    }

    protected void injectDependencies(BeanConfiguration config, Object obj) throws IllegalAccessException {
        for (var field : config.getFieldDependencies()) {
            Object dependency = findObject(field.getType())
                    .orElseThrow(()->new NoSuchBeanDefinitionException("Bean for field " + field + " not found"));
            field.setAccessible(true);
            field.set(obj, dependency);
        }
    }

    @Override
    public Object getBean(String name) {
        if (contextStatus != ContextStatus.STARTED) {
            throw new ApplicationContextNotStartedException();
        }
        BeanConfiguration bc = beanConfigurationMap.values().stream()
                .filter(b -> b.getName().equals(name))
                .findFirst()
                .orElseThrow(()->new NoSuchBeanDefinitionException("Bean \"" + name + "\" not found"));
        if (bc.getBeanScope() == BeanScope.SINGLETON) {
            return bc.getBeanObject().orElseThrow(()->new IllegalStateException("Singleton object is not initialized"));
        }
        try {
            return instantiateBean(bc);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        if (contextStatus != ContextStatus.STARTED) {
            throw new ApplicationContextNotStartedException();
        }
        T obj = createBeanFromClass(clazz);
        BeanConfiguration beanConfiguration = beanConfigurationMap.get(clazz);
        try {
            injectDependencies(beanConfiguration, obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return obj;
    }

    protected Object instantiateBean(BeanConfiguration bc) throws IllegalAccessException {
        if (bc.getBeanObject().isPresent()) {
            return bc.getBeanObject().get();
        }

        Object obj;
        try {
            obj = bc.getBeanClass().getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        injectDependencies(bc, obj);
        return obj;
    }

    private Optional<Object> findObject(Class<?> cl) {
        if (!beanConfigurationMap.containsKey(cl)) {
            return Optional.empty();
        }
        return beanConfigurationMap.get(cl).getBeanObject();
    }

    private void checkRunning() {
        if (!isRunning()) {
            throw new ApplicationContextNotStartedException();
        }
    }

    private void checkIfBeanExists(String name) {
        if (beanConfigurationMap.values().stream().noneMatch(bc -> bc.getName().equals(name))) {
            throw new NoSuchBeanDefinitionException("Bean \"" + name + "\" not defined");
        }
    }

    @Override
    public boolean isRunning() {
        return contextStatus == ContextStatus.STARTED;
    }

    @Override
    public boolean containsBean(String name) {
        checkRunning();
        return beanConfigurationMap.values().stream()
                .anyMatch(bc -> bc.getName().equals(name));
    }

    @Override
    public boolean isPrototype(String name) {
        checkRunning();
        checkIfBeanExists(name);
        return beanConfigurationMap.values().stream()
                .anyMatch(bc -> bc.getName().equals(name) && bc.getBeanScope() == BeanScope.PROTOTYPE);
    }

    @Override
    public boolean isSingleton(String name) {
        checkRunning();
        checkIfBeanExists(name);
        return beanConfigurationMap.values().stream()
                .anyMatch(bc -> bc.getName().equals(name) && bc.getBeanScope() == BeanScope.SINGLETON);
    }

    protected static class BeanConfiguration {

        private final String name;
        private final Class<?> beanClass;
        private final BeanScope beanScope;
        private final List<Field> fieldDependencies;
        private Object beanObject;

        public BeanConfiguration(String name, Class<?> beanClass, BeanScope beanScope, List<Field> fieldDependencies) {
            this.name = name;
            this.beanClass = beanClass;
            this.beanScope = beanScope;
            this.fieldDependencies = fieldDependencies;
        }

        public Optional<Object> getBeanObject() {
            return beanObject == null ? Optional.empty() : Optional.of(beanObject);
        }

        public String getName() {
            return name;
        }

        public Class<?> getBeanClass() {
            return beanClass;
        }

        public BeanScope getBeanScope() {
            return beanScope;
        }

        public List<Field> getFieldDependencies() {
            return fieldDependencies;
        }

        public void setBeanObject(Object beanObject) {
            this.beanObject = beanObject;
        }
    }

}
