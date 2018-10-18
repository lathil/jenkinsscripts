import groovy.xml.XmlUtil

@NonCPS
def getVersionFromNexusMeta( metaContent){
    def artifactResolution = new XmlSlurper().parseText(metaContent)
    return artifactResolution.data.version.toString()
}

pipeline {
    agent any

    tools {
        maven 'Maven3.3.9'
    }

    parameters {
        string(defaultValue: '', description: '', name: 'GROUP_ID')
        string(defaultValue: '', description: '', name: 'ARTIFACT_ID')
        string(defaultValue: '', description: '', name: 'VERSION')
        string(defaultValue: '', description: '', name: 'BIN_NAME')
        string(defaultValue: '', description: '', name: 'OPTION_ETL')
        string(defaultValue: '', description: '', name: 'APP_NAME')
        booleanParam(defaultValue: false, description: '', name: 'HAS_ETL')
        booleanParam(defaultValue: false, description: '', name: 'CTRLX')


    }

    environment  {
        DARBUILDER_VERSION = '10.0.0'
        NEXUS_URL="$NEXUS_REPOSITORY_URL"
        HAS_JEE = false
        HAS_BASE = false
        HAS_BATCH = false
    }

    stages {

        stage('Recuperation artifact tgz'){

            steps {
                script {
                    TRIGRAMME = params.ARTIFACT_ID.toString().split('-')[0]
                    echo("Artifact trigramme: ${TRIGRAMME}")
                }

                fileOperations([
                    folderCreateOperation( folderPath: 'tgz'),
                    folderCreateOperation( folderPath: 'dar'),
                    folderCreateOperation( folderPath: 'report'),
                    folderCreateOperation( folderPath: 'tmp'),
                    folderCreateOperation( folderPath: 'validation-normes')
                ])


                sh "mvn dependency:get -Ddest=${WORKSPACE}/ -DgroupId=${params.GROUP_ID} -DartifactId=${params.ARTIFACT_ID} -Dversion=${params.VERSION} -Dpackaging=tar.gz -Dclassifier=${params.BIN_NAME} -Dtransitive=false > ${WORKSPACE}/result"

                sh "wget --no-check-certificate -O nexus.xml \'${env.NEXUS_URL}/service/local/artifact/maven/resolve?g=${params.GROUP_ID}&a=${params.ARTIFACT_ID}&v=${params.VERSION}&r=public&e=tar.gz&c=${params.BIN_NAME}\'"
                sh"ls -l nexus.xml"
            }

        }

        stage( 'Validation contenu tgz'){
            steps {
                fileOperations([
                        fileUnTarOperation(filePath: "${params.ARTIFACT_ID}-${params.VERSION}-${params.BIN_NAME}.tar.gz", targetLocation: 'tgz', isGZIP: true)
                ])
                dir('validation-normes') {
                    git([url: "git@pvld0000:devl_xld/validation-normes.git", branch: 'master'])
                }

                dir('validation-normes/arborescence/python') {
                    sh """
                        python 'dir2xml.py' "${WORKSPACE}/tgz" "${WORKSPACE}/tmp/dir1.xml"
                        python 'checkRules.py' 'Validation avant DarBuilder' "${WORKSPACE}/tmp/dir1.xml" "${WORKSPACE}/report/report_darbuilder.json" "${WORKSPACE}/validation-normes/arborescence/rules/preDar"
                    """
                }
            }
        }

        stage('Construction Dar'){
            steps {
                fileOperations([
                    fileCreateOperation(fileName: 'pom.xml', fileContent: '<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">\n' +
                            '\t<modelVersion>4.0.0</modelVersion>\n' +
                            '\t<groupId>xld-cnp-darbuilder</groupId>\n' +
                            '\t<artifactId>xld-cnp-darbuilder</artifactId>\n' +
                            '\t<version>1.0.0</version>\n' +
                            '</project>')
                ])

                script {
                    //def artifactResolution = new XmlSlurper().parseText(readFile('nexus.xml'))
                    NEXUS_VERSION = getVersionFromNexusMeta(readFile('nexus.xml'))

                    echo("Nexus versions : ${NEXUS_VERSION}")
                }

                sh " mvn fr.cnp.xld:dar-builder-maven-plugin:${env.DARBUILDER_VERSION}:tgz2dar -Dtgz2dar.application=${TRIGRAMME} -Dtgz2dar.version=${NEXUS_VERSION} -Dtgz2dar.tgzFile=${params.ARTIFACT_ID}-${params.VERSION}-${params.BIN_NAME}.tar.gz -Dtgz2dar.darDir=${WORKSPACE}/dar -Dtgz2dar.appName=${params.APP_NAME}"

            }
        }

        stage('Validation contenu Dar Jee'){
            when{
                expression{ return fileExists('tgz/jee')}
            }
            steps {
                echo('Dar has Jee')
                script { HAS_JEE = true}
                dir('validation-normes/arborescence/python') {
                    sh """
                    python 'dir2xml.py' "${WORKSPACE}/dar/${TRIGRAMME}-${params.APP_NAME}-jee-${NEXUS_VERSION}.dar.dir" "${WORKSPACE}/tmp/dir1.xml"
                    python 'checkRules.py' 'Validation arborescence Jee' "${WORKSPACE}/tmp/dir1.xml" "${WORKSPACE}/report/report_jee.json" "${WORKSPACE}/validation-normes/arborescence/rules/jee"
                    """
                }
            }
        }

        stage ('Validation contenu Dar Base de données'){
            when{
                expression{return fileExists('tgz/base') }
            }
            steps{
                echo('Dar has Base')
                script {HAS_BASE = true}
                dir('validation-normes/arborescence/python') {
                    sh """
                    python 'dir2xml.py' "${WORKSPACE}/tgz/base" "${WORKSPACE}/tmp/dir2.xml"
                    python 'checkRules.py' 'Validation arborescence Base' "${WORKSPACE}/tmp/dir2.xml" "${WORKSPACE}/report/report_base.json" "${WORKSPACE}/validation-normes//arborescence/rules/base"
                    #python 'verify.py' "${TRIGRAMME}" "${WORKSPACE}/tmp/dir2.xml" "${params.VERSION}" "${WORKSPACE}/tgz" ${params.CTRLX}
                    """
                }
            }
        }

        stage ('Validation Batchs') {
            when {
                expression{return fileExists('tgz/batch')}
            }
            steps {
                echo('Dar has Batch')
                script {HAS_BATCH = true}
                sh """#!/bin/bash
                    echo ". /soft/itb/tools/tools.profile; devops.sh packageCreate -t $APP_NAME -e $NEXUS_VERSION -C -f ${WORKSPACE}/report/report_itb.json.broken"
                    EXIT_CODE=\$?
                    
                    iconv -f ISO-8859-1 -t UTF8 < "${WORKSPACE}/report/report_itb.json.broken" > "${WORKSPACE}/report/report_itb.json"
                    #rm "${WORKSPACE}/report/report_itb.json.broken"
                    
                    exit \$EXIT_CODE
                    """
            }
        }

        stage('Validation Etl') {
            when {
                expression{ return params.HAS_ETL}
            }
            steps {
                sh """#!/bin/bash
                        echo "ksh -c '. /soft/itb/tools/tools.profile; devops.sh packageCreate -t $APP_NAME -e $NEXUS_VERSION -D -f ${WORKSPACE}/report/report_itb.json.broken $OPTION_ETL
                        EXIT_CODE=\$?
                        
                        iconv -f ISO-8859-1 -t UTF8 < "${WORKSPACE}/report/report_itb.json.broken" > "${WORKSPACE}/report/report_itb.json"
                        rm "${WORKSPACE}/report/report_itb.json.broken"
                        
                        exit \$EXIT_CODE
                    """
            }
        }

        stage('publish-dar') {
            steps {
                sh """
                     cd /soft/usinededev/ansible-deploy/cli-deploy-7/
                      ./cli.sh -f /soft/usinededev/ansible-deploy/cli-deploy-7/Xld_Create_Appl_dir.py ${TRIGRAMME}
                    """
                script {
                    new File("${WORKSPACE}/dar").eachFileMatch(~/package.dar/) { file ->
                        echo("Found package: " + file.toPath().toString())
                        xldPublishPackage serverCredentials: 'udd xldeploy', darPath: file.toPath().toString()
                    }
                }
            }
        }

        stage('validation-report'){
            steps {
                dir('validation-normes/arborescence/python') {
                    sh """
                        python 'generateReport.py' "${WORKSPACE}/report/report_darbuilder.json" "${WORKSPACE}/report/report_jee.json" "${WORKSPACE}/report/report_base.json" "${WORKSPACE}/report/report_itb.json" "${WORKSPACE}/report/validation-arborescence-report.html"
                   """
                }
            }
        }
    }
    post {
        failure {
            echo readFile('result')
        }
        always {
            /*
            script {
                DAR_CONTENTS = [BASE: HAS_BASE, BATCH:HAS_BATCH, JEE:HAS_JEE]
                writeJSON file: 'dar_contents.json', json: DAR_CONTENTS
            }
            */
            archiveArtifacts artifacts: "report/validation-arborescence-report.html", allowEmptyArchive:true, fingerprint: true
        }
    }
}
