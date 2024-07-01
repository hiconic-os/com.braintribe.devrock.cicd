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

public class EnumLab {

	public static void main(String[] args) {
		System.out.println(MyEnum.ONE.lookupKey());
		System.out.println(MyEnum.TWO.lookupKey());
		System.out.println(MyEnum.THREE.lookupKey());
		System.out.println(MyEnum.ONE.name());
		System.out.println(MyEnum.TWO.name());
		System.out.println(MyEnum.THREE.name());
	}
	
	
	enum MyEnum {
		
		ONE("One_"), TWO("_TWo"), THREE("T H R E E");
		
		private String lookupKey;

		private MyEnum(String lookupKey) {
			this.lookupKey = lookupKey;
		}
		
		public String lookupKey() {
			return lookupKey;
		}
		
	}
	
}
