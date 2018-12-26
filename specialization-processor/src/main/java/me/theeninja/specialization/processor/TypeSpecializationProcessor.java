package me.theeninja.specialization.processor;

import com.squareup.javapoet.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeSpecializationProcessor extends AbstractProcessor {
    private Types typeUtils() {
        return processingEnv.getTypeUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(GenerateClass.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_10;
    }

    private void error(final Element element, String message, final Object... formatArguments) {
        final Messager messager = processingEnv.getMessager();

        message = String.format(message, formatArguments);

        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    private static TypeMirror getClassTypeMirror(Runnable exceptionTriggerer) {
        try {
            exceptionTriggerer.run();
        }
        catch (final MirroredTypeException e) {
            return e.getTypeMirror();
        }

        throw new RuntimeException("Type Mirror could not be fetched");
    }

   private static List<? extends TypeMirror> getClassTypeMirrors(Runnable exceptionTriggerer) {
        try {
            exceptionTriggerer.run();
        }
        catch (final MirroredTypesException e) {
            return e.getTypeMirrors();
        }

       throw new RuntimeException("Type Mirror could not be fetched");

   }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elementSet = roundEnv.getElementsAnnotatedWith(GenerateClass.class);

        System.out.println("Annotated elements " + elementSet);

        for (Element element : elementSet) {
            final boolean processResult = processElement(element);

            if (!processResult) {
                return false;
            }
        }

        return true;
    }

    private static final TypeName WILDCARD_TYPE = WildcardTypeName.subtypeOf(Object.class);
    private static final ClassName CLASSTYPE = ClassName.get(Class.class);

    private ParameterizedTypeName getParametrizedClass(TypeVariableName typeVariableName) {
        return ParameterizedTypeName.get(CLASSTYPE, typeVariableName);
    }

    private boolean processElement(final Element element) {
        final TypeElement abstractType = (TypeElement) element;
        final TypeMirror abstractTypeMirror = abstractType.asType();

        final List<? extends Element> abstractTypeMemberElements = abstractType.getEnclosedElements();

        final Set<MethodSpec> abstractTypeFactoryMethods = new HashSet<>();

        final GenerateClass generateClass = abstractType.getAnnotation(GenerateClass.class);
        final Specialization[] specializations = generateClass.specializations();

        final String abstractClassQualifiedName = abstractType.getQualifiedName().toString();
        final int classNameIndex = abstractClassQualifiedName.lastIndexOf('.');

        // same as the package className of the annotated abstract class
        final String generatedClassPackageName = abstractClassQualifiedName.substring(0, classNameIndex);

        final List<? extends TypeParameterElement> typeParameterElements = abstractType.getTypeParameters();
        final List<TypeVariableName> generatedClassTypes = getTypeParameters(typeParameterElements);

        final TypeVariableName[] abstractClassTypeParameterArray = generatedClassTypes.toArray(new TypeVariableName[0]);

        final ClassName abstractClassName = ClassName.get(abstractType);
        final ClassName generatedClassName = ClassName.get(generatedClassPackageName, generateClass.className());

        final ParameterizedTypeName parametrizedGeneratedClassName = ParameterizedTypeName.get(
            abstractClassName,
            abstractClassTypeParameterArray
        );

       final TypeMirror defaultImplementation = getClassTypeMirror(generateClass::defaultImplementation);

        System.out.println("Annotated type elements " + abstractTypeMemberElements);

        final String factoryMethodName = generateClass.factoryMethodName();

        final int specializationArgumentCount = typeParameterElements.size();

        for (final Element abstractTypeMemberElement : abstractTypeMemberElements) {
            if (abstractTypeMemberElement.getKind() != ElementKind.CONSTRUCTOR) {
                continue;
            }

            final ExecutableElement abstractTypeConstructorElement = (ExecutableElement) abstractTypeMemberElement;

            final MethodSpec abstractTypeFactoryMethod = newFactoryMethod(
                    factoryMethodName,
                    abstractType,
                    abstractTypeConstructorElement,
                    parametrizedGeneratedClassName,
                    specializations,
                    specializationArgumentCount,
                    defaultImplementation,
                    generatedClassTypes
            );

            // Indicates error occurred during creation of factory method
            if (abstractTypeFactoryMethod == null) {
                return false;
            }

            abstractTypeFactoryMethods.add(abstractTypeFactoryMethod);
        }

        final TypeSpec.Builder generatedClassBuilder = TypeSpec
                .classBuilder(generatedClassName);

        final TypeSpec generatedClass = generatedClassBuilder
                .addModifiers(Modifier.PUBLIC)
                .addMethods(abstractTypeFactoryMethods)
                .build();

        try {
            generateClass(generatedClassPackageName, generatedClass);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return true;
    }

    private static TypeVariableName getTypeVariableName(TypeParameterElement typeParameterElement) {
        final String typeParameterSymbol = typeParameterElement.toString();
        final List<? extends TypeMirror> typeParameterBounds = typeParameterElement.getBounds();

        TypeName[] typeParameterBoundArray = typeParameterBounds.stream()
                .map(TypeName::get)
                .toArray(TypeName[]::new);

        return TypeVariableName.get(typeParameterSymbol, typeParameterBoundArray);
    }

    private List<TypeVariableName> getTypeParameters(
        final List<? extends TypeParameterElement> typeParameterElements
    ) {
        return typeParameterElements.stream()
                .map(TypeSpecializationProcessor::getTypeVariableName)
                .collect(Collectors.toList());
    }

    private boolean appendNormalReturn(
            final MethodSpec.Builder factoryMethodBuilder,
            final ParameterizedTypeName abstractParametrizedClassName,
            final TypeMirror defaultImplementation,
            final String[] constructorArgumentNames
    ) {
        appendReturnImplementation(
            factoryMethodBuilder,
            abstractParametrizedClassName,
            defaultImplementation,
            constructorArgumentNames
        );

        return true;
    }

    private static void appendReturnImplementation(
            final MethodSpec.Builder factoryMethodBuilder,
            final ParameterizedTypeName abstractParametrizedClassName,
            final TypeMirror implementation,
            final String[] constructorArgumentNames
    ) {
        String constructorArgumentJoinedList = String.join(", ", constructorArgumentNames);

        factoryMethodBuilder.addStatement(
                "return ($T) new $T($L)",
                abstractParametrizedClassName,
                implementation,
                constructorArgumentJoinedList
        );
    }

    private static void appendSpecializationCheck(
            final MethodSpec.Builder factoryMethodBuilder,
            final ParameterizedTypeName abstractParametrizedClassName,
            final ExtractedSpecialization extractedSpecialization,
            final String[] constructorArgumentNames
    ) {
        final List<? extends TypeMirror> specializationArguments = extractedSpecialization.getArguments();
        final TypeMirror specializedImplementation = extractedSpecialization.getImplementation();

        final StringBuilder ifConditionBuilder = new StringBuilder();

        for (int specializedArgumentIndex = 0;
             specializedArgumentIndex < specializationArguments.size();
             specializedArgumentIndex++) {

            final TypeMirror requiredSpecializationArgument = specializationArguments.get(specializedArgumentIndex);

            String specializationArgumentName = newSpecializationArgumentName(specializedArgumentIndex);

            ifConditionBuilder
                    .append(specializationArgumentName)
                    .append(" == ")
                    .append(requiredSpecializationArgument)
                    .append(".class");

            if (specializedArgumentIndex != specializationArguments.size() - 1) {
                ifConditionBuilder.append(" && ");
            }
        }

        final String ifCondition = ifConditionBuilder.toString();

        factoryMethodBuilder.beginControlFlow("if ($L)", ifCondition);

        appendReturnImplementation(
             factoryMethodBuilder,
             abstractParametrizedClassName,
             specializedImplementation,
             constructorArgumentNames
        );

        factoryMethodBuilder.endControlFlow();
    }

    private void generateClass(String packageName, TypeSpec classToGenerate) throws IOException {
        final Filer filer = processingEnv.getFiler();

        JavaFile javaFile = JavaFile
                .builder(packageName, classToGenerate)
                .indent("    ") // 4 spaces
                .build();

        javaFile.writeTo(filer);

        System.out.println("GENERATED CLASS");
    }

    private static class ExtractedSpecialization {
        private final List<? extends TypeMirror> arguments;
        private final TypeMirror implementation;

        private ExtractedSpecialization(Specialization specialization) {
            this.arguments = getClassTypeMirrors(specialization::arguments);
            this.implementation = getClassTypeMirror(specialization::implementation);
        }

        List<? extends TypeMirror> getArguments() {
            return arguments;
        }

        TypeMirror getImplementation() {
            return implementation;
        }
    }

    @SafeVarargs
    private static List<TypeName> getExceptionTypeNames(Class<? extends Exception>... exceptionClasses) {
        return Arrays.stream(exceptionClasses).map(TypeName::get).collect(Collectors.toList());
    }

    private MethodSpec newFactoryMethod(
            final String factoryMethodName,
            final TypeElement abstractType,
            final ExecutableElement annotatedTypeConstructorElement,
            final ParameterizedTypeName abstractParametrizedClassName,
            final Specialization[] specializations,
            final int specializationArgumentCount,
            final TypeMirror normalImplementation,
            final List<TypeVariableName> generatedClassTypes
    ) {

        final Set<Modifier> constructorModifiers = annotatedTypeConstructorElement.getModifiers();

        MethodSpec.Builder factoryMethodBuilder = MethodSpec
                .methodBuilder(factoryMethodName)
                .addTypeVariables(generatedClassTypes)
                .addModifiers(constructorModifiers)
                // all factory methods are static
                .addModifiers(Modifier.STATIC)
                .returns(abstractParametrizedClassName);

        final ExtractedSpecialization[] extractedSpecializations = extractSpecializations(specializations);

        final boolean areValidSpecializations = areValidSpecializations(extractedSpecializations, specializationArgumentCount);

        if (!areValidSpecializations) {
            return null;
        }

        final List<? extends VariableElement> factoryMethodParameters = annotatedTypeConstructorElement.getParameters();

        final int factoryMethodParameterCount = factoryMethodParameters.size();

        String[] constructorArgumentNames = new String[factoryMethodParameterCount];
        TypeName[] constructorArgumentTypes = new TypeName[factoryMethodParameterCount];

        appendFactoryMethodSpecializationParameters(
                factoryMethodBuilder,
                generatedClassTypes
        );

        extractConstructorArguments(
                factoryMethodParameters,
                constructorArgumentNames,
                constructorArgumentTypes
        );

        appendFactoryMethodConstructorParameters(
                factoryMethodBuilder,
                constructorArgumentNames,
                constructorArgumentTypes
        );

        final boolean wasSpecializationChecksAppendSuccessful = appendSpecializationChecks(
                factoryMethodBuilder,
                abstractType,
                abstractParametrizedClassName,
                extractedSpecializations,
                constructorArgumentTypes,
                constructorArgumentNames
        );

        if (!wasSpecializationChecksAppendSuccessful) {
            return null;
        }

        final boolean wasNormalCheckAppendSuccessful = appendNormalReturn(
                factoryMethodBuilder,
                abstractParametrizedClassName,
                normalImplementation,
                constructorArgumentNames
        );


        if (!wasNormalCheckAppendSuccessful) {
            return null;
        }

        return factoryMethodBuilder.build();
    }

    private boolean appendSpecializationChecks(
        final MethodSpec.Builder factoryMethodBuilder,
        final TypeElement abstractType,
        final ParameterizedTypeName abstractParametrizedClassName,
        final ExtractedSpecialization[] extractedSpecializations,
        final TypeName[] constructorArgumentTypes,
        final String[] constructorArgumentNames
    ) {
        for (ExtractedSpecialization extractedSpecialization : extractedSpecializations) {
            final List<? extends TypeMirror> specializationArguments = extractedSpecialization.getArguments();
            final TypeMirror specializationImplementation = extractedSpecialization.getImplementation();

            final TypeMirror[] specializationArgumentArray = specializationArguments.toArray(
                    new TypeMirror[0]
            );

            final DeclaredType requiredSuperClass = typeUtils().getDeclaredType(
                    abstractType,
                    specializationArgumentArray
            );

            System.out.println("Required super class " + requiredSuperClass);

            boolean isSuperType = typeUtils().isAssignable(
                    extractedSpecialization.getImplementation(),
                    requiredSuperClass
            );

            if (!isSuperType) {
                error(abstractType, "Not a superclass of %s", specializationImplementation);
                return false;
            }

            appendSpecializationCheck(
                    factoryMethodBuilder,
                    abstractParametrizedClassName,
                    extractedSpecialization,
                    constructorArgumentNames
            );
        }

        return true;
    }

    private static ExtractedSpecialization[] extractSpecializations(Specialization[] specializations) {
        return Arrays.stream(specializations)
                .map(ExtractedSpecialization::new)
                .toArray(ExtractedSpecialization[]::new);
    }

    private static boolean areValidSpecializations(ExtractedSpecialization[] extractedSpecializations, final int requiredSpecializationArgumentCount) {
        return Arrays.stream(extractedSpecializations).allMatch(extractedSpecialization -> {
            final List<? extends TypeMirror> specializationArguments = extractedSpecialization.getArguments();

            return specializationArguments.size() == requiredSpecializationArgumentCount;
        });
    }

    private static String newSpecializationArgumentName(int specializationArgumentIndex) {
        return "type_" + specializationArgumentIndex;
    }

    private void appendFactoryMethodSpecializationParameters(
            final MethodSpec.Builder factoryMethodBuilder,
            List<TypeVariableName> generatedClassTypes
    ) {
        for (int specializationArgumentIndex = 0;
             specializationArgumentIndex < generatedClassTypes.size();
             specializationArgumentIndex++) {

            final String specializationArgumentName = newSpecializationArgumentName(specializationArgumentIndex);
            final TypeVariableName generatedClassType = generatedClassTypes.get(specializationArgumentIndex);

            final TypeName specializationClassType = getParametrizedClass(generatedClassType);

            factoryMethodBuilder.addParameter(
                    specializationClassType,
                    specializationArgumentName,
                    Modifier.FINAL
            );
        }
    }

    private void extractConstructorArguments(
            final List<? extends VariableElement> factoryMethodConstructorParameters,
            final String[] constructorArgumentNames,
            final TypeName[] constructorArgumentTypeNames
    ) {
        if (constructorArgumentNames.length != constructorArgumentTypeNames.length) {
            throw new IllegalArgumentException("Lengths of argument arrays do not match");
        }

        for (int factoryMethodParameterIndex = 0;
             factoryMethodParameterIndex < constructorArgumentNames.length;
             factoryMethodParameterIndex++) {

            final VariableElement factoryMethodParameter = factoryMethodConstructorParameters.get(
                    factoryMethodParameterIndex
            );

            final TypeMirror factoryMethodParameterType = factoryMethodParameter.asType();

            final TypeName factoryMethodParameterTypeName = TypeName.get(factoryMethodParameterType);
            final String factoryMethodParameterName = factoryMethodParameter.getSimpleName().toString();

            constructorArgumentNames[factoryMethodParameterIndex] = factoryMethodParameterName;
            constructorArgumentTypeNames[factoryMethodParameterIndex] = factoryMethodParameterTypeName;
        }
    }

    private void appendFactoryMethodConstructorParameters(
            final MethodSpec.Builder factoryMethodBuilder,
            final String[] argumentNames,
            final TypeName[] argumentTypeNames
    ) {
        if (argumentNames.length != argumentTypeNames.length) {
            throw new IllegalArgumentException("Lengths of argument arrays do not match");
        }

        for (int factoryMethodParameterIndex = 0;
             factoryMethodParameterIndex < argumentNames.length;
             factoryMethodParameterIndex++) {

            final String factoryMethodParameterName = argumentNames[factoryMethodParameterIndex];
            final TypeName factoryMethodParameterTypeName = argumentTypeNames[factoryMethodParameterIndex];

            factoryMethodBuilder.addParameter(
                    factoryMethodParameterTypeName,
                    factoryMethodParameterName,
                    Modifier.FINAL
            );
        }
    }
}
