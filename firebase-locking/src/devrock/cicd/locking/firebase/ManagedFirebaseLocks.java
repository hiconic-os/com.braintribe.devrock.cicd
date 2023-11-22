package devrock.cicd.locking.firebase;

import devrock.cicd.lock.data.model.LockRecord;

/**
 * @author pit
 * @author dirk
 *
 */
public interface ManagedFirebaseLocks {
	/**
	 * @param file
	 */
	void add(LockRecord lockRecord);
	/**
	 * @param file
	 */
	void remove(String key);
	/**
	 * @return
	 */
	long lockTimeToLiveInMs();
}
