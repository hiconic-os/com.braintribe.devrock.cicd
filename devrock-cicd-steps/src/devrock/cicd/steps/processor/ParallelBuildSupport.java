package devrock.cicd.steps.processor;

import static com.braintribe.console.ConsoleOutputs.brightWhite;
import static com.braintribe.console.ConsoleOutputs.cyan;
import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.yellow;
import static com.braintribe.utils.lcd.CollectionTools2.index;
import static com.braintribe.utils.lcd.CollectionTools2.newConcurrentSet;
import static com.braintribe.utils.lcd.CollectionTools2.newList;
import static com.braintribe.utils.lcd.CollectionTools2.newMap;
import static com.braintribe.utils.lcd.CollectionTools2.newSet;
import static com.braintribe.utils.lcd.CollectionTools2.newTreeSet;
import static java.util.Collections.emptySet;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.braintribe.console.ConsoleOutputs;
import com.braintribe.execution.CountingThreadFactory;
import com.braintribe.execution.ExtendedThreadPoolExecutor;
import com.braintribe.execution.graph.api.ParallelGraphExecution;
import com.braintribe.execution.graph.api.ParallelGraphExecution.PgeResult;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisDependency;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.encryption.Md5Tools;

import devrock.cicd.model.api.data.CodebaseAnalysis;
import devrock.cicd.model.api.data.CodebaseDependencyAnalysis;
import devrock.cicd.model.api.data.LocalArtifact;
import devrock.cicd.model.api.reason.ArtifactsBuildFailed;

/**
 * @author peter.gazdik
 */
/* package */ class ParallelBuildSupport {

	/**
	 * Runs given handler for each artifact in given builds, possibly in parallel, but respecting the depender-first order provided via
	 * dependencyAnalysis.
	 * 
	 * @param builds
	 *            artifacts to handle, which can be a subset of all artifacts from the dependencyAnalysis
	 * @param threads
	 *            (optional) number of threads to run with
	 * @param skip
	 *            If <tt>true</tt>, artifacts that were built in the previous run will be skipped.<br>
	 *            If <tt>false</tt>, but build fails, info about successfully built artifacts will be stored in a file inside the temp folder.<br>
	 *            If <tt>null</tt> nothing will be skipped, nor stored in a file.
	 */
	public static Reason runInParallel( //
			CodebaseAnalysis analysis, //
			CodebaseDependencyAnalysis dependencyAnalysis, //
			List<LocalArtifact> builds, //
			Consumer<LocalArtifact> handler, //
			Integer threads, //
			Boolean skip) {

		return new ParallelBuildSupport(analysis, dependencyAnalysis, builds, handler, threads, skip).run();
	}

	private static final int DEFAULT_NUMBER_OF_THREADS = 6;
	private static final int DEFAULT_THREAD_PRIORITY = 3;

	private final List<LocalArtifact> builds;
	private final Consumer<LocalArtifact> handler;

	private final Map<LocalArtifact, List<LocalArtifact>> artifactToDependers;
	private final File alreadyBuiltTempFile;
	private final Set<String> alreadyBuiltNames;

	private final AtomicInteger runningBuildsCounter = new AtomicInteger(0);
	private final AtomicInteger buildCounter = new AtomicInteger(0);

	private final int threads;
	private final Boolean skip;


	private ParallelBuildSupport( //
			CodebaseAnalysis analysis, //
			CodebaseDependencyAnalysis dependencyAnalysis, //
			List<LocalArtifact> builds, //
			Consumer<LocalArtifact> handler, //
			Integer threads, //
			Boolean skip) {

		this.builds = builds;
		this.handler = handler;
		this.threads = threads == null ? DEFAULT_NUMBER_OF_THREADS : threads;
		this.skip = skip;

		this.artifactToDependers = indexDependers(builds, dependencyAnalysis);
		this.alreadyBuiltTempFile = getAlreadyBuiltSolutionNamesTmpFile(analysis);
		this.alreadyBuiltNames = newConcurrentSet(resolveSkippedSolutionNames());
	}


	private Map<LocalArtifact, List<LocalArtifact>> indexDependers(List<LocalArtifact> builds, CodebaseDependencyAnalysis dependencyAnalysis) {
		Map<String, LocalArtifact> aIdToArtifact = index(builds) //
				.by(this::getLocalArtifactId) //
				.unique();

		Map<String, AnalysisArtifact> aIdToAnalysisArtifact = dependencyAnalysis.getArtifactIndex();

		return builds.stream() //
				.collect(Collectors.toMap( //
						a -> a, //
						a -> {
							String aId = getLocalArtifactId(a);
							AnalysisArtifact aa = aIdToAnalysisArtifact.get(aId);

							List<LocalArtifact> result = newList();

							for (AnalysisArtifact dep : getTransitiveDependers(aa)) {
								String depId = dep.getArtifactId();
								LocalArtifact depLocalArtifact = aIdToArtifact.get(depId);
								// null means it wasn't part of [builds], thus we can ignore it
								if (depLocalArtifact != null)
									result.add(depLocalArtifact);
							}

							return result;
						}));
	}

	private final Map<AnalysisArtifact, Set<AnalysisArtifact>> transitiveDependers = newMap();

	private Set<AnalysisArtifact> getTransitiveDependers(AnalysisArtifact aa) {
		Set<AnalysisArtifact> result = transitiveDependers.get(aa);
		if (result != null)
			return result;

		result = newSet();
		transitiveDependers.put(aa, result);

		for (AnalysisDependency dependerDependency : aa.getDependers()) {
			AnalysisArtifact dep = dependerDependency.getDepender();
			/* null means it was a terminal dependency (e.g. for -Prange=[xyz] xyz would have its regular dependers plus an extra representing the
			 * [xyz] expression, which would be without an AnalysisArtifact). */
			if (dep != null) {
				result.add(dep);
				result.addAll(getTransitiveDependers(dep));
			}
		}
		
		return result;
	}
	
	private File getAlreadyBuiltSolutionNamesTmpFile(CodebaseAnalysis analysis) {
		return FileTools.newTempFile("Devrock/gradle/build-" + analysis.getGroupId() + "-" + Md5Tools.getMd5(analysis.getBasePath()));
	}

	private Set<String> resolveSkippedSolutionNames() {
		if (!Boolean.TRUE.equals(skip))
			return emptySet();

		if (!alreadyBuiltTempFile.exists()) {
			println(yellow("\n[WARNING] Cannot use [skip] argument as the file with already built artifacts doesn't exist: "
					+ alreadyBuiltTempFile.getPath() + "\n" //
			));
			return emptySet();
		}

		return FileTools.read(alreadyBuiltTempFile) //
				.asLineStream() //
				.collect(Collectors.toSet());
	}

	// #############################################
	// ## . . . . . . . . . RUN . . . . . . . . . ##
	// #############################################

	private Reason run() {
		println(brightWhite("\nExecuting tasks with " + threads + " threads.\n"));

		ThreadPoolExecutor executor = newThreadPoolExecutor();

		try {
			PgeResult<LocalArtifact, Boolean> result = ParallelGraphExecution.foreach("Parallel Step Execution", builds) //
					.itemsToProcessAfter(this::resolveDependers) //
					.withThreadPoolExecutor(executor) //
					.run(s -> buildOrSkipSingleArtifact(s));

			if (result.hasError()) {
				storeSkippedSolutionsIfRelevant();
				return Reasons.build(ArtifactsBuildFailed.T).text("Parallel execution failed!").toReason();
			}

			alreadyBuiltTempFile.delete();
			return null;

		} finally {
			executor.shutdown();
		}
	}

	private List<LocalArtifact> resolveDependers(LocalArtifact artifact) {
		return artifactToDependers.get(artifact);
	}

	private ThreadPoolExecutor newThreadPoolExecutor() {
		ExtendedThreadPoolExecutor threadPool = new ExtendedThreadPoolExecutor( //
				threads, threads, // core/max pool size
				0L, TimeUnit.MILLISECONDS, // keep alive time
				new LinkedBlockingQueue<>(), //
				groupPerThread_ThreadFactory());

		threadPool.setDescription("ParallelTaskExecutor");
		threadPool.postConstruct();

		return threadPool;
	}

	/**
	 * For every one of our threads that are processing the tasks in parallel we want to set it's own group - see
	 * {@link #newThreadWithItsOwnGroup(ThreadGroup, Runnable, String)}. There are some tasks that might create other threads, and if those other
	 * threads log something, we want to be able to recognize which tasks' thread it belongs to.
	 */
	private CountingThreadFactory groupPerThread_ThreadFactory() {
		CountingThreadFactory result = new CountingThreadFactory("Parallel-Runner");
		result.setExtendedThreadFactory(this::newThreadWithItsOwnGroup);

		return result;
	}

	private Thread newThreadWithItsOwnGroup(ThreadGroup parentGroup, Runnable r, String name) {
		ThreadGroup group = new ThreadGroup(parentGroup, "Group-" + name);

		Thread t = new Thread(group, r, name, 0);
		t.setPriority(DEFAULT_THREAD_PRIORITY);
		return t;
	}

	private void buildOrSkipSingleArtifact(LocalArtifact artifact) {
		String artifactId = getLocalArtifactId(artifact);

		if (shouldIgnore(artifact))
			logBuildingInfoAboutArtifact(artifact, "IGNORING", 0);
		else if (alreadyBuiltNames.contains(artifactId))
			logBuildingInfoAboutArtifact(artifact, "SKIPPING", 0);
		else
			buildSingleArtifact(artifact);

		alreadyBuiltNames.add(artifactId);
	}

	private boolean shouldIgnore(@SuppressWarnings("unused") LocalArtifact artifact) {
		// Not supported for now
		return false;
	}

	private void buildSingleArtifact(LocalArtifact artifact) {
		try {
			int runningBuilds = runningBuildsCounter.incrementAndGet();

			logBuildingInfoAboutArtifact(artifact, "Starting", runningBuilds);

			handler.accept(artifact);

		} finally {
			runningBuildsCounter.decrementAndGet();
		}
	}

	private void storeSkippedSolutionsIfRelevant() {
		if (skip == null)
			return;

		TreeSet<String> skippedSorted = newTreeSet(alreadyBuiltNames);

		FileTools.write(alreadyBuiltTempFile).lines(skippedSorted);
	}

	private void logBuildingInfoAboutArtifact(LocalArtifact artifact, String startSkipOrIgnore, int runningBuilds) {
		int buildNumber = buildCounter.incrementAndGet();
		
		VersionedArtifactIdentification ai = artifact.getArtifactIdentification();
		String version = ai.getVersion();

		println(ConsoleOutputs.sequence( //
				brightWhite(startSkipOrIgnore + " task (" + buildNumber + "/" + builds.size() + ") " + runningBuildsInfoIfParallel(runningBuilds)), //
				cyan(ai.getArtifactId()), //
				brightWhite("#" + version + " (" + ai.getGroupId() + ")") //
		));
	}

	private String runningBuildsInfoIfParallel(int runningBuilds) {
		return runningBuilds > 0 ? "(R:" + runningBuilds + ") " : "";
	}

	private String getLocalArtifactId(LocalArtifact a) {
		return a.getArtifactIdentification().getArtifactId();
	}

}
