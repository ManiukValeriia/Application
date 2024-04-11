package src;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.SchemaFactory;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class Application {
    static Set<String> ethnicities = new HashSet<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            System.out.println("1) Розібрати та показати документ XML");
            System.out.println("2) Перевірити XML за допомогою XSD");
            System.out.println("3) Показати існуючі етноси");
            System.out.println("4) Показати найпопулярніші імена із зазначенням етнічної приналежності та статі");
            System.out.println("5) Читати та відображати за допомогою аналізатора DOM");
            System.out.println("6) Вихід");
            System.out.print("[Вибір]: ");

            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    parse_n_show_XML_doc();
                    break;
                case 2:
                    validate_XML_by_XSD();
                    break;
                case 3:
                    show_ethnicities();
                    break;
                case 4:
                    show_top_names();
                    break;
                case 5:
                    read_and_display_DOM();
                    break;
                case 6:
                    System.exit(0);
                    break;
                default:
                    System.out.println("Неправильний вибір номера... Спробуйте ще раз:");
            }
        } while (choice != 6);

        scanner.close();
    }

    static void parse_n_show_XML_doc() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser sax_parser = factory.newSAXParser();

            DefaultHandler handler = new DefaultHandler() {
                boolean status = false;

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    System.out.println("Start element: " + qName);
                    status = true;
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    System.out.println("End element: " + qName);
                    status = false;
                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    if (status) { System.out.println("Text: " + new String(ch, start, length)); }
                }
            };

            sax_parser.parse(new File("Popular_Baby_Names_NY.xml"), handler);
        } catch (ParserConfigurationException | SAXException | IOException e) { e.printStackTrace(); }
    }

    static void validate_XML_by_XSD() {
        try {
            File xsd_file = new File("Popular_Baby_Names_NY.xml");
            SchemaFactory schema_factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schema_factory.newSchema(xsd_file);

            System.out.println("XML document corresponds to XSD");
        } catch (SAXException e) { System.out.println("XML document does not corresponds to XSD"); }
    }

    static void show_ethnicities() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser sax_parser_ethnicity = factory.newSAXParser();

            DefaultHandler handler_ethnicity = new DefaultHandler() {
                boolean in_ethnicity = false;

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    if (qName.equalsIgnoreCase("ethcty")) { in_ethnicity = true; }
                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    if (in_ethnicity) {
                        String ethnicity = new String(ch, start, length).trim();
                        if (!ethnicity.isEmpty()) { ethnicities.add(ethnicity); }
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    if (qName.equalsIgnoreCase("ethcty")) {
                        in_ethnicity = false;
                    }
                }
            };

            sax_parser_ethnicity.parse(new File("Popular_Baby_Names_NY.xml"), handler_ethnicity);

            System.out.println("Етнічні групи:");
            for (String ethnicity : ethnicities) { System.out.println(ethnicity); }
        } catch (ParserConfigurationException | SAXException | IOException e) { e.printStackTrace(); }
    }


    static void show_top_names() {
        try {
            Scanner scanner = new Scanner(System.in);

            System.out.print("Етнічні групи: ");
            String ethnicity = scanner.nextLine();

            System.out.print("Введіть стать(MALE or FEMALE): ");
            String gender = scanner.nextLine();

            List<BabyName> baby_names = new ArrayList<>();

            File input_fle = new File("Popular_Baby_Names_NY.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(input_fle);

            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("row");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) nNode;
                    String ethnicity_node = element.getElementsByTagName("ethcty").item(0).getTextContent();
                    String gender_node = element.getElementsByTagName("gndr").item(0).getTextContent();

                    if (ethnicity_node.equalsIgnoreCase(ethnicity) && gender_node.equalsIgnoreCase(gender)) {
                        String name = element.getElementsByTagName("nm").item(0).getTextContent();

                        int count = Integer.parseInt(element.getElementsByTagName("cnt").item(0).getTextContent());
                        int rating = Integer.parseInt(element.getElementsByTagName("rnk").item(0).getTextContent());

                        baby_names.add(new BabyName(name, gender, count, rating, ethnicity_node));
                    }
                }
            }

            List<BabyName> merged_names = new ArrayList<>();
            for (BabyName name : baby_names) {
                boolean found = false;
                for (BabyName merged : merged_names) {
                    if (merged.getName().equalsIgnoreCase(name.getName())) {
                        merged.setCount(merged.getCount() + name.getCount());
                        found = true;
                        break;
                    }
                }

                if (!found) { merged_names.add(name); }
            }

            Collections.sort(merged_names);

            System.out.println("Top 10 popular names of " + ethnicity + " of " + gender + ":");
            for (int i = 0; i < Math.min(10, merged_names.size()); i++) {
                BabyName babyName = merged_names.get(i);
                System.out.println("Name: " + babyName.getName() + ", Gender: " + babyName.getGender() + ", Amount: " + babyName.getCount() + ", Rating: " + babyName.getRating());
            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document newDoc = docBuilder.newDocument();

        Element rootElement = newDoc.createElement("TopNames");
        newDoc.appendChild(rootElement);

        for (int i = 0; i < Math.min(10, merged_names.size()); i++) {
            BabyName babyName = merged_names.get(i);
            Element nameElement = newDoc.createElement("Name");
            nameElement.setAttribute("Name", babyName.getName());
            nameElement.setAttribute("Gender", babyName.getGender());
            nameElement.setAttribute("Amount", String.valueOf(babyName.getCount()));
            nameElement.setAttribute("Rating", String.valueOf(babyName.getRating()));
            rootElement.appendChild(nameElement);
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(newDoc);
        StreamResult result = new StreamResult(new File("TopNames.xml"));
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(source, result);

        System.out.println("Top 10 popular names of " + ethnicity + " of " + gender + " saved to TopNames.xml");

        } catch (ParserConfigurationException | SAXException | IOException | NumberFormatException | TransformerException e) { e.printStackTrace(); }
    }

    static class BabyName implements Comparable<BabyName> {
        private String name;
        private String gender;
        private String ethnicity;
        private int count;
        private int rating;

        public BabyName(String name, String gender, int count, int rating, String ethnicity) {
            this.name = name;
            this.gender = gender;
            this.ethnicity = ethnicity;
            this.count = count;
            this.rating = rating;
        }

        public String getName() { return name; }

        public String getGender() { return gender; }

        public int getCount() { return count; }

        public void setCount(int count) { this.count = count; }

        public int getRating() { return rating; }

        public String getEthnicity() { return ethnicity; }

        @Override
        public int compareTo(BabyName o) { return Integer.compare(o.rating, this.rating); }
    }

    static void read_and_display_DOM() {
        try {
            File inputFile = new File("Popular_Baby_Names_NY.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
    
            System.out.println("Root element: " + doc.getDocumentElement().getNodeName());
            NodeList nodeList = doc.getElementsByTagName("*");
            for (int temp = 0; temp < nodeList.getLength(); temp++) {
                Node node = nodeList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    System.out.print("Element: " + node.getNodeName());
                    if (node.hasAttributes()) {
                        NamedNodeMap nodeMap = node.getAttributes();
                        for (int i = 0; i < nodeMap.getLength(); i++) {
                            Node attr = nodeMap.item(i);
                            System.out.print(" [" + attr.getNodeName() + "=\"" + attr.getNodeValue() + "\"]");
                        }
                    }
                    System.out.println();
                    if (node.hasChildNodes()) {
                        System.out.println("Text: " + node.getTextContent().trim());
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) { e.printStackTrace(); }
    }
}
