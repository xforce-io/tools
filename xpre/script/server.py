#!/usr/bin/python

import os, subprocess, sys

mod_name = "xpre"
version_num = "1.0"
jar_name = "%s-%s" % (mod_name, version_num)
jar_filename = "%s.jar" % jar_name

def Stop() :
  cmd = "ps aux | grep %s | grep java | awk '{ print $2 }' | xargs -i kill -15 {}" % jar_name
  try :
    subprocess.check_output(cmd, shell=True)
  except Exception, e :
    pass

def Shutup() :
  cmd = "ps aux | grep %s | grep java | awk '{ print $2 }' | xargs -i kill -9 {}" % jar_name
  try :
    subprocess.check_output(cmd, shell=True)
  except Exception, e :
    pass

def Start() :
  cmd = "java -Xmx32768m -Djava.library.path=lib/ -Xloggc:logs/gc.log -jar %s" % jar_filename
  os.system(cmd)

def Init() :
  cmd = "java -Xmx32768m -Djava.library.path=lib/ -Xloggc:logs/gc.log -jar %s init" % jar_filename
  os.system(cmd)

if __name__ == "__main__" :
  if len(sys.argv) != 2 or \
      (   sys.argv[1] != "stop" and \
          sys.argv[1] != "start" and \
          sys.argv[1] != "restart" and \
          sys.argv[1] != "shutup" and \
          sys.argv[1] != "init") :
    print("Usage: ./server [stop|start|restart|shutup|init]")
  elif sys.argv[1] == "stop" :
    Stop()
  elif sys.argv[1] == "start" :
    Start()
  elif sys.argv[1] == "shutup" :
    Shutup()
  elif sys.argv[1] == "restart":
    Stop(); Start()
  elif sys.argv[1] == "init" :
    Init()
