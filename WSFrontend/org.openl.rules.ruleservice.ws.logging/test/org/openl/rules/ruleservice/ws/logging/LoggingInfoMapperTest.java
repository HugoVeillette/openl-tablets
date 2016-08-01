package org.openl.rules.ruleservice.ws.logging;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.cxf.interceptor.LoggingMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.logging.LoggingCustomData;
import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.LoggingInfoConvertor;
import org.openl.rules.ruleservice.logging.LoggingInfoMapper;
import org.openl.rules.ruleservice.logging.RuleServiceLoggingInfo;
import org.openl.rules.ruleservice.logging.TypeConvertor;
import org.openl.rules.ruleservice.logging.annotation.CustomDateValue1;
import org.openl.rules.ruleservice.logging.annotation.CustomDateValue2;
import org.openl.rules.ruleservice.logging.annotation.CustomDateValue3;
import org.openl.rules.ruleservice.logging.annotation.CustomNumberValue1;
import org.openl.rules.ruleservice.logging.annotation.CustomNumberValue2;
import org.openl.rules.ruleservice.logging.annotation.CustomNumberValue3;
import org.openl.rules.ruleservice.logging.annotation.CustomNumberValue4;
import org.openl.rules.ruleservice.logging.annotation.CustomNumberValue5;
import org.openl.rules.ruleservice.logging.annotation.CustomStringValue1;
import org.openl.rules.ruleservice.logging.annotation.CustomStringValue2;
import org.openl.rules.ruleservice.logging.annotation.CustomStringValue3;
import org.openl.rules.ruleservice.logging.annotation.CustomStringValue4;
import org.openl.rules.ruleservice.logging.annotation.CustomStringValue5;
import org.openl.rules.ruleservice.logging.annotation.IncomingTime;
import org.openl.rules.ruleservice.logging.annotation.InputName;
import org.openl.rules.ruleservice.logging.annotation.OutcomingTime;
import org.openl.rules.ruleservice.logging.annotation.Publisher;
import org.openl.rules.ruleservice.logging.annotation.Request;
import org.openl.rules.ruleservice.logging.annotation.Response;
import org.openl.rules.ruleservice.logging.annotation.ServiceName;
import org.openl.rules.ruleservice.logging.annotation.Url;
import org.openl.rules.ruleservice.logging.annotation.UseLoggingInfoConvertor;

public class LoggingInfoMapperTest {

    private static final String SOME_VALUE = RandomStringUtils.random(10, true, true);

    private long beginTime;
    private long endTime;

    @Before
    public void setUp() {
        beginTime = Timestamp.valueOf("1980-01-01 00:00:00").getTime();
        endTime = Timestamp.valueOf("2020-12-31 00:58:00").getTime();
    }

    /**
     * Method should generate random number that represents a time between two
     * dates.
     * 
     * @return
     */
    private Date getRandomTimeBetweenTwoDates() {
        long diff = endTime - beginTime + 1;
        long d = beginTime + (long) (Math.random() * diff);
        return new Date(d);
    }

    @Test
    public void testPublisherFilteringMapping() {
        LoggingInfoMapper mapper = new LoggingInfoMapper();

        RuleServiceLoggingInfo ruleServiceLoggingInfo = new RuleServiceLoggingInfo();
        final String customString1 = RandomStringUtils.random(10, true, true);
        final String customString2 = RandomStringUtils.random(10, true, true);

        LoggingCustomData loggingCustomData = new LoggingCustomData();
        loggingCustomData.setStringValue1(customString1);
        loggingCustomData.setStringValue2(customString2);

        ruleServiceLoggingInfo.setLoggingCustomData(loggingCustomData);

        final PublisherType publisher1 = PublisherType.RESTFUL;
        ruleServiceLoggingInfo.setPublisherType(publisher1);
        LoggingInfo loggingInfo = new LoggingInfo(ruleServiceLoggingInfo);

        TestEntity testEntity1 = new TestEntity();
        mapper.map(loggingInfo, testEntity1);

        // validation
        Assert.assertEquals(customString2, testEntity1.getValue2());
        Assert.assertEquals(null, testEntity1.getValue1());

        final PublisherType publisher2 = PublisherType.WEBSERVICE;
        ruleServiceLoggingInfo.setPublisherType(publisher2);

        TestEntity testEntity2 = new TestEntity();
        mapper.map(loggingInfo, testEntity2);

        // validation
        Assert.assertEquals(null, testEntity2.getValue2());
        Assert.assertEquals(customString1, testEntity2.getValue1());
    }

    @Test
    public void testPublisherConvertorMapping() {
        LoggingInfoMapper mapper = new LoggingInfoMapper();

        RuleServiceLoggingInfo ruleServiceLoggingInfo = new RuleServiceLoggingInfo();
        final String customString1 = RandomStringUtils.random(10, true, true);

        LoggingCustomData loggingCustomData = new LoggingCustomData();
        loggingCustomData.setStringValue1(" " + customString1 + " ");

        ruleServiceLoggingInfo.setLoggingCustomData(loggingCustomData);

        final PublisherType publisher1 = PublisherType.RESTFUL;
        ruleServiceLoggingInfo.setPublisherType(publisher1);
        LoggingInfo loggingInfo = new LoggingInfo(ruleServiceLoggingInfo);

        TestEntity testEntity = new TestEntity();
        mapper.map(loggingInfo, testEntity);

        // validation
        Assert.assertEquals(customString1, testEntity.getValue3());
    }

    @Test
    public void testSimpleMapping() {
        RuleServiceLoggingInfo ruleServiceLoggingInfo = new RuleServiceLoggingInfo();

        Random rnd = new Random(System.currentTimeMillis());

        final String customString1 = RandomStringUtils.random(10, true, true);
        final String customString2 = RandomStringUtils.random(10, true, true);
        final String customString3 = RandomStringUtils.random(10, true, true);
        final String customString4 = RandomStringUtils.random(10, true, true);
        final String customString5 = RandomStringUtils.random(10, true, true);

        final Long customNumber1 = rnd.nextLong();
        final Long customNumber2 = rnd.nextLong();
        final Long customNumber3 = rnd.nextLong();
        final Long customNumber4 = rnd.nextLong();
        final Long customNumber5 = rnd.nextLong();

        final Date customDate1 = getRandomTimeBetweenTwoDates();
        final Date customDate2 = getRandomTimeBetweenTwoDates();
        final Date customDate3 = getRandomTimeBetweenTwoDates();

        LoggingCustomData loggingCustomData = new LoggingCustomData();
        loggingCustomData.setDateValue1(customDate1);
        loggingCustomData.setDateValue2(customDate2);
        loggingCustomData.setDateValue3(customDate3);

        loggingCustomData.setNumberValue1(customNumber1);
        loggingCustomData.setNumberValue2(customNumber2);
        loggingCustomData.setNumberValue3(customNumber3);
        loggingCustomData.setNumberValue4(customNumber4);
        loggingCustomData.setNumberValue5(customNumber5);

        loggingCustomData.setStringValue1(customString1);
        loggingCustomData.setStringValue2(customString2);
        loggingCustomData.setStringValue3(customString3);
        loggingCustomData.setStringValue4(customString4);
        loggingCustomData.setStringValue5(customString5);

        ruleServiceLoggingInfo.setLoggingCustomData(loggingCustomData);

        final String request = RandomStringUtils.random(10);
        final String response = RandomStringUtils.random(10);
        final String inputName = RandomStringUtils.random(10);
        final String url = RandomStringUtils.random(10);
        final PublisherType publisher = PublisherType.RESTFUL;
        final String serviceName = RandomStringUtils.random(10);

        final Date incomingMessageTime = getRandomTimeBetweenTwoDates();
        final Date outcomingMessageTime = getRandomTimeBetweenTwoDates();

        ruleServiceLoggingInfo.setIncomingMessageTime(incomingMessageTime);
        ruleServiceLoggingInfo.setOutcomingMessageTime(outcomingMessageTime);
        ruleServiceLoggingInfo.setInputName(inputName);
        ruleServiceLoggingInfo.setServiceName(serviceName);
        ruleServiceLoggingInfo.setPublisherType(publisher);
        LoggingMessage requestLoggingMessage = new LoggingMessage("", "");
        requestLoggingMessage.getPayload().append(request);
        requestLoggingMessage.getAddress().append(url);
        ruleServiceLoggingInfo.setRequestMessage(requestLoggingMessage);

        LoggingMessage responseLoggingMessage = new LoggingMessage("", "");
        responseLoggingMessage.getPayload().append(response);
        responseLoggingMessage.getAddress().append(url);
        ruleServiceLoggingInfo.setResponseMessage(responseLoggingMessage);

        LoggingInfo loggingInfo = new LoggingInfo(ruleServiceLoggingInfo);

        LoggingInfoMapper mapper = new LoggingInfoMapper();
        TestEntity testEntity = new TestEntity();

        mapper.map(loggingInfo, testEntity);

        // validation
        Assert.assertEquals(SOME_VALUE, testEntity.getId());
        Assert.assertEquals(inputName, testEntity.getInputName());
        Assert.assertEquals(incomingMessageTime, testEntity.getIncomingTime());
        Assert.assertEquals(outcomingMessageTime, testEntity.getOutcomingTime());
        Assert.assertEquals(serviceName, testEntity.getServiceName());
        Assert.assertEquals(publisher.toString(), testEntity.getPublisherType());
        Assert.assertEquals(url, url);
        Assert.assertEquals(request, testEntity.getRequest());
        Assert.assertEquals(response, testEntity.getResponse());

        // Custom data
        Assert.assertEquals(customString1, testEntity.getStringValue1());
        Assert.assertEquals(customString2, testEntity.getValue2());
        Assert.assertEquals(customString2, testEntity.getStringValue2());
        Assert.assertEquals(customString3, testEntity.getStringValue3());
        Assert.assertEquals(customString4, testEntity.getStringValue4());
        Assert.assertEquals(customString5, testEntity.getStringValue5());

        Assert.assertEquals(customNumber1, testEntity.getNumberValue1());
        Assert.assertEquals(customNumber2, testEntity.getNumberValue2());
        Assert.assertEquals(customNumber3, testEntity.getNumberValue3());
        Assert.assertEquals(customNumber4, testEntity.getNumberValue4());
        Assert.assertEquals(customNumber5, testEntity.getNumberValue5());

        Assert.assertEquals(customDate1, testEntity.getDateValue1());
        Assert.assertEquals(customDate2, testEntity.getDateValue2());
        Assert.assertEquals(customDate3, testEntity.getDateValue3());
    }

    public static class SomeValueConvertor implements LoggingInfoConvertor<String> {
        @Override
        public String convert(LoggingInfo loggingInfo) {
            return SOME_VALUE;
        }
    }

    public static class TrimConvertor implements TypeConvertor<String, String> {
        @Override
        public String convert(String value) {
            if (value == null){
                return null;
            }
            return value.trim();
        }
    }

    public static class TestEntity {
        private String id;
        private Date incomingTime;
        private Date outcomingTime;
        private String request;
        private String response;
        private String serviceName;
        private String url;
        private String inputName;
        private String publisherType;
        private String stringValue1;
        private String stringValue2;
        private String stringValue3;
        private String stringValue4;
        private String stringValue5;
        private Long numberValue1;
        private Long numberValue2;
        private Long numberValue3;
        private Long numberValue4;
        private Long numberValue5;
        private Date dateValue1;
        private Date dateValue2;
        private Date dateValue3;

        private String value1;
        private String value2;
        private String value3;

        public TestEntity() {
        }

        public String getId() {
            return id;
        }

        @UseLoggingInfoConvertor(convertor = SomeValueConvertor.class)
        public void setId(String id) {
            this.id = id;
        }

        public Date getIncomingTime() {
            return incomingTime;
        }

        @IncomingTime
        public void setIncomingTime(Date incomingTime) {
            this.incomingTime = incomingTime;
        }

        public Date getOutcomingTime() {
            return outcomingTime;
        }

        @OutcomingTime
        public void setOutcomingTime(Date outcomingTime) {
            this.outcomingTime = outcomingTime;
        }

        public String getRequest() {
            return request;
        }

        @Request
        public void setRequest(String request) {
            this.request = request;
        }

        public String getResponse() {
            return response;
        }

        @Response
        public void setResponse(String response) {
            this.response = response;
        }

        public String getServiceName() {
            return serviceName;
        }

        @ServiceName
        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getUrl() {
            return url;
        }

        @Url
        public void setUrl(String url) {
            this.url = url;
        }

        public String getInputName() {
            return inputName;
        }

        @InputName
        public void setInputName(String inputName) {
            this.inputName = inputName;
        }

        public String getPublisherType() {
            return publisherType;
        }

        @Publisher
        public void setPublisherType(String publisherType) {
            this.publisherType = publisherType;
        }

        public String getStringValue1() {
            return stringValue1;
        }

        @CustomStringValue1
        public void setStringValue1(String stringValue1) {
            this.stringValue1 = stringValue1;
        }

        public String getStringValue2() {
            return stringValue2;
        }

        @CustomStringValue2
        public void setStringValue2(String stringValue2) {
            this.stringValue2 = stringValue2;
        }

        public String getStringValue3() {
            return stringValue3;
        }

        @CustomStringValue3
        public void setStringValue3(String stringValue3) {
            this.stringValue3 = stringValue3;
        }

        public String getStringValue4() {
            return stringValue4;
        }

        @CustomStringValue4
        public void setStringValue4(String stringValue4) {
            this.stringValue4 = stringValue4;
        }

        public String getStringValue5() {
            return stringValue5;
        }

        @CustomStringValue5
        public void setStringValue5(String stringValue5) {
            this.stringValue5 = stringValue5;
        }

        public Long getNumberValue1() {
            return numberValue1;
        }

        @CustomNumberValue1
        public void setNumberValue1(Long numberValue1) {
            this.numberValue1 = numberValue1;
        }

        public Long getNumberValue2() {
            return numberValue2;
        }

        @CustomNumberValue2
        public void setNumberValue2(Long numberValue2) {
            this.numberValue2 = numberValue2;
        }

        public Long getNumberValue3() {
            return numberValue3;
        }

        @CustomNumberValue3
        public void setNumberValue3(Long numberValue3) {
            this.numberValue3 = numberValue3;
        }

        public Long getNumberValue4() {
            return numberValue4;
        }

        @CustomNumberValue4
        public void setNumberValue4(Long numberValue4) {
            this.numberValue4 = numberValue4;
        }

        public Long getNumberValue5() {
            return numberValue5;
        }

        @CustomNumberValue5
        public void setNumberValue5(Long numberValue5) {
            this.numberValue5 = numberValue5;
        }

        public Date getDateValue1() {
            return dateValue1;
        }

        @CustomDateValue1
        public void setDateValue1(Date dateValue1) {
            this.dateValue1 = dateValue1;
        }

        public Date getDateValue2() {
            return dateValue2;
        }

        @CustomDateValue2
        public void setDateValue2(Date dateValue2) {
            this.dateValue2 = dateValue2;
        }

        public Date getDateValue3() {
            return dateValue3;
        }

        @CustomDateValue3
        public void setDateValue3(Date dateValue3) {
            this.dateValue3 = dateValue3;
        }

        @Override
        public String toString() {
            return "TestEntity [id=" + id + "]";
        }

        public String getValue1() {
            return value1;
        }

        @CustomStringValue1(publisherTypes = PublisherType.WEBSERVICE)
        public void setValue1(String value1) {
            this.value1 = value1;
        }

        public String getValue2() {
            return value2;
        }

        @CustomStringValue2(publisherTypes = PublisherType.RESTFUL)
        public void setValue2(String value2) {
            this.value2 = value2;
        }

        public String getValue3() {
            return value3;
        }

        @CustomStringValue1(convertor = TrimConvertor.class)
        public void setValue3(String value3) {
            this.value3 = value3;
        }

    }
}
