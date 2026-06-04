pipeline {

    agent any

    environment {
        DOCKERHUB_CREDENTIALS = credentials('docker-token')
    }

    triggers {
        pollSCM('* * * * *')
    }

    stages {

        stage("Compile") {
            steps {
                dir('restapi') {
                    sh "./gradlew compileJava"
                }
            }
        }

        stage("Build") {
            steps {
                dir('restapi') {
                    sh "./gradlew build"
                }
                sh "cp ./restapi/build/libs/*.jar ./docker/smboard/"
            }
        }

        stage("Docker Login") {
            steps {
                sh "echo ${DOCKERHUB_CREDENTIALS_PSW} | docker login -u ${DOCKERHUB_CREDENTIALS_USR} --password-stdin"
            }
        }

        stage("Docker Image Build") {
            steps {
                sh "docker build -t d41n11/apache2_smboard:${BUILD_NUMBER} ./docker/apache2/"
                sh "docker build -t d41n11/smboard_smboard:${BUILD_NUMBER} ./docker/smboard/"
                sh "docker build -t d41n11/mariadb_smboard:${BUILD_NUMBER} ./docker/mariadb/"
            }
        }

        stage("Docker Image Push") {
            steps {
                sh "docker push d41n11/apache2_smboard:${BUILD_NUMBER}"
                sh "docker push d41n11/smboard_smboard:${BUILD_NUMBER}"
                sh "docker push d41n11/mariadb_smboard:${BUILD_NUMBER}"
            }
        }

        stage("Docker Image Clean up") {
            steps {
                sh "docker image rm d41n11/apache2_smboard:${BUILD_NUMBER} || true"
                sh "docker image rm d41n11/smboard_smboard:${BUILD_NUMBER} || true"
                sh "docker image rm d41n11/mariadb_smboard:${BUILD_NUMBER} || true"
            }
        }

        stage("Minikube start") {
            steps {
                sh "minikube start --driver=docker --cni=calico || true"
            }
        }

        stage("Deploy") {
            steps {
                sh "sed -i 's/{{VERSION}}/${BUILD_NUMBER}/g' ./kubernetes/apache2.yml"
                sh "sed -i 's/{{VERSION}}/${BUILD_NUMBER}/g' ./kubernetes/smboard.yml"
                sh "sed -i 's/{{VERSION}}/${BUILD_NUMBER}/g' ./kubernetes/mariadb.yml"

                sh "kubectl delete -A ValidatingWebhookConfiguration ingress-nginx-admission || true"

                sh "kubectl apply -f ./kubernetes/mariadb.yml"
                sh "kubectl apply -f ./kubernetes/smboard.yml"
                sh "kubectl apply -f ./kubernetes/apache2.yml"
                sh "kubectl apply -f ./kubernetes/ingress.yml"
            }
        }

    }

    post {
        success {
            slackSend (
                channel: "#jenkins",
                color: "#2C953C",
                message: "smboard 배포가 성공하였습니다."
            )
            echo "Completed Server Deploy"
        }

        failure {
            slackSend (
                channel: "#jenkins",
                color: "#FF3232",
                message: "smboard 배포가 실패하였습니다."
            )
            echo "Fail Server Deploy"
        }
    }
}
