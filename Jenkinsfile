pipeline {
  agent { label 'linux' }

  environment {
    HARBOR_REGISTRY = 'harbor.lab:8080'
    HARBOR_PROJECT  = 'library'
    IMAGE_NAME      = 'corona-frontend'
    IMAGE_TAG       = "${env.BUILD_NUMBER}"
    FULL_IMAGE      = "${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build Docker image') {
      steps {
        sh "docker build -t ${FULL_IMAGE} ."
      }
    }

    stage('Push to Harbor') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'harbor-creds',
                                          usernameVariable: 'HARBOR_USER',
                                          passwordVariable: 'HARBOR_PASS')]) {
          sh '''
            echo "$HARBOR_PASS" | docker login ${HARBOR_REGISTRY} -u "$HARBOR_USER" --password-stdin
            docker push ${FULL_IMAGE}
            docker logout ${HARBOR_REGISTRY}
          '''
        }
      }
    }

    stage('Helm template') {
      steps {
        withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
          sh "helm template ${IMAGE_NAME} ./helm --set image.tag=${IMAGE_TAG}"
        }
      }
    }

    stage('Helm upgrade') {
      steps {
        withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
          sh "helm upgrade --install ${IMAGE_NAME} ./helm --set image.tag=${IMAGE_TAG} --namespace default"
        }
      }
    }
  }

  post {
    always {
      sh 'docker rmi ${FULL_IMAGE} || true'
    }
  }
}
