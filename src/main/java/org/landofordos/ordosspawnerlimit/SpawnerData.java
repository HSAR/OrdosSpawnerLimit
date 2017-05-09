package org.landofordos.ordosspawnerlimit;

import java.io.Serializable;

public class SpawnerData implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private int x, y, z;
    private int hashCode;
    private int spawnsRemaining;

    public SpawnerData(int x, int y, int z, int spawnsRemaining) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.spawnsRemaining = spawnsRemaining;
        String hashBuilder = getX() + "," + getY() + "," + getZ();
        hashCode = hashBuilder.hashCode();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getSpawnsRemaining() {
        return spawnsRemaining;
    }

    public void setSpawnsRemaining(int newVal) {
        spawnsRemaining = newVal;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SpawnerData) {
            SpawnerData otherPosition = (SpawnerData) o;
            if (x == otherPosition.getX()) {
                if (y == otherPosition.getY()) {
                    if (z == otherPosition.getZ()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String toString() {
        return ("(" + getX() + "," + getY() + "," + getZ() + "=" + spawnsRemaining + ")");
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
