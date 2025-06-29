FROM jenkins/jenkins:lts

USER root

# Install Ansible and SSH
RUN apt-get update && \
    apt-get install -y ansible ssh curl apt-transport-https ca-certificates gnupg lsb-release rsync && \
    apt-get clean

RUN echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] http://packages.cloud.google.com/apt cloud-sdk main" \
    | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list \
 && curl https://packages.cloud.google.com/apt/doc/apt-key.gpg \
    | apt-key --keyring /usr/share/keyrings/cloud.google.gpg add -

# Install the gcloud CLI
RUN apt-get update && apt-get install -y google-cloud-sdk 
RUN gcloud --version
USER jenkins