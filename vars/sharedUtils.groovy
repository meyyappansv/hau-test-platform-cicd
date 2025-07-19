def performISOUpdate(environmentName,currentVersion){

  echo "Downloading the ISO file from GCP bucket"
  def cleanedVersion = currentVersion.replace('.', '')
  echo "CLEANED VERSION: ${cleanedVersion}"
  def isoFileName = "debian-custom-${cleanedVersion}.iso"
  echo "ISO FILENAME: ${isoFileName}"
  def exeFileName = "HauApp${cleanedVersion}"
  echo "EXE FILENAME: ${exeFileName}"
  if (!fileExists(isoFileName)){
      echo "FILE: ${isoFileName} does not exist in the control node."
      try {
        withCredentials([file(credentialsId: 'jenkins-service-account-key', variable: 'GCP_KEY')]) {
            sh """
                gcloud auth activate-service-account --key-file="\$GCP_KEY"
                gcloud storage cp gs://hiper-global-artifacts/${isoFileName} ${isoFileName}
            """
          }
      }
      catch (Exception e) {
        //Issue with getting ISO from google cloud bucket
        echo "Issue with getting ISO from google cloud bucket"
        return [status: 'ERROR', message: "Issue with getting ${isoFileName} from google cloud bucket"]
      }

  }
  else{
    echo "FILE: ${isoFileName} exists in the control node. Not downloading the file"
  }
              
  echo "Updating ISO for FOG servers"
  def cmd = ""
  if(environmentName == "Development"){

        cmd = """
            ANSIBLE_HOST_KEY_CHECKING=False \
            ansible-playbook fog-iso-deploy.yaml \
            -i inventory.ini \
            --extra-vars "artifact_name=${isoFileName}" \
            --extra-vars "target_hosts=fog" \
        """
    }
    else if (environmentName == "Staging"){

          cmd =  """
            ansible-playbook fog-iso-deploy.yaml \
            -i hiperglobal-inventory.ini \
            --extra-vars "artifact_name=${isoFileName}" \
            --extra-vars "target_hosts=fogstaging" \
        """
      }
    else{
          cmd =  """
            ansible-playbook fog-iso-deploy.yaml \
            -i hiperglobal-inventory.ini \
            --extra-vars "artifact_name=${isoFileName}" \
            --extra-vars "target_hosts=foglive" \
        """
      }
    
      sshagent(['ansible-ssh-key']) {
        def result = runCommand(cmd)
          echo "Output:\n${result.output}"
          echo "Exit status: ${result.status}"

          if (result.status != 0) {
            return [status: 'ERROR', message: "Issue with performing ISO update"]
          }
          else {
            return [status: 'SUCCESS', message: "ISO update performed successfully"]
          }
      } 
      
    }

def runCommand(String cmd) {
    // Wrap the command to capture output and status in one pass
  // ensure the command doesn't cause pipeline-level failure
  def status = sh(script: "${cmd} > cmd.out 2>&1", returnStatus: true)
  def output = readFile('cmd.out').trim()
  echo "Exit code: ${status} — output:\n${output}"
  return [status: status,output: output ]
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
          if (rollbackISOFilename != "debian-custom-.iso"){
            if (isoFiles[i] != latestISOFilename && isoFiles[i] != rollbackISOFilename){ 
              echo "Deleting old ISO: ${isoFiles[i]}"
              sh "rm -f '${isoFiles[i]}'"
            }
          }
          else {
            if (isoFiles[i] != latestISOFilename){ 
              echo "Deleting old ISO: ${isoFiles[i]}"
              sh "rm -f '${isoFiles[i]}'"
            }
          }
      }
  } else {
            echo "No ISO files found."
          }
}

def cleanupOldExeFiles(currentVersion,rollBackVersion){
  // List all ISO files
  def cleanedCurrentVersion = currentVersion.replace('.', '')
  def cleanedRollBackVersion = rollBackVersion.replace('.','')
  //TODO Handle scenario where there are not existing ISO files
  def exeFiles = sh(script: "ls -1t HauApp*", returnStdout: true).trim().split('\n')
  def latestEXEFilename = "HauApp${cleanedCurrentVersion}"
  def rollbackEXEFilename = "HauApp${cleanedRollBackVersion}"
  echo "LATEST EXE FILE NAME: ${latestEXEFilename}"
  echo "ROLLBACK EXE FILE NAME: ${rollbackEXEFilename}"
  if (exeFiles.size() > 0) {
      // Loop through all except the latest
      for (int i = 0; i < exeFiles.size(); i++) {
          if (rollbackEXEFilename != "HauApp"){
            if (exeFiles[i] != latestEXEFilename && exeFiles[i] != rollbackEXEFilename){ 
              echo "Deleting old ISO: ${exeFiles[i]}"
              sh "rm -f '${exeFiles[i]}'"
            }
          }
          else {
            if (exeFiles[i] != latestEXEFilename){ 
              echo "Deleting old ISO: ${exeFiles[i]}"
              sh "rm -f '${exeFiles[i]}'"
            }
          }
      }
  } else {
            echo "No EXE files found."
          }
}

def cleanupRollbackISOFile(versionToRemove){
  // List all ISO files
  def cleanedVersiontoRemove = versionToRemove.replace('.', '')
  def isoFileToRemove = "debian-custom-${cleanedVersiontoRemove}.iso"
  sh """
      if [ -f ${isoFileToRemove} ]; then
        echo "Deleting file..."
        rm -f ${isoFileToRemove}
      else
        echo "File not found."
      fi
    """
}

def installUIPrerequisites(environmentName){
   echo "Updating EXE for FOG servers"
   def installPrereqCommand = ""
   if(environmentName == "Development"){

        installPrereqCommand = """
            ANSIBLE_HOST_KEY_CHECKING=False \
            ansible-playbook ui-install-apt-packages.yaml \
            -i inventory.ini \
            --extra-vars "target_hosts=ui" \
        """
    }
    else if (environmentName == "Staging"){

          installPrereqCommand =  """
            ansible-playbook ui-install-apt-packages.yaml \
            -i hiperglobal-inventory.ini \
            --extra-vars "target_hosts=uistaging" \
        """
      }
    else{
          installPrereqCommand =  """
            ansible-playbook ui-install-apt-packages.yaml \
            -i hiperglobal-inventory.ini \
            --extra-vars "target_hosts=uilive" \
        """
    }

   sshagent(['ansible-ssh-key']) {
        def result = runCommand(installPrereqCommand)
        echo "Output:\n${result.output}"
          echo "Exit status: ${result.status}"

          if (result.status != 0) {
            return [status: 'ERROR', message: "Issue with installing prerequisite packages in the UI machines"]
          }
          else {
            return [status: 'SUCCESS', message: "EXE prerequisite packages successfully"]
          }

    }
  } 


def performEXEUpdate(environmentName,currentVersion) {
  echo "Downloading the EXE file from GCP bucket"
  def cleanedVersion = currentVersion.replace('.', '')
  echo "CLEANED VERSION: ${cleanedVersion}"
  def exeFileName = "HauApp${cleanedVersion}"
  echo "EXE FILENAME: ${exeFileName}"
  if (!fileExists(exeFileName)){
      echo "FILE: ${exeFileName} does not exist in the control node."
      try {
        withCredentials([file(credentialsId: 'jenkins-service-account-key', variable: 'GCP_KEY')]) {
            sh """
                gcloud auth activate-service-account --key-file="\$GCP_KEY"
                gcloud storage cp gs://hiper-global-artifacts/${exeFileName} ${exeFileName}
            """
          }
      }
      catch (Exception e) {
          echo "Issue with getting ${exeFileName} from google cloud bucket"
        return [status: 'ERROR', message: "Issue with getting  ${exeFileName} from google cloud bucket"]
      }


  }
  else{
    echo "FILE: ${exeFileName} exists in the control node. Not downloading the file"
  }

  echo "Updating EXE for user laptops"
  def exeUpdateCommand = ""
  if(environmentName == "Development"){

        exeUpdateCommand ="""
            ANSIBLE_HOST_KEY_CHECKING=False \
            ansible-playbook ui-exe-deploy.yaml \
            -i inventory.ini \
            --extra-vars "artifact_name=${exeFileName}" \
            --extra-vars "target_hosts=ui" \
        """
    }
    else if (environmentName == "Staging"){

          exeUpdateCommand = """
            ansible-playbook ui-exe-deploy.yaml \
            -i hiperglobal-inventory.ini \
            --extra-vars "artifact_name=${exeFileName}" \
            --extra-vars "target_hosts=uistaging" \
        """
      }
    else{
          exeUpdateCommand = """
            ansible-playbook ui-exe-deploy.yam \
            -i hiperglobal-inventory.ini \
            --extra-vars "artifact_name=${exeFileName}" \
            --extra-vars "target_hosts=uilive" \
        """
      }
      
       sshagent(['ansible-ssh-key']) {
        def result = runCommand(exeUpdateCommand)
        echo "Output:\n${result.output}"
        echo "Exit status: ${result.status}"

        if (result.status != 0) {
            return [status: 'ERROR', message: "Issue with installing EXE in the UI machines"]
        }
          else {
            return [status: 'SUCCESS', message: "EXE was updated successfully"]
          }
      } 
    }
 


def sendEmailNotification(messageType,emailBody){
  def subject=""
  def body=""
  if (messageType == "error"){
    subject = "❌ Failure: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
    body = "Build failed. Check logs here: ${env.BUILD_URL}\nBUILD ERROR: ${emailBody}"
  }
  else if(messageType == "abort"){
    subject = "⚠️ Aborted: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
    body = "Build was aborted. ${env.BUILD_URL}\nBUILD INFO: ${emailBody}"
  }
  else {
    subject = "✅ Success: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
    body = "Build succeeded. ${env.BUILD_URL}\nBUILD INFO: ${emailBody}"
  }
 
  emailext(
         to: "${env.EMAIL_RECIPIENTS}",
         from: 'jenkins@ehaven.co',
         subject: "${subject}",
         body: "${body}"
    )
}