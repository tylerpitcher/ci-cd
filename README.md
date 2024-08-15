# My Build Jenkins
This repository contains the configurations and scripts for a custom Jenkins setup to build Docker images and update manifest files. The setup leverages Jenkins pipelines, Job DSL, and Jenkins Configuration as Code (JCasC).

## How to Run
1. Create a .env file in root directory
```
HOST=

GITHUB_USERNAME=
GITHUB_TOKEN=

GITHUB_CLIENT_ID=
GITHUB_CLIENT_SECRET=

DOCKER_USERNAME=
DOCKER_TOKEN=

GIT_NAME=
GIT_EMAIL=
```

2. Pull docker image
```
docker pull jenkins/jenkins:lts
```

3. Build
```
docker-compose build
```

4. Run
```
docker-compose up
```
