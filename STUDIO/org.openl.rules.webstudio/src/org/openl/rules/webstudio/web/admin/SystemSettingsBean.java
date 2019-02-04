package org.openl.rules.webstudio.web.admin;

import static org.openl.rules.webstudio.web.admin.AdministrationSettings.*;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.webstudio.web.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.webstudio.filter.ReloadableDelegatingFilter;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.ProductionRepositoriesTreeController;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Remove property getters/setters when migrating to EL 2.2
 *
 * @author Andrei Astrouski
 */
@ManagedBean
@ViewScoped
public class SystemSettingsBean {
    private final Logger log = LoggerFactory.getLogger(SystemSettingsBean.class);

    @ManagedProperty(value = "#{productionRepositoriesTreeController}")
    private ProductionRepositoriesTreeController productionRepositoriesTreeController;

    @ManagedProperty(value = "#{productionRepositoryFactoryProxy}")
    private ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;

    @ManagedProperty(value = "#{productionRepositoryConfigManagerFactory}")
    private ConfigurationManagerFactory productionConfigManagerFactory;

    @ManagedProperty(value = "#{deploymentManager}")
    private DeploymentManager deploymentManager;

    @ManagedProperty(value = "#{designTimeRepository}")
    private DesignTimeRepository designTimeRepository;

    private ConfigurationManager configManager;
    private RepositoryConfiguration designRepositoryConfiguration;
    private RepositoryConfiguration deployConfigRepositoryConfiguration;

    private ProductionRepositoryEditor productionRepositoryEditor;
    private SystemSettingsValidator validator;

    @PostConstruct
    public void afterPropertiesSet() {
        configManager = WebStudioUtils.getWebStudio(true).getSystemConfigManager();

        designRepositoryConfiguration = new RepositoryConfiguration("", configManager, RepositoryMode.DESIGN);

        deployConfigRepositoryConfiguration = new RepositoryConfiguration("", configManager, RepositoryMode.DEPLOY_CONFIG);

        productionRepositoryEditor = new ProductionRepositoryEditor(configManager,
                productionConfigManagerFactory,
                productionRepositoryFactoryProxy);

        validator = new SystemSettingsValidator(this);
    }

    public String getUserWorkspaceHome() {
        return configManager.getStringProperty(USER_WORKSPACE_HOME);
    }

    public void setUserWorkspaceHome(String userWorkspaceHome) {
        configManager.setProperty(USER_WORKSPACE_HOME, userWorkspaceHome);
    }

    public String getDatePattern() {
        return configManager.getStringProperty(DATE_PATTERN);
    }

    public void setDatePattern(String datePattern) {
        configManager.setProperty(DATE_PATTERN, datePattern);
    }

    public boolean isUpdateSystemProperties() {
        return configManager.getBooleanProperty(UPDATE_SYSTEM_PROPERTIES);
    }

    public void setUpdateSystemProperties(boolean updateSystemProperties) {
        configManager.setProperty(UPDATE_SYSTEM_PROPERTIES, updateSystemProperties);
    }

    public String getProjectHistoryHome() {
        return configManager.getStringProperty(PROJECT_HISTORY_HOME);
    }

    public void setProjectHistoryHome(String projectHistoryHome) {
        configManager.setProperty(PROJECT_HISTORY_HOME, projectHistoryHome);
    }

    public String getProjectHistoryCount() {
        if (isUnlimitHistory()) {
            return "0";
        } else {
            return Integer.toString(configManager.getIntegerProperty(PROJECT_HISTORY_COUNT));
        }
    }

    public void setProjectHistoryCount(String count) {
        configManager.setProperty(PROJECT_HISTORY_COUNT, Integer.parseInt(count));
    }

    public boolean isUnlimitHistory() {
        return configManager.getBooleanProperty(PROJECT_HISTORY_UNLIMITED);
    }

    public void setUnlimitHistory(boolean unlimited) {
        configManager.setProperty(PROJECT_HISTORY_UNLIMITED, unlimited);
    }

    public RepositoryConfiguration getDesignRepositoryConfiguration() {
        return designRepositoryConfiguration;
    }

    public RepositoryConfiguration getDeployConfigRepositoryConfiguration() {
        return deployConfigRepositoryConfiguration;
    }

    public boolean isUseDesignRepo() {
        return !Boolean.parseBoolean(configManager.getStringProperty(DesignTimeRepositoryImpl.USE_SEPARATE_DEPLOY_CONFIG_REPO));
    }

    public void setUseDesignRepo(boolean useDesignRepo) {
        configManager.setProperty(DesignTimeRepositoryImpl.USE_SEPARATE_DEPLOY_CONFIG_REPO, !useDesignRepo);
    }

    public List<RepositoryConfiguration> getProductionRepositoryConfigurations() {
        return productionRepositoryEditor.getProductionRepositoryConfigurations();
    }

    public void setDispatchingValidationEnabled(boolean dispatchingValidationEnabled) {
        configManager.setProperty(OpenLSystemProperties.DISPATCHING_VALIDATION, dispatchingValidationEnabled);
    }
    
    public boolean isDispatchingValidationEnabled(){
        return OpenLSystemProperties.isDispatchingValidationEnabled(configManager.getProperties());
    }
    
    public boolean isRunTestsInParallel() {
        return OpenLSystemProperties.isRunTestsInParallel(configManager.getProperties());
    }

    public void setRunTestsInParallel(boolean runTestsInParallel) {
        configManager.setProperty(OpenLSystemProperties.RUN_TESTS_IN_PARALLEL, runTestsInParallel);
    }

    public String getTestRunThreadCount() {
        return Integer.toString(OpenLSystemProperties.getTestRunThreadCount(configManager.getProperties()));
    }

    public void setTestRunThreadCount(String testRunThreadCount) {
        configManager.setProperty(OpenLSystemProperties.TEST_RUN_THREAD_COUNT_PROPERTY, Integer.parseInt(StringUtils.trim(testRunThreadCount)));
    }

    public boolean isAutoCompile() {
        return OpenLSystemProperties.isAutoCompile(configManager.getProperties());
    }

    public void setAutoCompile(boolean autoCompile) {
        configManager.setProperty(OpenLSystemProperties.AUTO_COMPILE, autoCompile);
    }

    public void applyChanges() {
        try {
            RepositoryValidators.validate(designRepositoryConfiguration);
            RepositoryValidators.validateConnectionForDesignRepository(designRepositoryConfiguration, designTimeRepository,
                    RepositoryMode.DESIGN);

            if (!isUseDesignRepo()) {
                RepositoryValidators.validate(deployConfigRepositoryConfiguration);
                RepositoryValidators.validateConnectionForDesignRepository(deployConfigRepositoryConfiguration,
                        designTimeRepository,
                        RepositoryMode.DEPLOY_CONFIG);
            }

            productionRepositoryEditor.validate();
            productionRepositoryEditor.save(new ProductionRepositoryEditor.Callback() {
                @Override public void onDelete(String configName) {
                    deploymentManager.removeRepository(configName);
                }

                @Override public void onRename(String oldConfigName, String newConfigName) {
                    deploymentManager.removeRepository(oldConfigName);
                    deploymentManager.addRepository(newConfigName);
                }
            });

            saveSystemConfig();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            FacesUtils.addErrorMessage(e.getMessage());
        }
    }

    private void saveSystemConfig() {
        // TODO: This line also do configManager.save() implicitly
        boolean saved = designRepositoryConfiguration.save();
        if (!isUseDesignRepo()) {
            saved &= deployConfigRepositoryConfiguration.save();
        }

        if (saved) {
            refreshConfig();
        }
    }

    public void restoreDefaults() {
        productionRepositoryEditor.revertChanges();

        // We cannot invoke configManager.restoreDefaults(): in this case some 
        // settings (such as user.mode etc) not edited in this page
        // will be reverted too. We should revert only settings edited in Administration page
        for (String setting : AdministrationSettings.getAllSettings()) {
            configManager.removeProperty(setting);
        }
        saveSystemConfig();

        productionRepositoryEditor.reload();
    }

    public void setProductionConfigManagerFactory(ConfigurationManagerFactory productionConfigManagerFactory) {
        this.productionConfigManagerFactory = productionConfigManagerFactory;
    }

    public void setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    public void setDesignTimeRepository(DesignTimeRepository designTimeRepository) {
        this.designTimeRepository = designTimeRepository;
    }

    public void deleteProductionRepository(String configName) {
        try {
            productionRepositoryEditor.deleteProductionRepository(configName, new ProductionRepositoryEditor.Callback() {
                @Override public void onDelete(String configName) {
                    /* Delete Production repo from tree */
                    productionRepositoriesTreeController.deleteProdRepo(configName);
                }
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            FacesUtils.addErrorMessage(e.getMessage());
        }
    }

    public SystemSettingsValidator getValidator() {
        return validator;
    }

    public void setProductionRepositoriesTreeController(
            ProductionRepositoriesTreeController productionRepositoriesTreeController) {
        this.productionRepositoriesTreeController = productionRepositoriesTreeController;
    }

    public void setProductionRepositoryFactoryProxy(ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy) {
        this.productionRepositoryFactoryProxy = productionRepositoryFactoryProxy;
    }

    private void refreshConfig() {
        WebStudioUtils.getWebStudio().setNeedRestart(true);
        ReloadableDelegatingFilter.reloadApplicationContext(FacesUtils.getServletContext());
    }

}
