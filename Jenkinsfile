def CODE_CHANGE = false
def CURRENT_VERSION=""
pipeline {
  agent any

  environment {
    VERSION_FILE = 'version.txt'         // or changelog.md, package.json, etc.
    STORED_VERSION = '.last_version.txt' // stored version from last build
    TARGET_IP = "192.168.30.118"
    
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

          echo "Current version: ${CURRENT_VERSION}"
          echo "Last known version: ${lastVersion}"

          if (CURRENT_VERSION == lastVersion) {
            echo "No new version found. Skipping pipeline."
            currentBuild.result = 'NOT_BUILT'
            error("Version unchanged")
          } else {
            
            echo "New version detected: ${CURRENT_VERSION}"
            CODE_CHANGE = true
            writeFile file: env.STORED_VERSION, text: CURRENT_VERSION
          }
        }
      }
    }
        stage ("Debug code change"){
            steps {
                script {
                echo "CODE_CHANGE is: ${CODE_CHANGE}"
                    }
            }
        }
        stage('Run ISO Update') {
        when {
            expression {
            return CODE_CHANGE  
            }
        }
        steps {
                script {
                echo "Downloading the ISO file from GCP bucket"
                def cleanedVersion = CURRENT_VERSION.replace('.', '')
                echo "CLEANED VERSION: ${cleanedVersion}"
                def isoFileName = "debian-custom-${cleanedVersion}.iso"
                echo "ISO FILENAME: ${isoFileName}"
                def exeFileName = "HauApp${cleanedVersion}"
                echo "EXE FILENAME: ${exeFileName}"
                withCredentials([file(credentialsId: 'jenkins-service-account-key', variable: 'GCP_KEY')]) {
                sh """
                    gcloud auth activate-service-account --key-file="\$GCP_KEY"
                    gcloud storage cp gs://hiper_global_artifacts/${isoFileName} ${isoFileName}
                """
                }
                echo "Updating ISO for FOG servers"
                sshagent(['ansible-ssh-key']) {
                sh """
                    ansible all \
                    -i "${TARGET_IP}," \
                    -m ping \
                    -u hau \
                    --private-key $SSH_AUTH_SOCK
                """
                }
                
            }
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: '.last_version.txt', fingerprint: true
    }
  }
}
