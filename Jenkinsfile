def CODE_CHANGE = false
def CURRENT_VERSION=""
def ROLLBACK_VERSION=""
pipeline {
  agent any

  environment {
    VERSION_FILE = 'version.txt'         // or changelog.md, package.json, etc.
    STORED_VERSION = '.last_version.txt' // stored version from last built
    ROLLBACK_VERSION_FILE = '.rollback_version.txt' // File that stores the version to rollback to
  }

   parameters {
        choice(name: 'ENVIRONMENT', choices: ['onsite','dev'], description: 'Select environment to deploy to')
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
         def lastVersion = fileExists(env.STORED_VERSION) ? readFile(env.STORED_VERSION).trim() : ''
         ROLLBACK_VERSION = lastVersion
          echo "Current version: ${CURRENT_VERSION}"
          echo "Last known version: ${lastVersion}"

          if (CURRENT_VERSION == lastVersion) {
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
    stage('Run ISO Update in Development') {
        when {
            expression {
            return CODE_CHANGE  && params.ENVIRONMENT == 'dev'
            }
        }
        steps {
                //TODO Need to add exception handling here
                script {
                    performISOUpdate('Development',CURRENT_VERSION)
            }
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
        steps {
            writeFile file: env.STORED_VERSION, text: CURRENT_VERSION
            writeFile file: env.ROLLBACK_VERSION_FILE, text: ROLLBACK_VERSION
            script { 
              cleanupOldISOFiles(CURRENT_VERSION,ROLLBACK_VERSION)
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

def performISOUpdate(stageName,currentVersion){

  echo "Downloading the ISO file from GCP bucket"
  def cleanedVersion = currentVersion.replace('.', '')
  echo "CLEANED VERSION: ${cleanedVersion}"
  def isoFileName = "debian-custom-${cleanedVersion}.iso"
  echo "ISO FILENAME: ${isoFileName}"
  def exeFileName = "HauApp${cleanedVersion}"
  echo "EXE FILENAME: ${exeFileName}"
  if (!fileExists(isoFileName)){
      echo "FILE: ${isoFileName} does not exist in the control node."
      withCredentials([file(credentialsId: 'jenkins-service-account-key', variable: 'GCP_KEY')]) {
          sh """
              gcloud auth activate-service-account --key-file="\$GCP_KEY"
              gcloud storage cp gs://hiper-global-artifacts/${isoFileName} ${isoFileName}
          """
        }

  }
  else{
    echo "FILE: ${isoFileName} exists in the control node. Not downloading the file"
  }
              
  echo "Updating ISO for FOG servers"
  sshagent(['ansible-ssh-key']) {
    if(params.ENVIRONMENT == "dev"){

        sh """
            ANSIBLE_HOST_KEY_CHECKING=False \
            ansible-playbook fog-iso-deploy.yaml \
            -i inventory.ini \
            --extra-vars "artifact_name=${isoFileName}" \
            --extra-vars "target_hosts=fog" \
        """
    }
    else{
      if (stageName == "stage"){

          sh """
            ansible-playbook fog-iso-deploy.yaml \
            -i hiperglobal-inventory.ini \
            --extra-vars "artifact_name=${isoFileName}" \
            --extra-vars "target_hosts=fog,staging" \
        """
      }
      else{
          sh """
            ansible-playbook fog-iso-deploy.yaml \
            -i hiperglobal-inventory.ini \
            --extra-vars "artifact_name=${isoFileName}" \
            --extra-vars "target_hosts=fog,live" \
        """
      }
      
    }
  } 

}

def cleanupOldISOFiles(currentVersion,rollBackVersion){
  // List all ISO files
  def cleanedCurrentVersion = currentVersion.replace('.', '')
  def cleanedRollBackVersion = rollBackVersion.replace('.','')
  //TODO Handle scenario where there are not existing ISO files
  def isoFiles = sh(script: "ls -1t *.iso", returnStdout: true).trim().split('\n')
  def latestISOFilename = "debian-custom-${cleanedCurrentVersion}.iso"
  def rollbackISOFilename = "debian-custom-${cleanedRollBackVersion}.iso"
  echo "LATEST ISO FILE NAME: ${latestISOFilename}"
  echo "ROLLBACK ISO FILE NAME: ${rollbackISOFilename}"
  if (isoFiles.size() > 0) {
      // Loop through all except the latest
      for (int i = 0; i < isoFiles.size(); i++) {
          if (isoFiles[i] != latestISOFilename && isoFiles[i] != rollbackISOFilename){ 
            echo "Deleting old ISO: ${isoFiles[i]}"
            sh "rm -f '${isoFiles[i]}'"
          }
      }
  } else {
            echo "No ISO files found."
          }
}
