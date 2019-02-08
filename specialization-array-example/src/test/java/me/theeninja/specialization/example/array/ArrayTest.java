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
        testCreateSpecificArray(Boolean.class, BooleanArray.class);
    }
    @Test
    public void testCreateDefaultArray() {
        testCreateDefaultArray(Integer.class);
        testCreateDefaultArray(boolean.class);
    }

    private <T> void testCreateDefaultArray(final Class<T> arrayComponentType) {
        testCreateSpecificArray(arrayComponentType, DefaultArray.class);
    }

    private <T, A extends Array> void testCreateSpecificArray(final Class<T> arrayComponentType, final Class<A> expectedArrayType) {
        final Array<?> array = ArrayFactory.generate(arrayComponentType, SPECIFIC_ARRAY_SIZE);

        final Class<? extends Array> observedArrayType = array.getClass();

        assertEquals(expectedArrayType, observedArrayType);
    }
}