/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

import cn.ponfee.disjob.common.util.CRC16;
import com.google.common.hash.Hashing;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.*;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Consistent hashing algorithm.
 *
 * @param <T> the ring node type
 * @author Ponfee
 */
public class ConsistentHash<T> {

    /**
     * Hash String to long value
     */
    @FunctionalInterface
    public interface HashFunction {
        /**
         * Returns key's int hash value
         *
         * @param key string key
         * @return int hash value
         */
        int hash(String key);

        HashFunction MD5 = key -> {
            byte[] digest = DigestUtils.md5(key);

            // digest.length == 16
            int hash = 0;
            for (int i = 16; i > 3; ) {
                // each grouped 4 byte block to int value
                int h = ((digest[--i] & 0xFF) << 24)
                      | ((digest[--i] & 0xFF) << 16)
                      | ((digest[--i] & 0xFF) <<  8)
                      | ((digest[--i] & 0xFF)      );
                hash = (i == 12) ? h : (hash ^ h);
            }
            return hash;
        };

        /**
         * Fowler-Noll-Vo
         */
        HashFunction FNV = key -> {
            int p = 16777619;
            int h = (int) 2166136261L;
            for (int i = 0; i < key.length(); i++) {
                h = (h ^ key.charAt(i)) * p;
            }
            h += h << 13;
            h ^= h >> 7;
            h += h << 3;
            h ^= h >> 17;
            h += h << 5;
            return h;
        };

        HashFunction SIP_HASH = key -> Hashing.sipHash24().hashBytes(key.getBytes(UTF_8)).asInt();

        HashFunction MURMUR3_32 = key -> Hashing.murmur3_32_fixed().hashBytes(key.getBytes(UTF_8)).asInt();

        HashFunction CRC_16 = key -> CRC16.digest(key.getBytes(UTF_8));
    }

    /**
     * Virtual node of the consistent hash ring.
     */
    private class VirtualNode {
        private final T physicalNode;
        private final String physicalKey;
        private final String virtualKey;

        private VirtualNode(T physicalNode, int replicaIndex) {
            this.physicalNode = physicalNode;
            this.physicalKey = keyMapper.apply(physicalNode);
            this.virtualKey = "SHARD-" + physicalKey + "-NODE-" + replicaIndex;
        }

        private boolean isVirtualNodeOf(T pNode) {
            return physicalKey.equals(keyMapper.apply(pNode));
        }
    }

    private final TreeMap<Integer, VirtualNode> ring = new TreeMap<>();
    private final Function<T, String> keyMapper;
    private final HashFunction hashFunction;

    public ConsistentHash(Collection<T> pNodes, int vNodeCount) {
        this(pNodes, vNodeCount, String::valueOf, HashFunction.MD5);
    }

    public ConsistentHash(Collection<T> pNodes,
                          int vNodeCount,
                          Function<T, String> keyMapper) {
        this(pNodes, vNodeCount, keyMapper, HashFunction.MD5);
    }

    /**
     * @param pNodes       collections of physical nodes
     * @param vNodeCount   number of virtual nodes
     * @param keyMapper    physical node mapping to string key function
     * @param hashFunction hash function to hash node instances
     */
    public ConsistentHash(Collection<T> pNodes,
                          int vNodeCount,
                          Function<T, String> keyMapper,
                          HashFunction hashFunction) {
        this.keyMapper = Objects.requireNonNull(keyMapper, "Key mapper cannot be null.");
        this.hashFunction = Objects.requireNonNull(hashFunction, "Hash function cannot be null.");
        if (pNodes != null) {
            for (T pNode : pNodes) {
                addNode(pNode, vNodeCount);
            }
        }
    }

    /**
     * Add physic node to the hash ring with some virtual nodes
     *
     * @param pNode      physical node
     * @param vNodeCount the number virtual node of the physical node.
     */
    public void addNode(T pNode, int vNodeCount) {
        if (vNodeCount < 0) {
            throw new IllegalArgumentException("Invalid virtual node counts :" + vNodeCount);
        }
        int existingReplicas = getExistingReplicas(pNode);
        for (int i = 0; i < vNodeCount; i++) {
            VirtualNode vNode = new VirtualNode(pNode, i + existingReplicas);
            ring.put(hashFunction.hash(vNode.virtualKey), vNode);
        }
    }

    /**
     * Remove the physical node from the hash ring
     *
     * @param pNode the physical node
     */
    public void removeNode(T pNode) {
        Iterator<Integer> it = ring.keySet().iterator();
        while (it.hasNext()) {
            Integer key = it.next();
            VirtualNode virtualNode = ring.get(key);
            if (virtualNode.isVirtualNodeOf(pNode)) {
                it.remove();
            }
        }
    }

    /**
     * Returns the physical node of counted specified key
     *
     * @param key the key to find the nearest physical node
     * @return routed physical node
     */
    public T routeNode(String key) {
        if (ring.isEmpty()) {
            return null;
        }

        SortedMap<Integer, VirtualNode> tailMap = ring.tailMap(hashFunction.hash(key));
        VirtualNode virtualNode = tailMap.isEmpty()
                                ? ring.firstEntry().getValue()
                                : ring.get(tailMap.firstKey());
        return virtualNode.physicalNode;
    }

    public int getExistingReplicas(T pNode) {
        return (int) ring.entrySet()
                         .stream()
                         .filter(e -> e.getValue().isVirtualNodeOf(pNode))
                         .count();
    }

}
