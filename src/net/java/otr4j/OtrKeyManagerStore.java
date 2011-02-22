package net.java.otr4j;

public interface OtrKeyManagerStore {
	public abstract byte[] getPropertyBytes(String id);

	public abstract boolean getPropertyBoolean(String id, boolean defaultValue);

	public abstract void setProperty(String id, byte[] value);

	public abstract void setProperty(String id, boolean value);

	public abstract void removeProperty(String id);
}
