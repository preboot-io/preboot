package io.preboot.eventbus;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class LocalEventHandlerRepository implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(LocalEventHandlerRepository.class);
    private final Map<Class<?>, List<HandlerMethod>> eventHandlers = new HashMap<>();
    private final ReentrantLock initializationLock = new ReentrantLock();
    private volatile boolean initialized = false;
    private ApplicationContext applicationContext;

    private record HandlerMethod(Object instance, Method method, int priority, Class<?> typeParameter) {}

    public LocalEventHandlerRepository(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        // Defer handler scanning until first use
    }

    private void initializeHandlers() {
        if (initialized) {
            return; // Already initialized
        }

        initializationLock.lock();
        try {
            // Double-checked locking pattern
            if (initialized) {
                return; // Another thread already initialized
            }

            String[] beanNames = applicationContext.getBeanDefinitionNames();
            for (String beanName : beanNames) {
                try {
                    Object bean = applicationContext.getBean(beanName);
                    scanBean(bean);
                } catch (Exception e) {
                    log.debug("Could not process bean {} for event handlers: {}", beanName, e.getMessage());
                }
            }

            initialized = true;
        } finally {
            initializationLock.unlock();
        }
    }

    private void scanBean(Object bean) {
        Class<?> targetClass = bean.getClass();

        // Check if the class is public
        final boolean isNotPublicClass = !Modifier.isPublic(targetClass.getModifiers());

        for (Method method : targetClass.getMethods()) {
            EventHandler annotation = method.getAnnotation(EventHandler.class);
            if (annotation != null) {
                if (isNotPublicClass) {
                    log.error(
                            "Skipping event handler registration for non-public class: {}. Event handlers must be in public classes.",
                            targetClass.getName());
                    return;
                }

                if (method.getParameterCount() != 1) {
                    log.warn("Event handler method {} must have exactly one parameter", method.getName());
                    continue;
                }

                Class<?> eventType = method.getParameterTypes()[0];
                Class<?> typeParameter = annotation.typeParameter();

                // Use void.class as a marker for "no type parameter filter"
                if (typeParameter == void.class) {
                    typeParameter = null;
                }

                HandlerMethod handlerMethod = new HandlerMethod(bean, method, annotation.priority(), typeParameter);

                eventHandlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handlerMethod);
                eventHandlers.get(eventType).sort((h1, h2) -> Integer.compare(h2.priority(), h1.priority()));

                String typeParamInfo = typeParameter != null ? " with type parameter " + typeParameter.getName() : "";
                log.info(
                        "Registered event handler method: {}.{} for event type: {}{}",
                        bean.getClass().getName(),
                        method.getName(),
                        eventType.getName(),
                        typeParamInfo);
            }
        }
    }

    public void publish(final Object event) {
        initializeHandlers(); // Initialize on first use

        Class<?> eventType = event.getClass();
        List<HandlerMethod> handlers = eventHandlers.get(eventType);

        if (handlers != null) {
            for (HandlerMethod handler : handlers) {
                try {
                    // Skip if handler has type parameter constraint and event type parameter doesn't match
                    if (handler.typeParameter() != null
                            && event instanceof GenericEvent<?> genericEvent
                            && !handler.typeParameter().isInstance(genericEvent.getTypeParameter())) {
                        continue;
                    }

                    handler.method().invoke(handler.instance(), event);
                    log.debug(
                            "Event {} handled by {}.{}",
                            eventType.getName(),
                            handler.instance().getClass().getName(),
                            handler.method().getName());
                } catch (Exception e) {
                    throw new EventPublishException(e);
                }
            }
        }
    }

    public <T> boolean isHandlerMissing(final T event) {
        initializeHandlers(); // Initialize on first use
        Class<?> eventType = event.getClass();
        List<HandlerMethod> handlers = eventHandlers.get(eventType);
        return handlers == null || handlers.isEmpty();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
