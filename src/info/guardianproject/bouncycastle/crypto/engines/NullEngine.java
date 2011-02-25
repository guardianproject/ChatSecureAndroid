package info.guardianproject.bouncycastle.crypto.engines;

import info.guardianproject.bouncycastle.crypto.BlockCipher;
import info.guardianproject.bouncycastle.crypto.CipherParameters;
import info.guardianproject.bouncycastle.crypto.DataLengthException;

/**
 * The no-op engine that just copies bytes through, irrespective of whether encrypting and decrypting.
 * Provided for the sake of completeness.
 */
public class NullEngine implements BlockCipher
{
    private boolean initialised;
    protected static final int BLOCK_SIZE = 1;
    
    /**
     * Standard constructor.
     */
    public NullEngine()
    {
        super();
    }

    /* (non-Javadoc)
     * @see info.guardianproject.bouncycastle.crypto.BlockCipher#init(boolean, info.guardianproject.bouncycastle.crypto.CipherParameters)
     */
    public void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException
    {
        // we don't mind any parameters that may come in
        this.initialised = true;
    }

    /* (non-Javadoc)
     * @see info.guardianproject.bouncycastle.crypto.BlockCipher#getAlgorithmName()
     */
    public String getAlgorithmName()
    {
        return "Null";
    }

    /* (non-Javadoc)
     * @see info.guardianproject.bouncycastle.crypto.BlockCipher#getBlockSize()
     */
    public int getBlockSize()
    {
        return BLOCK_SIZE;
    }

    /* (non-Javadoc)
     * @see info.guardianproject.bouncycastle.crypto.BlockCipher#processBlock(byte[], int, byte[], int)
     */
    public int processBlock(byte[] in, int inOff, byte[] out, int outOff)
        throws DataLengthException, IllegalStateException
    {
        if (!initialised)
        {
            throw new IllegalStateException("Null engine not initialised");
        }
            if ((inOff + BLOCK_SIZE) > in.length)
            {
                throw new DataLengthException("input buffer too short");
            }

            if ((outOff + BLOCK_SIZE) > out.length)
            {
                throw new DataLengthException("output buffer too short");
            }
            
            for (int i = 0; i < BLOCK_SIZE; ++i)
            {
                out[outOff + i] = in[inOff + i];
            }
            
            return BLOCK_SIZE;
    }

    /* (non-Javadoc)
     * @see info.guardianproject.bouncycastle.crypto.BlockCipher#reset()
     */
    public void reset()
    {
        // nothing needs to be done
    }
}
