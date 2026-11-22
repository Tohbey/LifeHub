package com.lifehub.notes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.lifehub.notes.model.dto.Tags;
import com.lifehub.starter.web.ApiException;

class NotesRulesTest {

    @Test
    void tagsAreLowerCaseTrimmedAndUnique() {
        assertThat(Tags.normalise(Arrays.asList(" Bills ", "bills", "Home  Admin", "école")))
                .containsExactly("bills", "home admin", "école");
        assertThat(NoteServiceImpl.tagOrNull("  WiFi ")).isEqualTo("wifi");
        assertThat(NoteServiceImpl.tagOrNull(" ")).isNull();
    }

    @Test
    void onlyHttpLinksWithAHostAreSaved() {
        assertThat(BookmarkServiceImpl.url(" https://www.gov.uk/bin-collection ")).isEqualTo("https://www.gov.uk/bin-collection");
        for (String bad : new String[] { "javascript:alert(1)", "data:text/html,hi", "file:///etc/passwd", "https:///nohost" }) {
            assertThatThrownBy(() -> BookmarkServiceImpl.url(bad)).isInstanceOf(ApiException.class);
        }
    }

    @Test
    void aMissingTitleBecomesTheHost() {
        assertThat(BookmarkServiceImpl.title(" ", "https://www.gov.uk/x")).isEqualTo("www.gov.uk");
        assertThat(BookmarkServiceImpl.title("Bins", "https://www.gov.uk/x")).isEqualTo("Bins");
    }
}
