package de.keithpaterson.groundnet_checker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	private File projectBaseDir;

	@ParameterizedTest(name="Unconnected nodes : {0}")
	@ArgumentsSource(FileProvider.class)
	public void testUnconnected(String f, Graph<Element, DefaultEdge> loadGraph) {
		try {
			if(loadGraph.edgeSet().size()==0)
				return;
			List<String> unconnected = loadGraph.vertexSet().stream()
					.filter(p ->  Graphs.neighborListOf(loadGraph, p).size()== 0 ).map(m -> m.getAttribute("index")).collect(Collectors.toList());
			assertEquals(0,unconnected.size(),"There are unconnected nodes in " + f + " " + unconnected);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@ParameterizedTest(name="Runway routes {0}")
	@ArgumentsSource(FileProvider.class)
	public void testRunwayRoutes(String f, Graph<Element, DefaultEdge> loadGraph) {
		try {
			List<Element> runwayEnds = loadGraph.vertexSet().stream()
					.filter(p ->  Graphs.neighborListOf(loadGraph, p).size() == 1 )
					.filter(p -> p.getAttribute("isOnRunway").equals("1") )
					.collect(Collectors.toList());
			List<Element> parkingEnds = loadGraph.vertexSet().stream()
					.filter(p -> Graphs.neighborListOf(loadGraph, p).size() == 1 )
					.filter(p -> p.hasAttribute("type") )
					.collect(Collectors.toList());
			for (Element parkingNode : parkingEnds) {
				for (Element runwayNode : runwayEnds) {
					GraphPath<Element, DefaultEdge> pathBetween = DijkstraShortestPath.findPathBetween(loadGraph, parkingNode, runwayNode);
					assertNotNull( pathBetween, "No route between Node "+ parkingNode.getAttribute("name") + " (" + parkingNode.getAttribute("index") + ") Node " + parkingNode.getAttribute("name") + " (" + runwayNode.getAttribute("index") + ")");
//					System.out.println("Length " + pathBetween.getLength());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class FileProvider implements ArgumentsProvider {

		private static List<Arguments> files = fileProvider();

		private static List<Arguments> fileProvider() {
//			File projectBaseDir = new File("C:\\Users\\keith.paterson\\Documents\\FlightGear\\main");
			File projectBaseDir = new File(".");
			assertNotNull(projectBaseDir);
			assertTrue(projectBaseDir.exists());
			try {
				return Files.walk(projectBaseDir.toPath()).filter(p -> Files.isRegularFile(p))
						.filter(p -> !p.getFileName().toString().equals("pom.xml"))
						.filter(p -> p.getFileName().toString().matches("[a-zA-Z0-9]*\\.(groundnet)\\.xml"))
						.map(p -> new Object[]{p.getFileName().toString(),new GroundnetLoader().loadGraphSafe(p.toFile())})
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
