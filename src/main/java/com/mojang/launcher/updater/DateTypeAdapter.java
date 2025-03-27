/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonDeserializationContext
 *  com.google.gson.JsonDeserializer
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonParseException
 *  com.google.gson.JsonPrimitive
 *  com.google.gson.JsonSerializationContext
 *  com.google.gson.JsonSerializer
 *  com.google.gson.JsonSyntaxException
 */
package com.mojang.launcher.updater;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTypeAdapter
implements JsonSerializer<Date>,
JsonDeserializer<Date> {
    private final DateFormat enUsFormat = DateFormat.getDateTimeInstance(2, 2, Locale.US);
    private final DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!(json instanceof JsonPrimitive)) {
            throw new JsonParseException("The date should be a string value");
        }
        Date date = this.deserializeToDate(json.getAsString());
        if (typeOfT == Date.class) {
            return date;
        }
        throw new IllegalArgumentException(this.getClass() + " cannot deserialize to " + typeOfT);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
        DateFormat dateFormat;
        DateFormat dateFormat2 = dateFormat = this.enUsFormat;
        synchronized (dateFormat2) {
            return new JsonPrimitive(this.serializeToString(src));
        }
    }

    public Date deserializeToDate(String string) {
        DateFormat dateFormat;
        DateFormat dateFormat2 = dateFormat = this.enUsFormat;
        synchronized (dateFormat2) {
            try {
                return this.enUsFormat.parse(string);
            }
            catch (ParseException parseException) {
                try {
                    return this.iso8601Format.parse(string);
                }
                catch (ParseException parseException2) {
                    try {
                        String cleaned = string.replace("Z", "+00:00");
                        cleaned = cleaned.substring(0, 22) + cleaned.substring(23);
                        return this.iso8601Format.parse(cleaned);
                    }
                    catch (Exception e) {
                        throw new JsonSyntaxException("Invalid date: " + string, (Throwable)e);
                    }
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String serializeToString(Date date) {
        DateFormat dateFormat;
        DateFormat dateFormat2 = dateFormat = this.enUsFormat;
        synchronized (dateFormat2) {
            String result = this.iso8601Format.format(date);
            return result.substring(0, 22) + ":" + result.substring(22);
        }
    }
}

