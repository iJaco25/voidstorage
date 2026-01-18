package net.momo.silo.interaction;

/** Result of handling an interaction. */
public sealed interface InteractionResult {

    /** Interaction was handled successfully. */
    record Success() implements InteractionResult {}

    /** Interaction was skipped (preconditions not met). */
    record Skipped(String reason) implements InteractionResult {}

    /** Interaction failed with an error. */
    record Failed(String error) implements InteractionResult {}

    static InteractionResult success() {
        return new Success();
    }

    static InteractionResult skipped(String reason) {
        return new Skipped(reason);
    }

    static InteractionResult failed(String error) {
        return new Failed(error);
    }

    default boolean isSuccess() {
        return this instanceof Success;
    }

    default boolean isSkipped() {
        return this instanceof Skipped;
    }

    default boolean isFailed() {
        return this instanceof Failed;
    }
}
