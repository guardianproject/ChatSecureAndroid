Incoming Message
================

Service process:

* asmack
* XmppConnection (processPacket in PacketListener)
* ChatSession.onReceiveMessage (created if not existing)
* OtrChatListener.onIncomingMessage
* ChatSessionAdapter.ListenerAdapter.onIncomingMessage
  * Insert in DB
  * listeners - IChatListener.onIncomingMessage
  * or if no listeners - fire notification

UI process:

* ChatView (IChatListener).onIncomingMessage

Incoming Data
================

Service process:

* asmack
* XmppConnection (processPacket in PacketListener)
* ChatSession.onReceiveMessage (created if not existing)
* OtrChatListener.onIncomingMessage
* ChatSessionAdapter.ListenerAdapter.onIncomingDataRequest/Response
* OtrDataHandler.onIncomingRequest/Response
