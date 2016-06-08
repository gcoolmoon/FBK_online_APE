#!/bin/bash

script_dir=$(dirname $0)

source $script_dir/conf_env

lm_path=$1
pe_path=$2
ngram=3

global_count="$lm_path/global.count"
local_count="$lm_path/local.count"
global_lm="$lm_path/global.lm"
global_blm="$lm_path/global.blm"
log="$lm_path/log"

$IRSTLM/add-start-end.sh < $pe_path > $pe_path.sym
pe_path="$pe_path.sym"

if [[ -s $global_count ]]
then
    $IRSTLM/ngt -i=$pe_path -n=$ngram -o="$local_count" >& $log
    $IRSTLM/ngt -i=$global_count -aug=$local_count -n=$ngram -o=$global_count >& $log
    $IRSTLM/tlm -tr=$global_count -n=$ngram -lm=wb -o=$global_lm >& $log
    $IRSTLM/compile-lm $global_lm $global_blm >& $log
    #rm $global_lm
    rm $local_count
    rm $pe_path
else    
    $IRSTLM/ngt -i=$pe_path -n=$ngram -o="$global_count" >& $log
    $IRSTLM/tlm -tr=$global_count -n=$ngram -lm=wb -o=$global_lm >& $log
    $IRSTLM/compile-lm $global_lm $global_blm >& $log
    #rm $global_lm
    rm $local_count
    rm $pe_path
fi


