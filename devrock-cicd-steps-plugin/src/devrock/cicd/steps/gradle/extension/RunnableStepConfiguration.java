package devrock.cicd.steps.gradle.extension;

public class RunnableStepConfiguration extends StepConfiguration {
    private Runnable runnable;

    public RunnableStepConfiguration(String name, Runnable runnable) {
        super(name);
        this.runnable = runnable;
    }

    public Runnable getRunnable() {
        return runnable;
    }
}

