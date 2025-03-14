/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.io.Charsets
 *  org.apache.commons.io.IOUtils
 *  org.apache.commons.lang3.Validate
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.mojang.authlib;

import com.mojang.authlib.BaseAuthenticationService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class HttpAuthenticationService
extends BaseAuthenticationService {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Proxy proxy;

    protected HttpAuthenticationService(Proxy proxy) {
        Validate.notNull((Object)proxy);
        this.proxy = proxy;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    protected HttpURLConnection createUrlConnection(URL url) throws IOException {
        Validate.notNull((Object)url);
        LOGGER.debug("Opening connection to " + url);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection(this.proxy);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setUseCaches(false);
        return connection;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String performPostRequest(URL url, String post, String contentType) throws IOException {
        Validate.notNull((Object)url);
        Validate.notNull((Object)post);
        Validate.notNull((Object)contentType);
        HttpURLConnection connection = this.createUrlConnection(url);
        byte[] postAsBytes = post.getBytes(Charsets.UTF_8);
        connection.setRequestProperty("Content-Type", contentType + "; charset=utf-8");
        connection.setRequestProperty("Content-Length", "" + postAsBytes.length);
        connection.setDoOutput(true);
        LOGGER.debug("Writing POST data to " + url + ": " + post);
        OutputStream outputStream = null;
        try {
            outputStream = connection.getOutputStream();
            IOUtils.write((byte[])postAsBytes, (OutputStream)outputStream);
        }
        finally {
            IOUtils.closeQuietly((OutputStream)outputStream);
        }
        LOGGER.debug("Reading data from " + url);
        InputStream inputStream = null;
        try {
            String string;
            inputStream = connection.getInputStream();
            String result = IOUtils.toString((InputStream)inputStream, (Charset)Charsets.UTF_8);
            LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
            LOGGER.debug("Response: " + result);
            String string2 = string = result;
            return string2;
        }
        catch (IOException e) {
            IOUtils.closeQuietly((InputStream)inputStream);
            inputStream = connection.getErrorStream();
            if (inputStream != null) {
                String string;
                LOGGER.debug("Reading error page from " + url);
                String result = IOUtils.toString((InputStream)inputStream, (Charset)Charsets.UTF_8);
                LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
                LOGGER.debug("Response: " + result);
                String string3 = string = result;
                return string3;
            }
            LOGGER.debug("Request failed", (Throwable)e);
            throw e;
        }
        finally {
            IOUtils.closeQuietly((InputStream)inputStream);
        }
    }

    public String performGetRequest(URL url) throws IOException {
        Validate.notNull((Object)url);
        HttpURLConnection connection = this.createUrlConnection(url);
        LOGGER.debug("Reading data from " + url);
        InputStream inputStream = null;
        try {
            String string;
            inputStream = connection.getInputStream();
            String result = IOUtils.toString((InputStream)inputStream, (Charset)Charsets.UTF_8);
            LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
            LOGGER.debug("Response: " + result);
            String string2 = string = result;
            return string2;
        }
        catch (IOException e) {
            IOUtils.closeQuietly((InputStream)inputStream);
            inputStream = connection.getErrorStream();
            if (inputStream != null) {
                String string;
                LOGGER.debug("Reading error page from " + url);
                String result = IOUtils.toString((InputStream)inputStream, (Charset)Charsets.UTF_8);
                LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
                LOGGER.debug("Response: " + result);
                String string3 = string = result;
                return string3;
            }
            LOGGER.debug("Request failed", (Throwable)e);
            throw e;
        }
        finally {
            IOUtils.closeQuietly((InputStream)inputStream);
        }
    }

    public static URL constantURL(String url) {
        try {
            return new URL(url);
        }
        catch (MalformedURLException ex) {
            throw new Error("Couldn't create constant for " + url, ex);
        }
    }

    public static String buildQuery(Map<String, Object> query) {
        if (query == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : query.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            try {
                builder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            }
            catch (UnsupportedEncodingException e) {
                LOGGER.error("Unexpected exception building query", (Throwable)e);
            }
            if (entry.getValue() == null) continue;
            builder.append('=');
            try {
                builder.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
            }
            catch (UnsupportedEncodingException e) {
                LOGGER.error("Unexpected exception building query", (Throwable)e);
            }
        }
        return builder.toString();
    }

    public static URL concatenateURL(URL url, String query) {
        try {
            if (url.getQuery() != null && url.getQuery().length() > 0) {
                return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "&" + query);
            }
            return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "?" + query);
        }
        catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Could not concatenate given URL with GET arguments!", ex);
        }
    }
}

