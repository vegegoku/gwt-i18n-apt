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

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.common.collect.SetMultimap;
import com.squareup.javapoet.*;
import org.gwtproject.i18n.client.Constants;
import org.gwtproject.i18n.client.ConstantsWithLookup;
import org.gwtproject.i18n.shared.Localizable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

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
                                    ClassName.get(packageName, element.getSimpleName().toString() + "_" + (localePostfix(locale))));

                            Properties resource = getPropertiesResource(element, localePostfix(locale));
                            loadPropertiesByLocale(resource, context, element, localePostfix(locale));
                            if (nonNull(resource)) {
                                localeResourceMap.put(locale, resource);
                            }
                            if (!"default".equals(locale)) {

                                try {
                                    Properties propertiesResource =
                                            getPropertiesResource(element, locale);
                                    loadPropertiesByLocale(propertiesResource, context, element, locale);
                                    if (!propertiesResource.isEmpty()) {
                                        localeResourceMap.put(locale, propertiesResource);
                                    }

                                } catch (Exception e) {
                                    context.messager.printMessage(
                                            Diagnostic.Kind.ERROR, "failed to find resource");
                                }
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
        String enclosingElementName = !(ElementKind.PACKAGE == element.getEnclosingElement().getKind())
                ? element.getEnclosingElement().getSimpleName().toString() + "_" : "";
        String factoryClassName = enclosingElementName + element.getSimpleName().toString() + "_Factory";

        TypeSpec.Builder builder =
                TypeSpec.classBuilder(factoryClassName)
                        .addModifiers(Modifier.PUBLIC);

        MethodSpec.Builder createMethodBuilder =
                MethodSpec.methodBuilder("create")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(TypeName.get(element.asType()));

        localeResourceMap
                .keySet()
                .stream()
                .sorted(Comparator.reverseOrder())
                .forEach(
                        locale -> {
                            createMethodBuilder
                                    .beginControlFlow("if($S.equals(System.getProperty($S)))", locale, "locale")
                                    .addStatement(
                                            "return new $T()",
                                            ClassName.bestGuess(
                                                    context.elements.getPackageOf(element).getQualifiedName().toString()
                                                            + "."
                                                            + getConstantClassName(element, localePostfix(locale))
                                            )
                                    )
                                    .endControlFlow();
                        });

        createMethodBuilder.addStatement("return null");
        builder.addMethod(createMethodBuilder.build());

        writeSource(builder, context, element);
    }

    public Properties loadPropertiesByLocale(
            Properties properties, AptContext context, Element element, String locale) {
        TypeElement typeElement = (TypeElement) element;
        List<? extends TypeMirror> interfaces = typeElement.getInterfaces();

        if (!interfaces.isEmpty()) {
            interfaces
                    .stream()
                    .map(context.types::asElement)
                    .forEach(
                            extendedInterface -> {
                                Properties propertiesResource =
                                        getPropertiesResource(extendedInterface, locale);
                                if (nonNull(propertiesResource)) {
                                    propertiesResource.forEach(properties::putIfAbsent);
                                }

                                loadPropertiesByLocale(properties, context, extendedInterface, locale);
                            });
        }

        return properties;
    }

    private void generateConstantsClasses(
            Element element, AptContext context, Map<String, Properties> localeResourceMap) {

        localeResourceMap.forEach(
                (locale, resource) -> {
                    TypeSpec.Builder constantImplBuilder =
                            TypeSpec.classBuilder(getConstantClassName(element, locale))
                                    .addModifiers(Modifier.PUBLIC)
                                    .addSuperinterface(ClassName.get(element.asType()));

                    final boolean[] cacheAdded = new boolean[]{false};

                    LookupMethodsBuilder lookupMethodsBuilder = new LookupMethodsBuilder(context);
                    InterfaceMethods interfaceMethods = new InterfaceMethods();
                    getConstantsMethods(interfaceMethods, context, element)
                            .getMethods()
                            .stream()
                            .forEach(
                                    constantMethod -> {
                                        if (context.isAssignableFrom(element, ConstantsWithLookup.class)) {
                                            lookupMethodsBuilder.addMethod(constantMethod.getMethod());
                                        }

                                        if (context.isMap(constantMethod.getMethod().getReturnType())) {
                                            addCacheField(constantImplBuilder, cacheAdded);
                                            Optional<CodeBlock> methodBody =
                                                    getMapMethodBody(constantMethod, locale, localeResourceMap);
                                            methodBody.ifPresent(
                                                    codeBlock -> {
                                                        constantImplBuilder.addMethod(
                                                                MethodSpec.methodBuilder(constantMethod.getMethod().getSimpleName().toString())
                                                                        .addModifiers(Modifier.PUBLIC)
                                                                        .addAnnotation(Override.class)
                                                                        .returns(TypeName.get(constantMethod.getMethod().getReturnType()))
                                                                        .addCode(codeBlock)
                                                                        .build());
                                                    });

                                        } else if (context.isArray(constantMethod.getMethod().getReturnType())
                                                && context.isString(context.arrayComponentType(constantMethod.getMethod().getReturnType()))) {
                                            addCacheField(constantImplBuilder, cacheAdded);
                                            Optional<CodeBlock> methodBody =
                                                    getArrayMethodBody(constantMethod, locale, localeResourceMap);
                                            methodBody.ifPresent(
                                                    codeBlock -> {
                                                        constantImplBuilder.addMethod(
                                                                MethodSpec.methodBuilder(constantMethod.getMethod().getSimpleName().toString())
                                                                        .addModifiers(Modifier.PUBLIC)
                                                                        .addAnnotation(Override.class)
                                                                        .returns(TypeName.get(constantMethod.getMethod().getReturnType()))
                                                                        .addCode(codeBlock)
                                                                        .build());
                                                    });

                                        } else {
                                            constantImplBuilder.addMethod(
                                                    MethodSpec.methodBuilder(constantMethod.getMethod().getSimpleName().toString())
                                                            .addModifiers(Modifier.PUBLIC)
                                                            .addAnnotation(Override.class)
                                                            .returns(TypeName.get(constantMethod.getMethod().getReturnType()))
                                                            .addCode(
                                                                    getValueExpression(context, constantMethod, locale, localeResourceMap))
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

    private String getConstantClassName(Element element, String locale) {
        String enclosingElementName = !(ElementKind.PACKAGE == element.getEnclosingElement().getKind())
                ? element.getEnclosingElement().getSimpleName().toString() + "_" : "";
        return enclosingElementName + element.getSimpleName().toString() + "_" + localePostfix(locale);
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
            ConstantMethod method,
            String locale,
            Map<String, Properties> localeResourceMap) {
        CodeBlock.Builder builder = CodeBlock.builder();

        String key = method.getKey();
        String[] property = method.getArrayPropertyValue(locale, key, localeResourceMap);
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

            if (property.length > 0) {
                Arrays.asList(property)
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
            ConstantMethod method,
            String locale,
            Map<String, Properties> localeResourceMap) {
        CodeBlock.Builder builder = CodeBlock.builder();

        String key = method.getKey();
        String[] property = method.getArrayPropertyValue(locale, key, localeResourceMap);
        if (nonNull(property)) {
            builder.addStatement("$T[] args = ($T[])cache.get($S)", String.class, String.class, key);
            builder.beginControlFlow("if(args == null)").beginControlFlow("$T[] writer=", String.class);

            if (property.length > 0) {
                Arrays.asList(property)
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
            ConstantMethod method,
            String locale,
            Map<String, Properties> localeResourceMap) {

        TypeMirror returnType = method.getMethod().getReturnType();
        String key = method.getKey();
        String property = method.getPropertyValue(locale, key, localeResourceMap);

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

    private Properties getPropertiesResource(Element element, String locale) {
        try {

            ResourceOracleImpl resourceOracle = new ResourceOracleImpl(new AptContext(processingEnv));
            URL resource =
                    resourceOracle.findResource(
                            processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString(),
                            element.getSimpleName().toString()
                                    + ((locale.isEmpty()) ? "" : "_")
                                    + locale
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
                context.elements.getPackageOf(element).getQualifiedName().toString(),
                element);
    }

    protected void writeSource(JavaFile sourceFile, AptContext context, Element element) {
        try {
            sourceFile.writeTo(context.filer);
        } catch (IOException e) {
            ExceptionUtil.messageStackTrace(context.messager, e, element);
        }
    }

    protected void writeSource(
            List<TypeSpec.Builder> builders, AptContext context, String rootPackage, Element element) {
        builders.forEach(
                builder -> {
                    JavaFile javaFile = JavaFile.builder(rootPackage, builder.build()).build();
                    writeSource(javaFile, context, element);
                });
    }

    private List<String> getLocaleNames(Element element) {
        // breadth-first search to find the annotation since you can have diamond inheritance in
        // interfaces
        List<Element> typesToCheck = new ArrayList<>();
        typesToCheck.add(element);
        for (int i = 0; i < typesToCheck.size(); i++) {
            Element check = typesToCheck.get(i);
            Localizable.I18nLocaleSuffixes annotation =
                    check.getAnnotation(Localizable.I18nLocaleSuffixes.class);
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

    public InterfaceMethods getConstantsMethods(InterfaceMethods interfaceMethods, AptContext context, Element constantsInterfaceElement) {
        TypeElement elementType = (TypeElement) constantsInterfaceElement;

        if (elementType.getInterfaces().isEmpty()) {
            getMethods(interfaceMethods, context, constantsInterfaceElement);
            return interfaceMethods;
        }

        getMethods(interfaceMethods, context, constantsInterfaceElement);
        ((TypeElement) constantsInterfaceElement)
                .getInterfaces()
                .forEach(superInterface -> {
                            getConstantsMethods(interfaceMethods, context, context.types.asElement(superInterface))
                                    .getMethods()
                                    .forEach(constantMethod -> {
                                        if (nonNull(constantMethod) && !interfaceMethods.getMethods().contains(constantMethod)) {
                                            interfaceMethods.add(constantMethod);
                                        }
                                    });
                        }
                );

        return interfaceMethods;
    }

    private void getMethods(InterfaceMethods interfaceMethods, AptContext context, Element constantsInterfaceElement) {
        List<ConstantMethod> result =
                AptContext.getElementMethods(constantsInterfaceElement)
                        .stream()
                        .filter(executableElement -> executableElement.getParameters().isEmpty())
                        .map(executableElement -> new ConstantMethod(executableElement, context))
                        .collect(toList());

        interfaceMethods.addAll(result);
    }
}
