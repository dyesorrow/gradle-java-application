package com.vito.common.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import com.vito.common.thread.AsyncPool;
import com.vito.common.thread.GloablAsyncPool;
import com.vito.common.util.ProException;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TcpClient {

    @Setter
    private String serverIp;

    @Setter
    private int serverPort;

    @Setter
    private Integer timeout = 3000;

    @Setter
    private Integer buffSize = 10240;

    @Setter
    protected AsyncPool taskPool = GloablAsyncPool.getInstance();

    private SocketChannel channel;

    public void start() {
        try (Selector selector = Selector.open(); SocketChannel channel = SocketChannel.open();) {
            // 设置为非阻塞模式，这个方法必须在实际连接之前调用(所以open的时候不能提供服务器地址，否则会自动连接)
            channel.configureBlocking(false);

            boolean ret = channel.connect(new InetSocketAddress(serverIp, serverPort));
            if (ret) {
                channel.register(selector, SelectionKey.OP_READ);
            } else {
                channel.register(selector, SelectionKey.OP_CONNECT);
            }

            this.channel = channel;

            while (true) {
                if (selector.select(timeout) == 0) {
                    continue;
                }
                Iterator<SelectionKey> iters = selector.selectedKeys().iterator();
                while (iters.hasNext()) {
                    SelectionKey key = iters.next();
                    if (key.isConnectable()) {
                        if (channel.finishConnect()) {
                            handleConnect(key);
                        }else{
                            throw new ProException("connect error");
                        }
                    }
                    if (key.isReadable()) {
                        handleRead(key);
                    }
                    if (key.isWritable()) {
                        handleWrite(key);
                    }
                    iters.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleConnect(SelectionKey key) throws IOException {
        log.info("client connetct to: {}:{}", serverIp, serverPort);

        ByteBuffer buffer = ByteBuffer.allocate(buffSize);
        channel.configureBlocking(false);
        channel.register(key.selector(), SelectionKey.OP_READ, buffer);
    }

    private void handleRead(SelectionKey key) throws IOException {
        final SocketChannel sc = (SocketChannel) key.channel();
        final ByteBuffer buffer = (ByteBuffer) key.attachment();
        final StringBuilder str = new StringBuilder();

        int bytesRead = sc.read(buffer);
        while (bytesRead > 0) {
            buffer.flip();
            byte[] bytes = new byte[bytesRead];
            buffer.get(bytes);
            String v = new String(bytes, StandardCharsets.UTF_8);
            str.append(v);
            buffer.clear();
            bytesRead = sc.read(buffer);
        }
        if (bytesRead == -1) {
            sc.close();
        }

        this.taskPool.push(() -> {
            onResponse(str.toString());
        });
    }

    private void handleWrite(SelectionKey key) {
        // 这个发送有延迟，不使用此方式
    }

    // 子类重新此方法
    protected void onResponse(String data) {

    }

    // 调用发送接口
    protected final void send(String data) {
        ByteBuffer buffer = ByteBuffer.allocate(data.length());
        buffer.put(data.getBytes());
        buffer.flip();
        try {
            if (channel.isConnected()) {
                channel.write(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
