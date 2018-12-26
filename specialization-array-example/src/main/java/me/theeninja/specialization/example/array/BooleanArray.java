package me.theeninja.specialization.example.array;

public class BooleanArray extends Array<Boolean> {
    private final int[] bits;

    BooleanArray(int size) {
        super(size);

        final int chunksCount = (int) Math.ceil(size / Integer.SIZE);

        this.bits = new int[chunksCount];
    }

    private void setTrueBit(final int chunkIndex, final int bitInChunkIndex) {
        int flag = 1 << bitInChunkIndex;

        getBits()[chunkIndex] |= flag;
    }

    private void setFalseBit(final int chunkIndex, final int bitInChunkIndex) {
        int flag = ~(1 << bitInChunkIndex);

        getBits()[chunkIndex] &= flag;
    }

    @Override
    void setHelper(final int index, final Boolean value) {
        final int chunkIndex = index / Integer.SIZE;
        final int bitInChunkIndex = index % Integer.SIZE;

        if (value) {
            setTrueBit(chunkIndex, bitInChunkIndex);
        } else {
            setFalseBit(chunkIndex, bitInChunkIndex);
        }
    }

    @Override
    Boolean getHelper(final int index) {
        final int chunkIndex = index / Integer.SIZE;
        final int bitInChunkIndex = index % Integer.SIZE;

        final int flag = 1 << bitInChunkIndex;

        return (getBits()[chunkIndex] & flag) > 0;
    }

    public int[] getBits() {
        return bits;
    }
}
