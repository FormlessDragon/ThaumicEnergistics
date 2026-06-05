package thaumicenergistics.api.storage;

import ae2.items.storage.BasicStorageCell;
import thaumicenergistics.me.key.AEEssentiaKeys;

public class EssentiaStorageCell extends BasicStorageCell {

    private static final int BYTES_PER_TYPE = 8;

    public EssentiaStorageCell(int kilobytes) {
        super(1, kilobytes, BYTES_PER_TYPE, 12, AEEssentiaKeys.INSTANCE);
    }

    @SuppressWarnings("unused")
    public EssentiaStorageCell(int kilobytes, int totalTypes) {
        super(1, kilobytes, BYTES_PER_TYPE, totalTypes, AEEssentiaKeys.INSTANCE);
    }

}
