package net.momo.silo.util;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/** Result type for operations that can fail. */
public final class Result<T> {

    private final T value;
    private final String error;
    private final boolean success;

    private Result(T value, String error, boolean success) {
        this.value = value;
        this.error = error;
        this.success = success;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, null, true);
    }

    public static <T> Result<T> failure(String error) {
        return new Result<>(null, Objects.requireNonNull(error), false);
    }

    public boolean isSuccess() { return success; }
    public boolean isFailure() { return !success; }

    public T value() {
        if (!success) throw new IllegalStateException("Result is error: " + error);
        return value;
    }

    public String error() {
        if (success) throw new IllegalStateException("Result is success");
        return error;
    }

    public T orElse(T defaultValue) {
        return success ? value : defaultValue;
    }

    public <U> Result<U> map(Function<T, U> mapper) {
        if (success) {
            return Result.success(mapper.apply(value));
        }
        return Result.failure(error);
    }

    public void ifSuccess(Consumer<T> consumer) {
        if (success) consumer.accept(value);
    }

    public void ifFailure(Consumer<String> consumer) {
        if (!success) consumer.accept(error);
    }
}
