#!/bin/bash

######################################################
# Download the dataset from the LIRIS dataset server
######################################################

printf "\n%s$(date +%FT%T) - [Download] Dataset download started."

rm -rf ../dataset

mkdir -p ../dataset/triples

cd ../dataset/triples || exit

## Get the versions
curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2018_split.ttl' \
  --output 'GratteCiel_2018_split.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2015_split.ttl' \
  --output 'GratteCiel_2015_split.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2012_split.ttl' \
  --output 'GratteCiel_2012_split.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2012_alt_split.ttl' \
  --output 'GratteCiel_2012_alt_split.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2009_split.ttl' \
  --output 'GratteCiel_2009_split.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2009_alt_split.ttl' \
  --output 'GratteCiel_2009_alt_split.ttl'


## Get the workspace
curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2009_2018_Workspace.ttl' \
  --output 'GratteCiel_2009_2018_Workspace.ttl'

## Get the version transitions
curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/Transition_2009_2009b.ttl' \
  --output 'Transition_2009_2009b.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/Transition_2009_2012.ttl' \
  --output 'Transition_2009_2012.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/Transition_2009b_2012b.ttl' \
  --output 'Transition_2009b_2012b.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/Transition_2012_2015.ttl' \
  --output 'Transition_2012_2015.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/Transition_2012b_2015.ttl' \
  --output 'Transition_2012b_2015.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/Transition_2015_2018.ttl' \
  --output 'Transition_2015_2018.ttl'

printf "\n%s$(date +%FT%T) - [Download] Dataset download completed."