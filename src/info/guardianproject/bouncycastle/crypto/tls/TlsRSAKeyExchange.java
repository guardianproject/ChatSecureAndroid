package info.guardianproject.bouncycastle.crypto.tls;

import info.guardianproject.bouncycastle.asn1.x509.KeyUsage;
import info.guardianproject.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import info.guardianproject.bouncycastle.asn1.x509.X509CertificateStructure;
import info.guardianproject.bouncycastle.crypto.InvalidCipherTextException;
import info.guardianproject.bouncycastle.crypto.encodings.PKCS1Encoding;
import info.guardianproject.bouncycastle.crypto.engines.RSABlindedEngine;
import info.guardianproject.bouncycastle.crypto.params.AsymmetricKeyParameter;
import info.guardianproject.bouncycastle.crypto.params.ParametersWithRandom;
import info.guardianproject.bouncycastle.crypto.params.RSAKeyParameters;
import info.guardianproject.bouncycastle.crypto.util.PublicKeyFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * TLS 1.0 RSA key exchange.
 */
class TlsRSAKeyExchange implements TlsKeyExchange
{
    protected TlsClientContext context;

    protected AsymmetricKeyParameter serverPublicKey = null;

    protected RSAKeyParameters rsaServerPublicKey = null;

    protected byte[] premasterSecret;

    TlsRSAKeyExchange(TlsClientContext context)
    {
        this.context = context;
    }

    public void skipServerCertificate() throws IOException
    {
        throw new TlsFatalAlert(AlertDescription.unexpected_message);
    }

    public void processServerCertificate(Certificate serverCertificate) throws IOException
    {
        X509CertificateStructure x509Cert = serverCertificate.certs[0];
        SubjectPublicKeyInfo keyInfo = x509Cert.getSubjectPublicKeyInfo();

        try
        {
            this.serverPublicKey = PublicKeyFactory.createKey(keyInfo);
        }
        catch (RuntimeException e)
        {
            throw new TlsFatalAlert(AlertDescription.unsupported_certificate);
        }

        // Sanity check the PublicKeyFactory
        if (this.serverPublicKey.isPrivate())
        {
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }

        this.rsaServerPublicKey = validateRSAPublicKey((RSAKeyParameters)this.serverPublicKey);

        TlsUtils.validateKeyUsage(x509Cert, KeyUsage.keyEncipherment);

        // TODO 
        /*
         * Perform various checks per RFC2246 7.4.2: "Unless otherwise specified, the
         * signing algorithm for the certificate must be the same as the algorithm for the
         * certificate key."
         */
    }

    public void skipServerKeyExchange() throws IOException
    {
        // OK
    }

    public void processServerKeyExchange(InputStream is)
        throws IOException
    {
        // TODO
        throw new TlsFatalAlert(AlertDescription.unexpected_message);
    }

    public void validateCertificateRequest(CertificateRequest certificateRequest)
        throws IOException
    {
        short[] types = certificateRequest.getCertificateTypes();
        for (int i = 0; i < types.length; ++i)
        {
            switch (types[i])
            {
                case ClientCertificateType.rsa_sign:
                case ClientCertificateType.dss_sign:
                case ClientCertificateType.ecdsa_sign:
                    break;
                default:
                    throw new TlsFatalAlert(AlertDescription.illegal_parameter);
            }
        }
    }

    public void skipClientCredentials() throws IOException
    {
        // OK
    }

    public void processClientCredentials(TlsCredentials clientCredentials) throws IOException
    {
        if (!(clientCredentials instanceof TlsSignerCredentials))
        {
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }
    }

    public void generateClientKeyExchange(OutputStream os) throws IOException
    {
        /*
         * Choose a PremasterSecret and send it encrypted to the server
         */
        premasterSecret = new byte[48];
        context.getSecureRandom().nextBytes(premasterSecret);
        TlsUtils.writeVersion(premasterSecret, 0);

        PKCS1Encoding encoding = new PKCS1Encoding(new RSABlindedEngine());
        encoding.init(true, new ParametersWithRandom(this.rsaServerPublicKey, context.getSecureRandom()));

        try
        {
            byte[] keData = encoding.processBlock(premasterSecret, 0, premasterSecret.length);
            TlsUtils.writeUint24(keData.length + 2, os);
            TlsUtils.writeOpaque16(keData, os);
        }
        catch (InvalidCipherTextException e)
        {
            /*
             * This should never happen, only during decryption.
             */
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }
    }

    public byte[] generatePremasterSecret() throws IOException
    {
        byte[] tmp = this.premasterSecret;
        this.premasterSecret = null;
        return tmp;
    }

    // Would be needed to process RSA_EXPORT server key exchange
//    protected void processRSAServerKeyExchange(InputStream is, Signer signer) throws IOException
//    {
//        InputStream sigIn = is;
//        if (signer != null)
//        {
//            sigIn = new SignerInputStream(is, signer);
//        }
//
//        byte[] modulusBytes = TlsUtils.readOpaque16(sigIn);
//        byte[] exponentBytes = TlsUtils.readOpaque16(sigIn);
//
//        if (signer != null)
//        {
//            byte[] sigByte = TlsUtils.readOpaque16(is);
//
//            if (!signer.verifySignature(sigByte))
//            {
//                handler.failWithError(AlertLevel.fatal, AlertDescription.bad_certificate);
//            }
//        }
//
//        BigInteger modulus = new BigInteger(1, modulusBytes);
//        BigInteger exponent = new BigInteger(1, exponentBytes);
//
//        this.rsaServerPublicKey = validateRSAPublicKey(new RSAKeyParameters(false, modulus,
//            exponent));
//    }

    protected RSAKeyParameters validateRSAPublicKey(RSAKeyParameters key) throws IOException
    {
        // TODO What is the minimum bit length required?
//        key.getModulus().bitLength();

        if (!key.getExponent().isProbablePrime(2))
        {
            throw new TlsFatalAlert(AlertDescription.illegal_parameter);
        }

        return key;
    }
}
