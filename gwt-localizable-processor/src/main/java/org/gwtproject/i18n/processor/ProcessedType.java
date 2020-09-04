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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

public class ProcessedType {

    private final TypeElement enclosingType;
    private final List<ConstantMethod> methods = new ArrayList<>();
    private final Elements elements;

    public ProcessedType(Elements elements, TypeElement enclosingType) {
        this.elements = elements;
        this.enclosingType = enclosingType;
    }

    public void addMethod(ConstantMethod method) {
        if (!methods.contains(method)) {
            methods.add(method);
        }
    }

    public boolean overrides(ExecutableElement targetMethod) {
        for (ConstantMethod constantMethod : methods) {
            if (elements.overrides(constantMethod.getMethod(), targetMethod, enclosingType)) {
                return true;
            }
        }

        return false;
    }
}
