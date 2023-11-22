package devrock.cicd.locking.firebase.test;

import java.net.http.HttpClient;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;

import com.braintribe.wire.impl.properties.PropertyLookups;

import devrock.cicd.locking.firebase.FirebaseLockFactory;

@Ignore
public class FirebaseLockingTest {
	// htps://hiconic-os-default-rtdb.europe-west1.firebasedatabase.app/locks/opq.json
	
	String tableUri = "https://hiconic-os-default-rtdb.europe-west1.firebasedatabase.app/test-locks/";
	
	
	private static EnvironmentProperties properties = PropertyLookups.create(EnvironmentProperties.class, System::getenv);

	private FirebaseLockFactory createLockFactory() {
		HttpClient httpClient = HttpClient.newHttpClient();
		String owner = "test";
		FirebaseLockFactory factory = new FirebaseLockFactory(httpClient, owner, tableUri);
		factory.setWebApiKey(properties.webApiKey());
		factory.setUser(properties.user());
		factory.setPassword(properties.password());
		return factory;
	}

	
	@Test
	public void testFirebaseLock() throws InterruptedException {
		FirebaseLockFactory factory = createLockFactory();
		
		String key = "devrock:test/" + UUID.randomUUID();
		
		Lock lock1 = factory.apply(key);
		Lock lock2 = factory.apply(key);
		
		boolean lock1_locked = lock1.tryLock();
		
		boolean locked = lock2.tryLock();
		
		lock1.unlock();
		
		boolean locked2 = lock2.tryLock();
		
		lock2.unlock();
		
		Assertions.assertThat(lock1_locked).isEqualTo(true);
		Assertions.assertThat(locked).isEqualTo(false);
		Assertions.assertThat(locked2).isEqualTo(true);
		
		
	}
	
	@Test
	public void testFirebaseLockComplexKey() throws InterruptedException {
		FirebaseLockFactory factory = createLockFactory();
		
		String key = "update-artifact-index:https://maven.pkg.github.com/hiconic-os/maven-test";
		
		Lock lock1 = factory.apply(key);
		
		boolean lock1_locked = lock1.tryLock();
		
		lock1.unlock();
		
		Assertions.assertThat(lock1_locked).isEqualTo(true);
	}
	
	@Test
	public void testStaleLock() throws InterruptedException {
		
		testStaleness(true);
		
		
	}
	
	@Test
	public void testAliveLock() throws InterruptedException {
		
		testStaleness(false);
		
		
	}

	private void testStaleness(boolean stale) throws InterruptedException {
		FirebaseLockFactory factory = createLockFactory();
		factory.setTouchIntervalInMs(1000);
		String key = "devrock:test/" + UUID.randomUUID();
		
		Lock lock1 = factory.apply(key);
		Lock lock2 = factory.apply(key);
		
		boolean lock1_locked = lock1.tryLock();
		Assertions.assertThat(lock1_locked).isEqualTo(true);
		
		if (stale)
			factory.getManagedFirebaseLocks().remove(key);
		
		Thread.sleep(2500);
		
		boolean locked = lock2.tryLock();
		lock2.unlock();
		
		Assertions.assertThat(locked).isEqualTo(stale);
	}
}
