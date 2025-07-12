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
        return false
      }

  }
  else{
    echo "FILE: ${isoFileName} exists in the control node. Not downloading the file"
  }
              
  echo "Updating ISO for FOG servers"
  sshagent(['ansible-ssh-key']) {
    if(environmentName == "Development"){

        sh """
            ANSIBLE_HOST_KEY_CHECKING=False \
            ansible-playbook fog-iso-deploy.yaml \
            -i inventory.ini \
            --extra-vars "artifact_name=${isoFileName}" \
            --extra-vars "target_hosts=fog" \
        """
    }
    else{
      if (environmentName == "Staging"){

          sh """
            ansible-playbook fog-iso-deploy.yaml \
            -i hiperglobal-inventory.ini \
            --extra-vars "artifact_name=${isoFileName}" \
            --extra-vars "target_hosts=fogstaging" \
        """
      }
      else{
          sh """
            ansible-playbook fog-iso-deploy.yaml \
            -i hiperglobal-inventory.ini \
            --extra-vars "artifact_name=${isoFileName}" \
            --extra-vars "target_hosts=foglive" \
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
   sshagent(['ansible-ssh-key']) {
    if(environmentName == "Development"){

        sh """
            ANSIBLE_HOST_KEY_CHECKING=False \
            ansible-playbook ui-install-apt-packages.yaml \
            -i inventory.ini \
            --extra-vars "target_hosts=ui" \
        """
    }
    else{
      if (environmentName == "Staging"){

          sh """
            ansible-playbook ui-install-apt-packages.yaml \
            -i hiperglobal-inventory.ini \
            --extra-vars "target_hosts=uistaging" \
        """
      }
      else{
          sh """
            ansible-playbook ui-install-apt-packages.yaml \
            -i hiperglobal-inventory.ini \
            --extra-vars "target_hosts=uilive" \
        """
      }
      
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
      withCredentials([file(credentialsId: 'jenkins-service-account-key', variable: 'GCP_KEY')]) {
          sh """
              gcloud auth activate-service-account --key-file="\$GCP_KEY"
              gcloud storage cp gs://hiper-global-artifacts/${exeFileName} ${exeFileName}
          """
        }

  }
  else{
    echo "FILE: ${exeFileName} exists in the control node. Not downloading the file"
  }

  echo "Updating EXE for user laptops"
  sshagent(['ansible-ssh-key']) {
    if(environmentName == "Development"){

        sh """
            ANSIBLE_HOST_KEY_CHECKING=False \
            ansible-playbook ui-exe-deploy.yaml \
            -i inventory.ini \
            --extra-vars "artifact_name=${exeFileName}" \
            --extra-vars "target_hosts=ui" \
        """
    }
    else{
      if (environmentName == "Staging"){

          sh """
            ansible-playbook ui-exe-deploy.yaml \
            -i hiperglobal-inventory.ini \
            --extra-vars "artifact_name=${exeFileName}" \
            --extra-vars "target_hosts=uistaging" \
        """
      }
      else{
          sh """
            ansible-playbook ui-exe-deploy.yam \
            -i hiperglobal-inventory.ini \
            --extra-vars "artifact_name=${exeFileName}" \
            --extra-vars "target_hosts=uilive" \
        """
      }
      
    }
  } 
}