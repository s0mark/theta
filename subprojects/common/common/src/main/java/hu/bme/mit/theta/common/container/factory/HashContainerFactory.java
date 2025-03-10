/*
 *  Copyright 2025 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.common.container.factory;

import java.util.*;

public class HashContainerFactory implements ContainerFactory {

    @Override
    public <K, V> Map<K, V> createMap() {
        return new HashMap<K, V>();
    }

    @Override
    public <K, V> Map<K, V> createMap(int initialCapacity) {
        return new HashMap<K, V>(initialCapacity);
    }

    @Override
    public <K, V> Map<K, V> createMap(int initialCapacity, float loadFactor) {
        return new HashMap<K, V>(initialCapacity, loadFactor);
    }

    @Override
    public <K, V> Map<K, V> createMap(Map<? extends K, ? extends V> m) {
        return new HashMap<K, V>(m);
    }

    @Override
    public <E> Set<E> createSet() {
        return new HashSet<E>();
    }

    @Override
    public <E> Set<E> createSet(int initialCapacity) {
        return new HashSet<E>(initialCapacity);
    }

    @Override
    public <E> Set<E> createSet(int initialCapacity, float loadFactor) {
        return new HashSet<E>(initialCapacity, loadFactor);
    }

    @Override
    public <E> Set<E> createSet(Collection<? extends E> c) {
        return new HashSet<E>(c);
    }
}
