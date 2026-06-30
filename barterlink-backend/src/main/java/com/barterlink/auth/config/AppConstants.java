package com.barterlink.auth.config;

public class AppConstants {

    public static final String[] AUTH_PUBLIC_URLS =
            {
            "/api/v1/auth/**",

            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
            };

    public static final String[] AUTH_ADMIN_URLS = {
      "/api/v1/users/**"
    };

    public static final String[] AUTH_GUEST_URLS = {
            "HttpMethod.GET"
    };

    public static final String ADMIN_ROLE = "ADMIN";
    public static final String GUEST_ROLE = "GUEST";


    //other project related constants

}
