package com.lifehub.starter.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class IdsTest {

    @Test
    void generatesVersion7Ids() {
        assertThat(Ids.newId().version()).isEqualTo(7);
    }

    @Test
    void idsGeneratedInSequenceSortInCreationOrder() {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            ids.add(Ids.newId());
        }
        // Compare as unsigned strings, which is how MySQL orders BINARY(16).
        assertThat(ids.stream().map(UUID::toString).toList()).isSorted();
    }

    @Test
    void roundTripsThroughBytes() {
        UUID id = Ids.newId();
        byte[] bytes = Ids.toBytes(id);
        assertThat(bytes).hasSize(16);
        assertThat(Ids.fromBytes(bytes)).isEqualTo(id);
    }

    @Test
    void rejectsWrongLength() {
        assertThatThrownBy(() -> Ids.fromBytes(new byte[8])).isInstanceOf(IllegalArgumentException.class);
    }
}
