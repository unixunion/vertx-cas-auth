package com.deblox.tasks;

import junit.framework.Assert;
import org.junit.Test;
import org.vertx.java.core.json.JsonObject;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by keghol on 6/10/14.
 */
public class TaskTests {
    @Test
    public void testNewTask() {
        Task t = newTestTask();
        assertEquals("DID123", t.getDataDocumentId());
        assertEquals(t.stages.get(0).app, "telephonics");
        assertEquals(t.locked, false);
    }

    @Test
    public void testSerialize() {
        Task t = newTestTask();
        String s = Task.serialize(t);
        assertEquals(s.getClass(), String.class);
    }

    @Test
    public void testToJson() {
        Task t = newTestTask();
        JsonObject jo = Task.toJson(t);
        assertEquals(jo.getString("taskName"), "verifyInternationalPhoneNumber");
    }

    @Test
    public void testGetCurrentStage() {
        Task t = newTestTask();
        Stage s = t.getCurrentStage();
        assertEquals(s.app, "telephonics");
        assertEquals(s.callback, null);
        Assert.assertEquals(s.task, "validateNumber");
    }

    public static Task newTestTask() {
        Task t = null;
        try {
            t = new Task("verifyInternationalPhoneNumber", "DID123", "UAID123");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("new Task: " + t.toString());
        return t;
    }

}
