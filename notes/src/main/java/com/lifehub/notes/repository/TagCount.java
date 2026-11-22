package com.lifehub.notes.repository;

/** How many visible notes or bookmarks carry a tag. Top level, so JPQL can construct it. */
public record TagCount(String tag, long count) {
}
