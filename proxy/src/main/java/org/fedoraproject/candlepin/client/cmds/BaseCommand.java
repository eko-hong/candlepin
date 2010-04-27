/**
 * Copyright (c) 2009 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.fedoraproject.candlepin.client.cmds;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.fedoraproject.candlepin.client.CandlepinConsumerClient;

/**
 * BaseCommand
 */
public abstract class BaseCommand {

    protected Options options = null;
    protected CandlepinConsumerClient client;

    public BaseCommand() {
        options = getOptions();
        client = new CandlepinConsumerClient("https://localhost:8443/candlepin");
    }

    public abstract String getName();

    public abstract String getDescription();

    public void execute(CommandLine cmdLine) {

    }

    public Options getOptions() {
        Options opts = new Options();
        opts.addOption("h", "help", false, "Module Help");
        return opts;
    }

    public CommandLine getCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);
        return cmd;
    }

    public void generateHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(String.format("CLIMain %s [options]", getName()),
            options);
    }

    public CandlepinConsumerClient getClient() {
        return client;
    }
}
