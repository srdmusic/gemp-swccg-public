#!/bin/bash

ls src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set*/*/Card*java | sort | \
    sed -e 's/^.*.Card//' -e 's/\.java$//' | \
    while read raw_id ; do
        setNo=$(echo $raw_id | cut -d "_" -f 1)
        id=$(echo $raw_id | cut -d "_" -f 2- | sed 's/^0*//')
        grep -q "${setNo}_${id}" src/gemp-swccg-async/src/main/web/js/gemp-016/cards/CardImages.js || \
            ls src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set${setNo}/*/Card${raw_id}.java
    done

