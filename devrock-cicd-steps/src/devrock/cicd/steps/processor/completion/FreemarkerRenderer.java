// ============================================================================
package devrock.cicd.steps.processor.completion;

import java.io.IOException;
import java.io.Writer;

import com.braintribe.exception.Exceptions;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class FreemarkerRenderer {

	private final Configuration freemarkerConfig = new Configuration(Configuration.VERSION_2_3_28);

	public FreemarkerRenderer(TemplateLoader templateLoader) {
		// Got these settings from the tutorial:
		// https://freemarker.apache.org/docs/pgui_quickstart_createconfiguration.html
		freemarkerConfig.setTemplateLoader(templateLoader);
		freemarkerConfig.setDefaultEncoding("UTF-8");
		freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		freemarkerConfig.setLogTemplateExceptions(false);
		freemarkerConfig.setWrapUncheckedExceptions(true);
	}

	public static FreemarkerRenderer loadingViaClassLoader(Class<?> clazz) {
		return loadingViaClassLoader(clazz, "");
	}

	public static FreemarkerRenderer loadingViaClassLoader(Class<?> clazz, String basePackagePath) {
		return new FreemarkerRenderer(new ClassTemplateLoader(clazz, basePackagePath));
	}

	public void renderTemplate(String templateName, Object dataModel, Writer writer) {
		try{
			Template temp = freemarkerConfig.getTemplate(templateName);

			temp.process(dataModel, writer);

		} catch (TemplateException | IOException e) {
			throw Exceptions.unchecked(e, "Could not write file using freemarker template");
		}
	}

}
