package com.deblox.tasks;

import com.deblox.utils.Util;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.testtools.TestVerticle;

import java.io.IOException;
import java.util.UUID;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;
import static org.vertx.testtools.VertxAssert.assertTrue;
import static org.vertx.testtools.VertxAssert.testComplete;

/**
 * Created by keghol on 6/9/14.
 */
public class TaskVerticleTests extends TestVerticle {

    JsonObject config;
    Logger logger;
    EventBus eb;

    String newTaskAddress = "tasks.newtask";
    String getTaskAddress = "tasks.task";

    @Override
    public void start() {
        initialize();
        eb = vertx.eventBus();
        config = new JsonObject();
        logger = container.logger();

        container.deployVerticle("com.deblox.tasks.TaskVerticle", new Handler<AsyncResult<String>>() {
            @Override
            public void handle(AsyncResult<String> event) {
                logger.info("event: " + event.toString());
                assertTrue(event.succeeded());
                if (event.failed()) {
                    logger.error(event.cause());
                } else {

                    JsonObject couchConfig = null;
                    try {
                        couchConfig = Util.loadConfig(this, "/couch-conf.json");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    container.deployModule("com.deblox~mod-couchbase~0.0.2-SNAPSHOT", couchConfig, 1, new AsyncResultHandler<String>() {

                                @Override
                                public void handle(AsyncResult<String> event) {
                                    if (!event.failed()) {
                                        startTests();
                                    } else {
                                        logger.error("didnt deploy couchbase module");
                                    }
                                }
                            }
                    );



                }
            }
        });
    }


    public JsonObject newJsonTask() {
        JsonObject request = new JsonObject().putString("taskName", "verifyInternationalPhoneNumber")
                .putString("documentId", "DOC123")
                .putString("appId", "AID123");
        return request;
    }

    @Test
    public void newTask() {
        eb.send(newTaskAddress, newJsonTask(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                assertNotNull(UUID.fromString(event.body().getString("key")));
                testComplete();
            }
        });
    }

    @Test
    public void newTaskError() {
        JsonObject jo = newJsonTask();
        jo.removeField("taskName");

        eb.send(newTaskAddress, jo, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                System.out.println("Response: " +event.body());
                assertEquals(event.body().getString("status"), "error");
                testComplete();
            }
        });

    }


    /**
     * get a task by its utid
     */
    @Test
    public void getTask() {
        JsonObject request =  new JsonObject().putString("key", "cbcdf904-72e2-46ac-9ff2-d13c5e80d84f");

        eb.send(getTaskAddress, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                System.out.println("response: " + event.body().toString());

                Task f = Task.deserialize(event.body());

                System.out.println(f.toString());
                testComplete();
            }
        });
    }



}
