package com.lifehub.starter.membership;

/** Membership could not be checked. The request is denied (fail closed). */
public class MembershipUnavailableException extends RuntimeException {

    /** Wraps the failure that stopped the membership check. */
    public MembershipUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
