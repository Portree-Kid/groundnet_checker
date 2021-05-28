package de.keithpaterson.groundnet_checker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
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

    @ParameterizedTest(name = "{0}#Unconnected nodes")
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
            assertEquals(0, unconnected.size(), "There are unconnected nodes " + " " + unconnected);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @ParameterizedTest(name = "{0}#Runway routes")
    @ArgumentsSource(FileProvider.class)
    public void testRunwayRoutes(String f, Graph<Element, DefaultEdge> loadGraph) {
        if (loadGraph == null)
            return;
        try {
            List<Element> runwayEnds = loadGraph.vertexSet().stream()
                    .filter(p -> p.getAttribute("isOnRunway").equals("1"))
                    .filter(p -> Graphs.neighborListOf(loadGraph, p).size() >= 1).collect(Collectors.toList());
            List<Element> parkingEnds = loadGraph.vertexSet().stream().filter(p -> p.getNodeName().equals("Parking"))
                    .filter(p -> Graphs.neighborListOf(loadGraph, p).size() >= 1).collect(Collectors.toList());
            for (Element parkingNode : parkingEnds) {
                for (Element runwayNode : runwayEnds) {
                    GraphPath<Element, DefaultEdge> pathBetween = DijkstraShortestPath.findPathBetween(loadGraph,
                            parkingNode, runwayNode);
                    assertNotNull(pathBetween,"There are missing routes in " + " From " + parkingNode.getAttribute("name") + " To ("
                                    + parkingNode.getAttribute("index") + ") Node " + runwayNode.getAttribute("name")
                                    + " (" + runwayNode.getAttribute("index") + ")");
//					System.out.println("Length " + pathBetween.getLength());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ParameterizedTest(name = "{0}#Pushback routes")
    @ArgumentsSource(FileProvider.class)
    public void testPushbackRoutes(String f, Graph<Element, GroundnetEdge> loadGraph) {
        if (loadGraph == null)
            return;
        try {
            String message = "";
            List<Element> parkingEnds = loadGraph.vertexSet().stream().filter(p -> p.hasAttribute("type")).collect(Collectors.toList());
            for (Element parkingNode : parkingEnds) {
                if(parkingNode.hasAttribute("pushBackRoute")) {
                    List<Element> nextNodes = Graphs.neighborListOf(loadGraph, parkingNode);
                    List<Element> pushbackPoint = loadGraph.vertexSet().stream()
                            .filter(p -> p.getAttribute("index").equals(parkingNode.getAttribute("pushBackRoute"))).collect(Collectors.toList());
                    if( pushbackPoint.size() != 1 ) {
                        message += "Pushback Node " + parkingNode.getAttribute("pushBackRoute") + " doesn't exist"  + "||";
                        continue;
                    }
                    if( !"PushBack".equals(pushbackPoint.get(0).getAttribute("holdPointType").toString()) ) {
                        message += "Pushback Node " + parkingNode.getAttribute("pushBackRoute") + " must be of type PushBack ("  + pushbackPoint.get(0).getAttribute("holdPointType").toString() + ")||";
                        continue;
                    }
                    GraphPath<Element, GroundnetEdge> pathBetween = DijkstraShortestPath.findPathBetween(loadGraph,
                            parkingNode, pushbackPoint.get(0));
                    if(pathBetween==null) {
                        message += "Route from Parking " + parkingNode.getAttribute("index") + " pushing back to " + parkingNode.getAttribute("pushBackRoute") + " doesn't exist" + "||";
                    } else {
                        boolean allPushback = true;
                        for (GroundnetEdge edgy :pathBetween.getEdgeList()) {
                            allPushback &= edgy.isPushback();
                        }
                        if(!allPushback) {
                            message += "Route from Parking " + parkingNode.getAttribute("index") + " pushing back to " + parkingNode.getAttribute("pushBackRoute") + " must consist of pushback segments" + "||";
                        }
                    }

                }
            }
            assertEquals("", message, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ParameterizedTest(name = "{0}#Parking Radius")
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
            assertEquals(0, missizedParkings.size(), "There are parkings with large radius " + missizedParkings);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ParameterizedTest(name = "{0}#Nowhere to Park ")
    @ArgumentsSource(FileProvider.class)
    public void testNowhereToPark(String f, Graph<Element, DefaultEdge> loadGraph) {
        if (loadGraph == null)
            return;
        try {
            List<Element> possibleParkings = loadGraph.vertexSet().stream().filter(p -> p.hasAttribute("type")).filter(
                    p -> p.getAttribute("type").equals("gate") && p.getAttribute("airlineCodes").trim().equals(""))
                    .collect(Collectors.toList());

            assertTrue(possibleParkings.size() > 0, "Nowhere to park ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ParameterizedTest(name = "{0}#Intersecting parking ")
    @ArgumentsSource(FileProvider.class)
    public void testIntersectingParking(String f, Graph<Element, DefaultEdge> loadGraph) {
        if (loadGraph == null)
            return;
        try {
            List<Element> possibleParkings = loadGraph.vertexSet().stream().filter(p -> p.hasAttribute("type"))
                    .collect(Collectors.toList());
            String message = "";
            // To check if we already have checke the invers
            HashMap<String, String> intersects = new HashMap<>();
            for (Element p1 : possibleParkings) {
                for (Element p2 : possibleParkings) {
                    if (p1 == p2)
                        continue;
					if (p1.getAttribute("index").equals(p2.getAttribute("index")))
						continue;
                    if (p1.getAttribute("radius").isEmpty())
                        continue;
                    if (p2.getAttribute("radius").isEmpty())
                        continue;
                    String key = p1.getAttribute("index") + "_" + p2.getAttribute("index");
                    String inverseKey = p2.getAttribute("index") + "_" + p1.getAttribute("index");
                    if(intersects.containsKey(inverseKey))
                        continue;
                    Area p1Box = buildBox(p1);
                    p1Box.intersect(buildBox(p2));

                    if (!p1Box.isEmpty()) {
                        message += "Parkings " + p1.getAttribute("name") + "("+p2.getAttribute("index")+") & " + p2.getAttribute("name")
                                + "("+p1.getAttribute("index")+") intersect" + "||";
                        intersects.put(key, "FAIL");
                    } else {
                        intersects.put(key, "PASS");
                    }
                }
            }
            assertEquals("", message, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Area buildBox(Element p1) {
        double lat1 = parse(p1.getAttribute("lat"));
        double lon1 = parse(p1.getAttribute("lon"));
        double r1 = Double.parseDouble(p1.getAttribute("radius"));
        double h1 = Double.parseDouble(p1.getAttribute("heading"));
        Point2D start = new Point2D.Double();
        // Math order of coordinates
        start.setLocation(lon1, lat1);

        double left = normalize(h1 - 90);
        double right = normalize(h1 + 90);
        double backwards = normalize(h1 + 180);

        double thirdRadiusKM = r1/3;
        double halfRadiusKM = r1/2;


        Point2D front = destination(start, h1, r1);
        Point2D back = destination(start, backwards, r1);
        Point2D leftBack = destination(back, left, r1);
        Point2D rightBack = destination(back, right, r1);
        Point2D leftMiddle = destination(start, left, r1);
        Point2D rightMiddle = destination(start, right, r1);
        Point2D leftFront = destination(front, left, thirdRadiusKM);
        Point2D rightFront = destination(front, right, thirdRadiusKM);
        Point2D leftIntermediate = destination(leftFront, backwards, halfRadiusKM);
        Point2D rightIntermediate = destination(rightFront, backwards, halfRadiusKM);

        Path2D p = new Path2D.Double();
        p.moveTo(leftFront.getX(), leftFront.getY());
        p.lineTo(rightFront.getX(), rightFront.getY());
        p.lineTo(rightIntermediate.getX(), rightIntermediate.getY());
        p.lineTo(rightMiddle.getX(), rightMiddle.getY());
        p.lineTo(rightBack.getX(), rightBack.getY());
        p.lineTo(leftBack.getX(), leftBack.getY());
        p.lineTo(leftMiddle.getX(), leftMiddle.getY());
        p.lineTo(leftIntermediate.getX(), leftIntermediate.getY());
        p.closePath();
        Area a = new Area(p);
        return a;
    }

    private Point2D destination(Point2D start, double direction, double distance) {
        GeodeticCalculator calcy = new GeodeticCalculator();
        calcy.setStartingGeographicPoint(start);
        calcy.setDirection(direction, distance);
        return calcy.getDestinationGeographicPoint();
    }

    double normalize(double value) {
        double width = 360;   //
        return (value - (Math.floor(value / width) * width));
        // + start to reset back to start of original range
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

    /**
     * Provides a list of files and the loaded groundnet
     */

    public static class FileProvider implements ArgumentsProvider {

        private static HashMap<String, Traffic> trafficList;
        private static String branch;

        @SuppressWarnings("unchecked")
        private static void loadTraffic() {
            try (ObjectInputStream is = new ObjectInputStream(new FileInputStream("traffic.obj"))) {
                trafficList = (HashMap<String, Traffic>) is.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private static List<Arguments> files = null;

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
            loadTraffic();
            File projectBaseDir = new File(".");
            assertNotNull(projectBaseDir);
            assertTrue(projectBaseDir.exists());
            try {
                if (branch != null && branch.matches("[A-Z]*_[a-zA-Z0-9]*_[0-9]*")) {
                    String type = branch.substring(0, branch.indexOf("_"));
                    if( type.equals("GROUNDNET")) {
                        String icao = branch.substring(branch.indexOf("_") + 1, branch.indexOf("_", branch.indexOf("_") + 1));
                        System.out.println("Matched Branch : " + icao);
                        return Files.walk(projectBaseDir.toPath()).filter(p -> Files.isRegularFile(p))
                                .filter(p -> !p.getFileName().toString().equals("pom.xml"))
                                .filter(p -> p.getFileName().toString().matches(icao + "\\.(groundnet)\\.xml"))
                                .map(p -> new Object[]{p.getFileName().toString(),
                                        new GroundnetLoader().loadGraphSafe(p.toFile())})
                                .map(Arguments::of).collect(Collectors.toList());
                    }
                } else {
                    return Files.walk(projectBaseDir.toPath()).filter(p -> Files.isRegularFile(p))
                            .filter(p -> !p.getFileName().toString().equals("pom.xml"))
                            .filter(p -> p.getFileName().toString().matches("[a-zA-Z0-9]*\\.(groundnet)\\.xml"))
                            //.peek(p -> System.out.println(p.getFileName().toString().split("[.]")[0] + "\t" + trafficList.containsKey(p.getFileName().toString().split("[.]")[0])))
                            .filter(p -> !(ignore.containsKey(p.getFileName().toString())) ||
                                    trafficList.containsKey(p.getFileName().toString().split("[.]")[0]))
                            .peek(p -> System.out.println(p.getFileName()))
                            .map(p -> new Object[]{p.getFileName().toString(),
                                    new GroundnetLoader().loadGraphSafe(p.toFile())})
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
            if (branch == null || !branch.equals(System.getProperty("TRAVIS_BRANCH"))) {
                branch = System.getProperty("TRAVIS_BRANCH");
                System.out.println("Creating for Branch : " + branch);
                files = fileProvider();
            }
            return files.stream();
        }
    }

}
