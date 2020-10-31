/*********************************************************************
 * LRU Cache
 * @Author: huxiaomin 
 * @Date: 2020-10-04 11:50:07 
 * @Last Modified by:   huxiaomin 
 * @Last Modified time: 2020-10-04 11:50:07 
*********************************************************************/

package com.vito.common.collection;

import java.util.HashMap;
import java.util.Map;

public class LRUCache<K, V> {
    public static class ListNode<K, V> {
        private K key;
        private V value;
        private ListNode<K, V> next;
        private ListNode<K, V> prev;
    }

    private Integer capacity = 128;
    private Map<K, ListNode<K, V>> hashTbale = new HashMap<>();
    private ListNode<K, V> head;
    private ListNode<K, V> tail;

    public void put(K key, V value) {
        ListNode<K, V> node = this.hashTbale.get(key);
        if (node == null) {
            node = new ListNode<K, V>();
            node.key = key;
            this.hashTbale.put(key, node);
        }
        node.value = value;
        moveToHead(node);
    }

    public V get(K key) {
        ListNode<K, V> node = this.hashTbale.get(key);
        if (node == null) {
            return null;
        }
        moveToHead(node);
        return node.value;
    }

    public void resize(int capacity) {
        this.capacity = capacity;
    }

    private void moveToHead(ListNode<K, V> node) {
        // 如果是尾部，更新尾部
        if(node == tail){
            tail = tail.prev;
        }

        // 更新前后节点
        if (node.prev != null) {
            node.prev.next = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        }

        // 设置为头部
        if(head != null){
            head.prev = node;
        }
        node.next = head;
        node.prev = null;
        head = node;        
        

        if (tail == null) {
            tail = node;
        }

        // 超出容量移除
        if (this.hashTbale.size() > this.capacity) {
            this.hashTbale.remove(tail.key);
            if (tail.prev != null) {
                tail.prev.next = null;
            }
            tail = tail.prev;
        }
    }

    public static void check(boolean flag){
        if(!flag){
            throw new RuntimeException();
        }
    }
}
