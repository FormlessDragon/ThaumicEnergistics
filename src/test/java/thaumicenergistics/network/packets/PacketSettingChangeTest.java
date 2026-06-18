package thaumicenergistics.network.packets;

import ae2.api.config.ActionItems;
import ae2.api.config.Settings;
import ae2.api.config.SortOrder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import thaumicenergistics.api.config.SearchBoxMode;
import thaumicenergistics.config.ThESettings;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketSettingChangeTest {

    @Test
    void byteBufRoundTripPreservesSupergiantSettingAndValue() {
        PacketSettingChange decoded = roundTrip(new PacketSettingChange(Settings.SORT_BY, SortOrder.AMOUNT));

        assertAll(
                () -> assertSame(Settings.SORT_BY, decoded.getSetting()),
                () -> assertEquals(SortOrder.AMOUNT, decoded.getValue()));
    }

    @Test
    void byteBufRoundTripPreservesThaumicActionsSettingAndValue() {
        PacketSettingChange decoded = roundTrip(new PacketSettingChange(ThESettings.actions(), ActionItems.STASH));

        assertAll(
                () -> assertSame(ThESettings.actions(), decoded.getSetting()),
                () -> assertEquals(ActionItems.STASH, decoded.getValue()));
    }

    @Test
    void byteBufRoundTripPreservesThaumicSearchModeSettingAndValue() {
        PacketSettingChange decoded = roundTrip(new PacketSettingChange(ThESettings.searchMode(),
                SearchBoxMode.JEI_AUTOSEARCH_KEEP));

        assertAll(
                () -> assertSame(ThESettings.searchMode(), decoded.getSetting()),
                () -> assertEquals(SearchBoxMode.JEI_AUTOSEARCH_KEEP, decoded.getValue()));
    }

    @Test
    void unknownSettingResolvesNull() {
        PacketSettingChange decoded = roundTrip(new PacketSettingChange("missing_setting", SortOrder.NAME.name()));
        PacketSettingChange.ValidationResult validation = decoded.validate();

        assertAll(
                () -> assertNull(decoded.getSetting()),
                () -> assertNull(decoded.getValue()),
                () -> assertFalse(validation.valid()),
                () -> assertEquals("missing_setting", validation.failure().settingName()),
                () -> assertEquals(SortOrder.NAME.name(), validation.failure().valueName()),
                () -> assertTrue(validation.failure().message().contains("Unknown config setting")));
    }

    @Test
    void invalidValueResolvesNull() {
        PacketSettingChange decoded = roundTrip(new PacketSettingChange(Settings.SORT_BY.getName(), "NOT_A_SORT"));
        PacketSettingChange.ValidationResult validation = decoded.validate();

        assertAll(
                () -> assertSame(Settings.SORT_BY, decoded.getSetting()),
                () -> assertNull(decoded.getValue()),
                () -> assertFalse(validation.valid()),
                () -> assertEquals(Settings.SORT_BY.getName(), validation.failure().settingName()),
                () -> assertEquals("NOT_A_SORT", validation.failure().valueName()),
                () -> assertTrue(validation.failure().message().contains("Invalid value")));
    }

    @Test
    void serverHandlerRejectsUnknownSettingBeforeUsingMinecraftContext() {
        PacketSettingChange packet = roundTrip(new PacketSettingChange("missing_setting", SortOrder.NAME.name()));

        assertNull(new PacketSettingChange.HandlerServer().onMessage(packet, null));
    }

    @Test
    void clientHandlerRejectsInvalidValueBeforeUsingMinecraftContext() {
        PacketSettingChange packet = roundTrip(new PacketSettingChange(Settings.SORT_BY.getName(), "NOT_A_SORT"));

        assertNull(new PacketSettingChange.HandlerClient().onMessage(packet, null));
    }

    @Test
    void validPacketHasExplicitDiagnosticSuccessResult() {
        PacketSettingChange decoded = roundTrip(new PacketSettingChange(Settings.SORT_BY, SortOrder.AMOUNT));
        PacketSettingChange.ValidationResult validation = decoded.validate();

        assertAll(
                () -> assertTrue(validation.valid()),
                () -> assertSame(Settings.SORT_BY, validation.setting()),
                () -> assertEquals(SortOrder.AMOUNT, validation.value()),
                () -> assertNull(validation.failure()));
    }

    @Test
    void nullValueFailsFastDuringValueResolution() {
        PacketSettingChange packet = new PacketSettingChange(Settings.SORT_BY.getName(), null);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, packet::getValue);

        assertTrue(thrown.getMessage().contains(Settings.SORT_BY.getName()));
    }

    @Test
    void valueParsingIsCaseInsensitive() {
        PacketSettingChange decoded = roundTrip(new PacketSettingChange(Settings.SORT_BY.getName(), "amount"));

        assertAll(
                () -> assertSame(Settings.SORT_BY, decoded.getSetting()),
                () -> assertEquals(SortOrder.AMOUNT, decoded.getValue()));
    }

    private static PacketSettingChange roundTrip(PacketSettingChange packet) {
        ByteBuf buffer = Unpooled.buffer();
        packet.toBytes(buffer);
        PacketSettingChange decoded = new PacketSettingChange();
        decoded.fromBytes(buffer);
        assertEquals(0, buffer.readableBytes(), "PacketSettingChange should consume its payload");
        return decoded;
    }
}
