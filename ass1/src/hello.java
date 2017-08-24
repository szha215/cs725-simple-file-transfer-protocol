import java.io.File;
import java.nio.file.*;

class HelloWorld {
	
	private static final File ROOT_DIRECTORY = FileSystems.getDefault().getPath("ServerFolder").toFile();
	
	public static void main(String[] args) {
		System.out.println("Hello world");
		
		String filename = "a.a";
		
		if (filename.matches("^[A-Z0-9.]+$")) {
			System.out.println("GOOD");
		}
				
//		String ROOT_DIRECTORY = FileSystems.getDefault().getPath("").toAbsolutePath().toString();
		
//		System.out.println(ROOT_DIRECTORY);
		
		Path path = FileSystems.getDefault().getPath("").toAbsolutePath();
		
		
		System.out.println(path);
		
		File file = path.toFile();
		
		System.out.println(file.isDirectory());
		
		System.out.println(path.getName(3));
		
		File f2 = new File(file.toString().concat("/ServerFolder"));
		System.out.println(f2.toString());
		
		System.out.println(HelloWorld.ROOT_DIRECTORY.isDirectory());
		
	}
}