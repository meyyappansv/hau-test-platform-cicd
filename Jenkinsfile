def CODE_CHANGE = false
pipeline {
  agent any

  environment {
    VERSION_FILE = 'version.txt'         // or changelog.md, package.json, etc.
    STORED_VERSION = '.last_version.txt' // stored version from last build
    TARGET_IP = "192.168.30.118"
    CURRENT_VERSION=''
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
          def currentVersion = readFile(env.VERSION_FILE).trim()
          env.CURRENT_VERSION = currentVersion
          def lastVersion = fileExists(env.STORED_VERSION) ? readFile(env.STORED_VERSION).trim() : ''

          echo "Current version: ${currentVersion}"
          echo "Last known version: ${lastVersion}"

          if (currentVersion == lastVersion) {
            echo "No new version found. Skipping pipeline."
            currentBuild.result = 'NOT_BUILT'
            error("Version unchanged")
          } else {
            
            echo "New version detected: ${currentVersion}"
            CODE_CHANGE = true
            writeFile file: env.STORED_VERSION, text: currentVersion
          }
        }
      }
    }

        stage('Run ISO Update') {
        expression {
          return CODE_CHANGE  
        }
        steps {
                script {
                    def cleanedVersion = env.CURRENT_VERSION.replace('.', '')
                echo "Downloading the ISO file from GCP bucket"
                def isoFileName = "debian-custom-${cleanedVersion}.iso"
                def exeFileName = "HauApp${cleanedVersion}"
                withCredentials([file(credentialsId: 'jenkins-service-account-key', variable: 'GCP_KEY')]) {
                sh """
                    gcloud auth activate-service-account --key-file=\"$GCP_KEY\"
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
