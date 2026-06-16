package thaumicenergistics.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ArcaneP2PTransfer {

    private ArcaneP2PTransfer() {
    }

    public static List<Float> distribute(float amount, int outputs) {
        if (amount <= 0 || outputs <= 0) {
            return Collections.emptyList();
        }

        List<Float> distribution = new ArrayList<>(outputs);
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
