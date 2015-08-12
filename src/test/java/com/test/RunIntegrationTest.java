package com.test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.vertx.core.Vertx;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;


public class RunIntegrationTest {

    Vertx vertx;
    Client client;


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

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        client = ClientBuilder.newClient(clientConfig);
    }

    @After
    public void teardDown() {
        vertx.close();
    }

    @Test
    public void testPrematurelyEndedRequest() {
        WebTarget target = client.target("http://localhost:8080").path("/request-ended-demo");
        Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
        invocationBuilder.header("Authorization","1234");
        Invocation invocation = invocationBuilder.buildGet();
        Response response = invocation.invoke();

        assertThat(response.getStatus(), is(equalTo(200)));
    }
}
