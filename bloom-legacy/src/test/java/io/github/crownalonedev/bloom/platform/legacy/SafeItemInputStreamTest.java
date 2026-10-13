package io.github.crownalonedev.bloom.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.junit.jupiter.api.Test;

class SafeItemInputStreamTest {

    private static byte[] write(Object value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes);
        out.writeObject(value);
        out.close();
        return bytes.toByteArray();
    }

    private static Object read(byte[] bytes) throws IOException, ClassNotFoundException {
        SafeItemInputStream in = new SafeItemInputStream(new ByteArrayInputStream(bytes));
        try {
            return in.readObject();
        } finally {
            in.close();
        }
    }

    @Test
    void itemShapedDataRoundTrips() throws Exception {
        // What ItemStack/ItemMeta serialization produces: maps of strings, boxed numbers and (Guava) collections.
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("display-name", "§aGem");
        meta.put("lore", ImmutableList.of("line 1", "line 2"));
        meta.put("enchants", ImmutableMap.of("DURABILITY", 1));
        meta.put("repair-cost", 3);
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("type", "EMERALD");
        item.put("damage", (short) 0);
        item.put("amount", 2);
        item.put("meta", meta);
        item.put("tags", new ArrayList<Object>(Arrays.asList(1.5d, 2L, true, new int[] {1, 2})));

        @SuppressWarnings("unchecked")
        Map<String, Object> copy = (Map<String, Object>) read(write(item));
        assertEquals("EMERALD", copy.get("type"));
        assertEquals(meta, copy.get("meta"));
    }

    /** Stands in for a library "gadget" class on the server's class path. */
    static final class Gadget implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    @Test
    void otherClassesAreRejectedBeforeInstantiation() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("meta", new Gadget());
        InvalidClassException e = assertThrows(InvalidClassException.class, () -> read(write(payload)));
        assertTrue(e.getMessage().contains(Gadget.class.getName()), e.getMessage());

        assertThrows(InvalidClassException.class, () -> read(write(new File("x"))));
    }

    @Test
    void proxiesAreRejected() throws Exception {
        Object proxy = Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {Runnable.class, Serializable.class}, new Handler());
        assertThrows(InvalidClassException.class, () -> read(write(proxy)));
    }

    static final class Handler implements java.lang.reflect.InvocationHandler, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            return null;
        }
    }

    @Test
    void allowList() {
        assertTrue(SafeItemInputStream.isAllowed("org.bukkit.util.io.Wrapper"));
        assertTrue(SafeItemInputStream.isAllowed("java.util.HashMap"));
        assertTrue(SafeItemInputStream.isAllowed("com.google.common.collect.ImmutableMap$SerializedForm"));
        assertTrue(SafeItemInputStream.isAllowed("[Ljava.lang.Object;"));
        assertTrue(SafeItemInputStream.isAllowed("[[I"));
        assertFalse(SafeItemInputStream.isAllowed("java.util.concurrent.ConcurrentHashMap"));
        assertFalse(SafeItemInputStream.isAllowed("org.apache.commons.collections.functors.InvokerTransformer"));
        assertFalse(SafeItemInputStream.isAllowed("[Lorg.apache.commons.collections.Transformer;"));
        assertFalse(SafeItemInputStream.isAllowed("com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl"));
        assertFalse(SafeItemInputStream.isAllowed("java.lang.reflect.Proxy"));
    }
}
