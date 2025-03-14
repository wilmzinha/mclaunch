/*
 * Decompiled with CFR 0.152.
 */
package com.mojang.launcher.events;

import com.mojang.launcher.game.process.GameProcess;

public interface GameOutputLogProcessor {
    public void onGameOutput(GameProcess var1, String var2);
}

