# 2026/11/03 - 2.3.0
## Features
* Added the ME Essentia Cell Housing and rebuilt Essentia Storage Cell crafting around it: components, cells and the
  "housing + component" assembly are now Arcane Crafting recipes, the research pages list them, and cell disassembly
  returns the new housing instead of the AE2 housing
* Creative Essentia Cells that are not marked now provide every essentia instead of nothing, with a tooltip line for
  the unmarked state; marking the cell still limits it to the marked aspects
* Knowledge Core items now report the number of stored recipes in their tooltip, and holding Shift lists the stored
  results
* Arcane Terminals keep their own armor slots: armor placed there is stored on the terminal (wireless terminals keep it
  in the terminal item's NBT) and provides its Vis discount, together with Baubles and potion effects
* Valid destination slots are highlighted while an item, upgrade or JEI preview is selected (terminal armor/upgrades,
  inscriber knowledge core, assembler knowledge core/upgrades, knowledge core upgrades)
* Vis amounts shown by the terminal and the assembler now match the terminal type's real values
* The internal Vis key is no longer offered in the terminal key-type selection, while still being registered so it can
  be serialized over the network
## Bugs
* Fixed the Knowledge Core GUI not refreshing after storing/deleting a recipe, showing stale contents, and throwing on
  invalid client actions
* Fixed Knowledge Core tooltips and GUI work: recipe slots are only rebuilt when the core actually changes, the tooltip
  only decodes stored recipes when expanded, and the inscriber's action buttons cache their enabled state
* Fixed JEI/HEI integration failing when ThaumicJEI is not installed: the Essentia Smelting category, its recipes, its
  catalyst items and the aspect converter are only registered when the mod is present
* Fixed the JEI recipe transfer preflight freezing on recipes it cannot satisfy (bounded search) and throwing instead of
  reporting a transfer error, and localized the transfer messages
* Fixed Knowledge Core/JEI transfer: malformed payloads are rejected with a message, leftovers from matrix writes return
  to ME storage, and player items that no longer fit go back to the inventory
* Fixed the Arcane Crafting Terminal result slot only handling pickup and quick move, results merging into stacks with
  different NBT, and self-returning ingredients being consumed
* Fixed Arcane Assembler particles corrupting the shared particle batch and leaking GL state; they now render in their
  own pass with independent spawn timers per assembler
* Fixed the Infusion Provider rescanning the whole network on every essentia change
* Fixed NPEs around parts without a tile or world (Vis access, wrench use, returning from a sub GUI)
* Fixed the Arcane Assembler container accepting interaction with a removed or out-of-range block
* Fixed the "Invalid shaped ore recipe" crash from the new arcane cell and housing recipes (AE2 block definitions have
  to be unwrapped to blocks before they can be used as recipe keys)
* Fixed item rendering leaving GL color, depth and blend state modified (essentia/vis icons, JEI category overlay,
  assembler GUI colors, particle colors)
* Fixed sound registration running before the sound registry event (empty sound registry) and sound ids being
  registered without the mod's namespace
* Fixed `ForgeUtil.areNBTTagsEqual` throwing on stacks without NBT and treating non-equal tags as equal
* Fixed `TCUtil.getCrystalAspect` throwing on empty or missing aspect lists
* Fixed ME storage simulation reporting more essentia than can actually be extracted from or inserted into containers
## API Changes
* `IArcaneTerminalHost` gained `getArmorInventory()`; third-party terminal hosts must implement it
* Added `IAEKeyViewable` so key types can opt out of the key-type selection screen
* `SlotArmor` now takes an `InternalInventory` instead of a Forge `IItemHandler`
* `TCCraftingManager.getDiscount` now takes the armor stacks to consider
* Renamed `ArcaneVisKeyType` to `ArcaneVisKeys`
* Renamed the terminal armor slot semantic id from `THE_PLAYER_ARMOR` to `THE_TERMINAL_ARMOR` (style JSONs updated)
* Removed the unused `KnowledgeCoreUtil.getRecipeMap` and the dead `sparseToCompressed` mapping
## Misc
* Mixin configuration renamed to `thaumicenergistics.mixins.json`; dropped `thaumicenergistics.default.mixin.json` and
  the unused pattern access mixin and accessor
* Textures overhauled (storage cells, components, knowledge cores, infusion provider, wireless items), with animated
  icons for the knowledge core, creative cell, diffusion core and coalescence core
* Removed unused textures (essentia bus faces and the old essentia terminal sprites)
* Added and corrected localizations (en_us, zh_cn), including JEI messages, the knowledge core tooltip, the unmarked
  creative cell and the cell housing
* Reworked the build scripts and CI workflows for the new toolchain setup, including the Gradle wrapper update
* Updated dependencies: AE2 Supergiant 1.1.0, JEI 4.35.0, and ThaumicJEI is now an optional (compile only) dependency

# 2023/04/30 - 2.2.7
## Features
* Jars and Void Jars can now be filled with the Essentia Terminal
## Bugs
* Fix overflow issues when creative essentia cells coexist with other cells
* Fixed a duplication bug where ghost items in the Arcane Inscriber would drop when broken or wrenched
## Misc
* Building off AE2 Extended Life now rather than base AE2
* Converted changelog to markdown
* Relocated primary git branch to main
* Artifact name changed to reflect extended life

# 2023/03/06 - 2.2.6
* Creative Essentia Cells now list MAX_INT essentia instead of 1000. They were previously still infinite, but with
  a cap of 1000 present in the AE system would prohibit large crafts.

# 2022/07/09 - 2.2.5 Release
* Corrected Essentia cell partitioning. Previously cells could be partitioned, but this was being ignored. Now
  partitioned cells will only accept the filtered essentia.
* Improved net handling code around Arcane Crafting Terminal to handle diffs of the AE2 inventory changes rather than
  the entire inventory. Should be a bit faster.
* Fixing Essentia export buses not respecting export cards. This continued to work on the first aspect in the list,
  without checking if it was needed or not. Now this will pass to the next essentia if none would be transferred.

## Build
* Fixed a number of issues around the use of the CurseMaven plugin, probably fallout from the CurseForge API
  changes lately. Builds simply broke with missing dependencies, and some version numbers for dependent mods seemed
  shifted.

# 2019/07/28 - 2.2.3 - Release
* You can now scan any item or block from AE2 to unlock research
* Added ME Controller as a Research Aid
* Added Buttons & Scroll to Essentia Terminal
* Hide Dummy Aspect (#381)
* Update ru_ru.lang (#376)

# 2019/05/01 - 2.2.2 - Release
* Compress JEI Packet (#368)
* Fixed being able to autocraft any item
* Fixed Autocraft Autostart bug (#373)
* Fixed firing crafting events (#369)
* Fixed crafting non-1 stacks (#375)

# 2019/03/12 - 2.2.1 - Release
* Fixed issue with inserting non-phial items into Essentia Terminal (#366)
* Fixed Tall Terminal Style (#365)
* Minor Research Improvements

# 2019/03/05 - 2.2.0 - Beta
* Implemented Autocrafting for the Arcane Crafting Terminal
* Added Terminal Style option to ACT
* Fixed issue with Infusion Provider not always connecting
* Fixed issue with parts not dropping contents
* Fixes issue when wrenching parts and gui still opens

# 2019/02/07 - 2.1.1 - Release
* Added ru_ru.lang
* Fixed issue with viewing just crafted/stored items

# 2019/01/05 - 2.1.0 - Beta
* Added ability to search items by the aspects they break into
* Added JEI & Thaumic JEI integration
* Added Clear Grid button to ACT
* Fixed issue with dragging stacks in ACT
* Minor bug fixes

# 2018/11/28 - 2.0.0 - Beta
* Initial public build for Minecraft 1.12.2
* Rewritten from scratch
* Added Essentia Storage Cells
* Added Essentia Import Bus
* Added Essentia Export Bus
* Added Essentia Storage Bus
* Added Essentia Terminal
* Added Infusion Provider
* Added Arcane Crafting Terminal
