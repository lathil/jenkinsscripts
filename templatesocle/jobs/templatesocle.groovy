

freeStyleJob("${PROJECT_NAME}") {
    description("TemplateSeedJob2017")

    properties {
        promotions {
            promotion {
                name ('Deploy_ to_DEV')
                icon ('Green star')
                conditions {
                    manual(''){
                        parameters {
                            booleanParam('PROVISIONNING', true, "Provisionning infrastructure")
                        }
                    }
                }
                actions {

                    downstreamParameterized {
                        trigger('xld-cnp-deploy-v10',{
                            block {
                                buildStepFailure('FAILURE')
                                failure('FAILURE')
                                unstable('UNSTABLE')
                            }
                            parameters {
                                currentBuild()
                                predefinedProp('ENV', 'developpement')

                            }
                        })
                    }

                }
            }

            promotion {
                name ('Deploy_to_VFG')
                icon ('Blue star')
                conditions {
                    manual('') {
                        parameters {
                            booleanParam('PROVISIONNING', true, "Provisionning infrastructure")
                        }
                    }
                }

                actions {

                    downstreamParameterized {
                        trigger('xld-cnp-deploy-v10',{
                            block {
                                buildStepFailure('FAILURE')
                                failure('FAILURE')
                                unstable('UNSTABLE')
                            }
                            parameters {
                                currentBuild()
                                predefinedProp('ENV', 'vfg')
                            }
                        })
                    }

                }
            }
        }
    }

    scm {
        git {
            remote {
                branch('*/' + "${GIT_BRANCH}")
                url("${GIT_URL}")
            }
            extensions {
                localBranch("${GIT_BRANCH}")
            }
        }
    }

    logRotator {
        daysToKeep(30)
        numToKeep(30)
    }

    environmentVariables {

        keepBuildVariables(true)

        env('TAG_PREFIX',"${TRIGRAM}")
        env('MAVEN_ARGS','-B -e -U clean deploy')
        env('CHECKOUT_DIR','.')
        env('POM_DIR','.')
        env('CHECK_REPORT_DIR','target/report')
        env('OPTION_ETL','')
        env('#[projet]_ETL','true')
        env('BIN_NAME','[\"bin\"]')
        env('APPNAMES','[\"' + "${TRIGRAM}" + '\"]')
    }

    jdk('JAVA8-64')



    wrappers {

        maskPasswords()

        release {

            releaseVersionTemplate('Release${VERSION_OFFICIELLE}')
            overrideBuildParameters(true)
            parameters {
                stringParam('VERSION_OFFICIELLE',"", "Version officiel doit être au format  m.n ou m.n.c \n" +
                        "m : version majeure (commence à 1)\n" +
                        "n : version mineure (commence à 0)\n" +
                        "c : correction (commence à 1)")
                stringParam('VERSION_DEVELOPPEMENT', "-SNAPSHOT")
            }



            preBuildSteps {
                environmentVariables {
                    env('MAVEN_ARGS','-B -Dresume=false release:prepare release:perform  -Dtag=${TAG_PREFIX}${VERSION_OFFICIELLE}  -DdevelopmentVersion=${VERSION_DEVELOPPEMENT} -DreleaseVersion=${VERSION_OFFICIELLE}')
                    env('CHECKOUT_DIR','${POM_DIR}/target/checkout/')
                }
            }

            postSuccessfulBuildSteps {
                shell('/soft/usinededev/ansible-deploy/current/bin/ref_event.ksh ${TAG_PREFIX} ${VERSION_OFFICIELLE} "udd" "release" "jenkins" "0" "&key1=techno&value1=java&key2=version%20alm&value2=v10"')
            }

            postFailedBuildSteps {
                shell('/soft/usinededev/ansible-deploy/current/bin/ref_event.ksh ${TAG_PREFIX} ${VERSION_OFFICIELLE} "udd" "release" "jenkins" "1" "&key1=techno&value1=java&key2=version%20alm&value2=v10"')
            }
        }
    }


    steps {
        maven(){
            goals('${MAVEN_ARGS}')
            rootPOM('${POM_DIR}/pom.xml')
            injectBuildVariables(true)
            mavenInstallation('Maven3.3.9')
        }

        systemGroovyCommand(readFileFromWorkspace('src/main/groovy/launchDarBuilder.groovy')){
            classpath('file:///soft/usinededev/groovy-postbuild/current-support/grv-v10.jar')
        }
    }


    publishers {

        groovyPostBuild{
            script(readFileFromWorkspace('src/main/groovy/postBuildScript.groovy'))
            behavior(Behavior.MarkFailed)
            classpath('file:///soft/usinededev/groovy-postbuild/current-support/grv-v10.jar')
        }

        publishHtml {
            report('${CHECKOUT_DIR}/${CHECK_REPORT_DIR}',{
                reportFiles('validation-arborescence-report.html')
                reportTitles('Validation Report')
                allowMissing(true)
                alwaysLinkToLastBuild(true)
                keepAll(true)
            })
        }

        sonar(){
            installationName('sonar')
            jdk('JAVA8-64')
            mavenInstallation('Maven3.3.9')
            additionalProperties('-Dsonar.branch=${JOB_NAME}\n' +
                    '-Dsonar.java.coveragePlugin=jacoco\n'+
                    '-Dsonar.jacoco.reportPath=target/jacoco.exec\n' +
                    '-Dsonar.jacoco.itReportPath=target/jacoco-it.exec\n' +
                    '-Dsonar.surefire.reportsPath=target/surefire-reports')
        }

        postBuildScripts {

            steps {
                shell('#!/bin/bash\n' +
                        '# permet de rollbacker en cas de problème de release\n' +
                        'if [ -f $PWD/pom.xml.releaseBackup ]\n' +
                        'then\n' +
                        '  echo \"rollback build failed\"\n' +
                        '  mvn release:rollback\n' +
                        '  mvn release:clean\n' +
                        'fi')
            }
        }

        extendedEmail {
            recipientList('$DEFAULT_RECIPIENTS')
            replyToList('$DEFAULT_REPLYTO')
            contentType('text/html')
            defaultSubject('$DEFAULT_SUBJECT')
            defaultContent('$DEFAULT_CONTENT')
            attachBuildLog(false)
            preSendScript('$DEFAULT_PRESEND_SCRIPT')
            triggers {
                failure {
                    sendTo {developers()}
                }
                replyToList('$PROJECT_DEFAULT_REPLYTO')
                contentType('default')
                defaultSubject('$PROJECT_DEFAULT_SUBJECT')
                defaultContent('$PROJECT_DEFAULT_CONTENT')
                attachBuildLog(false)
            }
        }
    }
}
