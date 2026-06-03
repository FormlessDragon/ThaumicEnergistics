package thaumicenergistics.client.gui.helpers;

import ae2.api.config.SortDir;
import ae2.api.config.SortOrder;
import ae2.api.config.ViewItems;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.core.AEConfig;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.api.config.PrefixSetting;
import thaumicenergistics.api.config.SearchBoxMode;
import thaumicenergistics.integration.jei.ThEJEI;
import thaumicenergistics.util.TCUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static ae2.api.config.ViewItems.CRAFTABLE;
import static ae2.api.config.ViewItems.STORED;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.UNICODE_CASE;

/**
 * Terminal repository backed by key-based display rows.
 */
public class MERepo {

    private final Map<AEKey, TerminalDisplayStack> list = new LinkedHashMap<>();
    private ArrayList<TerminalDisplayStack> view = new ArrayList<>();
    private String searchString = "";
    private String innerSearch = "";
    private ViewItems viewMode;
    private SortDir sortDir;
    private SortOrder sortOrder;
    private SearchBoxMode searchBoxMode;
    private GuiScrollBar scrollBar;
    private int rowSize = 9;

    private ViewItems lastView;
    private SearchBoxMode lastSearchMode;
    private SortOrder lastSortBy;
    private SortDir lastSortDir;
    private String lastSearch = "";

    private boolean resort = true;
    private boolean changed = false;

    public MERepo() {
        this.viewMode = ViewItems.ALL;
        this.sortDir = SortDir.ASCENDING;
        this.sortOrder = SortOrder.NAME;
        this.searchBoxMode = ThEApi.instance().config().searchBoxMode();
    }

    public MERepo(Class<?> ignored) {
        this();
    }

    public void updateView() {

        if (lastView != viewMode) {
            resort = true;
            lastView = viewMode;
        }

        if (lastSearchMode != searchBoxMode) {
            resort = true;
            lastSearchMode = searchBoxMode;
        }

        if (!lastSearch.equals(searchString)) {
            resort = true;
            lastSearch = searchString;
        }

        if (lastSortBy != sortOrder) {
            resort = true;
            lastSortBy = sortOrder;
        }

        if (lastSortDir != sortDir) {
            resort = true;
            lastSortDir = sortDir;
        }

        if (!changed && !resort) {
            return;
        }

        changed = false;
        resort = false;
        view = new ArrayList<>();

        innerSearch = searchString.toLowerCase();
        boolean searchMod = false;
        boolean searchAspect = false;
        boolean searchSpecific = false;

        PrefixSetting modSearchSetting = ThEApi.instance().config().modSearchSetting();
        PrefixSetting aspectSearchSetting = ThEApi.instance().config().aspectSearchSetting();

        if (Stream.of(SearchBoxMode.JEI_AUTOSEARCH, SearchBoxMode.JEI_MANUAL_SEARCH, SearchBoxMode.JEI_AUTOSEARCH_KEEP, SearchBoxMode.JEI_MANUAL_SEARCH_KEEP).anyMatch(m -> m == this.searchBoxMode)) {
            ThEJEI.setSearchText(searchString);
        }

        switch (modSearchSetting) {
            case ENABLED:
                searchMod = true;
            case REQUIRE_PREFIX:
                String modSearchPrefix = ThEApi.instance().config().modSearchPrefix();
                if (!innerSearch.startsWith(modSearchPrefix))
                    break;
                innerSearch = innerSearch.substring(modSearchPrefix.length());
                searchSpecific = true;
                searchMod = true;
            default:
        }

        if (!searchSpecific) {
            switch (aspectSearchSetting) {
                case ENABLED:
                    searchAspect = true;
                case REQUIRE_PREFIX:
                    String aspectSearchPrefix = ThEApi.instance().config().aspectSearchPrefix();
                    if (!innerSearch.startsWith(aspectSearchPrefix))
                        break;
                    innerSearch = innerSearch.substring(aspectSearchPrefix.length());
                    searchSpecific = true;
                    searchAspect = true;

                    searchMod = false;
                default:
            }
        }

        Pattern pattern;
        try {
            pattern = Pattern.compile(innerSearch, CASE_INSENSITIVE | UNICODE_CASE);
        } catch (Throwable ignored) {
            try {
                pattern = Pattern.compile(Pattern.quote(innerSearch), CASE_INSENSITIVE | UNICODE_CASE);
            } catch (Throwable ignored2) {
                return;
            }
        }

        final Pattern p = pattern;
        final boolean finalSearchSpecific = searchSpecific;
        final boolean searchByMod = searchMod;
        final boolean searchByAspect = searchAspect;

        newArrayList(list.values()).stream()
                .filter(t -> this.getViewMode() != CRAFTABLE || t.craftable())
                .filter(t -> this.getViewMode() != STORED || t.stackSize() > 0)
                .filter(t -> searchByQuery(finalSearchSpecific, searchByAspect, searchByMod, t, p))
                .forEach(t -> {
                    TerminalDisplayStack stack = t.copy();
                    if (this.getViewMode().equals(CRAFTABLE)) {
                        if (!stack.craftable())
                            return;
                        stack = new TerminalDisplayStack(stack.key(), 0, true);
                    } else if (this.getViewMode().equals(STORED) && stack.stackSize() < 1) {
                        return;
                    }
                    this.view.add(stack);
                });

        view.sort(getComparator(sortOrder));
    }

    private Comparator<TerminalDisplayStack> getComparator(SortOrder sortBy) {
        Comparator<TerminalDisplayStack> comparator;

        if (sortBy == SortOrder.MOD) {
            comparator = Comparator.comparing((TerminalDisplayStack stack) -> stack.key().getModId(), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(MERepo::displayName, String.CASE_INSENSITIVE_ORDER);
        } else if (sortBy == SortOrder.AMOUNT) {
            comparator = Comparator.comparingLong(TerminalDisplayStack::stackSize).reversed();
        } else if (sortBy == SortOrder.INVTWEAKS) {
            comparator = Comparator.comparing(MERepo::displayName, String.CASE_INSENSITIVE_ORDER);
        } else {
            comparator = Comparator.comparing(MERepo::displayName, String.CASE_INSENSITIVE_ORDER);
        }

        if (this.sortDir == SortDir.DESCENDING) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    public boolean searchByQuery(boolean searchSpecific,
                                 boolean searchByAspect,
                                 boolean searchByMod,
                                 TerminalDisplayStack stack,
                                 Pattern pattern) {
        if (searchSpecific) {
            if (searchByAspect) {
                return searchAspects(stack, pattern);
            } else if (searchByMod) {
                return searchMod(stack, pattern);
            }
        } else {
            if (searchByAspect && searchAspects(stack, pattern))
                return true;
            if (searchByMod && searchMod(stack, pattern))
                return true;
            return searchName(stack, pattern) || searchTooltip(stack, pattern);
        }

        return true;
    }

    public void postUpdate(TerminalDisplayStack stack) {
        if (stack == null || stack.key() == null) {
            return;
        }
        this.list.put(stack.key(), stack.copy());
        changed = true;
    }

    public void postUpdate(KeyCounter counter) {
        for (Object2LongMap.Entry<AEKey> entry : counter) {
            this.postUpdate(entry.getKey(), entry.getLongValue(), false);
        }
    }

    public void postUpdate(AEKey key, long amount, boolean craftable) {
        this.postUpdate(TerminalDisplayStacks.of(key, amount, craftable));
    }

    public TerminalDisplayStack getReferenceStack(int i) {
        int scroll = 0;
        if (this.scrollBar != null) {
            scroll = (int) Math.max(Math.min(this.scrollBar.getCurrentPosition(), Math.ceil((double) this.view.size() / this.rowSize)), 0);
        }
        i += scroll * this.rowSize;
        if (i < this.view.size())
            return this.view.get(i);
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

    public GuiScrollBar getScrollBar() {
        return this.scrollBar;
    }

    public void setRowSize(int rowSize) {
        this.rowSize = rowSize;
    }

    public int getRowSize() {
        return this.rowSize;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getSearchString() {
        return this.searchString;
    }

    public void setViewMode(ViewItems view) {
        this.viewMode = view;
    }

    public ViewItems getViewMode() {
        return this.viewMode;
    }

    public SortDir getSortDir() {
        return this.sortDir;
    }

    public void setSortDir(SortDir sortDir) {
        this.sortDir = sortDir;
    }

    public SortOrder getSortOrder() {
        return this.sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public SearchBoxMode getSearchBoxMode() {
        return searchBoxMode;
    }

    public void setSearchBoxMode(SearchBoxMode searchBoxMode) {
        this.searchBoxMode = searchBoxMode;
    }

    private boolean searchName(TerminalDisplayStack stack, Pattern p) {
        return p.matcher(displayName(stack)).find();
    }

    private boolean searchTooltip(TerminalDisplayStack stack, Pattern p) {
        boolean terminalSearchToolTips = AEConfig.instance().isSearchModNameInTooltips();

        if (!terminalSearchToolTips) {
            return true;
        }

        ItemStack itemStack = stack.asItemStackRepresentation();
        List<String> tooltip = itemStack.getTooltip(null, ITooltipFlag.TooltipFlags.NORMAL);
        for (String line : tooltip) {
            if (p.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    private boolean searchMod(TerminalDisplayStack stack, Pattern p) {
        return p.matcher(stack.key().getModId()).find();
    }

    private boolean searchAspects(TerminalDisplayStack stack, Pattern p) {
        AspectList aspects = TCUtil.getItemAspects(stack.asItemStackRepresentation());
        if (aspects == null || aspects.size() < 1)
            return false;
        final Pattern pf = p;
        Stream<Aspect> stream = aspects.aspects.keySet().stream();
        return stream.anyMatch(aspect -> pf.matcher(aspect.getName()).find());
    }

    private static String displayName(TerminalDisplayStack stack) {
        if (stack == null || stack.key() == null) {
            return "";
        }
        return stack.key().getDisplayName().getFormattedText();
    }
}
