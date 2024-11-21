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
package devrock.cicd.steps.processor.locking;

import java.net.http.HttpClient;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.utils.collection.impl.AttributeContexts;

import devrock.cicd.locking.firebase.FirebaseLockFactory;
import devrock.cicd.model.api.FirebaseRealtimeDistributedLocking;
import devrock.step.api.StepExchangeContext;
import devrock.step.api.StepExchangeContextAttribute;

public class DistributedLocking {
	public static Function<String, Lock> lockManager() {
		StepExchangeContext context = AttributeContexts.peek().findOrNull(StepExchangeContextAttribute.class);
		
		if (context == null)
			return new DefaultLocking();
		
		return context.getService(DistributedLockingAttribute.class, () -> createLockingManager(context));
	}
	
	private static Function<String, Lock> createLockingManager(StepExchangeContext context) {
		Maybe<devrock.cicd.model.api.DistributedLocking> lockingMaybe = context.load(devrock.cicd.model.api.DistributedLocking.T);
		
		if (lockingMaybe.isUnsatisfied()) {
			if (lockingMaybe.isUnsatisfiedBy(NotFound.T)) {
				return new DefaultLocking();
			}
			
			throw new UnsatisfiedMaybeTunneling(lockingMaybe.whyUnsatisfied());
		}
		
		devrock.cicd.model.api.DistributedLocking distributedLocking = lockingMaybe.get();
		
		if (distributedLocking instanceof FirebaseRealtimeDistributedLocking) {
			FirebaseRealtimeDistributedLocking fbLocking = (FirebaseRealtimeDistributedLocking)distributedLocking;
			
			FirebaseLockFactory lockFactory = new FirebaseLockFactory(HttpClient.newHttpClient(), fbLocking.getOwner(), fbLocking.getTableUri());
			lockFactory.setUser(fbLocking.getUser());
			lockFactory.setPassword(fbLocking.getPassword());
			lockFactory.setWebApiKey(fbLocking.getWebApiKey());
			lockFactory.setTouchIntervalInMs(fbLocking.getTouchIntervalInMs());
			lockFactory.setTouchWorkerIntervalInMs(fbLocking.getTouchWorkerIntervalInMs());
			
			return lockFactory;
		}
		
		throw new UnsupportedOperationException("Unsupported DistributedLocking of type: " + distributedLocking.entityType().getTypeSignature());
	}
}
