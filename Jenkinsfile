pipeline {
  agent any
  stages {
    stage ('Git Checkout') {
      steps {
        git branch: 'master', credentialsId: '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', url: 'https://github.com/tahritakwa/authentication.git'
    }
  }
    stage('build and server') {
      steps {
        sh 'docker build -t acct_image .'
      }
    }
    stage('docker') {
      steps {
        sh 'docker-compose up --build -d'
      }
    }

  }
}
