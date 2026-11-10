
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
