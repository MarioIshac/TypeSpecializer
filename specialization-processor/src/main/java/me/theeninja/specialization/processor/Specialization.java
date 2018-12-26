package me.theeninja.specialization.processor;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Specialization {
    Class<?>[] arguments();
    Class<?> implementation();
}
