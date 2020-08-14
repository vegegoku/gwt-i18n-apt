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

import static java.util.Objects.nonNull;

import com.squareup.javapoet.TypeName;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.gwtproject.i18n.client.LocalizableResource;

/** @author Dmitrii Tikhomirov <chani@me.com> Created by treblereel on 10/26/18. */
public class AptContext {

  public final Messager messager;
  public final Filer filer;
  public final Elements elements;
  public final Types types;
  public final ProcessingEnvironment processingEnv;

  public AptContext(final ProcessingEnvironment processingEnv) {
    this.filer = processingEnv.getFiler();
    this.messager = processingEnv.getMessager();
    this.elements = processingEnv.getElementUtils();
    this.types = processingEnv.getTypeUtils();
    this.processingEnv = processingEnv;
  }

  public boolean isAssignableFrom(Element element, Class<?> targetClass) {
    return types.isAssignable(
        element.asType(),
        types.getDeclaredType(elements.getTypeElement(targetClass.getCanonicalName())));
  }

  /**
   * isAssignableFrom.
   *
   * @param typeMirror a {@link javax.lang.model.type.TypeMirror} object.
   * @param targetClass a {@link java.lang.Class} object.
   * @return a boolean.
   */
  public boolean isAssignableFrom(TypeMirror typeMirror, Class<?> targetClass) {
    return types.isAssignable(
        typeMirror, types.getDeclaredType(elements.getTypeElement(targetClass.getCanonicalName())));
  }

  public boolean isSameType(TypeMirror typeMirror, Class<?> targetClass) {
    return types.isSameType(
        typeMirror, types.getDeclaredType(elements.getTypeElement(targetClass.getCanonicalName())));
  }

  public static boolean isPrimitive(TypeMirror typeMirror) {
    return typeMirror.getKind().isPrimitive();
  }

  /**
   * isArray.
   *
   * @param typeMirror a {@link javax.lang.model.type.TypeMirror} object.
   * @return a boolean.
   */
  public boolean isArray(TypeMirror typeMirror) {
    return TypeKind.ARRAY.compareTo(typeMirror.getKind()) == 0;
  }

  /**
   * arrayComponentType.
   *
   * @param typeMirror a {@link javax.lang.model.type.TypeMirror} object.
   * @return a {@link javax.lang.model.type.TypeMirror} object.
   */
  public TypeMirror arrayComponentType(TypeMirror typeMirror) {
    return ((ArrayType) typeMirror).getComponentType();
  }

  public boolean isString(TypeMirror typeMirror) {
    return isSameType(typeMirror, String.class);
  }

  /**
   * isMap.
   *
   * @param typeMirror a {@link javax.lang.model.type.TypeMirror} object.
   * @return a boolean.
   */
  public boolean isMap(TypeMirror typeMirror) {
    return !isPrimitive(typeMirror) && isAssignableFrom(typeMirror, Map.class);
  }

  public boolean isFloat(TypeMirror returnType) {
    return (isPrimitive(returnType) && "float".equals(returnType.toString()))
        || isAssignableFrom(returnType, Float.class);
  }

  public boolean isInteger(TypeMirror returnType) {
    return (isPrimitive(returnType) && "int".equals(returnType.toString()))
        || isAssignableFrom(returnType, Integer.class);
  }

  public boolean isDouble(TypeMirror returnType) {
    return (isPrimitive(returnType) && "double".equals(returnType.toString()))
        || isAssignableFrom(returnType, Double.class);
  }

  public boolean isBoolean(TypeMirror returnType) {
    return (isPrimitive(returnType) && "boolean".equals(returnType.toString()))
        || isAssignableFrom(returnType, Boolean.class);
  }

  public static String getKey(ExecutableElement method) {
    LocalizableResource.Key keyAnnotation = method.getAnnotation(LocalizableResource.Key.class);
    if (nonNull(keyAnnotation)) {
      return keyAnnotation.value();
    }
    return method.getSimpleName().toString();
  }

  public static List<ExecutableElement> getElementMethods(Element element) {
    return element
        .getEnclosedElements()
        .stream()
        .filter(e -> e.getKind() == ElementKind.METHOD)
        .map(e -> (ExecutableElement) e)
        .collect(Collectors.toList());
  }

  /**
   * wrapperType.
   *
   * @param type a {@link TypeMirror} object.
   * @return a {@link TypeName} object.
   */
  public static TypeName wrapperType(TypeMirror type) {
    if (isPrimitive(type)) {
      if ("boolean".equals(type.toString())) {
        return TypeName.get(Boolean.class);
      } else if ("byte".equals(type.toString())) {
        return TypeName.get(Byte.class);
      } else if ("short".equals(type.toString())) {
        return TypeName.get(Short.class);
      } else if ("int".equals(type.toString())) {
        return TypeName.get(Integer.class);
      } else if ("long".equals(type.toString())) {
        return TypeName.get(Long.class);
      } else if ("char".equals(type.toString())) {
        return TypeName.get(Character.class);
      } else if ("float".equals(type.toString())) {
        return TypeName.get(Float.class);
      } else if ("double".equals(type.toString())) {
        return TypeName.get(Double.class);
      } else {
        return TypeName.get(Void.class);
      }
    } else {
      return TypeName.get(type);
    }
  }
}
