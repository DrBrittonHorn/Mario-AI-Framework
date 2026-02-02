package mff.forwardmodel.slim.core;

import mff.forwardmodel.slim.sprites.FireballSlim;
import mff.forwardmodel.slim.sprites.ShellSlim;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class MarioUpdateContextSlim {

    public MarioWorldSlim world;
    public boolean[] actions;
    public int fireballsOnScreen;

    public final ArrayList<FireballSlim> fireballsToCheck = new ArrayList<>();
    public final ArrayList<ShellSlim> shellsToCheck = new ArrayList<>();
    final ArrayList<MarioSpriteSlim> addedSprites = new ArrayList<>();
    final ArrayList<MarioSpriteSlim> removedSprites = new ArrayList<>();

    // ThreadLocal pool - each thread gets its own pool for thread safety
    private static final ThreadLocal<ArrayDeque<MarioUpdateContextSlim>> pool =
        ThreadLocal.withInitial(ArrayDeque::new);

    public static MarioUpdateContextSlim get() {
        MarioUpdateContextSlim ctx = pool.get().poll();
        if (ctx != null) return ctx;

        MarioUpdateContextSlim newCtx = new MarioUpdateContextSlim();
        newCtx.addedSprites.ensureCapacity(10);
        newCtx.removedSprites.ensureCapacity(10);
        return newCtx;
    }

    static void back(MarioUpdateContextSlim ctx) {
        pool.get().add(ctx);
    }
}
