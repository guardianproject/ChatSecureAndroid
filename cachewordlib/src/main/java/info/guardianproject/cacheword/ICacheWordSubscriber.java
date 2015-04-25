
package info.guardianproject.cacheword;

/**
 * A simple interface for notifying about state changes.
 */
public interface ICacheWordSubscriber {

    /**
     * Called when CacheWord is reset and there are no secrets to unlock.
     */
    public void onCacheWordUninitialized();

    /**
     * Called when the cached secrets are wiped from memory.
     */
    public void onCacheWordLocked();

    /**
     * Called when the secrets are available.
     */
    public void onCacheWordOpened();

}
