package de.keithpaterson.groundnet_checker;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
		if("NOOP".equals(f)) {
			return;
		}
		try {
			new GroundnetLoader().loadGraph(p.toFile());
		} catch (Exception e) {
			fail( f + ":Error loading "  + e.getMessage());
		}
	}

	public static class FileProvider implements ArgumentsProvider {

		private static List<Arguments> files = null;

        private static String branch;

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
                if (branch != null && branch.matches("GROUNDNET_[a-zA-Z0-9]*_[0-9]*")) {
                    String icao = branch.substring(branch.indexOf("_")+1, branch.indexOf("_", branch.indexOf("_")+1));
                    System.out.println("Matched Branch : " + icao);

                    return Files.walk(projectBaseDir.toPath()).filter(p -> Files.isRegularFile(p))
                            .filter(p -> !p.getFileName().toString().equals("pom.xml"))
//							.filter(p -> {System.out.println(p); return true;})
							.filter(p -> p.getFileName().toString().matches(icao + "\\.(groundnet)\\.xml"))
                            .map(p -> new Object[]{p.getFileName().toString(), p})
                            .map(Arguments::of).collect(Collectors.toList());
                }
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			ArrayList<Arguments> ret = new ArrayList<>();
			ret.add(Arguments.of("NOOP", null));
			return ret;
		}

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            if(branch==null||!branch.equals(System.getProperty("TRAVIS_BRANCH")))
            {
                branch = System.getProperty("TRAVIS_BRANCH");
				System.out.println("Creating for Branch : " + branch);
                files = fileProvider();
            }
			return files.stream();
		}
	}

}
