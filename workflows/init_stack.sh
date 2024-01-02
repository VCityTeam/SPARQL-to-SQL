#!/bin/bash

cd ..

docker compose down

for docker_volume_id in $(docker volume ls -q | grep sparql-to-sql)
do
   docker volume rm "$docker_volume_id"
done

docker compose up -d

