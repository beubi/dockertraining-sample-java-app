#! groovy

import groovy.transform.Field

def branches = []

@Field
def pushedBranches = []

@Field
def develop = 'develop'

@Field
def release = 'release-dummy/'

@Field
def bugfix = 'bugfix-dummy/'

@Field
def hotfix = 'hotfix-dummy/'

def sonarAnalysis(pathToPom = '', profile = '', analyser = '') {
    if (pathToPom != '') {
        pathToPom = " -f ${pathToPom}"
    }

    if (profile != '') {
        profile = " -P ${profile}"
    }

    analysers = ['maven', 'angular', 'msbuild']
    if (!analysers.contains(analyser)) {
        analyser = 'maven'
    }

    echo "Analysing (${analyser}) branch ${BRANCH_NAME} ..."
    if (analyser == 'maven') {
        withSonarQubeEnv('SonarQube') {
            bat "mvn${pathToPom}${profile} -U -DskipTests sonar:sonar -Dsonar.exclusions=**/ear/* -Dsonar.host.url=http://naptfe:9000 -Dsonar.branch=${BRANCH_NAME}"
        }
    } else if (analyser == 'angular') {
        withSonarQubeEnv('SonarQube') {
            bat "mvn${pathToPom}${profile} -U -DskipTests sonar:sonar -Dsonar.sources=client,generator -Dsonar.host.url=http://naptfe:9000 -Dsonar.branch=${BRANCH_NAME}"
        }
    } else if (analyser == 'msbuild') {
        bat "${sqScannerMsBuildHome}\\SonarQube.Scanner.MSBuild.exe begin /k:nBAM-Webapp /d:sonar.host.url=http://naptfe:9000 /d:sonar.branch=${BRANCH_NAME}"
        bat "MSBuild.exe /t:Rebuild"
        bat "${sqScannerMsBuildHome}\\SonarQube.Scanner.MSBuild.exe end"
    }
}

def pushChanges(branches) {
    try {
        echo "Branches to be pushed -> ${branches}"

        branches.each { branch ->
            echo "Push branch ${branch} ..."
            bat "git checkout ${branch}"
            bat "git push origin ${branch}"
            pushedBranches.add(branch)
        }
    } catch (e) {
        reverseChanges(pushedBranches)
        throw e
    }
}

def reverseChanges(pushedBranches) {
    echo "Branches to reverse -> ${pushedBranches}"

    pushedBranches.each { branch ->
        echo "Deleting branch ${branch}"
        bat "git push origin --delete ${branch}"
    }

    pushedBranches = []
}

def latestBuild() {
    git([branches: [
            [name: 'refs/tags/*:refs/tags/*']],
        url: 'https://bitbucket.sa.sibs.local:7443/scm/rt/dummy.git',
        credentialsId: '21073d68-6e07-4288-8d3f-b00820560743'
    ])

    releaseBranches = bat(script: "git branch --remote --list *${release}${TAG_NAME}-*", returnStdout: true).trim()
    previousBuilds = releaseBranches.split("  ")

    latestBuild = -1
    for (int i = 1; i < previousBuilds.size(); i++) {
        buildNr = previousBuilds[i].substring(previousBuilds[i].lastIndexOf('-') + 1)

        if (buildNr.toInteger() > latestBuild.toInteger()) {
            latestBuild = buildNr.toInteger()
        }
    }

    return latestBuild
}

def createRelease() {
    // Alternatively use BUILD_NUMBER, though if the build fails for some reason the number will still be incremented
    // leading to skipped build numbers in the release branches
    build = latestBuild() + 1
    version = "${TAG_NAME}-${build}"
    release = release + version
    bugfix = bugfix + version

    echo "Create release branch ..."
    bat "git checkout -b ${release}"

    try {
        echo "Set release version"
        bat "mvn versions:set -DnewVersion=${version}"
    } catch (e) {
        echo "Failed setting release version"
        throw e
    }

    try {
        echo "Update dependency versions according to specified range"
        bat "mvn versions:resolve-ranges"
    } catch (e) {
        echo "Failed updating dependency versions"
        throw e
    }

    echo "Commit changes in release branch ..."
    bat 'git commit -a -m "New release candidate"'

    echo "Create bugfix branch ..."
    bat "git checkout -b ${bugfix} ${release}"

    bat "git checkout ${release}"
}

pipeline {
    agent {
        node {
            label 'Java'
        }
    }

    tools {
        maven 'Apache Maven 3.3.9'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    stages {
        stage('Pull Request:') {
            when {
                changeRequest()
            }
            stages {
                stage('Unit Tests') {
                    steps {
                        script {
                            try {
                                echo "Testing ..."
                                echo "Run mvn clean test here ..."
                            } catch (e) {
                                echo "Unit tests failed"
                                throw e
                            }
                        }
                    }
                }
                stage('Build') {
                    steps {
                        script {
                            try {
                                echo "Building ${BRANCH_NAME} ..."
                                bat "mvn clean install"
                            } catch (e) {
                                echo "Build failed"
                                throw e
                            }
                        }
                    }
                }
                stage('Static Analysis') {
                    steps {
                        script {
                            sonarAnalysis()
                        }
                    }
                }
            }
        }
        stage('Create release:') {
            when {
                tag '*.*'
            }
            stages {
                stage('Build develop') {
                    steps {
                        script {
                            try {
                                echo "Building develop ..."
                                bat "mvn clean install -U"
                            } catch (e) {
                                echo "Build failed"
                                throw e
                            }
                        }
                    }
                }
                stage('Create release branch') {
                    steps {
                        script {
                            createRelease()
                        }
                    }
                }
                stage('Integration Tests') {
                    steps {
                        script {
                            try {
                                echo "Testing ..."
                                echo "Run mvn clean verify here"
                            } catch (e) {
                                throw e
                            }
                        }
                    }
                }
                stage('Push changes') {
                    steps {
                        script {
                            branches = [release, bugfix]
                            pushChanges(branches)
                        }
                    }
                }
            }
        }
        stage('Release:') {
            when {
                branch 'release*'
            }
            stages {
                stage('Build release') {
                    steps {
                        script {
                            try {
                                echo "Building release branch ..."
                                bat "mvn clean install -U"
                            } catch (e) {
                                throw e
                            }
                        }
                    }
                }
                stage('Integration Tests') {
                    steps {
                        script {
                            try {
                                echo "Testing ..."
                                echo "Run mvn clean verify here"
                            } catch (e) {
                                throw e
                            }
                        }
                    }
                }
                stage('Deploy to Nexus') {
                    steps {
                        script {
                            try {
                                echo "Deploying to Nexus ..."
                                bat "mvn deploy:deploy"
                            } catch (e) {
                                echo "Deploy to Nexus failed"
                                throw e
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Cleaning Workspace after build...'
            deleteDir()
        }
    }
}