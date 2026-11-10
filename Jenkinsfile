
pipeline {
    agent any

    stages {

        stage('Environment Check') {
            steps {
                sh '''
                    echo "Checking Jenkins environment"
                    java -version
                    git --version
                    docker --version
                '''
            }
        }

        stage('Frontend Build') {
            steps {
                dir('frontend') {
                    nodejs(nodeJSInstallationName: 'NodeJS') {
                        sh '''
                            node --version
                            npm --version
                            npm ci
                            npm run build
                        '''
                    }
                }
            }
        }

        stage('Backend Build and Tests') {
            steps {
                dir('backend') {
                    withEnv(["PATH+MAVEN=${tool 'Maven3'}/bin"]) {
                        sh '''
                            echo "Building backend and running tests"

                            mvn -B -Djacoco.skip=true package

                            test -f target/library-1.0.0.jar

                            echo "Backend build completed"
                        '''
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                catchError(
                    buildResult: 'UNSTABLE',
                    stageResult: 'FAILURE'
                ) {
                    dir('backend') {
                        withSonarQubeEnv('SonarQube') {
                            withEnv(["PATH+MAVEN=${tool 'Maven3'}/bin"]) {
                                sh '''
                                    echo "Running SonarQube code analysis"

                                    mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar \
                                        -Dsonar.projectKey=Library-Management-System

                                    echo "SonarQube analysis completed"
                                '''
                            }
                        }
                    }
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    env.IMAGE_TAG = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                }

                sh '''
                    echo "Building Docker image"

                    docker build \
                        -t library-backend:$IMAGE_TAG \
                        backend

                    docker image inspect library-backend:$IMAGE_TAG

                    echo "Docker build completed"
                '''
            }
        }

        stage('Trivy Scan') {
            steps {
                sh '''
                    echo "Scanning Docker image with Trivy"

                    docker run --rm \
                        -v /var/run/docker.sock:/var/run/docker.sock \
                        -v trivy-cache:/root/.cache/ \
                        aquasec/trivy:0.76.2 image \
                        --image-src docker \
                        --db-repository ghcr.io/aquasecurity/trivy-db:2 \
                        --java-db-repository ghcr.io/aquasecurity/trivy-java-db:1 \
                        --timeout 8m \
                        --scanners vuln \
                        --severity HIGH,CRITICAL \
                        --exit-code 1 \
                        library-backend:$IMAGE_TAG

                    echo "Trivy scan passed"
                '''
            }
        }
    }

stage('Harbor Push') {
    steps {
        withCredentials([
            usernamePassword(
                credentialsId: 'harbor-credentials',
                usernameVariable: 'HARBOR_USER',
                passwordVariable: 'HARBOR_PASSWORD'
            )
        ]) {
            sh '''
                set -eu

                HARBOR_REGISTRY="host.docker.internal:8082"

                HARBOR_IMAGE="$HARBOR_REGISTRY/library-management-usecase1/library-backend:$IMAGE_TAG"

                echo "Tagging Docker image for Harbor"

                docker tag \
                    "library-backend:$IMAGE_TAG" \
                    "$HARBOR_IMAGE"

                echo "Logging in to Harbor"

                export DOCKER_CONFIG="$(mktemp -d)"
                trap 'rm -rf "$DOCKER_CONFIG"' EXIT

                printf '%s' "$HARBOR_PASSWORD" | \
                    docker login "$HARBOR_REGISTRY" \
                    --username "$HARBOR_USER" \
                    --password-stdin

                echo "Pushing Docker image to Harbor"

                docker push "$HARBOR_IMAGE"

                echo "Harbor image push completed successfully"
            '''
        }
    }
}


    post {
        success {
            echo 'SUCCESS: Jenkins pipeline completed.'
        }

        unstable {
            echo 'UNSTABLE: SonarQube analysis encountered an issue.'
        }

        failure {
            echo 'FAILURE: Check Jenkins Console Output.'
        }
    }
}
