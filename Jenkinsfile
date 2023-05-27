#!groovy

node("docker_slave") {
  def branch = "lookup" // env.BRANCH_NAME

  echo "Current branch is ${branch}"

  // Mandatory, to maintain branch integrity
  checkout scm

  stage("Integration test") {
    sh "sbt -Dsbt.ivy.home=/var/jenkins_home/.ivy2/ clean it:test"
  }

  /*stage('Build') {
    sh "sbt package"
  }*/

  /*stage('Publish-Local') {
    sh "sbt publish-local"
  }*/

  //stage('Archive') {
  //  archive 'target/**/test-dep*.jar'
  //}
}