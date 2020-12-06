/*
 * Copyright © 2018 The GWT Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gwtproject.i18n.processor;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.lang.annotation.Annotation;
import java.util.*;
import javax.lang.model.element.ExecutableElement;
import org.gwtproject.i18n.client.Constants;
import org.gwtproject.i18n.client.LocalizableResource;

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

  public void addSuperMethod(ConstantMethod superMethod) {
    superMethods.add(superMethod);
  }

  public String getKey() {
    LocalizableResource.Key keyAnnotation = method.getAnnotation(LocalizableResource.Key.class);
    if (nonNull(keyAnnotation)) {
      return keyAnnotation.value();
    }

    for (ConstantMethod constantMethod : superMethods) {
      keyAnnotation = constantMethod.getMethod().getAnnotation(LocalizableResource.Key.class);
      if (nonNull(keyAnnotation)) {
        return keyAnnotation.value();
      }
    }
    return method.getSimpleName().toString();
  }

  public String getPropertyValue(
      String locale, String key, Map<String, Properties> localeResourceMap) {
    String value = localeResourceMap.get(locale).getProperty(key);

    Constants.DefaultBooleanValue defaultBooleanValue =
        getDefaultValueAnnotation(Constants.DefaultBooleanValue.class);
    if (isNull(value) && nonNull(defaultBooleanValue)) {
      return String.valueOf(defaultBooleanValue.value());
    }

    Constants.DefaultDoubleValue defaultDoubleValue =
        getDefaultValueAnnotation(Constants.DefaultDoubleValue.class);
    if (isNull(value) && nonNull(defaultDoubleValue)) {
      return String.valueOf(defaultDoubleValue.value());
    }

    Constants.DefaultFloatValue defaultFloatValue =
        getDefaultValueAnnotation(Constants.DefaultFloatValue.class);
    if (isNull(value) && nonNull(defaultFloatValue)) {
      return String.valueOf(defaultFloatValue.value());
    }

    Constants.DefaultIntValue defaultIntValue =
        getDefaultValueAnnotation(Constants.DefaultIntValue.class);
    if (isNull(value) && nonNull(defaultIntValue)) {
      return String.valueOf(defaultIntValue.value());
    }

    Constants.DefaultStringValue defaultStringValue =
        getDefaultValueAnnotation(Constants.DefaultStringValue.class);
    if (isNull(value) && nonNull(defaultStringValue)) {
      return defaultStringValue.value();
    }

    return value;
  }

  public String[] getArrayPropertyValue(
      String locale, String key, Map<String, Properties> localeResourceMap) {
    String property = localeResourceMap.get(locale).getProperty(key);
    if (nonNull(property)) {
      if (property.isEmpty()) {
        return new String[] {};
      }
      return property.split("(?<![\\\\]),", -1);
    }

    Constants.DefaultStringArrayValue defaultStringArrayValue =
        getDefaultValueAnnotation(Constants.DefaultStringArrayValue.class);
    if (nonNull(defaultStringArrayValue)) {
      return defaultStringArrayValue.value();
    }

    return null;
  }

  private <T extends Annotation> T getDefaultValueAnnotation(Class<T> annotation) {
    T result = method.getAnnotation(annotation);
    if (isNull(result)) {
      for (ConstantMethod constantMethod : superMethods) {
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
    ConstantMethod other = (ConstantMethod) o;
    return method.getSimpleName().toString().equals(other.method.getSimpleName().toString())
        && context.isSameType(method.getReturnType(), other.method.getReturnType());
  }

  @Override
  public String toString() {
    return "ConstantMethod{" + "method=" + method + ", context=" + context + '}';
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = 31 * result + method.getSimpleName().toString().hashCode();
    result = 31 * result + method.getReturnType().toString().hashCode();
    return result;
  }
}
