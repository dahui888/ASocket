package com.king.asocket.udp;

import com.king.asocket.ISocket;
import com.king.asocket.util.LogUtils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * UDP客户端：收发单播或广播消息
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 */
public class UDPClient implements ISocket<DatagramSocket> {

    private DatagramSocket mSocket;

    private String mHost;

    private int mPort;

    private int mLocalPort;

    private int mLength;

    private boolean isStart;

    private OnMessageReceivedListener mOnMessageReceivedListener;

    private InetAddress mInetAddress;

    /**
     * 构造
     * @param host UDP服务端的主机地址（广播地址）
     * @param port UDP服务端的端口
     */
    public UDPClient(String host,int port){
        this(host,port,0);
    }

    /**
     * 构造
     * @param host UDP服务端的主机地址（广播地址）
     * @param port UDP服务端的端口
     * @param localPort UDP客户端的本地端口
     */
    public UDPClient(String host,int port,int localPort){
        this(host,port,localPort,1460);
    }

    /**
     * 构造
     * @param host UDP服务端的主机地址（广播地址）
     * @param port UDP服务端的端口
     * @param localPort UDP客户端的本地端口
     * @param length 接收数据包的长度，超出会造成拆分成多条数据包
     */
    public UDPClient(String host,int port,int localPort,int length){
        mHost = host;
        mPort = port;
        mLocalPort = localPort;
        mLength = length;
    }

    @Override
    public DatagramSocket getSocket(){
        return mSocket;
    }

    @Override
    public void start() {
        if(isStart()){
            return;
        }
        try {
            mSocket = new DatagramSocket(mLocalPort);
            mLocalPort = mSocket.getLocalPort();
            LogUtils.d(String.format("localAddress:%s:%d",mSocket.getLocalAddress().getHostAddress(),mLocalPort));
            mSocket.setReuseAddress(true);
            isStart = mSocket.isBound();
            while (isStart()){
                DatagramPacket data = new DatagramPacket(new byte[mLength],mLength);
                mSocket.receive(data);
                byte[] value = new byte[data.getLength() - data.getOffset()];
                System.arraycopy(data.getData(),data.getOffset(),value,0,value.length);
                if(LogUtils.isShowLog()){
                    LogUtils.d("Received:" + String.format("%s:%d -> ",data.getAddress(),data.getPort())  + new String(value));
                }
                if(mOnMessageReceivedListener != null){
                    mOnMessageReceivedListener.onMessageReceived(value);
                }
            }
            mSocket.close();
        } catch (Exception e) {
            isStart = false;
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try{
            if(!isClosed()){
                mSocket.close();
            }
            isStart = false;
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public boolean isStart() {
        return mSocket != null && isStart && !mSocket.isClosed();
    }

    @Override
    public boolean isClosed() {
        if(mSocket != null){
            return mSocket.isClosed();
        }
        return true;
    }

    @Override
    public void write(byte[] data) {
        if(!isStart()){
            LogUtils.d("Client has not started");
            return;
        }
        try {
            if(mInetAddress == null){
                mInetAddress = InetAddress.getByName(mHost);
            }
            DatagramPacket packet = new DatagramPacket(data,0,data.length,mInetAddress,mPort);
            mSocket.send(packet);
            if(LogUtils.isShowLog()) {
                LogUtils.d("write:" + new String(data));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void write(DatagramPacket data) {
        if(!isStart()){
            LogUtils.d("Client has not started");
            return;
        }
        try {
            mSocket.send(data);
            if(LogUtils.isShowLog()) {
                byte[] value = new byte[data.getLength() - data.getOffset()];
                System.arraycopy(data.getData(), data.getOffset(), value, 0, value.length);
                LogUtils.d("write:" + new String(value));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        mOnMessageReceivedListener = listener;
    }
}
