package thaumicenergistics.util;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;

public final class ArcaneP2PTransfer {

    private ArcaneP2PTransfer() {
    }

    public static FloatList distribute(float amount, int outputs) {
        if (amount <= 0 || outputs <= 0) {
            return new FloatArrayList();
        }

        FloatList distribution = new FloatArrayList(outputs);
        for (int i = 0; i < outputs; i++) {
            distribution.add(0.0f);
        }

        float remaining = amount;
        for (int i = 0; i < outputs; i++) {
            float share = i == outputs - 1 ? remaining : amount / outputs;
            distribution.set(i, share);
            remaining -= share;
        }
        return distribution;
    }
}
