package com.test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;


public class TestVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {

        HttpClient client = vertx.createHttpClient(new HttpClientOptions()
                .setMaxPoolSize(1)
                .setConnectTimeout(100)
                .setDefaultHost("localhost")
                .setDefaultPort(7777));


        Router router = Router.router(vertx);

        router.route("/test").handler(event -> {
            System.out.println("Handling /test");
            HttpClientRequest request1 = client.request(event.request().method(),
                    "/path/one", response -> {
                        response.bodyHandler(body -> {
                            event.response().setStatusCode(response.statusCode());
                            event.response().headers().setAll(response.headers());
                            event.response().end(body.toString());
                        });
                    })
                    .setTimeout(500L)
                    .exceptionHandler(throwable -> {
                        System.out.println(">>>PATH 1 TIMEOUT - expected");
                        //Not doing anything here as we are ok proceeding if this first path fails.
                    });

            HttpClientRequest request2 = client.request(event.request().method(),
                    "/path/two", response -> {
                        response.bodyHandler(body -> {
                            event.response().setStatusCode(response.statusCode());
                            event.response().headers().setAll(response.headers());
                            event.response().end(body.toString());
                        });
                    })
                    .setTimeout(500L)
                    .exceptionHandler(throwable -> {
                        System.out.println(">>>PATH 2 TIMEOUT - unexpected!");
                        event.response().setStatusCode(504);
                        //Deliberately ending response here as this is a critical service we depend on
                        event.response().end();
                    });

            request1.end();
            request2.end();
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
