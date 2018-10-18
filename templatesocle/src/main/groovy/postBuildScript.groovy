@Grab('fr.cnp.jenkins:groovy_script:1.0.0-SNAPSHOT')
import fr.cnp.jenkins.groovy_script.PostBuildScript_V10;
import groovy.io.*
import jenkins.util.*;
import jenkins.model.*;
import hudson.model.*;


def env = manager.build.getEnvironment();
def PostBuildScript_V10= new PostBuildScript_V10(manager);

PostBuildScript_V10.run(env["POM_DIR"]);

