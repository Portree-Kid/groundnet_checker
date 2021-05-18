package de.keithpaterson.groundnet_checker;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


public class TrafficTest {
	
	static HashMap<String, Traffic> trafficList = new HashMap<>();

	@Disabled
	@ParameterizedTest(name="{0}#LoadFile Test")
	@ArgumentsSource(FileProvider.class)
	public void testLoad(String f, Path p) {
		try {
			loadTraffic(p.toFile());
		} catch (Exception e) {
			fail( f + ":Error loading "  + e.getMessage());
		}
	}

	private void loadTraffic(File f) {
		if (f.canRead()) {
			try (Scanner scanner = new Scanner(f, StandardCharsets.UTF_8.toString())) {
				scanner.useDelimiter("\\A");
				String content = scanner.hasNext() ? scanner.next() : "";
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setNamespaceAware(true);
				DocumentBuilder builder;
				Document doc = null;
				builder = factory.newDocumentBuilder();
				doc = builder.parse(new InputSource(new StringReader(content)));

				// Create XPathFactory object
				XPathFactory xpathFactory = XPathFactory.newInstance();

				// Create XPath object
				XPath xpath = xpathFactory.newXPath();
				NodeList flightNodes = (NodeList) xpath.evaluate("/trafficlist/flight/departure/port", doc,
						XPathConstants.NODESET);
				for (int i = 0; i < flightNodes.getLength(); i++) {
					Element n = (Element) flightNodes.item(i);
					Traffic traffic = this.trafficList.getOrDefault(n.getTextContent(), new Traffic(n.getTextContent()));
					traffic.incrementFlights();
					System.out.println(traffic);			
					this.trafficList.put(n.getTextContent(), traffic);
				}
				flightNodes = (NodeList) xpath.evaluate("/trafficlist/flight/arrival/port", doc,
						XPathConstants.NODESET);
				for (int i = 0; i < flightNodes.getLength(); i++) {
					Element n = (Element) flightNodes.item(i);
					Traffic traffic = this.trafficList.getOrDefault(n.getTextContent(), new Traffic(n.getTextContent()));
					traffic.incrementFlights();
					System.out.println(traffic);			
					this.trafficList.put(n.getTextContent(), traffic);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
//				e.printStackTrace();
			}
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
			File projectBaseDir = new File("C:\\GIT\\fgdata\\AI\\Traffic");
			assertNotNull(projectBaseDir);
			assertTrue(projectBaseDir.exists());
			try {
				return Files.walk(projectBaseDir.toPath()).filter(p -> Files.isRegularFile(p))
						.filter(p -> !p.getFileName().toString().equals("pom.xml"))
						.filter(p -> p.getFileName().toString().matches("[a-zA-Z0-9]*\\.xml"))
						//.filter(p -> !ignore.containsKey(p.getFileName().toString()))
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

	@AfterAll
	public static void dumpDataBase() {
		if(trafficList.size() > 0) {
			try( ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream("traffic.obj")) ){
				os.writeObject(trafficList);
				os.flush();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
