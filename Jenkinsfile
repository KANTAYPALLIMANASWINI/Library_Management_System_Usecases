
pipeline {
    agent any

    stages {
        stage('Environment Check') {
            steps {
                sh '''
                    echo "Checking Jenkins build environment"
                    java -version
                    git --version
                    command -v mvn || true
                    command -v node || true
                    command -v npm || true
                    command -v docker || true
                    command -v trivy || true
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

        stage('Backend Unit Tests') {
            steps {
                dir('backend') {
                    withEnv(["PATH+MAVEN=${tool 'Maven3'}/bin"]) {
                        sh '''
                            mvn --version
                            mvn test
                        '''
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                dir('backend') {
                    withSonarQubeEnv('SonarQube') {
                        withEnv(["PATH+MAVEN=${tool 'Maven3'}/bin"]) {
                            sh '''
                                mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar \
                                  -Dsonar.projectKey=Library-Management-System
                            '''
                        }
                    }
                }
            }
        }

        stage('SonarQube Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Package Backend') {
            steps {
                dir('backend') {
                    withEnv(["PATH+MAVEN=${tool 'Maven3'}/bin"]) {
                        sh '''
                            mvn -B -DskipTests package
                            test -f target/library-1.0.0.jar
                        '''
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

                    echo "Docker image built successfully"

                    docker image inspect library-backend:$IMAGE_TAG
                '''
            }
        }
    }

    post {
        success {
            echo 'Jenkins CI pipeline completed successfully.'
        }
        failure {
            echo 'Jenkins CI pipeline failed. Check Console Output.'
        }
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
                        --severity HIGH,CRITICAL \
                        --exit-code 1 \
                        library-backend:$IMAGE_TAG

                    echo "Trivy scan passed"
                '''
            }
        }

