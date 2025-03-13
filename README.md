# Benchmark Test Suites for TrainTicket：A Benchmark Microservice System

This project provides a benchmark test suite for component and integration testing for the open-source microservice System [TrainTicket](https://github.com/FudanSELab/train-ticket)

## Supplementary Material

Supplementary materials, such as a listing of all mentions of issues within the system, can be found in the [Wiki](https://gitlab.lrz.de/marcel_skalski/train-ticket/-/wikis/home).

## General

The project is a train ticket booking system based on a microservice architecture which contains 41 microservices. The
programming languages and frameworks it uses are as follows:

- Java - Spring Boot, Spring Cloud
- Node.js - Express
- Python - Django
- Go - Webgo
- DB - Mongo, MySQL

## Setup for executing Integration Tests

Make sure to run `maven clean` or `mvn clean` and `maven package` or `mvn package` before executing the test cases.

Before running the integration tests, it's necessary to build the Docker images for each service that is called within
this project. A shell script is provided to build the images for all services automatically. You can run the script by 
executing the following command in the root directory that contains all the service directories (Make 
sure to give the script execution permission by running `chmod +x build_images.sh` before executing the script):

```shell
./build_images.sh
```

## Setup for running the Application locally
### Using Docker Compose

The easiest way to get start with the Train Ticket application is by using [Docker](https://www.docker.com/)
and [Docker Compose](https://docs.docker.com/compose/).

> If you don't have Docker and Docker Compose installed, you can refer to [the Docker website](https://www.docker.com/)
> to install them.

### Build Project Files

* Run `maven clean` or `mvn clean` to delete the target directory with all the build data before starting the build process
* Run `maven package` or `mvn package` to package all the modules into JAR files within the project/repository.

### Create / Run Docker Image

After, use command `docker-compose up --build` or `docker compose up` to build images for microservices and run the
system.

### Usage of Application

Once the application starts, you can visit the Train Ticket web page at [http://localhost:8080](http://localhost:8080).


In order to know how to use the application, you can refer
to [the User Guide](https://github.com/FudanSELab/train-ticket/wiki/User-Guide).
