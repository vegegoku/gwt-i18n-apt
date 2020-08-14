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
import static java.util.stream.Collectors.toList;
import static org.gwtproject.i18n.processor.AptContext.getKey;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.common.collect.SetMultimap;
import com.squareup.javapoet.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import org.gwtproject.i18n.client.Constants;
import org.gwtproject.i18n.client.ConstantsWithLookup;
import org.gwtproject.i18n.shared.Localizable;
import org.gwtproject.i18n.shared.cldr.LocaleInfoImpl;
import org.gwtproject.i18n.shared.cldr.impl.LocaleInfo_factory;

public class LocalizableProcessingStep implements BasicAnnotationProcessor.ProcessingStep {
  private final ProcessingEnvironment processingEnv;

  public LocalizableProcessingStep(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
  }

  @Override
  public Set<? extends Class<? extends Annotation>> annotations() {
    return Collections.singleton(Localizable.IsLocalizable.class);
  }

  @Override
  public Set<Element> process(
      SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {

    AptContext context = new AptContext(processingEnv);

    for (Element element : elementsByAnnotation.get(Localizable.IsLocalizable.class)) {
      try {
        String packageName = context.elements.getPackageOf(element).getQualifiedName().toString();

        List<String> locales = getLocaleNames(element);
        Map<String, ClassName> keyToNameMapping = new HashMap<>();

        Map<String, Properties> localeResourceMap = new HashMap<>();

        locales.forEach(
            locale -> {
              keyToNameMapping.put(
                  locale,
                  ClassName.get(
                      packageName,
                      element.getSimpleName().toString() + "_" + (localePostfix(locale))));

              Properties resource = getPropertiesResource(element, localePostfix(locale));
              fillUpProperties(resource, context, element, localePostfix(locale));
              if (nonNull(resource)) {
                localeResourceMap.put(locale, resource);
              }
              if (!"default".equals(locale)) {

                LocaleInfoImpl localeInfo = getLocaleInfo(locale);
                String[] sortedRegionCodes = localeInfo.getLocalizedNames().getSortedRegionCodes();

                Arrays.asList(sortedRegionCodes)
                    .forEach(
                        regionCode -> {
                          String localeAndRegion =
                              "default".equals(locale) ? "" : (locale + "_" + regionCode);
                          try {
                            Properties propertiesResource =
                                getPropertiesResource(element, localeAndRegion);
                            fillUpProperties(propertiesResource, context, element, localeAndRegion);
                            if (!propertiesResource.isEmpty()) {
                              localeResourceMap.put(localeAndRegion, propertiesResource);
                            }

                          } catch (Exception e) {
                            context.messager.printMessage(
                                Diagnostic.Kind.ERROR, "failed to find resource");
                          }
                        });
              }
            });

        if (context.isAssignableFrom(element, Constants.class)
            || context.isAssignableFrom(element, ConstantsWithLookup.class)) {
          generateConstantsClasses(element, context, localeResourceMap);
          generateFactoryClass(element, context, localeResourceMap);
        }

      } catch (Exception e) {
        ExceptionUtil.messageStackTrace(context.messager, e, element);
      }
    }

    return Collections.emptySet();
  }

  private void generateFactoryClass(
      Element element, AptContext context, Map<String, Properties> localeResourceMap) {
    TypeSpec.Builder builder =
        TypeSpec.classBuilder(element.getSimpleName().toString() + "_Factory")
            .addModifiers(Modifier.PUBLIC)
            .addMethod(
                MethodSpec.methodBuilder("create")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(TypeName.get(element.asType()))
                    .addStatement("return create(System.getProperty($S))", "locale")
                    .build());

    MethodSpec.Builder createMethodBuilder =
        MethodSpec.methodBuilder("create")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(TypeName.get(element.asType()))
            .addParameter(ParameterSpec.builder(TypeName.get(String.class), "locale").build());

    localeResourceMap
        .keySet()
        .stream()
        .sorted(Comparator.reverseOrder())
        .forEach(
            locale -> {
              createMethodBuilder
                  .beginControlFlow("if($S.equals(locale))", locale)
                  .addStatement(
                      "return new $T()",
                      ClassName.bestGuess(
                          context.elements.getPackageOf(element).getQualifiedName().toString()
                              + "."
                              + element.getSimpleName().toString()
                              + "_"
                              + (localePostfix(locale))))
                  .endControlFlow();
            });

    createMethodBuilder.addStatement("return null");
    builder.addMethod(createMethodBuilder.build());

    writeSource(builder, context, element);
  }

  public Properties fillUpProperties(
      Properties properties, AptContext context, Element element, String localeAndRegion) {
    TypeElement typeElement = (TypeElement) element;
    List<? extends TypeMirror> interfaces = typeElement.getInterfaces();

    if (!interfaces.isEmpty()) {
      interfaces
          .stream()
          .map(context.types::asElement)
          .forEach(
              extendedInterface -> {
                Properties propertiesResource =
                    getPropertiesResource(extendedInterface, localeAndRegion);
                if (nonNull(propertiesResource)) {
                  propertiesResource.forEach(properties::putIfAbsent);
                }

                fillUpProperties(properties, context, extendedInterface, localeAndRegion);
              });
    }

    return properties;
  }

  private void generateConstantsClasses(
      Element element, AptContext context, Map<String, Properties> localeResourceMap) {

    localeResourceMap.forEach(
        (locale, resource) -> {
          TypeSpec.Builder constantImplBuilder =
              TypeSpec.classBuilder(
                      element.getSimpleName().toString() + "_" + localePostfix(locale))
                  .addModifiers(Modifier.PUBLIC)
                  .addSuperinterface(ClassName.get(element.asType()));

          final boolean[] cacheAdded = new boolean[] {false};

          LookupMethodsBuilder lookupMethodsBuilder = new LookupMethodsBuilder(context);

          getConstantsMethods(new ArrayList<>(), context, element)
              .forEach(
                  method -> {
                    if (context.isAssignableFrom(element, ConstantsWithLookup.class)) {
                      lookupMethodsBuilder.addMethod(method);
                    }

                    if (context.isMap(method.getReturnType())) {
                      addCacheField(constantImplBuilder, cacheAdded);
                      Optional<CodeBlock> methodBody =
                          getMapMethodBody(context, method, locale, localeResourceMap);
                      methodBody.ifPresent(
                          codeBlock -> {
                            constantImplBuilder.addMethod(
                                MethodSpec.methodBuilder(method.getSimpleName().toString())
                                    .addModifiers(Modifier.PUBLIC)
                                    .addAnnotation(Override.class)
                                    .returns(TypeName.get(method.getReturnType()))
                                    .addCode(codeBlock)
                                    .build());
                          });

                    } else if (context.isArray(method.getReturnType())
                        && context.isString(context.arrayComponentType(method.getReturnType()))) {
                      addCacheField(constantImplBuilder, cacheAdded);
                      Optional<CodeBlock> methodBody =
                          getArrayMethodBody(context, method, locale, localeResourceMap);
                      methodBody.ifPresent(
                          codeBlock -> {
                            constantImplBuilder.addMethod(
                                MethodSpec.methodBuilder(method.getSimpleName().toString())
                                    .addModifiers(Modifier.PUBLIC)
                                    .addAnnotation(Override.class)
                                    .returns(TypeName.get(method.getReturnType()))
                                    .addCode(codeBlock)
                                    .build());
                          });

                    } else {
                      constantImplBuilder.addMethod(
                          MethodSpec.methodBuilder(method.getSimpleName().toString())
                              .addModifiers(Modifier.PUBLIC)
                              .addAnnotation(Override.class)
                              .returns(TypeName.get(method.getReturnType()))
                              .addCode(
                                  getValueExpression(context, method, locale, localeResourceMap))
                              .build());
                    }
                  });

          if (context.isAssignableFrom(element, ConstantsWithLookup.class)) {
            lookupMethodsBuilder
                .lookupMethods()
                .forEach(methodBuilder -> constantImplBuilder.addMethod(methodBuilder.build()));
          }

          writeSource(constantImplBuilder, context, element);
        });
  }

  private String localePostfix(String locale) {
    return "default".equals(locale) ? "" : locale;
  }

  private void addCacheField(TypeSpec.Builder constantImplBuilder, boolean[] cacheAdded) {
    if (!cacheAdded[0]) {
      constantImplBuilder.addField(
          FieldSpec.builder(ClassName.get(Map.class), "cache")
              .initializer("new $T()", HashMap.class)
              .build());
      cacheAdded[0] = true;
    }
  }

  private Optional<CodeBlock> getMapMethodBody(
      AptContext context,
      ExecutableElement method,
      String locale,
      Map<String, Properties> localeResourceMap) {
    CodeBlock.Builder builder = CodeBlock.builder();

    String key = getKey(method);
    String property = localeResourceMap.get(locale).getProperty(key);
    if (nonNull(property)) {
      builder.addStatement(
          "$T args = ($T)cache.get($S)",
          ParameterizedTypeName.get(Map.class, String.class, String.class),
          ParameterizedTypeName.get(Map.class, String.class, String.class),
          key);
      builder
          .beginControlFlow("if(args == null)")
          .addStatement(
              "args = new $T()",
              ParameterizedTypeName.get(LinkedHashMap.class, String.class, String.class));

      if (!property.isEmpty()) {
        Arrays.asList(property.split("(?<![\\\\]),", -1))
            .stream()
            .map(String::trim)
            .collect(Collectors.toSet())
            .forEach(
                valueKey -> {
                  String valueProperty = localeResourceMap.get(locale).getProperty(valueKey);
                  builder.addStatement("args.put($S, $S)", valueKey, valueProperty);
                });
      }

      builder.addStatement("args = $T.unmodifiableMap(args)", Collections.class);
      builder.addStatement("cache.put($S, args)", key);
      builder.endControlFlow();
      builder.addStatement("return args");

      return Optional.of(builder.build());
    } else {
      return Optional.empty();
    }
  }

  private Optional<CodeBlock> getArrayMethodBody(
      AptContext context,
      ExecutableElement method,
      String locale,
      Map<String, Properties> localeResourceMap) {
    CodeBlock.Builder builder = CodeBlock.builder();

    String key = getKey(method);
    String property = localeResourceMap.get(locale).getProperty(key);
    if (nonNull(property)) {
      builder.addStatement("$T[] args = ($T[])cache.get($S)", String.class, String.class, key);
      builder.beginControlFlow("if(args == null)").beginControlFlow("$T[] writer=", String.class);

      if (!property.isEmpty()) {
        Arrays.asList(property.split("(?<![\\\\]),", -1))
            .stream()
            .forEach(
                arrayValue -> {
                  builder.add("$S,\n", arrayValue.trim().replace("\\,", ","));
                });
      } else {
        builder.add("$S,\n", "");
      }
      builder.endControlFlow("");

      builder.addStatement("cache.put($S, writer)", key);
      builder.addStatement("return writer");
      builder.endControlFlow();
      builder.addStatement("return args");

      return Optional.of(builder.build());
    } else {
      return Optional.empty();
    }
  }

  private CodeBlock getValueExpression(
      AptContext context,
      ExecutableElement method,
      String locale,
      Map<String, Properties> localeResourceMap) {

    TypeMirror returnType = method.getReturnType();
    String key = getKey(method);
    String property = localeResourceMap.get(locale).getProperty(key);

    if (isFloat(context, returnType)) {
      return CodeBlock.builder().addStatement("return $Lf", property).build();
    }

    if (context.isString(returnType)) {
      return CodeBlock.builder().addStatement("return $S", property).build();
    }
    return CodeBlock.builder().addStatement("return $L", property).build();
  }

  private boolean isFloat(AptContext context, TypeMirror returnType) {
    return (context.isPrimitive(returnType) && "float".equals(returnType.toString()))
        || context.isAssignableFrom(returnType, Float.class);
  }

  private Properties getPropertiesResource(Element element, String localeAndRegion) {
    try {

      ResourceOracleImpl resourceOracle = new ResourceOracleImpl(new AptContext(processingEnv));
      URL resource =
          resourceOracle.findResource(
              processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString(),
              element.getSimpleName().toString()
                  + ((localeAndRegion.isEmpty()) ? "" : "_")
                  + localeAndRegion
                  + ".properties");
      Properties properties = new Properties();
      if (nonNull(resource)) {
        properties.load(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8));
      }

      return properties;
    } catch (Exception e) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "failed to find resource");
      return null;
    }
  }

  private void writeSource(TypeSpec.Builder builder, AptContext context, Element element) {
    writeSource(
        Arrays.asList(builder),
        context,
        context.elements.getPackageOf(element).getQualifiedName().toString());
  }

  protected void writeSource(JavaFile sourceFile, AptContext context) {
    try {
      sourceFile.writeTo(context.filer);
    } catch (IOException e) {
      ExceptionUtil.messageStackTrace(context.messager, e);
    }
  }

  protected void writeSource(
      List<TypeSpec.Builder> builders, AptContext context, String rootPackage) {
    builders.forEach(
        builder -> {
          JavaFile javaFile = JavaFile.builder(rootPackage, builder.build()).build();
          writeSource(javaFile, context);
        });
  }

  private LocaleInfoImpl getLocaleInfo(String locale) {
    return LocaleInfo_factory.create(locale);
  }

  private List<String> getLocaleNames(Element element) {
    // breadth-first search to find the annotation since you can have diamond inheritance in
    // interfaces
    List<Element> typesToCheck = new ArrayList<>();
    typesToCheck.add(element);
    for (int i = 0; i < typesToCheck.size(); i++) {
      Element check = typesToCheck.get(i);
      Localizable.I18nLocaleSuffuxes annotation =
          check.getAnnotation(Localizable.I18nLocaleSuffuxes.class);
      if (annotation != null) {
        return Arrays.asList(annotation.value());
      }
      TypeElement type = (TypeElement) check;
      typesToCheck.addAll(
          type.getInterfaces()
              .stream()
              .map(m -> processingEnv.getTypeUtils().asElement(m))
              .collect(Collectors.toList()));
    }
    return Collections.emptyList();
  }

  public List<ExecutableElement> getConstantsMethods(
      List<ProcessedType> processedTypes, AptContext context, Element constantsInterfaceElement) {
    TypeElement elementType = (TypeElement) constantsInterfaceElement;

    if (elementType.getInterfaces().isEmpty()) {
      return getMethods(processedTypes, context, constantsInterfaceElement);
    }

    List<ExecutableElement> methods = new ArrayList<>();
    methods.addAll(getMethods(processedTypes, context, constantsInterfaceElement));
    ((TypeElement) constantsInterfaceElement)
        .getInterfaces()
        .forEach(
            superInterface ->
                methods.addAll(
                    getConstantsMethods(
                        processedTypes, context, context.types.asElement(superInterface))));

    return methods;
  }

  private List<ExecutableElement> getMethods(
      List<ProcessedType> processedTypes, AptContext context, Element constantsInterfaceElement) {
    ProcessedType processedType =
        new ProcessedType(context.elements, (TypeElement) constantsInterfaceElement);
    processedTypes.add(processedType);
    List<ExecutableElement> result =
        AptContext.getElementMethods(constantsInterfaceElement)
            .stream()
            .filter(executableElement -> notOverridden(executableElement, processedTypes))
            .filter(executableElement -> executableElement.getParameters().isEmpty())
            .collect(toList());

    result.forEach(processedType::addMethod);
    return result;
  }

  private boolean notOverridden(ExecutableElement method, List<ProcessedType> processedTypes) {
    for (ProcessedType processedType : processedTypes) {
      if (processedType.overrides(method)) {
        return false;
      }
    }
    return true;
  }
}
