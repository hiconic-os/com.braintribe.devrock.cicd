package devrock.cicd.locking.firebase.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

public class FirebaseAuthLab {
	
	public static void main(String[] args) {
		try {
			HttpRequest request = HttpRequest.newBuilder() //
				.uri(URI.create("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=AIzaSyDpI4psVoN7qc01cDUYdPJmczf_xCBm_NA")) //
				.POST(BodyPublishers.ofString("{\"email\":\"dirk.scheffler@modularmind.eu\",\"password\":\"jeRpBPilflCAMHlfmW6v\",\"returnSecureToken\":true}"))
				.header("Content-Type", "application/json")
				.build();
			
			HttpClient httpClient = HttpClient.newHttpClient();
			
			BodyHandler<String> handler = BodyHandlers.ofString();
			
			HttpResponse<String> response = httpClient.send(request, handler);
			
			String body = response.body();
			
			System.out.println(body);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main1(String[] args) {
		try {
			// Load the service account key JSON file
			FileInputStream serviceAccount = new FileInputStream("res/hiconic-os-firebase-adminsdk-l6rei-f925bffa4e.json");

			// Authenticate a Google credential with the service account
			GoogleCredential googleCred = GoogleCredential.fromStream(serviceAccount);

			// Add the required scopes to the Google credential
			GoogleCredential scoped = googleCred.createScoped(
			    Arrays.asList(
			      "https://www.googleapis.com/auth/firebase.database",
			      "https://www.googleapis.com/auth/userinfo.email"
			    )
			);

			// Use the Google credential to generate an access token
			scoped.refreshToken();
			String token = scoped.getAccessToken();
			
			System.out.println(token);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
