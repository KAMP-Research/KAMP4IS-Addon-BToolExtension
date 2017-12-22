package com.ptvgroup.devcomp.tooling.shortcut;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import edu.kit.ipd.sdq.kampws.client.ChangeSpecificDependencies;
import edu.kit.ipd.sdq.kampws.client.ChangeSpecificDependenciesService;
import edu.kit.ipd.sdq.kampws.client.IllegalArgumentException_Exception;
import edu.kit.ipd.sdq.kampws.client.StringArray;

/**
 * Provides the methods exposed by the KAMP web service.
 * 
 * @author Milena Neumann
 */
public class KampWSClient {

    private ChangeSpecificDependencies csd;

    private final String DEFAULT_WSDL = "http://localhost:8080/kamp-ws/services/changeSpecificDependencies?wsdl";

    public KampWSClient() throws MalformedURLException {
        String wsdlVariable = System.getenv("B_SHORTCUT_WSDL");
        URL wsdl;
        if (wsdlVariable == null) {
            wsdl = new URL(DEFAULT_WSDL);
        } else {
            wsdl = new URL(wsdlVariable);
        }

        ChangeSpecificDependenciesService service = new ChangeSpecificDependenciesService(wsdl);
        csd = service.getChangeSpecificDependenciesPort();
    }

    /**
     * Initialize Client to access the web service with the specified WSDL
     * 
     * @param wsdl WSDL of the service
     */
    public KampWSClient(URL wsdl) {
        ChangeSpecificDependenciesService service = new ChangeSpecificDependenciesService(wsdl);
        csd = service.getChangeSpecificDependenciesPort();
    }

    /**
     * Returns the project names known to the service.
     * 
     * @return list of project names known to the service
     */
    public List<String> getPossibleProjectNames() {
        return csd.getPossibleProjectNames().getItem();
    }

    /**
     * Returns the change scenarios for the project with the specified name.
     * 
     * @param projectName name of the project
     * @return list of change scenarios for the project
     * @throws IllegalArgumentException if project name is not recognized
     */
    public List<String> getChangeScenarios(String projectName) throws IllegalArgumentException {
        try {
            return csd.getChangeScenarios(projectName).getItem();
        } catch (IllegalArgumentException_Exception e) {
            throw new IllegalArgumentException("Could not find change scenarios for project. (" + e.getMessage()
                + ")");
        }
    }

    /**
     * Returns the dependent projects for the specified change scenarios.
     * 
     * @param changeScenarios the changed projects
     * @return list of projects affected by the changes
     * @throws IllegalArgumentException if one or multiple change scenarios are
     *             not recognized
     */
    public List<String> getChangeSpecificDependencies(List<String> changeScenarios) throws IllegalArgumentException {
        StringArray input = new StringArray();
        input.getItem().addAll(changeScenarios);
        try {
            return csd.getChangeSpecificDependencies(input).getItem();
        } catch (IllegalArgumentException_Exception e) {
            throw new IllegalArgumentException("Could not resolve dependencies. (" + e.getMessage() + ")");
        }
    }

    /**
     * Returns the paths of build specification files for the specified
     * projects.
     * 
     * @param projectNames names of the projects of interest
     * @return list of paths of build specification files
     * @throws IllegalArgumentException if one or multiple project names are not
     *             recognized
     */
    public List<String> getBuildSpecificationPaths(List<String> projectNames) throws IllegalArgumentException {
        StringArray input = new StringArray();
        input.getItem().addAll(projectNames);
        try {
            return csd.getBuildSpecificationPaths(input).getItem();
        } catch (IllegalArgumentException_Exception e) {
            throw new IllegalArgumentException("Could not find build specification paths. (" + e.getMessage() + ")");
        }
    }

}
