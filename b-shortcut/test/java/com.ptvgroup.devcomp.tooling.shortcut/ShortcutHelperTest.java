package com.ptvgroup.devcomp.tooling.shortcut;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

public class ShortcutHelperTest {

    private static final Path ROOT = Paths.get("src/test/resources/shortcuttest/root").toAbsolutePath();

    @BeforeClass
    public static void initialize() throws MalformedURLException {
        KampWSClient client = null;
        try {
            client = new KampWSClient();
        } catch (Exception e) {} ;

        assumeTrue(client != null);
    }

    @Test
    public void testGetProjectNameFromPom() {
        Path pom = Paths.get("src/test/resources/shortcuttest/pom_generation.xml");
        String result = null;
        try {
            result = ShortcutHelper.getProjectNameFromPom(pom);
        } catch (IOException e) {
            fail();
        }
        assertTrue(result.contains("xs-generation"));
    }

    @Test
    public void testGetProjectNameFromPom_Nameless() {
        Path pom = Paths.get("src/test/resources/shortcuttest/pom_generation_nameless.xml");
        String result = null;
        try {
            result = ShortcutHelper.getProjectNameFromPom(pom);
        } catch (IOException e) {
            fail();
        }
        assertTrue(result.equals("generation"));
    }

    @Test(expected = IOException.class)
    public void testGetProjectNameFromPom_InvalidPath() throws IOException {
        Path pom = Paths.get("src/test/resources/shortcuttest/invalid.xml");
        ShortcutHelper.getProjectNameFromPom(pom);
    }

    @Test(expected = IOException.class)
    public void testGetProjectNameFromPom_InvalidXML() throws IOException {
        Path pom = Paths.get("src/test/resources/shortcuttest/notapom.xml");
        ShortcutHelper.getProjectNameFromPom(pom);
    }

    @Test
    public void testGetChangeScenarioByUserInput_singleScenario() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        String result = ShortcutHelper.getChangeScenarioForProject("xs-frontend-dashboard");
        assertTrue(result, result.equals("xs-frontend-dashboard_default"));
    }

    @Test
    public void testGetChangeScenarioByUserInput_1() {
        String input = "1";
        System.setIn(new BufferedInputStream(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        String result = ShortcutHelper.getChangeScenarioForProject("xs-generation");
        assertTrue(result, result.equals("xs-generation_default"));
    }

    @Test
    public void testGetChangeScenarioByUserInput_2() {
        String input = "2";
        System.setIn(new BufferedInputStream(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        String result = ShortcutHelper.getChangeScenarioForProject("xs-generation");
        assertTrue(result, result.equals("xs-generation_include_submodules"));
    }

    @Test
    public void testGetChangeScenarioByUserInput_Invalid() {
        String input = "nope\n2";
        System.setIn(new BufferedInputStream(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        String result = ShortcutHelper.getChangeScenarioForProject("xs-generation");
        assertTrue(result, result.equals("xs-generation_include_submodules"));
    }

    @Test
    public void testGetChangeScenarioByUserInput_InvalidNumbers() {
        String input = "5\n0\n8\n2";
        System.setIn(new BufferedInputStream(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        String result = ShortcutHelper.getChangeScenarioForProject("xs-generation");
        assertTrue("Expected: xs-generation_include_submodules, found: " + result,
            result.equals("xs-generation_include_submodules"));
    }

    @Test
    public void testGetChangeSpecificDependencies() {
        ArrayList<String> example = new ArrayList<>();
        example.add("xs-frontend_default");
        ArrayList<String> response = (ArrayList<String>) ShortcutHelper.getChangeSpecificDependencies(example);
        String[] expectedEntries = { "xs-server-services", "xs-server-container" };
        for (String entry : expectedEntries) {
            assertTrue("Expected Entry " + entry + " not found.", response.contains(entry));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetChangeSpecificDependencies_Invalid() {
        ArrayList<String> example = new ArrayList<>();
        example.add("invalid");
        ShortcutHelper.getChangeSpecificDependencies(example);
    }

    @Test
    public void testGetRelativeProjectPaths() throws IllegalArgumentException, FileNotFoundException {
        List<String> projectNames = new ArrayList<>();
        projectNames.add("lbc");
        projectNames.add("xs-frontend");

        List<String> result = null;

        result = ShortcutHelper.getRelativeProjectPaths(projectNames, ROOT);

        assertTrue(result.size() == 2);
        assertTrue(result.toString(), result.contains("lbc"));
        assertTrue(result.toString(), result.contains("xs/frontend"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRelativeProjectPaths_Invalid() throws FileNotFoundException {
        List<String> projectNames = new ArrayList<>();
        projectNames.add("invalid");
        projectNames.add("xs-runtime");
        ShortcutHelper.getRelativeProjectPaths(projectNames, ROOT);
    }

    @Test(expected = FileNotFoundException.class)
    public void testGetRelativeProjectPaths_POMDoesNotExist() throws FileNotFoundException {
        List<String> projectNames = new ArrayList<>();
        projectNames.add("xs-runtime");
        ShortcutHelper.getRelativeProjectPaths(projectNames, ROOT);
    }
}
