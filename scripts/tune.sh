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

src="$DATA_DIR/dev.$SRC_EXT"
tgt="$DATA_DIR/dev.$TGT_EXT"
moses_ini="$WORK_DIR/train/model/moses.ini"
tune_dir="$WORK_DIR/tune"

mkdir -p $tune_dir

$MOSES_DIR/scripts/training/mert-moses.pl $src $tgt $MOSES_DIR/bin/moses $moses_ini --working-dir $WORK_DIR/tune --mertdir $MERT_DIR --batch-mira --return-best-dev --decoder-flags="-threads 5" > $tune_dir/tune.out 2> $tune_dir/tune.err # --batch-mira-args '-J 10'

rm -rf $WORK_DIR/tune/filtered
find $WORK_DIR/tune -type f ! -name 'moses.ini' -delete
