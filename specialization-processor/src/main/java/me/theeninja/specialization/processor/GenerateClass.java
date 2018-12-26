package me.theeninja.specialization.processor;

public @interface GenerateClass {
    String className();
    String factoryMethodName();

    Class<?> defaultImplementation();

    Specialization[] specializations();
}
