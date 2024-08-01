package com.example.myappdemo.widget;

import android.serialport.SerialPort;

import com.example.myappdemo.utils.MyUtils;
import com.october.lib.logger.LogUtils;
import com.vi.vioserial.BaseSerial;
import com.vi.vioserial.util.SerialDataUtils;


public abstract class SerialHelper {
    private final String TAG = "SerialHelper";
    private BaseSerial mBaseSerial;
    private String sPort;
    private int iBaudRate = 9600;
    private boolean _isOpen = false;

    public SerialHelper(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
    }

    public SerialHelper() {
        // 默认 ttyXRUSB2 刷卡串口
        this("/dev/ttyXRUSB2", 9600);
    }

    /**
     * 打开串口
     * 0：打开串口成功
     * -1：无法打开串口：没有串口读/写权限！
     * -2：无法打开串口：未知错误！
     * -3：无法打开串口：参数错误！
     *
     * @return 是否打开
     */
    public boolean open() {
        int code = -4;
        LogUtils.d(TAG, "尝试打开串口");
        try {
            if (!MyUtils.isEmulator()) {
                // 设置系统路径
                SerialPort.setSuPath("/system/xbin/su");
                // 实例化工具
                mBaseSerial = new BaseSerial(sPort, iBaudRate) {
                    @Override
                    public void onDataBack(String hexstr) {
                        //这里是串口的数据返回，默认返回类型为16进制字符串
                        String decstr = SerialDataUtils.hexStringToString(hexstr); // 转为十进制字符串
                        LogUtils.d(TAG, "【" + sPort + "】\nhexstr: " + hexstr + "\ndecstr: " + decstr);
                        onMsgBack(hexstr, decstr);
                    }
                };
                // 打开
                code = mBaseSerial.openSerial();
            } else {
                LogUtils.d(TAG, "模拟器不支持打开串口");
            }
            _isOpen = code == 0;
            LogUtils.d(TAG, "打开串口状态：" + code);

        } catch (Exception | Error e) {
            LogUtils.e(TAG, "打开串口失败");
        }

        return _isOpen;
    }

    // 发送十六进制字符串
    public void sendHex(String hexstr) {
        if (mBaseSerial == null || !_isOpen) {
            return;
        }
        mBaseSerial.sendHex(hexstr);
    }

    // 发送十进制字符串
    public void sendDecTxt(String decstr) {
        if (mBaseSerial == null || !_isOpen) {
            return;
        }
        mBaseSerial.sendTxt(decstr);
    }

    // 触发回调信息
    public abstract void onMsgBack(String hexstr, String decstr);

    public void close() {
        if (mBaseSerial != null && _isOpen) {
            mBaseSerial.close();
            LogUtils.d(TAG, "关闭串口完成");
        }
        _isOpen = false;
    }


}
