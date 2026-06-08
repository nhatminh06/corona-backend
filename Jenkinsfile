pipeline {
  agent { label 'linux' }
  environment {
    HARBOR_REGISTRY = 'harbor.lab:8080'
    HARBOR_PROJECT  = 'library'
    IMAGE_NAME      = 'corona-backend'
    IMAGE_TAG       = "${env.BUILD_NUMBER}"
    FULL_IMAGE      = "${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
    NEXUS_BASE      = 'http://nexus.lab:8081'
    CHART_NAME      = 'corona-backend'
    CHART_VERSION   = '0.1.0'
    SONAR_HOST      = 'http://sonarqube.lab:9000'
  }
  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Security: secret scan (gitleaks)') {
      steps {
        sh 'gitleaks detect --source . --no-banner --redact --exit-code 1'
      }
    }

    stage('Security: SAST (semgrep)') {
      steps {
        sh '''
          set +e
          semgrep --config=auto --severity=ERROR --quiet .
          set +e
          echo "Semgrep scan complete (report only)"
        '''
      }
    }

    stage('Security: SCA (trivy)') {
      steps {
        sh '''
          trivy fs --severity HIGH,CRITICAL --exit-code 0 --no-progress --timeout 10m .
          echo "Trivy scan complete (report-only)"
        '''
      }
    }

    stage('Security: code quality (sonarqube)') {
      steps {
        withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
          sh '''
            mvn -B compile -DskipTests || true
            sonar-scanner \
              -Dsonar.host.url=${SONAR_HOST} \
              -Dsonar.token=${SONAR_TOKEN}
          '''
        }
      }
    }

    stage('Fetch secrets from Vault') {
      steps {
        withCredentials([
          string(credentialsId: 'vault-role-id', variable: 'VAULT_ROLE_ID'),
          string(credentialsId: 'vault-secret-id', variable: 'VAULT_SECRET_ID')
        ]) {
          sh '''
            VAULT_TOKEN=$(curl -sf -X POST \
              http://vault.lab:8200/v1/auth/approle/login \
              -d "role_id=${VAULT_ROLE_ID}&secret_id=${VAULT_SECRET_ID}" \
              | jq -r '.auth.client_token')

            if [ -z "$VAULT_TOKEN" ] || [ "$VAULT_TOKEN" = "null" ]; then
              echo "Vault authentication failed"
              exit 1
            fi

            TOOLS=$(curl -sf \
              -H "X-Vault-Token: ${VAULT_TOKEN}" \
              http://vault.lab:8200/v1/app-creds/data/jenkins-tools)

            echo "$TOOLS" | jq -r '.data.data.kubeconfig' | base64 -d > kubeconfig.yaml
            NEXUS_USER=$(echo "$TOOLS" | jq -r '.data.data.nexusUser')
            NEXUS_PASS=$(echo "$TOOLS" | jq -r '.data.data.nexusPassword')
            echo "HARBOR_USER=$(echo "$TOOLS" | jq -r '.data.data.harborUser')"     > build-creds.env
            echo "HARBOR_PASS=$(echo "$TOOLS" | jq -r '.data.data.harborPassword')" >> build-creds.env
            echo "NEXUS_USER=$(echo "$TOOLS" | jq -r '.data.data.nexusUser')"      >> build-creds.env
            echo "NEXUS_PASS=$(echo "$TOOLS" | jq -r '.data.data.nexusPassword')"  >> build-creds.env

            cat > settings.xml <<XMLEOF
<settings>
  <servers>
    <server>
      <id>nexus</id>
      <username>${NEXUS_USER}</username>
      <password>${NEXUS_PASS}</password>
    </server>
  </servers>
  <mirrors>
    <mirror>
      <id>nexus</id>
      <mirrorOf>*</mirrorOf>
      <url>http://nexus.lab:8081/repository/maven-central-proxy/</url>
    </mirror>
  </mirrors>
</settings>
XMLEOF

            if [ ! -s kubeconfig.yaml ]; then
              echo "Failed to fetch kubeconfig from Vault"
              exit 1
            fi

            echo "All credentials fetched from Vault successfully"
          '''
        }
      }
    }

    stage('Build Docker image') {
      steps {
        sh "docker build --add-host=nexus.lab:10.146.183.167 -t ${FULL_IMAGE} ."
      }
    }

    stage('Push to Harbor') {
      steps {
        sh '''
          set -a; . ./build-creds.env; set +a
          echo "$HARBOR_PASS" | docker login ${HARBOR_REGISTRY} \
            -u "$HARBOR_USER" --password-stdin
          docker push ${FULL_IMAGE}
          docker logout ${HARBOR_REGISTRY}
        '''
      }
    }

    stage('Fetch & template Helm chart') {
      steps {
        sh '''
          set -a; . ./build-creds.env; set +a
          curl -fsSL -u "$NEXUS_USER:$NEXUS_PASS" \
            -o chart.tgz \
            ${NEXUS_BASE}/repository/helm-charts/${CHART_NAME}-${CHART_VERSION}.tgz
          tar -xzf chart.tgz
          helm template ${IMAGE_NAME} ./${CHART_NAME} --set image.tag=${IMAGE_TAG}
        '''
      }
    }

    stage('Deploy') {
      steps {
        sh '''
          set -a; . ./build-creds.env; set +a
          export KUBECONFIG=$(pwd)/kubeconfig.yaml
          helm upgrade --install ${IMAGE_NAME} ./${CHART_NAME} \
            --set image.tag=${IMAGE_TAG} \
            --namespace default
        '''
      }
    }
  }
  post {
    always {
      sh 'rm -f build-creds.env kubeconfig.yaml settings.xml || true'
      sh 'docker rmi ${FULL_IMAGE} || true'
      sh 'rm -rf chart.tgz corona-backend/ || true'
    }
  }
}