/*
 *  Java OTR library
 *  Copyright (C) 2008-2009  Ian Goldberg, Muhaimeen Ashraf, Andrew Chung,
 *                           Can Tang
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of version 2.1 of the GNU Lesser General
 *  Public License as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/* Ported to otr4j by devrandom */

package net.java.otr4j.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;


import net.java.otr4j.io.OtrInputStream;
import net.java.otr4j.io.OtrOutputStream;
import net.java.otr4j.io.SerializationUtils;


public class SM {
    static public class SMState{
        BigInteger secret, x2, x3, g1, g2, g3, g3o, p, q, pab, qab;
        public int nextExpected;
        int receivedQuestion;
        public int smProgState;
        
        public SMState(){
            g1 = new BigInteger(1, SM.GENERATOR_S);
            smProgState = SM.PROG_OK;
        }
    }

	static public class SMException extends Exception {
		private static final long serialVersionUID = 1L;

		public SMException()
		{
			super("");
		}

		public SMException(Throwable t) {
			super(t.getMessage());
		}
		
		public SMException(String s)
		{
			super(s);
		}
	};
	
	public static final int EXPECT1 = 0;
	public static final int EXPECT2 = 1;
	public static final int EXPECT3 = 2;
	public static final int EXPECT4 = 3;
	public static final int EXPECT5 = 4;
	
	public static final int PROG_OK = 0;
	public static final int PROG_CHEATED = -2;
	public static final int PROG_FAILED = -1;
	public static final int PROG_SUCCEEDED = 1;

	public static final int MSG1_LEN = 6;
	public static final int MSG2_LEN = 11;
	public static final int MSG3_LEN = 8;
	public static final int MSG4_LEN = 3;
	
	public static final BigInteger MODULUS_S = new BigInteger(
			"FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"+
		    "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"+
		    "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"+
		    "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"+
		    "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"+
		    "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"+
		    "83655D23DCA3AD961C62F356208552BB9ED529077096966D"+
		    "670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFF", 16);
	
	public static final BigInteger MODULUS_MINUS_2 = new BigInteger(
			"FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"+
		    "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"+
		    "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"+
		    "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"+
		    "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"+
		    "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"+
		    "83655D23DCA3AD961C62F356208552BB9ED529077096966D"+
		    "670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFD", 16);
	
	public static final BigInteger ORDER_S = new BigInteger(
			"7FFFFFFFFFFFFFFFE487ED5110B4611A62633145C06E0E68"+
		    "948127044533E63A0105DF531D89CD9128A5043CC71A026E"+
		    "F7CA8CD9E69D218D98158536F92F8A1BA7F09AB6B6A8E122"+
		    "F242DABB312F3F637A262174D31BF6B585FFAE5B7A035BF6"+
		    "F71C35FDAD44CFD2D74F9208BE258FF324943328F6722D9E"+
		    "E1003E5C50B1DF82CC6D241B0E2AE9CD348B1FD47E9267AF"+
		    "C1B2AE91EE51D6CB0E3179AB1042A95DCF6A9483B84B4B36"+
		    "B3861AA7255E4C0278BA36046511B993FFFFFFFFFFFFFFFF", 16);
	
	public static final byte[] GENERATOR_S = Util.hexStringToBytes("02");
	public static final int MOD_LEN_BITS = 1536;
	public static final int MOD_LEN_BYTES = 192;
	
	
	/** Generate a random exponent */
	public static BigInteger randomExponent() {
		SecureRandom sr = new SecureRandom();
		byte[] sb = new byte[MOD_LEN_BYTES];
		sr.nextBytes(sb);
		return new BigInteger(1, sb);
	}
	
	/**
	 * Hash one or two BigIntegers.  To hash only one BigInteger, b may be set to NULL.
	 */
	public static BigInteger hash(int version, BigInteger a, BigInteger b) throws SMException
	{
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			sha256.update((byte)version);
			sha256.update(SerializationUtils.writeMpi(a));
			if (b != null)
				sha256.update(SerializationUtils.writeMpi(b));
			return new BigInteger(1, sha256.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new SMException("cannot find SHA-256");
		} catch (IOException e) {
			throw new SMException("cannot serialize bigint");
		}
	}
	
	public static byte[] serialize(BigInteger[] ints) throws SMException {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			OtrOutputStream oos = new OtrOutputStream(out);
			oos.writeInt(ints.length);
			for (BigInteger i : ints) {
				oos.writeBigInt(i);
			}
			byte[] b = out.toByteArray();
			oos.close();
			return b;
		} catch (IOException ex) {
			throw new SMException("cannot serialize bigints");
		}
	}
	
	public static BigInteger[] unserialize(byte[] bytes) throws SMException {
		try {
			ByteArrayInputStream in = new ByteArrayInputStream(bytes);
			OtrInputStream ois = new OtrInputStream(in);
			int len = ois.readInt();
			if (len > 100)
				throw new SMException("Too many ints");
			BigInteger[] ints = new BigInteger[len];
			for (int i = 0 ; i < len ; i++) {
				ints[i] = ois.readBigInt();
			}
			ois.close();
			return ints;
		} catch (IOException ex) {
			throw new SMException("cannot unserialize bigints");
		}
	}
	
	/** Check that an BigInteger is in the right range to be a (non-unit) group
	 * element */
	public static boolean checkGroupElem(BigInteger g)
	{
		return !(g.compareTo(BigInteger.valueOf(2)) > 0 &&
				g.compareTo(SM.MODULUS_MINUS_2) < 0);
	}
	
	/** Check that an BigInteger is in the right range to be a (non-zero) exponent */
	public static boolean checkExpon(BigInteger x)
	{
		return !(x.compareTo(BigInteger.ONE) > 0 && x.compareTo(SM.ORDER_S) <= 0);
	}
	
	/**
	 * Proof of knowledge of a discrete logarithm
	 * @throws SMException 
	 */
	public static BigInteger[] proofKnowLog(BigInteger g, BigInteger x, int version) throws SMException
	{
	    BigInteger r = randomExponent();
	    BigInteger temp = g.modPow(r, SM.MODULUS_S);
	    BigInteger c = hash(version, temp, null);
	    temp = x.multiply(c).mod(ORDER_S);
	    BigInteger d = r.subtract(temp).mod(ORDER_S);
	    BigInteger[] ret = new BigInteger[2];
	    ret[0]=c;
	    ret[1]=d;
	    return ret;
	}
	
	/**
	 * Verify a proof of knowledge of a discrete logarithm.  Checks that c = h(g^d x^c)
	 * @throws SMException 
	 */
	public static int checkKnowLog(BigInteger c, BigInteger d, BigInteger g, BigInteger x, int version) throws SMException
	{

	    BigInteger gd = g.modPow(d, MODULUS_S);
	    BigInteger xc = x.modPow(c, MODULUS_S);
	    BigInteger gdxc = gd.multiply(xc).mod(MODULUS_S);
	    BigInteger hgdxc = hash(version, gdxc, null);
	    
	    return hgdxc.compareTo(c);
	}
	
	/**
	 * Proof of knowledge of coordinates with first components being equal
	 * @throws SMException 
	 */
	public static BigInteger[] proofEqualCoords(SMState state, BigInteger r, int version) throws SMException
	{
	    BigInteger r1 = randomExponent();
	    BigInteger r2 = randomExponent();

	    /* Compute the value of c, as c = h(g3^r1, g1^r1 g2^r2) */
	    BigInteger temp1 = state.g1.modPow(r1, MODULUS_S);
	    BigInteger temp2 = state.g2.modPow(r2, MODULUS_S);
	    temp2 = temp1.multiply(temp2).mod(MODULUS_S);
	    temp1 = state.g3.modPow(r1, MODULUS_S);    
	    BigInteger c = hash(version, temp1, temp2);
	    
	    /* Compute the d values, as d1 = r1 - r c, d2 = r2 - secret c */
	    temp1 = r.multiply(c).mod(ORDER_S);
	    BigInteger d1 = r1.subtract(temp1).mod(ORDER_S);

	    temp1 = state.secret.multiply(c).mod(ORDER_S);
	    BigInteger d2 = r2.subtract(temp1).mod(ORDER_S);

	    BigInteger[] ret = new BigInteger[3];
	    ret[0]=c;
	    ret[1]=d1;
	    ret[2]=d2;
	    return ret;
	}
	
	/**
	 * Verify a proof of knowledge of coordinates with first components being equal
	 * @throws SMException 
	 */
	public static int checkEqualCoords(BigInteger c, BigInteger d1, BigInteger d2, BigInteger p,
			BigInteger q, SMState state, int version) throws SMException
	{

	    /* To verify, we test that hash(g3^d1 * p^c, g1^d1 * g2^d2 * q^c) = c
	     * If indeed c = hash(g3^r1, g1^r1 g2^r2), d1 = r1 - r*c,
	     * d2 = r2 - secret*c.  And if indeed p = g3^r, q = g1^r * g2^secret
	     * Then we should have that:
	     *   hash(g3^d1 * p^c, g1^d1 * g2^d2 * q^c)
	     * = hash(g3^(r1 - r*c + r*c), g1^(r1 - r*c + q*c) *
	     *      g2^(r2 - secret*c + secret*c))
	     * = hash(g3^r1, g1^r1 g2^r2)
	     * = c
	     */
		BigInteger temp2 = state.g3.modPow(d1, MODULUS_S);
		BigInteger temp3 = p.modPow(c, MODULUS_S);
		BigInteger temp1 = temp2.multiply(temp3).mod(MODULUS_S);
		
		temp2 = state.g1.modPow(d1, MODULUS_S);
		temp3 = state.g2.modPow(d2, MODULUS_S);
		temp2 = temp2.multiply(temp3).mod(MODULUS_S);
		temp3 = q.modPow(c, MODULUS_S);
		temp2 = temp3.multiply(temp2).mod(MODULUS_S);
		
	    BigInteger cprime=hash(version, temp1, temp2);

	    return c.compareTo(cprime);
	}
	
	/**
	 * Proof of knowledge of logs with exponents being equal
	 * @throws SMException 
	 */
	public static BigInteger[] proofEqualLogs(SMState state, int version) throws SMException
	{
	    BigInteger r = randomExponent();

	    /* Compute the value of c, as c = h(g1^r, (Qa/Qb)^r) */
	    BigInteger temp1 = state.g1.modPow(r, MODULUS_S);
	    BigInteger temp2 = state.qab.modPow(r, MODULUS_S);
	    BigInteger c = hash(version, temp1, temp2);

	    /* Compute the d values, as d = r - x3 c */
	    temp1 = state.x3.multiply(c).mod(ORDER_S);
	    BigInteger d = r.subtract(temp1).mod(ORDER_S);

	    BigInteger[] ret = new BigInteger[2];
	    ret[0]=c;
	    ret[1]=d;
	    return ret;
	}
	
	/**
	 * Verify a proof of knowledge of logs with exponents being equal
	 * @throws SMException 
	 */
	public static int checkEqualLogs(BigInteger c, BigInteger d, BigInteger r, SMState state, int version) throws SMException
	{

	    /* Here, we recall the exponents used to create g3.
	     * If we have previously seen g3o = g1^x where x is unknown
	     * during the DH exchange to produce g3, then we may proceed with:
	     * 
	     * To verify, we test that hash(g1^d * g3o^c, qab^d * r^c) = c
	     * If indeed c = hash(g1^r1, qab^r1), d = r1- x * c
	     * And if indeed r = qab^x
	     * Then we should have that:
	     *   hash(g1^d * g3o^c, qab^d r^c)
	     * = hash(g1^(r1 - x*c + x*c), qab^(r1 - x*c + x*c))
	     * = hash(g1^r1, qab^r1)
	     * = c
	     */
		
		BigInteger temp2 = state.g1.modPow(d, MODULUS_S);
		BigInteger temp3 = state.g3o.modPow(c, MODULUS_S);
		BigInteger temp1 = temp2.multiply(temp3).mod(MODULUS_S);
		
		temp3 = state.qab.modPow(d, MODULUS_S);
		temp2 = r.modPow(c, MODULUS_S);
		temp2 = temp3.multiply(temp2).mod(MODULUS_S);

	    BigInteger cprime = hash(version, temp1, temp2);

	    return c.compareTo(cprime);
	}
	
	/** Create first message in SMP exchange.  Input is Alice's secret value
	 * which this protocol aims to compare to Bob's. The return value is a serialized
	 * BigInteger array whose elements correspond to the following:
	 * [0] = g2a, Alice's half of DH exchange to determine g2
	 * [1] = c2, [2] = d2, Alice's ZK proof of knowledge of g2a exponent
	 * [3] = g3a, Alice's half of DH exchange to determine g3
	 * [4] = c3, [5] = d3, Alice's ZK proof of knowledge of g3a exponent 
	 * @throws SMException */
	public static byte[] step1(SMState astate, byte[] secret) throws SMException
	{
	    /* Initialize the sm state or update the secret */
		//Util.checkBytes("secret", secret);
	    BigInteger secret_mpi = new BigInteger(1, secret);

	    astate.secret = secret_mpi;
	    astate.receivedQuestion = 0;
	    astate.x2 = randomExponent();
	    astate.x3 = randomExponent();

	    BigInteger[] msg1 = new BigInteger[6];
	    msg1[0] = astate.g1.modPow(astate.x2, MODULUS_S);
	    BigInteger[] res = proofKnowLog(astate.g1, astate.x2, 1);
	    msg1[1]=res[0];
	    msg1[2]=res[1];
	    
	    msg1[3] = astate.g1.modPow(astate.x3, MODULUS_S);
	    res = proofKnowLog(astate.g1, astate.x3, 2);
	    msg1[4]=res[0];
	    msg1[5]=res[1];

	    byte[] ret = serialize(msg1);
	    astate.smProgState = PROG_OK;

	    return ret;
	}
	
	/** Receive the first message in SMP exchange, which was generated by
	 *  step1.  Input is saved until the user inputs their secret
	 * information.  No output. 
	 * @throws SMException */
	public static void step2a(SMState bstate, byte[] input, int received_question) throws SMException
	{

	    /* Initialize the sm state if needed */

	    bstate.receivedQuestion = received_question;
	    bstate.smProgState = PROG_CHEATED;

	    /* Read from input to find the mpis */
	    BigInteger[] msg1 = unserialize(input);

	    if (checkGroupElem(msg1[0]) || checkExpon(msg1[2]) ||
	    		checkGroupElem(msg1[3]) || checkExpon(msg1[5])) {
	        throw new SMException("Invalid parameter");
	    }

	    /* Store Alice's g3a value for later in the protocol */
	    bstate.g3o=msg1[3];
	    
	    /* Verify Alice's proofs */
	    if (checkKnowLog(msg1[1], msg1[2], bstate.g1, msg1[0], 1)!=0
	    	||checkKnowLog(msg1[4], msg1[5], bstate.g1, msg1[3], 2)!=0) {
	        throw new SMException("Proof checking failed");
	    }

	    /* Create Bob's half of the generators g2 and g3 */
	    
	    bstate.x2 = randomExponent();
	    bstate.x3 = randomExponent();

	    /* Combine the two halves from Bob and Alice and determine g2 and g3 */
	    bstate.g2= msg1[0].modPow(bstate.x2, MODULUS_S);
	    //Util.checkBytes("g2b", bstate.g2.getValue());
	    bstate.g3= msg1[3].modPow(bstate.x3, MODULUS_S);
	    //Util.checkBytes("g3b", bstate.g3.getValue());
	    
	    bstate.smProgState = PROG_OK;
	}
	
	/** Create second message in SMP exchange.  Input is Bob's secret value.
	 * Information from earlier steps in the exchange is taken from Bob's
	 * state.  Output is a serialized mpi array whose elements correspond
	 * to the following:
	 * [0] = g2b, Bob's half of DH exchange to determine g2
	 * [1] = c2, [2] = d2, Bob's ZK proof of knowledge of g2b exponent
	 * [3] = g3b, Bob's half of DH exchange to determine g3
	 * [4] = c3, [5] = d3, Bob's ZK proof of knowledge of g3b exponent
	 * [6] = pb, [7] = qb, Bob's halves of the (Pa/Pb) and (Qa/Qb) values
	 * [8] = cp, [9] = d5, [10] = d6, Bob's ZK proof that pb, qb formed correctly 
	 * @throws SMException */
	public static byte[] step2b(SMState bstate, byte[] secret) throws SMException
	{
	    /* Convert the given secret to the proper form and store it */
		//Util.checkBytes("secret", secret);
		BigInteger secret_mpi = new BigInteger(1, secret);
		bstate.secret = secret_mpi;

	    BigInteger[] msg2 = new BigInteger[11];
	    msg2[0] = bstate.g1.modPow(bstate.x2, MODULUS_S);
	    BigInteger[] res = proofKnowLog(bstate.g1,bstate.x2,3);
	    msg2[1]=res[0];
	    msg2[2]=res[1];

	    msg2[3] = bstate.g1.modPow(bstate.x3, MODULUS_S);
	    res = proofKnowLog(bstate.g1,bstate.x3,4);
	    msg2[4]=res[0];
	    msg2[5]=res[1];

	    /* Calculate P and Q values for Bob */
	    BigInteger r = randomExponent();
	    //BigInteger r = new BigInteger(SM.GENERATOR_S);

	    bstate.p = bstate.g3.modPow(r, MODULUS_S);
	    //Util.checkBytes("Pb", bstate.p.getValue());
	    msg2[6]=bstate.p;
	    BigInteger qb1 = bstate.g1.modPow(r, MODULUS_S);
	    //Util.checkBytes("Qb1", qb1.getValue());
	    BigInteger qb2 = bstate.g2.modPow(bstate.secret, MODULUS_S);
	    //Util.checkBytes("Qb2", qb2.getValue());
	    //Util.checkBytes("g2", bstate.g2.getValue());
	    //Util.checkBytes("secret", bstate.secret.getValue());
	    bstate.q = qb1.multiply(qb2).mod(MODULUS_S);
	    //Util.checkBytes("Qb", bstate.q.getValue());
	    msg2[7] = bstate.q;
	    
	    res = proofEqualCoords(bstate, r, 5);
	    msg2[8]=res[0];
	    msg2[9]=res[1];
	    msg2[10]=res[2];

	    /* Convert to serialized form */
	    return serialize(msg2);
	    
	}
	/** Create third message in SMP exchange.  Input is a message generated
	 * by otrl_sm_step2b. Output is a serialized mpi array whose elements
	 * correspond to the following:
	 * [0] = pa, [1] = qa, Alice's halves of the (Pa/Pb) and (Qa/Qb) values
	 * [2] = cp, [3] = d5, [4] = d6, Alice's ZK proof that pa, qa formed correctly
	 * [5] = ra, calculated as (Qa/Qb)^x3 where x3 is the exponent used in g3a
	 * [6] = cr, [7] = d7, Alice's ZK proof that ra is formed correctly 
	 * @throws SMException */
	public static byte[] step3(SMState astate, byte[] input) throws SMException
	{
	    /* Read from input to find the mpis */
	    astate.smProgState = PROG_CHEATED;
	    
	    BigInteger[] msg2 = unserialize(input);
	    if (checkGroupElem(msg2[0]) || checkGroupElem(msg2[3]) ||
		    checkGroupElem(msg2[6]) || checkGroupElem(msg2[7]) ||
		    checkExpon(msg2[2]) || checkExpon(msg2[5]) ||
		    checkExpon(msg2[9]) || checkExpon(msg2[10])) {
	        throw new SMException("Invalid Parameter");
	    }

	    BigInteger[] msg3 = new BigInteger[8];

	    /* Store Bob's g3a value for later in the protocol */
	    astate.g3o = msg2[3];

	    /* Verify Bob's knowledge of discreet log proofs */
	    if (checkKnowLog(msg2[1], msg2[2], astate.g1, msg2[0], 3)!=0 || 
	        checkKnowLog(msg2[4], msg2[5], astate.g1, msg2[3], 4)!=0) {
	    	throw new SMException("Proof checking failed");
	    }

	    /* Combine the two halves from Bob and Alice and determine g2 and g3 */
	    astate.g2 = msg2[0].modPow(astate.x2, MODULUS_S);
	    //Util.checkBytes("g2a", astate.g2.getValue());
	    astate.g3 = msg2[3].modPow(astate.x3, MODULUS_S);
	    //Util.checkBytes("g3a", astate.g3.getValue());
	    
	    /* Verify Bob's coordinate equality proof */
	    if (checkEqualCoords(msg2[8], msg2[9], msg2[10], msg2[6], msg2[7], astate, 5)!=0)
	    	throw new SMException("Invalid Parameter");

	    /* Calculate P and Q values for Alice */
	    BigInteger r = randomExponent();
	    //BigInteger r = new BigInteger(SM.GENERATOR_S);

	    astate.p = astate.g3.modPow(r, MODULUS_S);
	    //Util.checkBytes("Pa", astate.p.getValue());
	    msg3[0]=astate.p;
	    BigInteger qa1 = astate.g1.modPow(r, MODULUS_S);
	    //Util.checkBytes("Qa1", qa1.getValue());
	    BigInteger qa2 = astate.g2.modPow(astate.secret, MODULUS_S);
	    //Util.checkBytes("Qa2", qa2.getValue());
	    //Util.checkBytes("g2", astate.g2.getValue());
	    //Util.checkBytes("secret", astate.secret.getValue());
	    astate.q = qa1.multiply(qa2).mod(MODULUS_S);
	    msg3[1] = astate.q;
	    //Util.checkBytes("Qa", astate.q.getValue());
	    
	    BigInteger[] res = proofEqualCoords(astate,r,6);
	    msg3[2] = res[0];
	    msg3[3] = res[1];
	    msg3[4] = res[2];


	    /* Calculate Ra and proof */
	    BigInteger inv = msg2[6].modInverse(MODULUS_S);
	    astate.pab = astate.p.multiply(inv).mod(MODULUS_S);
	    inv = msg2[7].modInverse(MODULUS_S);
	    astate.qab = astate.q.multiply(inv).mod(MODULUS_S);
	    msg3[5] = astate.qab.modPow(astate.x3, MODULUS_S);
	    res = proofEqualLogs(astate, 7);
	    msg3[6]=res[0];
	    msg3[7]=res[1];
	    
	    byte[] output = serialize(msg3);
	   
	    astate.smProgState = PROG_OK;
	    return output;
	}

	/** Create final message in SMP exchange.  Input is a message generated
	 * by otrl_sm_step3. Output is a serialized mpi array whose elements
	 * correspond to the following:
	 * [0] = rb, calculated as (Qa/Qb)^x3 where x3 is the exponent used in g3b
	 * [1] = cr, [2] = d7, Bob's ZK proof that rb is formed correctly
	 * This method also checks if Alice and Bob's secrets were the same.  If
	 * so, it returns NO_ERROR.  If the secrets differ, an INV_VALUE error is
	 * returned instead. 
	 * @throws SMException */
	public static byte[] step4(SMState bstate, byte[] input) throws SMException
	{
	    /* Read from input to find the mpis */
	    BigInteger[] msg3 = unserialize(input);

	    bstate.smProgState = PROG_CHEATED;
	    
	    BigInteger[] msg4 = new BigInteger[3];

	    if (checkGroupElem(msg3[0]) || checkGroupElem(msg3[1]) ||
		    checkGroupElem(msg3[5]) || checkExpon(msg3[3]) ||
		    checkExpon(msg3[4]) || checkExpon(msg3[7]))  {
	    	throw new SMException("Invalid Parameter");
	    }

	    /* Verify Alice's coordinate equality proof */
	    if (checkEqualCoords(msg3[2], msg3[3], msg3[4], msg3[0], msg3[1], bstate, 6)!=0)
	    	throw new SMException("Invalid Parameter");
	    
	    /* Find Pa/Pb and Qa/Qb */
	    BigInteger inv = bstate.p.modInverse(MODULUS_S);
	    bstate.pab = msg3[0].multiply(inv).mod(MODULUS_S);
	    inv = bstate.q.modInverse(MODULUS_S);
	    bstate.qab = msg3[1].multiply(inv).mod(MODULUS_S);
   

	    /* Verify Alice's log equality proof */
	    if (checkEqualLogs(msg3[6], msg3[7], msg3[5], bstate, 7)!=0){
	    	throw new SMException("Proof checking failed");
	    }

	    /* Calculate Rb and proof */
	    msg4[0] = bstate.qab.modPow(bstate.x3, MODULUS_S);
	    BigInteger[] res = proofEqualLogs(bstate,8);
	    msg4[1]=res[0];
	    msg4[2]=res[1];
	    
	    byte[] output = serialize(msg4);

	    /* Calculate Rab and verify that secrets match */
	    
	    BigInteger rab = msg3[5].modPow(bstate.x3, MODULUS_S);
	    //Util.checkBytes("rab", rab.getValue());
	    //Util.checkBytes("pab", bstate.pab.getValue());
	    int comp = rab.compareTo(bstate.pab);

	    bstate.smProgState = (comp!=0) ? PROG_FAILED : PROG_SUCCEEDED;

	    return output;
	}

	/** Receives the final SMP message, which was generated in otrl_sm_step.
	 * This method checks if Alice and Bob's secrets were the same.  If
	 * so, it returns NO_ERROR.  If the secrets differ, an INV_VALUE error is
	 * returned instead. 
	 * @throws SMException */
	public static void step5(SMState astate, byte[] input) throws SMException
	{
	    /* Read from input to find the mpis */
	    BigInteger[] msg4 = unserialize(input);
	    astate.smProgState = PROG_CHEATED;

	    if (checkGroupElem(msg4[0])|| checkExpon(msg4[2])) {
	    	throw new SMException("Invalid Parameter");
	    }

	    /* Verify Bob's log equality proof */
	    if (checkEqualLogs(msg4[1], msg4[2], msg4[0], astate, 8)!=0)
	    	throw new SMException("Invalid Parameter");

	    /* Calculate Rab and verify that secrets match */
	    
	    BigInteger rab = msg4[0].modPow(astate.x3, MODULUS_S);
	    //Util.checkBytes("rab", rab.getValue());
	    //Util.checkBytes("pab", astate.pab.getValue());
	    int comp = rab.compareTo(astate.pab);
	    if (comp!=0){
	    	//System.out.println("checking failed");
	    }

	    astate.smProgState = (comp!=0) ? PROG_FAILED : PROG_SUCCEEDED;

	    return;
	}

	// ***************************************************
	// Session stuff - perhaps factor out
	
	public static void main(String[] args) throws SMException {
		BigInteger res = SM.MODULUS_MINUS_2.subtract(SM.MODULUS_S).mod(SM.MODULUS_S);
		String ss = Util.bytesToHexString(res.toByteArray());
		System.out.println(ss);
		
		byte[] secret1 = "abcdef".getBytes();
		SMState a = new SMState();
		SMState b = new SMState();
		
		byte[] msg1 = SM.step1(a, secret1);
		SM.step2a(b, msg1, 123);
		byte[] msg2 = SM.step2b(b, secret1);
		byte[] msg3 = SM.step3(a, msg2);
		byte[] msg4 = SM.step4(b, msg3);
		SM.step5(a, msg4);
	}
}
