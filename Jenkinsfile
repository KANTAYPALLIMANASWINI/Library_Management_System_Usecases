
pipeline {
    agent any

    stages {

        stage('Environment Check') {
            steps {
                sh '''
                    echo "Checking Jenkins build environment"
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
                        --timeout 30m \
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
            echo 'Jenkins pipeline completed successfully.'
        }

        failure {
            echo 'Jenkins pipeline failed. Check Console Output.'
        }
    }
}
