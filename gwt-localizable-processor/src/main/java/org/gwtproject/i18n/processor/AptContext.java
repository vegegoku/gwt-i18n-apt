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

import java.util.List;
import java.util.Map;
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

  /**
   * A wrapper over {@link Types#isAssignable(TypeMirror, TypeMirror)} which will apply type erasure
   * on the targetClass before calling the wrapped method..
   *
   * @param element a {@link javax.lang.model.element.Element} object.
   * @param targetClass a {@link java.lang.Class} object.
   * @return a boolean.
   */
  public boolean isAssignableFrom(Element element, Class<?> targetClass) {
    return isAssignableFrom(element.asType(), targetClass);
  }

  /**
   * A wrapper over {@link Types#isAssignable(TypeMirror, TypeMirror)} which will apply type erasure
   * on the targetClass before calling the wrapped method.
   *
   * @param typeMirror a {@link javax.lang.model.type.TypeMirror} object.
   * @param targetClass a {@link java.lang.Class} object.
   * @return a boolean.
   */
  public boolean isAssignableFrom(TypeMirror typeMirror, Class<?> targetClass) {
    return types.isAssignable(
        typeMirror,
        types.erasure(elements.getTypeElement(targetClass.getCanonicalName()).asType()));
  }

  /**
   * a wrapper for {@link Types#isSameType(javax.lang.model.type.TypeMirror,
   * javax.lang.model.type.TypeMirror)}
   *
   * @param typeMirror
   * @param targetClass
   * @return
   */
  public boolean isSameType(TypeMirror typeMirror, Class<?> targetClass) {
    return types.isSameType(
        typeMirror, elements.getTypeElement(targetClass.getCanonicalName()).asType());
  }

  /**
   * a wrapper for {@link Types#isSameType(javax.lang.model.type.TypeMirror,
   * javax.lang.model.type.TypeMirror)}
   *
   * @param typeMirror
   * @param otherTypeMirror
   * @return
   */
  public boolean isSameType(TypeMirror typeMirror, TypeMirror otherTypeMirror) {
    return types.isSameType(typeMirror, otherTypeMirror);
  }

  /**
   * @param typeMirror
   * @return true if the typeMirror is a primitive type
   */
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
    return TypeKind.ARRAY == typeMirror.getKind();
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

  /**
   * @param typeMirror
   * @return return true if the typeMirror is a String type
   */
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

  /**
   * @param typeMirror
   * @return return true if the typeMirror is a float/Float type
   */
  public boolean isFloat(TypeMirror typeMirror) {
    return "float".equals(typeMirror.toString()) || isSameType(typeMirror, Float.class);
  }

  /**
   * @param typeMirror
   * @return return true if the typeMirror is a int/Integer type
   */
  public boolean isInteger(TypeMirror typeMirror) {
    return "int".equals(typeMirror.toString()) || isSameType(typeMirror, Integer.class);
  }

  /**
   * @param typeMirror
   * @return return true if the typeMirror is a double/Double type
   */
  public boolean isDouble(TypeMirror typeMirror) {
    return "double".equals(typeMirror.toString()) || isSameType(typeMirror, Double.class);
  }

  /**
   * @param typeMirror
   * @return return true if the typeMirror is a boolean/Boolean type
   */
  public boolean isBoolean(TypeMirror typeMirror) {
    return "boolean".equals(typeMirror.toString()) || isSameType(typeMirror, Boolean.class);
  }

  /**
   * @param element
   * @return a list of all methods enclosed within an element
   */
  public static List<ExecutableElement> getElementMethods(Element element) {
    return element
        .getEnclosedElements()
        .stream()
        .filter(e -> e.getKind() == ElementKind.METHOD)
        .map(e -> (ExecutableElement) e)
        .collect(Collectors.toList());
  }
}
