package cn.ponfee.scheduler.common.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

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
            assert digest.length == 16;

            int hash = 0;
            for (int i = 15; i >= 3; ) {
                int h = ((digest[i--] & 0xFF) << 24)
                      | ((digest[i--] & 0xFF) << 16)
                      | ((digest[i--] & 0xFF) <<  8)
                      | ((digest[i--] & 0xFF)      );
                hash = (i == 11) ? h : (hash ^ h);
            }
            return hash;
        };

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

        HashFunction CRC16 = key -> cn.ponfee.scheduler.common.util.CRC16.digest(key.getBytes(StandardCharsets.UTF_8));
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
        if (keyMapper == null) {
            throw new NullPointerException("Key mapper is null.");
        }
        if (hashFunction == null) {
            throw new NullPointerException("Hash function is null.");
        }
        this.keyMapper = keyMapper;
        this.hashFunction = hashFunction;
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