from pathlib import Path
import subprocess,os
r=Path(__file__).resolve().parents[1];os.chdir(r);out=r/'build/session-classes';out.mkdir(parents=True,exist_ok=True)
cp=str(r/'tests/json-jvm.jar');src=r/'app/src/main/java/cn/qingzhuan/app'
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-cp',cp,'-d',str(out),str(src/'Core.java'),str(src/'FileProbe.java'),str(src/'SessionStore.java'),str(src/'TaskLedger.java'),str(src/'DocumentInbox.java'),str(src/'RecoveryPolicy.java'),str(src/'StartupReportStore.java'),str(src/'ResultSelection.java'),str(src/'FeedbackClock.java'),str(src/'GestureGate.java'),str(src/'ToolRouting.java'),str(src/'ConversionOutcomes.java'),'tests/OutcomeTest.java','tests/RoutingTest.java','tests/FeedbackClockTest.java','tests/SelectionTest.java','tests/SessionTest.java','tests/StartupReportTest.java'],check=True)
subprocess.run(['java','-cp',str(out)+':'+cp,'cn.qingzhuan.app.SessionTest'],check=True)
subprocess.run(['java','-cp',str(out)+':'+cp,'cn.qingzhuan.app.StartupReportTest'],check=True)

subprocess.run(['java','-cp',str(out)+':'+cp,'cn.qingzhuan.app.SelectionTest'],check=True)

subprocess.run(['java','-cp',str(out)+':'+cp,'cn.qingzhuan.app.FeedbackClockTest'],check=True)

subprocess.run(['java','-cp',str(out)+':'+cp,'cn.qingzhuan.app.RoutingTest'],check=True)

subprocess.run(['java','-cp',str(out)+':'+cp,'cn.qingzhuan.app.OutcomeTest'],check=True)
