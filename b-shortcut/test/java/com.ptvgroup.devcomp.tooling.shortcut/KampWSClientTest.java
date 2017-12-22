/**
 * 
 */
package com.ptvgroup.devcomp.tooling.shortcut;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.kit.ipd.sdq.kampws.client.IllegalArgumentException_Exception;

/**
 * Tests for the KampWSClient.
 * 
 * @author Milena Neumann
 */
public class KampWSClientTest {

    static KampWSClient client;

    @BeforeClass
    public static void initialize() throws MalformedURLException {
        try {
            client = new KampWSClient();
        } catch (Exception e) {} ;

        assumeTrue(client != null);
    }

    @Test
    public void testWSDLConstructor() {
        URL wsdl = null;
        try {
            wsdl = new URL("http://172.23.112.89:8009/kamp-ws/services/changeSpecificDependencies?wsdl");
        } catch (MalformedURLException e) {
            fail();
        }
        client = new KampWSClient(wsdl);
    }

    @Test
    public void testGetPossibleProjectNames() {
        List<String> response = client.getPossibleProjectNames();
        assertTrue(response.size() > 200);
    }

    @Test
    public void testGetPossibleChangeScenariosForProject_xs_frontend() throws IllegalArgumentException_Exception {
        ArrayList<String> response = (ArrayList<String>) client.getChangeScenarios("xs-frontend");
        assertTrue(response.size() > 2);
        assertTrue(response.contains("xs-frontend_default"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetChangeScenarios_Invalid() {
        client.getChangeScenarios("invalid");
    }

    @Test
    public void testGetChangeSpecificDependencies_xs_frontend_default() throws IllegalArgumentException_Exception {
        List<String> request = new ArrayList<>();
        request.add("xs-frontend_default");
        ArrayList<String> response = (ArrayList<String>) client.getChangeSpecificDependencies(request);
        assertTrue(response.size() > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetChangeSpecificDependencies_Invalid() {
        ArrayList<String> example = new ArrayList<>();
        example.add("invalid");
        client.getChangeSpecificDependencies(example);
    }

    @Test
    public void testGetBuildSpecificationPaths() throws IllegalArgumentException_Exception {
        List<String> request = new ArrayList<>();
        request.add("xs-generation-model");
        request.add("xs-frontend");
        request.add("xs-server-services");
        request.add("xs-server-container");
        ArrayList<String> response = (ArrayList<String>) client.getBuildSpecificationPaths(request);
        assertTrue(response.contains("xs/generation/model/pom.xml"));
        assertTrue(response.contains("xs/frontend/pom.xml"));
        assertTrue(response.contains("xs/server/services/pom.xml"));
        assertTrue(response.contains("xs/server/container/pom.xml"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBuildSpecificationPaths_Invalid() {
        ArrayList<String> example = new ArrayList<>();
        example.add("invalid");
        client.getBuildSpecificationPaths(example);
    }

}
