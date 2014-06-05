package com.deblox.cas;

/**
 * Created by keghol on 6/3/14.
 */
public interface Constants {

    public static final String HOST_HEADER = "Host";

    public static final String CONTENT_TYPE_HEADER = "Content-Type";

    public static final String VERTX_SESSION_COOKIE = "VXSESSIONID";

    public final static String SESSION_ID = "debloxSessionId";

    public final static String REQUESTED_URL = "debloxRequestedUrl";

    public final static String SEPARATOR = "::"; // this is used in regexes, so dont use special chars like $

    public final static Integer PROFILE_TIMEOUT = 15000; // in milliseconds

}
