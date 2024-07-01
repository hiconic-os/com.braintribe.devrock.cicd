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
package com.braintribe.devrock.cicd.ant.processing;

import java.io.PrintStream;

import org.apache.tools.ant.Project;

public class Demuxing {
	private static DemuxPrintStream hcDemuxOut, hcDemuxErr;
	private static PrintStream originalOut, originalErr;
	private static Object demuxMonitor = new Object();
	private static volatile boolean configuredDemuxing = false;
	
	public static void bindSubProjectToCurrentThread(Project project) {
		ensureDemuxing();

		hcDemuxOut.bindProjectToCurrentThread(project);
		hcDemuxErr.bindProjectToCurrentThread(project);
	}
	
	public static void unbindSubProjectFromCurrentThread() {
		hcDemuxOut.unbindCurrentThread();
		hcDemuxErr.unbindCurrentThread();
	}
	
	private static void ensureDemuxing() {
		if (!configuredDemuxing)
			synchronized (demuxMonitor) {
				if (!configuredDemuxing) {
					prepareHcDemuxOutputStream();
					configuredDemuxing = true;
				}
			}
	}

	
	/** @see DrDemuxOutputStream */
	private static void prepareHcDemuxOutputStream() {
		originalOut = System.out;
		originalErr = System.err;

		hcDemuxOut = new DemuxPrintStream(originalOut, false);
		hcDemuxErr = new DemuxPrintStream(originalErr, true);

		System.setOut(hcDemuxOut);
		System.setErr(hcDemuxErr);
	}
}
