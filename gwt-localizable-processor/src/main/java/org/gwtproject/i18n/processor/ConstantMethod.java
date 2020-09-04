package org.gwtproject.i18n.processor;

import org.gwtproject.i18n.client.Constants;
import org.gwtproject.i18n.client.LocalizableResource;

import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class ConstantMethod {

    private final ExecutableElement method;
    private final List<ConstantMethod> superMethods = new ArrayList<>();
    private AptContext context;

    public ConstantMethod(ExecutableElement method, AptContext context) {
        this.method = method;
        this.context = context;
    }

    public ExecutableElement getMethod() {
        return method;
    }

    public void addSuperMethod(ConstantMethod superMethod){
        superMethods.add(superMethod);
    }

    public String getKey() {
        LocalizableResource.Key keyAnnotation = method.getAnnotation(LocalizableResource.Key.class);
        if (nonNull(keyAnnotation)) {
            return keyAnnotation.value();
        }

        for (ConstantMethod constantMethod:superMethods) {
            keyAnnotation = constantMethod.getMethod().getAnnotation(LocalizableResource.Key.class);
            if (nonNull(keyAnnotation)) {
                return keyAnnotation.value();
            }
        }
        return method.getSimpleName().toString();
    }

    public String getPropertyValue(String locale, String key, Map<String, Properties> localeResourceMap) {
        String value = localeResourceMap.get(locale).getProperty(key);

        Constants.DefaultBooleanValue defaultBooleanValue = getDefaultValueAnnotation(Constants.DefaultBooleanValue.class);
        if (isNull(value) && nonNull(defaultBooleanValue)) {
            return String.valueOf(defaultBooleanValue.value());
        }

        Constants.DefaultDoubleValue defaultDoubleValue = getDefaultValueAnnotation(Constants.DefaultDoubleValue.class);
        if (isNull(value) && nonNull(defaultDoubleValue)) {
            return String.valueOf(defaultDoubleValue.value());
        }

        Constants.DefaultFloatValue defaultFloatValue = getDefaultValueAnnotation(Constants.DefaultFloatValue.class);
        if (isNull(value) && nonNull(defaultFloatValue)) {
            return String.valueOf(defaultFloatValue.value());
        }

        Constants.DefaultIntValue defaultIntValue = getDefaultValueAnnotation(Constants.DefaultIntValue.class);
        if (isNull(value) && nonNull(defaultIntValue)) {
            return String.valueOf(defaultIntValue.value());
        }

        Constants.DefaultStringValue defaultStringValue = getDefaultValueAnnotation(Constants.DefaultStringValue.class);
        if (isNull(value) && nonNull(defaultStringValue)) {
            return defaultStringValue.value();
        }

        return value;
    }

    public String[] getArrayPropertyValue(String locale, String key, Map<String, Properties> localeResourceMap) {
        String property = localeResourceMap.get(locale).getProperty(key);
        if (nonNull(property)) {
            if (property.isEmpty()) {
                return new String[]{};
            }
            return property.split("(?<![\\\\]),", -1);
        }

        Constants.DefaultStringArrayValue defaultStringArrayValue = getDefaultValueAnnotation(Constants.DefaultStringArrayValue.class);
        if (nonNull(defaultStringArrayValue)) {
            return defaultStringArrayValue.value();
        }

        return null;
    }

    private <T extends Annotation> T getDefaultValueAnnotation(Class<T> annotation) {
        T result = method.getAnnotation(annotation);
        if(isNull(result)){
            for (ConstantMethod constantMethod:superMethods) {
                result = constantMethod.getMethod().getAnnotation(annotation);
                if (nonNull(result)) {
                    return result;
                }
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        try {
            ConstantMethod other = (ConstantMethod) o;
            return method.getSimpleName().toString().equals(other.method.getSimpleName().toString())
                    && context.isSameType(method.getReturnType(), other.method.getReturnType());
        }catch (Exception e){
            context.messager.printMessage(Diagnostic.Kind.NOTE, this.toString() + " : " + o.toString());
            return false;
        }
    }

    @Override
    public String toString() {
        return "ConstantMethod{" +
                "method=" + method +
                ", context=" + context +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(method);
    }
}
