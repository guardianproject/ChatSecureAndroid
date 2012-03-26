package info.guardianproject.otr.app.im.plugin.xmpp;

import info.guardianproject.otr.app.im.plugin.xmpp.XmppConnection.MyXMPPConnection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.UnknownPacket;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;

public class XmppStreamHandler {
	private static final String URN_SM_2 = "urn:xmpp:sm:2";
	private MyXMPPConnection mConnection;
	private boolean isSmAvailable = false;
	private boolean isSmEnabled = false;
	private long previousIncomingStanzaCount = -1;
	private String sessionId;
	private long incomingStanzaCount = 0;

	public XmppStreamHandler(MyXMPPConnection connection) {
		mConnection = connection;
		startListening();
	}
	
	public boolean isResumePossible() {
		return sessionId != null;
	}

	public static void addExtensionProviders() {
		addSimplePacketExtension("sm", URN_SM_2);
		addSimplePacketExtension("r", URN_SM_2);
		addSimplePacketExtension("a", URN_SM_2);
		addSimplePacketExtension("enabled", URN_SM_2);
		addSimplePacketExtension("resumed", URN_SM_2);
		addSimplePacketExtension("failed", URN_SM_2);
	}

	public void notifyInitialLogin() {
		if (isSmAvailable) {
			sendEnablePacket();
		}
	}

	private void sendEnablePacket() {
		if (sessionId != null) {
			// TODO binding
			StreamHandlingPacket resumePacket = new StreamHandlingPacket("resume", URN_SM_2);
			resumePacket.addAttribute("h", String.valueOf(previousIncomingStanzaCount));
			resumePacket.addAttribute("previd", sessionId);
			mConnection.sendPacket(resumePacket);
		} else {
			StreamHandlingPacket enablePacket = new StreamHandlingPacket("enable", URN_SM_2);
			enablePacket.addAttribute("resume", "true");
			mConnection.sendPacket(enablePacket);
		}
	}
	
	private void startListening() {
        mConnection.addConnectionListener(new ConnectionListener() {
			public void reconnectionSuccessful() {
				if (isSmAvailable) {
					sendEnablePacket();
				} else {
					isSmEnabled = false;
					sessionId = null;
				}
			}
			
			public void reconnectionFailed(Exception e) {}
			
			public void reconnectingIn(int seconds) {}
			
			public void connectionClosedOnError(Exception e) {
				if (isSmEnabled && sessionId != null) {
					previousIncomingStanzaCount = incomingStanzaCount;
				}
				isSmEnabled = false;
				isSmAvailable = false;
			}
			
			public void connectionClosed() {
				previousIncomingStanzaCount = -1;
			}});
        
		mConnection.addPacketListener(new PacketListener() {
			public void processPacket(Packet packet) {
				incomingStanzaCount++;
				debug("" + incomingStanzaCount + " : " + packet.toXML());
				if (packet instanceof StreamHandlingPacket) {
					StreamHandlingPacket shPacket = (StreamHandlingPacket)packet;
					String name = shPacket.getElementName();
					if ("sm".equals(name)) {
						isSmAvailable = true;
					} else if ("r".equals(name)) {
						incomingStanzaCount--;
						StreamHandlingPacket ackPacket = new StreamHandlingPacket("a", URN_SM_2);
						ackPacket.addAttribute("h", String.valueOf(incomingStanzaCount));
						mConnection.sendPacket(ackPacket);
					} else if ("a".equals(name)) {
						// ignore for now 
					} else if ("enabled".equals(name)) {
						incomingStanzaCount = 0;
						isSmEnabled = true;
						mConnection.getRoster().setOfflineOnError(false);
						String resume = shPacket.getAttribute("resume");
						if ("true".equals(resume) || "1".equals(resume)) {
							sessionId = shPacket.getAttribute("id");
						}
					} else if ("resumed".equals(name)) {
						incomingStanzaCount = previousIncomingStanzaCount;
						isSmEnabled = true;
					} else if ("failed".equals(name)) {
						// Failed, shutdown and the parent will retry
						mConnection.getRoster().setOfflineOnError(true);
						mConnection.getRoster().setOfflinePresences();
						sessionId = null;
						mConnection.shutdown();
						// isSmEnabled is already false
					}
				}
			}},
			new PacketFilter() { public boolean accept(Packet packet) { return true; }});
	}

	private static void addSimplePacketExtension(final String name, final String namespace) {
		ProviderManager.getInstance().addExtensionProvider(name, namespace,
				new PacketExtensionProvider() {
			public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
				StreamHandlingPacket packet = new StreamHandlingPacket(name, namespace);
				int attributeCount = parser.getAttributeCount();
				for (int i = 0 ; i < attributeCount ; i++) {
					packet.addAttribute(parser.getAttributeName(i), parser.getAttributeValue(i));
				}
				return packet;
			}
		});
	}
	
	private void debug(String message) {
		XmppConnection.debug(XmppConnection.TAG, message);
	}

	static class StreamHandlingPacket extends UnknownPacket {
		private String name;
		private String namespace;
		Map<String, String> attributes;

		StreamHandlingPacket(String name, String namespace) {
			this.name = name;
			this.namespace = namespace;
			attributes = Collections.emptyMap();
		}

		public void addAttribute(String name, String value) {
			if (attributes == Collections.EMPTY_MAP)
				attributes = new HashMap<String, String>();
			attributes.put(name, value);
		}

		public String getAttribute(String name) {
			return attributes.get(name);
		}

		public String getNamespace() {
			return namespace;
		}

		public String getElementName() {
			return name;
		}

		public String toXML() {
			StringBuilder buf = new StringBuilder();
			buf.append("<").append(getElementName());
			
			// TODO Xmlns??
	        if (getNamespace() != null) {
	            buf.append(" xmlns=\"").append(getNamespace()).append("\"");
	        }
	        for (String key : attributes.keySet()) {
	        	buf.append(" ").append(key).append("=\"").append(StringUtils.escapeForXML(attributes.get(key))).append("\"");
	        }
	        buf.append("/>");
	        return buf.toString();
		}

	}
}
