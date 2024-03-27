package devrock.steps.sequencer.wire.contract;

import hiconic.rx.module.api.wire.EnvironmentPropertiesContract;

public interface SequencerEnvironmentContract extends EnvironmentPropertiesContract {
	boolean DEVROCK_PIPELINE_EXTERNAL_SEQUENCING();
}
