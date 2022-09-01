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
