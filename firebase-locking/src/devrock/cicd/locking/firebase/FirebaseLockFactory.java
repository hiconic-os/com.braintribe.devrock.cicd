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
package devrock.cicd.locking.firebase;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.function.Supplier;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.DestructionAware;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.json.JsonStreamMarshaller;
import com.braintribe.exception.AuthorizationException;
import com.braintribe.exception.CommunicationException;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.reflection.EssentialTypes;
import com.braintribe.utils.lcd.LazyInitialized;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

import devrock.cicd.lock.data.model.LockRecord;

public class FirebaseLockFactory implements Function<String, Lock>, DestructionAware {
	private static final JsonStreamMarshaller marshaller = new JsonStreamMarshaller();
	private static final Logger log = Logger.getLogger(FirebaseLockFactory.class);
	private HttpClient httpClient;
	private String owner;
	
	private long touchIntervalInMs = 10_000;
	private long touchWorkerIntervalInMs = 1_000;
	private ManagedFirebaseLocksImpl managedFirebaseLocks = new ManagedFirebaseLocksImpl();
	private LazyInitialized<FirebaseLockTransportHelper> lazyTransportHelper = new LazyInitialized<>(this::createHelper);
	private String tableUri;
	private String webApiKey;
	private String user;
	private String password;
	
	public FirebaseLockFactory(HttpClient httpClient, String owner, String tableUri) {
		super();
		this.httpClient = httpClient;
		this.owner = owner;
		this.tableUri = tableUri;
		
		managedFirebaseLocks.start();
	}
	
	@Configurable
	public void setTouchIntervalInMs(long touchIntervalInMs) {
		this.touchIntervalInMs = touchIntervalInMs;
	}
	
	@Configurable
	public void setTouchWorkerIntervalInMs(long touchWorkerIntervalInMs) {
		this.touchWorkerIntervalInMs = touchWorkerIntervalInMs;
	}
	
	@Configurable
	public void setWebApiKey(String webApiKey) {
		this.webApiKey = webApiKey;
	}
	
	@Configurable
	public void setUser(String user) {
		this.user = user;
	}
	
	@Configurable
	public void setPassword(String password) {
		this.password = password;
	}
	
	@Override
	public Lock apply(String key) {
		return new FirebaseLock(httpClient, lazyTransportHelper.get(), managedFirebaseLocks, owner, key);
	}
	
	private FirebaseLockTransportHelper createHelper() {
		return new FirebaseLockTransportHelper(tableUri, authenticate());
	}
	
	private String authenticate() {
		if (webApiKey == null)
			return null;
		
		final HttpResponse<String> response;
		
		try {
			HttpRequest request = HttpRequest.newBuilder() //
				.uri(URI.create("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + webApiKey)) //
				.POST(BodyPublishers.ofString(buildAuthJson())) //
				.header("Content-Type", "application/json")
				.build();
			
			HttpClient httpClient = HttpClient.newHttpClient();
			
			BodyHandler<String> handler = BodyHandlers.ofString();
			
			response = httpClient.send(request, handler);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		

		int statusCode = response.statusCode();
		
		if (statusCode >= 200 && statusCode < 300) {
			String body = response.body();
			String token = readTokenFromAuthJson(body);
			return token;
		}
		else if (statusCode == 401){
			throw new AuthorizationException("Could not authenticate for a token at googleapis");
		}
		else
			throw new CommunicationException("HTTP status error " + statusCode + " when authenticating for a token at googleapis");
	}
	
	private String buildAuthJson() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("email", user);
		map.put("password", password);
		map.put("returnSecureToken", true);
		
		GmSerializationOptions options = GmSerializationOptions.deriveDefaults().setInferredRootType(GMF.getTypeReflection().getMapType(EssentialTypes.TYPE_STRING, EssentialTypes.TYPE_OBJECT)).build();
				
		String data = marshaller.encode(map, options);
		
		return data;
	}
	
	private String readTokenFromAuthJson(String json) {
		
		GmSerializationOptions options = GmSerializationOptions.deriveDefaults().setInferredRootType(GMF.getTypeReflection().getMapType(EssentialTypes.TYPE_STRING, EssentialTypes.TYPE_OBJECT)).build();
		
		Map<String, Object> data = (Map<String, Object>) marshaller.decode(json);
		
		return (String) data.get("idToken");
	}
	
	private static class NodeIterator implements Iterator<ManagedFirebaseLock> {
		private ManagedFirebaseLock node;
		private ManagedFirebaseLock to;
		
		public NodeIterator(ManagedFirebaseLock from, ManagedFirebaseLock to) {
			super();
			this.to = to;
			this.node = from;
		}

		@Override
		public boolean hasNext() {
			return node.next != to;
		}
		
		@Override
		public ManagedFirebaseLock next() {
			return node = node.next;
		}
	}
	
	public static class ManagedFirebaseLock {
		public String key;
		public LockRecord record;
		public ManagedFirebaseLock prev;
		public ManagedFirebaseLock next;
		public long lastModified;
		
		public ManagedFirebaseLock() {
			
		}
		
		public ManagedFirebaseLock(boolean selfLinked) {
			if (selfLinked) {
				next = prev = this;
			}
		}
		
		public void insertBefore(ManagedFirebaseLock node) {
			next = node;
			prev = node.prev;
			
			next.prev = this;
			prev.next = this;
		}
		
		public void remove() {
			ManagedFirebaseLock p = prev;
			ManagedFirebaseLock n = next;
			p.next = n;
			n.prev = p;
		}
		
		public void moveBefore(ManagedFirebaseLock node) {
			if (this == node)
				return;
			
			// remove this node from it previous and next node and shortcut them
			remove();
			
			// link this node before node
			insertBefore(node);
		}
	}
	
	public List<String> getCurrentlyManagedLocks() {
		List<String> paths = new ArrayList<>();
		managedFirebaseLocks.iterator().forEachRemaining(n -> paths.add(n.key));
		return paths;
	}
	
	public ManagedFirebaseLocks getManagedFirebaseLocks() {
		return managedFirebaseLocks;
	}
			
	private class ManagedFirebaseLocksImpl extends Thread implements ManagedFirebaseLocks {
		
		private Map<String, ManagedFirebaseLock> nodeMap = new IdentityHashMap<>(); 
		public ManagedFirebaseLock anchor = new ManagedFirebaseLock(true);
		
		public ManagedFirebaseLocksImpl() {
			setPriority(MAX_PRIORITY);
		}
		
		public Iterator<ManagedFirebaseLock> iterator() {
			return new NodeIterator(anchor, anchor);
		}
		
		public Iterator<ManagedFirebaseLock> iterator(ManagedFirebaseLock node) {
			return new NodeIterator(node, anchor);
		}
		
		@Override
		public void remove(String key) {
			synchronized (this) {
				ManagedFirebaseLock node = nodeMap.remove(key);
					
				if (node != null) {
					node.remove();
				}
			}
		}
		
		@Override
		public void add(LockRecord lockRecord) {
			synchronized (this) {
				ManagedFirebaseLock node = new ManagedFirebaseLock();
				String key = lockRecord.getKey();
				node.key = key;
				node.record = lockRecord;
				node.lastModified = System.currentTimeMillis();
				node.insertBefore(anchor);
				
				nodeMap.put(key, node);
			}
		}
		
		@Override
		public void run() {
			while (!Thread.interrupted()) {
				try {
					Thread.sleep(touchWorkerIntervalInMs);
					touchLockFiles();
				} catch (InterruptedException e) {
					break;
				}
			}
		}
		
		private LockRecord[] getLocksToBeTouched() {
			synchronized (this) {
				ManagedFirebaseLock node = anchor.next;
				
				long time = System.currentTimeMillis();
				
				int count = 0;
				
				while (node != anchor) {
					long delta = time - node.lastModified;
					
					if (delta < touchIntervalInMs) {
						break;
					}
					
					count++;
					
					node = node.next;
				}
				
				LockRecord[] records = new LockRecord[count];
				int index = 0;
				ManagedFirebaseLock matchedNode = anchor.next;
				while (matchedNode != node) {
					matchedNode.lastModified = time;
					records[index++] = matchedNode.record;
					matchedNode = matchedNode.next;
				}
				
				// move anchor before node 
				anchor.moveBefore(node);
				
				return records;
			}
		}
		
		private void touchLockFiles() {
			LockRecord[] records = getLocksToBeTouched();
			
			for (LockRecord record: records) {
				try {
					touchLock(record);
				} catch (Exception e) {
					log.error("Error while touching lock with key: " + record.getKey(), e);
				}
			}
		}
		
		private void touchLock(LockRecord lockRecord) {
			Date touchDate = new Date();
			lockRecord.setTouched(touchDate);
			FirebaseLockTransportHelper transportHelper = lazyTransportHelper.get();
			BodyPublisher bodyPublisher = BodyPublishers.ofString(transportHelper.encode(lockRecord));
			HttpRequest request = transportHelper.buildRequest(lockRecord.getKey()) //
				.PUT(bodyPublisher) //
				.header("If-Match", "*") //
				.build();
			
			BodyHandler<Void> bodyHandler = BodyHandlers.discarding();
			
			try {
				HttpResponse<Void> response = httpClient.send(request, bodyHandler);
				
				int statusCode = response.statusCode();
				
				if (statusCode >= 200 && statusCode < 300) {
					return;
				}
				
				log.error("HTTP status code error " + statusCode + " while touching lock with key: " + lockRecord.getKey());
			} catch (Exception e) {
				log.error("Exception while touching lock with key: " + lockRecord.getKey(), e);
			} 
		}

		@Override
		public long lockTimeToLiveInMs() {
			return touchIntervalInMs * 2;
		}
	};
	
	@Override
	public void preDestroy() {
		managedFirebaseLocks.interrupt();
		try {
			managedFirebaseLocks.join();
		}
		catch (InterruptedException e) {
			// NOP
		}
	}

}
