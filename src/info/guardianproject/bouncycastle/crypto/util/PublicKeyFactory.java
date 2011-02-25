package info.guardianproject.bouncycastle.crypto.util;

import info.guardianproject.bouncycastle.asn1.ASN1InputStream;
import info.guardianproject.bouncycastle.asn1.ASN1Object;
import info.guardianproject.bouncycastle.asn1.ASN1OctetString;
import info.guardianproject.bouncycastle.asn1.ASN1Sequence;
import info.guardianproject.bouncycastle.asn1.DERBitString;
import info.guardianproject.bouncycastle.asn1.DEREncodable;
import info.guardianproject.bouncycastle.asn1.DERInteger;
import info.guardianproject.bouncycastle.asn1.DERObject;
import info.guardianproject.bouncycastle.asn1.DERObjectIdentifier;
import info.guardianproject.bouncycastle.asn1.DEROctetString;
import info.guardianproject.bouncycastle.asn1.nist.NISTNamedCurves;
import info.guardianproject.bouncycastle.asn1.oiw.ElGamalParameter;
import info.guardianproject.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import info.guardianproject.bouncycastle.asn1.pkcs.DHParameter;
import info.guardianproject.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import info.guardianproject.bouncycastle.asn1.sec.SECNamedCurves;
import info.guardianproject.bouncycastle.asn1.teletrust.TeleTrusTNamedCurves;
import info.guardianproject.bouncycastle.asn1.x509.AlgorithmIdentifier;
import info.guardianproject.bouncycastle.asn1.x509.DSAParameter;
import info.guardianproject.bouncycastle.asn1.x509.RSAPublicKeyStructure;
import info.guardianproject.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import info.guardianproject.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import info.guardianproject.bouncycastle.asn1.x9.DHDomainParameters;
import info.guardianproject.bouncycastle.asn1.x9.DHPublicKey;
import info.guardianproject.bouncycastle.asn1.x9.DHValidationParms;
import info.guardianproject.bouncycastle.asn1.x9.X962NamedCurves;
import info.guardianproject.bouncycastle.asn1.x9.X962Parameters;
import info.guardianproject.bouncycastle.asn1.x9.X9ECParameters;
import info.guardianproject.bouncycastle.asn1.x9.X9ECPoint;
import info.guardianproject.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import info.guardianproject.bouncycastle.crypto.params.AsymmetricKeyParameter;
import info.guardianproject.bouncycastle.crypto.params.DHParameters;
import info.guardianproject.bouncycastle.crypto.params.DHPublicKeyParameters;
import info.guardianproject.bouncycastle.crypto.params.DHValidationParameters;
import info.guardianproject.bouncycastle.crypto.params.DSAParameters;
import info.guardianproject.bouncycastle.crypto.params.DSAPublicKeyParameters;
import info.guardianproject.bouncycastle.crypto.params.ECDomainParameters;
import info.guardianproject.bouncycastle.crypto.params.ECPublicKeyParameters;
import info.guardianproject.bouncycastle.crypto.params.ElGamalParameters;
import info.guardianproject.bouncycastle.crypto.params.ElGamalPublicKeyParameters;
import info.guardianproject.bouncycastle.crypto.params.RSAKeyParameters;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;


/**
 * Factory to create asymmetric public key parameters for asymmetric ciphers from range of
 * ASN.1 encoded SubjectPublicKeyInfo objects.
 */
public class PublicKeyFactory
{
    /**
     * Create a public key from a SubjectPublicKeyInfo encoding
     * 
     * @param keyInfoData the SubjectPublicKeyInfo encoding
     * @return the appropriate key parameter
     * @throws IOException on an error decoding the key
     */
    public static AsymmetricKeyParameter createKey(byte[] keyInfoData) throws IOException
    {
        return createKey(SubjectPublicKeyInfo.getInstance(ASN1Object.fromByteArray(keyInfoData)));
    }

    /**
     * Create a public key from a SubjectPublicKeyInfo encoding read from a stream
     * 
     * @param inStr the stream to read the SubjectPublicKeyInfo encoding from
     * @return the appropriate key parameter
     * @throws IOException on an error decoding the key
     */
    public static AsymmetricKeyParameter createKey(InputStream inStr) throws IOException
    {
        return createKey(SubjectPublicKeyInfo.getInstance(new ASN1InputStream(inStr).readObject()));
    }

    /**
     * Create a public key from the passed in SubjectPublicKeyInfo
     * 
     * @param keyInfo the SubjectPublicKeyInfo containing the key data
     * @return the appropriate key parameter
     * @throws IOException on an error decoding the key
     */
    public static AsymmetricKeyParameter createKey(SubjectPublicKeyInfo keyInfo) throws IOException
    {
        AlgorithmIdentifier algId = keyInfo.getAlgorithmId();

        if (algId.getObjectId().equals(PKCSObjectIdentifiers.rsaEncryption)
            || algId.getObjectId().equals(X509ObjectIdentifiers.id_ea_rsa))
        {
            RSAPublicKeyStructure pubKey = new RSAPublicKeyStructure(
                (ASN1Sequence)keyInfo.getPublicKey());

            return new RSAKeyParameters(false, pubKey.getModulus(), pubKey.getPublicExponent());
        }
        else if (algId.getObjectId().equals(X9ObjectIdentifiers.dhpublicnumber))
        {
            DHPublicKey dhPublicKey = DHPublicKey.getInstance(keyInfo.getPublicKey());

            BigInteger y = dhPublicKey.getY().getValue();

            DHDomainParameters dhParams = DHDomainParameters.getInstance(keyInfo.getAlgorithmId().getParameters());

            BigInteger p = dhParams.getP().getValue();
            BigInteger g = dhParams.getG().getValue();
            BigInteger q = dhParams.getQ().getValue();

            BigInteger j = null;
            if (dhParams.getJ() != null)
            {
                j = dhParams.getJ().getValue();
            }

            DHValidationParameters validation = null;
            DHValidationParms dhValidationParms = dhParams.getValidationParms();
            if (dhValidationParms != null)
            {
                byte[] seed = dhValidationParms.getSeed().getBytes();
                BigInteger pgenCounter = dhValidationParms.getPgenCounter().getValue();

                // TODO Check pgenCounter size?

                validation = new DHValidationParameters(seed, pgenCounter.intValue());
            }

            return new DHPublicKeyParameters(y, new DHParameters(p, g, q, j, validation));
        }
        else if (algId.getObjectId().equals(PKCSObjectIdentifiers.dhKeyAgreement))
        {
            DHParameter params = new DHParameter(
                (ASN1Sequence)keyInfo.getAlgorithmId().getParameters());
            DERInteger derY = (DERInteger)keyInfo.getPublicKey();

            BigInteger lVal = params.getL();
            int l = lVal == null ? 0 : lVal.intValue();
            DHParameters dhParams = new DHParameters(params.getP(), params.getG(), null, l);

            return new DHPublicKeyParameters(derY.getValue(), dhParams);
        }
        else if (algId.getObjectId().equals(OIWObjectIdentifiers.elGamalAlgorithm))
        {
            ElGamalParameter params = new ElGamalParameter(
                (ASN1Sequence)keyInfo.getAlgorithmId().getParameters());
            DERInteger derY = (DERInteger)keyInfo.getPublicKey();

            return new ElGamalPublicKeyParameters(derY.getValue(), new ElGamalParameters(
                params.getP(), params.getG()));
        }
        else if (algId.getObjectId().equals(X9ObjectIdentifiers.id_dsa)
            || algId.getObjectId().equals(OIWObjectIdentifiers.dsaWithSHA1))
        {
            DERInteger derY = (DERInteger)keyInfo.getPublicKey();
            DEREncodable de = keyInfo.getAlgorithmId().getParameters();

            DSAParameters parameters = null;
            if (de != null)
            {
                DSAParameter params = DSAParameter.getInstance(de.getDERObject());
                parameters = new DSAParameters(params.getP(), params.getQ(), params.getG());
            }

            return new DSAPublicKeyParameters(derY.getValue(), parameters);
        }
        else if (algId.getObjectId().equals(X9ObjectIdentifiers.id_ecPublicKey))
        {
            X962Parameters params = new X962Parameters(
                (DERObject)keyInfo.getAlgorithmId().getParameters());
            ECDomainParameters dParams = null;

            if (params.isNamedCurve())
            {
                DERObjectIdentifier oid = (DERObjectIdentifier)params.getParameters();
                X9ECParameters ecP = X962NamedCurves.getByOID(oid);

                if (ecP == null)
                {
                    ecP = SECNamedCurves.getByOID(oid);

                    if (ecP == null)
                    {
                        ecP = NISTNamedCurves.getByOID(oid);

                        if (ecP == null)
                        {
                            ecP = TeleTrusTNamedCurves.getByOID(oid);
                        }
                    }
                }

                dParams = new ECDomainParameters(ecP.getCurve(), ecP.getG(), ecP.getN(),
                    ecP.getH(), ecP.getSeed());
            }
            else
            {
                X9ECParameters ecP = new X9ECParameters((ASN1Sequence)params.getParameters());
                dParams = new ECDomainParameters(ecP.getCurve(), ecP.getG(), ecP.getN(),
                    ecP.getH(), ecP.getSeed());
            }

            DERBitString bits = keyInfo.getPublicKeyData();
            byte[] data = bits.getBytes();
            ASN1OctetString key = new DEROctetString(data);

            X9ECPoint derQ = new X9ECPoint(dParams.getCurve(), key);

            return new ECPublicKeyParameters(derQ.getPoint(), dParams);
        }
        else
        {
            throw new RuntimeException("algorithm identifier in key not recognised");
        }
    }
}
