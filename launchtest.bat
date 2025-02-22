@echo off
@title MapleStory Server GMS ver.117.2
Color 0

:StartServer
PATH=jdk-1.8\bin;%PATH%;
set CLASSPATH=.;target\*
java -Xms128m -Xmx4g -server -XX:+UseG1GC -Dnet.sf.odinms.wzpath=wz server.Start
pause