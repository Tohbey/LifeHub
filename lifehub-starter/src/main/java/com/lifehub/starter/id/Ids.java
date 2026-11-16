package com.lifehub.starter.id;

import java.nio.ByteBuffer;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The one place LifeHub generates identifiers. Every ID is a UUIDv7 (time-ordered), stored in MySQL as
 * {@code BINARY(16)}. See ADR 0004.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Ids {

    private static final TimeBasedEpochGenerator GENERATOR = Generators.timeBasedEpochGenerator();

    /** Returns a new UUIDv7. IDs generated later sort after earlier ones. */
    public static UUID newId() {
        return GENERATOR.generate();
    }

    /** Converts a UUID to the 16 bytes stored in a {@code BINARY(16)} column, for plain JDBC queries. */
    public static byte[] toBytes(UUID id) {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }

    /** Converts the 16 bytes of a {@code BINARY(16)} column back to a UUID. */
    public static UUID fromBytes(byte[] bytes) {
        if (bytes.length != 16) {
            throw new IllegalArgumentException("A UUID needs 16 bytes, got " + bytes.length);
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
