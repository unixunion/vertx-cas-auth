package com.deblox.cas;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.*;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

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


    public void start() {
        logger = container.logger();
        logger.info("Staring CAS service");

        config = container.config();
        casHost = config.getString("casHost", "localhost"); // cas server hostname
        casPort = (int)config.getNumber("casPort", 8443); // port on the CAS server side.
        cas_redirectUrl = config.getString("cas_redirectUrl"); // login redirection URL eg: "https://localhost:8443/cas"
        keepAlive = config.getBoolean("keepAlive", false);  // keep connection to CAS alive or not.
        ssl = config.getBoolean("ssl", false); // use SSL to talk to the CAS server
        trustAll = config.getBoolean("trustAll", false); // trust any CA cert ( use for DEVELOPMENT ONLY! )

        server = vertx.createHttpServer();
        routeMatcher = new RouteMatcher();

        routeMatcher.get("/unauthorized", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest event) {
                event.response().end("Not Authorized");
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
                event.response().end("Logged in");
            }
        });

        routeMatcher.get("/logout", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest event) {
                redirect(event, cas_redirectUrl + "/logout");
            }
        });

        routeMatcher.get("/login", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest event) {
                logger.info("login: " + event.uri());
                authenticate(event, "/loggedin");
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
                authenticate(event, "/someservice",new Handler<HttpServerRequest>() {
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
        logger.info("Connected to CAS");
        logger.info("   casHost: " + casHost);
        logger.info("   casPort: " + casPort);
        logger.info("   ssl: " + ssl);
        logger.info("   trustAll: " + trustAll);

    }

    // redirect request to path
    public static void redirect(HttpServerRequest request, String path) {
        request.response().headers().set("Location", path);
        request.response().setStatusCode(302);
        request.response().end();
    }


    /**
     * Take the request, path and authenticate it, then either redirect to the path or call the callback handler.
     *
     * @param event<HttpServerRequest> the http request object
     * @param path<String> the url or "service" requesting access to
     * @param redirectToPath<Boolean> weather or not to redirect to path upon success
     *                               if this is false, it will check if callback is NOT null.
     * @param callback<Handler<HttpServerRequest>> the callback to call if success
     *                                           if this is NOT null AND redirectToPath is false, the callback
     *                                           is called.
     */
    public void authenticate(final HttpServerRequest event, final String path, final Boolean redirectToPath, final Handler<HttpServerRequest> callback) {
        String hostAddr = event.headers().get("host");
        String serviceURL = path;
        try {
            serviceURL = URLEncoder.encode("http://" + hostAddr + path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        logger.info("ServiceURL: " + serviceURL);

        if (!event.params().contains("ticket")) {
            logger.info("no ticket, redirecting to cas");
            redirect(event, cas_redirectUrl + "/login?service=" + serviceURL);
            return;
        }

        String ticket = event.params().get("ticket");
        HttpClient request = casClient.getNow(cas_redirectUrl + "/validate?ticket=" + ticket + "&service=" + serviceURL, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse resp) {
                if (event.response().getStatusCode() != 200) {
                    logger.info("invalid ticket");
                    redirect(event, "/fail");
                }

                resp.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer body) {
                        String[] data = null;
                        String result = null;
                        try {
                            data = new String(body.getBytes(), "UTF-8").split("\n");
                            logger.info(new String(body.getBytes(), Charset.forName("UTF-8")));
                            result = data[0];
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            redirect(event, "/fail");
                        }

                        if (!result.equalsIgnoreCase("yes")) {
                            logger.info("not authorized");
                            redirect(event, "/unauthorized");
                        } else {
                            logger.info("redirect to " + path);
                            if (redirectToPath.equals(true)) {
                                logger.info("redirecting to " + path);
                                redirect(event, path);
                            } else if (!callback.equals(null)) {
                                logger.info("calling callback");
                                callback.handle(event);
                            }
                            return;
                        }
                    }
                });

            }
        });
    }

    /**
     * authenticate with a request, path and then call the callback upon success
     *
     * @param event
     * @param path
     * @param callback
     */
    public void authenticate(final HttpServerRequest event, final String path, final Handler<HttpServerRequest> callback) {
        authenticate(event, path, false, callback);
    }

    /**
     * authenticate with a request and path only, redirects to path upon success
     *
     * @param event
     * @param path
     */
    public void authenticate(final HttpServerRequest event, final String path) {
        authenticate(event, path, true, null);
    }
}
