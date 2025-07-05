@Library('hiper-global-shared-library')
def CODE_CHANGE = false
def CURRENT_VERSION=""
def ROLLBACK_VERSION=""
def LAST_VERSION=""
pipeline {
  agent any

  environment {
    VERSION_FILE = 'version.txt'         // or changelog.md, package.json, etc.
    STORED_VERSION = '.last_version.txt' // stored version from last built
    ROLLBACK_VERSION_FILE = '.rollback_version.txt' // File that stores the version to rollback to
  }

   parameters {
        booleanParam(name: 'ROLLBACK', defaultValue: false, description: 'Check this if you want to rollback ISO & EXE deployment')
    }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Read Version') {
      
      steps {
        script {
         CURRENT_VERSION = readFile(env.VERSION_FILE).trim()
         LAST_VERSION = fileExists(env.STORED_VERSION) ? readFile(env.STORED_VERSION).trim() : ''
         ROLLBACK_VERSION = fileExists(env.ROLLBACK_VERSION_FILE) ? readFile(env.ROLLBACK_VERSION_FILE).trim() : ''
         echo "Current version: ${CURRENT_VERSION}"
         echo "Last known version: ${LAST_VERSION}"
         echo "Rollback Version: ${ROLLBACK_VERSION}"
        }
      }
    }
    stage('Decide if there is a code change'){
      when {
            expression {
            return !params.ROLLBACK
            }
        }
      steps{

          script {
             if (CURRENT_VERSION == LAST_VERSION) {
            echo "No new version found. Skipping pipeline."
            currentBuild.result = 'NOT_BUILT'
            error("Version unchanged")
          } else {
            
            echo "New version detected: ${CURRENT_VERSION}"
            CODE_CHANGE = true
            
          }
          }
      }

    }
    //TODO Add a check here to run this stage when DEV flag is checked
    stage('Run ISO Update') {
        when {
            expression {
            return CODE_CHANGE
            }
        }
        steps {
                //TODO Need to add exception handling here
                script {
                    sharedUtils.performISOUpdate('Development',CURRENT_VERSION)
            }
        }
    }
    stage('Rollback ISO Update in Development'){
      when {
            expression {
            return  params.ROLLBACK
            }
        }
        steps {
                //TODO Need to add exception handling here
                script {
                    sharedUtils.performISOUpdate('Development',ROLLBACK_VERSION)
                }
                writeFile file: env.STORED_VERSION, text: ROLLBACK_VERSION
                cleanupRollbackISOFile(ROLLBACK_VERSION)

        }

    }


    // stage('Run EXE Update in staging') {
    //     when {
    //         expression {
    //         return CODE_CHANGE  
    //         }
    //     }
    //     steps {
    //             script {
    //                 echo "Downloading the EXE file from GCP bucket"
    //                 def cleanedVersion = CURRENT_VERSION.replace('.', '')
    //                 echo "CLEANED VERSION: ${cleanedVersion}"
    //                 def exeFileName = "HauApp${cleanedVersion}"
    //                 echo "EXE FILENAME: ${exeFileName}"
    //                 //TODO Uncomment after debugging
    //                 //TODO Check if you have the ISO file locally, if not download the file
    //                 // withCredentials([file(credentialsId: 'jenkins-service-account-key', variable: 'GCP_KEY')]) {
    //                 // sh """
    //                 //     gcloud auth activate-service-account --key-file="\$GCP_KEY"
    //                 //     gcloud storage cp gs://hiper_global_artifacts/${exeFileName} ${exeFileName}
    //                 // """
    //                 // }
    //                 echo "Updating EXE for UI servers"
    //                 sshagent(['ansible-ssh-key']) {
    //                 sh """
    //                     ansible-playbook fog-iso-deploy.yaml \
    //                     -i inventory.ini \
    //                     -u hau \
    //                     --extra-vars "artifact_name=${isoFileName}" \
    //                     -vvv
    //                 """
    //                 } 
    //         }
    //     }
    // }
    stage('Update version files and cleanup old ISO files'){
        when {
            expression {
            return !params.ROLLBACK
            }
        }
        steps {
            writeFile file: env.STORED_VERSION, text: CURRENT_VERSION
            writeFile file: env.ROLLBACK_VERSION_FILE, text: ROLLBACK_VERSION
            script { 
              sharedUtils.cleanupOldISOFiles(CURRENT_VERSION,ROLLBACK_VERSION)
            }
        }
    }
}

  post {
    always {
      archiveArtifacts artifacts: '.last_version.txt', fingerprint: true
      archiveArtifacts artifacts: '.rollback_version.txt', fingerprint: true
    }
  }
}