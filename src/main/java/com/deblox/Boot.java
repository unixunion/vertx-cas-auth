package com.deblox;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Future;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Kegan Holtzhausen on 29/05/14.
 *
 * This loads the config and then starts the main application
 *
 */
public class Boot extends Verticle {

    JsonObject config;
    private Logger logger;

    @Override
    public void start(final Future<Void> startedResult) {
        logger = container.logger();
        config = this.getContainer().config();

        if (config.equals(new JsonObject())) {
            logger.info("loading default config");
            config = loadConfig(this, "/conf.json");
        }

        logger.info("Config: " + config.toString());
        logger.info("Booting " + config.getString("main"));

        container.deployVerticle(config.getString("main"), config ,new AsyncResultHandler<String>() {
            public void handle(AsyncResult<String> deployResult) {
                if (deployResult.succeeded()) {
                    startedResult.setResult(null);
                } else {
                    logger.error("error deploying module, " + deployResult.cause());
                    startedResult.setFailure(deployResult.cause());
                }
            }
        });
    }

    static public JsonObject loadConfig(Object o, String file) {
        try (InputStream stream = o.getClass().getResourceAsStream(file)) {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));

            String line = reader.readLine();
            while (line != null) {
                sb.append(line).append('\n');
                line = reader.readLine();
            }

            return new JsonObject(sb.toString());

        } catch (IOException e) {
            e.printStackTrace();
            return new JsonObject();
        }

    }

}