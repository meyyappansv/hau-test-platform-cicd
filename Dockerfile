FROM jenkins/jenkins:lts

USER root

# Install Ansible and SSH
RUN apt-get update && \
    apt-get install -y ansible ssh && \
    apt-get clean

USER jenkins