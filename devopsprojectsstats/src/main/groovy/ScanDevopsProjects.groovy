
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig
import hudson.tasks.Publisher
import hudson.util.DescribableList
import jenkins.model.*
import hudson.model.*
import hudson.tasks.Builder
import hudson.tasks.BuildStep
import hudson.tasks.Shell

import hudson.plugins.promoted_builds.JobPropertyImpl
import hudson.plugins.promoted_builds.PromotionProcess
import hudson.plugins.parameterizedtrigger.TriggerBuilder
import hudson.plugins.groovy.SystemGroovy
import hudson.plugins.groovy.StringSystemScriptSource
import org.jenkinsci.plugins.postbuildscript.PostBuildScript
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript
import org.jenkinsci.plugins.envinject.EnvInjectJobProperty
import hudson.plugins.git.GitSCM

def devopsProjectsStats(){

    def jobDevopsProcess = [:]
    def jobDevopsVersionCount = [:]

    // check for all job of type FreeStyle
    def jobs = Jenkins.get().getAllItems(FreeStyleProject)
    for( job in jobs) {
        def found = false
        if( !job.isDisabled() && ! job.name.startsWith('Template')) {
            // check first if we have promotion process for this job, good indicator of devops job
            JobPropertyImpl jobProperty = job.getProperty(JobPropertyImpl.class)
            if (jobProperty) {
                List<PromotionProcess> processes = jobProperty.getItems()
                for (process in processes) {

                    List<BuildStep> steps = process.getBuildSteps()
                    for (step in steps) {
                        if (step instanceof TriggerBuilder) {
                            TriggerBuilder builder = (TriggerBuilder) step
                            List<BlockableBuildTriggerConfig> buildconfigs = builder.getConfigs()
                            for (config in buildconfigs) {
                                if (config.getProjects().indexOf('xld-cnp-deploy-') > -1) {
                                    //println("Job: " + job.displayName + " has promotion process: " + process.displayName + " with deployment process " + config.getProjects())
                                    def version = config.getProjects().substring(config.getProjects().indexOf('xld-cnp-deploy-') + 'xld-cnp-deploy-'.length())
                                    jobDevopsProcess.put(job.displayName, version)
                                    found = true
                                    break
                                }

                            }
                        }
                        if (found) break
                    }
                    if (found) break
                }
            } else {
                // non promotion, check if we have dar builder
                List<Builder> builders = job.getBuilders()
                for (builder in builders) {
                    if (builder instanceof SystemGroovy) {
                        StringSystemScriptSource source = ((SystemGroovy) builder).getSource()
                        SecureGroovyScript secureScript = source.getScript()
                        String script = secureScript.getScript()
                        if (script.contains('xld-cnp-darbuilder-')) {
                            int startIndex = script.indexOf('xld-cnp-darbuilder-') + 'xld-cnp-darbuilder-'.length()
                            def version = script.substring(startIndex, startIndex + 3)
                            jobDevopsProcess.put(job.displayName, version)
                            found = true
                            //println("Job: " + job.displayName + " has xld-cnp-darbuilder only process : " + version)
                            break
                        }
                    } else if ( builder instanceof TriggerBuilder){
                        TriggerBuilder triggerBuilder = (TriggerBuilder) builder
                        List<BlockableBuildTriggerConfig> buildconfigs = triggerBuilder.getConfigs()
                        for (config in buildconfigs) {
                            if (config.getProjects().indexOf('xld-cnp-darbuilder-') > -1) {
                                def version = config.getProjects().substring(config.getProjects().indexOf('xld-cnp-darbuilder-') + 'xld-cnp-darbuilder-'.length())
                                jobDevopsProcess.put(job.displayName, version)
                                found = true
                                //println("Job: " + job.displayName + " has xld-cnp-darbuilder only process : " + version)
                                break
                            }

                        }
                    } else if( builder instanceof Shell){
                        Shell shell = builder as Shell
                        if( shell.command.contains('deploy-livrables.sh')){
                            def version = 'v1'
                            jobDevopsProcess.put(job.displayName, version)
                            found = true
                            //println("Job: " + job.displayName + " has deploy-livrables.sh only process : " + version)
                            break
                        }
                    }
                }
            }

            if( !found) {
                DescribableList<Publisher,Descriptor<Publisher>> publisherList =  job.publishersList
                for (Publisher publisher : publisherList) {
                    if(publisher instanceof PostBuildScript) {
                        for( BuildStep step : publisher.buildSteps ){
                            if(step instanceof Shell){
                                Shell shell = step as Shell
                                if( shell.command.contains('deploy-livrables.sh')){
                                    def version = 'v1'
                                    jobDevopsProcess.put(job.displayName, version)
                                    found = true
                                    //println("Job: " + job.displayName + " has deploy-livrables.sh only process : " + version)
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    jobDevopsProcess.each { k, v ->
        FreeStyleProject job = Jenkins.get().getItem(k) as FreeStyleProject
        EnvInjectJobProperty properties = job.getProperty(EnvInjectJobProperty.class)
        if (properties != null) {
            def propertiesString = properties.info.propertiesContent
            if(propertiesString.contains("db")){
                println( "job: ${k} has property db: ${propertiesString}")
            }
        }
    }
}

devopsProjectsStats();
