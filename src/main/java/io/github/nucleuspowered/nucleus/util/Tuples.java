/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.util;

import org.spongepowered.api.util.Tuple;

public final class Tuples {

    private Tuples() {}

    public static <A, B> Tuple<A, B> of(A a, B b) {
        return new Tuple<>(a, b);
    }

    public static <A, B, C> Triplet<A, B, C> of(A a, B b, C c) {
        return new Triplet<>(a, b, c);
    }

    public static class Triplet<A, B, C> {

        private A a;
        private B b;
        private C c;

        public Triplet(A a, B b, C c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        public A getA() {
            return a;
        }

        public void setA(A a) {
            this.a = a;
        }

        public B getB() {
            return b;
        }

        public void setB(B b) {
            this.b = b;
        }

        public C getC() {
            return c;
        }

        public void setC(C c) {
            this.c = c;
        }
    }
}
