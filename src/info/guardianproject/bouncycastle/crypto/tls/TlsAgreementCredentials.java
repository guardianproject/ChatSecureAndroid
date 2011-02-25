package info.guardianproject.bouncycastle.crypto.tls;

import info.guardianproject.bouncycastle.crypto.params.AsymmetricKeyParameter;

import java.io.IOException;


public interface TlsAgreementCredentials extends TlsCredentials
{
    byte[] generateAgreement(AsymmetricKeyParameter serverPublicKey) throws IOException;
}
