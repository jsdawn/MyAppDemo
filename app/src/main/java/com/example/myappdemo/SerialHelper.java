package com.example.myappdemo;

import android.serialport.SerialPort;

import com.october.lib.logger.LogUtils;
import com.vi.vioserial.BaseSerial;


public class SerialHelper {
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
        try {
            SerialPort.setSuPath("/system/xbin/su");
            mBaseSerial = new BaseSerial(sPort, iBaudRate) {
                @Override
                public void onDataBack(String s) {
                    //这里是串口的数据返回，默认返回类型为16进制字符串
                    LogUtils.d(TAG, "【" + sPort + "】：" + s);
                }
            };
            code = mBaseSerial.openSerial();
            _isOpen = code == 0;

        } catch (Exception | Error e) {
            LogUtils.e(TAG, "打开串口失败");
        }

        return _isOpen;
    }

    public void close() {
        if (mBaseSerial != null && _isOpen) {
            mBaseSerial.close();
            LogUtils.d(TAG, "关闭串口");
        }
        _isOpen = false;
    }


}
