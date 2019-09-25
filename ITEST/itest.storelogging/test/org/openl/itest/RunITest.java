package org.openl.itest;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static org.hamcrest.CoreMatchers.equalTo;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openl.itest.core.JettyServer;
import org.openl.rules.ruleservice.kafka.KafkaHeaders;
import org.openl.rules.ruleservice.logging.annotation.PublisherType;
import org.openl.rules.ruleservice.logging.cassandra.DefaultCassandraEntity;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.mapping.annotations.Table;

import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;
import net.mguenther.kafka.junit.EmbeddedKafkaConfig;
import net.mguenther.kafka.junit.KeyValue;
import net.mguenther.kafka.junit.ObserveKeyValues;
import net.mguenther.kafka.junit.SendKeyValues;

public class RunITest {
    // private static final int TIMEOUT = Integer.MAX_VALUE;
    private static final int TIMEOUT = 30;
    private static final String KEYSPACE = "openl_ws_logging";

    private static final String DEFAULT_TABLE_NAME = DefaultCassandraEntity.class.getAnnotation(Table.class).name();

    private static JettyServer server;

    private static void createKeyspaceIfNotExists(Session session,
            String keyspaceName,
            String replicationStrategy,
            int replicationFactor) {
        StringBuilder sb = new StringBuilder("CREATE KEYSPACE IF NOT EXISTS ").append(keyspaceName)
            .append(" WITH replication = {")
            .append("'class':'")
            .append(replicationStrategy)
            .append("','replication_factor':")
            .append(replicationFactor)
            .append("};");

        String query = sb.toString();
        session.execute(query);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra(EmbeddedCassandraServerHelper.CASSANDRA_RNDPORT_YML_FILE);
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();

        System.setProperty("cassandra.contactpoints", EmbeddedCassandraServerHelper.getHost());
        System.setProperty("cassandra.port", String.valueOf(EmbeddedCassandraServerHelper.getNativeTransportPort()));

        createKeyspaceIfNotExists(EmbeddedCassandraServerHelper.getSession(), KEYSPACE, "SimpleStrategy", 1);

        server = new JettyServer();
        server.start();

    }

    @Rule
    public EmbeddedKafkaCluster cluster = provisionWith(EmbeddedKafkaClusterConfig.create()
        .provisionWith(EmbeddedKafkaConfig.create().with("listeners", "PLAINTEXT://:61099").build())
        .build());

    private boolean truncateTableIfExists(final String keyspace, final String table) {
        try {
            EmbeddedCassandraServerHelper.getSession().execute("TRUNCATE " + keyspace + "." + table);
            return true;
        } catch (QueryExecutionException | InvalidQueryException e) {
            return false;
        }
    }

    private void testKafkaMethodServiceOk() throws Exception {
        final String REQUEST = "{\"hour\": 5}";
        final String RESPONSE = "\"Good Morning\"";

        truncateTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);

        KeyValue<String, String> record0 = new KeyValue<>(null, REQUEST);
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record0)).useDefaults());

        ObserveKeyValues<String, String> observeRequest0 = ObserveKeyValues.on("hello-out-topic", 1).build();
        List<String> observedValues = cluster.observeValues(observeRequest0);
        Assert.assertEquals(1, observedValues.size());
        Assert.assertEquals(RESPONSE, observedValues.get(0));

        Awaitility.given()
            .ignoreException(InvalidQueryException.class)
            .await()
            .atMost(TIMEOUT, TimeUnit.SECONDS)
            .until(() -> {
                ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + DEFAULT_TABLE_NAME);
                List<Row> rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                Assert.assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                Assert.assertNotNull(row.getString("id"));
                Assert.assertEquals(REQUEST, row.getString("request"));
                Assert.assertEquals(RESPONSE, row.getString("response"));
                Assert.assertEquals("Hello", row.getString("inputName"));
                Assert.assertEquals("simple1", row.getString("serviceName"));
                Assert.assertNotNull(row.getTimestamp("incomingTime"));
                Assert.assertNotNull(row.getTimestamp("outcomingTime"));
                Assert.assertEquals(PublisherType.KAFKA.toString(), row.getString("publisherType"));

                return true;
            }, equalTo(true));
    }

    private void testKafkaMethodServiceFail() throws Exception {
        final String REQUEST = "5";
        final String RESPONSE = REQUEST;

        truncateTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);

        KeyValue<String, String> record1 = new KeyValue<>(null, REQUEST);
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record1)).useDefaults());

        ObserveKeyValues<String, String> observeRequestDlt = ObserveKeyValues.on("hello-dlt-topic", 1).build();
        List<String> observedValuesDlt = cluster.observeValues(observeRequestDlt);
        Assert.assertEquals(1, observedValuesDlt.size());

        Awaitility.given()
            .ignoreException(InvalidQueryException.class)
            .await()
            .atMost(TIMEOUT, TimeUnit.SECONDS)
            .until(() -> {
                ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + DEFAULT_TABLE_NAME);
                List<Row> rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                Assert.assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                Assert.assertNotNull(row.getString("id"));
                Assert.assertEquals(REQUEST, row.getString("request"));
                Assert.assertEquals(RESPONSE, row.getString("response"));
                Assert.assertEquals("Hello", row.getString("inputName"));
                Assert.assertEquals("simple1", row.getString("serviceName"));
                Assert.assertNotNull(row.getTimestamp("incomingTime"));
                Assert.assertNotNull(row.getTimestamp("outcomingTime"));
                Assert.assertEquals(PublisherType.KAFKA.toString(), row.getString("publisherType"));
                return true;
            }, equalTo(true));
    }

    private void testKafkaServiceOk() throws Exception {
        final String REQUEST = "{\"hour\": 5}";
        final String RESPONSE = "\"Good Morning\"";

        final String METHOD_NAME = "Hello";

        truncateTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);

        KeyValue<String, String> record2 = new KeyValue<>(null, REQUEST);
        record2.addHeader(KafkaHeaders.METHOD_NAME, METHOD_NAME, Charset.forName("UTF8"));
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record2)).useDefaults());

        ObserveKeyValues<String, String> observeRequest2 = ObserveKeyValues.on("hello-out-topic-2", 1).build();
        List<String> observedValues2 = cluster.observeValues(observeRequest2);
        Assert.assertEquals(1, observedValues2.size());
        Assert.assertEquals(RESPONSE, observedValues2.get(0));

        Awaitility.given()
            .ignoreException(InvalidQueryException.class)
            .await()
            .atMost(TIMEOUT, TimeUnit.SECONDS)
            .until(() -> {
                ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + DEFAULT_TABLE_NAME);
                List<Row> rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                Assert.assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                Assert.assertNotNull(row.getString("id"));
                Assert.assertEquals(REQUEST, row.getString("request"));
                Assert.assertEquals(RESPONSE, row.getString("response"));
                Assert.assertEquals(METHOD_NAME, row.getString("inputName"));
                Assert.assertEquals("simple2", row.getString("serviceName"));
                Assert.assertNotNull(row.getTimestamp("incomingTime"));
                Assert.assertNotNull(row.getTimestamp("outcomingTime"));
                Assert.assertEquals(PublisherType.KAFKA.toString(), row.getString("publisherType"));

                return true;
            }, equalTo(true));
    }

    private void testKafkaServiceFail() throws Exception {
        final String REQUEST = "5";
        final String RESPONSE = "5";

        final String METHOD_NAME = "Hello";

        truncateTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);

        KeyValue<String, String> record = new KeyValue<>(null, "5");
        record.addHeader(KafkaHeaders.METHOD_NAME, METHOD_NAME, Charset.forName("UTF8"));
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequestDlt3 = ObserveKeyValues.on("hello-dlt-topic-2", 1).build();
        List<String> observedValuesDlt3 = cluster.observeValues(observeRequestDlt3);
        Assert.assertEquals(1, observedValuesDlt3.size());

        Awaitility.given()
            .ignoreException(InvalidQueryException.class)
            .await()
            .atMost(TIMEOUT, TimeUnit.SECONDS)
            .until(() -> {
                ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + DEFAULT_TABLE_NAME);
                List<Row> rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                Assert.assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                Assert.assertNotNull(row.getString("id"));
                Assert.assertEquals(REQUEST, row.getString("request"));
                Assert.assertEquals(RESPONSE, row.getString("response"));
                Assert.assertEquals(METHOD_NAME, row.getString("inputName"));
                Assert.assertEquals("simple2", row.getString("serviceName"));
                Assert.assertNotNull(row.getTimestamp("incomingTime"));
                Assert.assertNotNull(row.getTimestamp("outcomingTime"));
                Assert.assertEquals(PublisherType.KAFKA.toString(), row.getString("publisherType"));
                return true;
            }, equalTo(true));
    }

    @Test
    public void test() throws Exception {
        testKafkaMethodServiceOk();
        testKafkaMethodServiceFail();

        testKafkaServiceOk();
        testKafkaServiceFail();
    }

    @After
    public void destroy() {
        cluster.close();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Thread.sleep(Long.MAX_VALUE);

        server.stop();
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }

}