pipeline {
    agent any

    environment {
        DOCKERHUB_CREDENTIALS = credentials('docker-token')
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    triggers {
        pollSCM('* * * * *')
    }

    stages {

        stage("Clean") {
            steps {
                sh "./restapi/gradlew clean"
            }
        }

        stage("Build") {
            steps {
                sh "./restapi/gradlew build"
                sh "cp ./restapi/build/libs/MiniBoard-0.0.1-SNAPSHOT.jar ./docker/smboard/"
            }
        }

        stage("Docker Login") {
            steps {
                sh """
                    echo ${DOCKERHUB_CREDENTIALS_PSW} | docker login -u ${DOCKERHUB_CREDENTIALS_USR} --password-stdin
                """
            }
        }

        stage("Docker Image Build") {
            steps {
                sh "docker build -t d41n11/apache2_smboard:${IMAGE_TAG} ./docker/apache2/"
                sh "docker build -t d41n11/smboard_smboard:${IMAGE_TAG} ./docker/smboard/"
                sh "docker build -t d41n11/mariadb_smboard:${IMAGE_TAG} ./docker/mariadb/"
            }
        }

        stage("Docker Push") {
            steps {
                sh "docker push d41n11/apache2_smboard:${IMAGE_TAG}"
                sh "docker push d41n11/smboard_smboard:${IMAGE_TAG}"
                sh "docker push d41n11/mariadb_smboard:${IMAGE_TAG}"
            }
        }

        stage("Cleanup Images") {
            steps {
                sh "docker image rm d41n11/apache2_smboard:${IMAGE_TAG} || true"
                sh "docker image rm d41n11/smboard_smboard:${IMAGE_TAG} || true"
                sh "docker image rm d41n11/mariadb_smboard:${IMAGE_TAG} || true"
            }
        }

        stage("Deploy (Kubernetes)") {
            steps {
                sh "sed -i 's/{{VERSION}}/${IMAGE_TAG}/g' ./kubernetes/apache2.yml"
                sh "sed -i 's/{{VERSION}}/${IMAGE_TAG}/g' ./kubernetes/smboard.yml"
                sh "sed -i 's/{{VERSION}}/${IMAGE_TAG}/g' ./kubernetes/mariadb.yml"

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
            echo "Completed Server Deploy"
            slackSend (
                channel: "#jenkins",
                color: "#2C953C",
                message: "smboard 배포가 성공하였습니다."
            )
        }

        failure {
            echo "Fail Server Deploy"
            slackSend (
                channel: "#jenkins",
                color: "#FF3232",
                message: "smboard 배포가 실패하였습니다."
            )
        }
    }
}

