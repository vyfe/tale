#!/bin/sh
  # todo: backup dbfile
  WORK_DIR=`pwd`
  APP_NAME="tale"
  CODE_DIR="tale-vyfe"
  APP_PROD=${APP_NAME}-prod

  if [ ! -d ${APP_PROD} ];
  then
      mkdir ${APP_PROD}
  fi
  # todo: maven package
  cd $CODE_DIR
  rm -rf target/*
  mvn clean package -Pprod -Dmaven.test.skip=true
  # todo: copy tar from output dir
  if test -f $APP_NAME.tar.gz
  then
	  rm -rf $APP_NAME.tar.gz
  fi

  cp $CODE_DIR/target/dist/$APP_NAME.tar.gz $WORK_DIR
  # 解压部署
  tar -zxvf $APP_NAME.tar.gz -C $APP_PROD && cd $APP_PROD
  chmod +x tool
  echo 'unzip OK'
  sleep 1
  sh tool restart
