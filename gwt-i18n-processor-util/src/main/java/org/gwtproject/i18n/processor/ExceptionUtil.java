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

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil {

  /**
   * @param messager the messager to print the exception stack trace
   * @param e exception to be printed
   */
  public static void messageStackTrace(Messager messager, Exception e) {
    messager.printMessage(
        Diagnostic.Kind.ERROR, "error while generating source file " + stackTraceString(e));
  }

  /**
   * @param messager the messager to print the exception stack trace
   * @param e exception to be printed
   */
  public static void messageStackTrace(Messager messager, Exception e, Element element) {
    messager.printMessage(
        Diagnostic.Kind.ERROR, "error while generating source file for element" + stackTraceString(e), element);
  }

  /**
   * @param messager the messager to print the exception stack trace
   * @param e exception to be printed
   */
  public static void messageStackTrace(Messager messager, Exception e, Element element, AnnotationMirror annotationMirror) {
    messager.printMessage(
        Diagnostic.Kind.ERROR, "error while generating source file for element and annotation" + stackTraceString(e), element, annotationMirror);
  }

  private static String stackTraceString(Exception e) {
    StringWriter out = new StringWriter();
    e.printStackTrace(new PrintWriter(out));
    return out.getBuffer().toString();
  }

}
