# SonarQube Over SCM
Import / export SonarQube's quality profiles and gates with SCM.

## Introduction
SonarQube is an open source platform for continuous inspection of code quality to perform automatic reviews with static analysis of code to detect bugs, code smells and security vulnerabilities on 20+ programming languages.  
SonarQube uses “Quality Profiles” and “Quality Gates” to analyze your project.  
Quality Profiles allows you to define the requirements from the project by defining sets of rules, and with Quality Gate you can define set of Boolean conditions based on measure thresholds against which projects are measured.

SonarQube currently provides the ability to back up and restore Quality Profiles but not of Quality Gates. In addition, this option does not support integration with SCM tools (like Git).

**The purpose of this project is to enable:**
- *export* of Quality Profiles/Gates from SonarQube to Git.
- *import* of Quality Profiles/Gates from Git to SonarQube.

**Project structure:**
- The folder `vars` contains groovy scripts that use the existing SonarQube API to import/export profiles/gates.
- The folders `JenkinsJobs` and `JenkinsFiles` contains the configuration of Jenkins, through which we integrated the export and import operations with GIT.

## Table of Contents
- [Introduction](#introduction)
- [The Groovy Scripts](#the-groovy-scripts)
  * [ExportProfile.groovy](#exportprofilegroovy)
  * [ImportProfile.groovy](#importprofilegroovy)
  * [ExportGate.groovy](#exportgategroovy)
  * [ImportGate.groovy](#importgategroovy)
- [Integration With Jenkins](#integration-with-jenkins)
  * [Define Shared Library](#define-shared-library)
  * [Define Node](#define-node)
  * [Define Pipeline](#define-pipeline)
- [The Jenkins Jobs](#the-jenkins-jobs)
  * [Your project structure and some preparations](#your-project-structure-and-some-preparations)
  * [Export Profile Job](#export-profile-job)
  * [Import Profile Job](#import-profile-job)
  * [Export Gate Job](#export-gate-job)
  * [Import Gate Job](#import-gate-job)
  * [Error Handling](#error-handling)
 - [Authors](#authors)

## The Groovy Scripts

### ExportProfile.groovy

#### *Description:*
Export Quality Profiles *from* SonarQube *to* the folder that the `workspace` parameter represents.    

- This operation has 2 options:
 
  **Option 1:** Export all Quality Profiles that associated with a specific project.
  - This group of profiles includes:  
    - The profiles from the last scan of the project (appears on the left side of the project page, under the title "Quality Profiles").
    - The profiles currently associated to the project (appears in project page -> "Administration" -> "Quality Profiles", and includes only the profiles that correspond to the programming languages used by the project).  
  -  To use this option, the parameter `projectKey` must be passed, but not the parameters `profileName` and `language`.

  **Option 2:** Export specific Quality Profile, given the profile name and its corresponding programming language.
  - To use this option, the parameters `profileName` and `language` must be passed, but not the parameter `projectKey`.  
 
 - Each quality profile is saved under the name `profileName_language.xml`, where `profileName` is the name of the profile and `language` is the profile language.
 
 - In case of error - an error message is printed (starting with "sonar: " and then the error description) and the program ends. Examples of errors: Project does not exist, profile does not exist, unknown language, etc.
  
#### *Arguments:*
| Argument                 | Description
| ------------------------ | -----------------------------------------------------------
| `String server`          | SonarQube server.
| `String workspace`       | Name of the directory where the quality profiles will save.
| `String projectKey`      | The project key from SonarQube. <br/> Note: Use this argument only for 'option 1'.
| `String profileLanguage` | The quality profile language. <br/> One of the following values: "cs" (C#), "flex" (Flex),   "java" (Java), "js" (JavaScript), "php" (PHP), "py" (Python). <br/> Note: Use this argument only for 'option 2'.
| `String profileName`     | The quality profile name. <br/> Note: Use this argument only for 'option 2'.

#### *Usage:*
```groovy
// Option 1:
ExportProfile(String server, String workspace, String projectKey)
// Option 2:
ExportProfile(String server, String workspace, String profileLanguage, String profileName)
```

#### *Examples:*
```groovy
// Option 1:
ExportProfile("http://localhost:9000", "profilesDirectory", "myProjectKey")
// Option 2:
ExportProfile("http://localhost:9000", "profilesDirectory", "java", "Sonar way")
```

### ImportProfile.groovy

#### *Description:*
Import Quality Profile *from* the folder that the `workspace` parameter represents *to* SonarQube. 

- This operation has 2 options depending on the authentication method: 

  **Option 1:** User Token. 
  -  To use this option, the parameter `token` must be passed, but not the parameters `username` and `password`.  

  **Option 2:** Username + password.
  - To use this option, the parameters `username` and `password` must be passed, but not the parameter `token`.  

 - The input file that represents the quality profile (`fileName`) must be valid: It must be the output of the previous ExportProfile command (or of the SonarQube's built-in "restore" operation).
 
 - If SonarQube already has a quality profile with the same name as the imported profile, this profile will be overwritten, but its association with projects will not be affected.
 
 - In case of error - an error message is printed (starting with "sonar: " and then the error description) and the program ends. Examples of errors: Unauthorized, etc.

#### *Arguments:*
| Argument                 | Description
| ------------------------ | -----------------------------------------------------------
| `String server`          | SonarQube server.
| `String workspace`       | Name of the directory from which the quality profile is imported.
| `String fileName`        | Name of the (xml) file that represent the quality profile.
| `String token`           | User Token from SonarQube. <br/> Note: Use this argument only for 'option 1'.
| `String username`        | User name from SonarQube. <br/> Note: Use this argument only for 'option 2'.
| `String password`        | Password. <br/> Note: Use this argument only for 'option 2'.

#### *Usage:*
```groovy
// Option 1:
ImportProfile(String server, String workspace, String fileName, String token)
// Option 2:
ImportProfile(String server, String workspace, String fileName, String username, String password)
```

#### *Examples:*
```groovy
// Option 1:
ExportProfile("http://localhost:9000", "profilesDirectory", "Sonar way_java.xml", "myToken")
// Option 2:
ExportProfile("http://localhost:9000", "profilesDirectory", "Sonar way_java.xml", "admin", "admin")
```

### ExportGate.groovy

#### *Description:*
Export Quality Gate *from* SonarQube *to* the folder that the `workspace` parameter represents.    

- This operation has one option: Export specific Quality Gate given its name.

- The quality gate is saved under the name `outputFileName.json`, where `outputFileName` is one of the arguments.
 
- In case of error - an error message is printed (starting with "sonar: " and then the error description) and the program ends. Examples of errors: Gate does not exist, etc.
  
#### *Arguments:*
| Argument                 | Description
| ------------------------ | -----------------------------------------------------------
| `String server`          | SonarQube server.
| `String workspace`       | Name of the directory where the quality gate will save.
| `String gateName`        | The quality gate name. 
| `String outputFileName`  | The name of the output file.

#### *Usage:*
```groovy
ExportGate(String server, String workspace, String gateName, String outputFileName)
```

#### *Examples:*
```groovy
ExportGate("http://localhost:9000", "gatesDirectory", "SonarQube way", "myOutputGate")
```

### ImportGate.groovy

#### *Description:*
Import Quality Gate *from* the folder that the `workspace` parameter represents *to* SonarQube. 

- This operation has 2 options depending on the authentication method: 

  **Option 1:** User Token. 
  -  To use this option, the parameter `token` must be passed, but not the parameters `username` and `password`.  

  **Option 2:** Username + password.
  - To use this option, the parameters `username` and `password` must be passed, but not the parameter `token`.  

 - The input file that represents the quality gate (`fileName`) must be valid: It must be the output of the previous ExportGate command.
 
 - If SonarQube already has a quality gate with the same name as the imported gate, this gate will be overwritten, but its association with projects will not be affected.
 
 - In case of error - an error message is printed (starting with "sonar: " and then the error description) and the program ends. Examples of errors: Unauthorized, etc.

#### *Arguments:*
| Argument                 | Description
| ------------------------ | -----------------------------------------------------------
| `String server`          | SonarQube server.
| `String workspace`       | Name of the directory from which the quality gate is imported.
| `String fileName`        | Name of the (json) file that represent the quality gate.
| `String token`           | User Token from SonarQube. <br/> Note: Use this argument only for 'option 1'.
| `String username`        | User name from SonarQube. <br/> Note: Use this argument only for 'option 2'.
| `String password`        | Password. <br/> Note: Use this argument only for 'option 2'.

#### *Usage:*
```groovy
// Option 1:
ImportGate(String server, String workspace, String fileName, String token)
// Option 2:
ImportGate(String server, String workspace, String fileName, String username, String password)
```

#### *Examples:*
```groovy
// Option 1:
ImportGate("http://localhost:9000", "gatesDirectory", "SonarQube way.json", "myToken")
// Option 2:
ImportGate("http://localhost:9000", "gatesDirectory", "SonarQube way.json", "admin", "admin")
```

## Integration With Jenkins
With **Jenkins**, you can manage the import/export of Quality Profiles and Quality Gates **as part of your project's build**.

To do this, you need to configure Jenkins and perform the following steps:
1. Define a "Shared Library".
2. Define a "Node".
3. Define 4 "Pipeline"s - one for each of the 4 possible actions (this step can be done by importing existing Jenkins's jobs).
4. Run the Pipelines and see the result in SonarQube and Git.

### Define Shared Library
**Introduction:** 
- By defining a Shared Library, we can link Jenkins to our 4 groovy scripts stored in the `vars` folder. <br/>
  Therefore, to perform this step, you must set Git project repository where the folder `vars` is stored.

**Configuration instructions:**
1. From the Jenkins main page, go to *"Manage Jenkins"* → *"Configure System"*.
2. Under *"Global Pipeline Libraries"*, add a library with the following settings:
   - Library	Name: An identifier you pick for this library, to be used in the @Library annotation in the JenkinsFiles.
     - In this project we will use the example "sonarqubeLibrary".
   - Default version: A default version of the library to load (example: "master"). 
   - Retrieval method: Select "Modern SCM".
   - Source Code Management: Select "Git	Project Repository" and add URL of the remote repository that store the folder `vars` (see "Introduction").

**Usage:**
- To use the Shared Library in a pipeline, add `@Library("sonarqubeLibrary")_` to the top of your JenkinsFile. <br/>
  For more examples, see the folder `JenkinsFiles`.

### Define Node
**Introduction:** 
- To use "Scripted Pipeline" (as shown in the folder `JenkinsFiles`), you need to define a new "Node" for the project.

**Configuration instructions:**
1. From the Jenkins main page, go to *"Manage Jenkins"* → *"Manage Nodes"* → *"New Node"*.
2. Add a node with the following settings:
   - Node name: An identifier you pick for this node, to be used in the JenkinsFiles.
     - In this project we will use the example "SoanrQubeNode".
   - Select "Permanent Agent".
   - Click the "OK" button.
3. A nodes configuration page will be displayed. Fill the following settings:
   - Remote root directory: Path to this directory on the agent (example: "C:\Program Files (x86)\Jenkins").
   - Launch method: Select "Launch agent via Java Web Start".
     - If this option isn't available: Go to *"Manage Jenkins"* → *"Configure Global Security"* and enable the "TCP port of JNLP agents" option.
  
**Connect agent to Jenkins:** (This step must be performed every time you log in to Jenkins, otherwise you can not run the jobs that use this node)
1. From the Jenkins main page, go to *"Manage Jenkins"* → *"Manage Nodes"*.
2. Click on the newly created slave machine (our Node "SoanrQubeNode") and follow the instructions.  

### Define Pipeline
**Introduction:** 
- There are 2 ways to set up a directory: the first is manually by configuring the pipeline, and the second is by importing an existing job. We will explain the two ways.

**Configuration instructions:**
1. From the Jenkins main page, go to *"New Job"*.
2. A new job creation page will be displayed. Fill the following settings:
   - Enter an item name (example: "ourPipeline").
   - Select the "Pipeline" option.
   - Click the "OK" button.
3. A job configuration page will be displayed. Fill the following settings:
   - Under *"General"*: 
     - Select "This project is parameterized".
     - To use a pipeline script from SCM, add the following two string parameters:
       1. GIT_URL: The URL where you store the project files (the project that scanned by SonarQube) and the JenkinsFiles (under the directory `JenkinsFiles`).
       2. BRANCH_NAME: The name of the branch in your Git project where the changes will be made (for example: After running ExportProfile - the new Quality Profiles files will be saved in this branch only).
   - Under *"Pipeline"*:
     - Definition: Select "Pipeline script from SCM".
     - SCM: Select "Git".
     - Repositories: Under "Repository URL" enter "${GIT_URL}" and fill "Credentials" if necessary.
     - Branches to build: Under "Branch Specifier" enter "${BRANCH_NAME}".
     - Script Path: Enter "JenkinsFiles/ourJenkinsfile", where "ourJenkinsfile" is the JenkinsFile that corresponds to the current job.
     - Don't select "Lightweight checkout".

**Import job instructions:**
1. From the folder `JenkinsJobs`:
   - Choose the job that you want to import, and save it under some folder, for example "myFolder". 
2. From the Jenkins main page, go to *"Manage Jenkins"* → *"Configure Global Security"*.
   - Enable the "Enable security" option.
   - Under "Authorization" select "Anyone can do anything".
3. From the Jenkins main page, go to *"Manage Jenkins"* → *"Jenkins CLI"* and Select "create-job".
4. Download "jenkins-cli.jar" to "myFolder", open a terminal in this folder, and then run the required commands of "create-job". 

## The Jenkins Jobs

### Your project structure and some preparations

Before we give details about any of our 4 jobs, we will explain the **structure** that required from a project that wants to use those jobs.
- First, you must create a repository of the project. Next, we'll call it `GIT_URL`.
- Then, you must select a branch within the previous `GIT_URL`, where the changes will be made (adding profiles files, etc.). Next, we'll call it `BRANCH_NAME`.
- In addition, your repository should include several folders:
  - A folder named `JenkinsFiles` - where the JenkinsFiles will be saved (i.e., you can copy the `JenkinsFiles` folder as it appears in our project to your repository).
  - A folder for the Quality Profile files. Next, we'll call it `PROFILES_DIRECTORY`.
  - A folder for the Quality Gate files. Next, we'll call it `GATES_DIRECTORY`.

Note: You do not have to include the `vars` folder in your project - when you define the Shared Library you can use our repository.

### Export Profile Job

#### *Description:*
Export Quality Profiles *from* SonarQube *to* the folder that the `PROFILES_DIRECTORY` parameter represents.

This operation has 2 options:
1. Export all Quality Profiles that associated with a specific project.
2. Export specific Quality Profile, given the profile name and its corresponding programming language.
  
#### *Arguments:*
| Argument                 | Description
| ------------------------ | -----------------------------------------------------------
| `GIT_URL`                | Name of a repository that should contain your project.
| `BRANCH_NAME`            | Name of a branch inside `GIT_URL` that will contain the changes caused by the job.
| `SONARQUBE_SERVER`       | SonarQube server.
| `PROFILES_DIRECTORY`     | Name of the directory inside `GIT_URL` where the quality profiles will save.
| `PROJECT_KEY`            | The project key from SonarQube. <br/> Note: Use this argument only for 'option 1'.
| `PROFILE_LANGUAGE`       | The quality profile language. <br/> One of the following values: "cs" (C#), "flex" (Flex),   "java" (Java), "js" (JavaScript), "php" (PHP), "py" (Python). <br/> Note: Use this argument only for 'option 2'.
| `PROFILE_NAME`           | The quality profile name. <br/> Note: Use this argument only for 'option 2'.

### Import Profile Job

#### *Description:*
Import Quality Profile *from* the folder that the `PROFILES_DIRECTORY` parameter represents *to* SonarQube.

This operation has 2 options depending on the authentication method: 
1. User Token.
2. Username + password.
  
#### *Arguments:*
| Argument                 | Description
| ------------------------ | -----------------------------------------------------------
| `GIT_URL`                | Name of a repository that should contain your project.
| `BRANCH_NAME`            | Name of a branch inside `GIT_URL` that will contain the changes caused by the job.
| `SONARQUBE_SERVER`       | SonarQube server.
| `PROFILES_DIRECTORY`     | Name of the directory inside `GIT_URL` from which the quality profile is imported
| `INPUT_FILENAME`         | Name of the (xml) file that represent the quality profile.
| `TOKEN`                  | User Token from SonarQube. <br/> Note: Use this argument only for 'option 1'.
| `USERNAME`               | User name from SonarQube. <br/> Note: Use this argument only for 'option 2'.
| `PASSWORD`               | Password. <br/> Note: Use this argument only for 'option 2'.

### Export Gate Job

#### *Description:*
Export Quality Gate *from* SonarQube *to* the folder that the `GATES_DIRECTORY` parameter represents.    

This operation has one option: Export specific Quality Gate given its name.
  
#### *Arguments:*
| Argument                 | Description
| ------------------------ | -----------------------------------------------------------
| `GIT_URL`                | Name of a repository that should contain your project.
| `BRANCH_NAME`            | Name of a branch inside `GIT_URL` that will contain the changes caused by the job.
| `SONARQUBE_SERVER`       | SonarQube server.
| `GATES_DIRECTORY`        | Name of the directory inside `GIT_URL` where the quality gate will save.
| `GATE_NAME`              | The quality gate name.
| `OUTPUT_FILENAME`        | The name of the output file.

### Import Gate Job

#### *Description:*
Import Quality Gate *from* the folder that the `GATES_DIRECTORY` parameter represents *to* SonarQube.

This operation has 2 options depending on the authentication method: 
1. User Token.
2. Username + password.
  
#### *Arguments:*
| Argument                 | Description
| ------------------------ | -----------------------------------------------------------
| `GIT_URL`                | Name of a repository that should contain your project.
| `BRANCH_NAME`            | Name of a branch inside `GIT_URL` that will contain the changes caused by the job.
| `SONARQUBE_SERVER`       | SonarQube server.
| `GATES_DIRECTORY`        | Name of the directory inside `GIT_URL` from which the quality gate is imported.
| `INPUT_FILENAME`         | Name of the (json) file that represent the quality gate.
| `TOKEN`                  | User Token from SonarQube. <br/> Note: Use this argument only for 'option 1'.
| `USERNAME`               | User name from SonarQube. <br/> Note: Use this argument only for 'option 2'.
| `PASSWORD`               | Password. <br/> Note: Use this argument only for 'option 2'.

### Error Handling
- **SonarQube errors:** There are errors caused by incorrect use of SonarQube. 
  - For example, pass profile name to a profile that does not exist. 
  - In this case, the pipeline fails and prints an error message that starts with the string *"sonar:"* and then  details the error.
- **Pipeline errors:** Errors caused by passing incorrect number of parameters.
  - For example, in ImportProfile: `token` and `username`+`password` instead of just one option.
  - In this case, the pipeline fails and prints an error message that starts with the string *"pipeline:"* and then  details the error.
- **Jenkins errors:** Errors caused by passing incorrect use of Jenkins. 
  - For example, pass `GIT_URL` that does not exist. 
  - In this case, the pipeline fails and Jenkins prints a detailed error message.

## Authors
[Meshi Fried](https://github.com/MeshiFried) and [Ilanit Smul](https://github.com/IlanitSmul)
