package de.keithpaterson.groundnet_checker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.referencing.GeodeticCalculator;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.w3c.dom.Element;

public class GroundNetTest {

	private static Pattern COORDINATE_PATTERN = Pattern.compile("([NSEW])([0-9]*)\\s*([.0-9]*)");

	@ParameterizedTest(name = "{0} Unconnected nodes")
	@ArgumentsSource(FileProvider.class)
	public void testUnconnected(String f, Graph<Element, DefaultEdge> loadGraph) {
		if (loadGraph == null)
			return;
		try {
			if (loadGraph.edgeSet().size() == 0)
				return;
			List<String> unconnected = loadGraph.vertexSet().stream()
					.filter(p -> Graphs.neighborListOf(loadGraph, p).size() == 0).map(m -> m.getAttribute("index"))
					.collect(Collectors.toList());
			assertEquals(0, unconnected.size(), f + ":There are unconnected nodes " + " " + unconnected);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@ParameterizedTest(name = "{0} Runway routes")
	@ArgumentsSource(FileProvider.class)
	public void testRunwayRoutes(String f, Graph<Element, DefaultEdge> loadGraph) {
		if (loadGraph == null)
			return;
		try {
			List<Element> runwayEnds = loadGraph.vertexSet().stream()
					.filter(p -> p.getAttribute("isOnRunway").equals("1"))
					.filter(p -> Graphs.neighborListOf(loadGraph, p).size() >= 1)
					.collect(Collectors.toList());
			List<Element> parkingEnds = loadGraph.vertexSet().stream()
					.filter(p -> p.getNodeName().equals("Parking"))
					.filter(p -> Graphs.neighborListOf(loadGraph, p).size() >= 1)
					.collect(Collectors.toList());
			for (Element parkingNode : parkingEnds) {
				for (Element runwayNode : runwayEnds) {
					GraphPath<Element, DefaultEdge> pathBetween = DijkstraShortestPath.findPathBetween(loadGraph,
							parkingNode, runwayNode);
					assertNotNull(pathBetween,
							f + ":There are missing routes in " + " From " + parkingNode.getAttribute("name") + " To ("
									+ parkingNode.getAttribute("index") + ") Node " + parkingNode.getAttribute("name")
									+ " (" + runwayNode.getAttribute("index") + ")");
//					System.out.println("Length " + pathBetween.getLength());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@ParameterizedTest(name = "{0} Pushback routes")
	@ArgumentsSource(FileProvider.class)
	public void testPushbackRoutes(String f, Graph<Element, DefaultEdge> loadGraph) {
		if (loadGraph == null)
			return;
		try {
			List<Element> parkingEnds = loadGraph.vertexSet().stream()
					.filter(p -> p.hasAttribute("type"))
					.filter(p -> Graphs.neighborListOf(loadGraph, p).size() >= 1)
					.collect(Collectors.toList());
			for (Element parkingNode : parkingEnds) {
				List<Element> nextNodes = Graphs.neighborListOf(loadGraph, parkingNode);
				for (Element nextNode : new HashSet<Element>(nextNodes)) {
//					System.out.println(nextNode.getAttribute("index"));					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@ParameterizedTest(name = "{0} Parking Radius")
	@ArgumentsSource(FileProvider.class)
	public void testParkingRadius(String f, Graph<Element, DefaultEdge> loadGraph) {
		if (loadGraph == null)
			return;
		try {
			List<String> missizedParkings = loadGraph.vertexSet().stream()
					.filter(p -> Graphs.neighborListOf(loadGraph, p).size() == 1).filter(p -> p.hasAttribute("type"))
					.filter(p -> Double.parseDouble(p.getAttribute("radius")) > 40
							|| Double.parseDouble(p.getAttribute("radius")) < 5)
					.map(m -> "Id " + m.getAttribute("index") + " Size " + m.getAttribute("radius"))
					.collect(Collectors.toList());
			assertEquals(0, missizedParkings.size(),
					f + ":There are parkings with large radius " + missizedParkings);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@ParameterizedTest(name = "{0} Nowhere to Park ")
	@ArgumentsSource(FileProvider.class)
	public void testNowhereToPark(String f, Graph<Element, DefaultEdge> loadGraph) {
		if (loadGraph == null)
			return;
		try {
			List<Element> possibleParkings = loadGraph.vertexSet().stream().filter(p -> p.hasAttribute("type")).filter(
					p -> p.getAttribute("type").equals("gate") && p.getAttribute("airlineCodes").trim().equals(""))
					.collect(Collectors.toList());

			assertTrue(possibleParkings.size() > 0, f+":Nowhere to park ");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@ParameterizedTest(name = "{0} Intersecting parking ")
	@ArgumentsSource(FileProvider.class)
	public void testIntersectingParking(String f, Graph<Element, DefaultEdge> loadGraph) {
		if (loadGraph == null)
			return;
		try {
			List<Element> possibleParkings = loadGraph.vertexSet().stream().filter(p -> p.hasAttribute("type"))
					.collect(Collectors.toList());
			for (Element p1 : possibleParkings) {
				for (Element p2 : possibleParkings) {
					if (p1 == p2)
						continue;
//					if (p1.getAttribute("index").equals(p2.getAttribute("index")))
//						continue;
					GeodeticCalculator calcy = new GeodeticCalculator();
					double lat1 = parse(p1.getAttribute("lat"));
					double lon1 = parse(p1.getAttribute("lon"));
					double lat2 = parse(p2.getAttribute("lat"));
					double lon2 = parse(p2.getAttribute("lon"));
					calcy.setStartingGeographicPoint(lon1, lat1);
					calcy.setDestinationGeographicPoint(lon2, lat2);
					double distance = calcy.getOrthodromicDistance();
					if (p1.getAttribute("radius").isEmpty())
						continue;
					if (p2.getAttribute("radius").isEmpty())
						continue;
					double r1 = Double.parseDouble(p1.getAttribute("radius"));
					double r2 = Double.parseDouble(p2.getAttribute("radius"));
					assertTrue(distance > r1 + r2,
							f+":Radius of " + p1.getAttribute("index") + " & " + p2.getAttribute("index") + " intersect");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private double parse(String attribute) {
		Matcher matcher = COORDINATE_PATTERN.matcher(attribute);

		if (matcher.find()) {
			String sign = matcher.group(1);
			double degrees = Double.parseDouble(matcher.group(2));
			double minutes = Double.parseDouble(matcher.group(3));
//			System.out.println(attribute);
			return sign.equalsIgnoreCase("N") || sign.equalsIgnoreCase("E") ? (degrees + (minutes / 60))
					: -(degrees + (minutes / 60));
		}
		return 0;
	}

	public static class FileProvider implements ArgumentsProvider {

		private static List<Arguments> files = fileProvider();

		private static List<Arguments> fileProvider() {
			Properties ignore = new Properties();
			try {
				ignore.load(new FileReader("ignore.list"));
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
						.map(p -> new Object[] { p.getFileName().toString(),
								new GroundnetLoader().loadGraphSafe(p.toFile()) })
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
