import hudson.model.queue.QueueTaskFuture
import jenkins.util.*;
import jenkins.model.*;
import hudson.model.*;

import hudson.model.StringParameterValue
import hudson.AbortException;
import hudson.console.HyperlinkNote
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.concurrent.CancellationException;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
@Grab('fr.cnp.jenkins:groovy_script:1.0.0-SNAPSHOT')
import fr.cnp.jenkins.groovy_script.BuildTools_V10 ;


def buildTools = new BuildTools_V10 (build);
def env = build.getEnvironment();
def paramValues = []
def binNames = []
if ( env['APPNAMES'] != null){
    paramValues =  Eval.me(env['APPNAMES']);// ICI on va retrouver le nom des applications dans le cadre de CNPNET par exemple
    binNames =Eval.me(env['BIN_NAME']); // ICI on va retrouver les classifiers des diff�rentes applications dans le cadre de CNPNET par exemple
    println "paramValues  ${paramValues} "
}else{
    paramValues =[env["TAG_PREFIX"].toLowerCase()]
    binNames = ["bin"]
    if ( env["BIN_NAME"] != null){
        binNames =  [env["BIN_NAME"]]
    }
    println "paramValues  ${paramValues} "
}
buildTools.setStringBuildParam( "APPNAMES","${paramValues}");
buildTools.setStringBuildParam( "BIN_NAME","${binNames}");

def parmValues_count = paramValues.size()
def workspace = build.getModuleRoot().absolutize().toString();
println " launching var  ${workspace}/"+env["CHECKOUT_DIR"] +"/ "+env["CHECK_REPORT_DIR"] ;
boolean HAS_ETL = false
for (i in 0..parmValues_count-1)
{
    println "######## Application "+paramValues [i]
    HAS_ETL = false
    if (env["${paramValues[i]}_ETL"] != null ){
        HAS_ETL = env["${paramValues[i]}_ETL"]
    }
    sandbox = false
    if (env["XLD_SandBox"] != null ){
        sandbox  = env["XLD_SandBox"]
    }

    // Read project params
    def project = new XmlSlurper().parse(new File(workspace + "/" + env["POM_DIR"] + "/livrables/pom.xml"));
    def groupId = project.groupId.toString();
    def artifactId = project.artifactId.toString();
    def version = project.version.toString();
    def trigramme = project.artifactId.toString().split('-')[0]

    if ("" == groupId) {
        groupId = project.parent.groupId.toString();
    }
    if ("" == version) {
        version = project.parent.version.toString();
    }


    // Start another job
    WorkflowJob job = (WorkflowJob)Hudson.get().getJob('build-dar-pipeline')
    WorkflowRun anotherBuild
    try {
        def params = [
                new StringParameterValue("GROUP_ID", groupId),
                new StringParameterValue("ARTIFACT_ID", artifactId),
                new StringParameterValue("VERSION", version),
                new StringParameterValue('BIN_NAME',binNames[i]),
                new StringParameterValue('OPTION_ETL',  env["OPTION_ETL"]  ),
                new StringParameterValue('APP_NAME', paramValues[i]),
                new BooleanParameterValue("HAS_ETL",  HAS_ETL  ),
                new BooleanParameterValue('CTRLX',  sandbox  )

        ]
        //Declenchement du Job
        QueueTaskFuture<WorkflowRun> future = job.scheduleBuild2(0, new ParametersAction(params))
        println "Waiting for the completion of " + HyperlinkNote.encodeTo('/' + job.url, job.fullDisplayName)
        anotherBuild = future.get()
    } catch (CancellationException x) { //Si canceled
        throw new AbortException("${job.fullDisplayName} aborted.")
    }

    fils_result =  anotherBuild.buildVariableResolver.resolve("${paramValues[i]}")
    println HyperlinkNote.encodeTo('/' + anotherBuild.url, anotherBuild.fullDisplayName) + " completed. Result was " + anotherBuild.result

    def pipelineBuildSelector = new SpecificBuildSelector(anotherBuild.getBuildStatusUrl())
    // Check that it succeeded
    build.result = anotherBuild.result
    if (anotherBuild.result != Result.SUCCESS && anotherBuild.result != Result.UNSTABLE) {
        // We abort this build right here and now.
        throw new AbortException("${anotherBuild.fullDisplayName} failed.")
    }

    buildTools.setStringBuildParam( "${paramValues[i]}_Tec", "ARTIFACT_CLASSIFIER:${binNames[i]},${fils_result},ETL:${ HAS_ETL}");
    println "variable ${paramValues[i]} " + fils_result

}
