package devrock.cicd.steps.gradle.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.gradle.api.Task;

import com.braintribe.model.generic.reflection.EntityType;

import devrock.step.model.api.StepRequest;

public abstract class StepConfiguration {
    private boolean optional = false;
    private Consumer<Task> configurer;
    private String name;
    private List<String> requires = new ArrayList<>();

    protected StepConfiguration(String name) {
        this.name = name;
    }
    
    public void requires(@SuppressWarnings("unchecked") EntityType<? extends StepRequest>... steps) {
        for (EntityType<? extends StepRequest> step: steps) {
            String name = com.braintribe.utils.StringTools.camelCaseToDashSeparated(step.getShortName());
            requires.add(name);
        }
    }

    public void requires(String... steps) {
        for (String step: steps) {
            requires.add(step);
        }
    }

    public void optional(boolean optional) {
        this.optional = optional;
    }
    
    /* Configures the task */
    public void configure(Consumer<Task> configurer) {
        this.configurer = configurer;
    }

    public abstract Runnable getRunnable();

    public Consumer<Task> getConfigurer() {
        return configurer;
    }

    public boolean isOptional() {
        return optional;
    }

    public String getName() {
        return name;
    }
    
    public List<String> getRequires() {
        return requires;
    }
}

