#!/bin/sh

  WORK_DIR=`pwd`
  APP_NAME="tale"
  CODE_DIR="tale-vyfe"
  APP_PROD=${APP_NAME}-prod

  # todo: backup dbfile
  time2=$(date "+%Y%m%d%H")
  backfile=tale.db."${time2}"
  # 存在则不备份
  if [ ! -f backup/${backfile} ];
  then
    cp $APP_PROD/resources/tale.db /home/work/backup/"${backfile}"
  fi


  if [ ! -d ${APP_PROD} ];
  then
      mkdir ${APP_PROD}
  fi
  # maven package
  cd $CODE_DIR
  rm -rf target/*
  mvn clean package -Pprod -Dmaven.test.skip=true
  # todo: copy tar from output dir
  cd $WORK_DIR
  if test -f $APP_NAME.tar.gz
  then
	  rm -rf $APP_NAME.tar.gz
  fi

  cp $CODE_DIR/target/dist/$APP_NAME.tar.gz $WORK_DIR
  # 解压部署
  cd $APP_PROD && sh tool stop
  echo 'stopped'
  cd $WORK_DIR
  rm -rf $APP_PROD/*

  tar -zxvf $APP_NAME.tar.gz -C $APP_PROD && cd $APP_PROD
  chmod +x tool
  echo 'unzip OK'
  # replace schema.sql
  cd $WORK_DIR/backup && sqlite3 ${backfile} .dump > ${backfile}.sql
  cd $WORK_DIR
  mv backup/${backfile}.sql $APP_PROD/resources/schema.sql
  # install.lock(stop repeat install)
  sleep 2
  touch $APP_PROD/resources/install.lock
  sleep 1
  cd $APP_PROD && sh tool restart
