package com.deblox.events;

import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by keghol on 6/10/14.
 */
public class ReplyHandler {

    public static void sendError(Message<JsonObject> message, String error, Exception e) {
        JsonObject json = new JsonObject().putString("status", "error").putString("message", error);
        message.reply(json);
    }

}
