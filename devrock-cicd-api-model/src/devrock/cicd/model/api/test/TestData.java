package devrock.cicd.model.api.test;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface TestData extends GenericEntity {
	EntityType<TestData> T = EntityTypes.T(TestData.class);
	
	String value1 = "value1";
	String value2 = "value2";
	
	String getValue1();
	void setValue1(String value1);
	
	String getValue2();
	void setValue2(String value2);
}
