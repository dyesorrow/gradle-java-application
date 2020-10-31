/*********************************************************************
 * TODO
 * @Author: mikey.zhaopeng 
 * @Date: 2020-10-04 11:46:54 
 * @Last Modified by: huxiaomin
 * @Last Modified time: 2020-10-04 11:50:27
*********************************************************************/
package com.vito.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SerializeUtil {

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream ser = new ObjectOutputStream(out);
        ser.writeObject(obj);
        byte[] buf = out.toByteArray();
        ser.close();
        out.close();
        return buf;
    }

    @SuppressWarnings("unchecked")
    public static <E> E deserialize(Class<E> type, byte[] buf) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(buf);
        ObjectInputStream ser = new ObjectInputStream(in);
        E e = (E) ser.readObject();
        ser.close();
        in.close();
        return e;
    }

    public static Object deserialize(byte[] buf) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(buf);
        ObjectInputStream ser = new ObjectInputStream(in);
        Object e = ser.readObject();
        ser.close();
        in.close();
        return e;
    }

    public static abstract class SerializeObject<T> implements Serializable {

        private static final long serialVersionUID = -2025858693669150843L;

        public static <E> E load(Class<E> clazz, File file) throws ClassNotFoundException, IOException {
            if (!file.exists()) {
                file.createNewFile();
                return null;
            }
            FileInputStream reader = new FileInputStream(file);
            int len = (int) file.length();
            if (len == 0) {
                reader.close();
                return null;
            }
            byte[] buff = new byte[len];
            reader.read(buff);
            reader.close();
            return SerializeUtil.deserialize(clazz, buff);
        }

        public void backTo(File file) throws IOException {
            byte[] buff = SerializeUtil.serialize(this);
            FileOutputStream writer = new FileOutputStream(file);
            writer.write(buff);
            writer.close();
        }
    }

}
