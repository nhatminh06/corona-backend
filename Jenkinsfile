pipeline {
  agent { label 'linux' }

  tools {
    maven 'maven-3'
    jdk   'jdk-21'
  }

  environment {
    HARBOR_REGISTRY = 'harbor.lab:8080'
    HARBOR_PROJECT  = 'library'
    IMAGE_NAME      = 'corona-backend'
    IMAGE_TAG       = "${env.BUILD_NUMBER}"
    FULL_IMAGE      = "${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
    K8S_NAMESPACE   = 'default'
    RELEASE_NAME    = 'corona-backend'
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build JAR') {
      steps { sh 'mvn -B clean package -DskipTests' }
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

    stage('Helm template (render)') {
      steps {
        sh '''
          helm template ${RELEASE_NAME} ./helm \
            --set image.repository=${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${IMAGE_NAME} \
            --set image.tag=${IMAGE_TAG} \
            --namespace ${K8S_NAMESPACE}
        '''
      }
    }

    stage('Helm upgrade') {
      steps {
        withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
          sh '''
            helm upgrade --install ${RELEASE_NAME} ./helm \
              --set image.repository=${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${IMAGE_NAME} \
              --set image.tag=${IMAGE_TAG} \
              --namespace ${K8S_NAMESPACE} \
              --create-namespace
          '''
        }
      }
    }
  }

  post {
    always {
      sh 'docker rmi ${FULL_IMAGE} || true'   // clean up local image
    }
  }
}
