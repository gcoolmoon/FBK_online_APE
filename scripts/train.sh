#!/bin/bash

script_dir=$(dirname $0)

source $script_dir/conf_env

SRC_EXT="mtsrc"
TGT_EXT="pe"

DATA_DIR=""
WORK_DIR=""
global_lm=""
local_lm=""

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
        --global_lm)
            global_lm="$2"
            shift 2
            ;;
        --local_lm)
            local_lm="$2"
            shift 2
            ;;
        *)
            echo "Command line error"
            exit 1
            ;;
    esac
done

src="$DATA_DIR/train.$SRC_EXT"
tgt="$DATA_DIR/train.$TGT_EXT"
align_file="$DATA_DIR/align.mt.pe"

cp "$align_file" "$DATA_DIR/align.mt.pe.grow-diag-final-and"

mkdir -p "$WORK_DIR/train/model"

TRAIN_MODEL_OPTS="-alignment grow-diag-final-and \
		-reordering msd-bidirectional-fe \
		-alignment-file $align_file \
		-lm 0:3:$global_lm:1 \
		-lm 0:3:$local_lm:1 \
		-first-step 4 \
		-last-step 8 \
		-cores 2 -mgiza -mgiza-cpus 2 -parallel"

$MOSES_DIR/scripts/training/train-model.perl -external-bin-dir $EXTERNAL_BIN_DIR -root-dir "$WORK_DIR/train" -corpus "$DATA_DIR/train" -e "$TGT_EXT" -f "$SRC_EXT" $TRAIN_MODEL_OPTS > $WORK_DIR/train.out 2> $WORK_DIR/train.err

