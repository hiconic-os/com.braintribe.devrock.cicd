// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
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
