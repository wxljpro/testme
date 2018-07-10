#!/bin/bash
while getopts "a:b" args
do
    case $args in 
        a) echo "your val is $OPTARG" ;;
        b) echo "your val is b" ;;
        ?) echo "no this is param";exit 1  ;;
    esac
done
