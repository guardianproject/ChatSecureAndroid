package info.guardianproject.util;

import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkifyHelper {

    private static Pattern geo = Pattern.compile("geo:[-0-9.]+,[-0-9.]+[^ \t\n\"\':]*");
    private static Pattern market = Pattern.compile("market:[^ \t\n\"\':,]+");
    private static Pattern openpgp4fpr = Pattern.compile("openpgp4fpr:[A-Za-z0-9]+");
    private static Pattern xmpp = Pattern.compile("xmpp:[^ \t\n\"\':,]+");
    private static Pattern twitterHandle = Pattern.compile("@([A-Za-z0-9_-]+)");
    private static Pattern hashtag = Pattern.compile("#([A-Za-z0-9_-]+)");

    static TransformFilter returnMatchFilter = new TransformFilter() {
        @Override
        public final String transformUrl(final Matcher match, String url) {
            return match.group(1);
        }
    };

    /* Right now, if there is no app to handle */
    public static void addLinks(TextView text) {
        Linkify.addLinks(text, Linkify.ALL);
        Linkify.addLinks(text, geo, null);
        Linkify.addLinks(text, market, null);
        Linkify.addLinks(text, openpgp4fpr, null);
        Linkify.addLinks(text, xmpp, null);
        Linkify.addLinks(text, twitterHandle, "https://twitter.com/", null, returnMatchFilter);
        Linkify.addLinks(text, hashtag, "https://twitter.com/hashtag/", null, returnMatchFilter);
    }

    /**
     * Do not create this static utility class.
     */
    private LinkifyHelper() {}
}
