package net.momo.silo.util;

import com.hypixel.hytale.server.core.Message;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Fluent builder for multi-line messages with formatting. */
public final class MessageBuilder {

    private final List<Message> messages = new ArrayList<>();

    private MessageBuilder() {}

    public static MessageBuilder create() {
        return new MessageBuilder();
    }

    public MessageBuilder append(Message message) {
        messages.add(message);
        return this;
    }

    public MessageBuilder append(String text) {
        messages.add(Message.raw(text));
        return this;
    }

    public MessageBuilder line() {
        messages.add(Message.raw("\n"));
        return this;
    }

    public MessageBuilder separator() {
        messages.add(Message.raw("─────────────────").color(Color.DARK_GRAY));
        return line();
    }

    public MessageBuilder label(String key, String value) {
        messages.add(Message.raw(key + ": ").color(new Color(0x93, 0x84, 0x4c)).bold(true));
        messages.add(Message.raw(value));
        return line();
    }

    public MessageBuilder label(String key, long value) {
        return label(key, String.valueOf(value));
    }

    public Message build() {
        return Message.join(messages.toArray(new Message[0]));
    }
}
