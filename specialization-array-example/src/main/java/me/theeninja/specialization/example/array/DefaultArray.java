package me.theeninja.specialization.example.array;

public class DefaultArray<T> extends Array<T> {
    private final T[] values;

    @SuppressWarnings("unchecked")
    DefaultArray(int size) {
        super(size);

        this.values = (T[]) new Object[size()];
    }

    @Override
    void setHelper(int index, T value) {
        getValues()[index] = value;
    }

    @Override
    T getHelper(int index) {
        return getValues()[index];
    }

    private T[] getValues() {
        return values;
    }
}
