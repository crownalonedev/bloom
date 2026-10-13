package io.github.crownalonedev.bloom.api.text;

/**
 * The markup language used to write a message.
 *
 * <p>Both formats work on every supported platform:
 *
 * <ul>
 *   <li>On modern Paper, {@link #MINIMESSAGE} is parsed by the real MiniMessage library and {@link #LEGACY} by
 *       Adventure's legacy serializer.
 *   <li>On legacy servers, {@link #LEGACY} is native, and {@link #MINIMESSAGE} is converted by Bloom's
 *       MiniMessage subset converter: named colours, hex colours (downsampled to the nearest legacy colour),
 *       decorations, {@code <reset>} and {@code <newline>}. Interactive tags (click, hover, insertion) and
 *       gradients are rendered as plain text content without their effect.
 * </ul>
 *
 * @since 0.1.0
 */
public enum TextFormat {
    /** Legacy colour codes with {@code &} (or {@code §}), e.g. {@code "&dHello &lworld"}. */
    LEGACY,
    /** MiniMessage tags, e.g. {@code "<light_purple>Hello <bold>world"}. */
    MINIMESSAGE
}
