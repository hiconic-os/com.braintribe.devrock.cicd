package devrock.cicd.steps.gradle.common;

import groovy.lang.Closure;

public interface Closures {
	
	@SuppressWarnings("serial")
	static Closure<Void> from(Runnable runnable) {
		return new Closure<Void>(null) {
			@Override
			public Void call() {
				runnable.run();
				return null;
			}
		};
	}
	static void withConcurrently(Closure<?> closure, Object it) {
		Closure<?> concurrentClosure = (Closure<?>) closure.clone();
		concurrentClosure.setResolveStrategy(Closure.DELEGATE_FIRST);
		concurrentClosure.setDelegate(it);
		concurrentClosure.call(it);
	}
	
	static void with(Closure<?> closure, Object it) {
		Object oldDelegate = closure.getDelegate();
		int oldResolveStrategy = closure.getResolveStrategy();
		
		closure.setDelegate(it);
		closure.setResolveStrategy(Closure.DELEGATE_FIRST);
		
		try {
			closure.call(it);
		}
		finally {
			closure.setDelegate(oldDelegate);
			closure.setResolveStrategy(oldResolveStrategy);
		}
	}
}
