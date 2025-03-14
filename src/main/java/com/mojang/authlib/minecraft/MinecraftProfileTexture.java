/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  org.apache.commons.io.FilenameUtils
 *  org.apache.commons.lang3.builder.ToStringBuilder
 */
package com.mojang.authlib.minecraft;

import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class MinecraftProfileTexture {
    private final String url;
    private final Map<String, String> metadata;

    public MinecraftProfileTexture(String url, Map<String, String> metadata) {
        this.url = url;
        this.metadata = metadata;
    }

    public String getUrl() {
        return this.url;
    }

    @Nullable
    public String getMetadata(String key) {
        if (this.metadata == null) {
            return null;
        }
        return this.metadata.get(key);
    }

    public String getHash() {
        return FilenameUtils.getBaseName((String)this.url);
    }

    public String toString() {
        return new ToStringBuilder((Object)this).append("url", (Object)this.url).append("hash", (Object)this.getHash()).toString();
    }

    public static enum Type {
        SKIN,
        CAPE;

    }
}

