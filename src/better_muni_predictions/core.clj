(ns better-muni-predictions.core
  (:require [clojurewerkz.titanium.graph    :as tg]
            [clojurewerkz.titanium.edges    :as te]
            [clojurewerkz.titanium.vertices :as tv]
            [clojurewerkz.titanium.types    :as tt]
            [clojurewerkz.titanium.query    :as tq]
            [clj-nextbus.core :as nextbus])
  (:gen-class))

(def ^:dynamic *default-nextbus-agency* "sf-muni")

(defn parse-direction
  "Given a direction XML object, returns a direction metadata object.

   Direction metadata has the following schema:

     :tag         A unique identifier for the direction.
     :stops       A vector of stop :tag's which represent the stops (in order) at which a bus will
                    stop at."
  [direction]
  {:stops (map #(-> % :attrs :tag) (:content direction))
   :tag   (-> direction :attrs :tag)})

(defn parse-stop
  "Given a stop XML object and a route tag, returns a stop metadata object.

   Stop metadata has the following schema:

     :tag         The id of the stop, which is unique across location. Two routes that stop at the
                    same location have the same stop id at that location.
     :title       The friendly name of the stop.
     :lat         The latitutde of the stop.
     :lon         The longitude of the stop.
     :route-tag   A unique identifier for the route."
  [stop route-tag]
  (assoc (:attrs stop) :route-tag route-tag))

(defn mk-stop-to-neighbors
  "Makes a map from stop to a map of direction to neighbors."
  [directions]
  (println directions)
  (let [entries (for [dir (vals directions)
                      [a b c] (partition 3 1 (concat [nil] (:stops dir) [nil]))]
                  [b (:tag dir) [a c]])
        stop->entries (group-by first entries)
        ks (keys stop->entries)
        vs (vals stop->entries)
        direction->entries (map #(->> % (map (comp vec rest)) (into {})) vs)]
    (println direction->entries)
    (zipmap ks direction->entries)))

(defn process-route
  "Processes all info for a route given a route tag. Returns a map:

     :stops             A map with stop tag as keys and stop metadata as values.
     :directions        A map with direction tag as key and direction metadata as value.
     :stop-to-neighbors A map from stop-tag to map of direction tag to neighbors [before after]"
  [route]
  (let [route-tag (-> route :attrs :tag)
        route-conf (nextbus/route-config :route route-tag)
        stops-and-directions (-> route-conf :content first :content)
        stops (filter #(= :stop (:tag %)) stops-and-directions)
        directions (filter #(= :direction (:tag %)) stops-and-directions)
        stop-objects (map #(parse-stop % route-tag) stops)
        direction-objects (map parse-direction directions)
        stops (zipmap (map :tag stop-objects) stop-objects)
        directions (zipmap (map :tag direction-objects) direction-objects)
        stop-to-neighbors (mk-stop-to-neighbors directions)]
    {:stops stops
     :directions directions
     :stop-to-neighbors stop-to-neighbors}))

;; This script will populate an offline database of all MUNI stop times for the next hour.
;; The graph will have the following schema:
;;   - Nodes are represented by (:stop-location, :route-tag, :arrival-time) tuples.
;;   - Edges in this graph are directed and weighted. A edge exists from node 'A to node 'B if
;;     one of the following is true:
;;      1) condition: (and (= (:stop-location A) (:stop-location B))
;;                         (<= (:arrival-time A) (:arrival-time B)))
;;         weight:    (- (:arrival-time B) (:arrival-time A))
;;      2) condition: (and (= (:route-tag A) (:route-tag) B)
;;                         (is-stop-right-before A B))
;;            The second condition means if a bus on (:route-tag A) passes A, then the next stop
;;            that the bus arrives at would be B.
;;         weight:    (- (:arrival-time b) (:arrival-time a))
;;
(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (tg/open (System/getProperty "java.io.tmpdir"))
  (let [muni-routes (nextbus/route-list)]
    (doseq [route (:content muni-routes)]
      (process-route route))))
