package com.pandora.work;

/** OpenAPI v0 routes and enums from the shared project contract. */
public final class ApiContract {
    private ApiContract() {}
    public static final String BASE_PATH = "/api/v1";
    public static final String LOGIN = "/auth/login";
    public static final String REGISTER = "/auth/register";
    public static final String LOG_INTEGRITY = "/logs/integrity";
    public static final String LOGS = "/logs";
    public static final String PANEL_MAP = "/panels/map";
    public static final String TASKS = "/tasks"; // Sprint 2 contract only.
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_LEADER = "LEADER";
    public static final String ROLE_STAFF = "STAFF";
}
