pipeline {
    environment {
        DOCKERHUB_CREDENTIALS = credentials('docker-token')
    }
    agent any
    tools {
        jdk 'jdk17' 
    }
    triggers {
        pollSCM('* * * * *')
    }
    stages {
        stage("Compile") {
            steps {
                dir('restapi'){
                    sh "./gradlew clean compileJava -x test"
                }
            }
        }
        stage("Build") {
            steps {
                dir('restapi'){
                    sh "./gradlew clean build -x test"
                }
                sh """
                    cp ./restapi/build/libs/BoardRest-0.0.1-SNAPSHOT.jar ./docker/boardrest/boardrest.jar
                    ls -lah ./docker/boardrest/
                """
            }
        }
        stage("Docker Login") {
            steps {
                sh 'echo $DOCKERHUB_CREDENTIALS_PSW | docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin'
            }
        }
        stage("Docker Image Build") {
            steps {
                sh "docker build -t redleon1/apache_boardnext:${BUILD_NUMBER} ./docker/apache2/"
                sh "docker build -t redleon1/boardrest_boardnext:${BUILD_NUMBER} ./docker/boardrest/"
            }
        }
        stage("Docker Image Push") {
            steps {
                sh "docker push redleon1/apache_boardnext:${BUILD_NUMBER}"
                sh "docker push redleon1/boardrest_boardnext:${BUILD_NUMBER}"
            }
        }
        stage("Docker Image Clean up") {
            steps {
                sh "docker image rm redleon1/apache_boardnext:${BUILD_NUMBER}"
                sh "docker image rm redleon1/boardrest_boardnext:${BUILD_NUMBER}"
            }
        }
        stage("Deploy") {
            steps {
                sh "sed -i 's/{{VERSION}}/${BUILD_NUMBER}/g' ./kubernetes/apache2.yml"
                sh "sed -i 's/{{VERSION}}/${BUILD_NUMBER}/g' ./kubernetes/boardrest.yml"
                //sh "kubectl delete --ignore-not-found=true -A ValidatingWebhookConfiguration ingress-nginx-admission"
                sh "kubectl apply -f ./kubernetes/boardrest.yml"
                sh "kubectl apply -f ./kubernetes/apache2.yml"
                sh "kubectl apply -f ./kubernetes/ingress.yml"
            }
            post {
                success {
                    slackSend(channel: "#it교육", color: "#2C953C", message: "boardnext 배포가 성공하였습니다.")
                }
                failure {
                    slackSend(channel: "#it교육", color: "#FF3232", message: "boardnext 배포가 실패하였습니다.")
                }
            }
        }
    }
}
