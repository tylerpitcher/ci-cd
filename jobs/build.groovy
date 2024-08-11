pipeline {
  agent any
  stages {
    stage("Clone") {
      steps {
         // Clone repository using the repository parameter
        git url: "${params.repository}", branch: "main"
      }
    }
    stage("Build Image") {
      steps {
        script {
          withCredentials([usernamePassword(credentialsId: "dockerhub", usernameVariable: "USERNAME", passwordVariable: "PASSWORD")]) {
            // Build the image using the directory path and image name
            def app = docker.build("${USERNAME}/${params.imageName}", "${params.dockerfileDir}")

            // Push the image to the registry with a tag corresponding to the commit hash
            env.commitHash = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
            docker.withRegistry("https://registry.hub.docker.com", "dockerhub") {
              app.push(commitHash)
            }
          }
        }
      }
    }
    stage("Trigger SG.Update-Manifest") {
      steps {
        echo "Triggering SG.Update-Manifest"
        build job: "SG.Update-Manifest", parameters: [
          string(name: "repositoryName", value: params.repositoryName),
          string(name: "repository", value: params.repository),
          string(name: "imageName", value: params.imageName),
          string(name: "dockertag", value: env.commitHash)
        ]
      }
    }
  }
  post {
    always {
      cleanWs()
    }
  }
}
