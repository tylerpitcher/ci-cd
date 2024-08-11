import groovy.json.JsonSlurper
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI

// Function to fetch repositories from a user's GitHub account with a specific topic
def fetchRepositories = { githubToken, username, topic ->
  // Create a new HTTP client
  def client = HttpClient.newHttpClient()

  // Define the URL to fetch the user's repositories
  def reposUrl = "https://api.github.com/users/${username}/repos"

  // Build the HTTP request to get the list of repositories
  def reposRequest = HttpRequest.newBuilder()
    .uri(URI.create(reposUrl))
    .header("Authorization", "Bearer ${githubToken}")
    .header("Accept", "application/vnd.github.v3+json")
    .build()
  
  // Send the request and get the response
  def reposResponse = client.send(reposRequest, HttpResponse.BodyHandlers.ofString())
  
  // Parse the JSON response into a list of repositories
  def repos = new JsonSlurper().parseText(reposResponse.body())
  
  // Filter the repositories to include only those with the specified topic
  def result = repos.findAll { repo ->
    // Get the topics of a repository
    def topicsRequest = HttpRequest.newBuilder()
      .uri(URI.create("${repo.url}/topics"))
      .header("Authorization", "Bearer ${githubToken}")
      .header("Accept", "application/vnd.github.mercy-preview+json")
      .build()
    
    // Send the request and get the response
    def topicsResponse = client.send(topicsRequest, HttpResponse.BodyHandlers.ofString())

    // Parse the JSON response into a list of topics
    def topics = new JsonSlurper().parseText(topicsResponse.body()).names
    
    // Check if the specified topic is in the list of topics
    topics.contains(topic)
  }.collect { repo ->
    [name: repo.name, url: repo.html_url]
  }

  return result
}

pipeline {
  agent any
  stages {
    stage("Generate Build Jobs") {
      steps {
        script {
          withCredentials([usernamePassword(credentialsId: "github-username-token", usernameVariable: "GITHUB_USERNAME", passwordVariable: "GITHUB_TOKEN")]) {
            // Fetch the repositories with deployed topic
            def repositories = fetchRepositories(GITHUB_TOKEN, GITHUB_USERNAME, "deployed")
            
            // Generate DSL scripts for each repository
            def jobDslScripts = repositories.collect { repo ->              
              // Clone the repository
              sh "git clone ${repo.url} ${repo.name}"

              // Find all Dockerfile paths in the repository  
              def dirs = sh(script: "find ${repo.name} -name Dockerfile", returnStdout: true).trim().split("\n")

              // Generate DSL scripts for each Dockerfile
              return dirs.collect { dir ->
                dir = dir
                  .replaceAll("/Dockerfile", "")
                  .replaceFirst(repo.name, ".")

                def imageName = dir
                  .replaceAll("/","-")
                  .replaceFirst(".", repo.name)

                // Return the generated DSL script for the Dockerfile
                return """
                  pipelineJob('SG.Build-${imageName}') {
                    parameters {
                      wHideParameterDefinition {
                        name('repositoryName')
                        defaultValue('${repo.name}')
                        description('Name of repository')
                      } 
                      wHideParameterDefinition {
                        name('repository')
                        defaultValue('${repo.url}')
                        description('Repository of service')
                      } 
                      wHideParameterDefinition {
                        name('imageName')
                        defaultValue('${imageName}')
                        description('Name of service image')
                      } 
                      wHideParameterDefinition {
                        name('dockerfileDir')
                        defaultValue('${dir}')
                        description('Path to dockerfile')
                      } 
                    }

                    definition {
                      cpsScm {
                        scm {
                          git {
                            remote {
                              url('https://github.com/tylerpitcher/jenkins.git')
                            }
                          }
                        }
                        scriptPath('./jobs/build.groovy')
                      }
                    }
                  }
                """
              }.join("\n")
            }.join("\n")

            // Execute the generated DSL scripts
            jobDsl(scriptText: jobDslScripts)
          }
        }
      }
    }
    stage("Generate Update Job") {
      steps {
        script {
          // DSL script for updating manifests
          def jobDslScript = """
            pipelineJob('SG.Update-Manifest') {
              definition {
                cpsScm {
                  scm {
                    git {
                      remote {
                        url('https://github.com/tylerpitcher/jenkins.git')
                      }
                    }
                  }
                  scriptPath('./jobs/update.groovy')
                }
              }
            }
          """

          // Execute the generated DSL scripts
          jobDsl(scriptText: jobDslScript)
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
