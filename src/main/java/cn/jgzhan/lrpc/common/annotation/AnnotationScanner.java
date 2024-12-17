package cn.jgzhan.lrpc.common.annotation;

import cn.jgzhan.lrpc.common.dto.Pair;
import cn.jgzhan.lrpc.example.api.TestService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/9
 */
@Slf4j
public class AnnotationScanner {
    public static void main(String[] args) {
//        final var scanPackage = Config.getConsumerScanPackage();
//        ClassLoader loader = Thread.currentThread().getContextClassLoader();
//        Set<Class<?>> classes = getClasses(scanPackage, loader);
//        final var classes1 = getFieldsForAnnotation(classes, LrpcReference.class);
//        System.out.println("classes1 = " + classes1);

        System.out.println("TestService.class.getGenericInterfaces() = " + Arrays.toString(TestService.class.getGenericInterfaces()));

    }

    public static Set<Class<?>> getClasses(String packageName, ClassLoader loader) {
        Set<Class<?>> classes = new HashSet<>();
        String path = packageName.replace('.', '/');
        try {
            URL packageURL = loader.getResource(path);
            assert packageURL != null;
            File directory = new File(packageURL.getFile());
            if (directory.exists()) {
                findClasses(directory, packageName, classes);
            }
        } catch (NullPointerException e) {
            log.error("Package not found: {}, e", packageName, e);
        }
        return classes;
    }


    private static void findClasses(File directory, String packageName, Set<Class<?>> classes) {
        if (!directory.exists()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                // 递归处理子包
                findClasses(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                // 加载类并添加到列表中
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    log.error("Class not found: {}", className);
                }
            }
        }
    }

    @NonNull
    public static Map<Class<?>, Set<String>> getFieldsForLrpcReference(Set<Class<?>> classes) {
        final Map<Class<?>, Set<String>> result = new HashMap<>();
        for (Class<?> clazz : classes) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isAnnotationPresent(LrpcReference.class)) {
                    continue;
                }
                // 获取注解的addressArr属性
                final LrpcReference annotation = field.getAnnotation(LrpcReference.class);
                final String[] addressArr = annotation.addressArr();
                // 保存类该属性的类
                result.computeIfAbsent(clazz, k -> new HashSet<>()).addAll(Arrays.asList(addressArr));
            }
        }
        return result;
    }

    public static Set<Pair<Class<?>, Object>> groupByClassAndName(Set<Class<?>> classes) {
        final Set<Pair<Class<?>, Object>> result = new HashSet<>();
        for (Class<?> clz : classes) {
            try {
                findService(clz, result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        // provider.clz 一样的合并里面的map
        return result;
    }

    private static void findService(Class<?> clz,Set<Pair<Class<?>, Object>> result) throws Exception {
        if (!clz.isAnnotationPresent(LrpcService.class)) {
            return;
        }

        final var genericInterfaces = clz.getGenericInterfaces();
        if (genericInterfaces.length == 0) {
            final Object implInstance = clz.cast(clz.getDeclaredConstructor().newInstance());
            result.add(Pair.of(clz, implInstance));
            return;
        }
        final Class<?> interFace = Class.forName(genericInterfaces[0].getTypeName());
        final Object implInstance = clz.cast(clz.getDeclaredConstructor().newInstance());
        result.add(Pair.of(interFace, implInstance));
    }

}
