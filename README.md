# rr-kafka-example
request-response kafka example

![alt text](https://raw.githubusercontent.com/oen9/rr-kafka-example/main/img/rr.png "request-response")

## How to run

### Local with kafka in docker

1. `docker-compose up -d --scale kafka=3 zookeeper kafka`
1. `sbt` and then `reStart`

### Kubernetes

1. `minikube start --cpus=4 --memory=6144`
1. `eval $(minikube docker-env)`
1. `sbt docker:publishLocal`
1. `kubectl apply -f kubernetes/kafka.yaml`
1. `minikube dashboard` wait for kafka and zoo
1. `kubectl apply -f kubernetes/app.yaml`
1. `minikube service gateway-service`

## API

### example request

#### Local

GET `http://localhost:8080/rr/hello`

#### Kubernetes

GET `http://gateway-service-ip:port/rr/hello` e.g. `http http://192.168.99.109:30825/rr/hello`\
using minikube you can `minikube service gateway-service`

### example response

`msg: 'hello' processed`
