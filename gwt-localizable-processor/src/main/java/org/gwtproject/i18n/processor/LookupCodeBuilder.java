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

import com.squareup.javapoet.*;
import java.util.*;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

public class LookupCodeBuilder {
  private String name;
  private TypeName returnType;
  private TypeName wrapperType;
  private List<ExecutableElement> methods = new ArrayList<>();
  private boolean updateCache = true;

  public LookupCodeBuilder(String name, TypeName returnType, boolean updateCache) {
    this.name = name;
    this.returnType = returnType;
    this.updateCache = updateCache;
  }

  public LookupCodeBuilder(String name, TypeName returnType, TypeName wrapperType) {
    this.name = name;
    this.returnType = returnType;
    this.wrapperType = wrapperType;
  }

  public void addMethod(ExecutableElement method) {
    methods.add(method);
  }

  public Optional<MethodSpec.Builder> lookupMethod() {
    if (methods.isEmpty()) {
      return Optional.empty();
    }

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(name)
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(returnType)
            .addParameter(ParameterSpec.builder(TypeName.get(String.class), "key").build());

    methodBuilder.addStatement("$T target = ($T)cache.get(key)", inBodyType(), inBodyType());
    methodBuilder
        .beginControlFlow("if(target != null)")
        .addStatement("return target")
        .endControlFlow();

    methods.forEach(
        method -> {
          String key = method.getSimpleName().toString();
          methodBuilder
              .beginControlFlow("if(key.equals($S))", key)
              .addStatement("$T answer = $L()", inBodyType(), method.getSimpleName().toString());
          if (updateCache) {
            methodBuilder.addStatement("cache.put($S, $L)", key, "answer");
          }
          methodBuilder.addStatement("return $L", "answer").endControlFlow();
        });
    methodBuilder.addStatement(
        "throw new $T(\"Cannot find constant '\" +$L + \"'; expecting a method name\", \"org.gwtproject.i18n.client.ConstantsWithLookup\", $L)",
        MissingResourceException.class,
        "key",
        "key");

    return Optional.of(methodBuilder);
  }

  private TypeName inBodyType() {
    if (nonNull(wrapperType)) {
      return wrapperType;
    }
    return returnType;
  }
}
