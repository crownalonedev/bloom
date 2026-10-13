package io.github.crownalonedev.bloom.apidoc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ManifestDocletTest {

    @Test
    void simplifyDropsPackagePrefixes() {
        assertEquals(
                "Map<String, List<ItemStack>>",
                ManifestDoclet.simplify(
                        "java.util.Map<java.lang.String, java.util.List<org.bukkit.inventory.ItemStack>>"));
        assertEquals(
                "Database.Builder", ManifestDoclet.simplify("io.github.crownalonedev.bloom.storage.Database.Builder"));
        assertEquals("int[]", ManifestDoclet.simplify("int[]"));
    }

    @Test
    void referencesBecomeReadableNames() {
        assertEquals("Items.setData", ManifestDoclet.reference("Items#setData(ItemStack, String, String)"));
        assertEquals("build", ManifestDoclet.reference("#build()"));
        assertEquals("Capability", ManifestDoclet.reference("io.github.crownalonedev.bloom.api.platform.Capability"));
    }

    @Test
    void internalAndCommonPackagesAreHidden() {
        assertTrue(ManifestDoclet.isHidden("io.github.crownalonedev.bloom.storage.internal"));
        assertTrue(ManifestDoclet.isHidden("io.github.crownalonedev.bloom.common.text"));
        assertFalse(ManifestDoclet.isHidden("io.github.crownalonedev.bloom.items"));
        assertFalse(ManifestDoclet.isHidden("io.github.crownalonedev.bloom.commonplace"));
    }

    @Test
    void jsonEscapesAndSeparates() {
        Json json = new Json();
        json.beginObject()
                .field("a", "x\"y\n")
                .name("b")
                .beginArray()
                .beginObject()
                .endObject()
                .beginObject()
                .endObject()
                .endArray()
                .endObject();
        assertEquals("{\"a\":\"x\\\"y\\n\",\"b\":[{},{}]}", json.toString());
    }
}
