package com.lifehub.notes.model.entity;

/** Who can see a note or bookmark in a household workspace. */
public enum Visibility {
    /** Only the person who wrote it. Everyone else gets the same 404 as for something that does not exist. */
    PERSONAL,
    /** Every member of the workspace. */
    SHARED
}
