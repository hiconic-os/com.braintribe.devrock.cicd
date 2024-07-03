// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package devrock.process.execution;


public class ProcessResults {
	private String errorText;
	private String normalText;
	private int retVal;
	
	public ProcessResults(int retVal, String normalText, String errorText) {
		super();
		this.errorText = errorText;
		this.normalText = normalText;
		this.retVal = retVal;
	}
	
	public String getErrorText() {
		return errorText;
	}
	
	public String getNormalText() {
		return normalText;
	}
	
	public int getRetVal() {
		return retVal;
	}
	
	
}
