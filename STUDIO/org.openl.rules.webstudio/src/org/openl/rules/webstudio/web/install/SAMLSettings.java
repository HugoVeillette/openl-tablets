package org.openl.rules.webstudio.web.install;

public class SAMLSettings {
    private String webStudioUrl;
    private String samlServerMetadataUrl;
    private int requestTimeout;
    private String keystoreFilePath;
    private String keystorePassword;
    private String keystoreSpAlias;
    private String keystoreSpPassword;
    private String defaultGroup;
    private String usernameAttribute;
    private String firstNameAttribute;
    private String secondNameAttribute;
    private String groupsAttribute;
    private String authenticationContexts;
    private boolean localLogout;
    private String samlScheme;
    private String samlServerName;
    private int serverPort;
    private boolean includeServerPortInRequestUrl;
    private String contextPath;
    private int maxAuthenticationAge;
    private boolean metadataTrustCheck;
    private boolean isAppAfterBalancer;

    public SAMLSettings(String webStudioUrl,
            String samlServerMetadataUrl,
            int requestTimeout,
            String keystoreFilePath,
            String keystorePassword,
            String keystoreSpAlias,
            String keystoreSpPassword,
            String defaultGroup,
            String usernameAttribute,
            String firstNameAttribute,
            String secondNameAttribute,
            String groupsAttribute,
            String authenticationContexts,
            boolean localLogout,
            String samlScheme,
            String samlServerName,
            int serverPort,
            boolean includeServerPortInRequestUrl,
            String contextPath,
            int maxAuthenticationAge,
            boolean metadataTrustCheck,
            boolean isAppAfterBalancer) {
        this.webStudioUrl = webStudioUrl;
        this.samlServerMetadataUrl = samlServerMetadataUrl;
        this.requestTimeout = requestTimeout;
        this.keystoreFilePath = keystoreFilePath;
        this.keystoreSpAlias = keystoreSpAlias;
        this.keystorePassword = keystorePassword;
        this.keystoreSpPassword = keystoreSpPassword;
        this.defaultGroup = defaultGroup;
        this.usernameAttribute = usernameAttribute;
        this.firstNameAttribute = firstNameAttribute;
        this.secondNameAttribute = secondNameAttribute;
        this.groupsAttribute = groupsAttribute;
        this.authenticationContexts = authenticationContexts;
        this.localLogout = localLogout;
        this.samlScheme = samlScheme;
        this.samlServerName = samlServerName;
        this.serverPort = serverPort;
        this.includeServerPortInRequestUrl = includeServerPortInRequestUrl;
        this.contextPath = contextPath;
        this.maxAuthenticationAge = maxAuthenticationAge;
        this.metadataTrustCheck = metadataTrustCheck;
        this.isAppAfterBalancer = isAppAfterBalancer;
    }

    public String getWebStudioUrl() {
        return webStudioUrl;
    }

    public void setWebStudioUrl(String webStudioUrl) {
        this.webStudioUrl = webStudioUrl;
    }

    public String getSamlServerMetadataUrl() {
        return samlServerMetadataUrl;
    }

    public void setSamlServerMetadataUrl(String samlServerMetadataUrl) {
        this.samlServerMetadataUrl = samlServerMetadataUrl;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public String getKeystoreFilePath() {
        return keystoreFilePath;
    }

    public void setKeystoreFilePath(String keystoreFilePath) {
        if (keystoreFilePath == null || keystoreFilePath.startsWith("classpath:") || keystoreFilePath
            .startsWith("file:") || keystoreFilePath.startsWith("url:")) {
            this.keystoreFilePath = keystoreFilePath;
        } else {
            this.keystoreFilePath = "file:" + keystoreFilePath;
        }
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getKeystoreSpAlias() {
        return keystoreSpAlias;
    }

    public void setKeystoreSpAlias(String keystoreSpAlias) {
        this.keystoreSpAlias = keystoreSpAlias;
    }

    public String getKeystoreSpPassword() {
        return keystoreSpPassword;
    }

    public void setKeystoreSpPassword(String keystoreSpPassword) {
        this.keystoreSpPassword = keystoreSpPassword;
    }

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public String getUsernameAttribute() {
        return usernameAttribute;
    }

    public void setUsernameAttribute(String usernameAttribute) {
        this.usernameAttribute = usernameAttribute;
    }

    public String getFirstNameAttribute() {
        return firstNameAttribute;
    }

    public void setFirstNameAttribute(String firstNameAttribute) {
        this.firstNameAttribute = firstNameAttribute;
    }

    public String getSecondNameAttribute() {
        return secondNameAttribute;
    }

    public void setSecondNameAttribute(String secondNameAttribute) {
        this.secondNameAttribute = secondNameAttribute;
    }

    public String getGroupsAttribute() {
        return groupsAttribute;
    }

    public void setGroupsAttribute(String groupsAttribute) {
        this.groupsAttribute = groupsAttribute;
    }

    public String getAuthenticationContexts() {
        return authenticationContexts;
    }

    public void setAuthenticationContexts(String authenticationContexts) {
        this.authenticationContexts = authenticationContexts;
    }

    public boolean isLocalLogout() {
        return localLogout;
    }

    public void setLocalLogout(boolean localLogout) {
        this.localLogout = localLogout;
    }

    public String getSamlScheme() {
        return samlScheme;
    }

    public void setSamlScheme(String samlScheme) {
        this.samlScheme = samlScheme;
    }

    public String getSamlServerName() {
        return samlServerName;
    }

    public void setSamlServerName(String samlServerName) {
        this.samlServerName = samlServerName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public int getMaxAuthenticationAge() {
        return maxAuthenticationAge;
    }

    public void setMaxAuthenticationAge(int maxAuthenticationAge) {
        this.maxAuthenticationAge = maxAuthenticationAge;
    }

    public boolean isMetadataTrustCheck() {
        return metadataTrustCheck;
    }

    public void setMetadataTrustCheck(boolean metadataTrustCheck) {
        this.metadataTrustCheck = metadataTrustCheck;
    }

    public boolean isIncludeServerPortInRequestUrl() {
        return includeServerPortInRequestUrl;
    }

    public void setIncludeServerPortInRequestUrl(boolean includeServerPortInRequestUrl) {
        this.includeServerPortInRequestUrl = includeServerPortInRequestUrl;
    }

    public boolean isAppAfterBalancer() {
        return isAppAfterBalancer;
    }

    public void setAppAfterBalancer(boolean appAfterBalancer) {
        isAppAfterBalancer = appAfterBalancer;
    }
}
