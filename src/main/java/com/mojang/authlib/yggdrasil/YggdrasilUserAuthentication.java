/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Multimap
 *  net.lenni0451.commons.httpclient.HttpClient
 *  net.raphimc.minecraftauth.MinecraftAuth
 *  net.raphimc.minecraftauth.step.AbstractStep$InitialInput
 *  net.raphimc.minecraftauth.step.java.session.StepFullJavaSession$FullJavaSession
 *  net.raphimc.minecraftauth.step.msa.StepCredentialsMsaCode$MsaCredentials
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.mojang.authlib.yggdrasil;

import com.google.common.collect.Multimap;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.HttpUserAuthentication;
import com.mojang.authlib.UserType;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.request.ValidateRequest;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.authlib.yggdrasil.response.User;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepCredentialsMsaCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YggdrasilUserAuthentication
extends HttpUserAuthentication {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String BASE_URL = "https://authserver.mojang.com/";
    private static final URL ROUTE_AUTHENTICATE = HttpAuthenticationService.constantURL("https://authserver.mojang.com/authenticate");
    private static final URL ROUTE_REFRESH = HttpAuthenticationService.constantURL("https://authserver.mojang.com/refresh");
    private static final URL ROUTE_VALIDATE = HttpAuthenticationService.constantURL("https://authserver.mojang.com/validate");
    private static final URL ROUTE_INVALIDATE = HttpAuthenticationService.constantURL("https://authserver.mojang.com/invalidate");
    private static final URL ROUTE_SIGNOUT = HttpAuthenticationService.constantURL("https://authserver.mojang.com/signout");
    private static final String STORAGE_KEY_ACCESS_TOKEN = "accessToken";
    private final Agent agent;
    private GameProfile[] profiles;
    private String accessToken;
    private boolean isOnline;

    public YggdrasilUserAuthentication(YggdrasilAuthenticationService authenticationService, Agent agent) {
        super(authenticationService);
        this.agent = agent;
    }

    @Override
    public boolean canLogIn() {
        return !this.canPlayOnline() && StringUtils.isNotBlank((CharSequence)this.getUsername()) && (StringUtils.isNotBlank((CharSequence)this.getPassword()) || StringUtils.isNotBlank((CharSequence)this.getAuthenticatedToken()));
    }

    @Override
    public void logIn() throws AuthenticationException {
        if (StringUtils.isBlank((CharSequence)this.getUsername())) {
            throw new InvalidCredentialsException("Invalid username");
        }
        if (StringUtils.isNotBlank((CharSequence)this.getAuthenticatedToken())) {
            this.logInWithToken();
        } else if (StringUtils.isNotBlank((CharSequence)this.getPassword())) {
            this.logInWithPassword();
        } else {
            this.setUserid(this.getUsername());
            this.isOnline = false;
            this.getModifiableUserProperties().clear();
        }
    }

    protected void logInWithPassword() throws AuthenticationException {
        if (StringUtils.isBlank((CharSequence)this.getUsername())) {
            throw new InvalidCredentialsException("Invalid username");
        }
        if (StringUtils.isBlank((CharSequence)this.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }
        LOGGER.info("Logging in with username & password");
        HttpClient httpClient = MinecraftAuth.createHttpClient();
        StepFullJavaSession.FullJavaSession javaSession = null;
        try {
            javaSession = (StepFullJavaSession.FullJavaSession)MinecraftAuth.JAVA_CREDENTIALS_LOGIN.getFromInput(httpClient, (AbstractStep.InitialInput)new StepCredentialsMsaCode.MsaCredentials(this.getUsername(), this.getPassword()));
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new InvalidCredentialsException("Invalid password");
        }
        System.out.println("Username: " + javaSession.getMcProfile().getName());
        System.out.println("Access token: " + javaSession.getMcProfile().getMcToken().getAccessToken());
        System.out.println("Player certificates: " + javaSession.getPlayerCertificates());
        this.setUserType(UserType.MOJANG);
        this.setUserid(javaSession.getMcProfile().getName());
        this.isOnline = true;
        this.accessToken = javaSession.getMcProfile().getMcToken().getAccessToken();
        GameProfile profile = new GameProfile(javaSession.getMcProfile().getId(), javaSession.getMcProfile().getName());
        this.profiles = new GameProfile[]{profile};
        this.setSelectedProfile(profile);
        this.getModifiableUserProperties().clear();
    }

    protected void updateUserProperties(User user) {
        if (user == null) {
            return;
        }
        if (user.getProperties() != null) {
            this.getModifiableUserProperties().putAll((Multimap)user.getProperties());
        }
    }

    protected void logInWithToken() throws AuthenticationException {
        if (StringUtils.isBlank((CharSequence)this.getUserID())) {
            if (StringUtils.isBlank((CharSequence)this.getUsername())) {
                this.setUserid(this.getUsername());
            } else {
                throw new InvalidCredentialsException("Invalid uuid & username");
            }
        }
        if (StringUtils.isBlank((CharSequence)this.getAuthenticatedToken())) {
            throw new InvalidCredentialsException("Invalid access token");
        }
        LOGGER.info("Logging in with access token");
        this.setUserid(this.getUsername());
        this.isOnline = true;
        this.getModifiableUserProperties().clear();
    }

    protected boolean checkTokenValidity() throws AuthenticationException {
        ValidateRequest request = new ValidateRequest(this);
        try {
            this.getAuthenticationService().makeRequest(ROUTE_VALIDATE, request, Response.class);
            return true;
        }
        catch (AuthenticationException ex) {
            return false;
        }
    }

    @Override
    public void logOut() {
        super.logOut();
        this.accessToken = null;
        this.profiles = null;
        this.isOnline = false;
    }

    @Override
    public GameProfile[] getAvailableProfiles() {
        return this.profiles;
    }

    @Override
    public boolean isLoggedIn() {
        return StringUtils.isNotBlank((CharSequence)this.accessToken);
    }

    @Override
    public boolean canPlayOnline() {
        return this.isLoggedIn() && this.getSelectedProfile() != null && this.isOnline;
    }

    @Override
    public void selectGameProfile(GameProfile profile) throws AuthenticationException {
        this.isOnline = false;
        this.setSelectedProfile(profile);
    }

    @Override
    public void loadFromStorage(Map<String, Object> credentials) {
        super.loadFromStorage(credentials);
        this.accessToken = String.valueOf(credentials.get(STORAGE_KEY_ACCESS_TOKEN));
    }

    @Override
    public Map<String, Object> saveForStorage() {
        Map<String, Object> result = super.saveForStorage();
        if (StringUtils.isNotBlank((CharSequence)this.getAuthenticatedToken())) {
            result.put(STORAGE_KEY_ACCESS_TOKEN, this.getAuthenticatedToken());
        }
        return result;
    }

    @Deprecated
    public String getSessionToken() {
        if (this.isLoggedIn() && this.getSelectedProfile() != null && this.canPlayOnline()) {
            return String.format("token:%s:%s", this.getAuthenticatedToken(), this.getSelectedProfile().getId());
        }
        return null;
    }

    @Override
    public String getAuthenticatedToken() {
        return this.accessToken;
    }

    public Agent getAgent() {
        return this.agent;
    }

    @Override
    public String toString() {
        return "YggdrasilAuthenticationService{agent=" + this.agent + ", profiles=" + Arrays.toString(this.profiles) + ", selectedProfile=" + this.getSelectedProfile() + ", username='" + this.getUsername() + '\'' + ", isLoggedIn=" + this.isLoggedIn() + ", userType=" + (Object)((Object)this.getUserType()) + ", canPlayOnline=" + this.canPlayOnline() + ", accessToken='" + this.accessToken + '\'' + ", clientToken='" + this.getAuthenticationService().getClientToken() + '\'' + '}';
    }

    @Override
    public YggdrasilAuthenticationService getAuthenticationService() {
        return (YggdrasilAuthenticationService)super.getAuthenticationService();
    }
}

