/*
 * Decompiled with CFR 0.152.
 */
package com.mojang.launcher.game.process.direct;

import com.mojang.launcher.game.process.GameProcess;
import com.mojang.launcher.game.process.GameProcessBuilder;
import com.mojang.launcher.game.process.GameProcessFactory;
import com.mojang.launcher.game.process.direct.DirectGameProcess;
import java.io.IOException;
import java.util.List;

public class DirectGameProcessFactory
implements GameProcessFactory {
    @Override
    public GameProcess startGame(GameProcessBuilder builder) throws IOException {
        List<String> full = builder.getFullCommands();
        return new DirectGameProcess(full, new ProcessBuilder(full).directory(builder.getDirectory()).redirectErrorStream(true).start(), builder.getSysOutFilter(), builder.getLogProcessor());
    }
}

