#!/bin/bash

script_dir=$(dirname $0)

source $script_dir/conf_env

SRC_EXT="mtsrc"
TGT_EXT="pe"

DATA_DIR=""
WORK_DIR=""

while [[ $# > 1 ]]
do
    case "$1" in
        --data_dir)
            DATA_DIR="$2"
            shift 2
            ;;
        --work_dir)
            WORK_DIR="$2"
            shift 2
            ;;
        *)
            echo "Command line error"
            exit 1
            ;;
    esac
done

tgt="$DATA_DIR/train.$TGT_EXT"
$IRSTLM/add-start-end.sh < "$tgt" > "$tgt.add-sym"
tgt="$tgt.add-sym"
lm="$WORK_DIR/lm/local.lm"
blm="$WORK_DIR/lm/local.blm"

mkdir "$WORK_DIR/lm"

$IRSTLM/tlm -tr="$tgt" -n=3 -lm=wb -bo=yes -o="$lm" >& "$WORK_DIR/lm/log"
$IRSTLM/compile-lm "$lm" "$blm" >& "$WORK_DIR/lm/log"

rm "$lm"
rm "$tgt"

