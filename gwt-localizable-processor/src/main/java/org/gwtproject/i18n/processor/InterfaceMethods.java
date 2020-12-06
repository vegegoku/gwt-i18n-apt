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

import java.util.ArrayList;
import java.util.List;

public class InterfaceMethods {

  private final List<ConstantMethod> methods = new ArrayList<>();

  public List<ConstantMethod> getMethods() {
    return methods;
  }

  public void addAll(List<ConstantMethod> methods) {
    methods.forEach(this::add);
  }

  public void addAll(InterfaceMethods interfaceMethods) {
    addAll(interfaceMethods.methods);
  }

  public void add(ConstantMethod constantMethod) {
    if (!methods.contains(constantMethod)) {
      methods.add(constantMethod);
    } else {
      methods.get(methods.indexOf(constantMethod)).addSuperMethod(constantMethod);
    }
  }
}
