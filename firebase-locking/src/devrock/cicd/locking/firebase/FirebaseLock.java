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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import com.braintribe.exception.AuthorizationException;
import com.braintribe.exception.CommunicationException;
import com.braintribe.logging.Logger;

import devrock.cicd.lock.data.model.LockRecord;

public class FirebaseLock implements Lock {
	private static final Logger logger = Logger.getLogger(FirebaseLock.class);
	
	private HttpClient httpClient;
	private String key;
	private String owner;
	private long tryIntervalInMs = 500;
	private ManagedFirebaseLocks managedLocks;
	private FirebaseLockTransportHelper transportHelper;
	
	public FirebaseLock(HttpClient httpClient, FirebaseLockTransportHelper transportHelper, ManagedFirebaseLocks managedLocks, String owner, String key) {
		super();
		this.httpClient = httpClient;
		this.transportHelper = transportHelper;
		this.managedLocks = managedLocks;
		this.owner = owner;
		this.key = key;
	}

	@Override
	public void lock() {
		while (true) {
			try {
				if (tryLock(1, TimeUnit.DAYS))
					return;
			}
			catch (InterruptedException e) {
				// noop
			}
		}
	}
	
	private LockRecord buildRecord() {
		Date now = new Date();
		LockRecord lockRecord = LockRecord.T.create();
		lockRecord.setCreated(now);
		lockRecord.setTouched(now);
		lockRecord.setKey(key);
		lockRecord.setOwner(owner);
		return lockRecord;
	}
	
	@Override
	public void lockInterruptibly() throws InterruptedException {
		while (!tryLock(1, TimeUnit.DAYS));
	}

	@Override
	public boolean tryLock() {
		boolean success = _tryLock();
		
		if (success)
			return true;
		
		LockStatus status = getLockStatus();
		
		switch (status) {
		case UNKNOWN: return false;
		case ALIVE: return false;
		case NOT_FOUND: break;
		case STALE: delete(); break;
		}

		return _tryLock();
	}

	private enum LockStatus {
		STALE, NOT_FOUND, ALIVE, UNKNOWN
	}
	
	/**
	 * @return 0 = stale, 1 = non-existent, 2 = 
	 */
	private LockStatus getLockStatus() {
		HttpRequest request = transportHelper.buildRequest(key) //
			.GET() //
			.build();
		
		BodyHandler<String> bodyHandler = BodyHandlers.ofString();
		
		try {
			HttpResponse<String> response = httpClient.send(request, bodyHandler);
			
			int statusCode = response.statusCode();
			
			if (statusCode >= 200 && statusCode < 300) {
				
				LockRecord lockRecord = transportHelper.decode(response.body());
				
				if (lockRecord == null) {
					logger.warn("Got null lock record for " + request.uri() + " although response status code was " + statusCode+ ". Assuming lock not present");
					return LockStatus.UNKNOWN;
				}
				
				long now = System.currentTimeMillis();
				long touched = lockRecord.getTouched().getTime();
				
				long delta = now - touched;
				
				if (delta > managedLocks.lockTimeToLiveInMs() ) {
					return LockStatus.STALE;
				}
				
				return LockStatus.ALIVE;
			}
			else if (statusCode == 404) {
				return LockStatus.NOT_FOUND;
			}
			
			logger.error("HTTP status error " + statusCode + " while checking lock status for key: " + key);
			
			return LockStatus.UNKNOWN;
			 
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	private boolean _tryLock() {
		LockRecord lockRecord = buildRecord();
		BodyPublisher bodyPublisher = BodyPublishers.ofString(transportHelper.encode(lockRecord));
		HttpRequest request = transportHelper.buildRequest(key) //
			.PUT(bodyPublisher) //
			.header("If-Match", "null_etag") //
			.build();
		
		BodyHandler<String> bodyHandler = BodyHandlers.ofString();
		
		final HttpResponse<String> response;
		
		try {
			response = httpClient.send(request, bodyHandler);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
		
		int statusCode = response.statusCode();
		
		if (statusCode >= 200 && statusCode < 300) {
			managedLocks.add(lockRecord);
			return true;
		}
		else if (statusCode == 412) {
			return false;
		}
		else if (statusCode == 401) {
			throw new AuthorizationException("Authentication problem when trying to acquire lock due");
		}
		else
			throw new CommunicationException("HTTP status error " + statusCode + " while while acquiring lock via " + request.uri().toString() + ": " + response.body());
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		long deadTimespan = unit.toMillis(time);
		long start = System.currentTimeMillis();
		
		while (!tryLock()) {
			long now = System.currentTimeMillis();
			
			long millisConsumend = now - start;
			
			long leftOver = deadTimespan - millisConsumend;
			
			if (leftOver <= 0)
				return false;
			
			long wait = Math.min(leftOver, tryIntervalInMs);
			
			Thread.sleep( wait);
		}
		
		return true;
	}

	@Override
	public void unlock() {
		managedLocks.remove(key);
		delete();
	}
	
	private void delete() {
		HttpRequest request = transportHelper.buildRequest(key) //
				.DELETE() //
				.build();
			
			BodyHandler<Void> bodyHandler = BodyHandlers.discarding();
			
			try {
				HttpResponse<Void> response = httpClient.send(request, bodyHandler);
				
				int statusCode = response.statusCode();

				if (statusCode >= 200 && statusCode < 300) {
					return;
				}
				
				logger.error("HTTP status error " + statusCode + " while unlocking by deleting log record for key: " + key);
				
			} catch (Exception e) {
				logger.error("Exception while unlocking by deleting log record for key: " + key, e);
			} 
			
			return;
	}

	@Override
	public Condition newCondition() {
		throw new UnsupportedOperationException("not supported");
	}
	
}
