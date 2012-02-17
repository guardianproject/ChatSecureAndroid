package info.guardianproject.otr.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;

import net.java.otr4j.OtrEngineImpl;
import net.java.otr4j.OtrEngineListener;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrKeyManagerImpl;
import net.java.otr4j.OtrKeyManagerListener;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.session.OtrSm;
import net.java.otr4j.session.OtrSm.OtrSmEngineHost;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;
import net.java.otr4j.session.TLV;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.packet.Packet;

public class SampleApp implements OtrSmEngineHost {
	private String peer;
	private XMPPConnection con;
	private OtrKeyManagerImpl otrKeyManager;
	private OtrPolicy otrPolicy;
	private SessionID sessionID;
	private OtrSm otrSm;
	private OtrEngineImpl otrEngine;
	
	SampleApp(String domain, String user, String pass, String peer) throws Exception {
		this.peer = peer;
		con = new XMPPConnection(domain);
        con.addPacketListener(new PacketListener() {
			@Override
			public void processPacket(Packet packet) {
				org.jivesoftware.smack.packet.Message smackMessage = (org.jivesoftware.smack.packet.Message) packet;
				String body = smackMessage.getBody();
				String from = smackMessage.getFrom();
				try {
					body = otrEngine.transformReceiving(sessionID, body);
					List<TLV> tlvs = otrSm.getPendingTlvs();
					if (tlvs != null) {
						sendMessage(from, otrEngine.transformSending(sessionID, "", tlvs));
					}
				} catch (OtrException ex) {
					ex.printStackTrace();
				}
				if (body != null && !body.isEmpty()) {
					System.out.println(from + " : " + body);
				}
			}
		}, new MessageTypeFilter(org.jivesoftware.smack.packet.Message.Type.chat));

		con.connect();
		con.login(user, pass);
		otrKeyManager = new OtrKeyManagerImpl("/tmp/sample-keystore");
		otrKeyManager.addListener(new OtrKeyManagerListener() {
			public void verificationStatusChanged(SessionID session) {
				System.out.println(session + ": verification status=" + otrKeyManager.isVerified(session));
			}
		});
		otrPolicy = new OtrPolicyImpl();
		otrPolicy.setEnableAlways(true);
		sessionID = new SessionID("default", peer, "xmpp");
		otrEngine = new OtrEngineImpl(this);
		otrEngine.addOtrEngineListener(new OtrEngineListener() {
			@Override
			public void sessionStatusChanged(SessionID sessionID) {
				SessionStatus sessionStatus = otrEngine.getSessionStatus(sessionID);
				System.out.println(sessionID + " : status=" + sessionStatus);
				if (sessionStatus == SessionStatus.ENCRYPTED)
				{
					
					PublicKey remoteKey = otrEngine.getRemotePublicKey(sessionID);
					otrKeyManager.savePublicKey(sessionID, remoteKey);
				}
				
				// SMP handler - make sure we only add this once per session!
				otrSm = new OtrSm(otrEngine.getSession(sessionID), otrKeyManager, sessionID, SampleApp.this);
				otrEngine.getSession(sessionID).addTlvHandler(otrSm);
			}
		});
	}

	public void sendMessage(String to, String body) {
		org.jivesoftware.smack.packet.Message msg =
				new org.jivesoftware.smack.packet.Message(
						to,
						org.jivesoftware.smack.packet.Message.Type.chat
						);
		msg.setBody(body);
		con.sendPacket(msg);
	}

	public void disconnect() {
		con.disconnect();
	}

	@Override
	public void askForSecret(SessionID sessionID, String question) {
		System.out.println("asked for secret with q=" + question);
		System.out.println("enter /smpr SECRET");
	}

	@Override
	public void showError(SessionID sessionID, String error) {
		System.err.println(sessionID + ": error " + error);
	}

	@Override
	public void showWarning(SessionID sessionID, String warn) {
		System.err.println(sessionID + ": warn " + warn);
	}

	@Override
	public OtrPolicy getSessionPolicy(SessionID sessionID) {
		return otrPolicy;
	}

	@Override
	public void injectMessage(SessionID sessionID, String msg) {
		sendMessage(sessionID.getUserID(), msg);
	}

	@Override
	public KeyPair getKeyPair(SessionID sessionID) {
		KeyPair kp = null;
		try {
			kp = otrKeyManager.loadLocalKeyPair(sessionID);
		} catch (NullPointerException ex) {
			// ignore
		}

		if (kp == null)
		{
			otrKeyManager.generateLocalKeyPair(sessionID);	
			kp = otrKeyManager.loadLocalKeyPair(sessionID);
		}
		return kp;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			System.err.println("Usage: SampleApp DOMAIN USER PASS PEER_ADDRESS");
			return;
		}
		SampleApp app = new SampleApp(args[0], args[1], args[2], args[3]);
		app.run();
	}

	private void run() throws IOException, OtrException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			System.out.print(">");
			String line = reader.readLine().trim();
			if ("/quit".equals(line)) {
				disconnect();
				return;
			}
			else if ("/otr".equals(line)) {
				otrEngine.startSession(sessionID);
			}
			else if (line.startsWith("/smpr")) {
				String secret = line.substring("/smpr ".length());
				List<TLV> tlvs = otrSm.initRespondSmp(null, secret, false);
				String encrypted = otrEngine.transformSending(sessionID, "", tlvs);
				sendMessage(peer, encrypted);
			}
			else if (line.startsWith("/smpa")) {
				if (otrEngine.getSessionStatus(sessionID) != SessionStatus.ENCRYPTED) {
					System.err.println("Not currently encrypted - use /otr");
					continue;
				}
				List<TLV> tlvs = otrSm.abortSmp();
				String encrypted = otrEngine.transformSending(sessionID, "", tlvs);
				sendMessage(peer, encrypted);
			}
			else if (line.startsWith("/smp")) {
				if (otrEngine.getSessionStatus(sessionID) != SessionStatus.ENCRYPTED) {
					System.err.println("Not currently encrypted - use /otr");
					continue;
				}
				String[] splits = line.split(" ", 3);
				if (splits.length < 2) {
					System.err.println("missing arguments");
					continue;
				}
				List<TLV> tlvs;
				if (splits.length == 3) {
					tlvs = otrSm.initRespondSmp(splits[1], splits[2], true);
				} else {
					tlvs = otrSm.initRespondSmp(null, splits[1], true);
				}
				String encrypted = otrEngine.transformSending(sessionID, "", tlvs);
				sendMessage(peer, encrypted);
			}
			else if ("/help".equals(line)) {
				System.out.println("/quit");
				System.out.println("/otr");
				System.out.println("/smpr SECRET - SMP response");
				System.out.println("/smp [QUESTION] SECRET - SMP initiation");
				System.out.println("/smpa - SMP abort");
			}
			else if (line.startsWith("/")) {
				System.err.println("Unknown command");
			}
			else {
				String encrypted = otrEngine.transformSending(sessionID, line, null);
				sendMessage(peer, encrypted);
			}
		}
	}
}
