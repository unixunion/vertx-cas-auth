package com.deblox.cas;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * Created by Kegan Holtzhausen on 6/4/14.
 *
 * this is a simple CAS integration example in VertX.io
 *
 * Config file example:

 {
     "main":"com.deblox.cas.Casclient", // main class to boot
     "casHost": "localhost", // hostname the casClient should use to reach CAS
     "casPort": 8443, // port the casClient should use
     "cas_redirectUrl": "https://localhost:8443/cas", // used in redirects, NO trails!
     "trustAll": true, // allows self-signed certs for DEV
     "ssl": true,
 }

 */

public class CasHttpService extends Verticle {

    Logger logger;
    String casHost;
    Integer casPort;
    String cas_redirectUrl;
    Boolean keepAlive;
    Boolean ssl;
    Boolean trustAll;
    JsonObject config = null;
    HttpClient casClient;
    HttpServer server;
    RouteMatcher routeMatcher;

    /**
     * start the verticle and the http listeners
     */
    public void start() {
        logger = container.logger();
        logger.info("Staring CAS service");

        config = container.config();
        casHost = config.getString("casHost", "localhost"); // cas server hostname
        casPort = (int)config.getNumber("casPort", 8443); // port on the CAS server side.
        keepAlive = config.getBoolean("keepAlive", false);  // keep connection to CAS alive or not.
        ssl = config.getBoolean("ssl", false); // use SSL to talk to the CAS server
        trustAll = config.getBoolean("trustAll", false); // trust any CA cert ( use for DEVELOPMENT ONLY! )

        CasRequestClient.setCas_redirectUrl(config.getString("cas_redirectUrl"));


        server = vertx.createHttpServer();
        routeMatcher = new RouteMatcher();

        routeMatcher.get("/unauthorized", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest event) {
                event.response().end("Not authorized, or ticket invalid");
            }
        });

        routeMatcher.get("/fail", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest event) {
                event.response().end("CAS failure");
            }
        });

        routeMatcher.get("/loggedin", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest event) {
                CasRequestClient.authenticate(event, "/loggedin", new Handler<HttpServerRequest>() {
                    @Override
                    public void handle(HttpServerRequest event) {
                        event.response().end("Logged in");
                    }
                });
            }
        });

        routeMatcher.get("/logout", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest event) {
                SessionStorage.remove(SessionStorage.getOrCreateSessionId(event), "auth");
                redirect(event, CasRequestClient.getCas_redirectUrl() + "/logout");
            }
        });

        routeMatcher.get("/login", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest event) {
                logger.info("login: " + event.uri());
                CasRequestClient.authenticate(event, "/loggedin");
            }
        });

        /*
        some generic service you want to CAS-ify
         */
        routeMatcher.get("/someservice", new Handler<HttpServerRequest>() {

            /* handle requests to /someservice */
            @Override
            public void handle(HttpServerRequest event) {
                logger.info("someservice: " + event.uri());
                /* authenticate the service and setup a handler for if its successful */
                CasRequestClient.authenticate(event, "/someservice",new Handler<HttpServerRequest>() {
                    @Override
                    public void handle(HttpServerRequest event) {
                        logger.info("someservice callback");
                        event.response().end("this is the secured area!");
                    }
                });
            }
        });

        server.requestHandler(routeMatcher).listen(3001, "localhost");

        logger.info("Connecting to CAS");
        casClient = vertx.createHttpClient()
                .setPort(casPort)
                .setHost(casHost)
                .setKeepAlive(keepAlive)
                .setSSL(ssl)
                .setTrustAll(trustAll);

        // set the CasRequestClients connection up
        CasRequestClient.setCasClient(casClient);

        logger.info("Starting sessionExpiry timer");
        long timerID = vertx.setPeriodic(10000, new Handler<Long>() {
            public void handle(Long timerID) {
                logger.info("Cleaning out old sessions");
                SessionStorage.sessionCleanup();
            }
        });

    }

    // redirect request to path
    public static void redirect(HttpServerRequest request, String path) {
        request.response().headers().set("Location", path);
        request.response().setStatusCode(302);
        request.response().end();
    }

    public static void redirect(HttpServerRequest request, String path, Boolean redirectToPath, final Handler<HttpServerRequest> callback) {
        if (redirectToPath.equals(true)) {
            redirect(request, path);
        } else if (!callback.equals(null)) {
            callback.handle(request);
        }
    }

}
