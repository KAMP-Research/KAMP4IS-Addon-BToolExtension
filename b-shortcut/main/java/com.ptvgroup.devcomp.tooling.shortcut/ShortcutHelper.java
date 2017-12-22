package com.ptvgroup.devcomp.tooling.shortcut;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author Milena Neumann
 */
public class ShortcutHelper {

    static private XPath xpath;
    static private DocumentBuilder builder;
    static private DocumentBuilderFactory factory;
    static private KampWSClient kampws;

    static {
        xpath = XPathFactory.newInstance().newXPath();
        factory = DocumentBuilderFactory.newInstance();
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        try {
            kampws = new KampWSClient();
        } catch (Exception e) {
            kampws = null;
        }
    }

    public static boolean webServiceAvailable() {
        return (kampws != null);
    }

    /**
     * Returns the project name parsed from the specified POM file, or the
     * artifactId if no name is found.
     * 
     * @param pomFile Path of the POM file
     * @return name of of the project
     * @throws IOException If IO errors occur while parsing the POM file.
     */
    public static String getProjectNameFromPom(Path pomFile) throws IOException {
        String name = "";
        String exceptionText = "Could not parse required information in file " + pomFile.toString()
            + ". Is that file a valid POM?";
        try {
            Document pom = builder.parse(pomFile.toFile());
            name = xpath.evaluate("/project/name", pom);
            if (name.equals("")) {
                name = xpath.evaluate("/project/artifactId", pom);
            }
            if (name.equals("")) {
                throw new IOException(exceptionText);
            }
            return name;
        } catch (SAXException | XPathExpressionException e) {
            throw new IOException(exceptionText);
        }
    }

    /**
     * Returns the applicable change scenario for the specified project
     * (determined by user input).
     * 
     * @param projectName name of the project of interest
     * @return The change scenario for the project
     * @throws IllegalArgumentException if the project name is invalid
     */
    public static String getChangeScenarioForProject(String projectName) throws IllegalArgumentException {
        // get the change scenarios available for this projectName
        List<String> changeScenarios = kampws.getChangeScenarios(projectName);
        // ask user for the applicable change scenario
        return ShortcutHelper.getChangeScenarioByUserInput(changeScenarios, projectName);
    }

    /**
     * Asks the user to select one of the change scenarios specified in the
     * list.
     * 
     * @param changeScenarios List of change scenarios to choose from
     * @return the selected change scenario
     */
    private static String getChangeScenarioByUserInput(List<String> changeScenarios, String projectName) {
        System.out.println("");
        // if only one choice is available, don't bother to ask
        if (changeScenarios.size() == 1) {
            System.out.println("The project " + projectName + " has no build shortcuts.");
            return changeScenarios.get(0);
        }

        // print options
        System.out.println("The following change scenarios exist for " + projectName + ": ");
        for (int i = 0; i < changeScenarios.size(); i++) {
            System.out.println("    " + (i < 9 ? " " : "") + (i + 1) + ": " + changeScenarios.get(i));
        }
        Integer choice = getUserSelection(changeScenarios);

        String result = null;
        try {
            result = changeScenarios.get(choice);
        } catch (NoSuchElementException e) {
            System.out.println("Cannot recover from invalid input, ending shortcut execution.");
            return null;
        }

        System.out.println("\nYou selected this option: " + result);
        return result;
    }

    /**
     * Lets the user choose one of the specified change scenarios.
     * 
     * @param changeScenarios scenarios to choose from
     * @return Index of the selected change scenario
     */
    private static Integer getUserSelection(List<String> changeScenarios) throws NoSuchElementException {
        @SuppressWarnings("resource")
        Scanner scanner = new Scanner(System.in);

        // ask for selection
        System.out.println("Select the applicable scenario by entering its index: ");
        Integer choice;
        String line = "";
        do {
            try {
                line = scanner.nextLine();
                choice = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                choice = -1;
            }
            // ask again until it's valid
            if (choice <= 0 || choice > changeScenarios.size()) {
                System.out.println("\n\"" + line + "\" is not a valid selection. Please enter an integer in [1, "
                    + changeScenarios.size() + "]:");
            }
        } while (choice <= 0 || choice > changeScenarios.size());
        // correct the index
        return (choice - 1);
    }

    /**
     * Returns the change-specific dependencies for the specified list of change
     * scenarios.
     * 
     * @param changeScenarios the list of change scenarios
     * @return the list of dependent projects
     * @throws IllegalArgumentException if a change scenario is invalid
     */
    public static List<String> getChangeSpecificDependencies(List<String> changeScenarios)
        throws IllegalArgumentException {
        return kampws.getChangeSpecificDependencies(changeScenarios);
    }

    /**
     * Returns for each projectName the project path relative to root.
     * 
     * @param projectNames projects of interest
     * @param root the root directory
     * @return relative project paths
     * @throws IllegalArgumentException if a project name was invalid
     * @throws FileNotFoundException if a POM file for a project name is missing
     */
    public static List<String> getRelativeProjectPaths(List<String> projectNames, Path root)
        throws IllegalArgumentException, FileNotFoundException {

        List<String> result = new ArrayList<>();
        List<String> pomPaths = kampws.getBuildSpecificationPaths(projectNames);
        for (String pomPath : pomPaths) {
            String folderPath = pomPath.replace("/pom.xml", "");
            if (!pomAtPathExists(pomPath, root)) {
                throw new FileNotFoundException("There is no pom.xml in directory " + root + "/"
                    + folderPath + ". Did you forget to initialize?");
            }
            result.add(folderPath);
        }
        return result;
    }

    /**
     * Returns whether a POM file exists at the specified path.
     * 
     * @param relativePath Path relative to root where the POM file is expected
     * @param root the root directory
     * @return whether a POM file exists at the specified path
     */
    private static boolean pomAtPathExists(String relativePath, Path root) {
        String pomPath = root.toString() + "/" + relativePath;
        return Files.exists(Paths.get(pomPath));
    }
}
