package com.lifehub.notes.model.dto;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** The tag rules shared by notes and bookmarks. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Tags {

    /** Letters, digits, spaces and hyphens, in any script. */
    public static final String PATTERN = "[\\p{L}\\p{N} -]+";
    public static final String MESSAGE = "may contain only letters, digits, spaces and hyphens";

    /** Lower case, trimmed, inner spaces collapsed, duplicates dropped, sorted. */
    public static Set<String> normalise(Collection<String> tags) {
        Set<String> out = new TreeSet<>();
        if (tags != null) {
            for (String tag : tags) {
                String t = tag.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
                if (!t.isEmpty()) {
                    out.add(t);
                }
            }
        }
        return out;
    }
}
