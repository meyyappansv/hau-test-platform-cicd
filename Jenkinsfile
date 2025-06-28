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
          def currentVersion = readFile(env.VERSION_FILE).trim()
          def lastVersion = fileExists(env.STORED_VERSION) ? readFile(env.STORED_VERSION).trim() : ''

          echo "Current version: ${currentVersion}"
          echo "Last known version: ${lastVersion}"

          if (currentVersion == lastVersion) {
            echo "No new version found. Skipping pipeline."
            currentBuild.result = 'NOT_BUILT'
            error("Version unchanged")
          } else {
            echo "New version detected: ${currentVersion}"
            writeFile file: env.STORED_VERSION, text: currentVersion
          }
        }
      }
    }

    stage('Run Build Tasks') {
      steps {
        echo "Running tasks for new version!"
         sshagent(['ansible-ssh-key']) {
          sh '''
            ansible all \
              -i "${TARGET_IP}," \
              -m ping \
              -u hau \
              --private-key $SSH_AUTH_SOCK
          '''
        }
        // Add actual build steps here
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: '.last_version.txt', fingerprint: true
    }
  }
}
