/* Copyright 2011 Google Inc. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.java.otr4j.crypto;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class SMTest {

	private byte[] secret1;
	private byte[] secret2;

	@Before
	public void setUp() throws Exception {
		secret1 = "abcdef".getBytes();
		secret2 = "abCdef".getBytes();
	}

	@Test
	public void testSuccess() throws Exception {
		SMState a = new SMState();
		SMState b = new SMState();

		byte[] msg1 = SM.step1(a, secret1);
		SM.step2a(b, msg1, 123);
		byte[] msg2 = SM.step2b(b, secret1);
		byte[] msg3 = SM.step3(a, msg2);
		byte[] msg4 = SM.step4(b, msg3);
		SM.step5(a, msg4);
		assertEquals(SM.PROG_SUCCEEDED, a.smProgState);
		assertEquals(SM.PROG_SUCCEEDED, b.smProgState);
	}

	@Test
	public void testFail() throws Exception {
		SMState a = new SMState();
		SMState b = new SMState();

		byte[] msg1 = SM.step1(a, secret1);
		SM.step2a(b, msg1, 123);
		byte[] msg2 = SM.step2b(b, secret2);
		byte[] msg3 = SM.step3(a, msg2);
		byte[] msg4 = SM.step4(b, msg3);
		SM.step5(a, msg4);
		assertEquals(SM.PROG_FAILED, a.smProgState);
		assertEquals(SM.PROG_FAILED, b.smProgState);
	}
}
