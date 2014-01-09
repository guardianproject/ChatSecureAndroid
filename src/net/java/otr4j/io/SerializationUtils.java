/*
 * otr4j, the open source java otr library.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.otr4j.io;

import info.guardianproject.bouncycastle.util.encoders.Base64;
import info.guardianproject.otr.OtrConstants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.interfaces.DHPublicKey;

import net.java.otr4j.io.messages.AbstractEncodedMessage;
import net.java.otr4j.io.messages.AbstractMessage;
import net.java.otr4j.io.messages.DHCommitMessage;
import net.java.otr4j.io.messages.DHKeyMessage;
import net.java.otr4j.io.messages.DataMessage;
import net.java.otr4j.io.messages.ErrorMessage;
import net.java.otr4j.io.messages.MysteriousT;
import net.java.otr4j.io.messages.PlainTextMessage;
import net.java.otr4j.io.messages.QueryMessage;
import net.java.otr4j.io.messages.RevealSignatureMessage;
import net.java.otr4j.io.messages.SignatureM;
import net.java.otr4j.io.messages.SignatureMessage;
import net.java.otr4j.io.messages.SignatureX;

/** @author George Politis */
public class SerializationUtils {
    // Mysterious X IO.
    public static SignatureX toMysteriousX(byte[] b) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        OtrInputStream ois = new OtrInputStream(in);
        SignatureX x = ois.readMysteriousX();
        ois.close();
        return x;
    }

    public static byte[] toByteArray(SignatureX x) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OtrOutputStream oos = new OtrOutputStream(out);
        oos.writeMysteriousX(x);
        byte[] b = out.toByteArray();
        oos.close();
        return b;
    }

    // Mysterious M IO.
    public static byte[] toByteArray(SignatureM m) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OtrOutputStream oos = new OtrOutputStream(out);
        oos.writeMysteriousX(m);
        byte[] b = out.toByteArray();
        oos.close();
        return b;
    }

    // Mysterious T IO.
    public static byte[] toByteArray(MysteriousT t) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OtrOutputStream oos = new OtrOutputStream(out);
        oos.writeMysteriousT(t);
        byte[] b = out.toByteArray();
        out.close();
        return b;
    }

    // Basic IO.
    public static byte[] writeData(byte[] b) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OtrOutputStream oos = new OtrOutputStream(out);
        oos.writeData(b);
        byte[] otrb = out.toByteArray();
        out.close();
        return otrb;
    }

    // BigInteger IO.
    public static byte[] writeMpi(BigInteger bigInt) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OtrOutputStream oos = new OtrOutputStream(out);
        oos.writeBigInt(bigInt);
        byte[] b = out.toByteArray();
        oos.close();
        return b;
    }

    public static BigInteger readMpi(byte[] b) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        OtrInputStream ois = new OtrInputStream(in);
        BigInteger bigint = ois.readBigInt();
        ois.close();
        return bigint;
    }

    // Public Key IO.
    public static byte[] writePublicKey(PublicKey pubKey) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OtrOutputStream oos = new OtrOutputStream(out);
        oos.writePublicKey(pubKey);
        byte[] b = out.toByteArray();
        oos.close();
        return b;
    }

    // Message IO.
    public static String toString(AbstractMessage m) throws IOException {
        StringWriter writer = new StringWriter();
        writer.write(SerializationConstants.HEAD);

        switch (m.messageType) {
        case AbstractMessage.MESSAGE_ERROR:
            ErrorMessage error = (ErrorMessage) m;
            writer.write(SerializationConstants.HEAD_ERROR);
            writer.write(error.error);
            break;
        case AbstractMessage.MESSAGE_PLAINTEXT:
            PlainTextMessage plaintxt = (PlainTextMessage) m;
            writer.write(plaintxt.cleanText);
            if (plaintxt.versions != null && plaintxt.versions.size() > 0) {
                writer.write(" \\t  \\t\\t\\t\\t \\t \\t \\t  ");
                for (int version : plaintxt.versions) {
                    if (version == 1)
                        writer.write("  \\t\\t  \\t ");

                    if (version == 2)
                        writer.write(" \\t \\t  \\t ");
                }
            }
            break;
        case AbstractMessage.MESSAGE_QUERY:
            QueryMessage query = (QueryMessage) m;
            if (query.versions.size() == 1 && query.versions.get(0) == 1) {
                writer.write(SerializationConstants.HEAD_QUERY_Q);
            } else {
                writer.write(SerializationConstants.HEAD_QUERY_V);
                for (int version : query.versions)
                    writer.write(String.valueOf(version));

                writer.write(SerializationConstants.HEAD_QUERY_Q);
            }
            writer.write(OtrConstants.CommonRequest);
            break;
        case AbstractEncodedMessage.MESSAGE_DHKEY:
        case AbstractEncodedMessage.MESSAGE_REVEALSIG:
        case AbstractEncodedMessage.MESSAGE_SIGNATURE:
        case AbstractEncodedMessage.MESSAGE_DH_COMMIT:
        case AbstractEncodedMessage.MESSAGE_DATA:
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            OtrOutputStream s = new OtrOutputStream(o);

            switch (m.messageType) {
            case AbstractEncodedMessage.MESSAGE_DHKEY:
                DHKeyMessage dhkey = (DHKeyMessage) m;
                s.writeShort(dhkey.protocolVersion);
                s.writeByte(dhkey.messageType);
                s.writeDHPublicKey(dhkey.dhPublicKey);
                break;
            case AbstractEncodedMessage.MESSAGE_REVEALSIG:
                RevealSignatureMessage revealsig = (RevealSignatureMessage) m;
                s.writeShort(revealsig.protocolVersion);
                s.writeByte(revealsig.messageType);
                s.writeData(revealsig.revealedKey);
                s.writeData(revealsig.xEncrypted);
                s.writeMac(revealsig.xEncryptedMAC);
                break;
            case AbstractEncodedMessage.MESSAGE_SIGNATURE:
                SignatureMessage sig = (SignatureMessage) m;
                s.writeShort(sig.protocolVersion);
                s.writeByte(sig.messageType);
                s.writeData(sig.xEncrypted);
                s.writeMac(sig.xEncryptedMAC);
                break;
            case AbstractEncodedMessage.MESSAGE_DH_COMMIT:
                DHCommitMessage dhcommit = (DHCommitMessage) m;
                s.writeShort(dhcommit.protocolVersion);
                s.writeByte(dhcommit.messageType);
                s.writeData(dhcommit.dhPublicKeyEncrypted);
                s.writeData(dhcommit.dhPublicKeyHash);
                break;
            case AbstractEncodedMessage.MESSAGE_DATA:
                DataMessage data = (DataMessage) m;
                s.writeShort(data.protocolVersion);
                s.writeByte(data.messageType);
                s.writeByte(data.flags);
                s.writeInt(data.senderKeyID);
                s.writeInt(data.recipientKeyID);
                s.writeDHPublicKey(data.nextDH);
                s.writeCtr(data.ctr);
                s.writeData(data.encryptedMessage);
                s.writeMac(data.mac);
                s.writeData(data.oldMACKeys);
                break;
            }

            writer.write(SerializationConstants.HEAD_ENCODED);
            writer.write(new String(Base64.encode(o.toByteArray())));
            writer.write(".");
            break;
        default:
            throw new IOException("Illegal message type.");
        }

        return writer.toString();
    }

    static final Pattern patternWhitespace = Pattern
            .compile("( \\t  \\t\\t\\t\\t \\t \\t \\t  )( \\t \\t  \\t )?(  \\t\\t  \\t )?");

    public static AbstractMessage toMessage(String s) throws IOException {
        if (s == null || s.length() <= 1)
            return null;

        if (s.indexOf(SerializationConstants.HEAD) != 0
            || s.length() <= SerializationConstants.HEAD.length()) {
            // Try to detect whitespace tag.
            final Matcher matcher = patternWhitespace.matcher(s);

            boolean v1 = false;
            boolean v2 = false;
            while (matcher.find()) {
                if (!v1 && matcher.start(2) > -1)
                    v1 = true;

                if (!v2 && matcher.start(3) > -1)
                    v2 = true;

                if (v1 && v2)
                    break;
            }

            String cleanText = matcher.replaceAll("");
            List<Integer> versions;
            if (v1 && v2) {
                versions = new Vector<Integer>(2);
                versions.add(0, 1);
                versions.add(0, 2);
            } else if (v1) {
                versions = new Vector<Integer>(1);
                versions.add(0, 1);
            } else if (v2) {
                versions = new Vector<Integer>(1);
                versions.add(2);
            } else
                versions = null;

            return new PlainTextMessage(versions, cleanText);
        } else {
            char contentType = s.charAt(SerializationConstants.HEAD.length());
            String content = s.substring(SerializationConstants.HEAD.length() + 1);
            switch (contentType) {
            case SerializationConstants.HEAD_ENCODED:
                ByteArrayInputStream bin = new ByteArrayInputStream(Base64.decode(content
                        .getBytes()));
                OtrInputStream otr = new OtrInputStream(bin);
                // We have an encoded message.
                int protocolVersion = otr.readShort();
                int messageType = otr.readByte();
                switch (messageType) {
                case AbstractEncodedMessage.MESSAGE_DATA:
                    int flags = otr.readByte();
                    int senderKeyID = otr.readInt();
                    int recipientKeyID = otr.readInt();
                    DHPublicKey nextDH = otr.readDHPublicKey();
                    byte[] ctr = otr.readCtr();
                    byte[] encryptedMessage = otr.readData();
                    byte[] mac = otr.readMac();
                    byte[] oldMacKeys = otr.readMac();
                    return new DataMessage(protocolVersion, flags, senderKeyID, recipientKeyID,
                            nextDH, ctr, encryptedMessage, mac, oldMacKeys);
                case AbstractEncodedMessage.MESSAGE_DH_COMMIT:
                    byte[] dhPublicKeyEncrypted = otr.readData();
                    byte[] dhPublicKeyHash = otr.readData();
                    return new DHCommitMessage(protocolVersion, dhPublicKeyHash,
                            dhPublicKeyEncrypted);
                case AbstractEncodedMessage.MESSAGE_DHKEY:
                    DHPublicKey dhPublicKey = otr.readDHPublicKey();
                    return new DHKeyMessage(protocolVersion, dhPublicKey);
                case AbstractEncodedMessage.MESSAGE_REVEALSIG: {
                    byte[] revealedKey = otr.readData();
                    byte[] xEncrypted = otr.readData();
                    byte[] xEncryptedMac = otr.readMac();
                    return new RevealSignatureMessage(protocolVersion, xEncrypted, xEncryptedMac,
                            revealedKey);
                }
                case AbstractEncodedMessage.MESSAGE_SIGNATURE: {
                    byte[] xEncryted = otr.readData();
                    byte[] xEncryptedMac = otr.readMac();
                    return new SignatureMessage(protocolVersion, xEncryted, xEncryptedMac);
                }
                default:
                    throw new IOException("Illegal message type.");
                }
            case SerializationConstants.HEAD_MESSAGE:
                return new ErrorMessage(AbstractMessage.MESSAGE_ERROR, content);
            case SerializationConstants.HEAD_QUERY_V:
            case SerializationConstants.HEAD_QUERY_Q:
                List<Integer> versions = new Vector<Integer>();
                String versionString = null;
                if (SerializationConstants.HEAD_QUERY_Q == contentType) {
                    versions.add(1);
                    if (content.charAt(0) == 'v') {
                        versionString = content.substring(1, content.indexOf('?'));
                    }
                } else if (SerializationConstants.HEAD_QUERY_V == contentType) {
                    versionString = content.substring(0, content.indexOf('?'));
                }

                if (versionString != null) {
                    StringReader sr = new StringReader(versionString);
                    int c;
                    while ((c = sr.read()) != -1)
                        if (!versions.contains(c))
                            versions.add(Integer.parseInt(String.valueOf((char) c)));
                }
                QueryMessage query = new QueryMessage(versions);
                return query;
            default:
                throw new IOException("Unknown message type.");
            }
        }
    }
}
