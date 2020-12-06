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

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;

public class LookupMethodsBuilder {

  private final LookupCodeBuilder mapLookupBuilder =
      new LookupCodeBuilder(
          "getMap", ParameterizedTypeName.get(Map.class, String.class, String.class), false);
  private final LookupCodeBuilder arrayLookupBuilder =
      new LookupCodeBuilder("getStringArray", ArrayTypeName.of(TypeName.get(String.class)), false);
  private final LookupCodeBuilder booleanLookupBuilder =
      new LookupCodeBuilder("getBoolean", TypeName.get(boolean.class), TypeName.get(Boolean.class));
  private final LookupCodeBuilder integerLookupBuilder =
      new LookupCodeBuilder("getInt", TypeName.get(int.class), TypeName.get(Integer.class));
  private final LookupCodeBuilder doubleLookupBuilder =
      new LookupCodeBuilder("getDouble", TypeName.get(double.class), TypeName.get(Double.class));
  private final LookupCodeBuilder floatLookupBuilder =
      new LookupCodeBuilder("getFloat", TypeName.get(float.class), TypeName.get(Float.class));
  private final LookupCodeBuilder stringLookupBuilder =
      new LookupCodeBuilder("getString", TypeName.get(String.class), true);

  private final AptContext context;

  public LookupMethodsBuilder(AptContext context) {
    this.context = context;
  }

  public void addMethod(ExecutableElement method) {
    if (context.isMap(method.getReturnType())) {
      mapLookupBuilder.addMethod(method);
    }

    if (context.isArray(method.getReturnType())) {
      arrayLookupBuilder.addMethod(method);
    }

    if (context.isBoolean(method.getReturnType())) {
      booleanLookupBuilder.addMethod(method);
    }

    if (context.isInteger(method.getReturnType())) {
      integerLookupBuilder.addMethod(method);
    }

    if (context.isDouble(method.getReturnType())) {
      doubleLookupBuilder.addMethod(method);
    }

    if (context.isFloat(method.getReturnType())) {
      floatLookupBuilder.addMethod(method);
    }

    if (context.isString(method.getReturnType())) {
      stringLookupBuilder.addMethod(method);
    }
  }

  public List<MethodSpec.Builder> lookupMethods() {
    List<MethodSpec.Builder> lookupMethods = new ArrayList<>();
    mapLookupBuilder.lookupMethod().ifPresent(lookupMethods::add);
    arrayLookupBuilder.lookupMethod().ifPresent(lookupMethods::add);
    booleanLookupBuilder.lookupMethod().ifPresent(lookupMethods::add);
    integerLookupBuilder.lookupMethod().ifPresent(lookupMethods::add);
    doubleLookupBuilder.lookupMethod().ifPresent(lookupMethods::add);
    floatLookupBuilder.lookupMethod().ifPresent(lookupMethods::add);
    stringLookupBuilder.lookupMethod().ifPresent(lookupMethods::add);

    return lookupMethods;
  }
}
