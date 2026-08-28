package dev.corexinc.corex.engine.registry;

/**
 * One argument position declared by a command's syntax string.
 *
 * @param kind         whether the argument is positional, prefixed or a bare flag
 * @param name         the readable name: the prefix for {@code PREFIX}, the flag for
 *                     {@code FLAG}, the text inside {@code <>} for {@code LINEAR}
 * @param linearIndex  position among the positional arguments, or -1
 * @param required     declared with {@code []} rather than {@code ()}
 * @param defaultRaw   text after {@code =} inside the token, or null
 */
public record SyntaxSlot(Kind kind, String name, int linearIndex, boolean required, String defaultRaw) {

    public enum Kind { LINEAR, PREFIX, FLAG }

    public boolean isLinear() {
        return kind == Kind.LINEAR;
    }

    public boolean isFlag() {
        return kind == Kind.FLAG;
    }

    /** How this slot reads back in an error message, close to how it was written. */
    public String describe() {
        String body = switch (kind) {
            case LINEAR -> "<" + name + ">";
            case PREFIX -> name + ":<value>";
            case FLAG -> name;
        };
        if (defaultRaw != null) body += "=" + defaultRaw;
        return required ? "[" + body + "]" : "(" + body + ")";
    }
}
