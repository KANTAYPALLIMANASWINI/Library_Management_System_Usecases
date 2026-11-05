
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
                                mvn -B sonar:sonar \
                                  -Dsonar.projectKey=Library-Management-System
                            '''
                        }
                    }
                }
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
