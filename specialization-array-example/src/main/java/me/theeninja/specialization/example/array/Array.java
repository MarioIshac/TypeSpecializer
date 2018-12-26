package me.theeninja.specialization.example.array;

import me.theeninja.specialization.processor.GenerateClass;
import me.theeninja.specialization.processor.Specialization;

@GenerateClass(
    className = "ArrayFactory",
    factoryMethodName = "generate",
    defaultImplementation = DefaultArray.class,
    specializations = @Specialization(arguments = Boolean.class, implementation = BooleanArray.class)
)

public abstract class Array<T> {
    private final int size;

    public void set(int index, T value) {
        ensureValidIndex(index);

        setHelper(index, value);
    }
    abstract void setHelper(int index, T value);

    public T get(int index) {
        ensureValidIndex(index);

        return getHelper(index);
    }
    abstract T getHelper(int index);

    public int size() {
        return this.size;
    }

    public Array(int size) {
        this.size = size;
    }

    private boolean isValidIndex(int index) {
        return 0 <= index && index < size() - 1;
    }

    private void ensureValidIndex(int index) {
        if (!isValidIndex(index)) {
            throw new IndexOutOfBoundsException("Index Provided: " + index + ", Size: " + size());
        }
    }
}