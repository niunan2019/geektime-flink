### Fraud Detection Demo with Apache Flink

#### Requirements:
Demo is bundled in a self-contained package. In order to build it from sources you will need:

 - git
 - docker
 - docker-compose

 Recommended resources allocated to Docker:

 - 4 CPUs
 - 8GB RAM

 You can checkout the repository and run the demo locally.

#### How to run:

In order to run the demo locally, execute the following commands which build the project from sources and start all required services, including the Apache Flink and Apache Kafka clusters.

```bash
git clone https://github.com/afedulov/fraud-detection-demo
docker build -t demo-fraud-webapp:latest -f webapp/webapp.Dockerfile webapp/
docker build -t flink-job-fraud-demo:latest -f flink-job/Dockerfile flink-job/
docker-compose -f docker-compose-local-job.yaml up
```

__Note__: Dependencies are stored in a cached Docker layer. If you later only modify the source code, not the dependencies, you can expect significantly shorter packaging times for the subsequent builds.

When all components are up and running, go to `localhost:5656` in your browser.

__Note__: you might need to change exposed ports in _docker-compose-local-job.yaml_ in case of collisions.

