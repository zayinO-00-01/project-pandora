package com.projectpandora.api.user;

public enum Role {
    ADMIN,
    LEADER,
    FOUNDER,
    DEPT_HEAD,
    TEAM_LEAD,
    STAFF

    ;
    public int level() {
        return switch(this) {case FOUNDER -> 3; case DEPT_HEAD -> 2; case TEAM_LEAD, LEADER -> 1; case STAFF -> 0; case ADMIN -> 4;};
    }
}
