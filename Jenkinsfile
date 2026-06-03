pipeline {
    environment {
        DOCKERHUB_CREDENTIALS = credentials('docker-token')
    }
    agent any
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
                // --no-cache 옵션을 추가하여 레이어 캐시 문제를 차단.
                sh "docker build --no-cache -t redleon1/apache_boardnext:${BUILD_NUMBER} ./docker/apache2/"
                sh "docker build --no-cache -t redleon1/boardrest_boardnext:${BUILD_NUMBER} ./docker/boardrest/"
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
    // 파이프라인이 성공하든 실패하든, 마지막에 젠킨스 서버의 도커 캐시 찌꺼기를 완전히 청소.
    post {
        always {
            sh "docker builder prune -f"
        }
    }
}pipeline {
    environment {
        DOCKERHUB_CREDENTIALS = credentials('docker-token')
    }
    agent any
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
                // ð¡ --no-cache ìµìì ì¶ê°íì¬ ê¼¬ì¸ ë ì´ì´ ìºì ë¬¸ì ë¥¼ ìì² ì°¨ë¨í©ëë¤.
                sh "docker build --no-cache -t redleon1/apache_boardnext:${BUILD_NUMBER} ./docker/apache2/"
                sh "docker build --no-cache -t redleon1/boardrest_boardnext:${BUILD_NUMBER} ./docker/boardrest/"
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
                    slackSend(channel: "#itêµì¡", color: "#2C953C", message: "boardnext ë°°í¬ê° ì±ê³µíììµëë¤.")
                }
                failure {
                    slackSend(channel: "#itêµì¡", color: "#FF3232", message: "boardnext ë°°í¬ê° ì¤í¨íììµëë¤.")
                }
            }
        }
    }
    // ð¡ íì´íë¼ì¸ì´ ì±ê³µíë  ì¤í¨íë , ë§ì§ë§ì ì  í¨ì¤ ìë²ì ëì»¤ ìºì ì°êº¼ê¸°ë¥¼ ìì í ì²­ìí©ëë¤.
    post {
        always {
            sh "docker builder prune -f"
        }
    }
}
