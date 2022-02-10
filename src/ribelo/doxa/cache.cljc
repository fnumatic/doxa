(ns ribelo.doxa.cache
  (:require
   [ribelo.extropy :as ex]
   [ribelo.doxa.protocols :as p]
   [ribelo.doxa.util :as u])
  #?(:clj
     (:import
      [ribelo.doxa.util CachedResult])))

(deftype TickedCacheEntry [item ^long udt ^long tick-lru ^long tick-lfu])

(declare -refresh-cache)

(deftype DoxaCache [m ^long tick ^long cache-size ^long ttl-ms]
  #?@(:clj
      [clojure.lang.ILookup
       (valAt [_ k] (.-item ^TickedCacheEntry (ex/-get m k)))

       clojure.lang.IFn
       (invoke [_ k] (.-item ^TickedCacheEntry (ex/-get m k)))]

      :cljs
      [ILookup
       (-lookup [_ k] (.-item (ex/-get* m k)))

       IFn
       (-invoke [_ k] (.-item (ex/-get* m k)))])

  p/IDoxaCache
  (p/-has? [this k]
    (let [instant (ex/-now-udt)]
      (when-let [^TickedCacheEntry ?e (ex/-get m k)]
        (if (or (zero? ttl-ms) (< (- instant (.-udt ?e)) ttl-ms))
          true
          (not (boolean (p/-evict this k)))))))

  (p/-hit [_ args]
    (let [^TickedCacheEntry ?e (ex/-get m args)
          tick' (inc tick)
          m' (ex/-assoc* m args (TickedCacheEntry. (.-item ?e) (.-udt ?e) (.-tick-lru ?e) (inc (.-tick-lfu ?e))))]
      (DoxaCache. m' tick' cache-size ttl-ms)))

  (p/-evict [this args]
    (p/-run-gc this)
    (DoxaCache. (ex/-dissoc* m args) tick cache-size ttl-ms))

  (p/-miss [this k result]
    (let [this' (p/-run-gc this)
          tick' (inc tick)
          instant (ex/-now-udt)
          m' (ex/-assoc* (.-m ^DoxaCache this') k (TickedCacheEntry. result instant tick' 1))]
      (DoxaCache. m' tick' cache-size ttl-ms)))

  (p/-gc-now? [_]
    #?(:clj  (<= (java.lang.Math/random) (/ 1.0 16000))
       :cljs (<=       (.random js/Math) (/ 1.0 16000))))

  (p/-run-gc [this]
    (if (p/-gc-now? this)
      (let [instant (ex/-now-udt)
            m' (persistent!
                (ex/-reduce-kv
                 (fn [acc k ^TickedCacheEntry e]
                   (if (and (pos? ttl-ms) (> (- instant (.-udt e)) ttl-ms))
                     (ex/-dissoc!* acc k)
                     acc))
                 (transient (or m {}))
                 m))]
        (DoxaCache. m' tick cache-size ttl-ms))
      this))

  (p/-refresh [_ changes]
    (-refresh-cache m tick cache-size ttl-ms changes)))

(defn -refresh-cache [m tick cache-size ttl-ms changes]
  (let [m' (ex/-loop [me m :let [acc (transient m)]]
               (let [k (ex/-k* me)
                     ^TickedCacheEntry e (ex/-v* me)
                     datoms (.-datoms ^CachedResult (.-item e))]
                 (if (u/-datoms-match-changes? datoms changes)
                   (recur (ex/-dissoc!* acc k))
                   (recur acc)))
               (persistent! acc))]
    (DoxaCache. m' tick cache-size ttl-ms)))

(defn doxa-cache
  ([] (doxa-cache {}))
  ([{:keys [cache-size ttl-ms]
     :or {cache-size 1024
          ttl-ms 0}}]
   (DoxaCache. {} 0 cache-size ttl-ms)))
