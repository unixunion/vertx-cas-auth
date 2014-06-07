package com.deblox.cas;

import com.jetdrone.vertx.yoke.Yoke;
import com.jetdrone.vertx.yoke.middleware.Favicon;
import com.jetdrone.vertx.yoke.middleware.Router;
import com.jetdrone.vertx.yoke.middleware.Static;
import com.jetdrone.vertx.yoke.middleware.YokeRequest;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * Created by Kegan Holtzhausen on 6/4/14.
 *
 * this is a simple CAS integration example in VertX.io + Yoke middleware
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
    Boolean keepAlive;
    Boolean ssl;
    Boolean trustAll;
    String cas_redirectUrl;
    JsonObject config = null;
    HttpClient casClient;
    Yoke yoke;

    /**
     * start the Verticle and the http listener
     */
    public void start() {
        logger = container.logger();
        logger.info("Starting CAS service");

        config = container.config();
        casHost = config.getString("casHost", "localhost"); // cas server hostname
        casPort = (int)config.getNumber("casPort", 8443); // port on the CAS server side.
        keepAlive = config.getBoolean("keepAlive", false);  // keep connection to CAS alive or not.
        ssl = config.getBoolean("ssl", false); // use SSL to talk to the CAS server
        trustAll = config.getBoolean("trustAll", false); // trust any CA cert ( use for DEVELOPMENT ONLY! )
        cas_redirectUrl = config.getString("cas_redirectUrl");

        // push the CAS client to the CasClient static casClient
        logger.info("Connecting to CAS");
        casClient = vertx.createHttpClient()
                .setPort(casPort)
                .setHost(casHost)
                .setKeepAlive(keepAlive)
                .setSSL(ssl)
                .setTrustAll(trustAll);
        CasClient.setCasClient(casClient);
        CasClient.setCasRedirectURL(cas_redirectUrl);

        // Yoke server
        yoke = new Yoke(vertx)
                .use(new Favicon())
                .use(new Static("webroot"))
                .use(new AuthRouter()
                                .all("/authservice", new Handler<YokeRequest>() {
                                    @Override
                                    public void handle(YokeRequest event) {
                                        event.response().end("login user: " + event.params().get(Constants.USER_ID));
                                    }
                                })
                )
                .use(new Router()
                        .all("/openservice", new Handler<YokeRequest>() {
                            @Override
                            public void handle(YokeRequest event) {
                                event.response().end("open service");
                            }
                        })
                        .all("/logout", new Handler<YokeRequest>() {
                            @Override
                            public void handle(YokeRequest event) {
                                event.response().redirect(CasClient.getCasRedirectURL() + "/logout");
                            }
                        })
                )
                .listen(3000);

        logger.info("Starting sessionExpiry timer");
        long timerID = vertx.setPeriodic(10000, new Handler<Long>() {
            public void handle(Long timerID) {
                logger.info("Cleaning out old sessions");
                SessionStorage.sessionCleanup();
            }
        });

    }

}
