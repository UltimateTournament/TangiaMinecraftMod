FROM itzg/minecraft-server
ENV EULA="TRUE"
ENV VERSION="1.18.2"
ENV FORGEVERSION="40.1.68"
ENV TYPE="FORGE"
ENV MEMORY=""
ENV JVM_XX_OPTS="-XX:MaxRAMPercentage=75"
ENV TANGIA_ENV="STAGING"

RUN /start & export pid=$? && while ! mc-health ; do sleep 1 ; done && kill -9 $pid ; true
