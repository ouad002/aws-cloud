# Amazon S3 Configuration for Project

### Purpose

The bucket serves as the central storage for:

-  **Raw IoT traffic data** uploaded by branches.

-  **Summarized data** produced by the SummarizeWorker Lambda function.

-  **Consolidated results** from the ConsolidatorWorker Lambda function.
- **Final data:** exported by the Export Client.

  
---
### Instructions to Create the Bucket

  

1.  **Open Amazon S3**:

      - Log in to your **AWS Management Console**.

     - From the **Services** menu, select **S3 (Storage)**.

  

2.  **Create a New Bucket**:

    - Click on **Create bucket**.

    - Configure the bucket with the following settings:

    -  **Bucket Name**: `newbucket37920`.

    -  **AWS Region**: `us-east-1`.

  
  

3.  **Set Object Ownership**:

    - In the **Object Ownership** section, select **ACLs enabled**.

  

4.  **Enable Versioning**:

    - In the **Bucket Versioning** section, click **Enable**.



5.  **Finalize the Bucket Creation**:

    - Leave all other settings as default.

    - Click **Create bucket**.

  

# Using the UploadClient to Upload Files to S3

### Purpose

The `UploadClient` automates the process of uploading IoT traffic files to the `newbucket37920` S3 bucket. Files are organized by branch ID and date to facilitate traceability and scalability.

---

  

### Instructions to Use the UploadClient

  

#### 1. Prerequisites

Before using the `UploadClient`, ensure the following:

-  **AWS CLI** is installed and configured with valid credentials.

-  **Java 17+** is installed.

-  **Apache Maven** is installed.

- The S3 bucket `newbucket37920` is created and accessible.

  

---

  

#### 2. Build the Project

1. Clone the repository:

```bash
git clone https://github.com/ouad002

cd aws-cloud
```
  

2. Build the Project

  

To compile and build the project, use the following Maven command:
 

```bash
mvn  clean  package
``` 

3. Run the UploadClient

  

To upload a file to the S3 bucket using the `UploadClient`, use the following command:

  

```bash
java  -cp  target/aws-project-cloud-1.0-SNAPSHOT.jar  com.project.s3.UploadClient  /mnt/c/Users/user/Downloads/data-20221201.csv
```
## Configuring and Deploying AWS Lambda Functions

Use AWS Lambda for real-time processing of IoT traffic files. The **SummarizeWorker** and **ConsolidatorWorker** Lambda functions are triggered automatically when files are uploaded to the S3 bucket (`newbucket37920`).

---

### 1. Create Lambda Functions

1. Open the **AWS Lambda Console**:
   - Log in to your **AWS Management Console**.
   
   - Navigate to the **Lambda** service.

2. Create a New Lambda Function:
   - Click **Create function**.
   - Choose **Author from scratch** and configure the function:
     - **Function Name**: `SummarizeWorker` (repeat for `ConsolidatorWorker`).
     - **Runtime**: Java 11 or Java 17.
     - **Execution Role**: Create or use an IAM role with the following permissions:
       - `AmazonS3FullAccess`
       - `AWSLambdaBasicExecutionRole`
       - `CloudWatchLogsFullAccess`.
   - Click **Create function**.

---

### 2. Deploy the Lambda Code

1. Build the Project:
   - Use Maven to generate the `.jar` file:
     ```bash
     mvn clean package
     ```
   - The compiled `.jar` file will be located in the `target/` directory.

2. Upload the Code to Lambda:
   - Go to the Lambda function in the AWS Console.
   - In the **Code** section, click **Upload from > .zip or .jar file**.
   - Upload the `aws-project-cloud-1.0-SNAPSHOT.jar`.

---

### 3. Configure the Lambda Triggers

1. Add an S3 Trigger:
   - Navigate to the **Configuration > Triggers** tab of the Lambda function.
   - Click **Add trigger** and select **S3**.
   - Configure the trigger:
     - **Bucket**: `newbucket37920`.
     - **Event type**: `PUT` (trigger when a file is uploaded).
     - **Prefix**: Optionally, set a folder prefix (e.g., `raw/` for input files).
   - Save the trigger.

2. Repeat the process for both `SummarizeWorker` and `ConsolidatorWorker`.

---

### 4. Test the Lambda Functions

1. Upload a Test File:
   - Use the `UploadClient` or the AWS Management Console to upload a sample `.csv` file to the `newbucket37920` bucket.

2. Check the Logs:
   - Open **Amazon CloudWatch Logs**.
   - Verify the logs for each Lambda function to ensure the file was processed successfully.

## Configuring and Running the SummarizeWorker on EC2

This section explains how to configure an EC2 instance and run the `SummarizeWorker` Java application.

---

### 1. Configuring and Deploying AWS EC2 for the Java Application

In this project, we use an **AWS EC2 instance** to run the Java application for processing IoT traffic files.

#### Steps to Launch an EC2 Instance

1. **Open the AWS Management Console**:
   - Navigate to the **EC2 Dashboard**.

2. **Create a New EC2 Instance**:
   - Click **Launch Instance** and configure as follows:
     - **Name**: `IoTProcessingInstance`.
     - **AMI**: Select **Amazon Linux 2** or any Java-compatible OS.
     - **Instance Type**: Choose `t2.micro` (free-tier eligible) or higher.
     - **Key Pair**: Select an existing key pair or create a new one for SSH access.
     - **Security Group**: Configure inbound rules to allow:
       - **SSH**: Port 22 (for connecting to the instance).
       - **Custom TCP**: Port 8080 or any port your application requires.
   - Click **Launch Instance**.

#### Install Required Software on EC2

1. **Connect to the EC2 Instance**:
   - Use the following commands to connect via SSH:
     ```bash
     chmod 400 /home/user/project.pem
     ssh -i /home/user/project.pem ec2-user@ec2-54-174-115-182.compute-1.amazonaws.com
     ```
     Replace `/home/user/project.pem` with the path to your `.pem` key file and `ec2-54-174-115-182.compute-1.amazonaws.com` with your instance's public IP.

2. **Install Java and Maven**:
   - Run these commands on the EC2 instance:
     ```bash
     sudo dnf amazon-linux-extras enable corretto8
     sudo dnf install java-11-amazon-corretto -y
     sudo yum install maven -y
     ```

---

### 2. Running the SummarizeWorker on EC2

Once the EC2 instance is configured, you can run the `SummarizeWorker` Java application.

#### Steps to Run the SummarizeWorker

1. **Build the Project on EC2**:
   - Upload the project files to the EC2 instance.
   - Navigate to the project directory and build the application:
     ```bash
     mvn clean package
     ```
   - Confirm that the JAR file (`aws-project-cloud-1.0-SNAPSHOT.jar`) is located in the `target/` directory.

2. **Execute the SummarizeWorker**:
   - Run the following command:
     ```bash
     java -cp target/aws-project-cloud-1.0-SNAPSHOT.jar com.project.lambda.SummarizeWorkerApp newbucket37920 branch001/2025-01-10/data-20221204.csv
     ```
     Replace the parameters as follows:
     - `newbucket37920`: Your S3 bucket name.
     - `branch001/2025-01-10/`: The folder path in the bucket.
     - `data-20221204.csv`: The name of the file to process.

3. **Verify the Results**:
   - Check the EC2 console output to ensure successful processing.
   - Verify the processed file in the S3 bucket, if applicable.

---
This completes the configuration and running of the `SummarizeWorker` on AWS EC2.
