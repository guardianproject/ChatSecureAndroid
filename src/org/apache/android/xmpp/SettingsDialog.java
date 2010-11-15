package org.apache.android.xmpp;

import android.app.Dialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

/**
 * Gather the xmpp settings and create an XMPPConnection
 */
public class SettingsDialog extends Dialog implements android.view.View.OnClickListener {
    private XMPPClient xmppClient;

    public SettingsDialog(XMPPClient xmppClient) {
        super(xmppClient);
        this.xmppClient = xmppClient;
    }

    protected void onStart() {
        super.onStart();
        setContentView(R.layout.settings);
        getWindow().setFlags(4, 4);
        setTitle("XMPP Settings");
        Button ok = (Button) findViewById(R.id.ok);
        ok.setOnClickListener(this);
    }

    public void onClick(View v) {
        String host = getText(R.id.host);
        String port = getText(R.id.port);
        String service = getText(R.id.service);
        String username = getText(R.id.userid);
        String password = getText(R.id.password);

        // Create a connection
        ConnectionConfiguration connConfig =
                new ConnectionConfiguration(host, Integer.parseInt(port), service);
        XMPPConnection connection = new XMPPConnection(connConfig);

        try {
            connection.connect();
            Log.i("XMPPClient", "[SettingsDialog] Connected to " + connection.getHost());
        } catch (XMPPException ex) {
            Log.e("XMPPClient", "[SettingsDialog] Failed to connect to " + connection.getHost());
            Log.e("XMPPClient", ex.toString());
            xmppClient.setConnection(null);
        }
        try {
            connection.login(username, password);
            Log.i("XMPPClient", "Logged in as " + connection.getUser());

            // Set the status to available
            Presence presence = new Presence(Presence.Type.available);
            connection.sendPacket(presence);
            xmppClient.setConnection(connection);
        } catch (XMPPException ex) {
            Log.e("XMPPClient", "[SettingsDialog] Failed to log in as " + username);
            Log.e("XMPPClient", ex.toString());
                xmppClient.setConnection(null);
        }
        dismiss();
    }

    private String getText(int id) {
        EditText widget = (EditText) this.findViewById(id);
        return widget.getText().toString();
    }
}
