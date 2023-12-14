package devrock.cicd.steps.lab;

import java.net.URI;
import java.net.URISyntaxException;

public class UriLab {
	public static void main(String[] args) {
		//orgs/hiconic-os/packages?package_type=maven
		
		try {
			URI uri = new URI("https", "user", "api.github.com", -1, "/orgs/hiconic-os", "package_type=maven", null);
			System.out.println(uri);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
