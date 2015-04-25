package info.guardianproject.otr.app.im.plugin.xmpp;

import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.EmbeddedExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;

public class DeliveryReceipts {
    public static final String NAMESPACE = "urn:xmpp:receipts";

    static public class DeliveryReceiptRequest implements PacketExtension {
        public String getElementName() {
            return "request";
        }

        public String getNamespace() {
            return NAMESPACE;
        }

        public String toXML() {
            return "<request xmlns='" + NAMESPACE + "'/>";
        }
    }

    static public class DeliveryReceipt implements PacketExtension {
        private String id; /// original ID of the delivered message

        public DeliveryReceipt(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public String getElementName() {
            return "received";
        }

        public String getNamespace() {
            return NAMESPACE;
        }

        public String toXML() {
            return "<received xmlns='" + NAMESPACE + "' id='" + id + "'/>";
        }
    }

    static public class DeliveryReceiptProvider extends EmbeddedExtensionProvider {

        @Override
        protected PacketExtension createReturnExtension(String currentElement,
                String currentNamespace, Map<String, String> attributeMap,
                List<? extends PacketExtension> content) {
            return new DeliveryReceipt(attributeMap.get("id"));
        }

    }

    static void addExtensionProviders() {
        ProviderManager pm = ProviderManager.getInstance();
        // add IQ handling
        pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",
                new DiscoverInfoProvider());
        // add delivery receipts
        pm.addExtensionProvider("received", NAMESPACE, new DeliveryReceiptProvider());
    }
}
