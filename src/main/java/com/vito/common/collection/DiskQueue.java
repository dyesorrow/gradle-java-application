/*********************************************************************
 * 磁盘队列
 * 
 * @Author: huxiaomin
 * @Date: 2020-10-04 11:45:07 
 * @Last Modified by: huxiaomin
 * @Last Modified time: 2020-10-04 13:37:18
*********************************************************************/

package com.vito.common.collection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import com.vito.common.util.Functions.Function0;
import com.vito.common.util.SerializeUtil;
import com.vito.common.util.SerializeUtil.SerializeObject;

import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

// 注意这里读取时多个读取，只保证读取的
@Slf4j
public class DiskQueue<T> {

    @Setter
    private int MaxLength = 1024 * 1024 * 10;

    private RandomAccessFile poller; // 读取也是一直只能往后读
    private RandomAccessFile pusher;
    private DiskIndex pollIndex;
    private DiskIndex pushIndex;
    private File dir;
    private File flag;
    // 队列，必须保证在意外情况挂掉时，能从第一个未成功的位置起重试
    private Queue<Reader<T>> queue = new LinkedList<>();

    @ToString
    public static class DiskIndex extends SerializeObject<DiskIndex> {
        private static final long serialVersionUID = 2346929015539888867L;
        private int index = 0;
        private long pos = 0;
    }

    public static class Reader<T> {
        private int index;
        private long nextPos;
        private int flag;
        public T data;
        /**
         * 获取内容操作成功后，必须回调此函数来更新标志位，否则重启后将全部回滚一边
         * 对于未成功的操作，由使用者自行决定放弃或者重试
         * 只要自己没有调用success, 处于自己队列位置后边的内容，即使成功，当重启时也会回滚
         */
        public Function0 success;
    }

    public DiskQueue(String dirPath) {
        this.dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
    }

    public void init() throws ClassNotFoundException, IOException {
        // 加载读取标志
        flag = new File(dir, "read");
        pollIndex = DiskIndex.load(DiskIndex.class, flag);
        if (null == pollIndex) {
            log.warn("not read flag file, will create one");
            pollIndex = new DiskIndex();
        }
        log.info("success load poll index: {}", pollIndex);

        File file = new File(dir, "0");
        if (!file.exists()) {
            file.createNewFile();
        }

        // 获取文件写入器
        File[] files = dir.listFiles();
        Arrays.sort(files, (a, b) -> {
            if (a.getName().equals("read")) {
                return -1;
            }
            if (a.lastModified() > b.lastModified()) {
                return 1;
            } else {
                return -1;
            }
        });

        File currentFile = files[files.length - 1];
        pusher = new RandomAccessFile(currentFile, "rw");
        pushIndex = new DiskIndex();
        pushIndex.index = Integer.parseInt(currentFile.getName());
        log.info("success load current write file: {}", currentFile.getAbsolutePath());

        // 获取文件读取器
        poller = new RandomAccessFile(new File(dir, "" + pollIndex.index), "r");
    }

    @SuppressWarnings("unchecked")
    public Reader<T> poll() throws IOException, ClassNotFoundException, InterruptedException {
        synchronized (pollIndex) {

            // 获取一个可以读取的位置
            while (true) {
                if (pollIndex.pos < poller.length()) {
                    break;
                }
                if (poller.length() == 0) {
                    synchronized (poller) {
                        poller.wait();
                    }
                } else {
                    File toNew = new File(dir, "" + (pollIndex.index + 1));
                    if (toNew.exists()) {
                        pollIndex.index++;
                        pollIndex.pos = 0;
                        poller = new RandomAccessFile(toNew, "r");
                    } else {
                        synchronized (poller) {
                            poller.wait();
                        }
                    }
                }
            }

            // 读取数据
            poller.seek(pollIndex.pos);
            int len = poller.readInt();
            byte[] buff = new byte[len];
            poller.read(buff);
            pollIndex.pos = poller.getFilePointer();

            // 读取数据缓存器
            Reader<T> reader = new Reader<>();
            reader.index = pollIndex.index;
            reader.nextPos = pollIndex.pos;
            reader.flag = 0; // 正在处理
            reader.data = (T) SerializeUtil.deserialize(buff);

            // 成功回调，成功必须回调此函数
            reader.success = () -> {
                reader.flag = 1; // 处理成功
                synchronized (queue) {
                    // 成功则刷新成功标注位
                    Reader<T> last = queue.peek();
                    while (queue.peek() != null && queue.peek().flag == 1) {
                        last = queue.peek();
                        queue.poll();
                    }
                    if (last != null) {
                        DiskIndex index = new DiskIndex();
                        index.index = last.index;
                        index.pos = last.nextPos;
                        try {
                            index.backTo(flag);
                            log.debug("back read flag: {}", index);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            };

            // 添加到队列，等待成功回调
            queue.add(reader);
            return reader;
        }
    }

    public void push(T obj) throws FileNotFoundException, IOException {
        synchronized (pushIndex) {
            if (pusher.length() > MaxLength) {
                pushIndex.index++;
                pusher = new RandomAccessFile(new File(dir, "" + pushIndex.index), "rw");
            }

            byte[] buff = SerializeUtil.serialize(obj);
            pusher.seek(pusher.length());
            pusher.writeInt(buff.length);
            pusher.write(buff);

            synchronized (poller) {
                poller.notify();
            }

        }
    }

}
