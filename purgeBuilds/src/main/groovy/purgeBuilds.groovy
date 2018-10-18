import hudson.plugins.ansicolor.*
import jenkins.model.*
import hudson.model.*
import hudson.util.*
import hudson.tasks.*


def purgeBuilds(){

    List<FreeStyleProject> jobs = Jenkins.getInstance().getAllItems(FreeStyleProject)
    jobs.each { job ->
        job.getBuilds().each { build -> build.delete()
        }
    }
}

purgeBuilds()
