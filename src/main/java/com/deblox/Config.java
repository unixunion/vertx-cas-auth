package com.deblox;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.regex.Pattern;

/**
 * Created by keghol on 6/9/14.
 *
 * Store application configurations
 *
 */
public class Config {
    public final String casHost;
    public final int casPort;
    public final boolean keepAlive;
    public final boolean ssl;
    public final boolean trustAll;
    public final String cas_redirectUrl;
    public final String webroot;
    public final Domain[] domains;

    public static class Domain {
        public final String name;
        public final String namespace;
        public final Pattern pattern;

        Domain(String name, String namespace) {
            this.name = name;
            this.namespace = namespace;
            // Make "*" match expand match and limit range of match
            // Case insensitive matching
            pattern = Pattern.compile("^(" + name.replaceAll("\\.", "\\.").replaceAll("\\*", ".*") + ")$", Pattern.CASE_INSENSITIVE);
        }
    }
    public Config(JsonObject config) {
        casHost = config.getString("casHost", "localhost"); // cas server hostname
        casPort = (int)config.getNumber("casPort", 8443); // port on the CAS server side.
        keepAlive = config.getBoolean("keepAlive", false);  // keep connection to CAS alive or not.
        ssl = config.getBoolean("ssl", false); // use SSL to talk to the CAS server
        trustAll = config.getBoolean("trustAll", false); // trust any CA cert ( use for DEVELOPMENT ONLY! )
        cas_redirectUrl = config.getString("cas_redirectUrl");
        webroot = config.getString("webroot"); // the webroot either in the classpath or Fully qualified path
        JsonArray domains = config.getArray("domains");

        if (domains != null) {
            this.domains = new Domain[domains.size()];
            for (int i = 0; i < domains.size(); i++) {
                JsonObject domain = domains.get(i);
                String name = domain.getString("name");
                String namespace = domain.getString("namespace");
                this.domains[i] = new Domain(name, namespace);
            }
        } else {
            this.domains = new Domain[1];
            // Example catch all
            // Has the same namespace as the previous domain, so they share data
            // AdminRouter login has been disabled
            this.domains[0] = new Domain("*", "default");
        }
    }
}
