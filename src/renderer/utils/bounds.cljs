(ns renderer.utils.bounds
  (:require
   [clojure.core.matrix :as mat]
   [malli.experimental :as mx]
   [renderer.utils.math :refer [Vec2D]]))

(def Bounds
  "Coordinates that define a bounding box."
  [:tuple
   [number? {:title "left"}]
   [number? {:title "top"}]
   [number? {:title "right"}]
   [number? {:title "bottom"}]])

(mx/defn from-bbox :- [:maybe Bounds]
  "Experimental way of getting the bounds of uknown or complicated elements
   using the getBBox method.
   https://developer.mozilla.org/en-US/docs/Web/API/SVGGraphicsElement/getBBox"
  [^js/Element el]
  (when (.-getBBox el)
    (let [b (.getBBox el)
          x1 (.-x b)
          y1 (.-y b)
          x2 (+ x1 (.-width b))
          y2 (+ y1 (.-height b))]
      [x1 y1 x2 y2])))

(mx/defn union :- Bounds
  "Calculates the bounds that contain an arbitrary set of bounds."
  [& bounds :- [:+ Bounds]]
  (vec (concat (apply map min (map #(take 2 %) bounds))
               (apply map max (map #(drop 2 %) bounds)))))

(mx/defn ->dimensions :- Vec2D
  "Converts bounds to [width heigh]"
  [[x1 y1 x2 y2] :- Bounds]
  (mat/sub [x2 y2] [x1 y1]))

(mx/defn center :- Vec2D
  "Calculates the center of bounds."
  [b :- Bounds]
  (mat/add (take 2 b)
           (mat/div (->dimensions b) 2)))

(mx/defn intersect? :- boolean?
  "Tests whether the provided set of bounds intersect."
  [[a-left a-top a-right a-bottom] :- Bounds,
   [b-left b-top b-right b-bottom] :- Bounds]
  (not (or (> b-left a-right)
           (< b-right a-left)
           (> b-top a-bottom)
           (< b-bottom a-top))))

(mx/defn contained? :- boolean?
  "Tests whether `bounds-a` fully contain `bounds-b`."
  [[a-left a-top a-right a-bottom] :- Bounds,
   [b-left b-top b-right b-bottom] :- Bounds]
  (and (> a-left b-left)
       (> a-top b-top)
       (< a-right b-right)
       (< a-bottom b-bottom)))

(mx/defn contained-point? :- boolean?
  "Tests whether the provided bounds contain a point."
  [[left top right bottom] :- Bounds, [x y] :- Vec2D]
  (and (<= left x)
       (<= top y)
       (>= right x)
       (>= bottom y)))
