package com.deblox.cas;

import com.jetdrone.vertx.yoke.middleware.Router;
import com.jetdrone.vertx.yoke.middleware.YokeRequest;
import org.jetbrains.annotations.NotNull;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by keghol on 6/7/14.
 *
 * CAS version of Yoke Router
 *
 */
public class AuthRouter extends Router {

    @Override
    public void handle(@NotNull final YokeRequest request, @NotNull final Handler<Object> callbackHandler) {

        String hostAddr = request.headers().get("host");
        String serviceURL = request.path();
        System.out.println("ServiceURL: " + serviceURL);

        final String sessionId = SessionStorage.getOrCreateSessionId(request);
        final Boolean sessionAuth = (Boolean)SessionStorage.get(sessionId, "auth");

        try {
            serviceURL = URLEncoder.encode("http://" + hostAddr + request.path(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (!request.params().contains("ticket") && sessionAuth==null) {
            System.out.println("no ticket, no sessionAuth, checking in with CAS: " + CasClient.getCasRedirectURL());
            System.out.println("URL " + CasClient.getCasRedirectURL() + "/login?service=" + serviceURL);
            redirect(request, CasClient.getCasRedirectURL() + "/login?service=" + serviceURL);
            return;
        }

        if (sessionAuth!=null && sessionAuth.equals(true)) {
            System.out.println("already authenticated, passing through");
            request.params().add(Constants.USER_ID, SessionStorage.get(sessionId, Constants.USER_ID).toString());
            super.handle(request, callbackHandler);
            return;
        }

        String ticket = request.params().get("ticket");

        HttpClient casRequest = CasClient.getCasClient().getNow(CasClient.getCasRedirectURL() + "/validate?ticket=" + ticket + "&service=" + serviceURL, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse resp) {
                if (request.response().getStatusCode() != 200) {
                    System.out.println("failure");
                    redirect(request, "/casfail");
                }

                resp.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer body) {
                        String[] data = null;
                        String result = null;
                        try {
                            data = new String(body.getBytes(), "UTF-8").split("\n");
                            result = data[0];
                            System.out.println("result " + data[0]);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            redirect(request, "/casfail");
                            return;
                        }

                        if (!result.equalsIgnoreCase("yes")) {
                            // try if a ticket in the request, try redirect without the ticket, if no ticket, fail!
                            if (request.params().contains("ticket")) {
                                System.out.println("ticket in url, might be old, removing ticket");
                                redirect(request, request.path());
                                return;
                            } else {
                                System.out.println("no ticket, redirect to /unauthorized");
                                redirect(request, "/unauthorized");
                                return;
                            }

                        } else {
                            // this is an authorized request, save its auth bool in the sessionstore
                            System.out.println("everything is authenticated");
                            SessionStorage.remove("auth");
                            SessionStorage.save(sessionId, "auth", true);
                            SessionStorage.save(sessionId, "username", data[1]);
                            request.params().add(Constants.USER_ID, data[1]);
                            AuthRouter.super.handle(request, callbackHandler);
                            return;
                        }
                    }
                });

            }
        });


//        super.handle(request, next);
    }

    /**
     * redirect the request to the path specified
     *
     * @param request
     * @param path
     */
    public static void redirect(HttpServerRequest request, String path) {
        request.response().headers().set("Location", path);
        request.response().setStatusCode(302);
        request.response().end();
    }

    /**
     * redirect the request to the path OR callback depending on which are set
     * @param request
     * @param path
     * @param redirectToPath
     * @param callback
     */
    public static void redirect(HttpServerRequest request, String path, Boolean redirectToPath, final Handler<Object> callback) {
        if (redirectToPath.equals(true)) {
            redirect(request, path);
        } else if (!callback.equals(null)) {
            callback.handle(request);
        }
    }

}
