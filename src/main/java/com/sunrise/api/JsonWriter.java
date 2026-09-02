package com.sunrise.api;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

/**
 * Builds JSON by hand for the web services.
 *
 * <p>The brief asks for plain Java with no frameworks, so instead of pulling
 * in a JSON library the responses are written with this small class. It is
 * about a hundred lines, it escapes correctly, and it keeps the dependency
 * list short.</p>
 *
 * <p>Escaping is the part that matters. A patient address containing a
 * quotation mark, or a name containing a backslash, would otherwise produce
 * JSON the browser cannot parse.</p>
 *
 * <pre>
 * String json = new JsonWriter()
 *         .beginObject()
 *             .name("id").value(1)
 *             .name("name").value("Dr. Anura")
 *         .endObject()
 *         .toJson();
 * </pre>
 */
public final class JsonWriter {

    private final StringBuilder out = new StringBuilder();

    /** Whether a comma is needed before the next token. */
    private boolean needComma;

    public JsonWriter beginObject() {
        separate();
        out.append('{');
        needComma = false;
        return this;
    }

    public JsonWriter endObject() {
        out.append('}');
        needComma = true;
        return this;
    }

    public JsonWriter beginArray() {
        separate();
        out.append('[');
        needComma = false;
        return this;
    }

    public JsonWriter endArray() {
        out.append(']');
        needComma = true;
        return this;
    }

    /** Writes a property name, ready for the value that follows. */
    public JsonWriter name(String name) {
        separate();
        out.append('"').append(escape(name)).append("\":");
        needComma = false;
        return this;
    }

    public JsonWriter value(String text) {
        separate();
        if (text == null) {
            out.append("null");
        } else {
            out.append('"').append(escape(text)).append('"');
        }
        needComma = true;
        return this;
    }

    public JsonWriter value(long number) {
        separate();
        out.append(number);
        needComma = true;
        return this;
    }

    public JsonWriter value(boolean flag) {
        separate();
        out.append(flag);
        needComma = true;
        return this;
    }

    /** Money is written unquoted with its own scale, for example 1500.00. */
    public JsonWriter value(BigDecimal amount) {
        separate();
        out.append(amount == null ? "null" : amount.toPlainString());
        needComma = true;
        return this;
    }

    /** @return the finished JSON document */
    public String toJson() {
        return out.toString();
    }

    private void separate() {
        if (needComma) {
            out.append(',');
        }
    }

    /**
     * Escapes the characters JSON does not allow raw inside a string.
     * Control characters are written in the {@code \\u00XX} form.
     */
    private static String escape(String text) {
        StringBuilder escaped = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            switch (character) {
                case '"'  -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    // -----------------------------------------------------------------
    //  sending a response
    // -----------------------------------------------------------------

    /**
     * Sends a JSON document with the given HTTP status.
     *
     * <p>The status carries meaning for the caller: 200 for a result, 201
     * when something was created, 400 for invalid input, 404 when nothing
     * matched and 409 when the request conflicts with what is already
     * stored, such as a time slot that is taken.</p>
     */
    public static void send(HttpServletResponse response, int status, String json)
            throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");

        try (PrintWriter writer = response.getWriter()) {
            writer.write(json);
        }
    }

    /** Sends {@code {"error": "..."}} with the given status. */
    public static void sendError(HttpServletResponse response, int status, String message)
            throws IOException {

        send(response, status, new JsonWriter()
                .beginObject()
                    .name("error").value(message)
                .endObject()
                .toJson());
    }
}
