import java.nio.file.*;

class HelloWorld {
	
	public static void main(String[] args) {
		System.out.println("Hello world");
		
		String filename = "a.a";
		
		if (filename.matches("^[A-Z0-9.]+$")) {
			System.out.println("GOOD");
		}
		
		Path path = FileSystems.getDefault().getPath("").toAbsolutePath();
		
		String ROOT_DIRECTORY = FileSystems.getDefault().getPath("").toAbsolutePath().toString();
		
		System.out.println(ROOT_DIRECTORY);
	}
}