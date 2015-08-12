package com.test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.rx.java.RxHelper;
import rx.Observable;


public class TestVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {

        Router router = Router.router(vertx);


        router.route("/request-ended-demo").handler(event -> {

            assert !event.request().isEnded();

            String header = event.request().getHeader("Authorization");

            assert header.equals("1234");

            HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(8080));
            HttpClientRequest httpClientRequest = httpClient.request(HttpMethod.GET, "/loopback", response -> System.out.println("Loopback response received"));
            Observable<HttpClientResponse> obs = RxHelper.toObservable(httpClientRequest);
            obs
                    .finallyDo(event::next)
                    .subscribe(data -> {
                        if (event.request().isEnded()) {
                            System.out.println("Request ended unexpectedly");
                        }
                    });
            httpClientRequest.end();

            assert !event.request().isEnded();
        });

        //Simulates calling upstream services
        router.route("/loopback").handler(event -> event.response().setStatusCode(200).end());

        router.route().handler(event -> {
            System.out.println("Last handler in chain");

            Buffer requestBody = Buffer.buffer();
            event.request().handler(requestBody::appendBuffer);
            event.request().endHandler(Void -> System.out.println("In end handler"));

            event.response().setStatusCode(200).end();
        });

        HttpServer server = vertx.createHttpServer(
                new HttpServerOptions()
                        .setHost("localhost")
                        .setPort(8080));

        server.requestHandler(router::accept).listen(this::logStartup);
    }

    private void logStartup(AsyncResult result) {
        if (result.succeeded()) {
            System.out.println("Server started successfully");
        } else {
            System.err.println("Server failed to start");
            //noinspection ThrowableResultOfMethodCallIgnored
            result.cause().printStackTrace();
            System.exit(-1);
        }
    }


}
