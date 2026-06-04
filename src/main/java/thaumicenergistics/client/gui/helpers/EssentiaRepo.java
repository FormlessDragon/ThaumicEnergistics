package thaumicenergistics.client.gui.helpers;

import ae2.api.config.SortDir;
import ae2.api.config.SortOrder;
import ae2.api.config.ViewItems;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import thaumicenergistics.me.key.AEEssentiaKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import static ae2.api.config.ViewItems.CRAFTABLE;
import static ae2.api.config.ViewItems.STORED;

/**
 * Essentia-only terminal repository backed by key-based display rows.
 */
public class EssentiaRepo {

    private final Map<AEEssentiaKey, TerminalDisplayStack> list = new LinkedHashMap<>();
    private ArrayList<TerminalDisplayStack> view = new ArrayList<>();
    private ViewItems viewMode = ViewItems.ALL;
    private SortDir sortDir = SortDir.ASCENDING;
    private SortOrder sortOrder = SortOrder.NAME;
    private GuiScrollBar scrollBar;
    private int rowSize = 9;
    private boolean changed = true;

    public void updateView() {
        if (!this.changed) {
            return;
        }

        this.changed = false;
        this.view = new ArrayList<>();

        for (TerminalDisplayStack stack : this.list.values()) {
            TerminalDisplayStack copy = stack.copy();
            if (this.viewMode == CRAFTABLE) {
                if (!copy.craftable()) {
                    continue;
                }
                copy = new TerminalDisplayStack(copy.key(), 0, true);
            } else if (this.viewMode == STORED && copy.stackSize() < 1) {
                continue;
            }
            this.view.add(copy);
        }

        this.view.sort(this.getComparator());
    }

    public void postUpdate(TerminalDisplayStack stack) {
        if (stack == null || !(stack.key() instanceof AEEssentiaKey)) {
            return;
        }
        this.list.put((AEEssentiaKey) stack.key(), stack.copy());
        this.changed = true;
    }

    public void postUpdate(KeyCounter counter) {
        for (Object2LongMap.Entry<AEKey> entry : counter) {
            if (entry.getKey() instanceof AEEssentiaKey) {
                this.postUpdate(TerminalDisplayStacks.of(entry.getKey(), entry.getLongValue(), false));
            }
        }
    }

    public TerminalDisplayStack getReferenceStack(int i) {
        int scroll = 0;
        if (this.scrollBar != null) {
            scroll = (int) Math.max(Math.min(this.scrollBar.getCurrentPosition(), Math.ceil((double) this.view.size() / this.rowSize)), 0);
        }
        i += scroll * this.rowSize;
        if (i < this.view.size()) {
            return this.view.get(i);
        }
        return null;
    }

    public int size() {
        return this.view.size();
    }

    public void clear() {
        this.list.clear();
        this.changed = true;
    }

    public void setScrollBar(GuiScrollBar scrollBar) {
        this.scrollBar = scrollBar;
    }

    public void setSortDir(SortDir sortDir) {
        this.sortDir = sortDir;
        this.changed = true;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        this.changed = true;
    }

    public void setViewMode(ViewItems viewMode) {
        this.viewMode = viewMode;
        this.changed = true;
    }

    private Comparator<TerminalDisplayStack> getComparator() {
        Comparator<TerminalDisplayStack> comparator;
        if (this.sortOrder == SortOrder.AMOUNT) {
            comparator = Comparator.comparingLong(TerminalDisplayStack::stackSize).reversed();
        } else {
            comparator = Comparator.comparing(EssentiaRepo::getAspectName, String.CASE_INSENSITIVE_ORDER);
        }

        if (this.sortDir == SortDir.DESCENDING) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private static String getAspectName(TerminalDisplayStack stack) {
        if (stack == null || !(stack.key() instanceof AEEssentiaKey)) {
            return "";
        }
        AEEssentiaKey key = (AEEssentiaKey) stack.key();
        return key.getAspect() == null || key.getAspect().getName() == null ? "" : key.getAspect().getName();
    }
}
