package de.keithpaterson.groundnet_checker;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

public class GroundNetLoadingTest {

	@ParameterizedTest(name="{0}#LoadFile Test")
	@ArgumentsSource(FileProvider.class)
	public void testLoad(String f, Path p) {
		try {
			new GroundnetLoader().loadGraph(p.toFile());
		} catch (Exception e) {
			fail( f + ":Error loading "  + e.getMessage());
		}
	}

	public static class FileProvider implements ArgumentsProvider {

		private static List<Arguments> files = fileProvider();

		private static List<Arguments> fileProvider() {
			Properties ignore = new Properties();
			try {
				File f = new File("ignore.list");
				if (f.exists()) {
					ignore.load(new FileReader(f));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			File projectBaseDir = new File(".");
			assertNotNull(projectBaseDir);
			assertTrue(projectBaseDir.exists());
			try {
				return Files.walk(projectBaseDir.toPath()).filter(p -> Files.isRegularFile(p))
						.filter(p -> !p.getFileName().toString().equals("pom.xml"))
						.filter(p -> p.getFileName().toString().matches("[a-zA-Z0-9]*\\.(groundnet)\\.xml"))
						.filter(p -> !ignore.containsKey(p.getFileName().toString()))
						.map(p -> new Object[]{p.getFileName().toString(),p})
						.map(Arguments::of).collect(Collectors.toList());
				
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return null;
		}

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
			return files.stream();
		}
	}

}
