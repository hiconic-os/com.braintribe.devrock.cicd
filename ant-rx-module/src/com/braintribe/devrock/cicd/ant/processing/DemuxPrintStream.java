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
// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.devrock.cicd.ant.processing;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tools.ant.DemuxOutputStream;
import org.apache.tools.ant.Main;
import org.apache.tools.ant.Project;

import com.braintribe.utils.stream.NullOutputStream;

/**
 * Improvement over the default {@link DemuxOutputStream} used by ANT's {@link Main#runBuild main} method, this one allows registration of an Ant
 * {@link Project} for a given thread.
 * 
 * This is also relevant in a single-threaded build, as even there the default DemuxOutputStream is not aware of the sub-task.
 * 
 * Output WITHOUT this (warning are logged for [bt:transitive-build]):
 * 
 * <pre>
 *  compile:
 *    ...
 *    [bt:transitive-build] Note: Some input files use or override a deprecated API.
 *    [bt:transitive-build] Note: Recompile with -Xlint:deprecation for details.
 * </pre>
 * 
 * Output WITH this (warnings are logged for [javac]):
 * 
 * <pre>
 *  compile:
 *    ...
 *    [javac] Note: Some input files use or override a deprecated API.
 *    [javac] Note: Recompile with -Xlint:deprecation for details.
 * </pre>
 * 
 * @author peter.gazdik
 * @author Dirk Scheffler
 */
public class DemuxPrintStream extends PrintStream {

	private final Map<ThreadGroup, PrintStream> streams = new ConcurrentHashMap<>();

	private final PrintStream mainOs;

	private final boolean isErrorStream;
	

	public DemuxPrintStream(PrintStream mainOs, boolean isErrorStream) {
		super(NullOutputStream.getInstance());
		this.isErrorStream = isErrorStream;
		this.mainOs = mainOs;
	}
	
	@Override
	public Charset charset() {
		return resolveDelegate().charset();
	}

	public void bindProjectToCurrentThread(Project subProject) {
		streams.put(resolveThreadGroup(), newDemuxOs(subProject));
	}

	private PrintStream newDemuxOs(Project subProject) {
		return new PrintStream(new DemuxOutputStream(subProject, isErrorStream));
	}

	public void unbindCurrentThread() {
		this.streams.remove(resolveThreadGroup());
	}

	// PrintStream Methods
	@Override
	public void write(int b) {
		resolveDelegate().write(b);
	}

	@Override
	public boolean checkError() {
		return resolveDelegate().checkError();
	}

	@Override
	public void writeBytes(byte[] buf) {
		resolveDelegate().writeBytes(buf);
	}

	@Override
	public void print(boolean b) {
		resolveDelegate().print(b);
	}

	@Override
	public void print(char c) {
		resolveDelegate().print(c);
	}

	@Override
	public void print(int i) {
		resolveDelegate().print(i);
	}

	@Override
	public void print(long l) {
		resolveDelegate().print(l);
	}

	@Override
	public void print(float f) {
		resolveDelegate().print(f);
	}

	@Override
	public void print(double d) {
		resolveDelegate().print(d);
	}

	@Override
	public void print(char[] s) {
		resolveDelegate().print(s);
	}

	@Override
	public void print(String s) {
		resolveDelegate().print(s);
	}

	@Override
	public void print(Object obj) {
		resolveDelegate().print(obj);
	}

	@Override
	public PrintStream append(CharSequence csq) {
		return resolveDelegate().append(csq);
	}

	@Override
	public PrintStream append(CharSequence csq, int start, int end) {
		return resolveDelegate().append(csq, start, end);
	}

	@Override
	public PrintStream append(char c) {
		return resolveDelegate().append(c);
	}

	@Override
	public void println() {
		resolveDelegate().println();
	}

	@Override
	public void println(boolean x) {
		resolveDelegate().println(x);
	}

	@Override
	public void println(char x) {
		resolveDelegate().println(x);
	}

	@Override
	public void println(int x) {
		resolveDelegate().println(x);
	}

	@Override
	public void println(long x) {
		resolveDelegate().println(x);
	}

	@Override
	public void println(float x) {
		resolveDelegate().println(x);
	}

	@Override
	public void println(double x) {
		resolveDelegate().println(x);
	}

	@Override
	public void println(char[] x) {
		resolveDelegate().println(x);
	}

	@Override
	public void println(String x) {
		resolveDelegate().println(x);
	}

	@Override
	public void println(Object x) {
		resolveDelegate().println();
	}

	@Override
	public PrintStream printf(String format, Object... args) {
		resolveDelegate().printf(format, args);
		return this;
	}

	@Override
	public PrintStream printf(Locale l, String format, Object... args) {
		resolveDelegate().printf(l, format, args);
		return this;
	}

	@Override
	public PrintStream format(String format, Object... args) {
		resolveDelegate().format(format, args);
		return this;
	}

	@Override
	public PrintStream format(Locale l, String format, Object... args) {
		resolveDelegate().format(l, format, args);
		return this;
	}

	@Override
	public void write(byte[] b) throws IOException {
		resolveDelegate().write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) {
		resolveDelegate().write(b, off, len);
	}

	@Override
	public void flush() {
		resolveDelegate().flush();
	}

	@Override
	public void close() {
		resolveDelegate().close();
	}

	private PrintStream resolveDelegate() {
		PrintStream result = streams.get(resolveThreadGroup());
		if (result == null)
			result = mainOs;
		return result;
	}

	private ThreadGroup resolveThreadGroup() {
		return Thread.currentThread().getThreadGroup();
	}

}
