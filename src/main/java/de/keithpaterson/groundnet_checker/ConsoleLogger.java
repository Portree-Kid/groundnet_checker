package de.keithpaterson.groundnet_checker;

import de.vandermeer.asciitable.AsciiTable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ConsoleLogger {

    public static void main(String[] args) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        AsciiTable table = new AsciiTable();

        File f = new File(args[0]);
        File[] files = f.listFiles((dir, name) -> name.startsWith("TEST"));

        for (File testfile : files) {
            processFile(testfile, table);
        }
//        System.out.println(table.render());
        if (!table.getRawContent().isEmpty()) {
            System.out.println(table.render(140));
        }
    }

    private static void processFile(File f, AsciiTable table) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        FileInputStream fileIS = new FileInputStream(f);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document xmlDocument = builder.parse(fileIS);
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "/testsuite/testcase/failure";
        NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node n = nodeList.item(i);
            String file = n.getAttributes().getNamedItem("message").getTextContent().split(":")[0];
            String message = n.getAttributes().getNamedItem("message").getTextContent().split(":")[1];
            String[] messages = message.split("==>")[0].split("[|]+");

            System.out.println(message);
            for (int j = 0; j < messages.length; j++) {
                if (messages[j].trim().isEmpty())
                    continue;
                table.addRow(file, messages[j]);
                table.addRule();
                System.out.println(messages[j]);
            }
        }
    }

}
