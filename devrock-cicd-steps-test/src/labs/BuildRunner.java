package labs;
import java.io.IOException;

public class BuildRunner {
	public static void main(String[] args) {
		try {
			ProcessBuilder pb = new ProcessBuilder("dr");
			pb.inheritIO();
			pb.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
