import json
import pdb
import subprocess

kBatchConfpath = "conf/batch.conf"
kConfpath = "conf/xpre.conf"
kMaxsizeConf = 1024 * 1024
kFilepathBatchResult = "output/batchResult"
kFilepathBatchReport = "output/batchReport"

def call(cmd, forceSucc=False) :
    print "exec cmd[%s]" % cmd
    try :
        return subprocess.check_output(cmd, shell=True)
    except Exception, e:
        if forceSucc == True :
            assert False

def readBatchConf() :
    fin = open(kBatchConfpath, "r")
    result = json.load(fin)
    fin.close()
    return result

def applyConf(confItem) :
    fin = open(kConfpath, "r")
    confRepr = fin.read(kMaxsizeConf)
    lines = confRepr.split('\n')
    fin.close()

    fout = open(kConfpath, "w")
    for line in lines :
        written = False
        for key in confItem.keys() :
            val = confItem[key]
            confRepr = applySingleConf(line, key, val)
            if confRepr != None :
                fout.write(confRepr + '\n')
                written = True
                break

        if written == False : 
            fout.write(line + '\n')
    fout.close()

def applySingleConf(line, key, val) :
    if line.find(" %s=" % key) == -1 and line.find(" %s " % key) == -1 :
        return None

    if type(val) is int :
        return "  %s = %s" % (key, val)
    raise Exception("fail_apply_single_conf[%s|%s]" % (line, key))

def extractOutputs(outputItems) :
    lastLine = ""
    for line in open(kFilepathBatchResult, "r") :
        if line[-1] == '\n' : line = line[:-1]
        lastLine = line

    fout = open(kFilepathBatchReport, "a")
    items = lastLine.split(' ')    
    for outputItem in outputItems :
        target = False
        for item in items :
            if item[ : len(outputItem) + 1] == "%s[" % outputItem :
                fout.write(item[len(outputItem) + 1 : -1] + ',')
                target = True
        
        if target == False :
            raise Exception("unknown_output_item[%s]" % outputItem)

    fout.write('\n')
    fout.close()

if __name__ == "__main__" :
    call("cp %s %s.bak" % (kConfpath, kConfpath))
    call(":> %s" % kFilepathBatchReport)
    conf = readBatchConf()
    for confItem in conf :
        call("mkdir output")
        applyConf(confItem)
        #call("python script/server.py start > %s" % kFilepathBatchResult)
        outputItems = confItem["output"].split(",")
        extractOutputs(outputItems)
