#!/bin/bash

######################################################
# Data transformations
######################################################

cd ../dataset/triples || exit

## WORKAROUND - Replace data prefix (enabling versioning)
printf "\n%s$(date +%FT%T) - [Transformations] Replacement started."

### We set the new data prefix to a versionable compatible one
### FIXME : Autoincrement (fix entity linking)
replacement="@prefix data: <https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel#> ."

### Use the find command to locate all files ending with "split.ttl"
find . -type f -name "*split.ttl" -print0 | while IFS= read -r -d '' file
do
    sed -i '/^@prefix data:/c\'"$replacement" "$file"
done

printf "\n%s$(date +%FT%T) - [Transformations] Replacement completed."

## Transform data as quads
### Use the find command to locate all files ending with "split.ttl"
printf "\n%s$(date +%FT%T) - [Transformations] Version annotation started.\n"

find . -type f -name "*split.ttl" -print0 | while IFS= read -r -d '' file
do
    python3 ../../python/annotate_graph.py "$file" ttl "$file.theoretical.nq" theoretical Villeurbanne
done

find . -type f -name "*.ttl" -print0 | while IFS= read -r -d '' file
do
    python3 ../../python/annotate_graph.py "$file" ttl "$file.relational.nq" relational Villeurbanne
done

printf "\n%s$(date +%FT%T) - [Transformations] Version annotation completed."

## Moving quads
printf "\n%s$(date +%FT%T) - [Transformations] Moving quads started."
mkdir -p ../quads/theoretical
mkdir -p ../quads/relational

cp *.theoretical.nq ../quads/theoretical
cp *.relational.nq ../quads/theoretical
cp *.relational.nq ../quads/relational
printf "\n%s$(date +%FT%T) - [Transformations] Moving quads completed."

## Adds data in test workspace
printf "\n%s$(date +%FT%T) - [Transformations] Copy for test workspace started."
### Adds Villeurbanne tagged data in test workspace

rm -rf ../../src/test/resources/dataset
mkdir -p ../../src/test/resources/dataset
cp ../quads/relational/* ../../src/test/resources/dataset

printf "\n%s$(date +%FT%T) - [Transformations] Copy for test workspace completed."
