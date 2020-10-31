/*********************************************************************
 * @Author: huxiaomin
 * @Date: 2020-10-04 11:46:00 
 * @Last Modified by: huxiaomin
 * @Last Modified time: 2020-10-04 11:46:41
*********************************************************************/
package com.vito.common.util;

public class Functions {
    public static interface RFunction0<R> {
        R call();
    }

    public static interface RFunction1<A, R> {
        R call(A a);
    }

    public static interface RFunction2<A, B, R> {
        R call(A a, B b);
    }

    public static interface RFunction3<A, B, C, R> {
        R call(A a, B b, C c);
    }

    public static interface RFunction4<A, B, C, D, R> {
        R call(A a, B b, C c, D d);
    }

    public static interface RFunction5<A, B, C, D, E, R> {
        R call(A a, B b, C c, D d, E e);
    }

    public static interface RFunction6<A, B, C, D, E, F, R> {
        R call(A a, B b, C c, D d, E e, F f);
    }

    public static interface RFunction7<A, B, C, D, E, F, G, R> {
        R call(A a, B b, C c, D d, E e, F f, G g);
    }

    public static interface RFunction8<A, B, C, D, E, F, G, H, R> {
        R call(A a, B b, C c, D d, E e, F f, G g, H h);
    }

    public static interface Function0 {
        void call();
    }

    public static interface Function1<A> {
        void call(A a);
    }

    public static interface Function2<A, B> {
        void call(A a, B b);
    }

    public static interface Function3<A, B, C> {
        void call(A a, B b, C c);
    }

    public static interface Function4<A, B, C, D> {
        void call(A a, B b, C c, D d);
    }

    public static interface Function5<A, B, C, D, E> {
        void call(A a, B b, C c, D d, E e);
    }

    public static interface Function6<A, B, C, D, E, F> {
        void call(A a, B b, C c, D d, E e, F f);
    }

    public static interface Function7<A, B, C, D, E, F, G> {
        void call(A a, B b, C c, D d, E e, F f, G g);
    }

    public static interface Function8<A, B, C, D, E, F, G, H> {
        void call(A a, B b, C c, D d, E e, F f, G g, H h);
    }
}
