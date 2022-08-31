package fr.sparkit.accounting.services.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;

import org.reflections.Reflections;

public final class GenericServiceUtil {

    private GenericServiceUtil() {
        super();
    }

    public static List<Class> getClasses(String packageName) {

        Reflections reflections = new Reflections(packageName);

        reflections.getSubTypesOf(Object.class);

        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Entity.class);

        return Arrays.asList(annotated.toArray(new Class[annotated.size()]));
    }
}
