package devrock.cicd.steps.gradle.extension;

public class RunnableStepConfiguration extends StepConfiguration {

	private final Runnable runnable;

    public RunnableStepConfiguration(String name, Runnable runnable) {
        super(name);
        this.runnable = runnable;
    }

    @Override
	public Runnable getRunnable() {
        return runnable;
    }
}

