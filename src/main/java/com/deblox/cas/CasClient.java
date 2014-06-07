package com.deblox.cas;

import org.vertx.java.core.http.HttpClient;

/**
 * Created by keghol on 6/7/14.
 *
 * Static class for holding the CAS client's HTTP connection and redirection URL
 *
 */
public class CasClient {
    public static HttpClient casClient;
    public static String casRedirectURL;

    public static HttpClient getCasClient() {
        return casClient;
    }

    public static void setCasClient(HttpClient casClient) {
        CasClient.casClient = casClient;
    }

    public static String getCasRedirectURL() {
        return casRedirectURL;
    }

    public static void setCasRedirectURL(String casRedirectURL) {
        CasClient.casRedirectURL = casRedirectURL;
    }
}
