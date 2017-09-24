package com.cflint.mvn;

import com.cflint.BugInfo;
import com.cflint.CFLint;
import com.cflint.HTMLOutput;
import com.cflint.config.CFLintChainedConfig;
import com.cflint.config.CFLintConfig;
import com.cflint.config.CFLintConfiguration;
import com.cflint.config.CFLintPluginInfo;
import com.cflint.config.ConfigUtils;
import com.cflint.tools.CFLintFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import javax.xml.transform.TransformerException;

@Mojo(name = "cflint")
public class ColdFusionLintMojo extends AbstractMojo {

    @Parameter
    private File[] sources;

    public void execute() throws MojoExecutionException {
        getLog().info("Initiating Linting Procedure");

        try {
            CFLint cflint = new CFLint(buildConfigChain());

            cflint.setVerbose(true);
            cflint.setLogError(false);
            cflint.setQuiet(false);
            cflint.setStrictIncludes(false);
            cflint.setShowProgress(false);
            cflint.setProgressUsesThread(false);
            CFLintFilter filter = CFLintFilter.createFilter(false);
            cflint.getBugs().setFilter(filter);

            for (final File scanFolder : sources) {
                cflint.scan(scanFolder.getAbsolutePath());
            }

            int count = 0;
            for (BugInfo bug : cflint.getBugs()) {
                getLog().error(bug.getMessage() + " File - " + bug.getFilename() + " Line - " + bug.getLine());
                count++;
                cflint.getStats().getCounts().add(bug.getMessageCode(), bug.getSeverity());
            }

            if (count > 0) {
                try {
                    File htmlOutFile = new File("target/reports/cflint/cflint.html");
                    htmlOutFile.getParentFile().mkdirs();
                    getLog().info("Writing HTML to " + htmlOutFile.getAbsolutePath());
                    final Writer htmlWriter = new FileWriter(htmlOutFile);
                    new HTMLOutput("plain.xsl").output(cflint.getBugs(), htmlWriter, cflint.getStats());
                } catch (final TransformerException e) {
                    throw new IOException(e);
                }

                getLog().info("Total files scanned: " + cflint.getStats().getFileCount());
                getLog().info("Total bugs identified: " + count);

                throw new MojoExecutionException("");
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to execute", e);
        }
    }

    private CFLintConfiguration buildConfigChain() {
        final CFLintPluginInfo pluginInfo = ConfigUtils.loadDefaultPluginInfo();
        CFLintConfig defaultConfig = new CFLintConfig();
        defaultConfig.setRules(pluginInfo.getRules());

        return new CFLintChainedConfig(defaultConfig);
    }

}