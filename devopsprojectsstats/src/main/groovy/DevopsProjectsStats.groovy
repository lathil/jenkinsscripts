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

    def trigramSevopsProcessMin = [:]
    def trigramSevopsProcessMax = [:]
    def versionRatio = [:]

    jobDevopsProcess.each{ k, v ->
        def count = jobDevopsVersionCount.getOrDefault(v, 0)
        jobDevopsVersionCount.put(v, count + 1)

        def trigramm = k.toString().substring(0,3)
        def sversion = v.toString().replaceAll("[^0-9.]", "")

        if( sversion ) {

            def version = sversion as float
            if (trigramSevopsProcessMin.containsKey(trigramm)) {
                if (version < trigramSevopsProcessMin.get(trigramm).version) {
                    trigramSevopsProcessMin.put(trigramm, [version : version, job: k])
                }
            } else {
                trigramSevopsProcessMin.put(trigramm, [version : version, job: k])
            }

            if (trigramSevopsProcessMax.containsKey(trigramm)) {
                if (version > trigramSevopsProcessMax.get(trigramm).version) {
                    trigramSevopsProcessMax.put(trigramm, [version : version, job: k])
                }
            } else {
                trigramSevopsProcessMax.put(trigramm, [version : version, job: k])
            }


        }
    }

    trigramSevopsProcessMax.each {k, v ->
        if (versionRatio.containsKey(v.version)) {
            versionRatio.put(v.version, versionRatio.get(v.version) + 1 as int)
        } else {
            versionRatio.put(v.version, 1 as int)
        }
    }

    println("Number of jobs found: " + jobDevopsProcess.size())
    println("")
    println("Number of trigram found: " + trigramSevopsProcessMax.size())
    println("")
    versionRatio.sort().each {k,v -> println "version: ${k} - nb trigrams in this version: ${(v / trigramSevopsProcessMax.size()) * 100} %  - ${v} - ${trigramSevopsProcessMax.size()}"}
    println("")
    trigramSevopsProcessMax.each {k, v -> println "trigram: ${k} - max process version: ${v.version} - job: ${v.job}"}
    println("")
    trigramSevopsProcessMin.each {k, v -> println "trigram: ${k} - min process version: ${v.version} - job: ${v.job}"}
    println("")
    jobDevopsVersionCount.each { k, v -> println "devops version: ${k} - count job: ${v}"}
    println("")
    jobDevopsProcess.each{ k, v -> println "job: ${k} - devops version: ${v}" }


    println("")
    println("Job a migrer:")
    trigramSevopsProcessMax.find { k, v ->
        def excludedTrigrams = ['ARF', 'BOS', 'DAT', 'DCP', 'DDO', 'DVO', 'ECO', 'EOD', 'GPV', 'MAS', 'ODD', 'PLI', 'PSA', 'RAI', 'SAD', 'SDT', 'SIG', 'SIL', 'TES', 'TSC', 'UWX', 'XMX' ]
        if( !excludedTrigrams.contains(k) && v.version < 12 && v.version > 1){

            FreeStyleProject jobToMigrate = Jenkins.get().getItem(v.job) as FreeStyleProject
            if( jobToMigrate.scm != null && (jobToMigrate.scm instanceof  GitSCM)) {
                GitSCM scm = jobToMigrate.scm as GitSCM
                def barebranchname = scm.branches[0].name
                if( barebranchname.startsWith("*/")){
                    barebranchname = barebranchname.substring('*/'.length())
                }
                println("Job: ${v.job} git repos: ${scm.userRemoteConfigs[0].url} branch: barebranchname ")

                def migrationjobname = "testmigrationfrom_${v.version}_${v.job}"
                FreeStyleProject seedJob = Jenkins.get().getItem('devops-seed-job-v12')
                def params = [
                        new StringParameterValue('GIT_URL', scm.userRemoteConfigs[0].url),
                        new StringParameterValue('GIT_BRANCH', barebranchname ),
                        new StringParameterValue('PROJECT_NAME', migrationjobname),
                        new StringParameterValue('TRIGRAM', k ),
                        new StringParameterValue('APPNAMES',  k.toLowerCase()  ),
                ]
                //Declenchement du Job
                println("Migration job name : " + migrationjobname)
                def future = seedJob.scheduleBuild2(0, null, new ParametersAction(params))
                result = future.get()

            } else {
                println("Job ${v.job} got not git scm")
            }
        }
    }
}

devopsProjectsStats();
