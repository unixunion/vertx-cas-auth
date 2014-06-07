package com.deblox.cas;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by Kegan Holtzhausen on 6/7/14.
 *
 * a static CasRequestClients, which determines if a request is authenticated and if not does the usual CAS
 * redirections and ticket / token handling. init as follows Verticle
 *
 * set the login redirection CAS url

 CasRequestClient.setCas_redirectUrl("https://localhost:8443/cas);

 * make a HttpClient connection to the CAS server within your Verticle

HttpClient casClient = vertx.createHttpClient()
 .setPort(8443)
 .setHost("localhost")
 .setKeepAlive(true)
 .setSSL(true)
 .setTrustAll(true);

 * set the CasRequestClients connection to the HttpClient created above.

 CasRequestClient.setCasClient(casClient);

 * authenticating with a routeMatcher

 // /login authenticate redirect to /loggedin
 routeMatcher.get("/login", new Handler<HttpServerRequest>() {
    @Override
    public void handle(HttpServerRequest event) {
        logger.info("login: " + event.uri());
        CasRequestClient.authenticate(event, "/loggedin");
    }
 });

 // authenticating with a callback upon success
 routeMatcher.get("/someservice", new Handler<HttpServerRequest>() {
    @Override
    public void handle(HttpServerRequest event) {
        CasRequestClient.authenticate(event, "/someservice",new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest event) {
                    event.response().end("this is the secured area!");
            }
        });
    }
});

 *
 *
 */

public class CasRequestClient {

    static String cas_redirectUrl;
    static HttpClient casClient;

    public static String getCas_redirectUrl() {
        return cas_redirectUrl;
    }

    public static void setCas_redirectUrl(String cas_redirectUrl) {
        CasRequestClient.cas_redirectUrl = cas_redirectUrl;
    }

    public static HttpClient getCasClient() {
        return casClient;
    }

    public static void setCasClient(HttpClient casClient) {
        CasRequestClient.casClient = casClient;
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
    public static void authenticate(final HttpServerRequest event, final String path, final Boolean redirectToPath, final Handler<HttpServerRequest> callback) {
        String hostAddr = event.headers().get("host");
        String serviceURL = path;
        final String sessionId = SessionStorage.getOrCreateSessionId(event);
        final Boolean sessionAuth = (Boolean)SessionStorage.get(sessionId, "auth");

        try {
            serviceURL = URLEncoder.encode("http://" + hostAddr + path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (!event.params().contains("ticket") && sessionAuth==null) {
            redirect(event, cas_redirectUrl + "/login?service=" + serviceURL);
            return;
        }

        if (sessionAuth!=null && sessionAuth.equals(true)) {
            redirect(event, path, redirectToPath, callback);
            return;
        }

        String ticket = event.params().get("ticket");

        HttpClient request = casClient.getNow(cas_redirectUrl + "/validate?ticket=" + ticket + "&service=" + serviceURL, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse resp) {
                if (event.response().getStatusCode() != 200) {
                    redirect(event, "/fail");
                }

                resp.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer body) {
                        String[] data = null;
                        String result = null;
                        try {
                            data = new String(body.getBytes(), "UTF-8").split("\n");
                            result = data[0];
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            redirect(event, "/fail");
                            return;
                        }

                        if (!result.equalsIgnoreCase("yes")) {
                            // try if a ticket in the request, try redirect without the ticket, if no ticket, fail!
                            if (event.params().contains("ticket")) {
                                redirect(event, event.path());
                                return;
                            } else {
                                redirect(event, "/unauthorized");
                                return;
                            }

                        } else {
                            SessionStorage.remove("auth");
                            SessionStorage.save(sessionId, "auth", true);
                            redirect(event, path, redirectToPath, callback);
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
    public static void authenticate(final HttpServerRequest event, final String path, final Handler<HttpServerRequest> callback) {
        authenticate(event, path, false, callback);
    }

    /**
     * authenticate with a request and path only, redirects to path upon success
     *
     * @param event
     * @param path
     */
    public static void authenticate(final HttpServerRequest event, final String path) {
        authenticate(event, path, true, null);
    }
}
