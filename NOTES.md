# Corda Dev Notes

## Running the code
To run the code you will need to do the following

In the root directory
```text
$ ./gradlew clean deployNodes
```
The above command will build the node code.
If the above command throws the error: 
```text
$ ./gradlew: Permission denied
```
Then change permissions for the gradlew file using
```text
$ chmod 755 gradlew
```
If another error occurs which says
```text
> Could not resolve all files for configuration ':runtime'.
``` 

Then run the nodes like this:
```text
$ build/nodes/runnodes
```

Then we need to start the webserver that will serve the frontend and the REST API.
```text
$ ./gradlew runTemplateServer
```

There is also a client commandline example that can be run as follows:
```text
$ ./gradlew runTemplateClient
```

## Add the Token SDK dependencies
See [github reference](https://github.com/corda/cordapp-template-kotlin/blob/token-template/build.gradle)
and [token sdk](https://github.com/corda/token-sdk)

These token libraries allow you to make use of the token support. 

## Testing
For testing and running individual tests you will need to install Quasar

```text
$ ./gradlew installQuasar
```


Before creating the IntelliJ run configurations for these unit tests go 
to Run -> Edit Configurations -> Defaults -> JUnit, add `-javaagent:lib/quasar.jar` to 
the VM options, and set Working directory to `$PROJECT_DIR$` so that the Quasar 
instrumentation is correctly configured.

## Install Oracle JDK 8
check this: https://www3.ntu.edu.sg/home/ehchua/programming/howto/JDK_Howto.html

```shell script
wget -O jdk-8u221-linux-x64.tar.gz \
  -c --content-disposition \
  "https://javadl.oracle.com/webapps/download/AutoDL?BundleId=239835_230deb18db3e4014bb8e3e8324f81b43"
```
```
$ cd /usr/local
$ sudo mkdir java
```
```
$ cd /usr/local/java
$ sudo tar xzvf ~/Downloads/jdk-8u221-linux-x64.tar.gz
```
```shell script
$ cd /etc
$ sudo vi profile
```
Add these to end of file
```shell script
export JAVA_HOME=/usr/local/java/jdk1.8.0_221
export PATH=$JAVA_HOME/bin:$PATH
```
```shell script
$ source profile
```
## Next steps
Now I need to embelish the REST API and look at adding a React frontend.

