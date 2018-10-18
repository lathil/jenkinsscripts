import hudson.plugins.ansicolor.*
import jenkins.model.*
import hudson.model.*
import hudson.util.*
import hudson.tasks.*


def addAnsiColorToAllJobs(){

    // init map name
    def colorMapName = "xterm"

    // check for all job of type FreeStyle
    def jobs = Jenkins.getInstance().getAllItems(FreeStyleProject)
    for( job in jobs) {

        if( ! job.isDisabled() && job.name.contains('xld-cnp-deploy-v')) {
            DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrappers = job.getBuildWrappersList()

            boolean hasBuildWrapper = false

            for (BuildWrapper buildWrapper : buildWrappers) {
                if (buildWrapper instanceof AnsiColorBuildWrapper) {
                    hasBuildWrapper = true
                    break
                }
            }

            if (!hasBuildWrapper) {

                def newBuildWrappers = new DescribableList<BuildWrapper, Descriptor<BuildWrapper>>()
                newBuildWrappers.add(new AnsiColorBuildWrapper(colorMapName))

                newBuildWrappers.addAll(job.getBuildWrappersList())
                job.getBuildWrappersList().clear()
                job.getBuildWrappersList().addAll(newBuildWrappers)

                job.save()

                println('AnsiColor added to: ' + job.name)
            } else {
                println('AnsiColor already configured for: ' + job.name)
            }
        }
    }
}

addAnsiColorToAllJobs();