#! /bin/sh

rsync --recursive --progress --verbose --exclude=".svn" --rsh="ssh -l simonebordet" website/htdocs/* simonebordet@shell.sourceforge.net:/home/groups/f/fo/foxtrot/htdocs
