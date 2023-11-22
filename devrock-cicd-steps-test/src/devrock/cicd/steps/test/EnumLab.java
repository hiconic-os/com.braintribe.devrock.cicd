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
