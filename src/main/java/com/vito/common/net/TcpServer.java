package com.vito.common.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import com.vito.common.thread.AsyncPool;
import com.vito.common.thread.GloablAsyncPool;
import com.vito.common.util.Functions.RFunction0;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TcpServer {

    @Setter
    private Integer port = 8080;

    @Setter
    private Integer timeout = 3000;

    @Setter
    private Integer buffSize = 10240;

    @Setter
    private RFunction0<SelectionHandler> handlerCreator;

    @Setter
    private AsyncPool taskPool = GloablAsyncPool.getInstance();

    public void start() {

        try (Selector selector = Selector.open(); ServerSocketChannel ssc = ServerSocketChannel.open()) {
            ssc.socket().bind(new InetSocketAddress(port));
            ssc.configureBlocking(false);
            ssc.register(selector, SelectionKey.OP_ACCEPT);

            log.info("Server start with port[{}], timeout[{}], taskPoolSize[{}], buffSize[{}] !", port, timeout, taskPool.getSize(), buffSize);
            while (true) {
                if (selector.select(timeout) == 0) {
                    continue;
                }
                Iterator<SelectionKey> iters = selector.selectedKeys().iterator();
                while (iters.hasNext()) {
                    try {
                        SelectionKey key = iters.next();
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        }
                        if (key.isReadable()) {
                            handleRead(key);
                        }
                        if (key.isWritable()) {
                            handleWrite(key);
                        }
                        iters.remove();
                    } catch (Exception e) {
                        log.error("connect lose: {}", e.getMessage());
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 接收到请求
    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();
        SocketChannel sc = ssChannel.accept();
        SelectionHandler handler = handlerCreator.call();
        handler.buffer = ByteBuffer.allocate(buffSize);
        handler.sChannel = sc;
        // handler.selector = key.selector();

        sc.configureBlocking(false);
        sc.register(key.selector(), SelectionKey.OP_READ, handler);

        log.info("get connect...");
    }

    // 被动读取与处理
    private void handleRead(SelectionKey key) throws IOException {
        final SocketChannel sc = (SocketChannel) key.channel();
        final SelectionHandler handler = (SelectionHandler) key.attachment();
        final StringBuilder str = new StringBuilder();

        int bytesRead = sc.read(handler.buffer);
        while (bytesRead > 0) {
            handler.buffer.flip();
            byte[] bytes = new byte[bytesRead];
            handler.buffer.get(bytes);
            String v = new String(bytes, StandardCharsets.UTF_8);
            str.append(v);
            handler.buffer.clear();
            bytesRead = sc.read(handler.buffer);
        }
        if (bytesRead == -1) {
            sc.close();
        }

        this.taskPool.push(() -> {
            handler.consum(str.toString());
        });
    }

    // 这种方式，不能及时响应
    private void handleWrite(SelectionKey key) throws IOException {
        // SocketChannel sc = (SocketChannel) key.channel();
        // SelectionHandler handler = (SelectionHandler) key.attachment();

        // while (!handler.sendQueue.isEmpty()) {
        //     String sendData = handler.sendQueue.poll();
        //     try {
        //         ByteBuffer buffer = ByteBuffer.allocate(sendData.length());
        //         buffer.put(sendData.getBytes());
        //         buffer.flip();
        //         sc.write(buffer);
        //     } catch (IOException e) {
        //         e.printStackTrace();
        //     }
        // }

        // key.interestOps(SelectionKey.OP_READ);
    }

    public static abstract class SelectionHandler {
        // private Queue<String> sendQueue = new LinkedList<>();
        // private Selector selector;
        private SocketChannel sChannel;
        private ByteBuffer buffer;

        /**
         * 消费来自客户端的请求
         * 
         * @param str
         */
        public abstract void consum(String data);

        public final void close(){
            try {
                sChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * 向客户端发送响应信息
         * 
         * @param data
         */
        public final void send(String data) {
            ByteBuffer buffer = ByteBuffer.allocate(data.length());
            buffer.put(data.getBytes());
            buffer.flip();
            try {
                if(sChannel.isConnected()){
                    sChannel.write(buffer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // sendQueue.add(data);
            // try {
            //     sChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
            // } catch (ClosedChannelException e) {
            //     e.printStackTrace();
            // }
        }
    }

}
