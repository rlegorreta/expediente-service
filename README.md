# <img height="25" src="./images/AILLogoSmall.png" width="40"/> expediente-service

<a href="https://www.legosoft.com.mx"><img height="150px" src="./images/Icon.png" alt="AI Legorreta" align="left"/></a>
Microservice that acts Customer official document retrieval and storage. It uses `Alfresco` as a document repository.

The `expediente-service` depends on `Alfresco` repository.


## Introduction

This is a microservice handles all back end documents API that includes:

- Handles all service tasks that are declared in the BPMs that approve/denied a document reception process.
- Do all notifications with the microservice auditory `audit-service` to inform about:
    - A documents has some virus (simulation)
    - A document has been accepted by a user task.
    - A document has been declined by a user task
- Update a document using the `Alfresco` repository.

note: Future versions of this microservice will include many other BPMs to support a complete document flow, e.g., 
sign, vote, overdue document, drools, etc. This is just an initial version and more than a POC.

## How to use it

### Send Events

Send an Event via  **Kafka** with the next json structure:

```json
{
  "username":"user1",
  "correlationId": "saveUser_1",
  "eventType":"FULL_STORE",
  "eventName":"saveUser",
  "applicationName":"userServices",
  "eventBody":{
    "notificaFacultad": "NOTIFICA_BUP",
    "datos": {
      ...
    }
  }
}
```

#### Json

The **Event Request** is a **JSON** with the follow properties:

* **template:** The name of the template stored in `Alfresco.
* **to:** eMail to the receiver.
* **body:** Json structure that conform of the **datasource** defined for this **template**.

*__Example:__*

```json

```

### Databases

No database is used directly for this microservice:

- The BPM elasticsearch database is used to handle by the TaskList `Camnuda microservice`.
- The auditory persistence is handle in mongoDB using the `audit-microservice`.
- The document Postgres database is handled by `Alfresco` microservice.
- The bupNeo4j is handled by the `bup-service` microservice.

### Camunda work service

This microservice utilizes the @ZeebeWorker notation from `Camnuda` in order to attend the service task declared in
the document flow BPMs.

The BPMs are stored in a subdirectory /bpms and must be deployed manually using te `Camunda` modeler or in the test 
class DeployBPM


### Events generated

All events generated to `kafka` and are just mail error events

### Events listener

Listener any event with the topic `expediente`

  
## Running on Docker Desktop

### Create the image manually

```
./gradlew bootBuildImage
```

### Publish the image to GitHub manually

```
./gradlew bootBuildImage \
   --imageName ghcr.io/rlegorreta/expediente-service \
   --publishImage \
   -PregistryUrl=ghcr.io \
   -PregistryUsername=rlegorreta \
   -PregistryToken=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
```

### Publish the image to GitHub from the IntelliJ

To publish the image to GitHub from the IDE IntelliJ a file inside the directory `.github/workflows/commit-stage.yml`
was created.

To validate the manifest file for kubernetes run the following command:

```
kubeval --strict -d k8s
```

This file compiles de project, test it (for this project is disabled for some bug), test vulnerabilities running
skype, commits the code, sends a report of vulnerabilities, creates the image and lastly push the container image.

<img height="340" src="./images/commit-stage.png" width="550"/>

For detail information see `.github/workflows/commit-stage.yml` file.


### Run the image inside the Docker desktop

```
docker run \
    --net ailegorretaNet \
    -p 8352:8521 \
    -e SPRING_PROFILES_ACTIVE=local \
    expediente-service
```

Or a better method use the `docker-compose` tool. Go to the directory `ailegorreta-deployment/docker-platform` and run
the command:

```
docker-compose up
```

## Run inside Kubernetes

### Manually

If we do not use the `Tilt`tool nd want to do it manually, first we need to create the image:

Fist step:

```
./gradlew bootBuildImage
```

Second step:

Then we have to load the image inside the minikube executing the command:

```
image load ailegorreta/expediente-service --profile ailegorreta 
```

To verify that the image has been loaded we can execute the command that lists all minikube images:

```
kubectl get pods --all-namespaces -o jsonpath="{..image}" | tr -s '[[:space:]]' '\n' | sort | uniq -c\n
```

Third step:

Then execute the deployment defined in the file `k8s/deployment.yml` with the command:

```
kubectl apply -f k8s/deployment.yml
```

And after the deployment can be deleted executing:

```
kubectl apply -f k8s/deployment.yml
```

Fourth step:

For service discovery we need to create a service applying with the file: `k8s/service.yml` executing the command:

```
kubectl apply -f k8s/service.yml
```

And after the process we can delete the service executing:

```
kubectl deltete -f k8s/service.yml
```

Fifth step:

If we want to use the project outside kubernetes we have to forward the port as follows:

```
kubectl port-forward service/expediente-service 8521:80
```

Appendix:

If we want to see the logs for this `pod` we can execute the following command:

```
kubectl logs deployment/expediente-service
```

### Using Tilt tool

To avoid all these boilerplate steps is much better and faster to use the `Tilt` tool as follows: first create see the
file located in the root directory of the project called `TiltFile`. This file has the content:

```
# Tilt file for expediente-service
# Build
custom_build(
    # Name of the container image
    ref = 'expediente-service',
    # Command to build the container image
    command = './gradlew bootBuildImage --imageName $EXPECTED_REF',
    # Files to watch that trigger a new build
    deps = ['build.gradle', 'src']
)

# Deploy
k8s_yaml(['k8s/deployment.yml', 'k8s/service.yml'])

# Manage
k8s_resource('config-service', port_forwards=['8521'])
```

To execute all five steps manually we just need to execute the command:

```
tilt up
```

In order to see the log of the deployment process please visit the following URL:

```
http://localhost:10350
```

Or execute outside Tilt the command:

```
kubectl logs deployment/expediente-service
```

In order to undeploy everything just execute the command:

```
tilt down
```

To run inside a docker desktop the microservice need to use http://expediente-service:8521 to 8521 path


### Reference Documentation

* [Spring Boot Gateway](https://cloud.spring.io/spring-cloud-gateway/reference/html/)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.0.1/maven-plugin/reference/html/)
* [Config Client Quick Start](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/#_client_side_usage)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/3.0.1/reference/htmlsingle/#production-ready)

### Links to Springboot 3 Observability

https://tanzu.vmware.com/developer/guides/observability-reactive-spring-boot-3/

Baeldung:

https://www.baeldung.com/spring-boot-3-observability



### Contact AI Legorreta

Feel free to reach out to AI Legorreta on [web page](https://legosoft.com.mx).


Version: 2.0.0
©LegoSoft Soluciones, S.C., 2023
