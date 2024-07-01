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

