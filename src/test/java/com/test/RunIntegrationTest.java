package com.test;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.CountDownLatch;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.vertx.core.Vertx;

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * //TODO - document
 *
 * @author mquinlan
 */
public class RunIntegrationTest {

    Vertx vertx;


    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
            .bindAddress("localhost")
            .port(7777));

    @Before
    public void init() {
        WireMock.resetToDefault();

        vertx = Vertx.vertx();

        CountDownLatch startSignal = new CountDownLatch(1);
        vertx.deployVerticle(TestVerticle.class.getName(), asyncResult -> {
            if (asyncResult.succeeded()) {
                System.out.println("Deployed verticle successfully");
                startSignal.countDown();
            } else {
                fail("Could not deploy verticle: " + asyncResult.cause().toString());
            }
        });
    }

    @After
    public void teardDown() {
        vertx.close();
    }

    @Test
    public void testTimeouts() throws Exception {
        //GIVEN
        //Non-critical service times out
        ResponseDefinitionBuilder delayBuilder = WireMock.aResponse()
                .withStatus(200)
                .withFixedDelay(5000);
        WireMock.stubFor(WireMock.get(urlEqualTo("/path/one")).willReturn(delayBuilder));

        //Critical service works as expected
        ResponseDefinitionBuilder builder = WireMock.aResponse()
                .withStatus(200)
                .withHeader("header1", "headerValue1")
                .withBody("{ \"event\": \"value\" }");
        WireMock.stubFor(WireMock.get(urlEqualTo("/path/two")).willReturn(builder));

        //WHEN
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        Client client = ClientBuilder.newClient(clientConfig);
        WebTarget target = client.target("http://localhost:7777").path("/test");
        Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
        Invocation invocation = invocationBuilder.buildGet();
        Response response = invocation.invoke();

        //THEN
        assertThat(response.getStatus(), is(equalTo(200)));
    }
}
