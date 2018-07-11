 pipeline{
    agent any
    parameters{
        //string(name:'gitUrl',defaultValue:'http://192.168.8.246/yicamera_server/yicamera_server.git',description:'xxxx')
        string(name:'gitUrl',defaultValue:'ssh://git@192.168.8.246:10022/yicamera_server/yicamera_server.git',description:'xxxx')
        string(name:'opGitUrl',defaultValue:'ssh://git@192.168.8.246:10022/yicamera_server/orderpayment.git',description:'xxxx')
        string(name:'gitBranch',defaultValue:'TW_Release_JDK7',description:'xxxx')
        string(name:'opGitBranch',defaultValue:'TW_OrderPayment',description:'xxxx')
        //pom.xml的相对路径
        string(name:'pubPomPath', defaultValue: './familymonitor-pub/pom.xml', description: 'pom.xml的相对路径')
        string(name:'passPortPomPath', defaultValue: './ants-passport/pom.xml', description: 'pom.xml的相对路径')
        string(name:'opPomPath', defaultValue: './orderpayment/pom.xml', description: 'pom.xml的相对路径')
        string(name:'opuPomPath', defaultValue: './orderpaymentutilities/pom.xml', description: 'pom.xml的相对路径')
        string(name:'pomPath', defaultValue: './familymonitor-interface/pom.xml', description: 'pom.xml的相对路径')
    }
    environment{
        CRED_ID='d2f79bf0-3a80-4b01-bca5-4cd9a1d7ebc7'
    }
    options{
        timestamps();
    }
    tools{
        jdk 'jdk8'
        maven 'maven3'
    }
    //from monday to friday,4:00 check
    // triggers{
    //     pollSCM('H * 4 * * 1-5')
    // }
    stages{
        stage('Checkout'){
            steps{
                git credentialsId:CRED_ID,url:params.opGitUrl,branch:params.opGitBranch
                sh "mvn -f ${params.opuPomPath} clean install -Dautoconfig.skip=true -Dmaven.test.skip=true"
                sh "mvn -f ${params.opPomPath} clean install -Dautoconfig.skip=true -Dmaven.test.skip=true"
            }
        }
        stage('xxxx'){
            steps{
                //checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'd2f79bf0-3a80-4b01-bca5-4cd9a1d7ebc7', url: 'ssh://git@192.168.8.246:10022/yicamera_server/ant-crm-new.git']]])
                git credentialsId:CRED_ID,url:params.gitUrl,branch:params.gitBranch
            }
        }
        stage('instal depend package'){
            parallel{
                stage('package familymonitor-pub'){
                    steps{
                        sh "mvn -f ${params.pubPomPath} clean install -Dautoconfig.skip=true -Dmaven.test.skip=true"
                    }
                }
                stage('package hub'){
                    steps{
                        sh "mvn -f ${params.passPortPomPath} clean install -Dautoconfig.skip=true -Dmaven.test.skip=true"
                    }
                }
            }
        }
        stage('builde interface'){
            steps{
                sh "mvn -f ${params.pomPath} clean install -Dautoconfig.skip=true -Dmaven.test.skip=true"
				echo "ok"
            }
        }
    }
}
