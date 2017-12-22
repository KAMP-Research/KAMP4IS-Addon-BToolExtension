package com.ptvgroup.devcomp.tooling.creator.cmd;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.ptvgroup.devcomp.tooling.shortcut.ProjectListConverter;
import com.ptvgroup.devcomp.tooling.shortcut.ShortcutHelper;
import com.ptvgroup.tooling.poms.Misc;

/**
 * The shortcut command.
 * 
 * @author Milena Neumann
 */
@Parameters(commandDescription = "Use build shortcuts to build only what is affected by a change scenario.")
public class Shortcut extends CommandBase implements Subcommand {

    private JCommander self;

    @Parameter
    private List<String> parameters = new ArrayList<>();

    @Parameter(names = { "--projectNames", "-pn" },
        listConverter = ProjectListConverter.class, required = false,
        description = "If provided, will treat the specified list of projects as modified"
            + " instead of the project shortcut was executed in.")
    public List<String> projectNames = null;

    public static Shortcut create(JCommander jc) {
        Shortcut sc = new Shortcut();
        jc.addCommand(sc.getDefaultName(), sc, sc.getAliases());
        sc.self = jc.getCommands().get(sc.getDefaultName());
        sc.self.setAcceptUnknownOptions(true);
        return sc;
    }

    @Override
    public String getDefaultName() {
        return "shortcut";
    }

    @Override
    public String[] getAliases() {
        return new String[] { "s" };
    };

    @Override
    public int execute() throws Exception {
        if (help) {
            self.usage();
            return 0;
        }
        long startTime = System.currentTimeMillis();

        int succeeded = doExecute();

        if (succeeded != 0) {
            System.err.println("\nFailed due to errors!");
            System.err.println("Note: Remember to call 'b' instead of 'mvn' to retry");
            System.out.println("\nTook: " + (System.currentTimeMillis() - startTime) + "ms");
        }

        return succeeded;
    }

    private int doExecute() {

        if (!checkPreconditions()) {
            return 1;
        }

        // determine which projects were modified
        List<String> changedProjects = projectNames;
        if (changedProjects == null) {
            try {
                changedProjects = new ArrayList<String>(Arrays.asList(getProjectNameFromPom()));
            } catch (Exception e) {
                System.err.println(e.getMessage());
                return 1;
            }
        }

        // ... and in what way (change scenarios)
        List<String> changeScenarios = new ArrayList<>();
        try {
            for (String projectName : changedProjects) {
                changeScenarios.add(ShortcutHelper.getChangeScenarioForProject(projectName));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }

        if (verbose) {
            System.out.println("\nAll selected change scenarios: ");
            prettyPrintList(changeScenarios);
        }

        // determine which projects depend on change scenarios
        List<String> projectsToBuild = ShortcutHelper.getChangeSpecificDependencies(changeScenarios);
        if (verbose) {
            System.out.println("\nFound the following dependencies:");
            prettyPrintList(projectsToBuild);
        }

        // build the dependent projects
        int result;
        try {
            result = buildProjects(projectsToBuild);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }

        return result;
    }

    /**
     * Checks whether the web service is online and the current working
     * directory is a subdirectory of a valid checkout.
     * 
     * @return whether the preconditions are met
     */
    private boolean checkPreconditions() {
        if (!ShortcutHelper.webServiceAvailable()) {
            System.err.println(
                "Web service not found; cannot execute shortcut command!");
            System.out.println("Hint: here are two possible causes that may help you troubleshoot:\n" +
                "   1. The path specified in the system variable \"B_SHORTCUT_WSDL\" may be invalid.\n" +
                "   2. The web service may be offline");

            return false;
        }

        Path root = Misc.getCheckoutRoot(Paths.get("."));
        if (root == null) {
            System.err.println("Could not find checkout root from \'" + Paths.get(".").toAbsolutePath()
                + "\'. This command does not work with a partial checkout.");
            return false;
        }

        return true;
    }

    /**
     * Determines the name of a project from its POM file.
     * 
     * @return The name of the project or, if there is no name defined, its
     *         artifactId (default Maven behaviour).
     * @throws IOException if POM file cannot be properly parsed.
     * @throws FileNotFoundException if the POM file is not found.
     */
    private String getProjectNameFromPom() throws IOException, FileNotFoundException {
        Path cwd = Paths.get(".").toAbsolutePath();
        Path pom = Misc.getClosestPom(cwd);
        if (pom == null) {
            throw new FileNotFoundException("\nCould not find checkout root from \"" + cwd + "\"."
                + "\nNote: Please execute the shortcut command only in (sub)directories of a checkout root.");
        }
        pom = pom.normalize();

        System.out.println("\nDetermining project name from POM file in directory...");
        String projectName = ShortcutHelper.getProjectNameFromPom(pom);
        System.out.println("Found project name: " + projectName);

        return projectName;
    }

    /**
     * Sorts and prints every entry of the list in a new row with indentation.
     * 
     * @param list List to be pretty-printed
     */
    private static void prettyPrintList(List<String> list) {
        java.util.Collections.sort(list);
        for (String project : list) {
            System.out.println("    " + project);
        }
    }

    /**
     * Builds the projects with the names specified in the list.
     * 
     * @param projectNames List of projects to be built
     * @return Return code from Maven
     * @throws IllegalArgumentException If one of the project names is invalid.
     * @throws InterruptedException If the Maven execution was interrupted.
     * @throws IOException If I/O error occurs.
     */
    private int buildProjects(List<String> projectNames)
        throws IllegalArgumentException, IOException, InterruptedException {

        Path root = Misc.getCheckoutRoot(Paths.get(".")).toAbsolutePath();

        // get the relative project paths which are required by Maven
        List<String> relativePaths = ShortcutHelper.getRelativeProjectPaths(projectNames, root);

        // get the options for the Maven command
        List<String> options = getMavenOptions(relativePaths);

        System.out.println("");
        return Misc.executeMvn(options.stream().toArray(String[]::new), root);
    }

    /**
     * Returns the Options to be passed to Maven to build the specified
     * projects.
     * 
     * @param relativeProjectPaths List of projects to be built
     * @return Maven options
     */
    private List<String> getMavenOptions(List<String> relativeProjectPaths) {
        List<String> options = new ArrayList<>();
        options.add("-pl");
        options.add(toMavenProjectParameter(relativeProjectPaths));
        options.addAll(self.getUnknownOptions());
        options.addAll(parameters);
        return options;
    }

    /**
     * Transforms the list to a valid Maven -pl parameter
     * 
     * @param relativeProjectPaths list of projects
     * @return valid Maven parameter
     */
    private String toMavenProjectParameter(List<String> relativeProjectPaths) {
        StringBuilder result = new StringBuilder();
        result.append(relativeProjectPaths.get(0));
        for (int i = 1; i < relativeProjectPaths.size(); i++) {
            result.append("," + relativeProjectPaths.get(i));
        }
        return result.toString();
    }

}
