package org.gitian.android.im.plugin.xmpp;

import info.guardianproject.otr.OtrChatManager;

import java.util.Date;

import org.gitian.android.im.engine.ChatSession;
import org.gitian.android.im.engine.Contact;
import org.gitian.android.im.engine.Message;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

import android.util.Log;

public class XmppChatPacketListener implements PacketListener {

	private XmppConnection xConn;
	private OtrChatManager otrMgr;
	
	private final static String TAG = "Xmpp";
	
	private final static String OTR_INIT_STRING = "?OTR?v2?";

	private final static String OTR_HEADER_STRING = "?OTR";
	
	public XmppChatPacketListener (XmppConnection xConn)
	{
		this.xConn = xConn;
	}
	
	
	/**
	 * @return the otrMgr
	 */
	public OtrChatManager getOtrMgr() {
		return otrMgr;
	}


	/**
	 * @param otrMgr the otrMgr to set
	 */
	public void setOtrMgr(OtrChatManager otrMgr) {
		this.otrMgr = otrMgr;
		
	}


	@Override
	public void processPacket(Packet packet) {
		org.jivesoftware.smack.packet.Message message = (org.jivesoftware.smack.packet.Message) packet;
		
		if (message != null && message.getBody()!=null)
		{
			//android.os.Debug.waitForDebugger();
			

			String msgBody = message.getBody();
			
			
			Log.i(TAG, "msg.id: " + message.getPacketID());
			Log.i(TAG, "msg.from: " + message.getFrom());
			Log.i(TAG, "msg.to: " + message.getTo());
			Log.i(TAG, "msg.body: " +  message.getBody());
			
			
			if (msgBody.indexOf(OTR_INIT_STRING)!=-1)
			{
				//start a new session
				
				if (otrMgr == null)
				{
					otrMgr = new OtrChatManager(xConn);	
					xConn.setOtrManager(otrMgr);
				}
			}
			else if ((otrMgr == null && msgBody.indexOf(OTR_HEADER_STRING)!=-1))
			{
				//restart session
				
				otrMgr = new OtrChatManager(xConn);	
				xConn.setOtrManager(otrMgr);
				otrMgr.refreshSession(message.getTo(), message.getFrom());
			}
			
			if (otrMgr != null)
			{
				msgBody = (otrMgr.receiveMessage(message.getTo(), message.getFrom(), msgBody));
			}

			Message rec = new Message(msgBody);
			
			rec.setBody(msgBody);
			String address = parseAddressBase(message.getFrom());
			ChatSession session = findOrCreateSession(address);
			rec.setFrom(session.getParticipant().getAddress());
			rec.setDateTime(new Date());
			session.onReceiveMessage(rec);
		}
		
		
	}
	
	protected static String parseAddressBase(String from) {
		return from.replaceFirst("/.*", "");
	}
	
	private ChatSession findOrCreateSession(String address) {
		ChatSession session = xConn.findSession(address);
		if (session == null) {
			Contact contact = xConn.findOrCreateContact(address);
			session = xConn.createChatSession(contact);
		}
		
		
		return session;
	}

}
