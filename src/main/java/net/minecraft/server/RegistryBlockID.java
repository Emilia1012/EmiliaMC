package net.minecraft.server;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

public class RegistryBlockID<T> implements Registry<T> {

    private int a;
    private com.koloboke.collect.map.hash.HashObjIntMap<T> b; // Akarin - IdentityHashMap ->  HashObjIntMap
    private final List<T> c;

    public RegistryBlockID() {
        this(512);
    }

    public RegistryBlockID(int i) {
        this.c = Lists.newArrayListWithExpectedSize(i);
        this.b = com.koloboke.collect.map.hash.HashObjIntMaps.getDefaultFactory().withKeyEquivalence(com.koloboke.collect.Equivalence.identity()).newMutableMap(i); // Akarin - koloboke
    }

    public void a(T t0, int i) {
        // Akarin start
        if (t0 == null) return;
        com.koloboke.collect.map.hash.HashObjIntMap<T> toImmutable = com.koloboke.collect.map.hash.HashObjIntMaps.newMutableMap(this.b);
        toImmutable.put(t0, i);
        this.b = com.koloboke.collect.map.hash.HashObjIntMaps.getDefaultFactory().withKeyEquivalence(com.koloboke.collect.Equivalence.identity()).newImmutableMap(toImmutable);
        //this.b.put(t0, i);
        // Akarin end

        while (this.c.size() <= i) {
            this.c.add(null); // Paper - decompile fix
        }

        this.c.set(i, t0);
        if (this.a <= i) {
            this.a = i + 1;
        }

    }

    public void b(T t0) {
        this.a(t0, this.a);
    }

    public int getId(T t0) {
        //Integer integer = this.b.get(t0); // Akarin

        return this.b.getOrDefault(t0, -1); // Akarin
    }

    @Nullable
    public final T fromId(int i) {
        return i >= 0 && i < this.c.size() ? this.c.get(i) : null;
    }

    public Iterator<T> iterator() {
        return Iterators.filter(this.c.iterator(), Predicates.notNull());
    }

    public int size() { return this.a(); } // Paper - OBFHELPER
    public int a() {
        return this.b.size();
    }
}
