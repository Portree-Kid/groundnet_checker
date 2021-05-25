package de.keithpaterson.groundnet_checker;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Loads a ground net into a @link org.jgrapht.Graph
 */
public class GroundnetLoader  {

	private static Pattern COORDINATE_PATTERN = Pattern.compile("([NSEW])([0-9]*)\\s*([.0-9]*)");

	public void process(File f) throws Exception {
//		File f = new File(PATH, item.getPath() + "/" + item.icao + ".groundnet.xml");
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

				HashMap<String, Element> nodes = new HashMap<>();
				// Create XPath object
				XPath xpath = xpathFactory.newXPath();
				NodeList parkingNodes = (NodeList) xpath.evaluate("/groundnet/parkingList/Parking", doc,
						XPathConstants.NODESET);
				for (int i = 0; i < parkingNodes.getLength(); i++) {
					Element n = (Element) parkingNodes.item(i);
					double lat = parse(n.getAttribute("lat"));
					double lon = parse(n.getAttribute("lon"));
					nodes.put(n.getAttribute("index"), n);
				}
				NodeList taxiNodes = (NodeList) xpath.evaluate("/groundnet/TaxiNodes/node", doc,
						XPathConstants.NODESET);
				for (int i = 0; i < taxiNodes.getLength(); i++) {
					Element n = (Element) taxiNodes.item(i);
					nodes.put(n.getAttribute("index"), n);

				}
				NodeList arcNodes = (NodeList) xpath.evaluate("/groundnet/TaxiWaySegments/arc", doc,
						XPathConstants.NODESET);
				for (int i = 0; i < arcNodes.getLength(); i++) {
					Element n = (Element) arcNodes.item(i);
					Element start = nodes.get(n.getAttribute("begin"));
					Element end = nodes.get(n.getAttribute("end"));
					double lat1 = parse(start.getAttribute("lat"));
					double lon1 = parse(start.getAttribute("lon"));
					double lat2 = parse(end.getAttribute("lat"));
					double lon2 = parse(end.getAttribute("lon"));
					if (n.getAttribute("isPushBackRoute").equals("1")) {

					} else {
					}
				}
				
				System.out.println();

			} catch (Exception e) {
				e.printStackTrace();
			}
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
	
	public Graph<Element, GroundnetEdge> loadGraphSafe(File f)  {
		try {
			return loadGraph(f);
		} catch (Exception e) {
			//Ignore
		}
		return null;
	}
	

	public Graph<Element, GroundnetEdge> loadGraph(File f)  {
		
		Graph<Element, GroundnetEdge> g = buildEmptySimpleGraph();
	//		File f = new File(PATH, item.getPath() + "/" + item.icao + ".groundnet.xml");
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
	
					HashMap<String, Element> nodes = new HashMap<>();
					// Create XPath object
					XPath xpath = xpathFactory.newXPath();
					NodeList parkingNodes = (NodeList) xpath.evaluate("/groundnet/parkingList/Parking", doc,
							XPathConstants.NODESET);
					for (int i = 0; i < parkingNodes.getLength(); i++) {
						Element n = (Element) parkingNodes.item(i);
						double lat = parse(n.getAttribute("lat"));
						double lon = parse(n.getAttribute("lon"));
						nodes.put(n.getAttribute("index"), n);
	                    g.addVertex(n);
					}
					NodeList taxiNodes = (NodeList) xpath.evaluate("/groundnet/TaxiNodes/node", doc,
							XPathConstants.NODESET);
					for (int i = 0; i < taxiNodes.getLength(); i++) {
						Element n = (Element) taxiNodes.item(i);
						nodes.put(n.getAttribute("index"), n);
	                    g.addVertex(n);
					}
					NodeList arcNodes = (NodeList) xpath.evaluate("/groundnet/TaxiWaySegments/arc", doc,
							XPathConstants.NODESET);
					for (int i = 0; i < arcNodes.getLength(); i++) {
						Element n = (Element) arcNodes.item(i);
						Element start = nodes.get(n.getAttribute("begin"));
						if(start == null)
							throw new RuntimeException("Node with index=" + n.getAttribute("start") + " not found");
						Element end = nodes.get(n.getAttribute("end"));
						if(end == null)
							throw new RuntimeException("Node with index=" + n.getAttribute("end") + " not found");
						g.addEdge(start, end, new GroundnetEdge(n));
						double lat1 = parse(start.getAttribute("lat"));
						double lon1 = parse(start.getAttribute("lon"));
						double lat2 = parse(end.getAttribute("lat"));
						double lon2 = parse(end.getAttribute("lon"));
					}
					
//					System.out.println();
	
				} catch (Exception e) {
					throw new RuntimeException(e);
//					e.printStackTrace();
				}
			}
			return g;
		}

	
	private static Graph<Element, GroundnetEdge> buildEmptySimpleGraph()
    {
        return GraphTypeBuilder
            .<Element, GroundnetEdge> directed().allowingMultipleEdges(false)
            .allowingSelfLoops(true).edgeClass(GroundnetEdge.class).weighted(false).buildGraph();
    }
}
