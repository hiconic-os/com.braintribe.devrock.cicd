package com.braintribe.devrock.cicd.ant.wire.space;

import com.braintribe.devrock.cicd.ant.processing.AntProcessor;
import com.braintribe.utils.stream.api.StreamPipes;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import devrock.ant.model.api.RunAnt;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class AntRxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;
	
	@Override
	public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
		configuration.register(RunAnt.T, antProcessor());
	}
	
	@Managed
	private AntProcessor antProcessor() {
		AntProcessor bean = new AntProcessor();
		bean.setStreamPipeFactory(StreamPipes.simpleFactory());
		return bean;
	}

}