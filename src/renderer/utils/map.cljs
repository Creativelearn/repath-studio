(ns renderer.utils.map
  (:require
   [clojure.set :as set]))

(defn merge-common-with
  "Equivelent to merge-with for common keys across all maps."
  [f & maps]
  (let [common-keys (->> maps (apply (comp keys set)) set/intersection)]
    (apply merge-with f (map #(select-keys % common-keys) maps))))

(defn remove-nils
  "Removes nil values from maps (should be used sparingly)."
  [a]
  (->> a
       (remove (comp nil? val))
       (into {})))
