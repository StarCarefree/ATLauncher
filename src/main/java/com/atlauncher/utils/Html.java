/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2026 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML a platform sent, made safe to show and to quote as prose.
 *
 * <p>
 * CurseForge descriptions arrive as HTML. Swing's editor kit will load every {@code <img>}, honour
 * every inline colour, and hang on an {@code <iframe>} - none of which a themed dialog can use.
 * Markdown is already flattened by {@link Markdown}; this is the same job for the other half of
 * what a pack description can be.
 *
 * <p>
 * Links are the piece that used to vanish. Authors wrap a picture in {@code <a href>}, or point
 * {@code href} at {@code /linkout?remoteUrl=...} relative to CurseForge. Stripping the image left
 * an empty tag, and a pane with no document base turned the relative address into a {@code file:}
 * URL that went nowhere.
 */
public final class Html {
    /**
     * Elements Swing cannot paint, or that would only stall the pane while they tried.
     */
    private static final String DROP_BLOCKS =
            "(?is)<(script|style|iframe|object|embed|svg|video|audio)\\b[^>]*>.*?</\\1>";

    private static final String DROP_EMPTY_BLOCKS =
            "(?is)<(script|style|iframe|object|embed|svg|video|audio)\\b[^>]*/?>";

    private static final Pattern ANCHOR = Pattern.compile("(?is)<a\\b([^>]*)>(.*?)</a>");

    private static final Pattern HREF =
            Pattern.compile("(?i)\\bhref\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))");

    /** The only platform that ships HTML, so a leading slash is a path on this origin. */
    private static final String CURSEFORGE_ORIGIN = "https://www.curseforge.com";

    private Html() {
    }

    /**
     * Whether the author (or the platform) already wrote HTML, so it must not go through a
     * Markdown renderer - that would escape the tags and show them as text.
     */
    public static boolean looksLike(String text) {
        return text != null && text.matches(
                "(?is).*<(p|br|div|span|ul|ol|li|h[1-6]|strong|em|b|i|a\\s|table|img|pre|blockquote)[^>]*>.*");
    }

    /**
     * Drops what the editor kit cannot show, and what would fight the theme.
     *
     * <p>
     * Images become their alt text - the Markdown path already drops pictures, and JEditorPane
     * fetching them on the event thread is how a description dialog used to freeze. Inline colour
     * and {@code <font>} are how CurseForge authors painted white text on a dark card.
     *
     * <p>
     * Anchors are rewritten rather than left as they arrived: a relative {@code href} is resolved,
     * and a link that only wrapped a picture keeps the address as its text so it still has
     * something to click.
     */
    public static String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        String cleaned = html.replaceAll(DROP_BLOCKS, "");
        cleaned = cleaned.replaceAll(DROP_EMPTY_BLOCKS, "");
        cleaned = rewriteAnchors(cleaned, false);
        cleaned = replaceImages(cleaned);
        cleaned = cleaned.replaceAll("(?i)\\sstyle\\s*=\\s*\"[^\"]*\"", "");
        cleaned = cleaned.replaceAll("(?i)\\sstyle\\s*=\\s*'[^']*'", "");
        cleaned = cleaned.replaceAll("(?i)\\scolor\\s*=\\s*\"[^\"]*\"", "");
        cleaned = cleaned.replaceAll("(?i)\\scolor\\s*=\\s*'[^']*'", "");
        cleaned = cleaned.replaceAll("(?i)\\sbgcolor\\s*=\\s*\"[^\"]*\"", "");
        cleaned = cleaned.replaceAll("(?i)\\sbgcolor\\s*=\\s*'[^']*'", "");
        cleaned = cleaned.replaceAll("(?i)</?font\\b[^>]*>", "");

        return cleaned;
    }

    /**
     * Turns raw HTML {@code <a href>} tags into Markdown links so a document that is otherwise
     * Markdown can go through {@link Markdown} without the tags being escaped into visible source.
     */
    public static String anchorsToMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return rewriteAnchors(text, true);
    }

    /**
     * An href that a click can actually open. Relative CurseForge paths and protocol-relative
     * addresses become https; {@code javascript:} and in-page hashes are dropped.
     */
    public static String resolveHref(String href) {
        if (href == null) {
            return null;
        }

        String value = unescape(href.trim());

        if (value.isEmpty() || value.charAt(0) == '#') {
            return null;
        }

        if (startsWithIgnoreCase(value, "javascript:") || startsWithIgnoreCase(value, "data:")
                || startsWithIgnoreCase(value, "file:")) {
            return null;
        }

        if (value.startsWith("//")) {
            return "https:" + value;
        }

        if (value.startsWith("/")) {
            return CURSEFORGE_ORIGIN + value;
        }

        if (startsWithIgnoreCase(value, "http://") || startsWithIgnoreCase(value, "https://")) {
            return value;
        }

        if (startsWithIgnoreCase(value, "www.")) {
            return "https://" + value;
        }

        return value;
    }

    /**
     * The address a click should open.
     *
     * <p>
     * Prefer a resolved http(s) URL when Swing has one. A pane with no document base turns a
     * relative href into a {@code file:} URL that is non-null and useless; in that case the
     * description is the attribute as written, which {@link #resolveHref} can still make into
     * something a browser will open.
     */
    public static String hrefOf(java.net.URL url, String description) {
        if (url != null) {
            String resolved = url.toExternalForm();

            if (startsWithIgnoreCase(resolved, "http://") || startsWithIgnoreCase(resolved, "https://")) {
                return resolved;
            }
        }

        return resolveHref(description);
    }

    /**
     * The description as a single run of prose, for a card that cannot honour markup.
     */
    public static String toPlain(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String flattened = looksLike(text) ? stripTags(text) : flattenMarkdown(text);

        return flattened.replaceAll("\\s+", " ").trim();
    }

    private static String rewriteAnchors(String html, boolean markdown) {
        Matcher matcher = ANCHOR.matcher(html);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            String href = resolveHref(hrefFromAttrs(matcher.group(1)));
            String inner = matcher.group(2);

            if (href == null) {
                matcher.appendReplacement(out, Matcher.quoteReplacement(inner));
                continue;
            }

            String label = visibleAnchorText(inner, href);

            if (markdown) {
                matcher.appendReplacement(out,
                        Matcher.quoteReplacement("[" + markdownLabel(label) + "](" + href + ")"));
            } else {
                matcher.appendReplacement(out,
                        Matcher.quoteReplacement("<a href=\"" + escapeAttr(href) + "\">" + label + "</a>"));
            }
        }

        matcher.appendTail(out);

        return out.toString();
    }

    private static String hrefFromAttrs(String attrs) {
        Matcher matcher = HREF.matcher(attrs);

        if (!matcher.find()) {
            return null;
        }

        if (matcher.group(1) != null) {
            return matcher.group(1);
        }

        if (matcher.group(2) != null) {
            return matcher.group(2);
        }

        return matcher.group(3);
    }

    /**
     * What a link should show once pictures have been taken out of it.
     *
     * <p>
     * A CurseForge banner is {@code <a href="..."><img alt=""></a>}. Dropping the image left an
     * empty tag, which Swing paints as nothing - the link was there, it just had no face. Prefer
     * the alt text, then whatever words were already inside, then the address itself.
     */
    private static String visibleAnchorText(String inner, String href) {
        String withoutImages = replaceImages(inner);
        String plain = unescape(withoutImages.replaceAll("(?is)<[^>]+>", "")).replaceAll("\\s+", " ").trim();

        if (!plain.isEmpty()) {
            return withoutImages;
        }

        return escapeAttr(displayHref(href));
    }

    private static String displayHref(String href) {
        int remote = indexOfIgnoreCase(href, "remoteUrl=");

        if (remote < 0) {
            return href;
        }

        String encoded = href.substring(remote + "remoteUrl=".length());
        int cut = encoded.indexOf('&');

        if (cut >= 0) {
            encoded = encoded.substring(0, cut);
        }

        try {
            return URLDecoder.decode(encoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return href;
        } catch (IllegalArgumentException e) {
            return href;
        }
    }

    private static String replaceImages(String html) {
        String cleaned = html.replaceAll("(?is)<img\\b[^>]*\\balt\\s*=\\s*\"([^\"]*)\"[^>]*/?>", "$1");
        cleaned = cleaned.replaceAll("(?is)<img\\b[^>]*\\balt\\s*=\\s*'([^']*)'[^>]*/?>", "$1");

        return cleaned.replaceAll("(?is)<img\\b[^>]*/?>", "");
    }

    private static String markdownLabel(String html) {
        String plain = unescape(html.replaceAll("(?is)<[^>]+>", "")).replaceAll("\\s+", " ").trim();

        return plain.replace("\\", "\\\\").replace("]", "\\]");
    }

    private static String stripTags(String html) {
        String text = sanitize(html);
        text = text.replaceAll("(?is)<br\\s*/?>", " ");
        text = text.replaceAll("(?is)</(p|div|h[1-6]|li|tr)>", " ");
        text = text.replaceAll("(?is)<[^>]+>", "");

        return unescape(text);
    }

    private static String unescape(String text) {
        return text.replace("&nbsp;", " ").replace("&middot;", "·").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }

    private static String escapeAttr(String text) {
        return text.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String flattenMarkdown(String markdown) {
        return markdown
                .replaceAll("!?\\[([^\\]]*)\\]\\([^)]*\\)", "$1")
                .replaceAll("[*_`#>]", "");
    }

    private static boolean startsWithIgnoreCase(String text, String prefix) {
        return text.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static int indexOfIgnoreCase(String text, String needle) {
        return text.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT));
    }
}
