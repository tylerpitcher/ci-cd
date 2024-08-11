# jenkins
FROM jenkins/jenkins:lts

# Skip setup wizard on first load
ENV JAVA_OPTS -Djenkins.install.runSetupWizard=false

# Install docker
USER root
RUN apt-get update && \
    apt-get install -y apt-transport-https ca-certificates curl gnupg-agent software-properties-common && \
    apt-get install -y docker.io && \
    rm -rf /var/lib/apt/lists/*

# Copy the plugins.txt file into the container
COPY config/plugins.txt /usr/share/jenkins/ref/plugins.txt

# Install plugins listed in plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Copy the JCasC configuration file into the container
COPY config/jcasc.yaml /usr/share/jenkins/ref/casc.yaml

# Set environment variable to point Jenkins to the JCasC configuration file
ENV CASC_JENKINS_CONFIG /usr/share/jenkins/ref/casc.yaml
