/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License version 2.
 * 
 * This particular file is subject to the "Classpath" exception as provided in the 
 * LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
@nomodel
shared class SetOperators() {

    void m1(Set<Integer> a, Set<Integer> b) {
        Set<Integer> s1 = a | b;
        Set<Integer> s2 = a & b;
        Set<Integer> s3 = a ^ b;
        Set<Integer> s4 = a ~ b;
        variable Set<Integer> sync;
        sync := a | b;
        sync := a & b;
        sync := a ^ b;
        sync := a ~ b;
        sync |= a;
        sync &= a;
        sync ^= a;
        sync ~= a;
    }

    void m2(Set<Integer> a, Set<Float> b) {
        Set<Integer|Float> x1 = a | b;
        Set<Integer&Float> x2 = a & b;
        Set<Integer|Float> x3 = a ^ b;
        Set<Integer> x4 = a ~ b;
        variable value x5 := x1;
        x5 |= a;
        variable value x6 := x1;
        x6 &= b;
        variable value x7 := x3;
        x7 ^= b;
        variable value x8 := x4;
        x8 ~= b;
    }
    
    void m3(Set<Integer> a, Set<Bottom> b) {
        Set<Integer> s1 = a | b;
        Set<Bottom> s2 = a & b;
        Set<Integer> s3 = a ^ b;
        Set<Integer> s4 = a ~ b;
        variable Set<Integer> sync;
        sync := a | b;
        sync := a & b;
        sync := a ^ b;
        sync := a ~ b;
        sync |= a;
        sync &= a;
        sync ^= a;
        sync ~= a;
    }
    
    void m4<T>(Set<Object> a, Set<T> b) 
            given T satisfies Object{
        Set<Object> s1 = a | b;
        Set<T> s2 = a & b;
        Set<Object> s3 = a ^ b;
        Set<Object> s4 = a ~ b;
        variable Set<Object> sync;
        sync := a | b;
        sync := a & b;
        sync := a ^ b;
        sync := a ~ b;
        sync |= a;
        sync &= a;
        sync ^= a;
        sync ~= a;
    }
}