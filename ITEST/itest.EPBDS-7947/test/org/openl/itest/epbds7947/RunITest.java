package org.openl.itest.epbds7947;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.UUID;

import org.apache.cxf.binding.soap.SoapFault;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.generated.beans.Coverage;
import org.openl.generated.beans.Driver;
import org.openl.generated.beans.Policy;
import org.openl.generated.beans.Vehicle;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.openl.itest.core.SoapClientFactory;
import org.openl.itest.epbds7947.project.MainService;
import org.openl.itest.responsedto.ErrorResponse;
import org.openl.rules.ruleservice.core.ExceptionType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class RunITest {

    private static JettyServer server;
    private static String baseURI;

    private MainService soapClient;
    private RestTemplate rest;

    private Policy policy;
    private Policy[] policies;

    @BeforeClass
    public static void setUp() throws Exception {
        server = new JettyServer(true);
        baseURI = server.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Before
    public void before() {
        soapClient = new SoapClientFactory<>(baseURI + "/parent-datatype-validation", MainService.class).createProxy();
        rest = new RestClientFactory(baseURI + "/REST/parent-datatype-validation").create();

        policy = createPolicyBean();
        policies = new Policy[3];
        for (int i = 0; i < policies.length; i++) {
            policies[i] = createPolicyBean();
        }
    }

    private Policy createPolicyBean() {
        Policy policy = new Policy();
        policy.setId(UUID.randomUUID().toString());
        policy.setTransaction("NEW_BUSINESS");
        Vehicle[] vehicles = new Vehicle[2];
        for (int i = 0; i < vehicles.length; i++) {
            vehicles[i] = new Vehicle();
            vehicles[i].setId(UUID.randomUUID().toString());

            Driver driver = new Driver();
            driver.setBirthDate(new Date());
            driver.setGender("male");

            vehicles[i].setDriver(driver);

            Coverage[] coverages = new Coverage[4];
            for (int j = 0; j < coverages.length; j++) {
                coverages[j] = new Coverage();
                coverages[j].setName("COV" + (j + 1));
            }
            vehicles[i].setCoverages(coverages);
        }
        policy.setVehicles(vehicles);

        Integer[] brandCodes = new Integer[3];
        for (int i = 0; i < brandCodes.length; i++) {
            brandCodes[i] = (i + 1) * 10;
        }
        policy.setBrandCodes(brandCodes);

        return policy;
    }

    @Test
    public void test_validation_shouldBeOK() {
        ResponseEntity<String> responseEntity = rest.exchange("/checkValidation", HttpMethod.POST, RestClientFactory.request(policy), String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("OK", responseEntity.getBody());

        responseEntity = rest.exchange("/checkArrayValidation", HttpMethod.POST, RestClientFactory.request(policies), String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("OK", responseEntity.getBody());

        responseEntity = rest.exchange("/checkArrayValidationFromParent", HttpMethod.POST, RestClientFactory.request(policies), String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("OK", responseEntity.getBody());

        responseEntity = rest.exchange("/getGender", HttpMethod.POST, RestClientFactory.request("male"), String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("male", responseEntity.getBody());

        assertEquals("OK", soapClient.checkValidation(policy));
        assertEquals("OK", soapClient.checkArrayValidation(policies));
        assertEquals("male", soapClient.getGender("male"));
    }

    @Test
    public void test_validation_onPolicy_shouldBeFailed() {
        policy.setTransaction("WRONG");
        ResponseEntity<ErrorResponse> responseEntity = rest.exchange("/checkValidation", HttpMethod.POST, RestClientFactory.request(policy), ErrorResponse.class);
        assertRestValidationResponse("Object 'WRONG' is outside of valid domain 'TransanctionType'. Valid values: [NEW_BUSINESS, ENDORSMENT]", responseEntity);

        try {
            soapClient.checkValidation(policy);
            fail("Oops... Must be failed!");
        } catch (SoapFault e) {
            assertSoapValidationFault("Object 'WRONG' is outside of valid domain 'TransanctionType'. Valid values: [NEW_BUSINESS, ENDORSMENT]", e);
        }
    }

    @Test
    public void test_validation_onDriver_shouldBeFailed() {
        policy.getVehicles()[0].getDriver().setGender("NON");
        ResponseEntity<ErrorResponse> responseEntity = rest.exchange("/checkValidation", HttpMethod.POST, RestClientFactory.request(policy), ErrorResponse.class);
        assertRestValidationResponse("Object 'NON' is outside of valid domain 'Gender'. Valid values: [male, female, other]", responseEntity);

        try {
            soapClient.checkValidation(policy);
            fail("Oops... Must be failed!");
        } catch (SoapFault e) {
            assertSoapValidationFault("Object 'NON' is outside of valid domain 'Gender'. Valid values: [male, female, other]", e);
        }
    }

    @Test
    public void test_validation_onCoverage_shouldBeFailed() {
        policy.getVehicles()[0].getCoverages()[3].setName("COV10");
        ResponseEntity<ErrorResponse> responseEntity = rest.exchange("/checkValidation", HttpMethod.POST, RestClientFactory.request(policy), ErrorResponse.class);
        assertRestValidationResponse("Object 'COV10' is outside of valid domain 'CoverageName'. Valid values: [COV1, COV2, COV3, COV4]", responseEntity);

        try {
            soapClient.checkValidation(policy);
            fail("Oops... Must be failed!");
        } catch (SoapFault e) {
            assertSoapValidationFault("Object 'COV10' is outside of valid domain 'CoverageName'. Valid values: [COV1, COV2, COV3, COV4]", e);
        }
    }

    @Test
    public void test_validation_onBrandCode_shouldBeFailed() {
        policy.getBrandCodes()[1] = 100;
        ResponseEntity<ErrorResponse> responseEntity = rest.exchange("/checkValidation", HttpMethod.POST, RestClientFactory.request(policy), ErrorResponse.class);
        assertRestValidationResponse("Object '100' is outside of valid domain 'BrandCode[]'. Valid values: [10, 20, 30, 40]", responseEntity);

        try {
            soapClient.checkValidation(policy);
            fail("Oops... Must be failed!");
        } catch (SoapFault e) {
            assertSoapValidationFault("Object '100' is outside of valid domain 'BrandCode[]'. Valid values: [10, 20, 30, 40]", e);
        }
    }

    @Test
    public void test_validation_getGender_shouldBeFailed() {
        ResponseEntity<ErrorResponse> responseEntity = rest.exchange("/getGender", HttpMethod.POST, RestClientFactory.request("WRONG"), ErrorResponse.class);
        assertRestValidationResponse("Object 'WRONG' is outside of valid domain 'Gender'. Valid values: [male, female, other]", responseEntity);

        try {
            soapClient.getGender("WRONG");
            fail("Oops... Must be failed!");
        } catch (SoapFault e) {
            assertSoapValidationFault("Object 'WRONG' is outside of valid domain 'Gender'. Valid values: [male, female, other]", e);
        }
    }

    @Test
    public void test_validation_onArrays_shouldBeFailed() {
        policies[1].getVehicles()[0].getDriver().setGender("NON");
        ResponseEntity<ErrorResponse> responseEntity = rest.exchange("/checkArrayValidation", HttpMethod.POST, RestClientFactory.request(policies), ErrorResponse.class);
        assertRestValidationResponse("Object 'NON' is outside of valid domain 'Gender'. Valid values: [male, female, other]", responseEntity);

        responseEntity = rest.exchange("/checkArrayValidationFromParent", HttpMethod.POST, RestClientFactory.request(policies), ErrorResponse.class);
        assertRestValidationResponse("Object 'NON' is outside of valid domain 'Gender'. Valid values: [male, female, other]", responseEntity);

        try {
            soapClient.checkArrayValidation(policies);
            fail("Oops... Must be failed!");
        } catch (SoapFault e) {
            assertSoapValidationFault("Object 'NON' is outside of valid domain 'Gender'. Valid values: [male, female, other]", e);
        }

        try {
            soapClient.checkArrayValidationFromParent(policies);
            fail("Oops... Must be failed!");
        } catch (SoapFault e) {
            assertSoapValidationFault("Object 'NON' is outside of valid domain 'Gender'. Valid values: [male, female, other]", e);
        }
    }

    private static void assertRestValidationResponse(String expectedMsg, ResponseEntity<ErrorResponse> response) {
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        ErrorResponse error = response.getBody();
        assertNotNull(error);
        assertEquals(ExceptionType.VALIDATION.name(), error.getType());
        assertEquals(expectedMsg, error.getMessage());
        assertNull(error.getDetails());
    }

    private static void assertSoapValidationFault(String expectedMsg, SoapFault e) {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getStatusCode());
        assertEquals(expectedMsg, e.getMessage());
        assertEquals(ExceptionType.VALIDATION.name(), e.getDetail().getElementsByTagName("type").item(0).getTextContent());
    }
}
