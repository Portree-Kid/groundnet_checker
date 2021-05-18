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
        FileInputStream fileIS = new FileInputStream(new File("C:\\GIT\\main\\target\\surefire-reports\\TEST-de.keithpaterson.groundnet_checker.GroundNetTest.xml"));
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document xmlDocument = builder.parse(fileIS);
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "/testsuite/testcase/failure";
        NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);

        AsciiTable table = new AsciiTable();

        System.out.println("");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node n = nodeList.item(i);
            String file = n.getAttributes().getNamedItem("message").getTextContent().split(":")[0];
            String message = n.getAttributes().getNamedItem("message").getTextContent().split(":")[1];
            String[] messages = message.split("==>")[0].split("[|]+");

            System.out.println(message);
            for (int j = 0; j < messages.length; j++) {
                if(messages[j].isEmpty())
                    continue;
                table.addRow(file, messages[j]);
                table.addRule();
                System.out.println(messages[j]);
            }
        }
//        System.out.println(table.render());
        System.out.println(table.render(140));
    }

}
