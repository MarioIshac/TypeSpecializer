package me.theeninja.specialization.example.array;

import me.theeninja.specialization.example.array.Array;
import me.theeninja.specialization.example.array.ArrayFactory;
import me.theeninja.specialization.example.array.BooleanArray;
import me.theeninja.specialization.example.array.DefaultArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArrayTest {
    private static final int SPECIFIC_ARRAY_SIZE = 1;

    @Test
    public void testCreateBooleanArray() {
        final Array<Boolean> array = ArrayFactory.generate(Boolean.class, SPECIFIC_ARRAY_SIZE);

        final Class<? extends Array> observedArrayClass = array.getClass();

        assertEquals(BooleanArray.class, observedArrayClass);
    }
    @Test
    public void testCreateDefaultArray() {
        testCreateDefaultArray(Integer.class);
        testCreateDefaultArray(boolean.class);
    }

    private <T> void testCreateDefaultArray(Class<T> arrayType) {
        final Array<?> array = ArrayFactory.generate(Integer.class, SPECIFIC_ARRAY_SIZE);

        final Class<? extends Array> observedArrayClass = array.getClass();

        assertEquals(DefaultArray.class, observedArrayClass);
    }
}