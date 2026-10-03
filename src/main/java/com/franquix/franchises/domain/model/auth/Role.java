package com.franquix.franchises.domain.model.auth;

/** What a user may do. */
public enum Role {

    /** Full access: every franchise operation and user management. */
    ADMIN,

    /** Read-only access to franchises, branches and products. */
    VIEWER
}
