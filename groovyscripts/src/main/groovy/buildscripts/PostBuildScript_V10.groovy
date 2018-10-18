package buildscripts

import jenkins.util.*;
import jenkins.model.*;
import hudson.model.StringParameterValue
import hudson.model.BooleanParameterValue
import hudson.model.ParametersAction

public class PostBuildScript_V10 {

    def manager;

    public PostBuildScript(manager){
        this.manager = manager;
    }

    def run() {
        if ("SUCCESS".equals(manager.getResult())) {

            def currentBuild = manager.build;
            def workspace = currentBuild.getModuleRoot().absolutize().toString();

            def project = new XmlSlurper().parse(new File("$workspace/pom.xml"));
            def groupId = project.groupId.toString();
            def artifactId = project.artifactId.toString();
            def baseVersion;
            def version;
            def env = currentBuild.getEnvironment();

            String isReleaseVersion = env['VERSION_OFFICIELLE'];

            if (isReleaseVersion != null) {
                baseVersion = env['VERSION_OFFICIELLE'];
                version = env['VERSION_OFFICIELLE'];
            } else {
                baseVersion = project.version.toString();
                def metadataFile = new File("$workspace/livrables/target/nexus.xml");
                if (metadataFile.exists()) {
                    def nexus = new XmlSlurper().parse(new File("$workspace/livrables/target/nexus.xml"));
                    version = nexus.data.version.toString();
                } else {
                    addWarnBadge("No Nexus metadata file found !");
                    version = baseVersion;
                }
            }

            manager.listener.logger.println "Affectation des variables";

            def hasJee = new File("$workspace/jee").isDirectory();
            def hasBase = new File("$workspace/base").isDirectory();
            def hasBatch = new File("$workspace/batch").isDirectory();
            def trigramme = project.artifactId.toString().split('-')[0];

            def groupIdPV = new StringParameterValue("GROUP_ID", groupId);
            def artifactIdPV = new StringParameterValue("ARTIFACT_ID", artifactId);
            def baseVersionPV = new StringParameterValue("BASE_VERSION", baseVersion);
            def versionPV = new StringParameterValue("VERSION", version);
            def hasJeePV = new BooleanParameterValue("HAS_JEE", hasJee);
            def hasBasePV = new BooleanParameterValue("HAS_BASE", hasBase);
            def hasBatchPV = new BooleanParameterValue("HAS_BATCH", hasBatch);
            def trigrammePV = new StringParameterValue("TRIGRAMME", trigramme);

            def groupIdPA = new ParametersAction(groupIdPV);
            def artifactIdPA = new ParametersAction(artifactIdPV);
            def versionPA = new ParametersAction(versionPV);
            def baseVersionPA = new ParametersAction(baseVersionPV);

            def hasJeePA = new ParametersAction(hasJeePV);
            def hasBasePA = new ParametersAction(hasBasePV);
            def hasBatchPA = new ParametersAction(hasBatchPV);
            def trigrammePA = new ParametersAction(trigrammePV);

            currentBuild.addAction(groupIdPA);
            currentBuild.addAction(artifactIdPA);
            currentBuild.addAction(versionPA);
            currentBuild.addAction(baseVersionPA);
            currentBuild.addAction(hasJeePA);
            currentBuild.addAction(hasBasePA);
            currentBuild.addAction(hasBatchPA);
            currentBuild.addAction(trigrammePA);

            manager.listener.logger.println "TRIGRAMME: " + trigramme;
            manager.addInfoBadge("BASE_VERSION: " + baseVersion);
            manager.addInfoBadge("VERSION: " + version);
            manager.listener.logger.println "HAS_JEE: " + hasJee;
            manager.listener.logger.println "HAS_BASE: " + hasBase;
            manager.listener.logger.println "HAS_BATCH: " + hasBatch;

            def summary1 = manager.createSummary("notepad.gif");
            summary1.appendText("<b>TRIGRAMME:</b> " + trigramme + "<br/>\r\n", false);
            summary1.appendText("<b>BASE_VERSION:</b> " + baseVersion + "<br/>\r\n", false);
            summary1.appendText("<b>VERSION:</b> " + version + "<br/>\r\n", false);

            def summary2 = manager.createSummary("package.gif");
            //pattern = ~/Uploaded: (.*) \\(.*\\)/;
            def pattern = ~/.* Uploaded\: (https?:.*-livrables-.*-bin.tar.gz).*/;
            manager.build.logFile.eachLine { line ->
                def matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    summary2.appendText("<a href=\"" + matcher.group(1) + "\">" + matcher.group(1) + "</a><br/>\r\n", false);
                }
            }
        }
    }
}

