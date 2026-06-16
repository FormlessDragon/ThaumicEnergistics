package thaumicenergistics.part;

import org.junit.jupiter.api.Test;
import thaumicenergistics.util.ArcaneP2PTransfer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcaneP2PTransferTest {

    @Test
    void distributeSplitsVisEvenlyAcrossOutputs() {
        assertEquals(List.of(1.0f, 1.0f, 1.0f), ArcaneP2PTransfer.distribute(3.0f, 3));
    }

    @Test
    void distributePreservesFractionalVisWithoutRounding() {
        List<Float> shares = ArcaneP2PTransfer.distribute(1.0f, 3);

        assertEquals(3, shares.size());
        assertEquals(1.0f, shares.get(0) + shares.get(1) + shares.get(2), 0.000001f);
        assertEquals(1.0f / 3.0f, shares.get(0), 0.000001f);
        assertEquals(1.0f / 3.0f, shares.get(1), 0.000001f);
        assertEquals(1.0f - shares.get(0) - shares.get(1), shares.get(2), 0.000001f);
    }

    @Test
    void distributeDoesNotCreateOrLoseTinyVisAmounts() {
        List<Float> shares = ArcaneP2PTransfer.distribute(0.005f, 2);

        assertEquals(2, shares.size());
        assertEquals(0.005f, shares.get(0) + shares.get(1), 0.000001f);
    }

    @Test
    void distributeReturnsEmptyListWhenNoOutputsOrNoVis() {
        assertTrue(ArcaneP2PTransfer.distribute(1.0f, 0).isEmpty());
        assertTrue(ArcaneP2PTransfer.distribute(0.0f, 3).isEmpty());
    }
}
