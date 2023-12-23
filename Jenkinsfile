pipeline {
    agent any

    environment {
        AZURE_STORAGE_CONNECTION_STRING = credentials('BERRYSCAN_STORAGE_CONNECTION_STRING')
        TRAINING_DATA_DIR = "${WORKSPACE}/training_data" // Directory for training data
        VALIDATION_DATA_DIR = "${WORKSPACE}/validation_data" // Directory for validation data
        TRAINING_CONTAINER_NAME = "trainingdata" // Container for training data
        VALIDATION_CONTAINER_NAME = "validationdata" // Container for validation data
        OUTPUT_DIR = "${WORKSPACE}/output_strawberry_test"
        OUTPUT_CONTAINER_NAME = "outputdata"  // Directory for output
        DOCKER_IMAGE = "strawberry-disease-model"
    }

    stages {
        stage('Prepare Environment') {
            steps {
                script {
                    // Ensure the destination directories exist, do not clear them
                    sh 'mkdir -p $TRAINING_DATA_DIR'
                    sh 'mkdir -p $VALIDATION_DATA_DIR'
                    // Delete the output directory if it exists
                    sh 'rm -rf $OUTPUT_DIR'
                    // Create a fresh output directory
                    sh 'mkdir -p $OUTPUT_DIR'
                }
            }
        }

        stage('Pull Training Data from Azure Storage') {
            steps {
                script {
                    // Pull training data from the specified container in Azure storage, overwrite existing files
                    sh 'az storage blob download-batch --destination $TRAINING_DATA_DIR --source $TRAINING_CONTAINER_NAME --connection-string $AZURE_STORAGE_CONNECTION_STRING --overwrite'
                }
            }
        }

        stage('Pull Validation Data from Azure Storage') {
            steps {
                script {
                    // Pull validation data from the specified container in Azure storage, overwrite existing files
                    sh 'az storage blob download-batch --destination $VALIDATION_DATA_DIR --source $VALIDATION_CONTAINER_NAME --connection-string $AZURE_STORAGE_CONNECTION_STRING --overwrite'
                }
            }
        }

        stage('List Training Data') {
            steps {
                script {
                    // List contents of the training data directory
                    sh 'ls -lah $TRAINING_DATA_DIR'
                }
            }
        }
        stage('List Validation Data') {
            steps {
                script {
                    // List contents of the validation data directory
                    sh 'ls -lah $VALIDATION_DATA_DIR'
                }
            }
        }

        stage('Run Docker Image if Exists') {
            steps {
                script {
                    // Check if the Docker image exists
                    def imageExists = sh(script: "docker images -q $DOCKER_IMAGE", returnStdout: true).trim()
                    if (imageExists) {
                        // Run the Docker image with the necessary volume mounts
                        sh """
                        docker run --gpus all \\
                        -v $TRAINING_DATA_DIR/_annotations.coco.json:/app/data/train/_annotations.coco.json \\
                        -v $TRAINING_DATA_DIR:/app/data/train \\
                        -v $OUTPUT_DIR:/app/data/output_strawberry_test \\
                        -v $VALIDATION_DATA_DIR:/app/data/valid \\
                        -v $VALIDATION_DATA_DIR/_annotations.coco.json:/app/data/valid/_annotations.coco.json \\
                        $DOCKER_IMAGE
                        """
                    } else {
                        echo "Docker image $DOCKER_IMAGE does not exist."
                    }
                }
            }
        }

        stage('Upload Output to Azure Storage') {
            steps {
                script {
                    // Get the current date in YYYYMMDD format
                    def currentDate = new Date().format('yyyyMMdd')
                    // Define the new directory name with the date
                    def newOutputDirName = "output_strawberry_test_${currentDate}"
                    // Define the full path for the new directory
                    def newOutputDir = "${WORKSPACE}/${newOutputDirName}"

                    // Create the new directory and copy contents from the original output directory
                    sh """
                    mkdir -p $newOutputDir
                    cp -r $OUTPUT_DIR/* $newOutputDir/
                    """

                    // Upload the new directory to Azure Blob Storage
                    sh 'az storage blob upload-batch --destination $OUTPUT_CONTAINER_NAME --source $newOutputDir --connection-string $AZURE_STORAGE_CONNECTION_STRING'
                    
                    echo "Output uploaded to ${OUTPUT_CONTAINER_NAME}/${newOutputDirName}"
                }
            }
        }


        stage('Cleanup Resources') {
            steps {
                script {
                    // Define the new directory name with the date for cleanup
                    def currentDate = new Date().format('yyyyMMdd')
                    def newOutputDirName = "output_strawberry_test_${currentDate}"
                    def newOutputDir = "${WORKSPACE}/${newOutputDirName}"

                    echo 'Cleaning up resources...'
                    // Remove the temporary new output directory
                    sh "rm -rf $newOutputDir"
                }
            }
}

    }
    post {
        success {
            // Archive artifacts in the OUTPUT_DIR after a successful build
            archiveArtifacts artifacts: '$OUTPUT_DIR/*', fingerprint: true
        }
        always {
            // Even if the build fails or is aborted, clean up the OUTPUT_DIR
            echo 'Performing post-build cleanup...'
            sh 'rm -rf $OUTPUT_DIR'
        }
    }
}
