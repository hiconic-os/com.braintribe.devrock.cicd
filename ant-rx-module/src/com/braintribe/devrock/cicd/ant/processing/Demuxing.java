package com.braintribe.devrock.cicd.ant.processing;

import java.io.PrintStream;

import org.apache.tools.ant.Project;

public class Demuxing {
	private static DrDemuxOutputStream btDemuxOut, btDemuxErr;
	private static PrintStream originalOut, originalErr;
	private static Object demuxMonitor = new Object();
	private static volatile boolean configuredDemuxing = false;
	
	public static void bindSubProjectToCurrentThread(Project project) {
		ensureDemuxing();

		btDemuxOut.bindProjectToCurrentThread(project);
		btDemuxErr.bindProjectToCurrentThread(project);
	}
	
	public static void unbindSubProjectFromCurrentThread() {
		btDemuxOut.unbindCurrentThread();
		btDemuxErr.unbindCurrentThread();
	}
	
	private static void ensureDemuxing() {
		if (!configuredDemuxing)
			synchronized (demuxMonitor) {
				if (!configuredDemuxing) {
					prepareDrDemuxOutputStream();
					configuredDemuxing = true;
				}
			}
	}

	
	/** @see DrDemuxOutputStream */
	private static void prepareDrDemuxOutputStream() {
		originalOut = System.out;
		originalErr = System.err;

		btDemuxOut = new DrDemuxOutputStream(originalOut, false);
		btDemuxErr = new DrDemuxOutputStream(originalErr, true);

		System.setOut(new PrintStream(btDemuxOut));
		System.setErr(new PrintStream(btDemuxErr));
	}
}
