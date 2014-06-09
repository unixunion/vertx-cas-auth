package com.deblox.cms;

import com.deblox.Config;
import com.deblox.cas.AuthRouter;
import com.deblox.cas.CasClient;
import com.deblox.cas.Constants;
import com.deblox.cas.SessionStorage;
import com.jetdrone.vertx.yoke.Middleware;
import com.jetdrone.vertx.yoke.Yoke;
import com.jetdrone.vertx.yoke.engine.StringPlaceholderEngine;
import com.jetdrone.vertx.yoke.middleware.Favicon;
import com.jetdrone.vertx.yoke.middleware.Router;
import com.jetdrone.vertx.yoke.middleware.Static;
import com.jetdrone.vertx.yoke.middleware.YokeRequest;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by keghol on 6/8/14.
 */

public class Cms extends Verticle {
    Logger logger;
    HttpClient casClient;
    Yoke yoke;

    /**
     * start the Verticle and the http listener
     */
    public void start() {
        logger = container.logger();
        logger.info("Starting CAS service");

        // instantiate config
        final Config config = new Config(container.config());


        if (Files.notExists(Paths.get(config.webroot))) {
            logger.error("unable access webroot:" + config.webroot);
        }

        // push the CAS client to the CasClient static casClient
        logger.info("Connecting to CAS");
        casClient = vertx.createHttpClient()
                .setPort(config.casPort)
                .setHost(config.casHost)
                .setKeepAlive(config.keepAlive)
                .setSSL(config.ssl)
                .setTrustAll(config.trustAll);
        CasClient.setCasClient(casClient);
        CasClient.setCasRedirectURL(config.cas_redirectUrl);

        // Yoke server
        yoke = new Yoke(vertx)
                .use(new Favicon())
                .engine(new StringPlaceholderEngine())
                .use(new DomainMiddleware(config))
                .use(new Static(config.webroot))
                .use(new Router()
                        .all("/", new Middleware() {
                            @Override
                            public void handle(YokeRequest request, Handler<Object> next) {
                                request.put("somedata", "somevalue");
                                request.response().render("static/templates/index.shtml", next);
                            }
                        })
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
                .use(new AuthRouter()
                        .all("/authservice", new Handler<YokeRequest>() {
                            @Override
                            public void handle(YokeRequest event) {
                                event.response().end("login user: " + event.params().get(Constants.USER_ID));
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
