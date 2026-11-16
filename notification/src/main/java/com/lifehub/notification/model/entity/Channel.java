package com.lifehub.notification.model.entity;

/** Where a notification is delivered. In-app is always stored; the others are deliveries on top. */
public enum Channel {
    IN_APP,
    EMAIL
}
