package cn.jgzhan.lrpc.common.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/18
 */
public interface SingletonUtils {

    Map<String, Object> OBJECT_MAP = new ConcurrentHashMap<>();

    static <T> T getSingletonInstance(Class<T> clazz) {
        final String key = clazz.toString();
        if (OBJECT_MAP.containsKey(key)) {
            return clazz.cast(OBJECT_MAP.get(key));
        }
        synchronized (key) {
            if (OBJECT_MAP.containsKey(key)) {
                return clazz.cast(OBJECT_MAP.get(key));
            }
            try {
                final var instance = clazz.getDeclaredConstructor().newInstance();
                OBJECT_MAP.put(key, instance);
                return instance;
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
