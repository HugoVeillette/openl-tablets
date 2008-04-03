package com.exigen.example.solution.admin.database.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hsqldb.Server;
import org.hsqldb.ServerConfiguration;

import org.hsqldb.persist.HsqlProperties;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Properties;


/**
 * Bean that will start an instance of an HSQL database.
 *
 * <p>
 * This class is primarily intended to be used in demo applications. It allows for a self
 * contained distribution including a database instance. HSQLDB will be properly
 * "shutdowned".
 * </p>
 *
 * <p>
 * This is an example of a bean configuration:
 * </p>
 * <pre>
 *     &lt;bean id="dataBase" class="com.exigen.example.solution.admin.database.util.HsqlServerBean" singleton="true" lazy-init="false"&gt;
 *         &lt;property name="serverProperties"&gt;
 *             &lt;props&gt;
 *                 &lt;prop key="server.port"&gt;9101&lt;/prop&gt;
 *                 &lt;prop key="server.database.0"&gt;webapps/myapp/db/test&lt;/prop&gt;
 *                 &lt;prop key="server.dbname.0"&gt;test&lt;/prop&gt;
 *             &lt;/props&gt;
 *         &lt;/property&gt;
 *     &lt;/bean&gt;
 * </pre>
 *
 * @see org.hsqldb.Server
 *
 * @author Andrey Naumenko
 */
public class HsqlServerBean implements InitializingBean, DisposableBean {
    private static final Log log = LogFactory.getLog(HsqlServerBean.class);
    private Properties serverProperties;
    private boolean enabled = true;

    /** The actual server instance. */
    private Server server;

    /**
     * If <code>false</code> this class do nothing. <code>true</code> by default.
     *
     * @param enabled <code>true</code> or <code>false</code>.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Properties used to customize instance.
     *
     * @param serverProperties HSQLDB properties
     */
    public void setServerProperties(Properties serverProperties) {
        this.serverProperties = serverProperties;
    }

    public void afterPropertiesSet() throws Exception {
        if (!enabled) {
            log.debug("HsqlServerBean is disabled, so HSQLDB will not be started");
            return;
        }

        HsqlProperties configProps = new HsqlProperties(serverProperties);

        ServerConfiguration.translateDefaultDatabaseProperty(configProps);

        // finished setting up properties - set some important behaviors as well;
        server = new Server();
        server.setRestartOnShutdown(false);
        server.setNoSystemExit(true);
        server.setProperties(configProps);

        log.info("HSQL Server Startup sequence initiated");

        server.start();

        log.info("HSQL Server started on " + server.getAddress());
    }

    public void destroy() {
        if (!enabled) {
            return;
        }
        log.info("HSQL Server Shutdown sequence initiated");
        server.signalCloseAllServerConnections();
        server.shutdown();
        server = null;
        log.info("HSQL Server stopped");
    }
}
