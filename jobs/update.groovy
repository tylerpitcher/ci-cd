pipeline {
  agent any
  stages {
    stage("Clone") {
      steps {
         // Clone repository using the repository parameter
        git url: "${params.repository}", branch: "main"
      }
    }
    stage("Update Manifest") {
      steps {
        script {
          withCredentials([usernamePassword(credentialsId: "dockerhub", usernameVariable: "DOCKER_USERNAME", passwordVariable: "DOCKER_PASSWORD")]) {
            withCredentials([usernamePassword(credentialsId: "github-username-token", usernameVariable: "GIT_USERNAME", passwordVariable: "GIT_PASSWORD")]) {
              // Display the content of the manifest
              sh "cat manifests/${params.imageName}.yaml"

              // Update image tag in the manifest
              sh "sed -i 's+${DOCKER_USERNAME}/${params.imageName}.*+${DOCKER_USERNAME}/${params.imageName}:${params.dockertag}+g' manifests/${params.imageName}.yaml"
              
              // Display the content of the manifest after update
              sh "cat manifests/${params.imageName}.yaml"
              
              // Commit the changes to the manifest
              sh "git commit -am 'Updated image tag'"
              
              // Push commit
              sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${GIT_USERNAME}/${params.repositoryName}.git HEAD:main"
            }
          }
        }
      }
    }
  }
  post {
    always {
      cleanWs()
    }
  }
}
