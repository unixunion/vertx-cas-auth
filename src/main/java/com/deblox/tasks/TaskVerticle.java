package com.deblox.tasks;

import com.deblox.Config;
import com.deblox.couch.Couch;
import com.deblox.events.ReplyHandler;
import com.deblox.utils.Util;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by keghol on 6/9/14.
 *
 * handles task operations as requested over the message bus
 *
 * newTask - creates a new task and persist it, accepts name, document and appid, returns utid
 *  can accept a string or json object representation over messagebus, see TaskVerticleTests
 * task - retrieves a task by utid
 * nextStage - bumps the stage by utid
 * getStage - returns the current stage information by utid
 * periodicCheck - periodically check through open tasks, lock them and do stuff
 *
 * listens on tasks.newtask for a new task, returns the couch result;
 *
 */
public class TaskVerticle extends BusModBase {

    private Handler<Message<JsonObject>> newTaskHandler;
    private Handler<Message<JsonObject>> taskHandler;
    private Logger logger;
    private Config config;
    private EventBus eb;


    public void start() {
        eb = vertx.eventBus();
        logger = container.logger();
        config = new Config(container.config());

        newTaskHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                newTask(event);
            }
        };
        eb.registerHandler("tasks.newtask", newTaskHandler);

        taskHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                task(event);
            }
        };
        eb.registerHandler("tasks.task", taskHandler);

    }


    /**
     * open the message and call newTask
     *
     * {
     *  "taskName": "foo",
     *  "documentId": "123",
     *  "appId": "dsa",
     * }
     *
     * @param message
     */
    public void newTask(final Message<JsonObject> message) {
        logger.info("Got message: " + message.body());

        // decode the Gson to Json
        JsonObject decoded_message = Util.decode(message);
        String taskName = decoded_message.getString("taskName", null); // the name of the task, must exist in resources/tasks
        String documentId = decoded_message.getString("documentId", null); // the couch document this applies to
        String appId = decoded_message.getString("appId", null); // the application this task belongs to, tasks can be shared by apps

        final Task task;

       try {
           task = newTask(taskName, documentId, appId);
           updateTask(task, new Handler<Message>() {
               @Override
               public void handle(Message event) {
                   sendOK(message, new JsonObject(event.body().toString()).getObject("response"));
               }
           }
           );

        } catch (Exception e) {
            ReplyHandler.sendError(message, "taskName, documentId and appId must be set", e);
        }
    }



    /**
     * get task by utid
     *
     * request:
     *
     * { "key": "}219b9762-00e5-4636-9bc0-a07003b63602" }
     *
     * response:
     *
     *
     *
     * @param message
     */
    public void task(final Message<JsonObject> message) {
        logger.info("got message:" + message.body());
        try {
            JsonObject decoded_message = Util.decode(message);
            String utid = decoded_message.getString("key");
            getTask(utid, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject event) {
                    sendOK(message, event);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            ReplyHandler.sendError(message, "unable to get task by utid", e);
        }
    }

    public void getTask(String utid, final Handler<JsonObject> callback) {
        JsonObject request = Couch.Get(utid);
        eb.send(config.couchAddress, request, new Handler<Message>() {
                    @Override
                    public void handle(Message event) {
                        JsonObject data = new JsonObject(event.body().toString())
                                .getObject("response")
                                .getObject("data");
                        callback.handle(data);
                    }
                }
        );

    }

    /**
     * creates a new task and returns a json version
     * @param taskName
     * @param documentId
     * @param appId
     * @return
     */
    public Task newTask(String taskName, String documentId, String appId) throws Exception{

        try {
            Task task = new Task(taskName, documentId, appId);
            return task;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Unable to instantiate task");
        }

    }


    /**
     * bump to next stage, call app responsible for stage with utid of document
     * @param utid
     */
    public void nextStage(String utid) {

        getTask(utid, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                Task t = Task.deserialize(event);
                Stage s = t.nextStage();

                // notify the app of the task and the document
                eb.send(s.app + "." + s.task, t.utid);

            }
        });

    }

    /**
     * create / update a task.
     * @param task
     * @param callback
     */
    public void updateTask(Task task, final Handler<Message> callback) {
        JsonObject request = Couch.Set(task.getUtid(), Task.toJson(task));

        // persist to Couch
        eb.send(config.couchAddress, request, new Handler<Message>() {
                    @Override
                    public void handle(Message event) {
                        callback.handle(event);
                    }
                }
        );

    }

}
