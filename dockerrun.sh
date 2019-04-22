docker build -t clientdb:1.0.0 .
docker run -it --rm --expose=9000 -p 7000:7000 clientdb:1.0.0
