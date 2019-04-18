package org.openl.rules.webstudio.web.admin;

import org.openl.config.ConfigurationManager;
import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.RepositoryMode;
import org.openl.util.StringUtils;

public abstract class RepositorySettings {
    public static final String VERSION_IN_DEPLOYMENT_NAME = "version-in-deployment-name";
    private final String USE_CUSTOM_COMMENTS;
    private final String COMMENT_VALIDATION_PATTERN;
    private final String INVALID_COMMENT_MESSAGE;
    private final String COMMENT_TEMPLATE;
    private final String DEFAULT_COMMENT_SAVE;
    private final String DEFAULT_COMMENT_CREATE;
    private final String DEFAULT_COMMENT_ARCHIVE;
    private final String DEFAULT_COMMENT_RESTORE;
    private final String DEFAULT_COMMENT_ERASE;
    private final String DEFAULT_COMMENT_COPIED_FROM;
    private final String DEFAULT_COMMENT_RESTORED_FROM;

    private boolean includeVersionInDeploymentName;
    private String commentValidationPattern;
    private String invalidCommentMessage;
    private String commentTemplate;
    private String defaultCommentSave;
    private String defaultCommentCreate;
    private String defaultCommentArchive;
    private String defaultCommentRestore;
    private String defaultCommentErase;
    private String defaultCommentCopiedFrom;
    private String defaultCommentRestoredFrom;

    private boolean useCustomComments;

    RepositorySettings(ConfigurationManager configManager, String configPrefix) {
        USE_CUSTOM_COMMENTS = configPrefix + "comment-template.use-custom-comments";
        COMMENT_VALIDATION_PATTERN = configPrefix + "comment-validation-pattern";
        INVALID_COMMENT_MESSAGE = configPrefix + "invalid-comment-message";
        COMMENT_TEMPLATE = configPrefix + "comment-template";
        DEFAULT_COMMENT_SAVE = configPrefix + "comment-template.user-message.default.save";
        DEFAULT_COMMENT_CREATE = configPrefix + "comment-template.user-message.default.create";
        DEFAULT_COMMENT_ARCHIVE = configPrefix + "comment-template.user-message.default.archive";
        DEFAULT_COMMENT_RESTORE = configPrefix + "comment-template.user-message.default.restore";
        DEFAULT_COMMENT_ERASE = configPrefix + "comment-template.user-message.default.erase";
        DEFAULT_COMMENT_COPIED_FROM = configPrefix + "comment-template.user-message.default.copied-from";
        DEFAULT_COMMENT_RESTORED_FROM = configPrefix + "comment-template.user-message.default.restored-from";

        load(configManager);
    }

    public boolean isIncludeVersionInDeploymentName() {
        return includeVersionInDeploymentName;
    }

    public void setIncludeVersionInDeploymentName(boolean includeVersionInDeploymentName) {
        this.includeVersionInDeploymentName = includeVersionInDeploymentName;
    }

    public String getCommentValidationPattern() {
        return commentValidationPattern;
    }

    public void setCommentValidationPattern(String commentValidationPattern) {
        this.commentValidationPattern = commentValidationPattern;
    }

    public String getInvalidCommentMessage() {
        return invalidCommentMessage;
    }

    public void setInvalidCommentMessage(String invalidCommentMessage) {
        this.invalidCommentMessage = invalidCommentMessage;
    }

    public String getCommentTemplate() {
        return commentTemplate;
    }

    public void setCommentTemplate(String commentTemplate) {
        this.commentTemplate = commentTemplate;
    }

    public String getDefaultCommentSave() {
        return defaultCommentSave;
    }

    public void setDefaultCommentSave(String defaultCommentSave) {
        this.defaultCommentSave = defaultCommentSave;
    }

    public boolean isUseCustomComments() {
        return useCustomComments;
    }

    public void setUseCustomComments(boolean useCustomComments) {
        this.useCustomComments = useCustomComments;
    }

    public String getDefaultCommentCreate() {
        return defaultCommentCreate;
    }

    public void setDefaultCommentCreate(String defaultCommentCreate) {
        this.defaultCommentCreate = defaultCommentCreate;
    }

    public String getDefaultCommentArchive() {
        return defaultCommentArchive;
    }

    public void setDefaultCommentArchive(String defaultCommentArchive) {
        this.defaultCommentArchive = defaultCommentArchive;
    }

    public String getDefaultCommentRestore() {
        return defaultCommentRestore;
    }

    public void setDefaultCommentRestore(String defaultCommentRestore) {
        this.defaultCommentRestore = defaultCommentRestore;
    }

    public String getDefaultCommentErase() {
        return defaultCommentErase;
    }

    public void setDefaultCommentErase(String defaultCommentErase) {
        this.defaultCommentErase = defaultCommentErase;
    }

    public String getDefaultCommentCopiedFrom() {
        return defaultCommentCopiedFrom;
    }

    public void setDefaultCommentCopiedFrom(String defaultCommentCopiedFrom) {
        this.defaultCommentCopiedFrom = defaultCommentCopiedFrom;
    }

    public String getDefaultCommentRestoredFrom() {
        return defaultCommentRestoredFrom;
    }

    public void setDefaultCommentRestoredFrom(String defaultCommentRestoredFrom) {
        this.defaultCommentRestoredFrom = defaultCommentRestoredFrom;
    }

    private void load(ConfigurationManager configManager) {
        includeVersionInDeploymentName = Boolean.valueOf(configManager.getStringProperty(VERSION_IN_DEPLOYMENT_NAME));

        useCustomComments = Boolean.valueOf(configManager.getStringProperty(USE_CUSTOM_COMMENTS));
        commentValidationPattern = configManager.getStringProperty(COMMENT_VALIDATION_PATTERN);
        invalidCommentMessage = configManager.getStringProperty(INVALID_COMMENT_MESSAGE);
        commentTemplate = configManager.getStringProperty(COMMENT_TEMPLATE);
        defaultCommentSave = configManager.getStringProperty(DEFAULT_COMMENT_SAVE);
        defaultCommentCreate = configManager.getStringProperty(DEFAULT_COMMENT_CREATE);
        defaultCommentArchive = configManager.getStringProperty(DEFAULT_COMMENT_ARCHIVE);
        defaultCommentRestore = configManager.getStringProperty(DEFAULT_COMMENT_RESTORE);
        defaultCommentErase = configManager.getStringProperty(DEFAULT_COMMENT_ERASE);
        defaultCommentCopiedFrom = configManager.getStringProperty(DEFAULT_COMMENT_COPIED_FROM);
        defaultCommentRestoredFrom = configManager.getStringProperty(DEFAULT_COMMENT_RESTORED_FROM);
    }

    protected void fixState() {
    }

    protected void store(PropertiesHolder propertiesHolder) {
        propertiesHolder.setProperty(VERSION_IN_DEPLOYMENT_NAME, includeVersionInDeploymentName);

        propertiesHolder.setProperty(USE_CUSTOM_COMMENTS, useCustomComments);
        propertiesHolder.setProperty(COMMENT_VALIDATION_PATTERN, commentValidationPattern);
        propertiesHolder.setProperty(INVALID_COMMENT_MESSAGE, invalidCommentMessage);

        propertiesHolder.setProperty(COMMENT_TEMPLATE, commentTemplate);
        propertiesHolder.setProperty(DEFAULT_COMMENT_SAVE, defaultCommentSave);
        propertiesHolder.setProperty(DEFAULT_COMMENT_CREATE, defaultCommentCreate);
        propertiesHolder.setProperty(DEFAULT_COMMENT_ARCHIVE, defaultCommentArchive);
        propertiesHolder.setProperty(DEFAULT_COMMENT_RESTORE, defaultCommentRestore);
        propertiesHolder.setProperty(DEFAULT_COMMENT_ERASE, defaultCommentErase);
        propertiesHolder.setProperty(DEFAULT_COMMENT_COPIED_FROM, defaultCommentCopiedFrom);
        propertiesHolder.setProperty(DEFAULT_COMMENT_RESTORED_FROM, defaultCommentRestoredFrom);
    }

    protected void revert(ConfigurationManager configurationManager) {
        configurationManager.removeProperties(
                VERSION_IN_DEPLOYMENT_NAME,
                USE_CUSTOM_COMMENTS,
                COMMENT_VALIDATION_PATTERN,
                INVALID_COMMENT_MESSAGE,
                COMMENT_TEMPLATE,
                DEFAULT_COMMENT_SAVE,
                DEFAULT_COMMENT_CREATE,
                DEFAULT_COMMENT_ARCHIVE,
                DEFAULT_COMMENT_RESTORE,
                DEFAULT_COMMENT_ERASE,
                DEFAULT_COMMENT_COPIED_FROM,
                DEFAULT_COMMENT_RESTORED_FROM
        );
        load(configurationManager);
    }

    static String getTypePrefix(RepositoryMode repositoryMode) {
        String type;
        switch (repositoryMode) {
            case DESIGN:
                type = "design";
                break;
            case DEPLOY_CONFIG:
                type = "deploy-config";
                break;
            case PRODUCTION:
                type = "deployment";
                break;
            default:
                throw new UnsupportedOperationException();
        }

        return type;
    }

    protected void onTypeChanged(RepositoryType newRepositoryType) {
    }

    public void copyContent(RepositorySettings other) {
        setIncludeVersionInDeploymentName(other.isIncludeVersionInDeploymentName());
        setUseCustomComments(other.isUseCustomComments());
        setCommentValidationPattern(other.getCommentValidationPattern());
        setInvalidCommentMessage(other.getInvalidCommentMessage());
        setDefaultCommentCreate(other.getDefaultCommentCreate());
        setDefaultCommentArchive(other.getDefaultCommentArchive());
        setDefaultCommentRestore(other.getDefaultCommentRestore());
        setDefaultCommentErase(other.getDefaultCommentErase());
        setDefaultCommentCopiedFrom(other.getDefaultCommentCopiedFrom());
        setDefaultCommentRestoredFrom(other.getDefaultCommentRestoredFrom());
    }

    public RepositorySettingsValidators getValidators() {
        return new RepositorySettingsValidators();
    }
}
