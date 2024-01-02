#!/bin/bash

/bin/bash ./init_stack.sh

/bin/bash ./gratte_ciel/download.sh

/bin/bash ./gratte_ciel/transform.sh

/bin/bash ./gratte_ciel/import_relational.sh

/bin/bash ./gratte_ciel/import_triple.sh
