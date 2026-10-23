package com.musicplayer.app.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses LRC lyric text ([mm:ss.xx] words) and plain text lyrics.
 * Lines without timestamps get timeMs = -1 (displayed but never highlighted).
 */
public final class LrcParser {

    public static class LyricLine {
        public final long timeMs;
        public final String text;

        public LyricLine(long timeMs, String text) {
            this.timeMs = timeMs;
            this.text = text;
        }
    }

    private static final Pattern LRC_LINE = Pattern.compile(
            "\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]\\s*(.*)");

    private LrcParser() {
    }

    public static List<LyricLine> parse(String raw) {
        List<LyricLine> result = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return result;

        String[] split = raw.split("\\r?\\n");
        boolean anyTimestamp = false;

        for (String line : split) {
            Matcher m = LRC_LINE.matcher(line.trim());
            if (m.matches()) {
                anyTimestamp = true;
                long minutes = Long.parseLong(m.group(1));
                long seconds = Long.parseLong(m.group(2));
                String frac = m.group(3);
                long millis = 0;
                if (frac != null && !frac.isEmpty()) {
                    // .xx = centiseconds, .xxx = milliseconds
                    millis = frac.length() == 2
                            ? Long.parseLong(frac) * 10L
                            : Long.parseLong(frac);
                }
                long timeMs = minutes * 60_000 + seconds * 1000 + millis;
                String text = m.group(4) == null ? "" : m.group(4).trim();
                if (!text.isEmpty()) {
                    result.add(new LyricLine(timeMs, text));
                }
            } else if (!line.trim().isEmpty()) {
                result.add(new LyricLine(-1, line.trim()));
            }
        }

        // Plain lyrics (no timestamps at all) — keep insertion order.
        if (!anyTimestamp) {
            return result;
        }

        // Stable sort by timestamp for mixed content.
        result.sort((a, b) -> Long.compare(
                a.timeMs < 0 ? Long.MAX_VALUE : a.timeMs,
                b.timeMs < 0 ? Long.MAX_VALUE : b.timeMs));
        return result;
    }
}
