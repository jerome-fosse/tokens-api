FROM openjdk:8u111-jdk-alpine

# name, port and mem
ARG app_port
ARG app_name
ARG app_mem
ENV APP_NAME=$app_name
ENV APP_PORT=$app_port
ENV APP_MEM=$app_mem

# install bash
RUN apk update
RUN apk add bash

# install newrelic agent and conf
COPY newrelic /opt/newrelic-java-agent

# timezone
ENV TZ Europe/Paris
RUN apk add -U tzdata
RUN cp /usr/share/zoneinfo/Europe/Paris /etc/localtime

# work dir
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

# properties, launch, conf and lib
COPY properties /usr/src/app/properties
COPY target/$app_name/bin/launch_play.sh /usr/src/app/bin/launch.sh
COPY target/$app_name/bin/$app_name /usr/src/app/bin/$app_name
COPY target/$app_name/conf /usr/src/app/conf
COPY target/$app_name/lib /usr/src/app/lib

# make +x
RUN chmod u+x /usr/src/app/bin/$app_name

EXPOSE $app_port

ENTRYPOINT ["/usr/src/app/bin/launch.sh"]