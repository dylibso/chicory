# Build using the command:
# docker build -f jmh/Dockerfile . -t docker.io/andreatp/chicory-show-jmh
FROM node

RUN apt update && apt install zip wget -y
RUN npm install http-server -g

COPY scripts/show-results.sh .

ENTRYPOINT ["./show-results.sh"]
