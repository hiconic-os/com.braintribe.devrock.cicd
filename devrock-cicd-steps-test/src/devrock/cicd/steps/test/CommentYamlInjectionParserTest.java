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
package devrock.cicd.steps.test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.braintribe.common.lcd.Pair;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.utils.IOTools;

import devrock.cicd.model.api.data.GitContext;
import devrock.git.CommentYamlInjectionParser;
import devrock.step.model.api.ExchangeProperties;

public class CommentYamlInjectionParserTest {
	@Test
	public void testYamlInjection() throws IOException {
		String text = IOTools.slurp(new File("res/comment.txt"), "UTF-8");
		
		List<Pair<GenericEntity, String>> entities = CommentYamlInjectionParser.extractYamlSections(text).get();

		Assertions.assertThat(entities.size()).isEqualTo(2);
		
		Pair<GenericEntity, String> e1 = entities.get(0);
		Pair<GenericEntity, String> e2 = entities.get(1);
		
		Assertions.assertThat(e1.first()).isInstanceOf(ExchangeProperties.class);
		Assertions.assertThat(e2.first()).isInstanceOf(GitContext.class);
		
		ExchangeProperties exProps = (ExchangeProperties)e1.first();
		String exPropsKey = e1.second();
		GitContext gitContext = (GitContext)e2.first();
		String gitContextKey = e2.second();
		
		Assertions.assertThat(exPropsKey).isNull();
		Assertions.assertThat(gitContextKey).isEqualTo("classifier");
		
		Assertions.assertThat(exProps.getProperties().get("foo")).isEqualTo(true);
		Assertions.assertThat(exProps.getProperties().get("bar")).isEqualTo("text");
		
		Assertions.assertThat(gitContext.getBaseBranch()).isEqualTo("main");
		Assertions.assertThat(gitContext.getBaseHash()).isEqualTo("123");
	}
	
	@Test
	public void testYamlInjectionSimple() throws IOException {
		String text = IOTools.slurp(new File("res/simple-comment.txt"), "UTF-8");
		
		List<Pair<GenericEntity, String>> entities = CommentYamlInjectionParser.extractYamlSections(text).get();
		
		Assertions.assertThat(entities.size()).isEqualTo(1);
		
		Pair<GenericEntity, String> e1 = entities.get(0);
		
		Assertions.assertThat(e1.first()).isInstanceOf(ExchangeProperties.class);
		
		ExchangeProperties exProps = (ExchangeProperties)e1.first();
		String exPropsKey = e1.second();
		
		Assertions.assertThat(exPropsKey).isNull();
		
		Assertions.assertThat(exProps.getProperties().get("detectUnpublishedArtifacts")).isEqualTo(true);
	}
}
