package net.java.otr4j;

public class OtrPolicyImpl implements OtrPolicy {

	public OtrPolicyImpl() {
		this.setPolicy(NEVER);
	}

	public OtrPolicyImpl(int policy) {
		this.setPolicy(policy);
	}

	private int policy;

	public int getPolicy() {
		return policy;
	}

	private void setPolicy(int policy) {
		this.policy = policy;
	}

	public boolean getAllowV1() {
		return (policy & OtrPolicy.ALLOW_V1) != 0;
	}

	public boolean getAllowV2() {
		return (policy & OtrPolicy.ALLOW_V2) != 0;
	}

	public boolean getErrorStartAKE() {
		return (policy & OtrPolicy.ERROR_START_AKE) != 0;
	}

	public boolean getRequireEncryption() {
		return getEnableManual()
				&& (policy & OtrPolicy.REQUIRE_ENCRYPTION) != 0;
	}

	public boolean getSendWhitespaceTag() {
		return (policy & OtrPolicy.SEND_WHITESPACE_TAG) != 0;
	}

	public boolean getWhitespaceStartAKE() {
		return (policy & OtrPolicy.WHITESPACE_START_AKE) != 0;
	}

	public void setAllowV1(boolean value) {
		if (value)
			policy |= ALLOW_V1;
		else
			policy &= ~ALLOW_V1;
	}

	public void setAllowV2(boolean value) {
		if (value)
			policy |= ALLOW_V2;
		else
			policy &= ~ALLOW_V2;
	}

	public void setErrorStartAKE(boolean value) {
		if (value)
			policy |= ERROR_START_AKE;
		else
			policy &= ~ERROR_START_AKE;
	}

	public void setRequireEncryption(boolean value) {
		if (value)
			policy |= REQUIRE_ENCRYPTION;
		else
			policy &= ~REQUIRE_ENCRYPTION;
	}

	public void setSendWhitespaceTag(boolean value) {
		if (value)
			policy |= SEND_WHITESPACE_TAG;
		else
			policy &= ~SEND_WHITESPACE_TAG;
	}

	public void setWhitespaceStartAKE(boolean value) {
		if (value)
			policy |= WHITESPACE_START_AKE;
		else
			policy &= ~WHITESPACE_START_AKE;
	}

	public boolean getEnableAlways() {
		return getEnableManual() && getErrorStartAKE()
				&& getSendWhitespaceTag() && getWhitespaceStartAKE();
	}

	public void setEnableAlways(boolean value) {
		if (value)
			setEnableManual(true);

		setErrorStartAKE(value);
		setSendWhitespaceTag(value);
		setWhitespaceStartAKE(value);

	}

	public boolean getEnableManual() {
		return getAllowV1() && getAllowV2();
	}

	public void setEnableManual(boolean value) {
		setAllowV1(value);
		setAllowV2(value);
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null || obj.getClass() != this.getClass())
			return false;

		OtrPolicy policy = (OtrPolicy) obj;

		return policy.getPolicy() == this.getPolicy();
	}

	public int hashCode() {
		return this.getPolicy();
	}
}
