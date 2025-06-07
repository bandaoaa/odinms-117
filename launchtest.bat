@echo off
@title MapleStory Server GMS ver.117.2
Color 0

:StartServer
PATH=ms-21.0.7\bin;%PATH%;
set CLASSPATH=.;target\*
java -Xms128m -Xmx512m -server -XX:+UseG1GC -Dfile.encoding=GBK -Dgraalvm.js.allowMultiThreaded=true -Dnet.sf.odinms.wzpath=wz server.Start
pause