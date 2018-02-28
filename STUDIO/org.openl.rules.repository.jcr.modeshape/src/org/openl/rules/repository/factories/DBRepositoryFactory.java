package org.openl.rules.repository.factories;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import javax.naming.NamingException;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.loaders.jdbc.configuration.JdbcStringBasedCacheStoreConfigurationBuilder;
import org.infinispan.schematic.document.ParsingException;
import org.infinispan.transaction.TransactionMode;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.JcrNodeTypeManager;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.LocalEnvironment;
import org.modeshape.jcr.RepositoryConfiguration;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.util.db.JDBCDriverRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataBase-based Repository Factory. Creates an JCR repository in the DB.
 *
 * @author Yury Molchan
 */
abstract class DBRepositoryFactory extends AbstractJcrRepositoryFactory {
    private final Logger log = LoggerFactory.getLogger(DBRepositoryFactory.class);

    private static final String OPENL_JCR_REPO_ID_KEY = "openl-jcr-repo-id";

    private long initializeTime = 15000;
    private long finalizeTime = 10000;

    public void setInitializeTime(long initializeTime) {
        this.initializeTime = initializeTime;
    }

    public void setFinalizeTime(long finalizeTime) {
        this.finalizeTime = finalizeTime;
    }

    /**
     * Jackrabbit local repository
     */
    private ModeshapeJcrRepo repo;

    // ------ private methods ------

    /**
     * Starts modeshape JCR repository over DataBase. If there was no repository it will be created automatically.
     */
    private void init() throws Exception {
        JDBCDriverRegister.registerDrivers();

        String dbUrl = uri;
        String user = login;
        String pwd = password;

        log.info("Checking a connection to DB [{}]", dbUrl);
        Connection conn;
        conn = createConnection(dbUrl, user, pwd);

        DatabaseMetaData metaData = conn.getMetaData();
        String databaseName = metaData.getDatabaseProductName().toLowerCase().replace(" ", "_");
        CompositeConfiguration properties = getConfiguration(databaseName);
        Case namesCase = getCase(metaData);

        log.info("Preparing a repository...");
        initTable(conn, properties);
        String repoID = getRepoID(conn, properties);

        try {
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
        } catch (Exception e) {
            log.info("Failed to commit changes!", e);
        }

        conn.close();
        log.info("The repository for ID=[{}] has been prepared", repoID);

        RepositoryConfiguration config = getModeshapeConfiguration(dbUrl, user, pwd, repoID, properties, namesCase);

        log.info("Checking ModeShape configuration...");
        repo = new ModeshapeJcrRepo(config);

        String repoName = config.getName();
        log.info("Starting ModeShape repository [{}]...", repoName);
        Problems repoProblems = repo.getStartupProblems();
        if (repoProblems.hasErrors()) {
            log.error("ModeShape repository ID=[{}] has errors: ", repoProblems);
        }
        log.info("ModeShape repository ID=[{}] has been started", repoName);

        log.info("Checking the repository...");
        setRepository(repo);
    }

    abstract Connection createConnection(String dbUrl, String user, String pwd);

    private RepositoryConfiguration getModeshapeConfiguration(String url,
            String user,
            String password,
            final String repoName,
            CompositeConfiguration properties,
            Case namesCase) throws SQLException, ParsingException, FileNotFoundException, NamingException {
        // Create a local environment that we'll set up to own the external
        // components ModeShape needs ...
        LocalEnvironment environment = new LocalEnvironment() {
            @Override
            protected GlobalConfigurationBuilder createGlobalConfigurationBuilder() {
                GlobalConfigurationBuilder global = new GlobalConfigurationBuilder();
                global.globalJmxStatistics().enable().allowDuplicateDomains(true);
                global.transport().defaultTransport().clusterName(repoName);
                global.transport().addProperty("configurationFile", "openl-jgroups-mosh-config.xml");
                return global;
            }
        };

        // Infinispan cache declaration
        Configuration ispnConfig = getInfinispanConfiguration(url, user, password, properties, namesCase);
        String tableName = changeCase(namesCase, properties.getString("table.name"));
        environment.defineCache(tableName, ispnConfig);

        // Modeshape's configuration
        RepositoryConfiguration config = RepositoryConfiguration.read(
            "{'name':'" + repoName + "', 'jndiName':'', 'storage':{'cacheName':'" + tableName + "','binaryStorage':{'type':'cache','dataCacheName':'" + tableName + "','metadataCacheName':'" + tableName + "'}},'clustering':{'clusterName':'" + repoName + "', 'channelConfiguration':'openl-jgroups-insp-config.xml'}}");
        config = config.with(environment);

        // Verify the configuration for the repository ...
        Problems problems = config.validate();
        if (problems.hasErrors()) {
            String message = "Problems in the Modeshape configuration";
            log.error(message);
            log.error(problems.toString());
            throw new IllegalArgumentException(message);
        }
        return config;
    }

    private Configuration getInfinispanConfiguration(String url,
            String user,
            String password,
            CompositeConfiguration properties,
            Case namesCase) throws SQLException, NamingException {

        // Infinispan's configuration
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.transaction().transactionMode(TransactionMode.TRANSACTIONAL);
        JdbcStringBasedCacheStoreConfigurationBuilder jdbcBuilder = configurationBuilder.jmxStatistics()
            .enable()
            .clustering()
            .cacheMode(CacheMode.REPL_SYNC)
            .loaders()
            .shared(true)
            .addLoader(JdbcStringBasedCacheStoreConfigurationBuilder.class);

        buildDBConnection(jdbcBuilder, url, user, password);

        jdbcBuilder.table()
            .createOnStart(false)
            .tableNamePrefix(changeCase(namesCase, properties.getString("table.prefix")))
            .idColumnName(changeCase(namesCase, properties.getString("column.id.name")))
            .idColumnType(changeCase(namesCase, properties.getString("column.id.type")))
            .dataColumnName(changeCase(namesCase, properties.getString("column.data.name")))
            .timestampColumnName(changeCase(namesCase, properties.getString("column.time.name")));
        return configurationBuilder.build();
    }

    abstract void buildDBConnection(JdbcStringBasedCacheStoreConfigurationBuilder jdbcBuilder,
            String url,
            String user,
            String password);

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() throws RRepositoryException {
        try {
            init();
        } catch (Exception e) {
            try {
                close();
            } catch (Exception e1) {
                log.error(e1.getMessage(), e1);
            }
            throw new RRepositoryException("Failed to initialize DataBase: " + e.getMessage(), e);
        }
        log.info("Networking...");
        try {
            Thread.sleep(initializeTime);
        } catch (InterruptedException e) {
            log.info(e.getMessage(), e);
        }
        log.info("Configuring JCR session...");
        super.initialize();
        log.info("The repository has loaded");
    }

    @Override
    protected Session createSession() throws RepositoryException {
        return repository.login("default");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initNodeTypes(NodeTypeManager ntm) throws RepositoryException {
        JcrNodeTypeManager ntmi = (JcrNodeTypeManager) ntm;

        try {
            InputStream is = null;
            try {
                is = this.getClass().getResourceAsStream(DEFAULT_NODETYPE_FILE);
                ntmi.registerNodeTypes(is, true);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to init NodeTypes: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() throws IOException {
        log.info("Closing the connection to the repository...");
        try {
            Thread.sleep(finalizeTime);
        } catch (InterruptedException e) {
            log.info(e.getMessage(), e);
        }
        try {
            super.close();
        } finally {
            try {
                if (repo != null) {
                    repo.shutdown();
                    repo = null;
                }
            } catch (Exception e) {
                throw new IOException("Shutdown has failed.", e);
            }
        }
        log.info("The connection has been closed");
    }

    private void initTable(Connection conn, CompositeConfiguration properties) throws SQLException {
        String repoTable = getRepoTableName(properties);
        if (tableExists(conn, properties)) {
            log.info("Table '{}' already exists", repoTable);
            return;
        }
        createTable(conn, properties);
        if (tableExists(conn, properties)) {
            log.info("Table '{}' has been created", repoTable);
            return;
        }
        throw new IllegalStateException("Table '" + repoTable + "' has not created");
    }

    private boolean tableExists(Connection connection, CompositeConfiguration properties) {
        ResultSet rs = null;
        String repoTable = getRepoTableName(properties);
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            repoTable = changeCase(getCase(metaData), repoTable);
            if ("Oracle".equals(metaData.getDatabaseProductName())) {
                rs = metaData.getTables(null, metaData.getUserName(), repoTable, new String[] { "TABLE" });
            } else {
                rs = metaData.getTables(null, null, repoTable, new String[] { "TABLE" });
            }
            return rs.next();
        } catch (SQLException e) {
            log.debug("SQLException occurs while checking the table {}", repoTable, e);
            return false;
        } finally {
            safeClose(rs);
        }
    }

    private void createTable(Connection conn, CompositeConfiguration properties) throws SQLException {
        String sql = properties.getString("sql.create-table");
        log.info("The following SQL script being used [ {} ]", sql);
        Statement statement = null;
        try {
            statement = conn.createStatement();
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            log.warn("SQLException occurs while checking the table {}", getRepoTableName(properties), e);
        } finally {
            safeClose(statement);
        }
    }

    private String getRepoID(Connection conn, CompositeConfiguration properties) throws SQLException {
        String repoID = selectRepoID(conn, properties);

        if (repoID != null) {
            return repoID;
        }
        createRepoID(conn, properties);
        repoID = selectRepoID(conn, properties);
        if (repoID != null) {
            return repoID;
        }
        throw new IllegalStateException("The row with ID = '" + OPENL_JCR_REPO_ID_KEY + "' has not created");
    }

    private String selectRepoID(Connection conn, CompositeConfiguration properties) throws SQLException {
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = conn.prepareStatement(properties.getString("sql.select-id"));
            statement.setString(1, OPENL_JCR_REPO_ID_KEY);
            rs = statement.executeQuery();
            if (rs.next()) {
                InputStream binaryStream = rs.getBinaryStream(1);
                return IOUtils.toStringAndClose(binaryStream);
            } else {
                return null;
            }
        } catch (IOException e) {
            log.error("Unexpected IO failure", e);
            return null;
        } finally {
            safeClose(rs);
            safeClose(statement);
        }
    }

    private void createRepoID(Connection conn, CompositeConfiguration properties) throws SQLException {
        String repoId = "openl-jcr-repo-" + UUID.randomUUID().toString();
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(properties.getString("sql.insert-id"));
            statement.setString(1, OPENL_JCR_REPO_ID_KEY);
            statement.setBytes(2, StringUtils.toBytes(repoId));
            statement.executeUpdate();
        } finally {
            safeClose(statement);
        }
    }

    private void safeClose(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.warn("Unexpected sql failure", e);
            }
        }
    }

    private void safeClose(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (SQLException e) {
                log.warn("Unexpected sql failure", e);
            }
        }
    }

    private CompositeConfiguration getConfiguration(String databaseName) {
        CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
        // Configuration for specific DB. Can be absent
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setDelimiterParsingDisabled(true);
        configuration.setFileName("modeshape-" + databaseName + ".properties");
        try {
            configuration.load();
            compositeConfiguration.addConfiguration(configuration);
        } catch (ConfigurationException e) {
            log.debug("Configuration: {} file is absent", "modeshape-" + databaseName + ".properties", e);
        }
        // Default configuration
        configuration = new PropertiesConfiguration();
        configuration.setDelimiterParsingDisabled(true);
        configuration.setFileName("modeshape.properties");
        try {
            configuration.load();
        } catch (ConfigurationException e) {
            log.error("Error when initializing configuration: {}", "modeshape.properties", e);
        }
        compositeConfiguration.addConfiguration(configuration);

        return compositeConfiguration;
    }

    private String getRepoTableName(CompositeConfiguration configuration) {
        return configuration.getString("table.prefix") + "_" + configuration.getString("table.name");
    }

    private String changeCase(Case namesCase, String name) throws SQLException {
        switch (namesCase) {
            case LOWER:
                return name.toLowerCase();
            case UPPER:
                return name.toUpperCase();
            default:
                return name;
        }
    }

    private Case getCase(DatabaseMetaData metaData) throws SQLException {
        return metaData.storesLowerCaseIdentifiers() ? Case.LOWER
                                                     : metaData.storesUpperCaseIdentifiers() ? Case.UPPER : Case.MIXED;
    }

    private static class ModeshapeJcrRepo extends JcrRepository {

        private ModeshapeJcrRepo(RepositoryConfiguration configuration) {
            super(configuration);
        }

        private void shutdown() {
            doShutdown();
        }
    }

    private enum Case {
        UPPER,
        LOWER,
        MIXED
    }

    @Override
    public void setListener(Listener callback) {
        if (callback == null) {
            super.setListener(null);
        } else {
            super.setListener(new MultiAttemptListenerWrapper(callback));
        }
    }

    private static class MultiAttemptListenerWrapper implements Listener {
        private final Logger log = LoggerFactory.getLogger(MultiAttemptListenerWrapper.class);
        private final Listener listener;

        public MultiAttemptListenerWrapper(Listener listener) {
            this.listener = listener;
        }

        @Override
        public synchronized void onChange() {
            final Timer timer = new Timer();

            timer.schedule(new TimerTask() {
                int count = 0;

                @Override
                public void run() {
                    try {
                        log.info("Atempt # {}", count);
                        System.gc();
                        listener.onChange();
                        timer.cancel();
                    } catch (Exception ex) {
                        log.error("Unexpected error", ex);
                        count++;
                        if (count >= 5) {
                            timer.cancel();
                        }
                    }
                }
            }, 3000, 5000);
        }
    }
}
