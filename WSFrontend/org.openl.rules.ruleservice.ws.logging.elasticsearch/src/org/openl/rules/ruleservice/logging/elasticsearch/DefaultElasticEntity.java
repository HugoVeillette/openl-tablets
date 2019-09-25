package org.openl.rules.ruleservice.logging.elasticsearch;

import java.util.Date;

import org.openl.rules.ruleservice.logging.annotation.IncomingTime;
import org.openl.rules.ruleservice.logging.annotation.InputName;
import org.openl.rules.ruleservice.logging.annotation.OutcomingTime;
import org.openl.rules.ruleservice.logging.annotation.Publisher;
import org.openl.rules.ruleservice.logging.annotation.PublisherType;
import org.openl.rules.ruleservice.logging.annotation.QualifyPublisherType;
import org.openl.rules.ruleservice.logging.annotation.Request;
import org.openl.rules.ruleservice.logging.annotation.Response;
import org.openl.rules.ruleservice.logging.annotation.ServiceName;
import org.openl.rules.ruleservice.logging.annotation.Url;
import org.openl.rules.ruleservice.logging.annotation.WithStoreLoggingDataConvertor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "openl_logging")
public class DefaultElasticEntity {
    @WithStoreLoggingDataConvertor(convertor = RandomUUID.class)
    @Id
    private String id;

    @IncomingTime
    private Date incomingTime;

    @OutcomingTime
    private Date outcomingTime;

    @Request
    private String requestBody;

    @Response
    private String responseBody;

    @WithStoreLoggingDataConvertor(convertor = JSONRequest.class)
    @QualifyPublisherType(PublisherType.RESTFUL)
    private Object request;

    @WithStoreLoggingDataConvertor(convertor = JSONResponse.class)
    @QualifyPublisherType(PublisherType.RESTFUL)
    private Object response;

    @ServiceName
    private String serviceName;

    @InputName
    private String inputName;

    @Publisher
    private String publisherType;

    @Url
    private String url;

    public DefaultElasticEntity() {
    }

    public DefaultElasticEntity(String id,
            Date incomingTime,
            Date outcomingTime,
            Object request,
            Object response,
            String requestBody,
            String responseBody,
            String serviceName,
            String url,
            String inputName,
            String publisherType) {
        super();
        this.id = id;
        this.incomingTime = incomingTime;
        this.outcomingTime = outcomingTime;
        this.request = request;
        this.response = response;
        this.requestBody = requestBody;
        this.responseBody = responseBody;
        this.serviceName = serviceName;
        this.url = url;
        this.inputName = inputName;
        this.publisherType = publisherType;
    }

    public String getPublisherType() {
        return publisherType;
    }

    public String getId() {
        return id;
    }

    public Object getRequest() {
        return request;
    }

    public Object getResponse() {
        return response;
    }

    public Date getIncomingTime() {
        return incomingTime;
    }

    public Date getOutcomingTime() {
        return outcomingTime;
    }

    public String getInputName() {
        return inputName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getUrl() {
        return url;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setIncomingTime(Date incomingTime) {
        this.incomingTime = incomingTime;
    }

    public void setOutcomingTime(Date outcomingTime) {
        this.outcomingTime = outcomingTime;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public void setRequest(Object request) {
        this.request = request;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    public void setPublisherType(String publisherType) {
        this.publisherType = publisherType;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "ElasticStoreLoggingEntity [id=" + id + "]";
    }
}
