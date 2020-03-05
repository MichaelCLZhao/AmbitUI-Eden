/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ambitui.Test;

import ambitui.Telnet.TelnetOper;
import ambitui.AmbitUIView;
import ambitui.BaseClass;
import ambitui.ComPort;
import ambitui.ConfigFile.PnConfig;
import ambitui.ProcessOper;
import ambitui.ConfigFile.StationInfo;
import ambitui.RobotWindow.User32;
import ambitui.RobotWindow.W32API;
import com.alibaba.fastjson.JSONObject;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

/**
 *
 * @author lihaibin
 */
public class TestScript {

    public String resultLog;
    AmbitUIView apv;
    TelnetOper to = new TelnetOper();
    ProcessOper po;
    BaseClass bc = new BaseClass();
    PnConfig pc = new PnConfig();
    char[] cstop = new char[]{0x03};
    String[] question = new String[]{
        "系统灯是否为蓝色？",//0
        "请拔掉  HDMI线  和  网线,",//1
        "接上 CVBS线",//2
        "LED灯是否熄灭？",//3
        "LED灯是否闪烁？",//4
        "LED灯是否开启？",//5
        "LED是否蓝灯？",//6
        "LED灯是否绿灯？",//7
        "LED灯是否红灯？",//8
        "LED灯是否白灯？",//9
        "请拔掉网线",//10
        "请按住remote键",//11
        "HDMI是否有视频输出？",//12
        "HDMI是否有音频输出？",//13
        "视频播放是否正常？",//14
        "S/PDIF是否有音频输出？",//15
        "请拔掉HDMI线,连接S/PDIF线.",//16
        "请按下reset按钮",//17
        "请按下遥控器",//18
        "Led是否闪烁？",//19
        "请切换HDMI线.",//20
        "请接上电源线.",//21
    };

    public TestScript(AmbitUIView apv) {
        this.apv = apv;
        po = new ProcessOper(apv);
    }

    public boolean ping(String ip, int times) {
        if (po.ping(ip, times)) {
            return true;
        }

        return false;
    }

    public void addLog(String str, int id) {
        apv.addLog(str, id);
    }

    /**
     * 会上抛到数据库内容phase_details里
     *
     * @param str 内容
     * @param id 界面编号
     */
    private void addLog1(String str, int id) {
        apv.addLog1(str, id);
    }

    private void addLogContinuous(String str, int id) {
        apv.addLogContinuous(str, id);
    }

    public boolean ShowMessage(StationInfo si, int i, int id) {
        apv.showConfirmDialog(id, question[Integer.parseInt(si.ii.limitUp[i])]);
        return true;
    }

    public boolean getSN(int timeout, String str) {
        resultLog = "Pass";
        QuestionWindow qw = new QuestionWindow(timeout);
        qw.start(str);
        boolean ret = qw.getResult(timeout);
        if (!ret) {
            resultLog = "Fail";
        }
        return ret;
    }

    public boolean CheckQuestion(int timeout, String str) {
        resultLog = "Pass";
        QuestionWindow qw = new QuestionWindow(timeout);
        qw.start(str);
        boolean ret = qw.getResult(timeout);
        if (!ret) {
            resultLog = "Fail";
        }
        return ret;
    }

    public boolean ambitInsertDUTIntoFixture(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        apv.phase_items = new JSONObject[1];
        apv.phase_items[0] = new JSONObject();
        apv.phase_items[0].put("ip", si.ii.socketIp[i]);
        apv.phase_items[0].put("command", si.ii.cmd[i]);
        apv.phase_items[0].put("spec", si.ii.spec[i]);



        if (!ping(si.ii.socketIp[i], si.ii.diagCmdTime[i])) {   //ping 治具IP
            addLog1("Ping MCU " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        if (!to.connect(si.ii.socketIp[i])) {                  //连接治具
            addLog1("Telnet MCU" + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {
            to.readUntil(si.ii.diagCmd[i], i);                         //清除连接时产生的无用信息

            if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {   //下命令
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }
            String details = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]);  //砍头去尾留中间重要的内容
            addLog1(details, id);
            if (details.contains(si.ii.spec[i])) {               //判断
                resultLog = "PASS";
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean pingIP(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        int num = si.ii.diagCmdTime[i] / 6 + 1;
        for (int j = 0; j < num; j++) {
            try {
                Runtime.getRuntime().exec("cmd /c  arp -d");
            } catch (IOException ex) {
                Logger.getLogger(TestScript.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (ping(si.ii.socketIp[i], 6)) {
                addLog1("Ping " + si.ii.socketIp[i] + " PASS!", id);
                resultLog = "PASS";
                return true;
            }

        }

        addLog1("Ping " + si.ii.socketIp[i] + " Fail!", id);
        return false;
    }

    public boolean generateNodeFromBarcode(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        apv.addLog(apv.getPatternSfis_Te(), id);
//        apv.addLog(apv.this_sfis_te, id);
        apv.addLog(apv.getPatternTe_Sfis(), id);
        resultLog = "PASS";
        return true;
    }

    public boolean getMacFromShopFloor(StationInfo si, int i, int id) {
        apv.phase_items = new JSONObject[1];

        resultLog = "FAIL";
        if (!apv.getSfisStatus()) {
            addLog("cancelled  tiem", id);
            resultLog = "PASS";
            return true;
        }

        addLog("get mac=" + macTranslate(apv.ethMac[id - 1]).toLowerCase(), id);
        addLog("mac range up=" + macTranslate(si.ii.limitUp[i]).toLowerCase(), id);
        addLog("mac range down=" + macTranslate(si.ii.limitDown[i]).toLowerCase(), id);

        apv.phase_items[0] = new JSONObject();
        apv.phase_items[0].put("mac", macTranslate(apv.ethMac[id - 1]).toLowerCase());
        apv.phase_items[0].put("limit_up", macTranslate(si.ii.limitUp[i]).toLowerCase());
        apv.phase_items[0].put("limit_down", macTranslate(si.ii.limitDown[i]).toLowerCase());


        long mac = Long.parseLong(apv.ethMac[id - 1], 16);
        System.out.println(mac);
        long up = Long.parseLong(si.ii.limitUp[i], 16);
        long down = Long.parseLong(si.ii.limitDown[i], 16);
        System.out.println(up);
        System.out.println(down);
        if (mac < down || mac > up) {
            return false;
        } else {
            resultLog = apv.ethMac[id - 1];
            return true;
        }
//        addLog1("Ping " + si.ii.socketIp[i] + " Fail!", id);
//        return false;
    }

    public boolean ambitCheckIQBootup(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        if (ping(si.ii.socketIp[i], si.ii.diagCmdTime[i])) {
            addLog1("Ping " + si.ii.socketIp[i] + " PASS!", id);
            resultLog = "PASS";
            return true;
        }
        addLog1("Ping " + si.ii.socketIp[i] + " Fail!", id);
        return false;
    }

    public boolean FixtureControl(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        addLog("CMD: " + si.ii.cmd[i], id);
        ComPort cp = new ComPort(apv, id);



        try {
            for (int j = 0; j < 3; j++) {
                if (!cp.open(si.ii.comPort[i], si.ii.transRate[i])) {
                    cp.close();
                    continue;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    return false;
                }
                if (!cp.writeAndReadUntil(si.ii.cmd1[i] + "\r\n", si.ii.spec[i], 5)) {
                    addLog("cmd " + si.ii.cmd1[i] + " FAIL!", id);
//                    resultLog = "PASS";
                    return false;
                }
                if (!cp.writeAndReadUntil(si.ii.cmd2[i] + "\r\n", si.ii.spec[i], 5)) {
                    addLog("cmd " + si.ii.cmd2[i] + " FAIL!", id);
//                    resultLog = "PASS";
                    return false;
                }
                if (!cp.writeAndReadUntil(si.ii.cmd3[i] + "\r\n", si.ii.spec[i], 5)) {
                    addLog("cmd " + si.ii.cmd2[i] + " FAIL!", id);
//                    resultLog = "PASS";
                    return false;
                }
                if (cp.writeAndReadUntil(si.ii.cmd4[i] + "\r\n", si.ii.spec[i], 5)) {
                    resultLog = "PASS";
                    return true;
                } else {
                    addLog("cmd " + si.ii.cmd3[i] + " FAIL!", id);
                }

            }

        } finally {
            cp.close();
        }
        return false;
    }

    public boolean ambitRunIQScrip(StationInfo si, int i, int id) {
        resultLog = "FAIL";
//        BufferedReader br = null;
        File IQLogFile = null;
//        String IQlog = "";
//        String litepoint = "Eden_P0_K9BK0038_2-19_Fail_RF.txt";
        String litepoint = si.ii.cmd[i];
        IQLogFile = new File(litepoint);             //查看IQ是否跑完
        try {
            if (!ping(si.ii.socketIp[i], 5)) {
                addLog1("Ping  IQ" + si.ii.socketIp[i] + " FAIL!", id);
                return false;
            }

            if (IQLogFile.exists()) {
                IQLogFile.delete();
            }
            if (IQLogFile.exists()) {
                apv.showConfirmDialog(id, si.ii.cmd[i] + "\r\nDelete fail");
                return false;
            }
            Runtime.getRuntime().exec(si.ii.diagCmd[i]);
            Thread.sleep(5000);
            if (!IQLogFile.exists()) {
                this.addLog("not have log.txt", id);
                return false;
            }

            //保存文件路径
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());//设置日期格式
            String Time = new SimpleDateFormat("HHmmss").format(new Date());//设置日期格式  
            String filePath = si.logPath + File.separator + "IQlog" + File.separator + date + File.separator;
            File fnewpath = new File(filePath); //文件新（目标）地址 
            if (!fnewpath.exists()) //判断文件夹是否存在 
            {
                fnewpath.mkdirs();
            }
            //保存文件路径
            filePath = filePath + apv.sn[id - 1] + "_" + Time + ".txt";
            readIQlog(IQLogFile, filePath, si.ii.diagCmdTime[i]);

            ParseIQLog(IQLogFile);
            if (apv.IQlog.containsKey("RESERT")) {
                resultLog = "PASS";
                return true;
            } else {
                this.addLog1(" log_all.txt erro", id);
            }

        } catch (Exception e) {
            e.printStackTrace();
            this.addLog(e.toString(), id);
            return false;
        } finally {
            try {
                Runtime.getRuntime().exec("taskkill /f /t /im  IQfactRun_Console.exe");
            } catch (IOException ex) {
                Logger.getLogger(TestScript.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    public boolean writeServerLog(String filePath, String str) {

        File file = new File(filePath);
        String dir = file.getParent();
        file = new File(dir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                JOptionPane.showMessageDialog(null, filePath + " not exist.", "Tip", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        }

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(filePath, true));
            String data = str;
            out.write(data);
            out.close();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Tip", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

//    public boolean ambitReadIQLog(StationInfo si, int i, int id) {
//        resultLog = "FAIL";
//        if (!apv.IQlog.containsKey("RESERT")) {
//            this.addLog("IQ log erro", id);
//            return false;
//        }
//        String value = apv.IQlog.get("RESERT").toString();
//        if (value.contains("P A S S")) {
//            resultLog = "PASS";
//            return true;
//        } else {
//            String[] line = value.split("\r\n");
//            si.ii.errorCode[i] = "";
//            for (int j = 0; j < line.length; j++) {
//                if (line[i].contains("--- [Failed]")) {
//                    this.addLog(line[i], id);
//                    if (line[i].contains(" EVM POWER MASK")) {
//                        int index = line[i].indexOf("EVM POWER MASK") + 15;
//                        si.ii.errorCode[i] += "EM" + line[i].substring(index, index + 2).trim();
//                    } else if (line[i].contains(" EVM POWER")) {
//                        int index = line[i].indexOf("EVM POWER") + 10;
//                        si.ii.errorCode[i] += "EM" + line[i].substring(index, index + 2).trim();
//                    } else if (line[i].contains(" PER ")) {
//                        int index = line[i].indexOf(" PER ") + 5;
//                        si.ii.errorCode[i] += "EM" + line[i].substring(index, index + 2).trim();
//                    } else {
//                        String str = line[i].substring(0, line[i].indexOf(".")).trim();
//                        if (str.length() < 2) {
//                            str = "0" + str;
//                        }
//                        si.ii.errorCode[i] += "IQ" + str;
//                    }
//                }
//            }
//        }
//        return false;
//    }
    /**
     * 把IQlog按项目切割，放入 map IQlog里,可以按项目搜索出内容。
     *
     * @param IQLogFile
     */
    public void ParseIQLog(File IQLogFile) {

        BufferedReader br = null;
//        StringBuilder stringbuffer = new StringBuilder();
        String iqLogLine = "";
        ArrayList<String> details = new ArrayList<String>();
        String key = "";
        int num = 1;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(IQLogFile)));

            while ((iqLogLine = br.readLine()) != null) {
//                stringbuffer.append(iqLogLine).append("\r\n");
                if (iqLogLine.startsWith(num + ".") && iqLogLine.contains("________")) {

                    num++;

                    apv.IQlog.put(key, details.toArray(new String[details.size()]));

                    details.clear();
                    details.add(iqLogLine);
//                    sb.append(iqLogLine).append("\r\n");

                    key = iqLogLine.substring(0, iqLogLine.indexOf("______")).trim();
                } else if (iqLogLine.contains(" *************")) {

                    num++;
                    if (!key.equals("RESERT")) {
                        apv.IQlog.put(key, details.toArray(new String[details.size()]));

                        details.clear();
                    }
                    details.add(iqLogLine);
                    key = "RESERT";
                } else {
                    details.add(iqLogLine);
                }

            }
            apv.IQlog.put(key, details.toArray(new String[details.size()]));
        } catch (IOException ex) {
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(TestScript.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

//        return stringbuffer.toString();
    }

    public void ExitIQWindows() {
        W32API.HWND hwnd_wifi = null;
        hwnd_wifi = User32.INSTANCE.FindWindow(0, "Administrator:  ROKU_WIFI");
        try {
            Process po = Runtime.getRuntime().exec("cmd /C start closeCMD.bat");
        } catch (Exception ee) {
            System.out.println(ee.getMessage());
        }
        if (hwnd_wifi != null) {
            User32.INSTANCE.SetForegroundWindow(hwnd_wifi);
            try {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    System.out.println("Exception out");
                }
                Robot rt = new Robot();
                rt.keyPress(KeyEvent.VK_CONTROL);
                rt.keyPress(KeyEvent.VK_C);
                rt.keyRelease(KeyEvent.VK_C);
                rt.keyRelease(KeyEvent.VK_CONTROL);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    System.out.println("Exception out");
                }
                rt.keyPress(KeyEvent.VK_Y);
                rt.keyRelease(KeyEvent.VK_Y);
                rt.keyPress(KeyEvent.VK_ENTER);
                rt.keyRelease(KeyEvent.VK_ENTER);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    System.out.println("Exception out");
                }
                rt.keyPress(KeyEvent.VK_E);
                rt.keyRelease(KeyEvent.VK_E);
                rt.keyPress(KeyEvent.VK_X);
                rt.keyRelease(KeyEvent.VK_X);
                rt.keyPress(KeyEvent.VK_I);
                rt.keyRelease(KeyEvent.VK_I);
                rt.keyPress(KeyEvent.VK_T);
                rt.keyRelease(KeyEvent.VK_T);
                rt.keyPress(KeyEvent.VK_ENTER);
                rt.keyRelease(KeyEvent.VK_ENTER);
            } catch (Exception ee) {
                System.out.println(ee.getMessage());
            }
        }
    }

    public boolean ambitDUTWaitForTelnet(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        } else {
            addLog1("Telnet " + si.ii.socketIp[i] + " PASS!", id);
            resultLog = "PASS";
            to.disconnect();
            return true;
        }

    }

    public boolean currentTest(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        StringBuilder Value = new StringBuilder();
        StringBuilder Items = new StringBuilder();
        ComPort cp = new ComPort(apv, id);
        double voltage = 0;
        double electricity = 0;
        apv.phase_items = new JSONObject[2];
        try {

            if (!cp.open(si.ii.comPort[i], si.ii.transRate[i])) {
                return false;
            }
            if (!cp.writeAndReadUntil(si.ii.cmd1[i], "FAIL", 1)) { //电源电压
                voltage = Double.valueOf(cp.readAll.substring(cp.readAll.indexOf("VALUE=") + 6).trim());
            }
            if (!cp.writeAndReadUntil(si.ii.cmd1[i], "FAIL", 1)) { //电源电流
                electricity = Double.valueOf(cp.readAll.substring(cp.readAll.indexOf("VALUE=") + 6).trim());
            }


            double value[] = {voltage, electricity};
            String name[] = {"voltage", "electricity"};
            String unit[] = {"V", "A"};
            String[] up = si.ii.limitUp[i].split(",");
            String[] down = si.ii.limitDown[i].split(",");
            for (int j = 0; j < 2; j++) {   // 电压   电流 
                apv.phase_items[j] = new JSONObject();
                apv.phase_items[j].put("name", name[j]);
                apv.phase_items[j].put("value", value[j]);
                apv.phase_items[j].put("up", up[j]);
                apv.phase_items[j].put("down", down[j]);
                apv.phase_items[j].put("unit", unit[j]);
                addLog(name[j] + ":" + value[j] + unit[j], id);
                Items.append(",").append(name[j]);
                Value.append(",").append(value[j]);
                double Up = Double.valueOf(up[j].trim());
                double Down = Double.valueOf(down[j].trim());
                if (Up < value[j] || value[j] < Down) {
                    addLog("up:" + up[j] + " down:" + down[j] + " unit:" + unit[j], id);

                    return false;
                }

            }

            resultLog = "PASS";
            si.ii.resultType[i] = si.ii.resultType[i] + Items.toString();
            apv.testLog[id - 1] += resultLog + Value.toString();
            return true;
        } catch (Exception e) {

            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            cp.close();
        }

        return false;

    }

    public boolean CPUTemperatureTestAfterBootup(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        String[] command = {si.ii.diagCmd1[i], si.ii.diagCmd2[i], si.ii.diagCmd3[i], si.ii.diagCmd4[i]};
//        double[] value = new double[command.length];
        apv.phase_items = new JSONObject[command.length];
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {

            to.readUntil(si.ii.diagCmd[i], i);
            resultLog = "";
            boolean bool = true;
            for (int j = 0; j < command.length; j++) {
                if (!to.sendCommandAndRead(command[j], si.ii.diagCmd[i], 3)) {
                    addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                    return false;
                }

                String details = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]).trim();
                addLog(details, id);
                resultLog += details + "-";
                apv.phase_items[j] = new JSONObject();
                apv.phase_items[j].put("name", "cpu" + j);
                apv.phase_items[j].put("value", details);
                apv.phase_items[j].put("up", si.ii.limitUp[i]);
                apv.phase_items[j].put("down", si.ii.limitDown[i]);
                double value = Double.valueOf(details.trim());
                double up = Double.valueOf(si.ii.limitUp[i]);
                double down = Double.valueOf(si.ii.limitDown[i]);
                if (value > up || value < down) {
                    bool = false;
                }

            }
            return bool;

        } catch (Exception e) {

            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }

        return false;

    }

    public boolean ambitVoltageTest(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        ComPort cp = new ComPort(apv, id);

        //        String VoltageName[] = si.ii.d1iagCmd[i].split(",");

        String[] voltageName = (si.ii.diagCmd1[i] + "," + si.ii.diagCmd2[i] + "," + si.ii.diagCmd3[i]).split(",");
        String[] spec = (si.ii.Cut1[i] + "," + si.ii.Cut2[i] + "," + si.ii.Cut3[i]).split(",");
        String[] range = (si.ii.cmd4[i] + "," + si.ii.cmd5[i] + "," + si.ii.cmd6[i]).split(",");
        double percent = Double.valueOf(si.ii.spec[i].trim());
        boolean status = true;
        StringBuilder Value = new StringBuilder();
        StringBuilder Items = new StringBuilder();
        try {

            if (!cp.open(si.ii.comPort[i], si.ii.transRate[i])) {
                addLog1("open " + si.ii.comPort[i] + " Fail!", id);
                return false;
            }

            Thread.sleep(1000);
            apv.phase_items = new JSONObject[voltageName.length];
            for (int k = 0; k < voltageName.length; k++) {

                spec[k] = spec[k].trim();
                voltageName[k] = voltageName[k].trim();
                double spec_value = Double.valueOf(spec[k]);
                addLog("voltageName:" + voltageName[k] + " IN" + k, id);
//   addLog("MesValue=" + num, id) ;
                Items.append(",").append(k);
                if (voltageName[k].equalsIgnoreCase("null")) {
                    Value.append(",").append("cancelled");
//                        addLog("MesValue=" + num, id);
                    continue;
                }
                cp.writeAndReadUntil("AT+" + voltageName[k] + "%", "\r\n", 3);
                double value = Double.valueOf(cp.readAll.substring(cp.readAll.indexOf("VALUE=") + 6).trim());
                value = value + Double.valueOf(range[k].trim());
                Value.append(",").append(value);
                voltageName[k] = voltageName[k].trim();
                DecimalFormat df = new DecimalFormat("0.0000");
                addLog("value=" + df.format(value) + " V" + " spec=" + spec[k], id);
//                    System.out.println(value);
                percent = Double.valueOf(si.ii.spec[i].trim());
                if (spec_value == 9) {
                    percent = 0.1;
                }
                if (value > spec_value * (1 + percent) || value < spec_value * (1 - percent)) {
                    status = false;
                    addLog("xxxxxxxxxxxxxxxxxxxxxxxxx", id);

                }

                addLog("up=" + df.format((spec_value * (1 + percent))) + "  down=" + df.format((spec_value * (1 - percent))), id);
                apv.phase_items[k] = new JSONObject();
                apv.phase_items[k].put("name", voltageName[k]);
                apv.phase_items[k].put("value", df.format(value));
                apv.phase_items[k].put("unit", "V");
                apv.phase_items[k].put("limit_up", df.format(spec_value * (1 + percent)));
                apv.phase_items[k].put("limit_down", df.format(spec_value * (1 - percent)));

            }


            //
            //            cp.write(resultLog);
            //            cp.writeAndReadUntil(si.ii.cmd[i], si.ii.diagCmd[i], 3);
            ////        cp.readAll="Rail9V:3.9;Rail5V:3.9;Rail3.3V:3.9;Rail1.35V:3.9;Rail1.1V:3.9;Rail1.1V:3.9;";
            //            addLog1(cp.readAll, id);
            //            String content[] = cp.readAll.split(";");
            //            apv.phase_items = new JSONObject[content.length];
            //            for (int j = 0; j < content.length; j++) {
            //                apv.phase_items[j] = new JSONObject();
            //                String voltageName = content[j].split(":")[0].trim();
            //                Double voltageValue = Double.parseDouble(content[j].split(":")[1].trim());
            //                Double voltageUP = Double.parseDouble(VoltageUP[j].trim());
            //                Double voltageDown = Double.parseDouble(VoltageDown[j].trim());
            //                apv.phase_items[j].put("name", voltageName);
            //                apv.phase_items[j].put("value", voltageValue);
            //                apv.phase_items[j].put("voltage", "5.05");
            //                apv.phase_items[j].put("unit", "V");
            //                apv.phase_items[j].put("limit_max", voltageUP);
            //                apv.phase_items[j].put("limit_min", voltageDown);
            //                if (!voltageName.equals(VoltageName[j])) {
            //                    addLog("voltageName not match config", id);
            //                    status = false;
            //                    continue;
            //                }
            //                if (voltageValue < voltageDown || voltageValue > voltageUP) {
            //                    addLog("voltageValue " + voltageName + " erro", id);
            //                    addLog("voltageValue=" + voltageValue, id);
            //                    addLog("voltageDown=" + voltageDown, id);
            //                    addLog("voltageUP=" + voltageUP, id);
            //                    status = false;
            //                }
            //            }
            if (status) {
                resultLog = "PASS";
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);

        } finally {
            si.ii.resultType[i] = si.ii.resultType[i] + Items.toString();
            apv.testLog[id - 1] += resultLog + Value.toString();
            cp.close();
        }

        return false;
    }

    public boolean ambitVoltageTest1(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        ComPort cp = new ComPort(apv, id);

        //        String VoltageName[] = si.ii.d1iagCmd[i].split(",");

        String[] cmd = {si.ii.cmd1[i], si.ii.cmd2[i], si.ii.cmd3[i]};
        String[] VoltageName = {si.ii.diagCmd1[i], si.ii.diagCmd2[i], si.ii.diagCmd3[i]};
        String[] Spec = {si.ii.Cut1[i], si.ii.Cut2[i], si.ii.Cut3[i]};
        String[] Range = {si.ii.cmd4[i], si.ii.cmd5[i], si.ii.cmd6[i]};
        double percent = Double.valueOf(si.ii.spec[i].trim());
        boolean status = true;
        StringBuilder Value = new StringBuilder();
        StringBuilder Items = new StringBuilder();
        try {

            if (!cp.open(si.ii.comPort[i], si.ii.transRate[i])) {
                addLog1("open " + si.ii.comPort[i] + " Fail!", id);
                return false;
            }
            for (int j = 0; j < cmd.length; j++) {
                addLog("*****************Modules" + (j + 1) + "******************", id);
//                System.out.println("******************************");
//                for (int k = 0; k < cmd[j].toCharArray().length; k++) {
//                int[] command = new int[10];
                int[] command = new int[8];
//                System.out.println(VoltageName[j]);
                String[] voltageName = VoltageName[j].split(",");
                String[] spec = Spec[j].split(",");
                String[] range = Range[j].split(",");
                for (int k = 0; k < command.length - 2; k++) {

                    command[k] = Integer.valueOf(cmd[j].substring(k * 2, (k + 1) * 2));
//                    System.out.println(command[k]);
                }
                int wCrc = CRC.CRC_Check(command, command.length - 2);
                command[7] = ((wCrc & 0xff00) >> 8);
                command[6] = (wCrc & 0xff);

                cp.write(command);
                Thread.sleep(1000);
                byte[] by = cp.readbyte();
                if (cp.bytelean < 0) {
                    return false;
                }
//              double[] MesValue=new double[8];

                for (int k = 0; k < 8; k++) {
                    int a = (int) by[k * 2 + 3] << 8 & 0xffff;
                    int b = (int) by[k * 2 + 3] & 0xff;
                    int num = a + b;
                    spec[k] = spec[k].trim();
                    voltageName[k] = voltageName[k].trim();
                    double spec_value = Double.valueOf(spec[k]);
                    int scope = 5;
                    if (spec_value > 0) {
                        if (spec_value < 1) {
                            scope = 1;
                        } else if (spec_value < 5) {
                            scope = 5;
                        } else if (spec_value < 10) {
                            scope = 10;
                        }
                    }
                    addLog("voltageName:" + voltageName[k] + " IN" + k, id);
//   addLog("MesValue=" + num, id) ;
                    Items.append(",").append("Modules").append(j + 1).append("_IN").append(k);
                    if (voltageName[k].equalsIgnoreCase("null")) {
                        Value.append(",").append("cancelled");
                        addLog("MesValue=" + num, id);
                        continue;
                    }
                    double value = MesValueToEngValue(num, scope);
                    value = value + Double.valueOf(range[k].trim());
                    Value.append(",").append(value);
                    voltageName[k] = voltageName[k].trim();
                    DecimalFormat df = new DecimalFormat("0.0000");

                    addLog("value=" + df.format(value) + " V" + " spec=" + spec[k], id);
//                    System.out.println(value);

                    if (spec_value == 9) {
                        percent = 0.1;
                    } else {
                        percent = Double.valueOf(si.ii.spec[i].trim());
                    }
                    if (value > spec_value * (1 + percent) || value < spec_value * (1 - percent)) {
                        status = false;
                        addLog("xxxxxxxxxxxxxxxxxxxxxxxxx", id);

                    }

                    addLog("up=" + df.format((spec_value * (1 + percent))) + "  down=" + df.format((spec_value * (1 - percent))), id);

                }
            }


            if (status) {
                resultLog = "PASS";
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);

        } finally {
            si.ii.resultType[i] = si.ii.resultType[i] + Items.toString();
            apv.testLog[id - 1] += resultLog + Value.toString();
            cp.close();
        }

        return false;
    }

    public boolean ambitDUTSleepTransition(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        try {
            addLog1("sleep " + si.ii.diagCmdTime[i] + " s", id);
            Thread.sleep(si.ii.diagCmdTime[i] * 1000);
            resultLog = "PASS";
            return true;
        } catch (InterruptedException ex) {
            Logger.getLogger(TestScript.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public boolean TestCmd(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {

            to.readUntil(si.ii.diagCmd[i], i);
            if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }

            String details = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]);
            addLog1(details, id);
            if (details.contains(si.ii.spec[i])) {
                resultLog = "PASS";
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean ambitCheckTestImageVersion(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {

            to.readUntil(si.ii.diagCmd[i], i);
            if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }

            String details = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]);
            addLog1(details, id);
            resultLog = details;
            apv.phase_items = new JSONObject[1];
            apv.phase_items[0] = new JSONObject();
            apv.phase_items[0].put("value", details);
            apv.phase_items[0].put("spec", si.ii.spec[i]);
            if (details.contains(si.ii.spec[i])) {

                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean ambitVerifyUBootVersion(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        apv.phase_items = new JSONObject[1];
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {
            to.readUntil(si.ii.diagCmd[i], i);
            if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }

            String details = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]);
            addLog(details, id);
            if (!details.contains(si.ii.spec[i])) {
                addLog1("VerifyUBootVersion Fail!", id);
                si.ii.errorDes[i] = "erifyUBootVersion Fail!";
                return false;
            }

            if (!apv.getSfisStatus()) {
//                addLog("cancelled  tiem", id);
                resultLog = "PASS";
                return true;
            }

            String serial = "";
            String mac = "";
            int index = details.indexOf("serial=") + 7;
            serial = details.substring(index, index + 16);

            if (apv.mlbSn[id - 1].equals("")) {
                apv.mlbSn[id - 1] = serial;
//                addLog("DEVICE_SN=" + apv.mlbSn[id - 1], id);
            }

            index = details.indexOf("mac=") + 4;
            mac = details.substring(index, index + 18);
            apv.phase_items[0] = new JSONObject();
            apv.phase_items[0].put("serial", serial);
            apv.phase_items[0].put("serial spec", apv.mlbSn[id - 1]);
            apv.phase_items[0].put("mac", mac);
            apv.phase_items[0].put("mac spec", macTranslate(apv.ethMac[id - 1]).toLowerCase());
            if (!details.contains("serial=" + apv.mlbSn[id - 1])) {

                addLog1("serial Fail!", id);
                addLog("DEVICE_SN=" + apv.mlbSn[id - 1], id);
//                si.ii.errorCode[i] = "3.1";
//                si.ii.errorDes[i] = "Device.Serial Fail!";
                return false;
            }
            if (!details.contains("mac=" + macTranslate(apv.ethMac[id - 1]).toLowerCase())) {
                addLog1("eth mac Fail!", id);
                addLog("eth mac=" + apv.ethMac[id - 1], id);
//                si.ii.errorCode[i] = "17.3";
//                si.ii.errorDes[i] = "Version.MMC Fail!";

                return false;
            }
            resultLog = "PASS";
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean checkQSDKVersion(StationInfo si, int i, int id) {
        resultLog = "FAIL";

        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {

            to.readUntil(si.ii.diagCmd[i], i);
            if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }

            String details = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]);
            addLog1(details, id);
            resultLog = details;
            apv.phase_items = new JSONObject[1];
            apv.phase_items[0] = new JSONObject();
            apv.phase_items[0].put("value", details);
            apv.phase_items[0].put("spec", si.ii.spec[i]);
            if (details.contains(si.ii.spec[i])) {

                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean subsystemTest(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        String[] name = si.ii.cmd[i].split(",");
        String[] cmd = {si.ii.cmd1[i], si.ii.cmd2[i], si.ii.cmd3[i], si.ii.cmd4[i], si.ii.cmd5[i], si.ii.cmd6[i]};
        String[] spec = {si.ii.diagCmd1[i], si.ii.diagCmd2[i], si.ii.diagCmd3[i], si.ii.diagCmd4[i], si.ii.diagCmd5[i], si.ii.diagCmd6[i]};
        String[] eero_code = si.ii.errorCode[i].split(",");
        String[] item = si.ii.cmd[i].split(",");
        apv.phase_items = new JSONObject[item.length];
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {
            to.readUntil(si.ii.diagCmd[i], i);
            boolean result = true;
            for (int j = 0; j < cmd.length; j++) {
                if (cmd[j].trim().length() < 2) {
                    continue;
                }
                if (!to.sendCommandAndRead(cmd[j], si.ii.diagCmd[i], 3)) {
                    addLog1("CMD " + cmd[j] + " Fail!", id);
                    result = false;
                    continue;
                }
                String value = substring(to.getString, cmd[j], si.ii.diagCmd[i]);
                addLog(value, id);
                apv.phase_items[j] = new JSONObject();
                apv.phase_items[j].put("name", item[j]);
                apv.phase_items[j].put("value", value);
                apv.phase_items[j].put("spec", spec[j]);
//                 to.readTime(1);
                String Spec[] = spec[j].split(",");
                boolean bool = false;
                for (int k = 0; k < Spec.length; k++) {
                    if (value.contains(Spec[k])) {

                        bool = true;
                        break;
                    }
                }
                if (!bool) {

                    addLog1(name[j] + "  Fail!", id);

                    si.ii.errorCode[i] = eero_code[j];
                    si.ii.errorDes[i] = name[j] + " Fail";
                    result = false;
                }

            }
            if (result) {
                resultLog = "PASS";
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }

        return false;
    }

    public boolean ambitEthernetSpeedTest(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        boolean status = true;
        BufferedReader br = null;
        ArrayList<Double> TX_value = new ArrayList();
        ArrayList<Double> RX_value = new ArrayList();
        int value[] = new int[2];
        double spec[] = new double[2];
        String name[] = si.ii.cmd[i].split(",");
        spec[0] = Double.valueOf(si.ii.spec[i].split(",")[0].trim());
        spec[1] = Double.valueOf(si.ii.spec[i].split(",")[1].trim());
        apv.phase_items = new JSONObject[2];


        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {

            if (!ping(si.ii.cmd1[i], 3)) {
                addLog("Localhost  IP ERRO", id);
                return false;
            }
            to.readUntil(si.ii.diagCmd[i], 1);

            to.getString = "";

            String cmd = "cmd /c iperf -s -w 2M";
            addLog(" PC CMD: " + cmd, id);
            Process p = Runtime.getRuntime().exec(cmd);

            br = new BufferedReader(new InputStreamReader(p.getInputStream(), "GB2312"));
            Thread.sleep(1000);
            cmd = "iperf -c " + si.ii.cmd1[i] + " " + si.ii.cmd2[i];

            addLog("DUT CMD: " + cmd, id);
            to.sendCommandAndRead(cmd, si.ii.diagCmd[i], si.ii.diagCmdTime[i], this);
            br.close();
//            addLog(to.getString + cmd, id);
            String details = to.getString;
            Pattern pa = Pattern.compile("(?<=ytes)\\s++\\w\\d*\\.?\\d*");
            Matcher macher = pa.matcher(details);
            while (macher.find()) {
                TX_value.add(Double.valueOf(macher.group().trim()));
                System.out.println(macher.group().trim());
            }

            cmd = "iperf -s";
            to.sendCommand(cmd);
            Thread.sleep(1000);
            addLog("DUT CMD: " + cmd, id);
            cmd = "cmd /c " + "iperf -c " + si.ii.socketIp[i] + " " + si.ii.cmd2[i];
            addLog(" PC CMD: " + cmd, id);
            details = DOS_Set_read_cmd(cmd, 3);
            System.out.println(details);
            pa = Pattern.compile("(?<=ytes)\\s++\\w\\d*\\.?\\d*");
            macher = pa.matcher(details);
            while (macher.find()) {
                RX_value.add(Double.valueOf(macher.group().trim()));

            }

            Double[] value_tx = TX_value.toArray(new Double[TX_value.size()]);

            Double[] value_rx = RX_value.toArray(new Double[RX_value.size()]);
//            if (value_tx.length != value_rx.length) {
//                return false;
//            }
            double value_TX = 0;
            double value_RX = 0;
            for (int j = 1; j < value_tx.length - 1; j++) {
                value_TX = value_TX + value_tx[j];
//                value[1] = value[1] + value_rx[j];
            }
            for (int j = 1; j < value_rx.length - 1; j++) {
//                value[0] = value[0] + value_tx[j];
                value_RX = value_RX + value_rx[j];
            }
            value[0] = (int) (value_TX / (value_tx.length - 2));
            value[1] = (int) (value_RX / (value_rx.length - 2));
            resultLog = value[0] + "-" + value[1];
            for (int j = 0; j < name.length; j++) {
                apv.phase_items[j] = new JSONObject();
                apv.phase_items[j].put("name", name[j].trim());
                apv.phase_items[j].put("value", value[j]);
                apv.phase_items[j].put("unit", "Mbits/sec");
                apv.phase_items[j].put("limit_min", spec[j]);
                apv.phase_items[j].put("limit_max", "null");
                addLog(name[j].trim() + " value " + value[j] + "  limit_min " + spec[j], id);

                if (value[j] < spec[j]) {
                    status = false;

                }
            }
            if (status) {

                return true;
            }
            //    Process pro = Runtime.getRuntime().exec("cmd /c open.bat");
        } catch (Exception ex) {
            ex.printStackTrace();
            addLog(ex.toString(), id);
        } finally {
            try {
                Runtime.getRuntime().exec(" taskkill /f /t /im  iperf.exe");
                Runtime.getRuntime().exec(" taskkill /f /t /im  iperf3.exe");
                Robot robot;
                try {
                    robot = new Robot();
                    robot.keyPress(KeyEvent.VK_ENTER);
                    robot.keyRelease(KeyEvent.VK_ENTER);
                } catch (AWTException ex) {
                    Logger.getLogger(TestScript.class.getName()).log(Level.SEVERE, null, ex);
                }

                to.disconnect();

                br.close();
            } catch (IOException ex) {
                Logger.getLogger(TestScript.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return false;
    }

    public boolean WifiSpeedTest(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        apv.phase_items = new JSONObject[2];

        boolean status = true;

        double value[] = new double[2];
        double spec[] = new double[2];
        String name[] = si.ii.cmd[i].split(",");
        spec[0] = Double.valueOf(si.ii.spec[i].split(",")[0].trim());
        spec[1] = Double.valueOf(si.ii.spec[i].split(",")[1].trim());
        TelnetOper golden = new TelnetOper();
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        addLog1("Telnet " + si.ii.socketIp[i] + " Pass!", id);
        if (!golden.connectWithPassword(si.ii.cmd1[i], si.ii.diagCmd1[i], si.ii.diagCmd2[i])) {
            addLog1("Telnet " + si.ii.cmd1[i] + " Fail!", id);
            return false;
        }
        addLog1("Telnet " + si.ii.cmd1[i] + " Pass!", id);
        try {
            to.getString = "";

            String cmd = "iperf -s";
            addLog(" golden CMD: " + cmd, id);
            golden.write(cmd + "\r\n");
//            Process p = Runtime.getRuntime().exec(cmd);
//            String line = "";
//            br = new BufferedReader(new InputStreamReader(p.getInputStream(), "GB2312"));
            to.readTime(1);
            cmd = "iperf -c " + si.ii.cmd1[i] + " " + si.ii.cmd2[i];

            addLog("DUT CMD: " + cmd, id);
            to.sendCommandAndRead(cmd, si.ii.diagCmd[i], si.ii.diagCmdTime[i], this);

//            addLog(to.getString + cmd, id);
            value[0] = Double.valueOf(to.getString.substring(to.getString.lastIndexOf("Bytes") + 6, to.getString.lastIndexOf("bits/sec") - 1).trim());
            golden.disconnect();
            if (!golden.connectWithPassword(si.ii.cmd1[i], si.ii.diagCmd1[i], si.ii.diagCmd2[i])) {
                addLog1("connect " + si.ii.cmd1[i] + " Fail!", id);
                return false;
            }
            cmd = "iperf -s";
            to.sendCommand(cmd);
//            Thread.sleep(1000);
            addLog("DUT CMD: " + cmd, id);
            cmd = "iperf -c " + si.ii.socketIp[i] + " " + si.ii.cmd2[i];
            addLog("golden CMD: " + cmd, id);

            golden.readTime(1);
            golden.sendCommandAndRead(cmd, "#", si.ii.diagCmdTime[i], this);

            value[1] = Double.valueOf(golden.getString.substring(golden.getString.lastIndexOf("Bytes") + 6, golden.getString.lastIndexOf("bits/sec") - 1).trim());
            for (int j = 0; j < name.length; j++) {
                apv.phase_items[j] = new JSONObject();
                apv.phase_items[j].put("name", name[j].trim());
                apv.phase_items[j].put("value", value[j]);
                apv.phase_items[j].put("unit", "Mbits/sec");
                apv.phase_items[j].put("limit_min", spec[j]);
                apv.phase_items[j].put("limit_max", "null");
                addLog(name[j].trim() + " value " + value[j] + "  limit_down " + spec[j], id);
                if (value[j] < spec[j]) {
                    status = false;

                }
            }
            if (status) {
                resultLog = "PASS";
                return true;
            }
            //    Process pro = Runtime.getRuntime().exec("cmd /c open.bat");
        } catch (Exception ex) {
            ex.printStackTrace();
            addLog(ex.toString(), id);
        } finally {

            to.disconnect();
            golden.disconnect();

        }

        return false;
    }

    public double IperfValve(String cmd, String Spec) {
        double value = 0;
        try {
            Process p = Runtime.getRuntime().exec("cmd /c " + cmd);
            String line = "";
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "GB2312"));
            while ((line = br.readLine()) != null) {
                if (line.indexOf("Mbits/sec") != -1) {
                    value = Double.valueOf(line.substring(line.indexOf("MBytes") + 7, line.indexOf("Mbits/sec")).trim());
                    Runtime.getRuntime().exec("cmd /c open.bat");
                    break;
                }

            }
            //    Process pro = Runtime.getRuntime().exec("cmd /c open.bat");
        } catch (IOException ex) {
            System.out.println("open open.bat FAIL");
        }

        return value;

    }

    public boolean ambitFlashAndCheckBTFW(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {
            to.readUntil(si.ii.diagCmd[i], i);
            if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }

            String details = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]);
            addLog1(details, id);
            if (details.contains(si.ii.spec[i])) {
                resultLog = "PASS";
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean MMCReadWriteSpeedTest(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        double writing_value = 0;
        double reading_value = 0;

        String[] down = si.ii.limitDown[i].split(",");   //averge value Writing/Reading
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {
            to.readUntil(si.ii.diagCmd[i], i);
            if (!to.sendCommandAndRead(si.ii.cmd[i] + "\r\n", si.ii.diagCmd[i], 10)) {
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }

            String details = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]);
            addLog(details, id);
            resultLog = "";
            String MMCReadWrite[] = details.split("real    0m");
            if (MMCReadWrite.length != si.ii.diagCmdTime[i]) {
                return false;
            }
            for (int j = 0; j < MMCReadWrite.length - 1; j++) {
                int value_head = details.indexOf("seconds,") + 8;
                int value_end = details.indexOf("MB/s");
                if (value_head > value_end || value_end <= 0) {                         // 判断
                    return false;
                }
                double value = Double.valueOf(MMCReadWrite[j].substring(value_head, value_end).trim());   // 截取
                if (MMCReadWrite[j].contains("Writing")) {
                    writing_value += value;                           // Writing 赋值
                    addLog("writing averge=" + value, id);
                } else if (MMCReadWrite[j].contains("Reading")) {
                    reading_value += value;                          // Reading 赋值
                    addLog("reading averge=" + value, id);
                } else {
                    return false;
                }
            }


            double reading_averge = reading_value / MMCReadWrite.length - 1;
            double writing_averge = writing_value / MMCReadWrite.length - 1;
            apv.phase_items = new JSONObject[2];
            apv.phase_items[0] = new JSONObject();
            apv.phase_items[0].put("model", "writing");
            apv.phase_items[0].put("value", writing_averge);
            apv.phase_items[0].put("limit_min", down[0].trim());
            apv.phase_items[0].put("limit_max", "null");
            apv.phase_items[1] = new JSONObject();
            apv.phase_items[1].put("model", "reading");
            apv.phase_items[1].put("value", reading_averge);
            apv.phase_items[0].put("limit_min", down[1].trim());
            apv.phase_items[0].put("limit_max", "null");
            if (writing_averge < Double.valueOf(down[0].trim())) {
                si.ii.errorCode[i] = "3.10.2";
                si.ii.errorDes[i] = "Device.MMC.Write fail";
                return false;
            } else if (reading_averge < Double.valueOf(down[1].trim())) {
                si.ii.errorCode[i] = "3.10.1";
                si.ii.errorDes[i] = "Device.MMC.Read fail";
                return false;
            }

            return true;
        } catch (Exception e) {
            addLog1(e.toString(), id);
            e.printStackTrace();
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean thermalShutdown(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {
            to.readUntil(si.ii.diagCmd[i], i);
            if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }

            String details = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]);
            addLog1(details, id);
            if (details.contains(si.ii.spec[i])) {
                resultLog = "PASS";
                return true;
            }
            try {
                Runtime.getRuntime().exec("cmd /c  arp -d");
            } catch (IOException ex) {
                Logger.getLogger(TestScript.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (ping(si.ii.socketIp[i], si.ii.diagCmdTime[i])) {
                addLog1("Ping " + si.ii.socketIp[i] + " PASS!", id);
                resultLog = "PASS";
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean radioCalibration(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        if (!apv.IQlog.containsKey(si.ii.diagCmd[i])) {
            addLog("not have items:" + si.ii.diagCmd[i], id);
            return false;
        }
        String[] details = (String[]) apv.IQlog.get(si.ii.diagCmd[i]);

        for (int j = 0; j < details.length; j++) {
//            addLog(details[j], id);
            if (details[j].contains(si.ii.cmd[i])) {
                addLog(details[j], id);
                if (!details[j].contains(si.ii.spec[i])) {
                    resultLog = "PASS";
                    return true;
                } else {
                    return false;
                }
            }

        }
        addLog("not havecontent:" + si.ii.cmd[i], id);
        return false;
    }

    public boolean xtal_Calibration(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        apv.phase_items = new JSONObject[1];
        apv.phase_items[0] = new JSONObject();
//        addLog1(si.ii.diagCmd[i], id);
        if (!apv.IQlog.containsKey(si.ii.diagCmd[i])) {
            addLog("not have items:" + si.ii.diagCmd[i], id);
            return false;
        }
        String[] details = (String[]) apv.IQlog.get(si.ii.diagCmd[i]);
        boolean bool = true;
        for (int j = details.length - 1; j <= 0; j--) {
            if (details[j].contains(si.ii.spec[i])) {
                addLog("not have items:" + si.ii.diagCmd[i], id);
                bool = false;
            }
//            if (details[j].startsWith("Trigger level:")) {
//                String item[] = details[j].split(",");
//
//                for (int k = 0; k < item.length; k++) {
//                    String name = item[k].split(":")[0];
//                    String value = item[k].split(":")[1];
//                    apv.phase_items[0].put(name, value);
//                }
//            }
        }
        if (bool) {
            resultLog = "PASS";
            return true;
        }

        return false;
    }

    public boolean TX_Power_EVM_Verify(StationInfo si, int i, int id) {

        resultLog = "FAIL";
        String[] testItems = {si.ii.diagCmd1[i], si.ii.diagCmd2[i], si.ii.diagCmd3[i], si.ii.diagCmd4[i],
            si.ii.diagCmd5[i], si.ii.diagCmd6[i], si.ii.diagCmd7[i], si.ii.diagCmd8[i], si.ii.diagCmd9[i],
            si.ii.diagCmd10[i], si.ii.diagCmd11[i], si.ii.diagCmd12[i], si.ii.diagCmd13[i], si.ii.diagCmd14[i],
            si.ii.diagCmd15[i], si.ii.diagCmd16[i]};
        boolean bool = true;
        String[] erro_code = si.ii.errorCode[i].split(",");
        si.ii.errorCode[i] = erro_code[0];
        String[][] NAME = new String[testItems.length][];
        String[][] VALUE = new String[testItems.length][];
        String[][] UNIT = new String[testItems.length][];
        String[][] SPEC_UP = new String[testItems.length][];
        String[][] SPEC_DOWN = new String[testItems.length][];
        String log = "";
        String antistop[] = si.ii.cmd[i].split(",");
        String Items = "";
        int itemsNum = 0;
        try {
            for (int j = 0; j < testItems.length; j++) {
                itemsNum++;
                if ("".equals(testItems[j])) {
                    break;
                }
            }
            apv.phase_items = new JSONObject[itemsNum];
            for (int j = 0; j < itemsNum; j++) {
                apv.phase_items[j] = new JSONObject();
                boolean result = true;
                String lg = "";
                String item = "";
                ArrayList<String> Name = new ArrayList<String>();
                ArrayList<String> Value = new ArrayList<String>();
                ArrayList<String> Unit = new ArrayList<String>();
                ArrayList<String> Spec_up = new ArrayList<String>();
                ArrayList<String> Spec_down = new ArrayList<String>();

                testItems[j] = testItems[j].trim();

                if (!apv.IQlog.containsKey(testItems[j])) {
                    addLog("not have items:" + testItems[j], id);
                    return false;
                }
                String[] details = (String[]) apv.IQlog.get(testItems[j]);

                addLog(details[0], id);
                apv.phase_items[j].put("items_name", testItems[j].substring(testItems[j].indexOf(".") + 1));
                for (int k = 0; k < details.length; k++) {
                    if (details[k].contains("(,)")) {   //没规格
                        continue;
                    }

                    if (details[k].contains("(") && details[k].contains(",") && details[k].contains(")")) {//有规格
                        String name = details[k].substring(0, details[k].indexOf(" ")).trim();

                        details[k] = details[k].substring(details[k].indexOf(":") + 1).trim();
                        String value = details[k].substring(0, details[k].indexOf(" "));

                        String unit = details[k].substring(details[k].indexOf(" "), details[k].indexOf("(") - 1).trim();

                        details[k] = details[k].substring(details[k].indexOf("(")).trim();
                        int index = details[k].indexOf(",");
                        String spec_down = "null";
                        String spec_up = "null";
                        if (index > 1) {
                            spec_down = details[k].substring(1, index).trim();
                        }
                        if (index < details[k].length() - 2) {
                            spec_up = details[k].substring(index + 1, details[k].indexOf(")")).trim();
                        }
                        for (int l = 0; l < antistop.length; l++) {
                            if (name.contains(antistop[l])) {
                                apv.phase_items[j].put(name.toLowerCase() + "_value", value);
                                apv.phase_items[j].put(name.toLowerCase() + "_unit", unit);
                                apv.phase_items[j].put(name.toLowerCase() + "_down", spec_down);
                                apv.phase_items[j].put(name.toLowerCase() + "_up", spec_up);
                                Name.add(name);
                                Value.add(value);
                                Unit.add(unit);
                                Spec_down.add(spec_down);
                                Spec_up.add(spec_up);
                                lg += "," + value;
                                item += "," + name;
                                addLog(name + " " + value + " " + unit + " " + details[k], id);
                            }
                        }
                    }

                    if (details[k].contains(si.ii.spec[i])) {
//                        addLog(testItems[j], id);
                        if (testItems[j].contains("ANT")) {
                            int num = Integer.valueOf(testItems[j].substring(testItems[j].length() - 1));
                            si.ii.errorCode[i] = erro_code[num];
                        }
                        addLog(details[k], id);
                        result = false;
                        bool = false;
//                    break;
                    }
                }

                NAME[j] = Name.toArray(new String[Name.size()]);
                VALUE[j] = Value.toArray(new String[Value.size()]);
                UNIT[j] = Unit.toArray(new String[Unit.size()]);
                SPEC_DOWN[j] = Spec_down.toArray(new String[Spec_down.size()]);
                SPEC_UP[j] = Spec_up.toArray(new String[Spec_up.size()]);
                Items += "," + testItems[j] + item;
                log += "," + result + lg;

            }
            si.ii.resultType[i] = si.ii.resultType[i] + Items;
            if (bool) {
                resultLog = "PASS";
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            apv.testLog[id - 1] += resultLog + log;
            resultLog = "";
        }
        return false;
    }

    public boolean RX_PER_Verify(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        String[] testItems = {si.ii.diagCmd1[i], si.ii.diagCmd2[i], si.ii.diagCmd3[i], si.ii.diagCmd4[i],
            si.ii.diagCmd5[i], si.ii.diagCmd6[i], si.ii.diagCmd7[i], si.ii.diagCmd8[i], si.ii.diagCmd9[i],
            si.ii.diagCmd10[i], si.ii.diagCmd11[i], si.ii.diagCmd12[i], si.ii.diagCmd13[i], si.ii.diagCmd14[i],
            si.ii.diagCmd15[i], si.ii.diagCmd16[i]};
        boolean bool = true;
        String[] erro_code = si.ii.errorCode[i].split(",");
        si.ii.errorCode[i] = erro_code[0];
        String[][] NAME = new String[testItems.length][];
        String[][] VALUE = new String[testItems.length][];
        String[][] UNIT = new String[testItems.length][];
        String[][] SPEC_UP = new String[testItems.length][];
        String[][] SPEC_DOWN = new String[testItems.length][];
        String log = "";
        String Items = "";
        int itemsNum = 0;
        try {
            for (int j = 0; j < testItems.length; j++) {
                itemsNum++;
                if ("".equals(testItems[j])) {
                    break;
                }
            }
            apv.phase_items = new JSONObject[itemsNum];
            for (int j = 0; j < itemsNum; j++) {
                apv.phase_items[j] = new JSONObject();
                boolean result = true;
                String lg = "";
                String item = "";
                ArrayList<String> Name = new ArrayList<String>();
                ArrayList<String> Value = new ArrayList<String>();
                ArrayList<String> Unit = new ArrayList<String>();
                ArrayList<String> Spec_up = new ArrayList<String>();
                ArrayList<String> Spec_down = new ArrayList<String>();

                testItems[j] = testItems[j].trim();

                if (!apv.IQlog.containsKey(testItems[j])) {
                    addLog("not have items:" + testItems[j], id);
                    return false;
                }
                String[] details = (String[]) apv.IQlog.get(testItems[j]);

                addLog(testItems[j], id);
                apv.phase_items[j].put("items_name", testItems[j].substring(testItems[j].indexOf(".") + 1));
                for (int k = 0; k < details.length; k++) {
                    if (details[k].contains("(,)")) {   //没规格
                        continue;
                    }
                    if (details[k].contains("(") && details[k].contains(",") && details[k].contains(")")) {//有规格
                        String name = details[k].substring(0, details[k].indexOf(" ")).trim();
                        details[k] = details[k].substring(details[k].indexOf(":") + 1).trim();
                        String value = details[k].substring(0, details[k].indexOf(" "));
                        String unit = details[k].substring(details[k].indexOf(" "), details[k].indexOf("(") - 1).trim();
                        details[k] = details[k].substring(details[k].indexOf("(")).trim();
                        int index = details[k].indexOf(",");
                        String spec_down = "null";
                        String spec_up = "null";
                        if (index > 1) {
                            spec_down = details[k].substring(1, index).trim();
                        }
                        if (index < details[k].length() - 2) {
                            spec_up = details[k].substring(index + 1, details[k].indexOf(")")).trim();
                        }
                        apv.phase_items[j].put(name.toLowerCase() + "_value", value);
                        apv.phase_items[j].put(name.toLowerCase() + "_unit", unit);
                        apv.phase_items[j].put(name.toLowerCase() + "_down", spec_down);
                        apv.phase_items[j].put(name.toLowerCase() + "_up", spec_up);
                        Name.add(name);
                        Value.add(value);
                        Unit.add(unit);
                        Spec_down.add(spec_down);
                        Spec_up.add(spec_up);
                        lg += "," + value;
                        item += "," + name;
                        addLog(name + " " + value + " " + unit + " " + details[k], id);
                        if (value.contains("0")) {
                            System.out.println("");
                        }
                    }

                    if (details[k].contains(si.ii.spec[i])) {
//                        addLog(testItems[j], id);
                        if (testItems[j].contains("ANT")) {
                            int num = Integer.valueOf(testItems[j].substring(testItems[j].length() - 1));
                            si.ii.errorCode[i] = erro_code[num];
                        }
                        addLog(details[k], id);
                        result = false;
                        bool = false;
//                    break;
                    }
                }

                NAME[j] = Name.toArray(new String[Name.size()]);
                VALUE[j] = Value.toArray(new String[Value.size()]);
                UNIT[j] = Unit.toArray(new String[Unit.size()]);
                SPEC_DOWN[j] = Spec_down.toArray(new String[Spec_down.size()]);
                SPEC_UP[j] = Spec_up.toArray(new String[Spec_up.size()]);
                Items += "," + testItems[j] + item;
                log += "," + result + lg;

            }
            si.ii.resultType[i] = si.ii.resultType[i] + Items;

            if (bool) {
                resultLog = "PASS";
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            apv.testLog[id - 1] += resultLog + log;
            resultLog = "";
        }
        return false;
    }

    public boolean ambitMMCReadTest(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {
            to.readUntil(si.ii.diagCmd[i], i);
            if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }

            String details = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]);
            addLog1(details, id);
            if (details.contains(si.ii.spec[i])) {
                resultLog = "PASS";
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean ambitWriteEthernetMacs(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        if (!apv.getSfisStatus()) {
            addLog("cancelled  tiem", id);
            resultLog = "PASS";
            return true;
        }
        String eth0 = macTranslate(apv.ethMac[id - 1]).toLowerCase();
//        long num = Long.parseLong(apv.ethMac[id - 1], 16);
//        String eth1 = macTranslate(Long.toHexString(num + 1)).toLowerCase();
//        apv.phase_items = new JSONObject[1];

        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {
            to.readUntil(si.ii.diagCmd[i], i);

            addLog1("Command: " + si.ii.cmd[i] + " " + eth0, id);
            if (!to.sendCommandAndRead(si.ii.cmd[i] + " " + eth0, si.ii.diagCmd[i], 3)) {  //write mac
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }
            resultLog = "PASS";
            return true;
//            }
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean setUBootVersion(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        if (!apv.getSfisStatus()) {
            addLog("cancelled  tiem", id);
            resultLog = "PASS";
            return true;
        }
//        String eth0 = macTranslate(apv.ethMac[id - 1]).toLowerCase();
//        long num = Long.parseLong(apv.ethMac[id - 1], 16);
//        String eth1 = macTranslate(Long.toHexString(num + 1)).toLowerCase();
//        apv.phase_items = new JSONObject[1];

        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {
            to.readUntil(si.ii.diagCmd[i], i);

            addLog1("Command: " + si.ii.cmd[i], id);
            if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {  //write mac
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }
            resultLog = "PASS";
            return true;
//            }
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean ambitEthernetMacCheck(StationInfo si, int i, int id) {
        resultLog = "FAIL";

        String eth0 = macTranslate(apv.ethMac[id - 1]).toLowerCase();
        long num = Long.parseLong(apv.ethMac[id - 1], 16);
        String eth1 = macTranslate(Long.toHexString(num + 1)).toLowerCase();
        apv.phase_items = new JSONObject[1];
        apv.phase_items[0] = new JSONObject();
        apv.phase_items[0].put("eth0", eth0);
        apv.phase_items[0].put("eth1", eth1);
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {

            to.readUntil(si.ii.diagCmd[i], i);

            if (!to.sendCommandAndRead(si.ii.cmd1[i], si.ii.diagCmd[i], 3)) {   //get  mac
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }
            String details = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]);

            String eth[] = details.split("\n");
            apv.phase_items[0].put("eth0", eth[0]);
            apv.phase_items[1].put("eth1", eth[1]);
            if (eth0.equals(eth[0]) && eth1.equals(eth[1])) {
                resultLog = "PASS";
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean writeProductSerialNumber(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        if (!apv.getSfisStatus()) {
            addLog("cancelled  tiem", id);
            resultLog = "PASS";
            return true;
        }

//        StringBuilder Serial = new StringBuilder(apv.sn[id - 1]);
//        String[] str = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
//            "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "M", "N",
//            "P", "Q", "R", "S", "T", "V", "W", "X", "Y", "Z"}; //not have I L O U
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {
//            for (int j = 0; j < 8; j++) {
//                Random r = new Random();
//                String sn = str[r.nextInt(str.length)];
//                Serial.append(sn);
//            }
//            String deveice_sn = Serial.toString();
            addLog1("write Serial: " + apv.MLBSN, id);
            to.readUntil(si.ii.diagCmd[i], i);

            if (!to.sendCommandAndRead(si.ii.cmd[i] + apv.MLBSN, si.ii.diagCmd[i], 3)) {  //write mac
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }
            apv.mlbSn[id - 1] = apv.MLBSN;
            resultLog = "PASS";
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
            return false;
        } finally {
            to.disconnect();
        }

    }

    public boolean ambitSubsystemTest(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        String[] cmd = {si.ii.cmd1[i], si.ii.cmd2[i], si.ii.cmd3[i], si.ii.cmd4[i], si.ii.cmd5[i], si.ii.cmd6[i]};
        String[] spec = {si.ii.diagCmd1[i], si.ii.diagCmd2[i], si.ii.diagCmd3[i], si.ii.diagCmd4[i], si.ii.diagCmd5[i], si.ii.diagCmd6[i]};
        String[] item = si.ii.cmd[i].split(",");
        apv.phase_items = new JSONObject[item.length];
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {
            to.readUntil(si.ii.diagCmd[i], 3);

            boolean result = true;
            for (int j = 0; j < item.length; j++) {


                if (!to.sendCommandAndRead(cmd[j], si.ii.diagCmd[i], 3)) {
                    addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                    return false;
                }
                String value = substring(to.getString, cmd[j], si.ii.diagCmd[i]);
                if (!value.contains(spec[j])) {
                    addLog(item[j] + " Fail", id);
                    result = false;
                }
                apv.phase_items[j] = new JSONObject();
                apv.phase_items[j].put("name", item[j]);
                apv.phase_items[j].put("value", value);
                apv.phase_items[j].put("spec", spec[j]);
            }

            if (result) {
                resultLog = "PASS";
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean ambitResetButtonTest(StationInfo si, int i, int id) {
        resultLog = "FAIL";
//        TipWindow tw = new TipWindow(si.ii.diagCmdTime[i]);          //提示框时间
        ComPort cp = new ComPort(apv, id);
        apv.phase_items = new JSONObject[1];
        apv.phase_items[0] = new JSONObject();
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        if (!cp.open(si.ii.comPort[i], si.ii.transRate[i])) {
            addLog1(si.ii.comPort[i] + " Fail!", id);
            cp.close();
            return false;
        }
        try {

            to.readUntil(si.ii.diagCmd[i], 3);
            cp.write(si.ii.cmd1[i]);                 //按下复位键
            Thread.sleep(si.ii.diagCmdTime[i] * 1000);
            cp.write(si.ii.cmd2[i]);                 //松开复位键
            apv.phase_items[0].put("spec", si.ii.diagCmdTime[i]);

            if (to.readUntil(si.ii.spec[i], si.ii.diagCmdTime[i])) {
                apv.phase_items[0].put("value", to.getString);
                addLog1("Reset button  PASS!", id);
                resultLog = "PASS";
                return true;
            }
            apv.phase_items[0].put("value", to.getString);

            addLog1("Reset button  Fail!", id);
            return false;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cp.write(si.ii.cmd2[i]);
            to.disconnect();
            cp.close();
        }
        return false;
    }

    public boolean firmwareUpdate(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        TipWindow tw = new TipWindow(si.ii.diagCmdTime[i]);          //提示框时间
        ComPort cp = new ComPort(apv, id);
        if (!cp.open(si.ii.comPort[i], si.ii.transRate[i])) {
            addLog1("open " + si.ii.comPort[i] + " Fail!", id);
            return false;
        }
        try {
            tw.start(question[21]);
            for (int j = 0; j < si.ii.diagCmdTime[i]; j++) {
                if (cp.writeAndReadUntil("\r\n", si.ii.diagCmd[i], 1)) {
                    tw.stop();
                    break;
                }
                if (cp.readAll.contains("root@OpenWrt:/# ")) {
                    cp.write("reboot");
                    try {
                        Thread.sleep(1000);
                    } catch (EnumConstantNotPresentException e) {
                        e.notifyAll();
                    }
                }
                if (j >= si.ii.diagCmdTime[i] - 1) {
                    addLog("login bootloader fail", id);
                    return false;
                }
            }
            cp.writeAndReadUntil(si.ii.cmd1[i] + "\r\n", si.ii.diagCmd[i], 1);
            cp.writeAndReadUntil(si.ii.cmd2[i] + "\r\n", si.ii.diagCmd[i], 1);
            for (int j = 0; j < 5; j++) {
                cp.writeAndReadUntil(si.ii.cmd3[i] + "\r\n", si.ii.diagCmd[i], si.ii.diagCmdTime[i]);
                if (cp.readAll.contains("is alive")) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (EnumConstantNotPresentException e) {
                    e.notifyAll();
                }
            }
            if (!cp.readAll.contains("is alive")) {
                addLog("cmd fail: " + si.ii.cmd3[i], id);
                return false;
            }

            cp.writeAndReadUntil(si.ii.cmd4[i] + "\r\n", si.ii.diagCmd[i], si.ii.diagCmdTime[i]);
            if (!cp.readAll.contains("ipq807x_eth_halt: done")) {
                addLog("cmd fail: " + si.ii.cmd4[i], id);
                return false;
            }
            cp.writeAndReadUntil(si.ii.cmd5[i] + "\r\n", si.ii.diagCmd[i], 1);
            cp.writeAndReadUntil(si.ii.cmd6[i] + "\r\n", si.ii.diagCmd[i], 1);
            cp.writeAndReadUntil(si.ii.cmd7[i] + "\r\n", si.ii.diagCmd[i], si.ii.diagCmdTime[i]);
            String[] details = cp.readAll.split("\n");
            int count = 0;
            for (int j = 0; j < details.length; j++) {
                if (details[j].contains("[ done ]")) {
                    count++;
                }

            }
            int spec = Integer.parseInt(si.ii.spec[i].trim());
            if (spec != count) {
                addLog("done count=" + count + "  spec=" + spec, id);
                return false;

            }
            cp.write("reset");
            cp.readString(2);
            for (int j = 0; j < si.ii.diagCmdTime[i]; j++) {
                if (cp.writeAndReadUntil("\r\n", si.ii.diagCmd[i], 1)) {
                    tw.stop();
                    break;
                }
                if (cp.readAll.contains("root@OpenWrt:/# ")) {
                    cp.write("reboot");
                    try {
                        Thread.sleep(1000);
                    } catch (EnumConstantNotPresentException e) {
                        e.notifyAll();
                    }
                }
                if (j >= si.ii.diagCmdTime[i] - 1) {
                    addLog("login bootloader fail", id);
                    return false;
                }
            }
            cp.writeAndReadUntil("setenv bootcmd bootipq\r\n", si.ii.diagCmd[i], 1);
            cp.writeAndReadUntil("saveenv\r\n", si.ii.diagCmd[i], 1);

            resultLog = "PASS";
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cp.close();
            tw.stop();
        }
        return false;
    }

    public boolean ambitLEDIrradianceTest(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        String[] erroCode = si.ii.errorCode[i].split(",");
        try {

            String on = si.ii.spec[i].split(",")[0].trim();
            String off = si.ii.spec[i].split(",")[1].trim();
            String cmd[] = {si.ii.cmd1[i], si.ii.cmd2[i], si.ii.cmd3[i], si.ii.cmd4[i]};
            String ledName[] = {"blue", "green", "red", "white"};

            boolean status = true;
            if (!to.connect(si.ii.socketIp[i])) {
                addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
                return false;
            }

            to.readUntil(si.ii.diagCmd[i], i);
            if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                si.ii.errorCode[i] = erroCode[0];
                return false;
            }
            for (int j = 0; j < cmd.length; j++) {
                QuestionWindow qw = new QuestionWindow(si.ii.diagCmdTime[i]);
                if (!to.sendCommandAndRead(cmd[j] + " " + on, si.ii.diagCmd[i], 3)) {   //打开灯
                    addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                    return false;
                }
                qw.start(question[6 + j]);
                if (!qw.getResult(si.ii.diagCmdTime[i])) {
                    addLog1("LED " + ledName[j] + " Fail!", id);
                    si.ii.errorCode[i] = erroCode[j + 1];
                    status = false;
                }
                qw.stop();
                if (!to.sendCommandAndRead(cmd[j] + " " + off, si.ii.diagCmd[i], 3)) {  //关闭
                    addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                    return false;
                }
            }
            if (status) {
                resultLog = "PASS";
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean LEDIrradianceTest(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        String[] erroCode = si.ii.errorCode[i].split(",");
        ComPort cp = new ComPort(apv, id);
        try {

            String on = si.ii.spec[i].split(",")[0].trim();
            String off = si.ii.spec[i].split(",")[1].trim();
            String[] cmd = {si.ii.cmd1[i], si.ii.cmd2[i], si.ii.cmd3[i], si.ii.cmd4[i]};
            String[] ledName = si.ii.Cut0[i].split(",");

            String[] spec_up = {si.ii.diagCmd1[i], si.ii.diagCmd2[i], si.ii.diagCmd3[i]};
            String[] spec_down = {si.ii.Cut1[i], si.ii.Cut2[i], si.ii.Cut3[i]};

            boolean status = true;
            if (!to.connect(si.ii.socketIp[i])) {
                addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
                return false;
            }
            if (!cp.open(si.ii.comPort[i], si.ii.transRate[i])) {
                addLog1(si.ii.comPort[i] + " Fail!", id);
                cp.close();
                return false;
            }
            to.readUntil(si.ii.diagCmd[i], i);
            if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                si.ii.errorCode[i] = erroCode[0];
                return false;
            }

            apv.phase_items = new JSONObject[cmd.length];
            for (int j = 0; j < cmd.length; j++) {

                if (!to.sendCommandAndRead(cmd[j] + " " + on, si.ii.diagCmd[i], 3)) {   //打开灯
                    addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                    return false;
                }
                try {
                    Thread.sleep(1000);
                } catch (EnumConstantNotPresentException e) {
                    e.notifyAll();
                }
                String spec = ledName[j].toUpperCase() + "ON";
                if (cp.writeAndReadUntil(si.ii.cmd10[i], "\r\n", 3)) {
                    addLog(cp.readAll + " Fail! need have " + spec, id);
                    return false;
                }
                int[] rgb = new int[3];
                String str = cp.readAll.substring(cp.readAll.indexOf("LED_R=") + 6).trim();
                rgb[0] = Integer.parseInt(str.substring(0, cp.readAll.indexOf(",")).trim());
                str = cp.readAll.substring(cp.readAll.indexOf("LED_G=") + 6).trim();
                rgb[1] = Integer.parseInt(str.substring(0, cp.readAll.indexOf(",")).trim());
                str = cp.readAll.substring(cp.readAll.indexOf("LED_B=") + 6).trim();
                rgb[2] = Integer.parseInt(str.substring(0, cp.readAll.indexOf(",")).trim());

                String[] Up = spec_up[j].split(",");
                String[] Down = spec_down[j].split(",");
                addLog(ledName[j] + ":" + rgb[0] + "," + rgb[1] + "," + rgb[2], id);
                apv.phase_items[j] = new JSONObject();
                apv.phase_items[j].put("color", ledName[j]);
                apv.phase_items[j].put("red_value", rgb[0]);
                apv.phase_items[j].put("green_value", rgb[1]);
                apv.phase_items[j].put("blue_value", rgb[2]);
                apv.phase_items[j].put("red_up", Up[0].trim());
                apv.phase_items[j].put("green_up", Up[1].trim());
                apv.phase_items[j].put("blue_up", Up[2].trim());
                apv.phase_items[j].put("red_down", Down[0].trim());
                apv.phase_items[j].put("green_down", Down[1].trim());
                apv.phase_items[j].put("blue_down", Down[2].trim());

                for (int k = 0; k < rgb.length; k++) {
                    int up = Integer.parseInt(Up[k].trim());
                    int down = Integer.parseInt(Down[k].trim());
                    if (rgb[k] > up) {
                        addLog("spec_up:" + Up[0] + "," + Up[1] + "," + Up[2], id);
                        status = false;
                        si.ii.errorCode[i] = erroCode[j + 1];
                        si.ii.errorDes[i] = "led " + ledName + " up fail";
                        break;
                    } else if (rgb[k] < down) {
                        addLog("spec_down:" + Down[0] + "," + Down[1] + "," + Down[2], id);
                        status = false;
                        si.ii.errorCode[i] = erroCode[j + 1];
                        si.ii.errorDes[i] = "led " + ledName + " down fail";
                        break;
                    }
                }

                if (!to.sendCommandAndRead(cmd[j] + " " + off, si.ii.diagCmd[i], 3)) {  //关闭
                    addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                    return false;
                }

            }
            if (status) {
                resultLog = "PASS";
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);

        } finally {
            cp.close();
            to.disconnect();
        }
        return false;
    }

    public boolean ambitHWVerCheck(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {
            to.readUntil(si.ii.diagCmd[i], i);
            if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }

            String details = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]);
            addLog(details, id);
            apv.phase_items = new JSONObject[1];
            apv.phase_items[0] = new JSONObject();
            apv.phase_items[0].put("value", details);
            apv.phase_items[0].put("spec", si.ii.spec[i]);
            resultLog = details;
            if (details.contains(si.ii.spec[i])) {

                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean USBHighSpeedTest(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        to.readUntil(si.ii.diagCmd[i], i);
        if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {
            addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
            return false;
        }

        String details = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]);
        addLog(details, id);
        apv.phase_items = new JSONObject[1];
        apv.phase_items[0] = new JSONObject();
        apv.phase_items[0].put("value", details);
        apv.phase_items[0].put("spec", si.ii.spec[i]);
        if (details.contains(si.ii.spec[i])) {
            resultLog = "PASS";
            return true;
        }
        return false;
    }

    public boolean USBReadandWriteTest(StationInfo si, int i, int id) {

        resultLog = "FAIL";
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {

            to.readUntil(si.ii.diagCmd[i], i);
            if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }

            String details = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]);
            addLog1(details, id);

            if (details.indexOf("Writing") < 0) {
                return false;
            }
            boolean bool = true;
            int value_seat = details.indexOf("seconds, ") + 8;
            double write = Double.valueOf(details.substring(value_seat, details.indexOf("KB/s")).trim());
            addLog1("write=" + write, id);

            if (write < Double.valueOf(si.ii.limitDown[i])) {
                addLog("write " + write + "< " + si.ii.limitDown[i], id);
                bool = false;
            }
            if (details.indexOf("Reading") < 0) {
                return false;
            }
            details = details.substring(details.indexOf("Reading"));
            value_seat = details.indexOf("seconds, ") + 8;
            double read = Double.valueOf(details.substring(value_seat, details.indexOf("KB/s")).trim());
            addLog1("read=" + read, id);
            resultLog = write + "-" + read;
            if (read < Double.valueOf(si.ii.limitDown[i])) {
                addLog("read " + read + "< " + si.ii.limitDown[i], id);
                bool = false;
            }
            apv.phase_items = new JSONObject[2];
            apv.phase_items[0] = new JSONObject();
            apv.phase_items[0].put("model", "write");
            apv.phase_items[0].put("value", write);
            apv.phase_items[0].put("limit_down", si.ii.limitDown[i]);
            apv.phase_items[0].put("limit_up", "null");
            apv.phase_items[1] = new JSONObject();
            apv.phase_items[1].put("model", "read");
            apv.phase_items[1].put("value", read);
            apv.phase_items[1].put("limit_down", si.ii.limitDown[i]);
            apv.phase_items[1].put("limit_up", "null");
            if (bool) {
                resultLog = "PASS";
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            to.disconnect();
        }
        return false;

    }

    public boolean telnetBatchProcessingFile(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        apv.phase_items = new JSONObject[1];
        apv.phase_items[0] = new JSONObject();
        apv.phase_items[0].put("ip", si.ii.socketIp[i]);
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }

        File CmdFile = new File(si.ii.cmd[i]);

        if (!CmdFile.exists()) {
            this.addLog1("not have file " + si.ii.cmd[i], id);
            return false;
        }

        BufferedReader br = null;

        String iqLogLine = "";
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(CmdFile)));
            int num = 0;
            while ((iqLogLine = br.readLine()) != null) {
                to.sendCommand(iqLogLine);
                apv.phase_items[0].put("command" + num, iqLogLine);
                num++;
                try {
                    Thread.sleep(200);
                    //                to.readUntil(si.ii.diagCmd[i], 5);
                } catch (InterruptedException ex) {
                    Logger.getLogger(TestScript.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            to.readAll(si.ii.diagCmdTime[i]);

            resultLog = "PASS";
            return true;


        } catch (IOException e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean telnetBatchProcessing(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        if (!to.connect(si.ii.socketIp[i])) {   //先telnet
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        ArrayList<String> command = new ArrayList<String>();
        String[] cmd = {si.ii.diagCmd1[i], si.ii.diagCmd2[i], si.ii.diagCmd3[i],
            si.ii.diagCmd4[i], si.ii.diagCmd5[i], si.ii.diagCmd6[i],
            si.ii.diagCmd7[i], si.ii.diagCmd8[i], si.ii.diagCmd9[i],
            si.ii.diagCmd10[i], si.ii.diagCmd11[i], si.ii.diagCmd12[i],
            si.ii.diagCmd13[i], si.ii.diagCmd14[i], si.ii.diagCmd15[i],
            si.ii.diagCmd16[i]};//最多可以放16个指令
        for (int j = 0; j < cmd.length; j++) { //调整指令有效指令集合在一起
            if (!"".equals(cmd[j])) {
                command.add(cmd[j]);
            }
        }
        cmd = command.toArray(new String[command.size()]);

        try {

            to.readUntil(si.ii.diagCmd[i], 1); //清除telnet开头内容
            apv.phase_items = new JSONObject[0];
            apv.phase_items[0] = new JSONObject();

            for (int j = 0; j < cmd.length; j++) {
                //下指令 命令  结束语句 最长时间S
                to.sendCommand(cmd[j]);
                apv.phase_items[0].put("command" + (j + 1), cmd[j]);
//                addLog(to.getString, id);
                Thread.sleep(200);
            }
            to.readAll(si.ii.diagCmdTime[i]);
            addLog(to.getString, id);
            if (to.getString.contains(si.ii.spec[i])) {//判断最后一个指令有没包含si.ii.spec[i]
                resultLog = "PASS";
                return true;
            }
//            }

        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean commandBatchProcessing(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        ComPort cp = new ComPort(apv, id);
        if (!cp.open(si.ii.comPort[i], si.ii.transRate[i])) {
            addLog1("open " + si.ii.comPort[i] + " Fail!", id);
            return false;

        }

        try {

            File CmdFile = new File(si.ii.cmd[i]);
            if (!CmdFile.exists()) {
                this.addLog("not have file " + si.ii.cmd[i], id);
                return false;
            }

            BufferedReader br = null;

            String iqLogLine = "";

            br = new BufferedReader(new InputStreamReader(new FileInputStream(CmdFile)));
            while ((iqLogLine = br.readLine()) != null) {
                cp.write(iqLogLine + "\r\n");
                try {
                    Thread.sleep(200);
                    //                to.readUntil(si.ii.diagCmd[i], 5);
                } catch (InterruptedException ex) {
                    Logger.getLogger(TestScript.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(TestScript.class.getName()).log(Level.SEVERE, null, ex);
            }
//            cp.readUntil(resultLog, id);
//           String spec[]=si.ii.spec[i].split(",");
//          String  details=to.readTime(10);
//          addLog(details, id);
//            for (int j = 0; j < spec.length; j++) {
            if (cp.readUntil(si.ii.spec[i], 30)) {
                resultLog = "PASS";
                return true;
            }
//            }

        } catch (IOException e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            cp.close();
        }
        return false;
    }

    public boolean desenseTRXtest(StationInfo si, int i, int id) {
        resultLog = "FAIL";

        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        //-------------------------start dut command---------------
        File CmdFile = new File(si.ii.cmd[i]);
        if (!CmdFile.exists()) {
            this.addLog1("not have file " + si.ii.cmd[i], id);
            return false;
        }
        BufferedReader br = null;
        String iqLogLine = "";
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(CmdFile)));
            while ((iqLogLine = br.readLine()) != null) {
                to.sendCommand(iqLogLine);
                try {
                    Thread.sleep(200);
                    //                to.readUntil(si.ii.diagCmd[i], 5);
                } catch (InterruptedException ex) {
                    Logger.getLogger(TestScript.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            to.readAll(2);
//            if (!to.getString.contains(si.ii.spec[i])) {
//             
//                return false;
//            }
        } catch (IOException e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        }
        this.addLog("cmd file " + si.ii.cmd[i] + " OK", id);
        //------------------------run  iQ----------------------------------
        try {
            Thread.sleep(2000);
            File IQLogFile = null;

            IQLogFile = new File(si.ii.diagCmd1[i]);
            if (IQLogFile.exists()) {
                IQLogFile.delete();
            }
            if (IQLogFile.exists()) {
                apv.showConfirmDialog(id, si.ii.cmd1[i] + "\r\nDelete fail");
                return false;
            }
            this.addLog("runing IQ " + si.ii.diagCmd[i], id);
            Runtime.getRuntime().exec(si.ii.diagCmd[i]);
            Thread.sleep(5000);

            if (!IQLogFile.exists()) {
                this.addLog("not have " + si.ii.diagCmd1[i], id);
                return false;
            }

            //保存文件路径
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());//设置日期格式
            String Time = new SimpleDateFormat("HHmmss").format(new Date());//设置日期格式  
            String filePath = si.logPath + File.separator + "IQlog" + File.separator + date + File.separator;
            File fnewpath = new File(filePath); //文件新（目标）地址 
            if (!fnewpath.exists()) //判断文件夹是否存在 
            {
                fnewpath.mkdirs();
            }
            //保存文件路径
            filePath = filePath + apv.sn[id - 1] + "_" + si.ii.itemDes[i] + "_" + Time + ".txt";

            readIQlog(IQLogFile, filePath, si.ii.diagCmdTime[i]);
            ParseIQLog(IQLogFile);
            if (apv.IQlog.containsKey("RESERT")) {
                this.addLog1(" IQ end of run", id);
//                String[] details = (String[]) apv.IQlog.get("RESERT");
//                for (int j = 0; j < details.length; j++) {
//                    if (details[j].contains("F A I L") || (details[j].contains("[Failed]"))) {
//                        return false;
//                    } else if (details[j].contains("P A S S")) {
                resultLog = "PASS";
                return true;
//                    }
//                }
//                return false;
            } else {
                this.addLog1(" log_all.txt erro", id);
            }

            resultLog = "PASS";
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        } finally {
            //-------------------------close dut command---------------
            to.sendCommand("sleep 1");
            to.sendCommand("qcatestcmd -i wifi0 --tx off --phyId 0");
            to.sendCommand("qcatestcmd -i wifi0 --tx off --phyId 1");
            to.sendCommand("qcatestcmd -i wifi0 --tx off --phyId 2");
            to.sendCommand("sleep 1");

            try {
                Runtime.getRuntime().exec("taskkill /f /t /im  IQfactRun_Console.exe");
            } catch (IOException ex) {
                Logger.getLogger(TestScript.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(TestScript.class.getName()).log(Level.SEVERE, null, ex);
            }
            to.disconnect();
        }

    }

    public boolean ambitVerifyBootcodeVersion(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        try {

            if (!to.connect(si.ii.socketIp[i])) {
                addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
                return false;
            }
            to.readUntil(si.ii.diagCmd[i], i);
            if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }

            String details = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]);
            addLog1(details, id);
            if (details.contains(si.ii.spec[i])) {
                resultLog = "PASS";
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    public boolean testImageVersion(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {
            to.readUntil(si.ii.diagCmd[i], i);
            if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }

            String version = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]);
            addLog1(version, id);
            apv.uploadAPI.setValue("test_image_version", version);
            if (to.getString.contains(si.ii.spec[i])) {
                resultLog = "PASS";
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    /**
     * 字符串截取
     *
     * @param str 原字符串
     * @param start 开始截取的字符串
     * @param end 结束截取的字符串
     * @return 砍头去尾留中间
     */
    public String substring(String str, String start, String end) {
        str.substring(start.length(), str.length() - end.length()).trim();
        return str.substring(start.length(), str.length() - end.length()).trim();
    }

    /**
     * MAC格式转换
     *
     * @param mac 字符串16位
     *
     * @return XX:XX:XX:XX:XX:XX
     */
    public String macTranslate(String mac) {
        char[] ch = mac.toCharArray();
        StringBuffer Mac = new StringBuffer();

        for (int i = 0; i < ch.length; i++) {

            if (i % 2 == 0 && i != 0) {
                Mac.append(":");
            }
            Mac.append(ch[i]);
        }
        return Mac.toString();
    }

    public boolean ambitPowerTest(StationInfo si, int i, int id) {
        resultLog = "FAIL";
        if (!to.connect(si.ii.socketIp[i])) {
            addLog1("Telnet " + si.ii.socketIp[i] + " Fail!", id);
            return false;
        }
        try {
            to.readUntil(si.ii.diagCmd[i], i);
            if (!to.sendCommandAndRead(si.ii.cmd[i], si.ii.diagCmd[i], 3)) {
                addLog1("CMD " + si.ii.cmd[i] + " Fail!", id);
                return false;
            }

            String version = substring(to.getString, si.ii.cmd[i], si.ii.diagCmd[i]);
            addLog1(version, id);

            if (to.getString.contains(si.ii.spec[i])) {
                resultLog = "PASS";
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), id);
        } finally {
            to.disconnect();
        }
        return false;
    }

    /**
     * 截圖
     *
     * @param vlc_title 標題欄
     * @param fileName 圖片路徑
     * @param time 判斷時間
     * @param id 輸出控制臺
     * @return boolean
     *
     */
    public boolean PictureLive(String vlc_title, String fileName, int time, int id) {            //截图
        W32API.HWND hwnd = null;
        try {
            Toolkit tk = Toolkit.getDefaultToolkit();
            Dimension screensize = tk.getScreenSize();
            int j = 0;
            int WindowWidth = screensize.width;
            int WindowHeight = screensize.height;

            for (j = 0; j <= time; j++) {
                boolean bool = false;
                String VLV_title[] = vlc_title.split(",");
                for (int i = 0; i < VLV_title.length; i++) {
                    hwnd = User32.INSTANCE.FindWindow(null, VLV_title[i]);//判断VLC media player程序是否
                    if (hwnd == null) {
                        // System.out.println("get " + vlc_title + " hwnd=" + hwnd + " fail");
                        addLog("get " + vlc_title + " hwnd=" + hwnd + " fail", id);
                    } else {
                        addLog("get " + vlc_title + " hwnd=" + hwnd + " Pass", id);
                        User32.INSTANCE.ShowWindow(hwnd, 5);
                        //  User32.INSTANCE.SetForegroundWindow(hwnd);
                        bool = true;
                        break;
                    }
                }
                if (bool) {
                    break;
                }
                if (j == time) {
                    return false;
                }
                Thread.sleep(1000);
            }

            if (User32.INSTANCE.SetForegroundWindow(hwnd) && User32.INSTANCE.MoveWindow(hwnd, 0, 0, WindowWidth, WindowHeight)) {
////                if (User32.INSTANCE.FindWindow(0, "dshow://dshow-vdev=DMx 51BU02 - "+ vlc_title) == null || !User32.INSTANCE.SetForegroundWindow(hwnd) || !User32.INSTANCE.MoveWindow(hwnd, 0, 0, WindowWidth, WindowHeight)) {        
//                System.out.println(vlc_title + " FAIL");
                Thread.sleep(500);
                ScreenShot screenshot = new ScreenShot();
                screenshot.Shot(fileName, 0, 0, WindowWidth, WindowHeight);
            }

//                User32.INSTANCE.ShowWindow(hwnd, 0);
//          
            return true;
        } catch (InterruptedException ex) {
            addLog(ex.getMessage(), id);

        } finally {
            if (hwnd != null) {
                //     User32.INSTANCE.ShowWindow(hwnd, 0);
                hwnd = User32.INSTANCE.FindWindow(null, "AmbitUI");
                User32.INSTANCE.SetForegroundWindow(hwnd);
            }
        }
        return false;
    }

    public boolean readIQlog(File file, String newpath, int time) {

        BufferedReader br = null;

        String iqLogLine = "";
        BufferedWriter out = null;

        int num = 0;

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

            out = new BufferedWriter(new FileWriter(newpath, true));
            while (num < time) {

                iqLogLine = br.readLine();
//                String data = str;

                if (iqLogLine == null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(NewMain.class.getName()).log(Level.SEVERE, null, ex);
                    }

                } else {
                    num = 0;
                    System.out.println(iqLogLine);
                    addLog(iqLogLine, 1);

                    out.write(iqLogLine + "\r\n");
                    out.flush();
                    if (iqLogLine.startsWith("IQfactRun_Console:")) {

                        return false;
                    } else if (iqLogLine.startsWith("press any key to continue")) {

                        return true;
                    } else if (iqLogLine.contains("P       A   A   SSSS    SSSS")) {

                        return true;
                    } else if (iqLogLine.startsWith("Failed Run(s) on Limits")) {

                        return true;
                    }
                }
                num++;

            }
            System.out.println("*****************");
            System.out.println(iqLogLine);
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try {
                br.close();
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(TestScript.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    public class ReadIQlog extends Thread {

        public String getString = "";
        private File file;
        public boolean bool = true;

        public ReadIQlog(File file) {
            this.file = file;
            bool = true;
        }

        @Override
        public void run() {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                while (bool) {
                    getString = br.readLine();
                    System.out.println(getString);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                bool = false;
            } finally {
                try {
                    br.close();
                } catch (IOException ex) {
                    Logger.getLogger(TestScript.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
    }

    /**
     * DOS 命令针对执行后不会停止的CMD的方法
     *
     * @param cmd 指令
     * @param time 多少秒内没有回传值就结束
     * @return 返回最后一行内容
     */
    public String DOS_Set_read_cmd(String cmd, int time) {
        DOS_Set_read_cmd dos = new DOS_Set_read_cmd(cmd);
        dos.start();
        String str = "";
        int conter = 0;
        while (conter < time) {
            if (!str.equals(dos.getString)) {
                str = dos.getString;
                conter = 0;
//                if (str.contains("SUM")) {
                addLog(str, 1);

//                }
            } else {
                try {
                    conter++;
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(TestScript.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        dos.stop();
        return dos.stringbuffer.toString();
    }

    public class DOS_Set_read_cmd extends Thread {

        private String cmd;
        String getString = "";
        StringBuffer stringbuffer = new StringBuffer();

        public DOS_Set_read_cmd(String cmd) {
            this.cmd = cmd;
        }

        @Override
        public void run() {

            try {
                BufferedReader br = null;
                Process p;
                p = Runtime.getRuntime().exec(cmd);
                String line = "";
                br = new BufferedReader(new InputStreamReader(p.getInputStream(), "GB2312"));
                while ((line = br.readLine()) != null) {
                    getString = line;
                    if (line.contains("local")) {
                        br.readLine();
                    }
                    stringbuffer.append(line).append("\r\n");
//                    if (getString.contains("SUM")) {
//                        addLog(getString, 1);
//
//                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(NewMain.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    public double MesValueToEngValue(int value, int range) {
        return (((double) value - 32768) / 32768) * range;
    }
}
