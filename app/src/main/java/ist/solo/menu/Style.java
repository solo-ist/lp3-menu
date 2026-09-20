package ist.solo.menu;

import android.graphics.Color;

/**
 * Measured against the real LightOS toolbox on 582-release-lp3
 * (1080x1240 @ 480dpi, scale 3.0), captured 2026-09-19.
 *
 * Observed geometry:
 *   row pitch        190px → 63dp
 *   text line box    150px → 50dp
 *   six rows visible per page, text CENTRED horizontally (all rows share
 *   centre x = 540 = screen middle)
 *   page dots in a right-hand column, centre x ≈ 1016, pitch ≈ 80px
 *
 * Typeface: LightOS maps the system "sans-serif" family to Akkurat LL in
 * /system/etc/fonts.xml, so asking for sans-serif-light yields
 * AkkuratLLTT-Light — the toolbox's actual font, with nothing to ship.
 */
final class Style {
    private Style() {}

    static final int BACKGROUND = Color.BLACK;
    static final int FOREGROUND = Color.WHITE;
    /** The add-row and other secondary text. */
    static final int MUTED = Color.parseColor("#6E6E6E");

    /** Resolves to AkkuratLLTT-Light on LightOS. */
    static final String FONT_FAMILY = "sans-serif-light";

    /**
     * Matched by width against the reference: real "Calculator" occupies
     * 513px for ten glyphs (~51px/char). 42sp measured ~64px/char and ran
     * into the dot rail, so it came down.
     */
    static final float ROW_TEXT_SP = 34f;
    static final int ROW_PITCH_DP = 63;
    static final int ROWS_PER_PAGE = 6;

    static final int DOT_DIAMETER_DP = 9;
    static final int DOT_PITCH_DP = 27;
    static final int DOT_MARGIN_END_DP = 16;

    /** Keeps long labels clear of the dot rail while staying screen-centred. */
    static final int ROW_PAD_HORIZONTAL_DP = 32;
}
