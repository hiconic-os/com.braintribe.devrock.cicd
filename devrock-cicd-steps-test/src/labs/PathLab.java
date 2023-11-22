package labs;
import java.nio.file.Path;

public class PathLab {
	public static void main(String[] args) {
		Path p = Path.of("../..");
		System.out.println(p.toAbsolutePath().normalize());
	}
}
