package org.janelia.render.client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.Serializable;

import org.janelia.alignment.json.JsonUtils;

/**
 * Base parameters for all command line tools.
 *
 * @author Eric Trautman
 */
@Parameters
public class CommandLineParameters implements Serializable {

    @Parameter(names = "--help", description = "Display this note", help = true)
    protected transient boolean help;

    private transient JCommander jCommander;

    public CommandLineParameters() {
        this.help = false;
        this.jCommander = null;
    }

    public void parse(final String[] args,
                      final Class programClass) throws IllegalArgumentException {

        jCommander = new JCommander(this);
        jCommander.setProgramName("java -cp current-ws-standalone.jar " + programClass.getName());

        boolean parseFailed = true;
        try {
            jCommander.parse(args);
            parseFailed = false;
        } catch (final Throwable t) {
            JCommander.getConsole().println("\nERROR: failed to parse command line arguments\n\n" + t.getMessage());
        }

        if (help || parseFailed) {
            JCommander.getConsole().println("");
            jCommander.usage();
            System.exit(1);
        }
    }

    /**
     * @return string representation of these parameters.
     */
    @Override
    public String toString() {
        try {
            return JsonUtils.MAPPER.writeValueAsString(this);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

}