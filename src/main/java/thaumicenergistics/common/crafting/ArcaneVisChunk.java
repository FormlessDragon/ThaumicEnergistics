package thaumicenergistics.common.crafting;

/**
 * Identifies one aura-bearing chunk without retaining a world reference in the crafting worker.
 *
 * @param dimension the numeric dimension id captured on the server thread
 * @param x         the chunk x coordinate
 * @param z         the chunk z coordinate
 */
public record ArcaneVisChunk(int dimension, int x, int z) implements Comparable<ArcaneVisChunk> {

    @Override
    public int compareTo(ArcaneVisChunk other) {
        int comparison = Integer.compare(this.dimension, other.dimension);
        if (comparison == 0) {
            comparison = Integer.compare(this.x, other.x);
        }
        if (comparison == 0) {
            comparison = Integer.compare(this.z, other.z);
        }
        return comparison;
    }
}
