package net.momo.silo.core;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Service locator for plugin-wide services. Thread-safe. */
public final class Services {

    private static final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    private Services() {}

    /** Registers a service instance. Overwrites any existing registration. */
    public static <T> void register(Class<T> type, T instance) {
        if (type == null || instance == null) {
            throw new IllegalArgumentException("Type and instance must not be null");
        }
        services.put(type, instance);
    }

    /** Gets a required service. Throws if not registered. */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type) {
        Object service = services.get(type);
        if (service == null) {
            throw new IllegalStateException("Service not registered: " + type.getName());
        }
        return (T) service;
    }

    /** Gets an optional service. */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> find(Class<T> type) {
        return Optional.ofNullable((T) services.get(type));
    }

    /** Checks if a service is registered. */
    public static boolean has(Class<?> type) {
        return services.containsKey(type);
    }

    /** Clears all registered services. Use for testing or shutdown. */
    public static void clear() {
        services.clear();
    }
}
