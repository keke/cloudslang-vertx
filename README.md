# CloudSlang Vertx

CloudSlang in Rest API based on [Vertx](vertx.io).

## API

```
POST /{baseUrl}/urest/v1/{cs-flow-name}
```

`ca-flow-name` is the name of CloudSlang flow. The input of the flow is passed via the body of `POST` request which is in `application/json`.

## Config

`TODO`

## Run

### Fat Jar

```
java -jar cloudslang-vertx-{version}-fat.jar
```

### Docker

See [Dockerfile](https://github.com/keke/cloudslang-vertx/blob/master/Dockerfile) for detail

```
docker run -v /local/data:/data -e "CONTENT_PATH=/data/cs" -p 9999:9999 keke/cloudslang-vertx
```
