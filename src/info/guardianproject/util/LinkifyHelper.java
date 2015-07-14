package info.guardianproject.util;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkifyHelper {

    private static Pattern bitcoin = Pattern.compile("bitcoin:[1-9a-km-zA-HJ-NP-Z]{27,34}(\\?[a-zA-Z0-9$\\-_.+!*'(),%:;@&=]*)?");
    private static Pattern geo = Pattern.compile("geo:[-0-9.]+,[-0-9.]+[^ \t\n\"\':]*");
    private static Pattern market = Pattern.compile("market://[^ \t\n\"\':,<>]+");
    private static Pattern openpgp4fpr = Pattern.compile("openpgp4fpr:[A-Za-z0-9]{8,40}");
    private static Pattern xmpp = Pattern.compile("xmpp:[^ \t\n\"\':,<>]+");
    private static Pattern twitterHandle = Pattern.compile("@([A-Za-z0-9_-]+)");
    private static Pattern hashtag = Pattern.compile("#([A-Za-z0-9_-]+)");
    private static Pattern bridge = Pattern.compile("bridge:[^ \t\n\"\':,<>]+");

    static TransformFilter returnMatchFilter = new TransformFilter() {
        @Override
        public final String transformUrl(final Matcher match, String url) {
            return match.group(1);
        }
    };

    /* Right now, if there is no app to handle */
    public static void addLinks(TextView text, SpanConverter<URLSpan, ClickableSpan> converter) {
        Linkify.addLinks(text, Linkify.ALL);
        Linkify.addLinks(text, geo, null);
        Linkify.addLinks(text, market, null);
        Linkify.addLinks(text, openpgp4fpr, null);
        Linkify.addLinks(text, xmpp, null);
        Linkify.addLinks(text, twitterHandle, "https://twitter.com/", null, returnMatchFilter);
        Linkify.addLinks(text, hashtag, "https://twitter.com/hashtag/", null, returnMatchFilter);
        text.setText(replaceAll(text.getText(), URLSpan.class, converter));
    }

    /**
     * These are clickable links that will always be safe to click on, whether
     * or not ChatSecure is using Tor or not.
     *
     * @param text
     */
    public static void addTorSafeLinks(TextView text) {
        Linkify.addLinks(text, bridge, null);
    }

    /**
     * Do not create this static utility class.
     */
    private LinkifyHelper() {
    }

    // thanks to @commonsware https://stackoverflow.com/a/11417498
    public static <A extends CharacterStyle, B extends CharacterStyle> Spannable replaceAll(
            CharSequence original, Class<A> sourceType, SpanConverter<A, B> converter) {
        SpannableString result = new SpannableString(original);
        A[] spans = result.getSpans(0, result.length(), sourceType);

        for (A span : spans) {
            int start = result.getSpanStart(span);
            int end = result.getSpanEnd(span);
            int flags = result.getSpanFlags(span);

            result.removeSpan(span);
            result.setSpan(converter.convert(span), start, end, flags);
        }

        return (result);
    }

    public interface SpanConverter<A extends CharacterStyle, B extends CharacterStyle> {
        B convert(A span);
    }
}
