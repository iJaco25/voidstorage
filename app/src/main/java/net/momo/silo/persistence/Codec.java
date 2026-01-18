package net.momo.silo.persistence;

import com.google.gson.JsonElement;

/** Serialization/deserialization contract for a type. */
public interface Codec<T> {

    /** Serializes the value to JSON. */
    JsonElement serialize(T value);

    /** Deserializes the value from JSON. */
    T deserialize(JsonElement json);

    /** Creates a codec from serialize/deserialize functions. */
    static <T> Codec<T> of(Serializer<T> serializer, Deserializer<T> deserializer) {
        return new Codec<>() {
            @Override
            public JsonElement serialize(T value) {
                return serializer.serialize(value);
            }

            @Override
            public T deserialize(JsonElement json) {
                return deserializer.deserialize(json);
            }
        };
    }

    @FunctionalInterface
    interface Serializer<T> {
        JsonElement serialize(T value);
    }

    @FunctionalInterface
    interface Deserializer<T> {
        T deserialize(JsonElement json);
    }
}
