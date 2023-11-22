package devrock.cicd.steps.processor.locking;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class DefaultLocking implements Function<String, Lock> {
	private Map<String, Lock> lockMap = new ConcurrentHashMap<>();
	
	@Override
	public Lock apply(String key) {
		return lockMap.computeIfAbsent(key, k -> new ReentrantLock());
	}
}
