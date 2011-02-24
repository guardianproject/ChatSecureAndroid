package info.guardianproject.otr;

import java.security.SecureRandom;

public class AndroidSecureRandom extends SecureRandom {

	@Override
	public byte[] generateSeed(int numBytes) {
		// TODO Auto-generated method stub
		return super.generateSeed(numBytes);
	}

	@Override
	public String getAlgorithm() {
		// TODO Auto-generated method stub
		return super.getAlgorithm();
	}

	@Override
	public synchronized void nextBytes(byte[] bytes) {
		// TODO Auto-generated method stub
		super.nextBytes(bytes);
	}

	@Override
	public synchronized void setSeed(byte[] seed) {
		// TODO Auto-generated method stub
		super.setSeed(seed);
	}

	@Override
	public void setSeed(long seed) {
		// TODO Auto-generated method stub
		super.setSeed(seed);
	}

}
