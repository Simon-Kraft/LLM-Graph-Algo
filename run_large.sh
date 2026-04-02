#!/bin/bash
for s in 15 16 17 18; do
    echo "Running RMAT scale $s..."
    java -cp bin Main -1 -r $s -o results/opt_rmat${s}_$(date +%Y%m%d_%H%M).txt
done
echo "Done."